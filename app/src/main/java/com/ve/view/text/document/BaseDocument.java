package com.ve.view.text.document;

import com.ve.view.ext.AutoIndent;
import com.ve.view.text.cache.Entry;
import com.ve.view.text.cache.TextCache;
import com.ve.view.text.undo.UndoManager;
import com.ve.view.text.undo.Undoable;

import static com.ve.view.text.document.Gap.MIN_GAP_SIZE;

public class BaseDocument implements Undoable, CharSequence, DocumentEdit {
    protected int nextCapacity;
    protected int lineCount;
    protected char[] contents;
    protected Gap gap;
    protected TextCache cache;
    protected UndoManager undo;
//    protected List<Pair> spans;


    public BaseDocument() {
        contents = new char[MIN_GAP_SIZE + 1]; // extra char for EOF
        contents[MIN_GAP_SIZE] = CommonLanguage.EOF;
        gap = new Gap(contents);
        nextCapacity = 1;
        lineCount = 1;
        cache = new TextCache();
        undo = new UndoManager(this);
    }


    ///static
    protected static int calculateCapacity(int textSize) {
        long bufferSize = textSize + MIN_GAP_SIZE + 1; // extra char for EOF
        if (bufferSize < Integer.MAX_VALUE) {
            return (int) bufferSize;
        }
        return -1;
    }

    ///private

    protected synchronized void setContentData(char[] data, int dataLength, int lineCount) {
        contents = data;
        gap.onContentChange(contents);
        this.lineCount = lineCount;
        nextCapacity = 1;


        int toPosition = contents.length - 1;
        contents[toPosition--] = CommonLanguage.EOF; // mark end of file
        int fromPosition = dataLength - 1;
        while (fromPosition >= 0) {
            contents[toPosition--] = contents[fromPosition--];
        }
        gap.reset(toPosition + 1);
    }

    private int countNewline(int start, int count) {
        int newlines = 0;
        for (int i = start; i < (start + count); ++i) {
            if (contents[i] == CommonLanguage.NEWLINE) {
                ++newlines;
            }
        }

        return newlines;
    }


    protected void growBufferBy(int minIncrement) {
        int increasedSize = minIncrement + MIN_GAP_SIZE * nextCapacity;
        char[] temp = new char[contents.length + increasedSize];
        int i = 0;
        while (i < gap.getStart()) {
            temp[i] = contents[i];
            ++i;
        }

        i = gap.getEnd();
        while (i < contents.length) {
            temp[i + increasedSize] = contents[i];
            ++i;
        }

        gap.moveEnd(increasedSize);
        contents = temp;
        gap.onContentChange(contents);
        nextCapacity <<= 1;
    }

    ///public


    public synchronized void setText(char[] text) {
        int lineCount = 1;
        int len = text.length;
        for (int i = 0; i < len; i++) {
            if (text[i] == '\n')
                lineCount++;
        }
        setContentData(text, len, lineCount);
    }

    public synchronized void setText(CharSequence text) {
        int lineCount = 1;
        int len = text.length();
        char[] ca = new char[BaseDocument.calculateCapacity(len)];
        for (int i = 0; i < len; i++) {
            ca[i] = text.charAt(i);
            if (text.charAt(i) == '\n')
                lineCount++;
        }
        setContentData(ca, len, lineCount);
    }


    public synchronized int gainPositionOfLine(int targetLine) {
        if (targetLine < 0) {
            return -1;
        }
        int targetPosition = -1;


        Entry cachedEntry = cache.getNearestEntryByLine(targetLine);


        int currentIndex = gap.position2index(cachedEntry.getPosition());
        int currentLine = cachedEntry.getLine();

        if (targetLine > currentLine) {
            while ((currentLine < targetLine) && (currentIndex < contents.length)) {
                if (contents[currentIndex] == CommonLanguage.NEWLINE) {
                    ++currentLine;
                }
                ++currentIndex;

                if (currentIndex == gap.getStart()) {
                    currentIndex = gap.getEnd();
                }
            }

            if (currentLine != targetLine) {
                targetPosition = -1;
            } else {
                targetPosition = gap.index2position(currentIndex);
            }

        } else if (targetLine < currentLine) {
            if (targetLine == 0) {
                targetPosition = 0;
            } else {
                while (currentLine > (targetLine - 1) && currentIndex >= 0) {
                    if (currentIndex == gap.getEnd()) {
                        currentIndex = gap.getStart();
                    }
                    --currentIndex;

                    if (contents[currentIndex] == CommonLanguage.NEWLINE) {
                        --currentLine;
                    }

                }

                targetPosition = currentIndex >= 0 ? gap.index2position(currentIndex) + 1 : -1;
            }

        } else {
            targetPosition = cachedEntry.getPosition();

        }

        if (targetPosition >= 0) {
            cache.updateEntry(targetLine, targetPosition);
        }

        return targetPosition;


    }

    public synchronized int gainLineOfPosition(int position) {
        if (!isValidPosition(position)) {
            return -1;
        }

        Entry cachedEntry = cache.getNearestEntryByPosition(position);
        int line = cachedEntry.getLine();
        int offset = gap.position2index(cachedEntry.getPosition());

        int targetOffset = gap.position2index(position);
        int lastKnownLine = -1;
        int lastKnownCharOffset = -1;

        if (targetOffset > offset) {
            while ((offset < targetOffset) && (offset < contents.length)) {
                if (contents[offset] == CommonLanguage.NEWLINE) {
                    ++line;
                    lastKnownLine = line;
                    lastKnownCharOffset = gap.index2position(offset) + 1;
                }

                ++offset;
                if (offset == gap.getStart()) {
                    offset = gap.getEnd();
                }
            }
        } else if (targetOffset < offset) {
            while ((offset > targetOffset) && (offset > 0)) {
                if (offset == gap.getEnd()) {
                    offset = gap.getStart();
                }
                --offset;

                if (contents[offset] == CommonLanguage.NEWLINE) {
                    lastKnownLine = line;
                    lastKnownCharOffset = gap.index2position(offset) + 1;
                    --line;
                }
            }
        }


        if (offset == targetOffset) {
            if (lastKnownLine != -1) {
                cache.updateEntry(lastKnownLine, lastKnownCharOffset);
            }
            return line;
        } else {
            return -1;
        }
    }


    public synchronized String gainLineText(int line) {
        int startIndex = gainPositionOfLine(line);

        if (startIndex < 0) {
            return new String();
        }
        int lineSize = gainLineLength(line);

        return subSequence(startIndex, lineSize).toString();
    }

    public synchronized int gainLineLength(int lineNumber) {
        int lineLength = 0;
        int pos = gainPositionOfLine(lineNumber);

        if (pos > 0) {
            pos = gap.position2index(pos);
            while (contents[pos] != CommonLanguage.NEWLINE && contents[pos] != CommonLanguage.EOF) {//注意没有设置EOF
                ++lineLength;
                ++pos;

                if (pos == gap.getStart()) {
                    pos = gap.getEnd();
                }
            }
            ++lineLength; // account for the line terminator char
        }

        return lineLength;
    }

    public synchronized int getLineCount() {
        return lineCount;
    }

    public synchronized char getCharAt(int charOffset) {
        return contents[gap.position2index(charOffset)];
    }

    public char[] createAutoIndent(int position,int autoIndentWidth) {
        int lineNum = gainLineOfPosition(position);
        int startOfLine = gainPositionOfLine(lineNum);
        int whitespaceCount = 0;

        Seeker seeker = this.getNewSeeker();
        seeker.seekPosition(startOfLine);
        while (seeker.hasNext()) {
            char c = seeker.next();
            if ((c != CommonLanguage.SPACE && c != CommonLanguage.TAB) || startOfLine + whitespaceCount >= position) {
                break;
            }
            ++whitespaceCount;
        }

        whitespaceCount += autoIndentWidth * AutoIndent.createAutoIndent(subSequence(startOfLine, position - startOfLine));
        if (whitespaceCount < 0)
            return new char[]{CommonLanguage.NEWLINE};

        char[] indent = new char[1 + whitespaceCount];
        indent[0] = CommonLanguage.NEWLINE;

        seeker.seekPosition(startOfLine);
        for (int i = 0; i < whitespaceCount; ++i) {
            indent[1 + i] = CommonLanguage.SPACE;
        }
        return indent;
    }

    @Override
    public int length() {
        return contents.length - gap.size();
    }


    @Override
    public char charAt(int position) {
        if (isValidPosition(position)) {
            return getCharAt(position);
        } else {
            return CommonLanguage.NULL_CHAR;
        }
    }


    @Override
    public synchronized String subSequence(int position, int count) {
        if (!isValidPosition(position) || count <= 0) {
            return new String();
        }
        int totalChars = count;
        if ((position + totalChars) > length()) {
            totalChars = length() - position;
        }
        int realIndex = gap.position2index(position);
        char[] chars = new char[totalChars];

        for (int i = 0; i < totalChars; ++i) {
            chars[i] = contents[realIndex];
            ++realIndex;
            // skip the gap
            if (realIndex == gap.getStart()) {
                realIndex = gap.getEnd();
            }
        }

        return new String(chars);
    }

    @Override
    public char[] subEditingSequence(int count) {
        char[] data = new char[count];

        for (int i = 0; i < count; ++i) {
            data[i] = contents[gap.getStart() + i];
        }

        return data;
    }

    public synchronized void insert(char[] c, int charOffset, long timestamp, boolean undoable) {
        if (undoable) {
            undo.captureInsert(charOffset, c.length, timestamp);
        }

        int insertIndex = gap.position2index(charOffset);

        // shift gap to insertion point
        if (insertIndex != gap.getEnd()) {
            if (gap.isBeforeGap(insertIndex)) {
                gap.shiftGapLeft(insertIndex);
            } else {
                gap.shiftGapRight(insertIndex);
            }
        }

        if (c.length >= gap.size()) {
            growBufferBy(c.length - gap.size());
        }

        for (int i = 0; i < c.length; ++i) {
            if (c[i] == CommonLanguage.NEWLINE) {
                ++lineCount;
            }
            contents[gap.getStart()] = c[i];
            gap.moveStart(1);
        }

        cache.invalidateEntriesFrom(charOffset);
    }

    public synchronized void delete(int charOffset, int totalChars, long timestamp, boolean undoable) {
        if (undoable) {
            undo.captureDelete(charOffset, totalChars, timestamp);
        }

        int newGapStart = charOffset + totalChars;

        // shift gap to deletion point
        if (newGapStart != gap.getStart()) {
            if (gap.isBeforeGap(newGapStart)) {
                gap.shiftGapLeft(newGapStart);
            } else {
                gap.shiftGapRight(newGapStart + gap.size());
            }
        }

        // increase gap size
        for (int i = 0; i < totalChars; ++i) {
            gap.moveStart(-1);
            if (contents[gap.getStart()] == CommonLanguage.NEWLINE) {
                --lineCount;
            }
        }

        cache.invalidateEntriesFrom(charOffset);
    }

    @Override
    public synchronized void shiftGapStart(int displacement) {
        if (displacement >= 0) {
            lineCount += countNewline(gap.getStart(), displacement);
        } else {
            lineCount -= countNewline(gap.getStart() + displacement, -displacement);
        }

        gap.moveStart(displacement);
        cache.invalidateEntriesFrom(gap.index2position(gap.getStart() - 1) + 1);
    }


    public final synchronized boolean isValidPosition(int position) {
        return (position >= 0 && position < length());
    }

    ////span

//    public void clearSpans() {
//        spans = new Vector<Pair>();
//        spans.add(new Pair(0, Lexer.NORMAL));
//    }
//
//    public List<Pair> getSpans() {
//        return spans;
//    }
//
//    public void setSpans(List<Pair> spans) {
//        this.spans = spans;
//    }


    //undo

    public boolean isBatchEdit() {
        return undo.isBatchEdit();
    }

    public void beginBatchEdit() {
        undo.beginBatchEdit();
    }


    public void endBatchEdit() {
        undo.endBatchEdit();
    }

    public boolean canUndo() {
        return undo.canUndo();
    }

    public boolean canRedo() {
        return undo.canRedo();
    }

    public int undo() {
        return undo.undo();
    }

    public int redo() {
        return undo.redo();
    }

    ///
    @Override
    public void insertBefore(char c, int insertionPoint, long timestamp) {
        if (!isValidPosition(insertionPoint)) {
            return;
        }

        char[] a = new char[1];
        a[0] = c;
        insert(a, insertionPoint, timestamp, true);
    }

    @Override
    public void insertBefore(char[] cArray, int insertionPoint, long timestamp) {
        if (!isValidPosition(insertionPoint) || cArray.length == 0) {
            return;
        }

        insert(cArray, insertionPoint, timestamp, true);
    }

    @Override
    public void deleteAt(int deletionPoint, long timestamp) {
        if (!isValidPosition(deletionPoint)) {
            return;
        }
        delete(deletionPoint, 1, timestamp, true);
    }


    @Override
    public void deleteAt(int deletionPoint, int maxChars, long time) {
        if (!isValidPosition(deletionPoint) || maxChars <= 0) {
            return;
        }
        int totalChars = Math.min(maxChars, length() - deletionPoint);
        delete(deletionPoint, totalChars, time, true);
    }


    ///
    @Override
    public String toString() {
        int len = length();
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < len; i++) {
            char c = getCharAt(i);
            if (c == CommonLanguage.EOF)
                break;
            buf.append(c);
        }
        return new String(buf);
    }

    public Seeker getNewSeeker() {
        return new Seeker();
    }

    public class Seeker {
        private int position;

        public int seekPosition(int index) {
            return this.position = (isValidPosition(index) ? index : -1);
        }

        public boolean hasNext() {
            return position >= 0 && position < length();
        }

        public char next() {
            return charAt(position++);
        }
    }
}
