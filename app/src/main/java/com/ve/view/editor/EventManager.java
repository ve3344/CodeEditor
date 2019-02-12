package com.ve.view.editor;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.GestureDetector;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;

import com.ve.utils.SysUtils;
import com.ve.view.text.document.Document;
import com.ve.view.utils.ZoomChecker;


public class EventManager {
    protected Editor editor;
    protected Caret caret;
    protected GestureDetector gestureDetector;
    protected ZoomChecker zoomChecker;
    protected int fling;
    protected CursorHandlerManager cursorHandler;
    protected BaseListener baseListener;
    protected static int TOUCH_SLOP = 12;

    public EventManager(Editor editor) {
        this.editor = editor;
        baseListener = new BaseListener();
        gestureDetector = new GestureDetector(editor.getContext(), baseListener);
        gestureDetector.setIsLongpressEnabled(true);
        cursorHandler=new CursorHandlerManager(editor);
        zoomChecker = new ZoomChecker();
        zoomChecker.setListener(editor.getPainter());
    }

    public void init() {
        caret = editor.caret;
        cursorHandler.init();
    }


    public boolean onTouchEvent(MotionEvent event) {
        zoomChecker.checkZoom(event);
        boolean handled = gestureDetector.onTouchEvent(event);
        if (!handled && (event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
            handled = baseListener.onUp(event);
        }
        return handled;
    }


    public boolean isNearChar(int x, int y, int charOffset) {
        Rect bounds = editor.painter.getBoundingBox(charOffset);

        return (y >= (bounds.top - TOUCH_SLOP)
                && y < (bounds.bottom + TOUCH_SLOP)
                && x >= (bounds.left - TOUCH_SLOP)
                && x < (bounds.right + TOUCH_SLOP)
        );
    }

    public void onTextDrawComplete(Canvas canvas) {
        cursorHandler.draw(canvas);
    }


    class BaseListener extends GestureDetector.SimpleOnGestureListener {

        public boolean onUp(MotionEvent e) {
            caret.stopAutoScroll();
            caret.setTouching(false);
            zoomChecker.reset();
            fling = 0;
            cursorHandler.onUp(e);
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            int x = editor.displayInterface.screenToViewX((int) e.getX());
            int y = editor.displayInterface.screenToViewY((int) e.getY());
            caret.setTouching(isNearChar(x, y, caret.getPosition()));

            if (editor.displayInterface.isFlingScrolling()) {
                editor.displayInterface.stopFlingScrolling();
            } else if (editor.getSelectInterface().isSelecting()) {
                if (isNearChar(x, y, editor.getSelectInterface().getSelectionStart())) {
                    editor.displayInterface.scrollToSelectionStart();
                    editor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);

                    caret.setTouching(true);
                } else if (isNearChar(x, y, editor.getSelectInterface().getSelectionEnd())) {
                    editor.displayInterface.scrollToSelectionEnd();
                    editor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    caret.setTouching(true);
                }
            }

            if (caret.isTouching()) {
                editor.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
            }
            cursorHandler.onDown(e);

            return true;
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (cursorHandler.onSingleTapUp(e)){
                return true;
            }

            int x = editor.displayInterface.screenToViewX((int) e.getX());
            int y = editor.displayInterface.screenToViewY((int) e.getY());
            int position = editor.getPainter().getPositionByCoord(x, y, false);

            if (editor.getSelectInterface().isSelecting()) {
                int strictCharOffset = editor.getPainter().getPositionByCoord(x, y, true);
                if (editor.getSelectInterface().inSelectionRange(strictCharOffset) || isNearChar(x, y, editor.getSelectInterface().getSelectionStart()) || isNearChar(x, y, editor.getSelectInterface().getSelectionEnd())) {
                    // do nothing
                } else {
                    editor.getSelectInterface().cancelSelect();
                    if (strictCharOffset >= 0) {
                        caret.moveToPosition(position);
                    }
                }
            } else {
                if (position >= 0) {
                    caret.moveToPosition(position);
                }
            }
            SysUtils.showInputMethod(editor, true);
            return true;
        }


        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (cursorHandler.onScroll(e1, e2, distanceX, distanceY)){
                return true;
            }

            if (caret.isTouching()) {
                caret.onDrag(e2);
            } else if (e2.getPointerCount() == 1) {
                if (fling == 0)
                    if (Math.abs(distanceX) > Math.abs(distanceY))
                        fling = 1;
                    else
                        fling = -1;
                if (fling == 1)
                    distanceY = 0;
                else if (fling == -1)
                    distanceX = 0;

                editor.displayInterface.scroll(distanceX, distanceY);

            }
            if ((e2.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP) {
                onUp(e2);
            }
            return true;
        }


        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (cursorHandler.onFling(e1, e2, velocityX, velocityY)){
                return true;
            }

            if (!caret.isTouching()) {
                if (fling == 1) {
                    velocityY = 0;
                } else if (fling == -1) {
                    velocityX = 0;
                }
                editor.displayInterface.flingScroll((int) -velocityX, (int) -velocityY);
            }
            onUp(e2);
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {
            onDoubleTap(e);
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (cursorHandler.onDoubleTap(e)){
                return true;
            }

            caret.setTouching(true);
            int x = editor.displayInterface.screenToViewX((int) e.getX());
            int y = editor.displayInterface.screenToViewY((int) e.getY());
            int charOffset = editor.getPainter().getPositionByCoord(x, y, false);

            if (charOffset >= 0) {
                caret.moveToPosition(charOffset);
                Document document = editor.document;
                int start;
                int end;
                for (start = charOffset; start >= 0; start--) {
                    char c = document.charAt(start);
                    if (!Character.isJavaIdentifierPart(c))
                        break;
                }
                if (start != charOffset)
                    start++;
                for (end = charOffset; end >= 0; end++) {
                    char c = document.charAt(end);
                    if (!Character.isJavaIdentifierPart(c))
                        break;
                }
                //editor.getSelectInterface().select(true);
                editor.getSelectInterface().select(start, end - start);
            }

            return true;
        }

    }


}
