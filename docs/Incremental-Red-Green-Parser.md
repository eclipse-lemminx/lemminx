# Incremental Red-Green Parser

LemMinX uses a **Roslyn-style red-green tree** architecture for its XML DOM parser. This design enables **incremental reparsing**: when a user edits a document, only the affected region is reparsed, while unchanged subtrees are reused from the previous parse. This makes editing large XML files (100k+ lines) feel instantaneous.

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [Green Tree (Immutable Layer)](#green-tree-immutable-layer)
- [Red Tree (Mutable Facade)](#red-tree-mutable-facade)
- [GreenTreeBuilder](#greentreebuilder)
- [RedTreeBuilder](#redtreebuilder)
- [Incremental Parsing](#incremental-parsing)
  - [Algorithm Overview](#algorithm-overview)
  - [Prefix/Suffix Detection](#prefixsuffix-detection)
  - [Recursive Descent](#recursive-descent)
  - [Text Coalescing](#text-coalescing)
  - [Fallback to Full Parse](#fallback-to-full-parse)
- [Integration with LSP](#integration-with-lsp)
  - [ModelTextDocument](#modeltextdocument)
  - [Atomic Swap (volatile)](#atomic-swap-volatile)
  - [Parse Lambda](#parse-lambda)
- [Performance Characteristics](#performance-characteristics)
- [Limitations](#limitations)
- [Node Types](#node-types)

---

## Architecture Overview

The parser pipeline has three stages:

```
  XML text
     |
     v
 GreenTreeBuilder ──► GreenDocument (immutable, width-based)
     |
     v
 RedTreeBuilder   ──► DOMDocument  (mutable, absolute offsets, parent pointers)
     |
     v
  LSP features (completion, validation, hover, ...)
```

On subsequent edits, the `IncrementalParser` replaces the first stage:

```
  Old GreenDocument + edit info + new text
     |
     v
 IncrementalParser ──► New GreenDocument (shares unchanged subtrees)
     |
     v
 RedTreeBuilder    ──► New DOMDocument
```

The key insight from the [Roslyn compiler](https://github.com/dotnet/roslyn) is to split the tree into two layers:

| Layer | Offsets | Parent pointers | Mutability | Sharing |
|-------|---------|-----------------|------------|---------|
| **Green** (syntax) | Relative (widths) | No | Immutable | Yes - across versions |
| **Red** (semantic) | Absolute | Yes | Mutable | No - rebuilt each time |

Because green nodes store **widths** (relative offsets) rather than absolute positions, two structurally identical subtrees at different document positions use the exact same green node objects.

---

## Green Tree (Immutable Layer)

All green nodes extend `GreenNode`:

```
GreenNode (abstract)
├── width: int         — total span in characters
├── closed: boolean    — whether this node was properly closed
├── children(): GreenNode[]
├── childrenStartRel(): int  — offset from node start where children begin
└── nodeType(): short
```

### Green Node Types

| Class | DOM Node Type | Description |
|-------|--------------|-------------|
| `GreenDocument` | `DOCUMENT_NODE` | Root, width = text length |
| `GreenElement` | `ELEMENT_NODE` | Tag, attributes, children |
| `GreenText` | `TEXT_NODE` | Text content, tracks `whitespace` flag |
| `GreenComment` | `COMMENT_NODE` | `<!-- ... -->` |
| `GreenCDATA` | `CDATA_SECTION_NODE` | `<![CDATA[ ... ]]>` |
| `GreenProcessingInstruction` | `PROCESSING_INSTRUCTION_NODE` | `<?target ...?>`, prolog |
| `GreenDocumentType` | `DOCUMENT_TYPE_NODE` | `<!DOCTYPE ...>` |
| `GreenDTDElementDecl` | `DTD_ELEMENT_DECL_NODE` | `<!ELEMENT ...>` |
| `GreenDTDAttlistDecl` | `DTD_ATT_LIST_NODE` | `<!ATTLIST ...>` |
| `GreenDTDEntityDecl` | `ENTITY_NODE` | `<!ENTITY ...>` |
| `GreenDTDNotationDecl` | `DTD_NOTATION_DECL` | `<!NOTATION ...>` |

### GreenElement Fields

All offsets are **relative to the element's own start** (offset 0 is the first `<`):

```java
tag: String                  // "div", "root", etc.
selfClosed: boolean          // true for <br/>
startTagCloseRel: int        // offset of '>' in start tag
endTagOpenRel: int           // offset of '<' in </tag>
endTagCloseRel: int          // offset of '>' in </tag>
contentStartRel: int         // offset where first child content starts
attributes: GreenAttr[]
children: GreenNode[]
```

Example for `<root><a/><b/></root>` (width = 21):
```
startTagCloseRel = 5       (the '>' after "root")
endTagOpenRel = 14         (the '<' in "</root>")
endTagCloseRel = 20        (the '>' in "</root>")
contentStartRel = 6        (start of first child)
children = [GreenElement("a", w=4), GreenElement("b", w=4)]
```

### GreenElement.withNewChildren()

Creates a new element node with different children, adjusting end-tag offsets by the width delta:

```java
public GreenElement withNewChildren(GreenNode[] newChildren, int widthDelta) {
    return new GreenElement(
            width() + widthDelta, closed(), tag, selfClosed,
            startTagCloseRel,
            endTagOpenRel != NULL_VALUE ? endTagOpenRel + widthDelta : NULL_VALUE,
            endTagCloseRel != NULL_VALUE ? endTagCloseRel + widthDelta : NULL_VALUE,
            contentStartRel, attributes, newChildren);
}
```

The start tag is unchanged (same `startTagCloseRel`, same `attributes`), but the end tag shifts by `delta` because the content area grew or shrank.

### GreenAttr

Each attribute stores name/value offsets relative to its owning element:

```java
nameStartRel, nameEndRel       // "class" in class="foo"
delimiterRel                   // the '='
valueStartRel, valueEndRel     // "\"foo\"" (including quotes)
```

---

## Red Tree (Mutable Facade)

The **red tree** is the existing `DOMDocument` / `DOMNode` / `DOMElement` hierarchy that all LemMinX features depend on. It provides:

- **Absolute offsets** (`getStart()`, `getEnd()`)
- **Parent pointers** (`getParentNode()`)
- **W3C DOM API** (`getChildNodes()`, `getAttributes()`, etc.)

The red tree is built fresh from the green tree on every parse, but this is fast because `RedTreeBuilder` only walks the tree once, computing absolute offsets on the fly.

---

## GreenTreeBuilder

`GreenTreeBuilder.parse(text, uri, monitor)` produces a `GreenDocument` from XML text.

It uses the existing `XMLScanner` (char-by-char, no regex, no substring) and mirrors the token-handling logic of the original `DOMParser`, but builds `GreenNode` objects instead of `DOMNode` objects.

### parseRange()

```java
GreenTreeBuilder.parseRange(text, uri, rangeStart, rangeEnd, monitor)
```

Parses a **sub-range** of the document text. The scanner starts at `rangeStart` and stops when it reaches `rangeEnd` with an empty stack. This is the building block for incremental parsing: only the affected range is re-scanned.

### NodeBuilder

Internally, `GreenTreeBuilder` uses `NodeBuilder` — a mutable scratch object that accumulates scanner tokens and converts them into an immutable `GreenNode` via `buildGreen()`. Each `NodeBuilder` tracks:

- Tag name, attributes, children
- Absolute offsets (converted to relative during `buildGreen()`)
- DTD-specific fields (element decls, attlists, entities, notations)

---

## RedTreeBuilder

`RedTreeBuilder.build(greenDoc, textDocument, resolverExtensionManager)` walks the green tree recursively, creating the corresponding red (`DOMNode`) tree.

For each green node, it:
1. Computes absolute offsets: `absStart + relativeOffset`
2. Creates the corresponding `DOMNode` subclass
3. Copies fields (tag, attributes, content offsets)
4. Recursively processes children, advancing `childAbsStart` by each child's width

Whitespace-only text nodes between elements are skipped when `ignoreWhitespaceContent` is true (the default), matching the original parser's behavior.

---

## Incremental Parsing

The `IncrementalParser` is the heart of the performance optimization. Given an old `GreenDocument`, the new text, and edit coordinates, it produces a new `GreenDocument` that structurally shares unchanged subtrees with the old one.

### Algorithm Overview

```
incrementalParse(oldDoc, newText, editStart, deleteLength, insertLength)
  │
  ├─ tryIncrementalOnChildren(oldDoc.children, ...)
  │   │
  │   ├─ Find prefix: children entirely before the edit
  │   ├─ Find suffix: children entirely after the edit
  │   │
  │   ├─ If exactly 1 middle child is a GreenElement with children:
  │   │   └─ tryDescentIntoElement() → recursive descent
  │   │
  │   ├─ If no sharing possible (prefix + suffix == 0):
  │   │   └─ return null (fallback to full parse)
  │   │
  │   └─ parseRange the middle region
  │       ├─ If last reparsed child is unclosed: extend to include suffix
  │       └─ splice(prefix + middle + suffix) → coalesceAdjacentText()
  │
  └─ If result is null: GreenTreeBuilder.parse() (full reparse)
```

### Prefix/Suffix Detection

The algorithm scans children left-to-right to find the **prefix** — consecutive children whose end offset falls before the edit start — and right-to-left for the **suffix** — children whose start offset falls after the edit end.

```
Document children: [PI] [Text] [Element-A] [Element-B] [Element-C]
                    ^^^   ^^^                             ^^^^^^^^^
                    prefix (2)                            suffix (1)
                              ^^^^^^^^^^^^  ^^^^^^^^^^^^
                              middle (2) — must be reparsed
```

Prefix and suffix children are **reused as-is** from the old tree. Only the middle region is reparsed via `GreenTreeBuilder.parseRange()`.

### Recursive Descent

When exactly **one child** contains the edit and that child is a `GreenElement` with sub-children, the parser **descends into it** rather than reparsing the entire element.

This is critical for the common XML pattern where a single root element wraps thousands of children:

```xml
<catalog>                    ← root element (1 child of document)
  <product id="1">...</product>
  <product id="2">...</product>   ← edit is here
  <product id="3">...</product>
  ...
  <product id="24863">...</product>
</catalog>
```

Without recursive descent, the entire `<catalog>` element (and all 24,863 products) would be reparsed. With descent, the algorithm enters `<catalog>`, finds 24,862 products as prefix/suffix, and reparses only the one affected product.

The descent algorithm in `tryDescentIntoElement()`:

1. Check that the edit is entirely within the element's **children area** (not in the start tag or end tag). If the edit touches an attribute or tag name, descent is not safe.
2. Recursively call `tryIncrementalOnChildren()` on the element's children.
3. If successful, return `elem.withNewChildren(newChildren, delta)` — a new `GreenElement` with the same tag, attributes, and start tag, but different children and adjusted end-tag offsets.

Descent can be **multi-level**: if the element's single middle child is itself an element with children, the algorithm descends again. This handles arbitrarily deep nesting.

### Text Coalescing

After splicing prefix + reparsed middle + suffix, adjacent `GreenText` nodes may appear at the boundaries. For example:

```
Old children: [...] [Text "hello world"] [...]
                          ^^^^^
                          edit here
After splice:  [Text "hello "] [Text "new"] [Text " world"]
                ^^ prefix text   ^^ middle    ^^ suffix text
```

A full parse would produce a single `GreenText` for the combined content. To ensure the incremental result matches the full-parse structure, `coalesceAdjacentText()` merges adjacent `GreenText` nodes:

```java
[Text "hello "] [Text "new"] [Text " world"]  →  [Text "hello new world"]
```

The merged node's `whitespace` flag is `true` only if **all** merged nodes were whitespace.

### Fallback to Full Parse

The incremental parser falls back to a full reparse when:

- **No structural sharing is possible**: `prefixCount + suffixCount == 0` at the document level
- **The reparsed middle ends with an unclosed element**: this invalidates the suffix (the unclosed element would absorb suffix nodes). The parser first tries extending the reparse range to include the suffix. If that also fails, it falls back to full parse.
- **The edit touches a tag name or attribute** of the single middle child: descent guard prevents corruption.

---

## Integration with LSP

### ModelTextDocument

`ModelTextDocument<DOMDocument>` extends `TextDocument` with three `volatile` fields for thread-safe incremental support:

```java
volatile DOMDocument model;          // current parsed model
volatile DOMDocument previousModel;  // model before the last edit
volatile EditInfo pendingEdit;       // edit coordinates
```

The `update()` method is overridden to capture `EditInfo` **before** the text is changed (because offsets refer to the old text):

```java
@Override
public void update(List<TextDocumentContentChangeEvent> changes) {
    // Capture edit info BEFORE super.update() changes the text
    if (changes != null && changes.size() == 1) {
        Range range = changes.get(0).getRange();
        if (range != null) {
            int start = offsetAt(range.getStart());
            pendingEdit = new EditInfo(start, deleteLength, insertLength);
        }
    } else {
        pendingEdit = null;  // multiple changes → can't incrementally parse
    }
    super.update(changes);  // applies the text change
}
```

### Atomic Swap (volatile)

The `volatile` keyword on `model`, `previousModel`, and `pendingEdit` ensures that:

- The parsing thread sees the latest `previousModel` and `pendingEdit` set by the LSP event thread
- The LSP event thread sees the latest `model` set by the parsing thread

The `cancelModel()` method atomically transitions the model state:

```java
private void cancelModel() {
    if (model != null) {
        previousModel = model;  // save current model for incremental use
    }
    model = null;  // mark as dirty
}
```

The `DOMDocument` also stores its green tree via `volatile GreenDocument greenDocument`, so the incremental parser can access the previous green tree.

### Parse Lambda

In `XMLTextDocumentService`, the parse lambda checks for the incremental path:

```java
DOMDocument prev = mtd.getPreviousModel();
ModelTextDocument.EditInfo editInfo = mtd.getPendingEdit();
if (prev != null && prev.getGreenDocument() != null && editInfo != null) {
    return parser.parseIncremental(document,
            prev.getGreenDocument(),
            editInfo.getStartOffset(),
            editInfo.getDeleteLength(),
            editInfo.getInsertLength(),
            resolverExtensionManager,
            true /* ignoreWhitespaceContent */, cancelChecker);
}
// fallback: full parse
return parser.parse(document, resolverExtensionManager, true, cancelChecker);
```

The incremental path is taken when all three conditions are met:
1. A previous model exists (the document was parsed before)
2. The previous model has a green tree
3. A single-change edit info is available

If any condition is missing (first parse, multiple simultaneous changes, full-document replacement), the standard full-parse path is taken.

### Edit Flow

The complete sequence for a `textDocument/didChange` notification:

```
1. setVersion(newVersion)      → cancelModel()  → previousModel = model; model = null
2. update(changes)             → captures EditInfo before super.update()
                               → super.update() modifies the text
3. (later) getModel()          → model is null → getSynchronizedModel()
4. getSynchronizedModel()      → parse lambda runs
   a. Checks previousModel, pendingEdit, greenDocument
   b. If available: IncrementalParser.incrementalParse()
   c. Else: GreenTreeBuilder.parse() (full)
   d. RedTreeBuilder.build() (in both cases)
   e. model = result; previousModel = null; pendingEdit = null
```

---

## Performance Characteristics

### Time Complexity

| Operation | Full Parse | Incremental Parse |
|-----------|-----------|-------------------|
| GreenTreeBuilder | O(n) | O(m) where m = middle range size |
| RedTreeBuilder | O(n) | O(n) — always walks full tree |
| IncrementalParser | — | O(k) where k = children count at each level |

For a 600,000-line file with a single-character edit in one element:
- **Full parse**: scans all 600,000 lines
- **Incremental parse with descent**: scans only the ~25 lines of the affected element, plus O(log depth) work for descent

### Memory

Green nodes are shared between versions via structural sharing. For a single-character edit in a 24,863-element document, only the modified element and its ancestors are new allocations — the other 24,862 elements are the exact same Java objects.

### Scanner Performance

The `XMLScanner` and `GreenTreeBuilder` use only `charAt()` for character-by-character scanning. No `substring()`, no regex, no string allocation in the hot path. This is critical for handling multi-MB files without GC pressure.

---

## Limitations

1. **The red tree is always fully rebuilt.** `RedTreeBuilder` walks the entire green tree on every parse. For very large files, this is the bottleneck rather than the scanner. A future optimization could cache and reuse red subtrees that map to unchanged green subtrees.

2. **Multi-change edits bypass incremental parsing.** If `textDocument/didChange` contains more than one `TextDocumentContentChangeEvent`, the incremental path is skipped (full reparse). This is uncommon in practice — most editors send single-change events.

3. **Full-document replacements bypass incremental parsing.** A `TextDocumentContentChangeEvent` without a `Range` replaces the entire document. This cannot be incrementally parsed.

4. **Edits that touch element structure fall back to broader reparsing.** If an edit changes a tag name or removes a closing tag, the incremental parser correctly detects that the reparsed middle is unclosed and extends the reparse range or falls back entirely.

5. **DTD internal subsets are not incrementally parsed.** The `GreenDocumentType` node is treated as atomic — any edit within a `<!DOCTYPE [...]>` block reparses the entire doctype.

---

## Node Types

### Class Diagram

```
GreenNode (abstract)
├── GreenDocument
├── GreenElement
├── GreenText
├── GreenComment
├── GreenCDATA
├── GreenProcessingInstruction
├── GreenDocumentType
│   └── (DTD nodes as children)
├── GreenDTDDeclNode
│   ├── GreenDTDElementDecl
│   ├── GreenDTDAttlistDecl
│   ├── GreenDTDEntityDecl
│   └── GreenDTDNotationDecl
├── GreenAttr (not a GreenNode — stored in GreenElement.attributes[])
└── GreenDTDParam (not a GreenNode — stores DTD parameter offsets)
```

### File Locations

```
org.eclipse.lemminx/src/main/java/org/eclipse/lemminx/
├── dom/green/
│   ├── GreenNode.java              — abstract base, width/closed/children
│   ├── GreenDocument.java          — document root
│   ├── GreenElement.java           — XML element with tag, attrs, children
│   ├── GreenText.java              — text content
│   ├── GreenComment.java           — comment
│   ├── GreenCDATA.java             — CDATA section
│   ├── GreenProcessingInstruction.java — PI and prolog
│   ├── GreenDocumentType.java      — DOCTYPE
│   ├── GreenDTDDeclNode.java       — base for DTD declarations
│   ├── GreenDTDElementDecl.java    — <!ELEMENT>
│   ├── GreenDTDAttlistDecl.java    — <!ATTLIST>
│   ├── GreenDTDEntityDecl.java     — <!ENTITY>
│   ├── GreenDTDNotationDecl.java   — <!NOTATION>
│   ├── GreenAttr.java              — attribute (name/value offsets)
│   ├── GreenDTDParam.java          — DTD parameter offsets
│   ├── GreenTreeBuilder.java       — scanner → green tree
│   └── IncrementalParser.java      — old green + edit → new green
├── dom/
│   ├── DOMParser.java              — entry point (parse + parseIncremental)
│   └── RedTreeBuilder.java         — green tree → DOMDocument
└── commons/
    └── ModelTextDocument.java       — volatile model + EditInfo capture
```

### Test Locations

```
org.eclipse.lemminx/src/test/java/org/eclipse/lemminx/
├── dom/green/
│   ├── GreenTreeBuilderTest.java   — 37 tests (green tree correctness)
│   ├── RedTreeBuilderTest.java     — 77 tests (green→red equivalence)
│   └── IncrementalParserTest.java  — 59 tests (incremental correctness)
└── commons/
    └── ModelTextDocumentTest.java   — 6 tests (EditInfo, volatile state)
```
