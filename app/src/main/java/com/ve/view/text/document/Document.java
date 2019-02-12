package com.ve.view.text.document;

import com.ve.view.utils.EditorException;

import java.util.ArrayList;

public class Document extends BaseDocument {
    private boolean isWordWrapEnable = false;

    private WordWrapable wordWrapable;

    private ArrayList<Integer> rowTable;

    public Document() {
        super();
        resetRowTable();
    }


    private void resetRowTable() {
        ArrayList<Integer> newRowTable = new ArrayList<Integer>();
        newRowTable.add(0);
        rowTable = newRowTable;
    }

    public void setWordWrapable(WordWrapable wordWrapable) {
        this.wordWrapable = wordWrapable;
    }

    public void setWordWrapEnable(boolean enable) {
        if (enable && !isWordWrapEnable) {
            isWordWrapEnable = true;
            analyzeWordWrap();
        } else if (!enable && isWordWrapEnable) {
            isWordWrapEnable = false;
            analyzeWordWrap();
        }
    }

    public boolean isWordWrapEnable() {
        return isWordWrapEnable;
    }


    @Override
    public synchronized void delete(int charOffset, int totalChars, long timestamp, boolean undoable) {
        super.delete(charOffset, totalChars, timestamp, undoable);

        int startRow = gainRowOfPosition(charOffset);
        int analyzeEnd = findNextLineFrom(charOffset);
        updateWordWrapAfterEdit(startRow, analyzeEnd, -totalChars);
    }

    @Override
    public synchronized void insert(char[] c, int charOffset, long timestamp, boolean undoable) {
        super.insert(c, charOffset, timestamp, undoable);

        int startRow = gainRowOfPosition(charOffset);
        int analyzeEnd = findNextLineFrom(charOffset + c.length);
        updateWordWrapAfterEdit(startRow, analyzeEnd, c.length);
    }

    @Override
    public synchronized void shiftGapStart(int displacement) {
        super.shiftGapStart(displacement);

        if (displacement != 0) {
            int startOffset = (displacement > 0)
                    ? gap.getStart() - displacement
                    : gap.getStart();
            int startRow = gainRowOfPosition(startOffset);
            int analyzeEnd = findNextLineFrom(gap.getStart());
            updateWordWrapAfterEdit(startRow, analyzeEnd, displacement);
        }
    }

    //No error checking is done on parameters.
    private int findNextLineFrom(int charOffset) {
        int lineEnd = gap.position2index(charOffset);

        while (lineEnd < contents.length) {
            // skip the gap
            if (lineEnd == gap.getStart()) {
                lineEnd = gap.getEnd();
            }

            if (contents[lineEnd] == CommonLanguage.NEWLINE || contents[lineEnd] == CommonLanguage.EOF) {
                break;
            }

            ++lineEnd;
        }

        return gap.index2position(lineEnd) + 1;
    }

    private void updateWordWrapAfterEdit(int startRow, int analyzeEnd, int delta) {
        if (startRow > 0) {
            --startRow;
        }
        int analyzeStart = rowTable.get(startRow);

        //changes only affect the rowTable after startRow
        removeRowMetadata(startRow + 1, analyzeEnd - delta);
        adjustOffsetOfRowsFrom(startRow + 1, delta);
        analyzeWordWrap(startRow + 1, analyzeStart, analyzeEnd);
    }

    private void removeRowMetadata(int fromRow, int endOffset) {
        while (fromRow < rowTable.size() && rowTable.get(fromRow) <= endOffset) {
            rowTable.remove(fromRow);
        }
    }

    private void adjustOffsetOfRowsFrom(int fromRow, int offset) {
        for (int i = fromRow; i < rowTable.size(); ++i) {
            rowTable.set(i, rowTable.get(i) + offset);
        }
    }

    public void analyzeWordWrap() {

        resetRowTable();

        if (isWordWrapEnable && !hasMinimumWidthForWordWrap()) {
            if (wordWrapable.getEditorWidth() > 0) {
                EditorException.fail("Text field has non-zero width but still too small for word wrap");
            }
            //没有绘制之前getRowWidth() 可能是0
            return;
        }

        analyzeWordWrap(1, 0, length());
    }

    private boolean hasMinimumWidthForWordWrap() {
        final int maxWidth = wordWrapable.getEditorWidth();
        return (maxWidth >= 2 * wordWrapable.measure('M'));
    }

    private void analyzeWordWrap(int rowIndex, int startPosition, int endPosition) {
        if (!isWordWrapEnable) {
            int startIndex = gap.position2index(startPosition);
            int end = gap.position2index(endPosition);
            ArrayList<Integer> rowTable = new ArrayList<Integer>();

            while (startIndex < end) {
                if (startIndex == gap.getStart()) {
                    startIndex = gap.getEnd();
                }
                char c = contents[startIndex];
                if (c == CommonLanguage.NEWLINE) {
                    rowTable.add(gap.index2position(startIndex) + 1);
                }
                ++startIndex;

            }
            this.rowTable.addAll(rowIndex, rowTable);
            return;
        }
        if (!hasMinimumWidthForWordWrap()) {
            EditorException.fail("Not enough space to do word wrap");
            return;
        }

        ArrayList<Integer> rowTable = new ArrayList<Integer>();
        int offset = gap.position2index(startPosition);
        int end = gap.position2index(endPosition);
        int potentialBreakPoint = startPosition;
        int wordExtent = 0;
        final int maxWidth = wordWrapable.getEditorWidth();
        int remainingWidth = maxWidth;

        while (offset < end) {
            // skip the gap
            if (offset == gap.getStart()) {
                offset = gap.getEnd();
            }

            char c = contents[offset];
            wordExtent += wordWrapable.measure(c);

            if (CommonLanguage.isWhitespace(c)) {
                //full word obtained
                if (wordExtent <= remainingWidth) {
                    remainingWidth -= wordExtent;
                } else if (wordExtent > maxWidth) {
                    //handle a word too long to fit on one row
                    int current = gap.position2index(potentialBreakPoint);
                    remainingWidth = maxWidth;

                    //start the word on a new row, if it isn't already
                    if (potentialBreakPoint != startPosition && (rowTable.isEmpty() ||
                            potentialBreakPoint != rowTable.get(rowTable.size() - 1))) {
                        rowTable.add(potentialBreakPoint);
                    }

                    while (current <= offset) {
                        // skip the gap
                        if (current == gap.getStart()) {
                            current = gap.getEnd();
                        }

                        int advance = wordWrapable.measure(contents[current]);
                        if (advance > remainingWidth) {
                            rowTable.add(gap.index2position(current));
                            remainingWidth = maxWidth - advance;
                        } else {
                            remainingWidth -= advance;
                        }

                        ++current;
                    }
                } else {
                    //invariant: potentialBreakPoint != startOffset
                    //put the word on a new row
                    rowTable.add(potentialBreakPoint);
                    remainingWidth = maxWidth - wordExtent;
                }

                wordExtent = 0;
                potentialBreakPoint = gap.index2position(offset) + 1;
            }

            if (c == CommonLanguage.NEWLINE) {
                //start a new row
                rowTable.add(potentialBreakPoint);
                remainingWidth = maxWidth;
            }

            ++offset;
        }

        //merge with existing row table
        this.rowTable.addAll(rowIndex, rowTable);
    }

    public String gainRowText(int rowNumber) {

        int rowSize = gainRowLength(rowNumber);
        if (rowSize == 0) {
            return new String();
        }

        int startIndex = rowTable.get(rowNumber);
        return subSequence(startIndex, rowSize).toString();
    }

    public int gainRowLength(int row) {

        if (!isValidRow(row)) {
            return 0;
        }

        if (row != (rowTable.size() - 1)) {
            return rowTable.get(row + 1) - rowTable.get(row);
        } else {
            return length() - rowTable.get(row);
        }
    }

    public int getRowCount() {
        return rowTable.size();
    }

    public int gainPositionOfRow(int row) {
        return isValidRow(row) ? rowTable.get(row) : -1;
    }

    public int gainRowOfPosition(int position) {

        if (!isValidPosition(position)) {
            return -1;
        }

        //binary search of rowTable
        int right = rowTable.size() - 1;
        int left = 0;
        while (right >= left) {
            int mid = (left + right) / 2;
            int nextLineOffset = ((mid + 1) < rowTable.size()) ? rowTable.get(mid + 1) : length();
            if (position >= rowTable.get(mid) && position < nextLineOffset) {
                return mid;
            }

            if (position >= nextLineOffset) {
                left = mid + 1;
            } else {
                right = mid - 1;
            }
        }

        //should not be here
        return -1;
    }

    public int getColumn(int position) {
        int row = gainRowOfPosition(position);
        EditorException.logIf(row < 0, "Invalid char offset given to getColumn");
        int firstCharOfRow = gainPositionOfRow(row);
        return position - firstCharOfRow;
    }

    protected boolean isValidRow(int row) {
        return row >= 0 && row < rowTable.size();
    }



}
