package com.ve.view.text.undo;

public interface Undoable {
    String subSequence(int start, int length);

    void shiftGapStart(int distance);

    void delete(int start, int length, long timestamp, boolean undoable);

    void insert(char[] chars, int start, long timestamp, boolean undoable);

    char[] subEditingSequence(int length);
}
