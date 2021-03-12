package kr.dogfoot.hwplib.drawer.util;

import java.awt.*;

public class Area {
    private long left;
    private long top;
    private long right;
    private long bottom;

    public Area(long left, long top, long right, long bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public Area(Area other) {
        this.left = other.left;
        this.top = other.top;
        this.right = other.right;
        this.bottom = other.bottom;
    }

    public long left() {
        return left;
    }

    public Area left(long left) {
        this.left = left;
        return this;
    }

    public long top() {
        return top;
    }

    public Area top(long top) {
        this.top = top;
        return this;
    }

    public long right() {
        return right;
    }

    public Area right(long right) {
        this.right = right;
        return this;
    }

    public long bottom() {
        return bottom;
    }

    public Area bottom(long bottom) {
        this.bottom = bottom;
        return this;
    }

    public long width() {
        return right - left;
    }

    public long height() {
        return bottom - top;
    }

    public Area applyMargin(long left, long top, long right, long bottom) {
        this.left += left;
        this.top += top;
        this.right -= right;
        this.bottom -= bottom;
        return this;
    }

    public Area moveX(long offsetX) {
        left += offsetX;
        right += offsetX;
        return this;
    }

    public Area moveY(long offsetY) {
        top += offsetY;
        bottom += offsetY;
        return this;
    }

    public Rectangle toConvertedRectangle() {
        Rectangle rectangle = new Rectangle();
        rectangle.x = Convertor.fromHWPUnit(left);
        rectangle.y = Convertor.fromHWPUnit(top);
        rectangle.width = Convertor.fromHWPUnit(width());
        rectangle.height = Convertor.fromHWPUnit(height());
        return rectangle;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("l:")
                .append(left)
                .append(", t:")
                .append(top)
                .append(", r:")
                .append(right)
                .append(", b:")
                .append(bottom);
        return sb.toString();
    }

}