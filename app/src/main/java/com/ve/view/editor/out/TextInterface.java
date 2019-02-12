package com.ve.view.editor.out;

import com.ve.view.text.document.Document;

public interface TextInterface {


    void replaceText(int position, int count, String text);

    int getLength();

    boolean isValidPosition(int position);

    int getRowCount();

    void setDocument(Document document);

    Document getDocument();

    void setEdited(boolean set);

    boolean isEdited();

    void undo();

    void redo();

    void cut();

    void copy();

    void paste();

    void paste(String text);
}
