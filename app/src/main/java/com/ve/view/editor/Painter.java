package com.ve.view.editor;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;

import com.ve.view.editor.out.Config;
import com.ve.view.editor.span.TextSpanData;
import com.ve.view.ext.ColorScheme;
import com.ve.view.text.document.CommonLanguage;
import com.ve.view.text.document.WordWrapable;
import com.ve.view.utils.CharRange;
import com.ve.view.utils.ZoomChecker;


public class Painter extends Base implements WordWrapable, ZoomChecker.ZoomListener {

    protected static int BASE_TEXTSIZE = 32;
    protected int tabLength = 4;


    protected ColorScheme colorScheme = new ColorScheme();
    protected TextSpanData textSpanData;

    protected Paint textPaint;
    protected Paint linePaint;
    protected Paint backgroundPaint;

    protected ContentPainter contentPainter;


    protected int contentWidth = 0;
    protected int lineNumberWidth;




    protected MeasureCache measureCache;
    protected MeasureCache.MeasureTool measureTool;



    class ContentPainter {
        protected char emoji;

        protected void draw(Canvas canvas) {

            int currentRow = getBeginPaintRow(canvas);
            int currentPosition = document.gainPositionOfRow(currentRow);
            if (currentPosition < 0) {
                return;
            }
            int currentLine = document.isWordWrapEnable() ? document.gainLineOfPosition(currentPosition) + 1 : currentRow + 1;
            int lastLineNum = 0;

            int endRow = getEndPaintRow(canvas);
            int paintX = 0;
            int paintY = getPaintBaseline(currentRow);
            int rowCount = document.getRowCount();


            lineNumberWidth = (int) linePaint.measureText(document.getRowCount() + " ");
            linePaint.setColor(Color.DKGRAY);
            int startX = lineNumberWidth - getSpaceWidth() / 2;
            canvas.drawLine(startX, editor.getScrollY(), startX, editor.getScrollY() + editor.getHeight(), linePaint);


            TextSpanData.SpanSeeker spanSeeker = textSpanData.getNewSeeker(textPaint);
            spanSeeker.begin(currentPosition);


            for (; currentRow <= endRow && currentRow <= rowCount; ++currentRow) {

                int rowLen = document.gainRowLength(currentRow);
                if (currentLine != lastLineNum) {
                    lastLineNum = currentLine;
                    drawLineNum(canvas, String.valueOf(currentLine), 0, paintY);
                }
                paintX = lineNumberWidth;


                for (int i = 0; i < rowLen; ++i, ++currentPosition) {
                    spanSeeker.listenSpan(currentPosition);
                    char c = document.charAt(currentPosition);

                    if (currentPosition == caret.position) {
                        drawCaret(canvas, paintX, paintY);
                    }

                    if (controller.inSelectionRange(currentPosition)) {
                        paintX += drawSelectedText(canvas, c, paintX, paintY);
                    } else {
                        paintX += drawChar(canvas, c, paintX, paintY);
                    }

                }

                if (document.charAt(currentPosition - 1) == CommonLanguage.NEWLINE) {
                    ++currentLine;
                }

                paintY += rowHeight();
                if (paintX > contentWidth) {
                    contentWidth = paintX;
                }

            }

            drawCaretRow(canvas);
        }

        private int getBeginPaintRow(Canvas canvas) {
            Rect bounds = canvas.getClipBounds();
            return bounds.top / rowHeight();
        }

        private int getEndPaintRow(Canvas canvas) {
            Rect bounds = canvas.getClipBounds();
            return (bounds.bottom - 1) / rowHeight();
        }


        private void drawCaretRow(Canvas canvas) {
            int y = getPaintBaseline(caret.row);
            int lineLength = Math.max(contentWidth, editor.getContentWidth());

            backgroundPaint.setColor(0x66889977);
            drawBackground(canvas, 0, caret.y, lineLength);
        }

        private int drawChar(Canvas canvas, char c, int paintX, int paintY) {
            int originalColor = textPaint.getColor();
            int charWidth = measureTool.measure(c);

            if (paintX > editor.getScrollX() || paintX < (editor.getScrollX() + editor.getContentWidth()))
                switch (c) {
                    case CommonLanguage.EMOJI1:
                    case CommonLanguage.EMOJI2:
                        emoji = c;
                        break;
                    case CommonLanguage.SPACE:
                        if (config.showNonPrinting) {
                            textPaint.setColor(colorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
                            canvas.drawText(CommonLanguage.GLYPH_SPACE, 0, 1, paintX, paintY, textPaint);
                            textPaint.setColor(originalColor);
                        } else {
                            canvas.drawText(" ", 0, 1, paintX, paintY, textPaint);
                        }
                        break;

                    case CommonLanguage.EOF: //fall-through
                    case CommonLanguage.NEWLINE:
                        if (config.showNonPrinting) {
                            textPaint.setColor(colorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
                            canvas.drawText(CommonLanguage.GLYPH_NEWLINE, 0, 1, paintX, paintY, textPaint);
                            textPaint.setColor(originalColor);
                        }
                        break;

                    case CommonLanguage.TAB:
                        if (config.showNonPrinting) {
                            textPaint.setColor(colorScheme.getColor(ColorScheme.Colorable.NON_PRINTING_GLYPH));
                            canvas.drawText(CommonLanguage.GLYPH_TAB, 0, 1, paintX, paintY, textPaint);
                            textPaint.setColor(originalColor);
                        }
                        break;

                    default:
                        if (emoji != 0) {
                            canvas.drawText(new char[]{emoji, c}, 0, 2, paintX, paintY, textPaint);
                            emoji = 0;
                        } else {
                            char[] ca = {c};
                            canvas.drawText(ca, 0, 1, paintX, paintY, textPaint);
                        }
                        break;
                }

            return charWidth;
        }

        private void drawBackground(Canvas canvas, int paintX, int paintY, int advance) {
            Paint.FontMetricsInt metrics = textPaint.getFontMetricsInt();
            canvas.drawRect(paintX,
                    paintY + metrics.ascent,
                    paintX + advance,
                    paintY + metrics.descent,
                    backgroundPaint);
        }

        private int drawSelectedText(Canvas canvas, char c, int paintX, int paintY) {
            int oldColor = textPaint.getColor();
            int advance = measureTool.measure(c);

            backgroundPaint.setColor(colorScheme.getColor(ColorScheme.Colorable.SELECTION_BACKGROUND));
            drawBackground(canvas, paintX, paintY, advance);

            textPaint.setColor(colorScheme.getColor(ColorScheme.Colorable.SELECTION_FOREGROUND));
            drawChar(canvas, c, paintX, paintY);

            textPaint.setColor(oldColor);
            return advance;
        }

        private void drawCaret(Canvas canvas, int paintX, int paintY) {
            caret.setCoord(paintX, paintY);
            backgroundPaint.setColor(Color.BLUE);
            drawBackground(canvas, paintX - 1, paintY, 2);
        }

        private int drawLineNum(Canvas canvas, String s, int paintX, int paintY) {
            canvas.drawText(s, paintX, paintY, linePaint);
            return lineNumberWidth;
        }

    }

    public Painter(Editor editor) {
        super(editor);
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(BASE_TEXTSIZE);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setTextSize(BASE_TEXTSIZE);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setTextSize(BASE_TEXTSIZE);

        textSpanData = new TextSpanData();
        contentPainter = new ContentPainter();
        measureCache = new MeasureCache(textPaint);
        measureTool = measureCache.getNewMeasureTool();
    }

    @Override
    public void init() {
        super.init();
        onConfig(config);
    }

    public void clearTextSpanData() {
        textSpanData.clear();
    }

    public void setTextSpanData(TextSpanData textSpanData) {
        this.textSpanData = textSpanData;
    }

    public void setLineNumberWidth(int lineNumberWidth) {
        this.lineNumberWidth = lineNumberWidth;
    }

    public int getLineNumberWidth() {
        return lineNumberWidth;
    }

    public void reset() {
        contentWidth = 0;
    }


    public int getPaintBaseline(int row) {
        Paint.FontMetricsInt metrics = textPaint.getFontMetricsInt();
        return (row + 1) * rowHeight() - metrics.descent;
    }


    protected void draw(Canvas canvas) {
        contentPainter.draw(canvas);
    }


    @Override
    public int measure(char c) {
        return measureTool.measure(c);
    }

    @Override
    final public int getEditorWidth() {
        return editor.getContentWidth() - lineNumberWidth;
    }


    protected CharRange getCharExtent(int position) {
        int row = document.gainRowOfPosition(position);
        int rowBeginPosition = document.gainPositionOfRow(row);
        int left = lineNumberWidth;
        int right = lineNumberWidth;
        int indexOfRow = position - rowBeginPosition;

        String rowText = document.gainRowText(row);

        MeasureCache.MeasureTool measureTool = measureCache.getNewMeasureTool();
        for (int i = 0; i < rowText.length() && i <= indexOfRow; i++) {
            char c = rowText.charAt(i);
            left = right;
            right += measureTool.measure(c);
        }


        return new CharRange(left, right);
    }


    Rect getBoundingBox(int charOffset) {
        if (charOffset < 0 || charOffset >= document.length()) {
            return new Rect(-1, -1, -1, -1);
        }

        int row = document.gainRowOfPosition(charOffset);
        int top = row * rowHeight();
        int bottom = top + rowHeight();

        CharRange xExtent = getCharExtent(charOffset);

        return new Rect(xExtent.getLeft(), top, xExtent.getRight(), bottom);
    }

    public ColorScheme getColorScheme() {
        return colorScheme;
    }

    protected int getNumVisibleRows() {
        return (int) Math.ceil((float) editor.getContentHeight() / rowHeight());
    }

    public int rowHeight() {
        return measureCache.getRowHeight();
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
        linePaint.setTypeface(typeface);
        measureCache.invalidate();
        if (document.isWordWrapEnable())
            document.analyzeWordWrap();
        caret.updateRow();
        if (!editor.displayInterface.scrollToPosition(caret.position)) {
            editor.invalidate();
        }
    }


    public Paint.FontMetricsInt getFontMetricsInt() {
        return textPaint.getFontMetricsInt();
    }

    public int getContentWidth() {
        return contentWidth;
    }

    public void setTabSpaces(int spaceCount) {
        if (spaceCount < 0) {
            return;
        }

        tabLength = spaceCount;
        if (document.isWordWrapEnable())
            document.analyzeWordWrap();
        caret.updateRow();
        if (!editor.displayInterface.scrollToPosition(caret.position)) {
            editor.invalidate();
        }
    }


    public int getSpaceWidth() {
        return measureCache.getSpaceWidth();
    }


    public void onConfig(Config config) {
        measureCache.setConfig(config);
    }

    //x,y of screen
    public int getPositionByCoord(int x, int y, boolean strict) {
        int row = y / rowHeight();

        if (row > document.getRowCount())
            return document.length() - 1;

        int position = document.gainPositionOfRow(row);
        if (position < 0) {
            return -1;
        }

        if (x < 0) {
            return strict ? -1 : position;
        }
        String text = document.gainRowText(row);
        int left = getLineNumberWidth();

        MeasureCache.MeasureTool measureTool = measureCache.getNewMeasureTool();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            left += measureTool.measure(c);

            if (left >= x) {
                if (measureTool.isJustEmoji()) {
                    return position + i - 1;
                } else {
                    return position + i;
                }
            }
        }

        return strict ? -1 : position + text.length() - 1;
    }


    @Override
    public void onZoom(double scale,double scaleAll) {

        setScale((float) (scaleAll * BASE_TEXTSIZE));
    }


    private void setScale(float fontSize) {

        float oldHeight = rowHeight();
        float oldWidth = measureTool.measure('a');

        textPaint.setTextSize(fontSize);
        linePaint.setTextSize(fontSize);
        measureCache.invalidate();
        if (document.isWordWrapEnable()) {
            document.analyzeWordWrap();
        }
        caret.updateRow();
        float x = editor.getScrollX() * ((float) measureTool.measure('a') / oldWidth);
        float y = editor.getScrollY() * ((float) rowHeight() / oldHeight);
        editor.scrollTo((int) x, (int) y);
        editor.invalidate();

    }
}
