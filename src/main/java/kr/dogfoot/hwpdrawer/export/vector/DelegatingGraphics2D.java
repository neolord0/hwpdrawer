package kr.dogfoot.hwpdrawer.export.vector;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

/**
 * 모든 호출을 대상 Graphics2D에 그대로 넘기는 래퍼의 기반 클래스.
 *
 * <p>벡터 형식(SVG/PDF)용 Graphics2D 구현에 렌더러가 바라는 동작을 덧붙일 때, 하위 클래스는 바꿀
 * 메서드만 재정의한다. 렌더러(ImageRenderer 등)는 손대지 않으므로 PNG 출력에는 영향이 없다.</p>
 */
public abstract class DelegatingGraphics2D extends Graphics2D {
    protected final Graphics2D target;

    protected DelegatingGraphics2D(Graphics2D target) {
        this.target = target;
    }

    /** {@link #create()}가 만든 하위 Graphics도 같은 래퍼로 감싼다. */
    protected abstract DelegatingGraphics2D wrap(Graphics2D newTarget);

    @Override
    public Graphics create() {
        return wrap((Graphics2D) target.create());
    }

    // ───── 글자 ─────

    @Override public void drawString(String text, int x, int y) { target.drawString(text, x, y); }
    @Override public void drawString(String text, float x, float y) { target.drawString(text, x, y); }
    @Override public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        target.drawString(iterator, x, y);
    }
    @Override public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        target.drawString(iterator, x, y);
    }
    @Override public void drawGlyphVector(GlyphVector glyphs, float x, float y) {
        target.drawGlyphVector(glyphs, x, y);
    }
    @Override public FontRenderContext getFontRenderContext() { return target.getFontRenderContext(); }
    @Override public FontMetrics getFontMetrics(Font font) { return target.getFontMetrics(font); }
    @Override public void setFont(Font font) { target.setFont(font); }
    @Override public Font getFont() { return target.getFont(); }

    // ───── 그림 ─────

    @Override public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
        return target.drawImage(img, xform, obs);
    }
    @Override public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
        target.drawImage(img, op, x, y);
    }
    @Override public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
        target.drawRenderedImage(img, xform);
    }
    @Override public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
        target.drawRenderableImage(img, xform);
    }
    @Override public boolean drawImage(Image img, int x, int y, ImageObserver obs) {
        return target.drawImage(img, x, y, obs);
    }
    @Override public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver obs) {
        return target.drawImage(img, x, y, width, height, obs);
    }
    @Override public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver obs) {
        return target.drawImage(img, x, y, bgcolor, obs);
    }
    @Override public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor,
                                       ImageObserver obs) {
        return target.drawImage(img, x, y, width, height, bgcolor, obs);
    }
    @Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                                       int sx1, int sy1, int sx2, int sy2, ImageObserver obs) {
        return target.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, obs);
    }
    @Override public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2,
                                       int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver obs) {
        return target.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, obs);
    }

    // ───── 도형 ─────

    @Override public void draw(Shape shape) { target.draw(shape); }
    @Override public void fill(Shape shape) { target.fill(shape); }
    @Override public boolean hit(Rectangle rect, Shape shape, boolean onStroke) {
        return target.hit(rect, shape, onStroke);
    }
    @Override public void drawLine(int x1, int y1, int x2, int y2) { target.drawLine(x1, y1, x2, y2); }

    /**
     * Graphics의 기본 drawRect는 drawLine 네 번(래스터용)이라, 벡터 형식에서는 변마다 점선 위상이 다시 시작되고
     * 모서리가 이어지지 않는다 (SVG 생성기도 drawRect를 따로 구현하지 않는다). PNG(SunGraphics2D)처럼 닫힌
     * 사각형 하나로 그린다.
     *
     * <p>폭이나 높이가 0이면(선처럼 쓰는 사각형 — 조직도 연결선 등) 닫힌 경로로 넘긴다. Java2D·PDF는 넓이 0인
     * 사각형 테두리도 선으로 그리지만, SVG 생성기(Batik)는 Rectangle을 {@code <rect>}로 쓰고 SVG는 넓이 0인
     * {@code <rect>}를 아예 그리지 않는다. {@code <path>}의 선은 그린다.</p>
     */
    @Override public void drawRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) return;
        target.draw(width == 0 || height == 0
                ? closedRectanglePath(x, y, width, height)
                : new Rectangle(x, y, width, height));
    }

    private static Shape closedRectanglePath(int x, int y, int width, int height) {
        Path2D.Float path = new Path2D.Float();
        path.moveTo(x, y);
        path.lineTo(x + width, y);
        path.lineTo(x + width, y + height);
        path.lineTo(x, y + height);
        path.closePath();
        return path;
    }
    @Override public void fillRect(int x, int y, int width, int height) { target.fillRect(x, y, width, height); }
    @Override public void clearRect(int x, int y, int width, int height) { target.clearRect(x, y, width, height); }
    @Override public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        target.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
    }
    @Override public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        target.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
    }
    @Override public void drawOval(int x, int y, int width, int height) { target.drawOval(x, y, width, height); }
    @Override public void fillOval(int x, int y, int width, int height) { target.fillOval(x, y, width, height); }
    @Override public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        target.drawArc(x, y, width, height, startAngle, arcAngle);
    }
    @Override public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        target.fillArc(x, y, width, height, startAngle, arcAngle);
    }
    @Override public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
        target.drawPolyline(xPoints, yPoints, nPoints);
    }
    @Override public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        target.drawPolygon(xPoints, yPoints, nPoints);
    }
    @Override public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        target.fillPolygon(xPoints, yPoints, nPoints);
    }
    @Override public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        target.copyArea(x, y, width, height, dx, dy);
    }

    // ───── 상태 ─────

    @Override public GraphicsConfiguration getDeviceConfiguration() { return target.getDeviceConfiguration(); }
    @Override public void setComposite(Composite comp) { target.setComposite(comp); }
    @Override public Composite getComposite() { return target.getComposite(); }
    @Override public void setPaint(Paint paint) { target.setPaint(paint); }
    @Override public Paint getPaint() { return target.getPaint(); }
    @Override public void setStroke(Stroke stroke) { target.setStroke(stroke); }
    @Override public Stroke getStroke() { return target.getStroke(); }
    @Override public void setColor(Color color) { target.setColor(color); }
    @Override public Color getColor() { return target.getColor(); }
    @Override public void setBackground(Color color) { target.setBackground(color); }
    @Override public Color getBackground() { return target.getBackground(); }
    @Override public void setPaintMode() { target.setPaintMode(); }
    @Override public void setXORMode(Color color) { target.setXORMode(color); }

    @Override public void setRenderingHint(RenderingHints.Key key, Object value) {
        target.setRenderingHint(key, value);
    }
    @Override public Object getRenderingHint(RenderingHints.Key key) { return target.getRenderingHint(key); }
    @Override public void setRenderingHints(Map<?, ?> hints) { target.setRenderingHints(hints); }
    @Override public void addRenderingHints(Map<?, ?> hints) { target.addRenderingHints(hints); }
    @Override public RenderingHints getRenderingHints() { return target.getRenderingHints(); }

    @Override public void translate(int x, int y) { target.translate(x, y); }
    @Override public void translate(double tx, double ty) { target.translate(tx, ty); }
    @Override public void rotate(double theta) { target.rotate(theta); }
    @Override public void rotate(double theta, double x, double y) { target.rotate(theta, x, y); }
    @Override public void scale(double sx, double sy) { target.scale(sx, sy); }
    @Override public void shear(double shx, double shy) { target.shear(shx, shy); }
    @Override public void transform(AffineTransform tx) { target.transform(tx); }
    @Override public void setTransform(AffineTransform tx) { target.setTransform(tx); }
    @Override public AffineTransform getTransform() { return target.getTransform(); }

    @Override public Rectangle getClipBounds() { return target.getClipBounds(); }
    @Override public void clipRect(int x, int y, int width, int height) { target.clipRect(x, y, width, height); }
    @Override public void setClip(int x, int y, int width, int height) { target.setClip(x, y, width, height); }
    @Override public Shape getClip() { return target.getClip(); }
    @Override public void setClip(Shape clip) { target.setClip(clip); }
    @Override public void clip(Shape shape) { target.clip(shape); }

    @Override public void dispose() { target.dispose(); }
}
