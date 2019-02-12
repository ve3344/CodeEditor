package com.ve.view.listener;

public interface RowListener {
    void onRowChange(int newRowIndex);

    class RowAdapter implements RowListener {
        @Override
        public void onRowChange(int newRowIndex) {

        }
    }
}
