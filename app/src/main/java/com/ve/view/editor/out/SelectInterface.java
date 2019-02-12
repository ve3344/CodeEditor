package com.ve.view.editor.out;

public interface SelectInterface {
    boolean isSelecting();

    void cancelSelect();

    void select(boolean select);

    void selectAll();

    void select(int position, int count);

    boolean inSelectionRange(int position);

    int getSelectionStart();

    int getSelectionEnd();
/*    boolean isSelecting();
    int getSelectionStart();
    int getSelectionEnd();
    void setSelection(int start, int end);
    String getSelectionText();
    void cancelSelection();*/

}
