package com.ve.view.editor;

import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.InputType;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Scroller;

import com.ve.view.utils.EditorException;
import com.ve.view.editor.out.CaretInterface;
import com.ve.view.editor.out.Config;
import com.ve.view.editor.out.DisplayInterface;
import com.ve.view.editor.out.TextInterface;
import com.ve.view.editor.out.SelectInterface;
import com.ve.view.editor.out.Status;
import com.ve.view.text.document.Document;
import com.ve.view.utils.CharRange;

public class Editor extends View {
    private static final String TAG = Editor.class.getSimpleName();
    protected ListenManager operator;

    protected EventManager touchNavigationMethod;
    protected Document document;
    protected TextController controller;
    protected EditorInputConnection inputConnection;
    protected Caret caret;
    protected Painter painter;
    protected SpanManager spanManager;

    protected ClipboardManager clipboardManager;
    protected Config config;

    protected Scroller scroller;
    protected boolean isEdited = false;
    protected boolean isLayout;


    protected long lastScroll;



    public Editor(Context context) {
        super(context);
        init(context);
    }

    public Editor(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public Editor(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }


    private void init(Context context) {
        setLongClickable(true);
        setFocusableInTouchMode(true);
        setHapticFeedbackEnabled(true);
        setScrollContainer(true);

        clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        scroller = new Scroller(context);


        document = new Document();
        caret = new Caret(this);
        painter = new Painter(this);
        controller = new TextController(this);
        inputConnection = new EditorInputConnection(this);
        touchNavigationMethod = new EventManager(this);
        operator = new ListenManager(this);
        spanManager = new SpanManager(this);
        config=new Config();
        resetView();
        invalidate();
    }


    private void resetView() {
        inputConnection.init();
        document.setWordWrapable(painter);
        controller.init();
        caret.init();
        touchNavigationMethod.init();
        painter.init();
        operator.init();
        caret.reset();
        painter.reset();
        spanManager.init();


        controller.setSelectText(false);
        inputConnection.stopTextComposing();
        painter.clearTextSpanData();
        if (getContentWidth() > 0 || !document.isWordWrapEnable()) {
            document.analyzeWordWrap();
        }
        operator.onRowChange(0);
        scrollTo(0, 0);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        canvas.clipRect(getScrollX() + getPaddingLeft(),
                getScrollY() + getPaddingTop(),
                getScrollX() + getWidth() - getPaddingRight(),
                getScrollY() + getHeight() - getPaddingBottom());
        canvas.translate(getPaddingLeft(), getPaddingTop());

        long drawStartTime = System.currentTimeMillis();
        painter.draw(canvas);
        long drawEndTime = System.currentTimeMillis();
        Log.d(TAG, String.format("onDraw: cost %d ms", drawEndTime - drawStartTime));

        canvas.restore();

        touchNavigationMethod.onTextDrawComplete(canvas);
    }


    @Override
    public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
        outAttrs.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE;
        outAttrs.imeOptions = EditorInfo.IME_FLAG_NO_ENTER_ACTION | EditorInfo.IME_ACTION_DONE | EditorInfo.IME_FLAG_NO_EXTRACT_UI;
        inputConnection.reset();
        return inputConnection;
    }

    @Override
    public boolean onCheckIsTextEditor() {
        return true;
    }

    @Override
    public boolean isSaveEnabled() {
        return true;
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(useAllDimensions(widthMeasureSpec),
                useAllDimensions(heightMeasureSpec));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        if (changed) {
            Rect rect = new Rect();
            getWindowVisibleDisplayFrame(rect);
            painter.setLineNumberWidth(rect.top + rect.height() - getHeight());
            if (!isLayout) {
                spanManager.startSpan();
            }
            isLayout = right > 0;
            invalidate();
        }
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (document.isWordWrapEnable() && oldw != w)
            document.analyzeWordWrap();
        caret.updateRow();
        if (h < oldh) {
            displayInterface.scrollToPosition(caret.position);
        }
    }

    private static int useAllDimensions(int measureSpec) {
        int specMode = MeasureSpec.getMode(measureSpec);
        int result = MeasureSpec.getSize(measureSpec);

        if (specMode != MeasureSpec.EXACTLY && specMode != MeasureSpec.AT_MOST) {
            result = Integer.MAX_VALUE;
            EditorException.fail("MeasureSpec cannot be UNSPECIFIED. Setting dimensions to max.");
        }

        return result;
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SYM || keyCode == KeyCharacterMap.PICKER_DIALOG_INPUT) {
            return true;
        }
        return controller.handleKeyDown(keyCode, event) ? true : super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onTrackballEvent(MotionEvent event) {
        int deltaX = Math.round(event.getX());
        int deltaY = Math.round(event.getY());
        while (deltaX > 0) {
            caret.moveRight();
            --deltaX;
        }
        while (deltaX < 0) {
            caret.moveLeft();
            ++deltaX;
        }
        while (deltaY > 0) {
            caret.moveDown();
            --deltaY;
        }
        while (deltaY < 0) {
            caret.moveUp();
            ++deltaY;
        }
        return true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isFocused()) {
            touchNavigationMethod.onTouchEvent(event);
        } else {
            if ((event.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_UP && isPointInView((int) event.getX(), (int) event.getY())) {
                requestFocus();
            }
        }
        return true;
    }

    private boolean isPointInView(int x, int y) {
        return (x >= 0 && x < getWidth() && y >= 0 && y < getHeight());
    }

    @Override
    protected void onFocusChanged(boolean gainFocus, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        invalidateCaretRow();
    }





    //------protected methods------
    protected void setEdited(boolean set) {
        isEdited = set;
    }

    protected int getContentHeight() {
        return getHeight() - getPaddingTop() - getPaddingBottom();
    }

    protected int getContentWidth() {
        return getWidth() - getPaddingLeft() - getPaddingRight();
    }

    //---invalidate---

    protected void invalidateRows(int startRow, int endRow) {
        EditorException.logIf(startRow > endRow || startRow < 0, "Invalid startRow and/or endRow");

        /*TODO The ascent of (startRow+1) may jut inside startRow, so part of
         that rows have to be invalidated as well.
         This is a problem for Thai, Vietnamese and Indic scripts*/
        Paint.FontMetricsInt metrics = painter.getFontMetricsInt();
        int top = startRow * painter.rowHeight() + getPaddingTop();
        top -=  metrics.descent;
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                endRow * painter.rowHeight() + getPaddingTop() );
    }


    protected void invalidateFromRow(int startRow) {
        EditorException.logIf(startRow < 0, "Invalid startRow");

        //TODO The ascent of (startRow+1) may jut inside startRow, so part of
        // that rows have to be invalidated as well.
        // This is a problem for Thai, Vietnamese and Indic scripts
        Paint.FontMetricsInt metrics = painter.getFontMetricsInt();
        int top = startRow * painter.rowHeight() + getPaddingTop();
        top -=  metrics.descent;
        top = Math.max(0, top);

        super.invalidate(0,
                top,
                getScrollX() + getWidth(),
                getScrollY() + getHeight());
    }

    protected void invalidateCaretRow() {
        invalidateRow(caret.row);
    }

    protected void invalidateRow(int row) {
        invalidateRows(row, row + 1);
    }

    protected void invalidateSelectionRows() {
        int startRow = document.gainRowOfPosition(controller.selectionStart);
        int endRow = document.gainRowOfPosition(controller.selectionEnd);
        invalidateRows(startRow, endRow + 1);
    }

    //---scroll---


    protected int scrollToRow(int position) {
        int scrollBy = 0;
        int charTop = document.gainRowOfPosition(position) * painter.rowHeight();
        int charBottom = charTop + painter.rowHeight();

        if (charTop < getScrollY()) {
            scrollBy = charTop - getScrollY();
        } else if (charBottom > (getScrollY() + getContentHeight())) {
            scrollBy = charBottom - getScrollY() - getContentHeight();
        }

        return scrollBy;
    }

    protected int scrollToColumn(int position) {
        int scrollBy = 0;
        CharRange visibleRange = painter.getCharExtent(position);

        if (visibleRange.getRight() > (getScrollX() + getContentWidth())) {
            scrollBy = visibleRange.getRight() - getScrollX() - getContentWidth();
        }

        if (visibleRange.getLeft() < getScrollX() + painter.getSpaceWidth()) {
            scrollBy = visibleRange.getLeft() - getScrollX() - painter.getSpaceWidth();
        }

        return scrollBy;
    }


    protected int getMaxScrollX() {
        if (document.isWordWrapEnable())
            return painter.getLineNumberWidth();
        else
            return Math.max(0, painter.getContentWidth() - getContentWidth() + painter.getSpaceWidth());
    }

    protected int getMaxScrollY() {
        return Math.max(0,
                document.getRowCount() * painter.rowHeight() - getContentHeight() / 2 );
    }

    @Override
    protected int computeVerticalScrollOffset() {
        return getScrollY();
    }

    @Override
    protected int computeVerticalScrollRange() {
        return document.getRowCount() * painter.rowHeight() + getPaddingTop() + getPaddingBottom();
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.getCurrX(), scroller.getCurrY());
            postInvalidate();
        }
    }


    //-------interfaces -------------



    public CaretInterface getCaretInterface() {
        return caret;
    }

    public TextInterface getTextInterface() {
        return textInterface;
    }

    protected TextInterface textInterface = new TextInterface() {
        @Override
        public void replaceText(int position, int count, String text) {
            document.beginBatchEdit();
            controller.replaceText(position, count, text);
            inputConnection.stopTextComposing();
            document.endBatchEdit();
        }

        @Override
        public int getLength() {
            return document.length();
        }

        @Override
        public boolean isValidPosition(int position) {
            return document.isValidPosition(position);
        }

        @Override
        public int getRowCount() {
            return document.getRowCount();
        }

        @Override
        public void setDocument(Document document) {
            Editor.this.document = document;
            resetView();
            spanManager.stopSpan();
            spanManager.startSpan();
            invalidate();
        }

        @Override
        public Document getDocument() {
            return document;
        }

        @Override
        public void setEdited(boolean set) {
            isEdited = set;
        }

        @Override
        public boolean isEdited() {
            return isEdited;
        }

        @Override
        public void undo() {
            int newPosition = document.undo();

            if (newPosition >= 0) {
                setEdited(true);

                spanManager.startSpan();
                selectInterface.cancelSelect();
                caret.moveToPosition(newPosition);
                invalidate();
            }

        }

        @Override
        public void redo() {
            int newPosition = document.redo();

            if (newPosition >= 0) {
                setEdited(true);

                spanManager.startSpan();
                selectInterface.cancelSelect();
                caret.moveToPosition(newPosition);
                invalidate();
            }

        }

        @Override
        public void cut() {
            controller.cut(clipboardManager);
        }

        @Override
        public void copy() {
            controller.copy(clipboardManager);
            selectInterface.cancelSelect();
        }

        @Override
        public void paste() {
            controller.paste(clipboardManager.getText().toString());
        }

        @Override
        public void paste(String text) {
            controller.paste(text);
        }
    };


    public SelectInterface getSelectInterface() {
        return selectInterface;
    }

    protected SelectInterface selectInterface = new SelectInterface() {


        @Override
        public final boolean isSelecting() {
            return controller.isSelecting();
        }


        @Override
        public void select(int position, int count) {
            controller.setSelectionRange(position, count, true);
        }

        @Override
        public void cancelSelect() {
            if (controller.isSelecting()) {
                invalidateSelectionRows();
                controller.setSelectText(false);
            }
        }

        @Deprecated
        @Override
        public void select(boolean select) {
            if (controller.isSelecting() && !select) {
                invalidateSelectionRows();
                controller.setSelectText(false);
            } else if (!controller.isSelecting() && select) {
                invalidateCaretRow();
                controller.setSelectText(true);
            }
        }

        @Override
        public void selectAll() {
            controller.setSelectionRange(0, document.length() - 1, false);
        }


        @Override
        public boolean inSelectionRange(int position) {
            return controller.inSelectionRange(position);
        }

        @Override
        public int getSelectionStart() {
            if (controller.selectionStart < 0) {
                return caret.position;
            } else {
                return controller.selectionStart;
            }
        }

        @Override
        public int getSelectionEnd() {
            if (controller.selectionEnd < 0) {
                return caret.position;
            } else {
                return controller.selectionEnd;
            }
        }

    };

    public DisplayInterface getDisplayInterface() {
        return displayInterface;
    }

    protected DisplayInterface displayInterface = new DisplayInterface() {
        @Override
        public boolean scrollToPosition(int position) {
            EditorException.logIf(position < 0 || position >= document.length(), "Invalid position given");
            int scrollVerticalBy = scrollToRow(position);
            int scrollHorizontalBy = scrollToColumn(position);

            if (scrollVerticalBy == 0 && scrollHorizontalBy == 0) {
                return false;
            } else {
                scrollBy(scrollHorizontalBy, scrollVerticalBy);
                return true;
            }
        }

        @Override
        public void scrollToSelectionStart() {
            controller.focusSelection(true);
        }

        @Override
        public void scrollToSelectionEnd() {
            controller.focusSelection(false);
        }

        @Override
        public void scroll(float dx, float dy) {
            int newX = (int) dx + getScrollX();
            int newY = (int) dy + getScrollY();

            int maxWidth = Math.max(getMaxScrollX(), getScrollX());
            if (newX > maxWidth) {
                newX = maxWidth;
            } else if (newX < 0) {
                newX = 0;
            }

            int maxHeight = Math.max(getMaxScrollY(), getScrollY());
            if (newY > maxHeight) {
                newY = maxHeight;
            } else if (newY < 0) {
                newY = 0;
            }
            smoothScrollTo(newX, newY);

        }

        @Override
        public final void smoothScrollTo(int x, int y) {
            smoothScroll(x - getScrollX(), y - getScrollY());
        }

        @Override
        public void flingScroll(int velocityX, int velocityY) {

            scroller.fling(getScrollX(), getScrollY(), velocityX, velocityY, 0, getMaxScrollX(), 0, getMaxScrollY());
            postInvalidate();
            //postInvalidateOnAnimation();
        }

        @Override
        public boolean isFlingScrolling() {
            return !scroller.isFinished();
        }

        @Override
        public void stopFlingScrolling() {
            scroller.forceFinished(true);
        }

        @Override
        public final void smoothScroll(int dx, int dy) {
            if (getHeight() == 0) {
                return;
            }
            long duration = AnimationUtils.currentAnimationTimeMillis() - lastScroll;
            if (duration > 250) {
                //final int maxY = getMaxScrollX();
                final int scrollY = getScrollY();
                final int scrollX = getScrollX();

                //dy = Math.max(0, Math.min(scrollY + dy, maxY)) - scrollY;

                scroller.startScroll(scrollX, scrollY, dx, dy);
                postInvalidate();
            } else {
                if (!scroller.isFinished()) {
                    scroller.abortAnimation();
                }
                scrollBy(dx, dy);
            }
            lastScroll = AnimationUtils.currentAnimationTimeMillis();
        }

        @Override
        public void scrollToCaret() {
            scrollToPosition(caret.getPosition());
        }

        @Override
        final public int screenToViewX(int x) {
            return x - getPaddingLeft() + getScrollX();
        }

        @Override
        final public int screenToViewY(int y) {
            return y - getPaddingTop() + getScrollY();
        }
    };


    //-----------config-------------
    public void applyConfig(Config config) {
        if (config==null){
            return;
        }
        this.config = config;
        onConfig();
    }

    public Config getConfig() {
        return config;
    }

    private void onConfig() {

        painter.onConfig(config);


        document.setWordWrapEnable(config.wordWrap);
        if (config.wordWrap) {
            painter.reset();
        }


        caret.updateRow();
        displayInterface.scrollToCaret();

        invalidate();
    }

    //-----------

    public ListenManager getOperator() {
        return operator;
    }

    public Painter getPainter() {
        return painter;
    }
    public void onDestroy() {
        spanManager.stopSpan();
    }

    public Status getStastus() {
        return new Status(this);
    }

    public void applyStatus(Status uiState) {
        final int caretPosition = uiState.caretPosition;
        if (uiState.selectMode) {
            final int selStart = uiState.selectBegin;
            final int selEnd = uiState.selectEnd;

            post(() -> {
                selectInterface.select(selStart, selEnd - selStart);
                if (caretPosition < selEnd) {
                    displayInterface.scrollToSelectionStart(); //caret at the end by default
                }
            });
        } else {
            post(() -> caret.moveToPosition(caretPosition));
        }
    }



}
