package kr.dogfoot.hwpdrawer.export.svg;

import kr.dogfoot.hwpdrawer.export.image.RasterRendering;
import kr.dogfoot.hwpdrawer.export.vector.DelegatingGraphics2D;
import kr.dogfoot.hwpdrawer.export.vector.OutlineTextGraphics2D;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * 글자를 윤곽선으로 그리되, 같은 글자의 윤곽선은 한 번만 정의하고 다시 쓰게 글자마다 {@link GlyphUse}를 채운다
 * ({@link GlyphReuse}). 맨 안쪽 대상이 {@link GlyphSvgGraphics2D}일 때만 쓴다. 모양은 {@link OutlineTextGraphics2D}와 같다.
 */
final class ReusedGlyphGraphics2D extends OutlineTextGraphics2D {
    private final GlyphReuse reuse;

    ReusedGlyphGraphics2D(Graphics2D target, GlyphReuse reuse) {
        super(target);
        this.reuse = reuse;
    }

    @Override
    protected DelegatingGraphics2D wrap(Graphics2D newTarget) {
        return new ReusedGlyphGraphics2D(newTarget, reuse);
    }

    @Override
    public void drawString(String text, float x, float y) {
        if (text.isEmpty()) return;
        GlyphVector glyphs = getFont().createGlyphVector(RasterRendering.fontRenderContext(), text);
        for (int index = 0; index < glyphs.getNumGlyphs(); index++) {
            drawGlyph(glyphs, index, x, y);
        }
    }

    private void drawGlyph(GlyphVector glyphs, int index, float x, float y) {
        Point2D position = glyphs.getGlyphPosition(index);
        Shape outlineAtOrigin = AffineTransform.getTranslateInstance(-position.getX(), -position.getY())
                .createTransformedShape(glyphs.getGlyphOutline(index));
        if (outlineAtOrigin.getBounds2D().isEmpty()) return;    // 공백처럼 보이지 않는 글리프

        target.fill(reuse.markGlyph(outlineAtOrigin, x + position.getX(), y + position.getY()));
    }
}
