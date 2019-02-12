package com.ve.view.editor.out;

import android.os.Parcel;
import android.os.Parcelable;

import com.ve.view.editor.Editor;

public class  Status implements Parcelable {
   public int caretPosition;
   public int scrollX, scrollY;
   public boolean selectMode;
   public int selectBegin, selectEnd;

    @Override
    public int describeContents() {
        return 0;
    }

    public Status(Editor editor) {
        caretPosition = editor.getCaretInterface().getPosition();
        scrollX = editor.getScrollX();
        scrollY = editor.getScrollY();
        selectMode = editor.getSelectInterface().isSelecting();
        selectBegin = editor.getSelectInterface().getSelectionStart();
        selectEnd = editor.getSelectInterface().getSelectionEnd();
    }

    private Status(Parcel in) {
        caretPosition = in.readInt();
        scrollX = in.readInt();
        scrollY = in.readInt();
        selectMode = in.readInt() != 0;
        selectBegin = in.readInt();
        selectEnd = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeInt(caretPosition);
        out.writeInt(scrollX);
        out.writeInt(scrollY);
        out.writeInt(selectMode ? 1 : 0);
        out.writeInt(selectBegin);
        out.writeInt(selectEnd);
    }

    public static final Creator<Status> CREATOR = new Creator<Status>() {
        @Override
        public Status createFromParcel(Parcel in) {
            return new Status(in);
        }

        @Override
        public Status[] newArray(int size) {
            return new Status[size];
        }
    };

}
