package com.ve.view.editor;

import android.graphics.Paint;

import com.ve.view.editor.out.Config;
import com.ve.view.text.document.CommonLanguage;

import java.util.HashMap;

public class MeasureCache {
    private HashMap<Character, Float> cache;
    private Paint paint;

    private int rowHeight;

    private int spaceWidth;
    private int tabWidth;
    private int newlineWidth;
    private Config config;

    public MeasureCache(Paint paint) {
        this.paint = paint;
        this.cache = new HashMap<>();
    }


    public int getRowHeight() {
        return rowHeight;
    }

    public int getSpaceWidth() {
        return spaceWidth;
    }

    public int getTabWidth() {
        return tabWidth;
    }

    public int getNewlineWidth() {
        return newlineWidth;
    }


    public void setConfig(Config config) {
        this.config = config;
        invalidate();
    }


    private float measureFromCache(char c) {

        Float value = cache.get(c);
        if (value == null) {
            value = measure(c);
            cache.put(c, value);
        }
        return value;
    }

    private float measure(char c) {
        return paint.measureText(new char[]{c}, 0, 1);
    }

    private float measure(String s) {
        return paint.measureText(s, 0, s.length());
    }

    private float measure(char c1, char c2) {
        return paint.measureText(new char[]{c1, c2}, 0, 2);
    }

    public void invalidate() {
        cache.clear();
        reMeasure();
    }

    private void reMeasure() {
        if (config.showNonPrinting) {
            spaceWidth = (int) measure(CommonLanguage.GLYPH_SPACE);
            newlineWidth = (int) measure(CommonLanguage.GLYPH_NEWLINE);
            tabWidth = (int) measure(CommonLanguage.GLYPH_TAB);

        } else {
            spaceWidth = (int) measure(CommonLanguage.SPACE);
            newlineWidth = (int) measure(CommonLanguage.SPACE);
            tabWidth = spaceWidth *config. tabLength;
        }
        rowHeight = (int) (paint.descent()-paint.ascent());
    }


    public MeasureTool getNewMeasureTool() {
        return new MeasureTool();
    }

    class MeasureTool {
        private char emojiFlag;
        private boolean justEmoji;//上一个是Emoji

        public boolean isJustEmoji() {
            return justEmoji;
        }

        public MeasureTool() {
            reset();
        }

        public void reset() {
            emojiFlag = CommonLanguage.NULL_CHAR;
            justEmoji = false;
        }

        public int measure(char c) {
            int width;

            justEmoji = false;
            switch (c) {
                case CommonLanguage.EMOJI1:
                case CommonLanguage.EMOJI2:
                    width = 0;
                    emojiFlag = c;
                    break;
                case CommonLanguage.SPACE:
                    width = spaceWidth;
                    break;
                case CommonLanguage.NEWLINE:
                case CommonLanguage.EOF:
                    width = newlineWidth;
                    break;
                case CommonLanguage.TAB:
                    width = tabWidth;
                    break;
                default:
                    if (emojiFlag != CommonLanguage.NULL_CHAR) {
                        width = (int) MeasureCache.this.measure(emojiFlag, c);
                        justEmoji = true;
                        emojiFlag = CommonLanguage.NULL_CHAR;
                    } else {
                        if (config.cacheEnable) {
                            width = (int) MeasureCache.this.measureFromCache(c);
                        } else {
                            width = (int) MeasureCache.this.measure(c);
                        }

                    }
                    break;
            }

            return width;
        }


    }
}
