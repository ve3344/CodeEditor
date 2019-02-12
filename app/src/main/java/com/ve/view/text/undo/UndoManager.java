package com.ve.view.text.undo;

import java.util.LinkedList;

public class UndoManager implements Undo {
    private Undoable undoable;
    private LinkedList<Command> commands = new LinkedList<Command>();
    private boolean isBatchEdit = false;
    private int groupId = 0;
    private int top = 0;
    private long lastEditTime = -1;

    public UndoManager(Undoable undoable) {
        this.undoable = undoable;
    }

    //private
    private void push(Command c) {
        trimStack();
        ++top;
        commands.add(c);
    }

    private void trimStack() {
        while (commands.size() > top) {
            commands.removeLast();
        }
    }

    @Override
    public int undo() {
        if (canUndo()) {
            Command lastUndone = commands.get(top - 1);
            int group = lastUndone.group;
            do {
                Command c = commands.get(top - 1);
                if (c.group != group) {
                    break;
                }

                lastUndone = c;
                c.undo();
                --top;
            } while (canUndo());

            return lastUndone.findUndoPosition();
        }

        return -1;
    }

    @Override
    public int redo() {
        if (canRedo()) {
            Command lastRedone = commands.get(top);
            int group = lastRedone.group;
            do {
                Command c = commands.get(top);
                if (c.group != group) {
                    break;
                }

                lastRedone = c;
                c.redo();
                ++top;
            } while (canRedo());

            return lastRedone.findRedoPosition();
        }

        return -1;
    }

    @Override
    public final boolean canUndo() {
        return top > 0;
    }

    @Override
    public final boolean canRedo() {
        return top < commands.size();
    }

    @Override
    public boolean isBatchEdit() {
        return isBatchEdit;
    }

    @Override
    public void beginBatchEdit() {
        isBatchEdit = true;
    }

    @Override
    public void endBatchEdit() {
        isBatchEdit = false;
        groupId++;
    }

    public void captureInsert(int start, int length, long time) {
        boolean mergeSuccess = false;

        if (canUndo()) {
            Command c = commands.get(top - 1);

            if (c instanceof InsertCommand && c.merge(start, length, time)) {
                mergeSuccess = true;
            } else {
                c.recordData();
            }
        }

        if (!mergeSuccess) {
            push(new InsertCommand(start, length, groupId));

            if (!isBatchEdit) {
                groupId++;
            }
        }

        lastEditTime = time;
    }

    public void captureDelete(int start, int length, long time) {
        boolean mergeSuccess = false;

        if (canUndo()) {
            Command c = commands.get(top - 1);

            if (c instanceof DeleteCommand && c.merge(start, length, time)) {
                mergeSuccess = true;
            } else {
                c.recordData();
            }
        }

        if (!mergeSuccess) {
            push(new DeleteCommand(start, length, groupId));

            if (!isBatchEdit) {
                groupId++;
            }
        }

        lastEditTime = time;
    }


    private abstract class Command {
        public final static long MERGE_TIME = 1000000000; //750ms in nanoseconds

        public int start, length;

        public String data;

        public int group;

        public abstract void undo();

        public abstract void redo();

        public abstract void recordData();

        public abstract int findUndoPosition();

        public abstract int findRedoPosition();

        public abstract boolean merge(int start, int length, long time);
    }

    private class InsertCommand extends Command {

        public InsertCommand(int start, int length, int groupNumber) {
            this.start = start;
            this.length = length;
            group = groupNumber;
        }

        @Override
        public boolean merge(int newStart, int length, long time) {
            if (lastEditTime < 0) {
                return false;
            }

            if ((time - lastEditTime) < MERGE_TIME
                    && newStart == start + this.length) {
                this.length += length;
                trimStack();
                return true;
            }

            return false;
        }

        @Override
        public void recordData() {
            data = undoable.subSequence(start, length).toString();
        }

        @Override
        public void undo() {
            if (data == null) {
                recordData();
                undoable.shiftGapStart(-length);
            } else {
                //dummy timestamp of 0
                undoable.delete(start, length, 0, false);
            }
        }

        @Override
        public void redo() {
            //dummy timestamp of 0
            undoable.insert(data.toCharArray(), start, 0, false);
        }

        @Override
        public int findRedoPosition() {
            return start + length;
        }

        @Override
        public int findUndoPosition() {
            return start;
        }
    }


    private class DeleteCommand extends Command {

        public DeleteCommand(int start, int length, int seqNumber) {
            this.start = start;
            this.length = length;
            group = seqNumber;
        }

        @Override
        public boolean merge(int newStart, int length, long time) {
            if (lastEditTime < 0) {
                return false;
            }

            if ((time - lastEditTime) < MERGE_TIME
                    && newStart == start - this.length - length + 1) {
                start = newStart;
                this.length += length;
                trimStack();
                return true;
            }

            return false;
        }

        @Override
        public void recordData() {
            data = new String(undoable.subEditingSequence(length));
        }

        @Override
        public void undo() {
            if (data == null) {
                recordData();
                undoable.shiftGapStart(length);
            } else {
                //dummy timestamp of 0
                undoable.insert(data.toCharArray(), start, 0, false);
            }
        }

        @Override
        public void redo() {
            //dummy timestamp of 0
            undoable.delete(start, length, 0, false);
        }

        @Override
        public int findRedoPosition() {
            return start;
        }

        @Override
        public int findUndoPosition() {
            return start + length;
        }
    }
}
