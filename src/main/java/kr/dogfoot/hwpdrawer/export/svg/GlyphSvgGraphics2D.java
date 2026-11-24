package kr.dogfoot.hwpdrawer.export.svg;

import org.apache.batik.svggen.DOMGroupManager;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;

import java.awt.Graphics;
import java.awt.Shape;

/**
 * Batik SVG 생성기에 글자 재사용({@link GlyphReuse})을 더한 것. {@link GlyphUse}를 채우면 경로 대신
 * {@code <use>} 요소를 넣는다 — Batik의 그룹 관리자가 다른 도형처럼 그 순간의 색·변환·클립을 붙인다.
 */
final class GlyphSvgGraphics2D extends SVGGraphics2D {
    private final GlyphReuse reuse;

    /** 글자는 이 앞의 래퍼가 윤곽선으로 바꾸므로 Batik의 글자 처리는 쓰지 않는다 (textAsShapes). */
    GlyphSvgGraphics2D(SVGGeneratorContext context, GlyphReuse reuse) {
        super(context, true);
        this.reuse = reuse;
    }

    private GlyphSvgGraphics2D(GlyphSvgGraphics2D source) {
        super(source);
        this.reuse = source.reuse;
    }

    /** Batik 기본 create()는 SVGGraphics2D를 만들어 글자 재사용을 잃는다. */
    @Override
    public Graphics create() {
        return new GlyphSvgGraphics2D(this);
    }

    @Override
    public void fill(Shape shape) {
        if (shape instanceof GlyphUse) {
            getDOMGroupManager().addElement(reuse.useElement((GlyphUse) shape, getDOMFactory()), DOMGroupManager.FILL);
        } else {
            super.fill(shape);
        }
    }
}
