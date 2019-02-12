package com.ve.view.editor.out;

public interface DisplayInterface {
    boolean scrollToPosition(int position);

    void scrollToSelectionStart();

    void scrollToSelectionEnd();

    void scroll(float dx, float dy);

    void smoothScrollTo(int x, int y);

    void flingScroll(int velocityX, int velocityY);

    boolean isFlingScrolling();

    void stopFlingScrolling();

    void smoothScroll(int dx, int dy);

    void scrollToCaret();

    int screenToViewX(int x);

    int screenToViewY(int y);
}
