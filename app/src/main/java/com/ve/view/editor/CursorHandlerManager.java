package com.ve.view.editor;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;

import com.ve.acpp.R;
import com.ve.utils.DisplayUtils;
import com.ve.view.utils.Rectangle;

public class CursorHandlerManager {
    private Editor editor;
    private BaseHandle mid;
    private BaseHandle start;
    private BaseHandle end;

    private boolean showHandler;

    private int handlerSize;
    private Caret caret;
    private Drawable handleIcon;

    public CursorHandlerManager(Editor editor) {
        this.editor = editor;

        handleIcon = editor.getContext().getResources().getDrawable(R.drawable.cursor );


        handlerSize = DisplayUtils.dp2px(editor.getContext(), 24);
        mid = new CommonHandle();
        start = new SelectHandle();
        end = new SelectHandle();
    }

    public void init() {
        caret = editor.caret;
    }

    public void onDown(MotionEvent e) {

        if (!caret.isTouching()) {
            int x = (int) e.getX() + editor.getScrollX();
            int y = (int) e.getY() + editor.getScrollY();

            if (mid.onDown(x, y)) {
                showHandler = true;
            } else if (start.onDown(x, y)) {
                editor.displayInterface.scrollToSelectionStart();
            } else if (end.onDown(x, y)) {
                editor.displayInterface.scrollToSelectionEnd();
            }
        }
    }

    public void onUp(MotionEvent e) {
        mid.clearTouch();
        start.clearTouch();
        end.clearTouch();
    }

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {


        if (mid.touched) {
            showHandler = true;
            mid.moveHandle(e2);
            return true;
        }
        if (start.touched) {
            start.moveHandle(e2);
            return true;
        }
        if (end.touched) {
            end.moveHandle(e2);
            return true;
        }
        return false;

    }


    public boolean onSingleTapUp(MotionEvent e) {
        int x = (int) e.getX() + editor.getScrollX();
        int y = (int) e.getY() + editor.getScrollY();

        if (mid.isInHandle(x, y) || start.isInHandle(x, y) || end.isInHandle(x, y)) {
            //忽略单次点击事件
            //拦截事件
            return true;
        } else {
            showHandler = true;
            return false;
        }
    }

    public boolean onDoubleTap(MotionEvent e) {
        int x = (int) e.getX() + editor.getScrollX();
        int y = (int) e.getY() + editor.getScrollY();

        if (mid.isInHandle(x, y)) {
            editor.getSelectInterface().select(true);
            return true;
        } else {
            return false;
        }
    }


    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mid.isTouched() || start.isTouched() || end.isTouched()) {
            return true;
        } else {
            return false;
        }
    }


    public void draw(Canvas canvas) {

        if (!editor.getSelectInterface().isSelecting()) {

            if (!mid.isTouched()) {
                mid.attachToPosition(caret.getPosition());
            }
            if (showHandler) {
                mid.draw(canvas);
            }
            showHandler = false;
        } else {
            if (!(start.isTouched() && end.isTouched())) {
                start.attachToPosition(editor.getSelectInterface().getSelectionStart());
                end.attachToPosition(editor.getSelectInterface().getSelectionEnd());
            }
            start.draw(canvas);
            end.draw(canvas);
        }
    }


    private class BaseHandle {
        public Rectangle body = new Rectangle(0, 0, handlerSize, (int) (1f*handlerSize*handleIcon.getIntrinsicHeight()/handleIcon.getIntrinsicWidth()));

        private int anchorX = 0, anchorY = 0;

        private int touchX = 0, touchY = 0;//相对位置

        private final Paint paint;


        private boolean touched;


        public boolean isTouched() {
            return touched;
        }

        public BaseHandle() {
            paint = new Paint();
            paint.setColor(Color.BLUE);
            paint.setAntiAlias(true);
        }

        public void moveHandle(MotionEvent e) {

            int newPosition = gainNearestPosition((int) e.getX(), (int) e.getY());

            if (editor.textInterface.isValidPosition(newPosition)) {
                caret.moveToPosition(newPosition);

                moveToPosition(newPosition);
            }

        }


        public void draw(Canvas canvas) {

            handleIcon.setBounds(body.toRect());
            handleIcon.draw(canvas);
            /*canvas.drawLine(anchorX, anchorY,
                    body.getCenterX(), body.getCenterY(), paint);
            canvas.drawArc(new RectF(anchorX - radius, anchorY - radius / 2 - offsetY(),
                    body.x + radius * 2, body.y + radius / 2), 60, 60, true, paint);
            canvas.drawOval(body.toRectF(), paint);*/


        }

        private int offsetY() {
            return 0;
        }


        public void moveToPosition(int position) {

            Rect newCaretBounds = editor.painter.getBoundingBox(position);
            int newX = newCaretBounds.left + editor.getPaddingLeft();
            int newY = newCaretBounds.bottom + editor.getPaddingTop();
            setRestingCoord(newX, newY);
            editor.invalidate(body.toRect());
        }


        public void setRestingCoord(int x, int y) {
            anchorX = x;
            anchorY = y;
            body.setPosition(x - body.width / 2, y + offsetY());

        }


        public int gainNearestPosition(int x, int y) {
            int attachedLeft = editor.displayInterface.screenToViewX(x) - touchX + body.width / 2;
            int attachedBottom = editor.displayInterface.screenToViewY(y) - touchY - offsetY() - 2;

            return editor.getPainter().getPositionByCoord(attachedLeft, attachedBottom, false);
        }

        public void setTouch(int x, int y) {
            touchX = x - body.x;
            touchY = y - body.y;
        }

        public void clearTouch() {
            touched = false;
            touchX = 0;
            touchY = 0;
        }


        public boolean isInHandle(int x, int y) {
            return body.contains(x, y);
        }

        public boolean onDown(int x, int y) {
            touched = isInHandle(x, y);
            if (touched) {
                setTouch(x, y);
                editor.invalidate(body.toRect());
            }
            return touched;
        }

        public void attachToPosition(int position) {
            Rect boundingBox = editor.painter.getBoundingBox(position);
            int x = boundingBox.left + editor.getPaddingLeft();
            int y = boundingBox.bottom + editor.getPaddingTop();
            setRestingCoord(x, y);
        }
    }

    class CommonHandle extends BaseHandle {
        @Override
        public boolean isInHandle(int x, int y) {
            return super.isInHandle(x, y) && !editor.getSelectInterface().isSelecting();
        }
    }

    class SelectHandle extends BaseHandle {
        @Override
        public boolean isInHandle(int x, int y) {
            return super.isInHandle(x, y) && editor.getSelectInterface().isSelecting();
        }
    }

}