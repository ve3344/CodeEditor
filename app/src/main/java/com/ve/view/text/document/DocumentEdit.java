package com.ve.view.text.document;

public interface DocumentEdit {


    void insertBefore(char c, int insertionPoint, long timestamp);

    void insertBefore(char[] cArray, int insertionPoint, long timestamp);

    void deleteAt(int deletionPoint, long timestamp);

    void deleteAt(int deletionPoint, int maxChars, long time);
}
