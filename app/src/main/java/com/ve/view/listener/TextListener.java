package com.ve.view.listener;

public interface TextListener {
    void onNewLine(int caretPosition, int p2);

    void onDelete(int caretPosition, int newCursorPosition);

    void onAdd(CharSequence text, int caretPosition, int newCursorPosition);

    class TextAdapter implements TextListener {
        @Override
        public void onNewLine(int caretPosition, int p2) {

        }

        @Override
        public void onDelete(int caretPosition, int newCursorPosition) {

        }

        @Override
        public void onAdd(CharSequence text, int caretPosition, int newCursorPosition) {

        }
    }
}
