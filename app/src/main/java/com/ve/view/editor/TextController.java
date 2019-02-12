package com.ve.view.editor;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.util.Log;
import android.view.KeyEvent;

import com.ve.view.utils.EditorException;
import com.ve.view.text.document.CommonLanguage;


public class TextController extends Base  {
    private static final String TAG = TextController.class.getSimpleName();

    protected boolean selecting;
    protected int selectionStart = -1, selectionEnd = -1;


    public TextController(Editor editor) {
        super(editor);
    }





    public boolean isSelecting() {
        return selecting;
    }



    public void setSelectText(boolean select) {
        if (select == selecting) {
            return;
        }

        if (select) {
            this.selectionStart = caret.position;
            this.selectionEnd = caret.position;
        } else {
            this.selectionStart = -1;
            this.selectionEnd = -1;
        }
        selecting = select;
        operator.onSelectionChanged(select, selectionStart, selectionEnd);
    }

    public boolean inSelectionRange(int position) {
        return this.selectionStart >= 0 && (this.selectionStart <= position && position < this.selectionEnd);
    }


    public void setSelectionRange(int start, int count, boolean scrollToStart) {
        EditorException.logIf(start < 0 || start + count >= document.length() || count < 0, "Invalid range to select");

        if (selecting) {
            editor.invalidateSelectionRows();
        } else {
            editor.invalidateCaretRow();
            setSelectText(true);

        }

        this.selectionStart = start;
        this.selectionEnd = this.selectionStart + count;

        caret.position = this.selectionEnd;
        inputConnection.stopTextComposing();
        caret.updateRow();
        operator.onSelectionChanged(isSelecting(), this.selectionStart, this.selectionEnd);

        boolean scrolled = editor.displayInterface.scrollToPosition(this.selectionEnd);

        if (scrollToStart) {
            scrolled = editor.displayInterface.scrollToPosition(this.selectionStart);
        }

        if (!scrolled) {
            editor.invalidateSelectionRows();
        }
    }

    public void focusSelection(boolean start) {
        if (selecting) {

            if (start && caret.position != this.selectionStart) {
                caret.updatePosition(this.selectionStart);

            } else if (!start && caret.position != this.selectionEnd) {
                caret.updatePosition(this.selectionEnd);
            }
        }
    }


    public void updateSelectionRange(int oldCaretPosition, int newCaretPosition) {
        if (!selecting) {
            return;
        }

        if (oldCaretPosition < this.selectionEnd) {
            if (newCaretPosition > this.selectionEnd) {
                this.selectionStart = this.selectionEnd;
                this.selectionEnd = newCaretPosition;
            } else {
                this.selectionStart = newCaretPosition;
            }

        } else {
            if (newCaretPosition < this.selectionStart) {
                this.selectionEnd = this.selectionStart;
                this.selectionStart = newCaretPosition;
            } else {
                this.selectionEnd = newCaretPosition;
            }
        }
    }


    public void cut(ClipboardManager cb) {
        copy(cb);
        selectionDelete();
    }


    public void copy(ClipboardManager cb) {
        if (selecting) {
            cb.setPrimaryClip(ClipData.newPlainText("data", getSelectionText()));
        }
    }

    public CharSequence getSelectionText() {
        if (selecting && this.selectionStart < this.selectionEnd) {
            return document.subSequence(this.selectionStart, this.selectionEnd - this.selectionStart);
        } else {
            return null;
        }
    }


    public void paste(String text) {
        if (text == null) {
            return;
        }

        document.beginBatchEdit();
        selectionDelete();

        int oldRow = caret.row;
        int oldPosition = document.gainPositionOfRow(oldRow);
        document.insertBefore(text.toCharArray(), caret.position, System.nanoTime());
        operator.onAdd(text, caret.position, text.length());
        document.endBatchEdit();

        caret.position += text.length();
        caret.updateRow();

        editor.setEdited(true);
        spanManager.startSpan();
        inputConnection.stopTextComposing();

        if (!editor.displayInterface.scrollToPosition(caret.position)) {
            int invalidateStartRow = oldRow;
            //invalidate previous row too if its wrapping changed
            if (document.isWordWrapEnable() && oldPosition != document.gainPositionOfRow(oldRow)) {
                --invalidateStartRow;
            }

            if (oldRow == caret.row && !document.isWordWrapEnable()) {
                editor.invalidateRows(invalidateStartRow, invalidateStartRow + 1);
            } else {
                editor.invalidateFromRow(invalidateStartRow);
            }
        }
    }


    public void selectionDelete() {
        if (!selecting) {
            return;
        }

        int deleteCount = this.selectionEnd - this.selectionStart;

        if (deleteCount > 0) {
            int originalRow = document.gainRowOfPosition(this.selectionStart);
            int originalOffset = document.gainPositionOfRow(originalRow);
            boolean isSingleRowSel = document.gainRowOfPosition(this.selectionEnd) == originalRow;
            document.deleteAt(this.selectionStart, deleteCount, System.nanoTime());
            operator.onDelete( caret.position, deleteCount);
            caret.position = this.selectionStart;
            caret.updateRow();
            editor.setEdited(true);
            spanManager.startSpan();
            setSelectText(false);
            inputConnection.stopTextComposing();

            if (!editor.displayInterface.scrollToPosition(caret.position)) {
                int invalidateStartRow = originalRow;
                //invalidate previous row too if its wrapping changed
                if (document.isWordWrapEnable() &&
                        originalOffset != document.gainPositionOfRow(originalRow)) {
                    --invalidateStartRow;
                }

                if (isSingleRowSel && !document.isWordWrapEnable()) {
                    //pasted text only affects current row
                    editor.invalidateRow(invalidateStartRow);
                } else {
                    //TODO invalidate damaged rows only
                    editor.invalidateFromRow(invalidateStartRow);
                }
            }
        } else {
            setSelectText(false);
            editor.invalidateCaretRow();
        }
    }

    public void replaceText(int start, int count, String text) {
        int invalidateStartRow;
        int originalOffset;
        boolean isInvalidateSingleRow = true;
        boolean dirty = false;

        if (selecting) {
            invalidateStartRow = document.gainRowOfPosition(this.selectionStart);
            originalOffset = document.gainPositionOfRow(invalidateStartRow);

            int deleteCount = this.selectionEnd - this.selectionStart;

            if (deleteCount > 0) {
                caret.position = this.selectionStart;
                document.deleteAt(this.selectionStart, deleteCount, System.nanoTime());

                if (invalidateStartRow != caret.row) {
                    isInvalidateSingleRow = false;
                }
                dirty = true;
            }

            setSelectText(false);
        } else {
            invalidateStartRow = caret.row;
            originalOffset = document.gainPositionOfRow(invalidateStartRow);
        }

        //delete requested chars
        if (count > 0) {
            int delFromRow = document.gainRowOfPosition(start);
            if (delFromRow < invalidateStartRow) {
                invalidateStartRow = delFromRow;
                originalOffset = document.gainPositionOfRow(delFromRow);
            }

            if (invalidateStartRow != caret.row) {
                isInvalidateSingleRow = false;
            }

            caret.position = start;
            document.deleteAt(start, count, System.nanoTime());
            dirty = true;
        }

        //insert
        if (text != null && text.length() > 0) {
            int insFromRow = document.gainRowOfPosition(start);
            if (insFromRow < invalidateStartRow) {
                invalidateStartRow = insFromRow;
                originalOffset = document.gainPositionOfRow(insFromRow);
            }

            document.insertBefore(text.toCharArray(), caret.position, System.nanoTime());
            caret.position += text.length();
            dirty = true;
        }

        if (dirty) {
            editor.setEdited(true);
            spanManager.startSpan();
        }

        int originalRow = caret.row;
        caret.updateRow();
        if (originalRow != caret.row) {
            isInvalidateSingleRow = false;
        }

        if (!editor.displayInterface.scrollToPosition(caret.position)) {
            //invalidate previous row too if its wrapping changed
            if (document.isWordWrapEnable() &&
                    originalOffset != document.gainPositionOfRow(invalidateStartRow)) {
                --invalidateStartRow;
            }

            if (isInvalidateSingleRow && !document.isWordWrapEnable()) {
                //replaced text only affects current row
                editor.invalidateRows(caret.row, caret.row + 1);
            } else {
                //TODO invalidate damaged rows only
                editor.invalidateFromRow(invalidateStartRow);
            }
        }
    }




    //------for connection
    public void replaceComposingText(int from, int charCount, String text) {
        int invalidateStartRow, originalOffset;
        boolean isInvalidateSingleRow = true;
        boolean dirty = false;

        //delete selection
        if (selecting) {
            invalidateStartRow = document.gainRowOfPosition(this.selectionStart);
            originalOffset = document.gainPositionOfRow(invalidateStartRow);

            int totalChars = this.selectionEnd - this.selectionStart;

            if (totalChars > 0) {
                caret.position = this.selectionStart;
                document.deleteAt(this.selectionStart, totalChars, System.nanoTime());

                if (invalidateStartRow != caret.row) {
                    isInvalidateSingleRow = false;
                }
                dirty = true;
            }

            setSelectText(false);
        } else {
            invalidateStartRow = caret.row;
            originalOffset = document.gainPositionOfRow(caret.row);
        }

        //delete requested chars
        if (charCount > 0) {
            int delFromRow = document.gainRowOfPosition(from);
            if (delFromRow < invalidateStartRow) {
                invalidateStartRow = delFromRow;
                originalOffset = document.gainPositionOfRow(delFromRow);
            }

            if (invalidateStartRow != caret.row) {
                isInvalidateSingleRow = false;
            }

            caret.position = from;
            document.deleteAt(from, charCount, System.nanoTime());
            dirty = true;
        }

        //insert
        if (text != null && text.length() > 0) {
            int insFromRow = document.gainRowOfPosition(from);
            if (insFromRow < invalidateStartRow) {
                invalidateStartRow = insFromRow;
                originalOffset = document.gainPositionOfRow(insFromRow);
            }

            document.insertBefore(text.toCharArray(), caret.position, System.nanoTime());
            caret.position += text.length();
            dirty = true;
        }

        operator.onAdd(text, caret.position, text.length() - charCount);
        if (dirty) {
            editor.setEdited(true);
            spanManager.startSpan();
        }

        int originalRow = caret.row;
        caret.updateRow();
        if (originalRow != caret.row) {
            isInvalidateSingleRow = false;
        }

        if (!editor.displayInterface.scrollToPosition(caret.position)) {
            //invalidate previous row too if its wrapping changed
            if (document.isWordWrapEnable() &&
                    originalOffset != document.gainPositionOfRow(invalidateStartRow)) {
                --invalidateStartRow;
            }

            if (isInvalidateSingleRow && !document.isWordWrapEnable()) {
                //replaced text only affects current row
                editor.invalidateRows(caret.row, caret.row + 1);
            } else {
                //TODO invalidate damaged rows only
                editor.invalidateFromRow(invalidateStartRow);
            }
        }
    }


    public void deleteAroundComposingText(int left, int right) {
        int start = caret.position - left;
        if (start < 0) {
            start = 0;
        }
        int end = caret.position + right;
        int docLength = document.length();
        if (end > (docLength - 1)) { //exclude the terminal EOF
            end = docLength - 1;
        }
        replaceComposingText(start, end - start, "");
    }

    public String getTextAfterCursor(int maxLen) {
        int docLength = document.length();
        if ((caret.position + maxLen) > (docLength - 1)) {
            return document.subSequence(caret.position, docLength - caret.position - 1).toString();
        }

        return document.subSequence(caret.position, maxLen).toString();
    }

    public String getTextBeforeCursor(int maxLen) {
        int start = caret.position - maxLen;
        if (start < 0) {
            start = 0;
        }
        return document.subSequence(start, caret.position - start).toString();
    }

    public boolean handleKeyDown(int keyCode, KeyEvent event) {
        boolean isNavKey = false;
        char keyChar = CommonLanguage.NULL_CHAR;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                caret.moveRight();
                isNavKey = true;
                break;
            case KeyEvent.KEYCODE_DPAD_LEFT:
                caret.moveLeft();
                isNavKey = true;
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                caret.moveDown();
                isNavKey = true;
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                caret.moveUp();
                isNavKey = true;
                break;


            case KeyEvent.KEYCODE_ENTER:
                keyChar = CommonLanguage.NEWLINE;
                break;
            case KeyEvent.KEYCODE_SPACE:
                keyChar = event.isShiftPressed() ? CommonLanguage.TAB : CommonLanguage.SPACE;
                break;
            case KeyEvent.KEYCODE_TAB:
                keyChar = CommonLanguage.TAB;
                break;
            case KeyEvent.KEYCODE_DEL:
                keyChar = CommonLanguage.BACKSPACE;
                break;
            default:
                if (event.isPrintingKey()) {
                    keyChar = (char) event.getUnicodeChar(event.getMetaState());
                } else {
                    Log.w(TAG, "getChar: " + event.toString());
                }
                break;
        }


        if (isNavKey) {
            if (event.isShiftPressed() && !isSelecting()) {
                editor.invalidateCaretRow();
                setSelectText(true);
            } else if (!event.isShiftPressed() && isSelecting()) {
                editor.invalidateSelectionRows();
                setSelectText(false);
            }

            return true;
        }


        if (keyChar != CommonLanguage.NULL_CHAR && event.getRepeatCount() == 0) {
            onCharInput(keyChar);
            return true;
        }

        return false;

    }

    public void onCharInput(char c) {
        boolean selectionDeleted = false;
        if (selecting) {
            selectionDelete();
            selectionDeleted = true;
        }

        int originalRow = caret.row;
        int originalOffset = document.gainPositionOfRow(originalRow);

        switch (c) {
            case CommonLanguage.BACKSPACE:
                if (selectionDeleted) {
                    break;
                }

                if (caret.position > 0) {
                    document.deleteAt(caret.position - 1, System.nanoTime());
                    char prechar = document.charAt(caret.position - 2);
                    if (CommonLanguage.isEmoji(prechar)) {
                        document.deleteAt(caret.position - 2, System.nanoTime());
                        caret.moveLeft(true);
                    }

                    operator.onDelete( caret.position, 1);
                    caret.moveLeft(true);

                    if (caret.row < originalRow) {

                        editor.invalidateFromRow(caret.row);
                    } else if (document.isWordWrapEnable()) {
                        if (originalOffset != document.gainPositionOfRow(originalRow)) {
                            --originalRow;
                        }
                        //TODO invalidate damaged rows only
                        editor.invalidateFromRow(originalRow);
                    }
                }
                break;

            case CommonLanguage.NEWLINE:
                if (config.isAutoIndent) {
                    char[] indent = document.createAutoIndent(caret.position, config.autoIndentWidth);
                    document.insertBefore(indent, caret.position, System.nanoTime());
                    caret.moveToPosition(caret.position + indent.length);
                } else {
                    document.insertBefore(c, caret.position, System.nanoTime());
                    caret.moveRight(true);
                }

                if (document.isWordWrapEnable() && originalOffset != document.gainPositionOfRow(originalRow)) {
                    //invalidate previous row too if its wrapping changed
                    --originalRow;
                }

                operator.onNewLine( caret.position, 1);

                editor.invalidateFromRow(originalRow);
                break;

            default:
                document.insertBefore(c, caret.position, System.nanoTime());
                caret.moveRight(true);
                operator.onAdd(c + "", caret.position, 1);

                if (document.isWordWrapEnable()) {
                    if (originalOffset != document.gainPositionOfRow(originalRow)) {
                        //invalidate previous row too if its wrapping changed
                        --originalRow;
                    }
                    //TODO invalidate damaged rows only
                    editor.invalidateFromRow(originalRow);
                }
                break;
        }

        editor.setEdited(true);
        spanManager.startSpan();
    }


}

	
