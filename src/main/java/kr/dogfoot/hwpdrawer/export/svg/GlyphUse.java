package kr.dogfoot.hwpdrawer.export.svg;

import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * 글자 하나를 {@code <use>}로 그리라는 표식 — 넓이 없는 도형이다. {@link ReusedGlyphGraphics2D}가 글자 자리에 이 도형을
 * 채우고, {@link GlyphSvgGraphics2D}가 받아 {@code <use href x y>}를 그 순간의 색·변환·클립 그룹에 넣는다.
 * 그 사이의 래퍼(그림 래퍼 등)는 도형을 그대로 넘긴다.
 */
final class GlyphUse implements Shape {
    private static final Shape NOTHING = new Path2D.Float();

    private final String definitionId;
    private final double x;
    private final double y;

    /**
     * @param definitionId 글자 윤곽선 정의의 id ({@link GlyphDefinitions})
     * @param x 글자 원점의 x (현재 사용자 좌표)
     * @param y 글자 원점의 y (기준선)
     */
    GlyphUse(String definitionId, double x, double y) {
        this.definitionId = definitionId;
        this.x = x;
        this.y = y;
    }

    String definitionId() { return definitionId; }
    double x() { return x; }
    double y() { return y; }

    // ───── 넓이 없는 도형 ─────

    @Override public Rectangle getBounds() { return NOTHING.getBounds(); }
    @Override public Rectangle2D getBounds2D() { return NOTHING.getBounds2D(); }
    @Override public boolean contains(double px, double py) { return false; }
    @Override public boolean contains(Point2D p) { return false; }
    @Override public boolean intersects(double rx, double ry, double w, double h) { return false; }
    @Override public boolean intersects(Rectangle2D r) { return false; }
    @Override public boolean contains(double rx, double ry, double w, double h) { return false; }
    @Override public boolean contains(Rectangle2D r) { return false; }
    @Override public PathIterator getPathIterator(AffineTransform at) { return NOTHING.getPathIterator(at); }
    @Override public PathIterator getPathIterator(AffineTransform at, double flatness) {
        return NOTHING.getPathIterator(at, flatness);
    }
}
