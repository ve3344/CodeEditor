package com.ve.view.text.undo;

public interface Undo {
    int undo();

    int redo();

    boolean canUndo();

    boolean canRedo();

    boolean isBatchEdit();

    void beginBatchEdit();

    void endBatchEdit();
}
