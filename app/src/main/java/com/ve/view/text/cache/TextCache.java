package com.ve.view.text.cache;

public class TextCache {
    private static final int CACHE_SIZE = 4; // minimum = 1
    private Entry[] entries = new Entry[CACHE_SIZE];

    public TextCache() {
        entries[0] = new Entry(0, 0);
        for (int i = 1; i < CACHE_SIZE; ++i) {
            entries[i] = new Entry(-1, -1);
        }
    }

    public Entry getNearestEntryByLine(int line) {
        int nearestMatch = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < CACHE_SIZE; ++i) {
            int distance = Math.abs(line - entries[i].getLine());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMatch = i;
            }
        }

        Entry nearestEntry = entries[nearestMatch];
        makeHead(nearestMatch);
        return nearestEntry;
    }

    public Entry getNearestEntryByPosition(int position) {
        int nearestMatch = 0;
        int nearestDistance = Integer.MAX_VALUE;
        for (int i = 0; i < CACHE_SIZE; ++i) {
            int distance = Math.abs(position - entries[i].getPosition());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestMatch = i;
            }
        }

        Entry nearestEntry = entries[nearestMatch];
        makeHead(nearestMatch);
        return nearestEntry;
    }


    private void makeHead(int newHead) {
        if (newHead != 0) {
            Entry temp = entries[newHead];
            for (int i = newHead; i > 1; --i) {
                entries[i] = entries[i - 1];
            }
            entries[1] = temp;
        }

    }

    public void updateEntry(int line, int position) {
        if (line > 0) {
            //update
            for (int i = 1; i < CACHE_SIZE; ++i) {
                if (entries[i].getLine() == line) {
                    entries[i].setPosition(position);
                    return;
                }
            }
            //insert
            makeHead(CACHE_SIZE - 1);
            entries[1] = new Entry(line, position);

        }

    }


    public void invalidateEntriesFrom(int position) {
        for (int i = 1; i < CACHE_SIZE; ++i) {
            if (entries[i].getPosition() >= position) {
                entries[i] = new Entry(-1, -1);
            }
        }
    }
}
