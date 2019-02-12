package com.ve.view.utils;

import android.graphics.Rect;
import android.graphics.RectF;

public class Rectangle {
    public int x;//left
    public int y;//top
    public int width;
    public int height;

    public Rectangle() {
    }
    public static void main(String[] args) {
        Rectangle rectangle=new Rectangle(3,4,8,8);
        System.out.println(rectangle);
        System.out.println(rectangle.right());
        System.out.println(rectangle.bottom());
    }

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }


    public RectF toRectF() {
        return new RectF(x, y, right(), bottom());
    }

    public Rect toRect() {
        return new Rect(x, y, right(), bottom());
    }

    public boolean intersects(Rectangle bounds) {
        return this.x >= bounds.x && this.x < bounds.x + bounds.width && this.y >= bounds.y && this.y < bounds.y + bounds.height;
    }

    public boolean contains(int x, int y) {
        return x >= this.x && x < this.x + this.width && y >= this.y && y < this.y + this.height;
    }

    public int getCenterX() {
        return this.x + this.width / 2;
    }

    public int getCenterY() {
        return this.y + this.height / 2;
    }

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public String toString() {
        return String.format("(%d,%d,%d,%d)\n",x,y,width,height);
    }
}
