package com.ve.view.listener;

public interface SelectionListener {
	 void onSelectionChanged(boolean active, int selStart, int selEnd);
	 class SelectionAdapter implements SelectionListener {
		@Override
		public void onSelectionChanged(boolean active, int selStart, int selEnd) {

		}
	}
}
