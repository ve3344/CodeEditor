package com.ve.view.editor;

import android.view.MotionEvent;

import com.ve.view.utils.EditorException;
import com.ve.view.editor.out.CaretInterface;

public class Caret extends Base implements CaretInterface{
    protected final static long SCROLL_PERIOD = 250;

    protected final static int SCROLL_UP = 0;
    protected final static int SCROLL_DOWN = 1;
    protected final static int SCROLL_LEFT = 2;
    protected final static int SCROLL_RIGHT = 3;

    protected static final int SCROLL_EDGE = 10;

    protected int position = 0, row = 0;
    protected int x, y;

    protected boolean touching;

    public Caret(Editor editor) {
        super(editor);
    }


    public boolean isTouching() {
        return touching;
    }

    public boolean autoScroll(int aspect) {
        boolean scrolled = false;
        switch (aspect) {
            case SCROLL_UP:
                editor.removeCallbacks(scrollUpTask);
                if ((!onFirstRow())) {
                    editor.post(scrollUpTask);
                    scrolled = true;
                }
                break;
            case SCROLL_DOWN:
                editor.removeCallbacks(scrollDownTask);
                if (!onLastRow()) {
                    editor.post(scrollDownTask);
                    scrolled = true;
                }
                break;
            case SCROLL_LEFT:
                editor.removeCallbacks(scrollLeftTask);
                if (!onStart() && row == document.gainRowOfPosition(position - 1)) {
                    editor.post(scrollLeftTask);
                    scrolled = true;
                }
                break;
            case SCROLL_RIGHT:
                editor.removeCallbacks(scrollRightTask);
                if (!onEnd() && row == document.gainRowOfPosition(position + 1)) {
                    editor.post(scrollRightTask);
                    scrolled = true;
                }
                break;
            default:
                EditorException.fail("Invalid scroll direction");
                break;
        }
        return scrolled;
    }

    public void stopAutoScroll() {
        editor.removeCallbacks(scrollDownTask);
        editor.removeCallbacks(scrollUpTask);
        editor.removeCallbacks(scrollLeftTask);
        editor.removeCallbacks(scrollRightTask);
    }

    public void stopAutoScroll(int aspect) {
        switch (aspect) {
            case SCROLL_UP:
                editor.removeCallbacks(scrollUpTask);
                break;
            case SCROLL_DOWN:
                editor.removeCallbacks(scrollDownTask);
                break;
            case SCROLL_LEFT:
                editor.removeCallbacks(scrollLeftTask);
                break;
            case SCROLL_RIGHT:
                editor.removeCallbacks(scrollRightTask);
                break;
            default:
                EditorException.fail("Invalid scroll direction");
                break;
        }
    }


    @Override
    public int getRow() {
        return row;
    }

    @Override
    public int getPosition() {
        return position;
    }


    @Override
    public boolean onFirstRow() {
        return (row == 0);
    }

    @Override
    public boolean onLastRow() {
        return (row == (document.getRowCount() - 1));
    }
    @Override
    public boolean onStart() {
        return position == 0;
    }
    @Override
    public boolean onEnd() {
        return (position == (document.length() - 1));
    }

    @Override
    public void moveLeft() {
        moveLeft(false);
    }

    @Override
    public void moveRight() {
        moveRight(false);
    }


    //TODO 待解决 按屏幕坐标移动

    @Override
    public void moveDown() {
        if (!onLastRow()) {
            int currCaret = position;
            int currRow = row;
            int newRow = currRow + 1;
            int currColumn = document.getColumn(currCaret);
            int currRowLength = document.gainRowLength(currRow);
            int newRowLength = document.gainRowLength(newRow);

            if (currColumn < newRowLength) {
                position += currRowLength;
            } else {
                position += currRowLength - currColumn + newRowLength - 1;
            }
            ++row;

            controller.updateSelectionRange(currCaret, position);
            if (!editor.displayInterface.scrollToPosition(position)) {
                editor.invalidateRows(currRow, newRow + 1);
            }
            operator.onRowChange(newRow);
            inputConnection.stopTextComposing();
        }
    }

    @Override
    public void moveUp() {
        if (!onFirstRow()) {
            int currCaret = position;
            int currRow = row;
            int newRow = currRow - 1;
            int currColumn = document.getColumn(currCaret);
            int newRowLength = document.gainRowLength(newRow);

            if (currColumn < newRowLength) {
                position -= newRowLength;
            } else {
                position -= (currColumn + 1);
            }
            --row;

            controller.updateSelectionRange(currCaret, position);
            if (!editor.displayInterface.scrollToPosition(position)) {
                editor.invalidateRows(newRow, currRow + 1);
            }
            operator.onRowChange(newRow);
            inputConnection.stopTextComposing();
        }
    }

    @Override
    public void moveRight(boolean isTyping) {
        if (!onEnd()) {
            int originalRow = row;
            ++position;
            updateRow();
            controller.updateSelectionRange(position - 1, position);
            if (!editor.displayInterface.scrollToPosition(position)) {
                editor.invalidateRows(originalRow, row + 1);
            }
            if (!isTyping) {
                inputConnection.stopTextComposing();
            }
        }
    }


    @Override
    public void moveLeft(boolean isTyping) {
        if (!onStart()) {
            int originalRow = row;
            --position;
            updateRow();
            controller.updateSelectionRange(position + 1, position);
            if (!editor.displayInterface.scrollToPosition(position)) {
                editor.invalidateRows(row, originalRow + 1);
            }

            if (!isTyping) {
                inputConnection.stopTextComposing();
            }
        }
    }

    public void moveToPosition(int position) {
        if (position < 0 || position >= document.length()) {
            EditorException.fail("Invalid caret position");
            return;
        }

        controller.updateSelectionRange(this.position, position);
        updatePosition(position);
    }



    //-------protected methods-------


    protected void updateRow() {
        int newRow = document.gainRowOfPosition(position);
        if (row != newRow) {
            row = newRow;
            operator.onRowChange(newRow);
        }
    }


    protected void reset() {
        position = 0;
        row = 0;
    }

    protected void setCoord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    protected void setTouching(boolean touching) {
        this.touching = touching;
    }


    protected void updatePosition(int position) {
        this.position = position;
        int oldRow = row;
        updateRow();
        if (!editor.displayInterface.scrollToPosition(position)) {
            editor.invalidateRows(oldRow, oldRow + 1);
            editor.invalidateCaretRow();
        }
        inputConnection.stopTextComposing();
    }

    protected void onDrag(MotionEvent e) {

        int x = (int) e.getX() - editor.getPaddingLeft();
        int y = (int) e.getY() - editor.getPaddingTop();
        boolean scrolled = false;


        if (x < SCROLL_EDGE) {
            scrolled = caret.autoScroll(Caret.SCROLL_LEFT);
        } else if (x >= (editor.getContentWidth() - SCROLL_EDGE)) {
            scrolled = caret.autoScroll(Caret.SCROLL_RIGHT);
        } else if (y < SCROLL_EDGE) {
            scrolled = caret.autoScroll(Caret.SCROLL_UP);
        } else if (y >= (editor.getContentHeight() - SCROLL_EDGE)) {
            scrolled = caret.autoScroll(Caret.SCROLL_DOWN);
        }

        if (!scrolled) {
            caret.stopAutoScroll();
            int newPosition = editor.getPainter().getPositionByCoord(
                    editor.displayInterface.screenToViewX((int) e.getX()),
                    editor.displayInterface.screenToViewY((int) e.getY()), false
            );
            if (newPosition >= 0) {
                caret.moveToPosition(newPosition);
            }
        }
    }

    private final Runnable scrollDownTask = new Runnable() {
        @Override
        public void run() {
            moveDown();
            if (!onLastRow()) {
                editor.postDelayed(scrollDownTask, SCROLL_PERIOD);
            }
        }
    };

    private final Runnable scrollUpTask = new Runnable() {
        @Override
        public void run() {
            moveUp();
            if (!onFirstRow()) {
                editor.postDelayed(scrollUpTask, SCROLL_PERIOD);
            }
        }
    };


    private final Runnable scrollLeftTask = new Runnable() {
        @Override
        public void run() {
            moveLeft();
            if (!onStart() && row == document.gainRowOfPosition(position - 1)) {
                editor.postDelayed(scrollLeftTask, SCROLL_PERIOD);
            }
        }
    };

    private final Runnable scrollRightTask = new Runnable() {
        @Override
        public void run() {
            moveRight();
            if (!onEnd() && row == document.gainRowOfPosition(position + 1)) {
                editor.postDelayed(scrollRightTask, SCROLL_PERIOD);
            }
        }
    };

}
