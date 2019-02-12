package com.ve.view.text.cache;

public class Entry {
    private int line,position;

    public Entry(int line, int position) {
        this.line = line;
        this.position = position;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }
}
