package kr.dogfoot.hwpdrawer.export.image;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.font.FontRenderContext;
import java.awt.image.BufferedImage;

/**
 * PNG 렌더링의 기준 설정(렌더링 힌트)과, 그 설정에서의 글자 측정.
 *
 * <p>글자 폭은 Graphics2D 구현마다 다르게 나올 수 있다. 예를 들어 PDF용 Graphics2D는 임베드한
 * PDF 폰트의 폭을 돌려준다. 그래서 그려진 결과로 위치를 정하는 곳(쪽 번호 가운데 정렬 등)은
 * 출력 형식과 상관없이 이 기준값을 써서, 모든 형식이 PNG와 같은 자리에 그리게 한다.</p>
 */
public final class RasterRendering {
    private static final Graphics2D MEASURING_GRAPHICS = createMeasuringGraphics();

    private RasterRendering() {}

    /** PNG 캔버스에 적용하는 렌더링 힌트. */
    public static void applyHints(Graphics2D g2d) {
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);
    }

    /** PNG 렌더링 기준의 FontMetrics. */
    public static FontMetrics fontMetrics(Font font) {
        return MEASURING_GRAPHICS.getFontMetrics(font);
    }

    /** PNG 렌더링 기준의 FontRenderContext (글리프 배치·윤곽선 계산용). */
    public static FontRenderContext fontRenderContext() {
        return MEASURING_GRAPHICS.getFontRenderContext();
    }

    private static Graphics2D createMeasuringGraphics() {
        Graphics2D g2d = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
        applyHints(g2d);
        return g2d;
    }
}
