package kr.dogfoot.hwpdrawer.export.vector;

import kr.dogfoot.hwpdrawer.export.image.RasterRendering;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.text.AttributedCharacterIterator;

/**
 * 글자를 글리프 윤곽선(도형)으로 바꿔 그리는 래퍼. SVG/HTML 내보내기에 쓴다.
 *
 * <p>왜 필요한가: 폰트 폴더의 파일에서 읽은 폰트(Font.createFont)는 시스템에 등록되지 않는다.
 * SVG에 {@code <text font-family=…>}로 쓰면 보는 쪽에 그 폰트가 없어 다른 글꼴로 나오고,
 * 그래서 SVG 생성기의 글자 처리는 쓰지 않는다.
 * 여기서는 그리는 Font 객체에서 바로 윤곽선을 뽑으므로 장평(transform)·합성 굵게까지 PNG와 같은
 * 모양이 나온다. 대신 SVG 안의 글자는 선택/검색되지 않는다.</p>
 *
 * <p>글자 배치·측정은 PNG 기준({@link RasterRendering})을 쓴다.</p>
 */
public class OutlineTextGraphics2D extends DelegatingGraphics2D {

    public OutlineTextGraphics2D(Graphics2D target) {
        super(target);
    }

    @Override
    protected DelegatingGraphics2D wrap(Graphics2D newTarget) {
        return new OutlineTextGraphics2D(newTarget);
    }

    @Override
    public void drawString(String text, float x, float y) {
        if (text.isEmpty()) return;
        GlyphVector glyphs = getFont().createGlyphVector(RasterRendering.fontRenderContext(), text);
        target.fill(glyphs.getOutline(x, y));
    }

    @Override
    public void drawString(String text, int x, int y) {
        drawString(text, (float) x, (float) y);
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, float x, float y) {
        if (iterator.getBeginIndex() == iterator.getEndIndex()) return;
        TextLayout layout = new TextLayout(iterator, RasterRendering.fontRenderContext());
        target.fill(layout.getOutline(AffineTransform.getTranslateInstance(x, y)));
    }

    @Override
    public void drawString(AttributedCharacterIterator iterator, int x, int y) {
        drawString(iterator, (float) x, (float) y);
    }

    @Override
    public void drawGlyphVector(GlyphVector glyphs, float x, float y) {
        target.fill(glyphs.getOutline(x, y));
    }

    @Override
    public FontRenderContext getFontRenderContext() {
        return RasterRendering.fontRenderContext();
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return RasterRendering.fontMetrics(font);
    }
}
