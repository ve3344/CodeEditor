package com.ve.view.editor;

import com.ve.view.listener.RowListener;
import com.ve.view.listener.SelectionListener;
import com.ve.view.listener.TextListener;

public class ListenManager extends Base implements RowListener, SelectionListener, TextListener {
    private RowListener rowListener;
    private SelectionListener selectionChangedListener;
    private TextListener textListener;

    public ListenManager(Editor editor) {
        super(editor);
        rowListener = new RowListener.RowAdapter();
        selectionChangedListener = new SelectionListener.SelectionAdapter();
        textListener = new TextListener.TextAdapter();
    }

    @Override
    public void onRowChange(int row) {
        if (rowListener != null) {
            rowListener.onRowChange(row);
        }
    }

    @Override
    public void onSelectionChanged(boolean select, int selectionStart, int selectionEnd) {
        selectionChangedListener.onSelectionChanged(select,selectionStart,selectionEnd);
    }


    @Override
    public void onNewLine( int caretPosition, int p2) {
        textListener.onNewLine( caretPosition, p2);
    }

    @Override
    public void onDelete(int caretPosition, int newCursorPosition) {
        textListener.onDelete( caretPosition, newCursorPosition);
    }

    @Override
    public void onAdd(CharSequence text, int caretPosition, int newCursorPosition) {
        textListener.onAdd(text, caretPosition, newCursorPosition);
    }

    public void setRowListener(RowListener rowListener) {
        this.rowListener = rowListener;
    }

    public void setSelectionChangedListener(SelectionListener selectionChangedListener) {
        this.selectionChangedListener = selectionChangedListener;
    }

    public void setTextListener(TextListener textListener) {
        this.textListener = textListener;
    }





    public int getCaretY() {
        return caret.y;
    }

    public int getCaretX() {
        return caret.x;
    }






}
