package org.eclipse.lemminx.commons;

import java.util.List;

@FunctionalInterface
public interface ModelUpdater<T> {

	void updateModel(T model, List<TextDocumentChange> changes, String oldText);
}
