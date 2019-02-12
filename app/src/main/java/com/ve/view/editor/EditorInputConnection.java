package com.ve.view.editor;

import android.content.Context;
import android.view.KeyEvent;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.InputMethodManager;

import com.ve.view.utils.EditorException;
import com.ve.view.text.document.Document;

public class EditorInputConnection extends BaseInputConnection {

    protected int composingLength = 0;
    protected Editor editor;
    protected Document document;
    protected Caret caret;
    protected TextController controller;

    public EditorInputConnection(Editor editor) {
        super(editor, true);
        this.editor = editor;
    }

    public void init() {
        this.document = editor.document;
        this.caret = editor.caret;
        this.controller = editor.controller;
    }

    public void reset() {
        composingLength = 0;
        document.endBatchEdit();
    }

    public boolean isComposing() {
        return composingLength == 0;
    }

    public void stopTextComposing() {
        InputMethodManager im = (InputMethodManager) editor.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        im.restartInput(editor);

        if (isComposing()) {
            reset();
        }
    }


    @Override
    public boolean performContextMenuAction(int id) {
        switch (id) {
            case android.R.id.copy:
                editor.getTextInterface().copy();
                break;
            case android.R.id.cut:
                editor.getTextInterface().cut();
                break;
            case android.R.id.paste:
                editor.getTextInterface().paste();
                break;
            case android.R.id.startSelectingText:
            case android.R.id.stopSelectingText:
            case android.R.id.selectAll:
                editor.selectInterface.selectAll();
                break;
        }

        return false;
    }

    @Override
    public boolean sendKeyEvent(KeyEvent event) {
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_SHIFT_LEFT:

                editor.selectInterface.select(!editor.selectInterface.isSelecting());
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                caret.moveLeft();
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                caret.moveUp();
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                caret.moveRight();
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                caret.moveDown();
                break;
            case KeyEvent.KEYCODE_MOVE_HOME:
                caret.moveToPosition(0);
                break;
            case KeyEvent.KEYCODE_MOVE_END:
                caret.moveToPosition(document.length() - 1);
                break;
            default:
                return super.sendKeyEvent(event);
        }
        return true;
    }


    @Override
    public boolean setComposingText(CharSequence text, int newCursorPosition) {
        if (!document.isBatchEdit()) {
            document.beginBatchEdit();
        }

        controller.replaceComposingText(caret.getPosition() - composingLength, composingLength, text.toString());
        composingLength = text.length();

        //TODO 减少重绘
        if (newCursorPosition > 1) {
            caret.moveToPosition(caret.position + newCursorPosition - 1);
        } else if (newCursorPosition <= 0) {
            caret.moveToPosition(caret.position - text.length() - newCursorPosition);
        }
        return true;
    }

    @Override
    public boolean commitText(CharSequence text, int newCursorPosition) {
        controller.replaceComposingText(caret.getPosition() - composingLength, composingLength, text.toString());
        composingLength = 0;
        document.endBatchEdit();

        //TODO 减少重绘
        if (newCursorPosition > 1) {
            caret.moveToPosition(caret.position + newCursorPosition - 1);
        } else if (newCursorPosition <= 0) {
            caret.moveToPosition(caret.position - text.length() - newCursorPosition);
        }

        return true;
    }


    @Override
    public boolean deleteSurroundingText(int leftLength, int rightLength) {
        EditorException.logIf(composingLength > 0, "deleteSurroundingText composingLength != 0");

        controller.deleteAroundComposingText(leftLength, rightLength);
        return true;
    }

    @Override
    public boolean finishComposingText() {
        reset();
        return true;
    }


    @Override
    public CharSequence getTextAfterCursor(int maxLen, int flags) {
        return controller.getTextAfterCursor(maxLen);
    }

    @Override
    public CharSequence getTextBeforeCursor(int maxLen, int flags) {
        return controller.getTextBeforeCursor(maxLen);
    }

    @Override
    public boolean setSelection(int start, int end) {
        if (start == end) {
            caret.moveToPosition(start);
        } else {
            controller.setSelectionRange(start, end - start, false);
        }
        return true;
    }

}