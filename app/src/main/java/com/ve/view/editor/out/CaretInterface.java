package com.ve.view.editor.out;

public interface CaretInterface {

    int getRow();

    int getPosition();

    boolean onFirstRow();

    boolean onLastRow();

    boolean onStart();

    boolean onEnd();

    void moveLeft();

    void moveRight();

    void moveDown();

    void moveUp();

    void moveRight(boolean isTyping);

    void moveLeft(boolean isTyping);
}
