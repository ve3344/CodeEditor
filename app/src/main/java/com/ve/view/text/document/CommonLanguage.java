package com.ve.view.text.document;

public class CommonLanguage {
    public final static char EOF = '\uFFFF';
    public final static char NULL_CHAR = '\u0000';
    public final static char NEWLINE = '\n';
    public final static char BACKSPACE = '\b';
    public static final char SPACE = ' ';
    public final static char TAB = '\t';
    public final static String GLYPH_NEWLINE = "\u21b5";
    public final static String GLYPH_SPACE = "\u00b7";
    public final static String GLYPH_TAB = "\u00bb";
    public final static char EMOJI1 = 0xd83c;
    public final static char EMOJI2 = 0xd83d;

    public static boolean isWhitespace(char c) {
        return c == SPACE || c == NEWLINE || c == TAB || c == '\r' || c == '\f' || c == EOF;
    }
    public static boolean isEmoji(char c) {
        return c == EMOJI1 || c == EMOJI2 ;
    }

}
