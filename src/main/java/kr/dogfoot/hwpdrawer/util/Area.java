package kr.dogfoot.hwpdrawer.util;

/**
 * 사각형 영역을 나타내는 클래스.
 * 모든 값은 HWP 단위(1/7200 인치)로 저장된다.
 */
public class Area {
    private long left;
    private long top;
    private long right;
    private long bottom;

    public Area() {
        this(0, 0, 0, 0);
    }

    public Area(long left, long top, long right, long bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
    }

    public long left() { return left; }
    public Area left(long left) { this.left = left; return this; }

    public long top() { return top; }
    public Area top(long top) { this.top = top; return this; }

    public long right() { return right; }
    public Area right(long right) { this.right = right; return this; }

    public long bottom() { return bottom; }
    public Area bottom(long bottom) { this.bottom = bottom; return this; }

    public long width() { return right - left; }
    public long height() { return bottom - top; }

    public Area set(long left, long top, long right, long bottom) {
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        return this;
    }

    public Area copyFrom(Area other) {
        this.left = other.left;
        this.top = other.top;
        this.right = other.right;
        this.bottom = other.bottom;
        return this;
    }

    public Area clone() {
        return new Area(left, top, right, bottom);
    }
}
