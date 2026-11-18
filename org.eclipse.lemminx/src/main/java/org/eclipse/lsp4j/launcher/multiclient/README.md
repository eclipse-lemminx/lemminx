# LSP4J Multi-Client Launcher

Generic multi-client support for Language Server Protocol servers based on Eclipse LSP4J.

## Purpose

This package provides a **generic, reusable** framework for LSP servers that need to support **multiple clients connecting simultaneously** to the same server instance via TCP sockets.

### Problem

By default, LSP servers are designed for a 1:1 relationship between client and server:
- Traditional stdio transport: one stdin/stdout pair per process
- Each client spawns its own server process
- Memory and CPU duplication when multiple clients need the same server

### Solution

The multi-client launcher allows:
- ✅ **Multiple clients** connecting to the same server process via TCP sockets
- ✅ **Shared server instance** - one language server instance serves all clients
- ✅ **Broadcast notifications** - server notifications (diagnostics, logs) sent to all connected clients
- ✅ **Independent requests** - each client can make requests independently
- ✅ **Transparent initialization** - first client initializes normally, subsequent clients receive cached capabilities
- ✅ **Auto-cleanup** - disconnected clients are automatically removed from broadcast list

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│              MultiClientLauncher.Builder                 │
│                                                           │
│   Primary Client (stdio)  ←────────┐                    │
│         ↓                           │                    │
│   ┌──────────────────────┐          │                    │
│   │  Language Server     │          │                    │
│   │  (single instance)   │          │                    │
│   └──────────────────────┘          │                    │
│         ↓                           │                    │
│   ┌──────────────────────┐          │                    │
│   │  MultiClientProxy    │  ←───────┤                    │
│   │  (broadcasts)        │          │                    │
│   └──────────────────────┘          │                    │
│         ↓          ↓                │                    │
│    Client 1    Client 2 ... Client N                     │
│    (stdio)     (socket)     (socket)                     │
└────────┬──────────┬─────────────────────────────────────┘
         │          │
      stdio      Socket 1, Socket 2, ...
```

## Components

### 1. **MultiClientLauncher.Builder**

Fluent API that extends LSP4J's `Launcher.Builder` with multi-client socket support.

**Usage:**
```java
XMLLanguageServer server = new XMLLanguageServer();

Launcher<LanguageClient> launcher = new MultiClientLauncher.Builder<LanguageClient>()
    .setLocalService(server)
    .setRemoteInterface(XMLLanguageClientAPI.class)
    .setInput(in)
    .setOutput(out)
    .setExecutorService(Executors.newCachedThreadPool())
    .wrapMessages(wrapper)
    .enableSocket(0, workspacePath, "lemminx")  // 0 = free port
    .create();

server.setClient(launcher.getRemoteProxy());
launcher.startListening();
```

**Key Features:**
- Extends standard LSP4J `Launcher.Builder` API
- `.enableSocket(port, workspacePath, serverType)` activates multi-client mode
- Port 0 = automatic free port selection
- Returns a wrapper that provides `MultiClientProxy` via `getRemoteProxy()`

### 2. **MultiClientProxy**

Dynamic proxy that broadcasts LSP notifications to all connected clients.

**Behavior:**
- **Notifications** (void methods, `publishDiagnostics`, etc.) → broadcast to all clients
- **Requests** (methods returning `CompletableFuture<T>`) → sent to first client only
- **Auto-cleanup** - clients that disconnect (IOException) are automatically removed

**Created automatically:**
```java
// When server calls:
server.getClient().publishDiagnostics(...);
// → All connected clients receive the notification
```

### 3. **SecondaryClientServerWrapper**

Wrapper for secondary clients that intercepts lifecycle methods.

**Behavior:**
- `initialize()` → returns cached `InitializeResult` from first client
- `initialized()` → no-op
- `shutdown()` → no-op (doesn't shutdown the shared server)
- `exit()` → no-op
- All other methods → delegated to the shared server

### 4. **LspInstanceRegistry**

Manages instance files for service discovery.

**File location:** `${workspace}/.lsp-servers/{serverType}.json`

**File format:**
```json
{
  "port": 54321,
  "pid": 12345
}
```

**Usage:**
```java
InstancePaths paths = InstancePaths.builder()
    .serverType("lemminx")
    .workspacePath("/path/to/workspace")
    .build();

// Register instance
LspInstanceRegistry.registerInstance(paths, port);

// Cleanup on shutdown
LspInstanceRegistry.unregisterInstance(paths);
```

### 5. **InstancePaths**

Pre-computes file paths to avoid duplication.

**Usage:**
```java
InstancePaths paths = InstancePaths.builder()
    .serverType("lemminx")
    .workspacePath("/path/to/workspace")
    .build();

Path instanceFile = paths.getInstanceFilePath();
// → /path/to/workspace/.lsp-servers/lemminx.json
```

## How It Works

### Primary Client Connects (stdio)

1. `MultiClientLauncher.Builder` creates the server
2. Wraps server with `InitializeInterceptor` to capture `InitializeResult`
3. Creates `MultiClientProxy` for broadcasting
4. Primary client connects via stdio, calls `initialize()`
5. Server initializes normally, result is cached
6. Primary client added to `MultiClientProxy`
7. Socket listener starts in background

### Secondary Clients Connect (socket)

1. Client discovers instance via `${workspace}/.lsp-servers/lemminx.json`
2. Client connects to socket
3. Server wraps itself with `SecondaryClientServerWrapper`
4. Client calls `initialize()` → wrapper returns **cached result**
5. Client added to `MultiClientProxy`
6. All requests go to shared server

### Notifications Flow

```
Server: client.publishDiagnostics(...)
           ↓
    MultiClientProxy
           ↓
   ┌───────┼───────┐
   ▼       ▼       ▼
Primary  Client2  Client3
(stdio)  (socket) (socket)
```

### Client Disconnection

When a client disconnects:
1. Next broadcast attempt throws `IOException`
2. `MultiClientProxy` detects connection error
3. Client automatically removed from broadcast list
4. Server continues serving remaining clients

## Use Cases

### 1. IDE + MCP Server Co-existence

A user has VS Code with an XML extension open, and simultaneously uses Claude Code with an MCP server that needs XML language intelligence.

**Without multi-client:**
- VS Code launches lemminx process #1
- MCP server launches lemminx process #2
- 2× memory, 2× indexing, 2× CPU

**With multi-client:**
- VS Code launches lemminx in multi-client mode
- MCP server connects to the same lemminx instance
- Shared memory, shared index, shared cache

### 2. Multi-Workspace Scenarios

Multiple workspace folders, all needing the same language server.

### 3. Testing and Development

Attach a test client to a running server without disrupting the main client.

## Integration Example: lemminx

### Server Side (XMLServerLauncher.java)

```java
public static void main(String[] args) {
    boolean mcpEnabled = hasArg("--mcp-enabled", args);
    String workspacePath = getArg("--workspace", args);
    int socketPort = getIntArg("--port", args, 0);  // 0 = free port
    String clientName = getArg("--client-name", args);
    String clientVersion = getArg("--client-version", args);

    XMLLanguageServer server = new XMLLanguageServer();
    
    if (mcpEnabled && workspacePath != null) {
        // Multi-client mode with socket
        MultiClientLauncher.Builder<LanguageClient> builder = new MultiClientLauncher.Builder<LanguageClient>()
            .setLocalService(server)
            .setRemoteInterface(XMLLanguageClientAPI.class)
            .setInput(System.in)
            .setOutput(System.out)
            .setExecutorService(Executors.newCachedThreadPool())
            .wrapMessages(new ParentProcessWatcher(server))
            .enableSocket(socketPort, workspacePath, "lemminx");
        
        // Add client info if provided (optional)
        if (clientName != null || clientVersion != null) {
            builder.setClientInfo(clientName, clientVersion);
        }
        
        Launcher<LanguageClient> launcher = builder.create();
        server.setClient(launcher.getRemoteProxy());
        launcher.startListening();
    } else {
        // Normal stdio-only mode
        launch(System.in, System.out);
    }
}
```

### Client Side: vscode-xml

#### Configuration

Add to VS Code settings:
```json
{
  "xml.mcp.enabled": true
}
```

#### What happens

When `xml.mcp.enabled: true`:
1. vscode-xml launches lemminx with arguments:
   ```bash
   java -jar lemminx.jar \
     --mcp-enabled \
     --workspace /path/to/workspace \
     --port 0 \
     --client-name "VS Code" \
     --client-version "1.95.0"
   ```
2. lemminx starts in multi-client mode:
   - Primary client: vscode-xml via stdio
   - Socket listener: accepts secondary clients
3. Instance file created: `${workspace}/.lsp-servers/lemminx.json`
   ```json
   {
     "port": 54321,
     "pid": 12345,
     "clientName": "VS Code",
     "clientVersion": "1.95.0"
   }
   ```
4. MCP servers can now discover and connect to this instance

#### TypeScript Implementation (javaServerStarter.ts)

```typescript
export async function prepareJavaExecutable(
  context: ExtensionContext,
  requirements: RequirementsData,
  xmlJavaExtensions: string[]
): Promise<ServerOptions> {

  const mcpEnabled = getXMLConfiguration().get('mcp.enabled', false);
  const workspacePath = workspace.workspaceFolders?.[0]?.uri.fsPath || '';

  return {
    command: path.resolve(requirements.java_home + '/bin/java'),
    args: prepareParams(requirements, xmlJavaExtensions, context, mcpEnabled, workspacePath)
  };
}

function prepareParams(..., mcpEnabled?: boolean, workspacePath?: string): string[] {
  // ... build classpath and JVM args ...
  
  params.push('org.eclipse.lemminx.XMLServerLauncher');
  
  // Add MCP arguments if enabled
  if (mcpEnabled && workspacePath) {
    params.push('--mcp-enabled');
    params.push('--workspace'); 
    params.push(workspacePath);
    
    // Optional: add client info for better instance tracking
    params.push('--client-name');
    params.push('VS Code');
    params.push('--client-version');
    params.push(vscode.version);
  }
  
  return params;
}
```

### Client Side: MCP Server

```java
// Try to find existing instance
String workspacePath = Paths.get(workspaceRoot).toString();
InstanceRegistry.InstanceInfo instance = 
    LspInstanceRegistry.findInstance(workspacePath, "lemminx");

if (instance != null) {
    // Connect to existing instance
    Socket socket = new Socket("localhost", instance.port);
    Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(
        client,
        socket.getInputStream(),
        socket.getOutputStream(),
        executorService,
        wrapper
    );
    languageServer = launcher.getRemoteProxy();
    launcher.startListening();
} else {
    // Launch own standalone instance
    launchStandaloneServer();
}
```

## Requirements

- **Java 11+** (for `ProcessHandle.current().pid()`)
- **Eclipse LSP4J 0.21+**

## License

Eclipse Public License v2.0 (EPL-2.0)

## Contributing

This package is designed to be **generic and reusable**. If you use it with a different language server (JDT.LS, typescript-language-server, etc.), please contribute improvements and documentation!

## See Also

- [Eclipse LSP4J](https://github.com/eclipse/lsp4j)
- [Language Server Protocol Specification](https://microsoft.github.io/language-server-protocol/)
- [lemminx (XML Language Server)](https://github.com/eclipse/lemminx)
- [vscode-xml (VS Code XML extension)](https://github.com/redhat-developer/vscode-xml)
