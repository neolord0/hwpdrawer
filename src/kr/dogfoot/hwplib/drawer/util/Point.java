package kr.dogfoot.hwplib.drawer.util;

public class Point {
    private long x;
    private long y;

    public Point(long x, long y) {
        this.x = x;
        this.y = y;
    }

    public long x() {
        return x;
    }

    public Point x(long x) {
        this.x = x;
        return this;
    }

    public long y() {
        return y;
    }

    public Point y(long y) {
        this.y = y;
        return this;
    }

    public Point moveX(long offsetX) {
        x += offsetX;
        return this;
    }

    public Point moveY(long offsetY) {
        y += offsetY;
        return this;
    }

    public Point toConvertedPoint() {
        return new Point(UnitConvertor.fromHWPUnit(x), UnitConvertor.fromHWPUnit(y));
    }
}
