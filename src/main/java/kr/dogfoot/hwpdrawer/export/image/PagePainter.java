package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwpdrawer.document.page.Footnote;
import kr.dogfoot.hwpdrawer.document.page.FootnoteArea;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.FontManager;
import kr.dogfoot.hwpdrawer.util.UnitConverter;
import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;

/**
 * 한 페이지(Page)의 내용을 Graphics2D에 그린다.
 *
 * <p>출력 형식(PNG/SVG/PDF/HTML)과 무관한 공통 그리기 순서를 담당한다. 캔버스 준비(이미지 생성,
 * 배경, 파일 저장)는 각 형식의 Exporter가 맡는다.</p>
 *
 * <p>그리기 순서: 뒤로 컨트롤 → 본문 → 머리말/꼬리말 → 각주(구분선 + 본문) → 앞으로 컨트롤
 * → 단 분리선 → 쪽 번호.</p>
 */
public class PagePainter {
    private final HWPFileReader reader;

    public PagePainter(HWPFileReader reader) {
        this.reader = reader;
    }

    public void paint(Page page, Graphics2D g2d) {
        // 정수 픽셀 변환의 기준 원점 = 이 페이지의 본문영역 좌상단 (UnitConverter 참고)
        UnitConverter.setOrigin(page.bodyArea().left(), page.bodyArea().top());

        ControlRenderer controlRenderer = new ControlRenderer(g2d, reader);
        ImageRenderer textRenderer = new ImageRenderer(g2d, reader);
        controlRenderer.renderBehindText(page);
        textRenderer.renderContent(page.content());
        if (page.header() != null) textRenderer.renderContent(page.header().content());
        if (page.footer() != null) textRenderer.renderContent(page.footer().content());
        renderFootnotes(g2d, textRenderer, page);
        controlRenderer.renderInFrontOfText(page);
        renderColumnSeparators(g2d, page);
        renderPageNumber(g2d, page);
    }

    /** 각주 구분선과 각주 본문을 그린다. */
    private void renderFootnotes(Graphics2D g2d, ImageRenderer textRenderer, Page page) {
        FootnoteArea area = page.footnotes();
        if (area == null) return;

        renderFootnoteDivideLine(g2d, area);
        for (Footnote footnote : area.notes()) {
            textRenderer.renderContent(footnote.content());
        }
    }

    private void renderFootnoteDivideLine(Graphics2D g2d, FootnoteArea area) {
        EachBorder border = area.divideLineBorder();
        if (border == null || border.getType() == null) return;
        Stroke stroke = StrokeFactory.forParaBorder(border.getType());
        if (stroke == null) return;

        // 단 분리선(renderColumnSeparators)과 같은 이유로 HWP 단위에서 한 번만
        // 픽셀 변환한다. 경계마다 toPixel(절삭)을 태우면 왼쪽으로 밀린다.
        int x1 = Math.round(UnitConverter.toPixelF(area.divideLineLeft()));
        int x2 = Math.round(UnitConverter.toPixelF(area.divideLineRight()));
        int y = UnitConverter.toPixel(area.divideLineY());
        g2d.setColor(colorOf(border, Color.BLACK));
        g2d.setStroke(stroke);
        g2d.drawLine(x1, y, x2, y);
    }

    private void renderColumnSeparators(Graphics2D g2d, Page page) {
        for (Page.ColumnSep sep : page.columnSeps()) {
            renderColumnSeparator(g2d, sep);
        }
    }

    private void renderColumnSeparator(Graphics2D g2d, Page.ColumnSep sep) {
        EachBorder border = sep.border;
        if (border == null || border.getType() == null) return;
        Stroke stroke = StrokeFactory.forColumnSeparator(border.getType());
        if (stroke == null) return;

        // gap 중심을 HWP 단위에서 먼저 구하고 픽셀 변환·반올림을 한 번만 한다. 경계마다
        // toPixel(절삭)을 태운 뒤 정수 나눗셈으로 또 절삭하면 최대 1.5px까지 왼쪽으로 밀려
        // 한글보다 1px 왼쪽에 그려졌다(맞쪽·오른쪽부터·같은크기 실측). 올림(ceil)은 과교정이다
        // — 맞쪽 354→355, 같은크기 370→371로 오히려 한글에서 멀어진다.
        int x = Math.round(UnitConverter.toPixelF((sep.innerA + sep.innerB) / 2.0));
        int y1 = UnitConverter.toPixel(sep.top);
        int y2 = UnitConverter.toPixel(sep.bottom);
        if (y1 >= y2) return;
        g2d.setColor(colorOf(border, Color.GRAY));
        g2d.setStroke(stroke);
        g2d.drawLine(x, y1, x, y2);
    }

    /** 쪽 번호(PageNumberPosition)를 CenterBottom 위치에 그린다. */
    private void renderPageNumber(Graphics2D g2d, Page page) {
        String text = page.pageNumberText();
        if (text == null || text.isEmpty()) return;

        Font font = pageNumberFont(page);
        g2d.setFont(font);
        g2d.setColor(Color.BLACK);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        // 폭은 PNG 기준으로 잰다 — PDF/SVG Graphics2D의 자체 FontMetrics는 값이 달라 가운데가 어긋난다.
        int textWidth = RasterRendering.fontMetrics(font).stringWidth(text);
        int centerX = UnitConverter.toPixelX(page.pageNumberCenterX());
        int baselineY = UnitConverter.toPixelY(page.pageNumberBaselineY());
        g2d.drawString(text, centerX - textWidth / 2, baselineY);
    }

    private static Font pageNumberFont(Page page) {
        float pixelSize = Math.max(1, UnitConverter.toPixelFontSize(1000));   // 10pt
        if (page.pageNumberCharShape() == null) {
            return new Font(Font.SERIF, Font.PLAIN, Math.round(pixelSize));
        }
        return FontManager.instance().getFont(page.pageNumberCharShape(), pixelSize);
    }

    private static Color colorOf(EachBorder border, Color defaultColor) {
        if (border.getColor() == null) return defaultColor;
        return new Color(border.getColor().getR(), border.getColor().getG(), border.getColor().getB());
    }
}
