package com.ve.view.text.document;

public class Gap {
    protected final static int MIN_GAP_SIZE = 64;

    private int start, end;
    private char[] contents;

    public Gap(char[] contents) {
        this.contents=contents;
        start = 0;
        end = MIN_GAP_SIZE;
    }

    public void move(int distance) {
        start += distance;
        end += distance;
    }

    public void moveStart(int distance) {
        start += distance;
    }

    public void moveEnd(int distance) {
        end += distance;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public int getEnd() {
        return end;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    protected final int size() {
        return end - start;
    }

    protected final int position2index(int position) {
        if (isBeforeGap(position)) {
            return position;
        } else {
            return position + size();
        }
    }

    protected final int index2position(int index) {
        if (isBeforeGap(index)) {
            return index;
        } else {
            return index - size();
        }
    }

    protected final boolean isBeforeGap(int index) {
        return index < start;
    }


    public void reset(int capacity) {
        start = 0;
        end = capacity;
    }

    final protected void shiftGapLeft(int newGapStart) {
        while (start > newGapStart) {

            start--;
            end--;
            contents[end] = contents[start];
        }
    }

    final protected void shiftGapRight(int newGapEnd) {
        while (end < newGapEnd) {
            contents[start] = contents[end];
            start++;
            end++;
        }
    }

    public void onContentChange(char[] contents) {
        this.contents=contents;
    }
}