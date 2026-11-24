package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPicture;
import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlRectangle;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControlType;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponent;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponentNormal;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineInfo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponenteach.ShapeComponentPicture;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponenteach.ShapeComponentRectangle;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineInfo;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.lineinfo.LineType;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.FillInfo;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.GradientFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.ImageFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.PatternFill;
import kr.dogfoot.hwplib.object.etc.Color4Byte;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.document.control.GsoContainerItem;
import kr.dogfoot.hwpdrawer.document.control.GsoPictureItem;
import kr.dogfoot.hwpdrawer.document.control.GsoPolygonItem;
import kr.dogfoot.hwpdrawer.document.control.GsoShapeItem;
import kr.dogfoot.hwpdrawer.document.control.GsoTextBoxItem;
import kr.dogfoot.hwpdrawer.document.control.TableItem;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.UnitConverter;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 페이지에 등록된 컨트롤 아이템(이미지, 글상자, 도형, 표)을 Graphics2D에 그린다.
 *
 * <p>렌더링 순서: behindText=true인 컨트롤 → 텍스트 → behindText=false인 컨트롤.
 * PagePainter가 이 클래스를 호출해서 텍스트 전/후에 컨트롤을 그린다 (모든 출력 형식 공통).</p>
 */
public class ControlRenderer {
    private final Graphics2D g2d;
    private final HWPFileReader reader;

    public ControlRenderer(Graphics2D g2d, HWPFileReader reader) {
        this.g2d = g2d;
        this.reader = reader;
    }

    /** 페이지의 뒤로 컨트롤(behindText=true)을 zOrder 오름차순으로 그린다. */
    public void renderBehindText(Page page) {
        for (ControlItem item : sortedByZOrder(page, true)) renderItem(item);
    }

    /** 페이지의 앞으로 컨트롤(behindText=false)을 zOrder 오름차순으로 그린다. */
    public void renderInFrontOfText(Page page) {
        for (ControlItem item : sortedByZOrder(page, false)) renderItem(item);
    }

    /**
     * zOrder가 작을수록 뒤(먼저 그림). 동일 zOrder는 등록 순 유지.
     *
     * <p>예외: 그림(GsoPictureItem)과 <b>겹치는</b> 표(TableItem)는, 그림의 zOrder가 더
     * 높더라도 그림보다 <b>앞</b>(나중에)에 그린다. 한글은 자리차지 표를 겹치는 그림 위에
     * 그리므로, 불투명 그림(예: 보도자료 상단 로고)이 표의 선/칸을 덮지 않게 한다.
     * 겹치지 않는 컨트롤은 순수 zOrder 순서를 유지해 기존 렌더 결과를 보존한다.</p>
     */
    private static java.util.List<ControlItem> sortedByZOrder(Page page, boolean behindText) {
        java.util.List<ControlItem> filtered = new java.util.ArrayList<>();
        for (ControlItem item : page.controls()) {
            if (item.behindText() == behindText) filtered.add(item);
        }
        filtered.sort(java.util.Comparator.comparingDouble(it -> drawKey(it, filtered)));
        return filtered;
    }

    /**
     * 정렬 키 — 기본은 zOrder. 단, 표가 zOrder가 같거나 더 높은 그림과 겹치면
     * (그림.zOrder + 0.5)로 올려 그림 바로 위에 그리게 한다. 정수 zOrder끼리는
     * 충돌하지 않으므로 안정적인 전순서가 유지된다.
     */
    private static double drawKey(ControlItem item, java.util.List<ControlItem> siblings) {
        if (!(item instanceof TableItem)) return item.zOrder();
        double key = item.zOrder();
        for (ControlItem other : siblings) {
            if (other instanceof GsoPictureItem
                    && other.zOrder() >= item.zOrder()
                    && overlaps(item.area(), other.area())) {
                key = Math.max(key, other.zOrder() + 0.5);
            }
        }
        return key;
    }

    /** 두 영역이 실제로 겹치는지(경계만 맞닿은 경우 제외). */
    private static boolean overlaps(Area a, Area b) {
        return a.left() < b.right() && b.left() < a.right()
                && a.top() < b.bottom() && b.top() < a.bottom();
    }

    private void renderItem(ControlItem item) {
        if (item instanceof GsoContainerItem) {
            renderContainer((GsoContainerItem) item);
        } else if (item instanceof GsoPictureItem) {
            renderPicture((GsoPictureItem) item);
        } else if (item instanceof GsoPolygonItem) {
            renderPolygon((GsoPolygonItem) item);
        } else if (item instanceof GsoTextBoxItem) {
            renderTextBox((GsoTextBoxItem) item);
        } else if (item instanceof GsoShapeItem) {
            renderShape((GsoShapeItem) item);
        } else if (item instanceof TableItem) {
            renderTable((TableItem) item);
        }
    }

    /** 다각형(마름모·삼각형 등)을 꼭짓점 좌표로 채우고 테두리를 그린다. */
    private void renderPolygon(GsoPolygonItem item) {
        long[] hx = item.xs(), hy = item.ys();
        if (hx.length < 2) return;
        int[] px = new int[hx.length];
        int[] py = new int[hy.length];
        for (int i = 0; i < hx.length; i++) {
            px[i] = UnitConverter.toPixelX(hx[i]);
            py[i] = UnitConverter.toPixelY(hy[i]);
        }
        ShapeComponent sc = item.gso().getShapeComponent();
        Color saved = g2d.getColor();
        Stroke savedStroke = g2d.getStroke();
        Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            if (sc instanceof ShapeComponentNormal) {
                ShapeComponentNormal scn = (ShapeComponentNormal) sc;
                Color back = patternBackColor(scn.getFillInfo());
                if (back != null) {
                    g2d.setColor(back);
                    g2d.fillPolygon(px, py, px.length);
                }
                LineInfo li = scn.getLineInfo();
                if (li != null && li.getProperty().getLineType() != LineType.None) {
                    g2d.setColor(color4ByteToColor(li.getColor()));
                    g2d.setStroke(new BasicStroke(gsoBorderWidth(li),
                            BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g2d.drawPolygon(px, py, px.length);
                }
            }
        } finally {
            g2d.setColor(saved);
            g2d.setStroke(savedStroke);
            if (savedAA != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
        }
    }

    /** 묶음 개체(그룹): 자식들을 등록 순서(뒤→앞)대로 그린다. */
    private void renderContainer(GsoContainerItem item) {
        for (ControlItem child : item.children()) {
            renderItem(child);
        }
    }

    // ── 이미지 ──────────────────────────────────────────────────────────

    private void renderPicture(GsoPictureItem item) {
        ControlPicture picture = item.picture();
        ShapeComponentPicture scp = picture.getShapeComponentPicture();
        int binItemId = scp.getPictureInfo().getBinItemID();
        BufferedImage img = reader.getEmbeddedImage(binItemId);

        Area area = item.area();
        int px = UnitConverter.toPixelX(area.left());
        int py = UnitConverter.toPixelY(area.top());
        int pw = UnitConverter.toPixel(area.width());
        int ph = UnitConverter.toPixel(area.height());

        if (img != null && pw > 0 && ph > 0) {
            kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.PictureInfo pi = scp.getPictureInfo();
            img = applyPictureTone(img, pi.getBrightness(), pi.getContrast());
            drawPictureCropped(img, scp, px, py, pw, ph);
        }

        // 이미지 테두리: LineType != None이면 항상 그린다 (thickness=0이면 1px).
        if (scp.getBorderProperty().getLineType() != LineType.None) {
            g2d.setColor(color4ByteToColor(scp.getBorderColor()));
            g2d.setStroke(new BasicStroke(Math.max(1f, scp.getBorderThickness() / 100f)));
            g2d.drawRect(px, py, pw, ph);
        }
    }

    /**
     * 그림을 <b>잘라내기(cutting)</b>를 반영해 그린다.
     *
     * <p>한글의 그림 자르기는 원본을 건드리지 않고 "보여줄 구간"만 기록한다
     * ({@code ShapeComponentPicture.getXxxAfterCutting()}, 단위는 {@code getImageWidth/Height()}와
     * 같은 좌표계). 이 구간을 무시하고 원본 전체를 그림 상자에 욱여넣으면 상자의 종횡비만큼
     * 내용이 찌그러진다.</p>
     *
     * <p>실문서 R07 page1 표 안의 캘리그라피가 그 예다 — 원본 2339x1654 중
     * 손글씨는 가운데 가로 띠에만 있고 자르기가 세로 17.3%만 남기는데(T=40964, B=62412 /
     * 전체 124080), 이를 무시하니 손글씨가 4px로 눌렸다(참조는 28px). 자른 구간의 종횡비
     * 7.17이 그림 상자의 6.82와 거의 같아, 자르고 넣으면 왜곡이 사라진다.</p>
     *
     * <p>자르기가 없는 그림(구간이 전체와 같음)은 기존과 동일하게 전체를 그린다 —
     * 테스트 코퍼스 61개 그림 중 자르기가 걸린 것은 3개뿐이다.</p>
     */
    private void drawPictureCropped(BufferedImage img, ShapeComponentPicture scp,
                                     int px, int py, int pw, int ph) {
        long imgW = scp.getImageWidth();
        long imgH = scp.getImageHeight();
        long cl = scp.getLeftAfterCutting();
        long ct = scp.getTopAfterCutting();
        long cr = scp.getRightAfterCutting();
        long cb = scp.getBottomAfterCutting();

        boolean noCrop = imgW <= 0 || imgH <= 0
                || (cl == 0 && ct == 0 && cr == imgW && cb == imgH)
                || cr <= cl || cb <= ct;
        if (noCrop) {
            g2d.drawImage(img, px, py, pw, ph, null);
            return;
        }

        // 자르기 좌표(이미지 좌표계) → 실제 비트맵 픽셀
        int sx1 = (int) Math.round(img.getWidth()  * (double) cl / imgW);
        int sy1 = (int) Math.round(img.getHeight() * (double) ct / imgH);
        int sx2 = (int) Math.round(img.getWidth()  * (double) cr / imgW);
        int sy2 = (int) Math.round(img.getHeight() * (double) cb / imgH);
        sx1 = Math.max(0, Math.min(img.getWidth(),  sx1));
        sx2 = Math.max(0, Math.min(img.getWidth(),  sx2));
        sy1 = Math.max(0, Math.min(img.getHeight(), sy1));
        sy2 = Math.max(0, Math.min(img.getHeight(), sy2));
        if (sx2 <= sx1 || sy2 <= sy1) {
            g2d.drawImage(img, px, py, pw, ph, null);
            return;
        }
        g2d.drawImage(img, px, py, px + pw, py + ph, sx1, sy1, sx2, sy2, null);
    }

    /**
     * 그림 밝기/명암(PictureInfo)을 반영해 톤을 조정한 이미지를 반환한다.
     * (hwplib 1.1.10 lib jar는 명암→밝기 순으로 파싱하도록 수정됨.)
     *
     * <p>밝기(brightness, -100~100%)는 각 색 채널에 균일한 가산 시프트를 준다
     * (예: 실문서 R25 배경 워터마크 로고 brightness=60 → v'≈clamp(v+153),
     * 실측 일치). 명암(contrast, -100~100%)은 128 중심의 대비 배율로 적용한다
     * (c&gt;0 대비↑, c&lt;0 대비↓, c=-100 이면 전부 128 회색). 알파는 보존한다.
     * 밝기·명암이 모두 0이면 원본을 그대로 반환한다(무영향).</p>
     */
    private static BufferedImage applyPictureTone(BufferedImage img, int brightness, int contrast) {
        if (img == null || (brightness == 0 && contrast == 0)) return img;
        int bShift = Math.round(brightness / 100f * 255f);
        float cFactor = 1f + contrast / 100f;

        int w = img.getWidth(), h = img.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = img.getRGB(x, y);
                int a = (argb >>> 24) & 0xFF;
                int r = toneChannel((argb >> 16) & 0xFF, cFactor, bShift);
                int g = toneChannel((argb >> 8) & 0xFF, cFactor, bShift);
                int b = toneChannel(argb & 0xFF, cFactor, bShift);
                out.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        return out;
    }

    /** 한 색 채널에 명암 배율(128 중심)과 밝기 가산을 적용하고 0~255로 클램프한다. */
    private static int toneChannel(int v, float cFactor, int bShift) {
        int t = Math.round((v - 128) * cFactor) + 128 + bShift;
        return t < 0 ? 0 : (t > 255 ? 255 : t);
    }

    // ── 글상자 ──────────────────────────────────────────────────────────

    private void renderTextBox(GsoTextBoxItem item) {
        Area area = item.area();
        ControlRectangle rect = item.rectangle();

        // 둥근 사각형(곡률 roundRate>0)이면 모서리를 둥글게 그린다.
        int arc = roundArc(rect, area);

        // 배경/테두리 그리기
        ShapeComponent sc = rect.getShapeComponent();
        if (sc instanceof ShapeComponentNormal) {
            ShapeComponentNormal scn = (ShapeComponentNormal) sc;
            drawShapeFill(scn.getFillInfo(), area, arc);
            drawShapeBorder(scn.getLineInfo(), area, arc, GsoControlType.Rectangle);
        }

        // 내부 텍스트 렌더링 (클리핑 + 세로 정렬)
        if (item.innerDocument() != null) {
            kr.dogfoot.hwplib.object.bodytext.control.gso.textbox.ListHeaderForTextBox lh =
                    rect.getTextBox().getListHeader();
            Area innerArea = new Area(
                    area.left()   + lh.getLeftMargin(),
                    area.top()    + lh.getTopMargin(),
                    area.right()  - lh.getRightMargin(),
                    area.bottom() - lh.getBottomMargin());
            renderInnerDocument(item.innerDocument(), innerArea, item.verticalAlignmentOffset());
        }
    }

    // ── 도형 ──────────────────────────────────────────────────────────

    private void renderShape(GsoShapeItem item) {
        Area area = item.area();
        int arc = (item.gso() instanceof ControlRectangle)
                ? roundArc((ControlRectangle) item.gso(), area) : 0;
        ShapeComponent sc = item.gso().getShapeComponent();
        if (sc instanceof ShapeComponentNormal) {
            ShapeComponentNormal scn = (ShapeComponentNormal) sc;
            drawShapeFill(scn.getFillInfo(), area, arc);
            drawShapeBorder(scn.getLineInfo(), area, arc, item.gso().getGsoType());
        }
    }

    /**
     * 둥근 사각형의 모서리 arc 지름(px)을 계산한다. 한글의 곡률(roundRate, %)은
     * 짧은 변 기준으로 arc 지름 = min(폭,높이) × roundRate/100 이다(roundRate 20 → 지름 20%).
     * 곡률이 0이면 0(각진 사각형).
     */
    private static int roundArc(ControlRectangle rect, Area area) {
        ShapeComponentRectangle scr = rect.getShapeComponentRectangle();
        if (scr == null) return 0;
        int roundRate = scr.getRoundRate();
        if (roundRate <= 0) return 0;
        int pw = UnitConverter.toPixel(area.width());
        int ph = UnitConverter.toPixel(area.height());
        return (int) Math.round(Math.min(pw, ph) * roundRate / 100.0);
    }

    // ── 표 ──────────────────────────────────────────────────────────────

    private void renderTable(TableItem item) {
        // 다중선(이중/삼중) 셀 테두리가 하나라도 있으면, 교차점이 한글과 동일한 격자가 되도록
        // 표 전체를 하나의 격자로 그린다(셀별 독립 렌더링은 공유 격자선을 이중으로 그려 over-fill).
        boolean lattice = false;
        for (TableItem.CellLayout cell : item.cells()) {
            BorderFill bf = borderFillById(cell.cell().getListHeader().getBorderFillId());
            if (TableBorderRenderer.hasMultiLine(bf)) { lattice = true; break; }
        }

        // 점선 테두리의 dash 위상 원점 — 격자선마다 점선이 연속으로 이어지는 구간(run)의 시작.
        // 셀마다 자기 좌표에서 패턴을 시작하면 한 격자선이 여러 위상으로 그려져 겹치는 구간의
        // 점이 길어진다(DashRunOrigins 참고).
        DashRunOrigins hRuns = new DashRunOrigins();
        DashRunOrigins vRuns = new DashRunOrigins();
        for (TableItem.CellLayout cell : item.cells()) {
            BorderFill bf = borderFillById(cell.cell().getListHeader().getBorderFillId());
            Area a = cell.area();
            BorderRenderer.collectDashRuns(bf,
                    UnitConverter.toPixelX(a.left()), UnitConverter.toPixelY(a.top()),
                    UnitConverter.toPixelX(a.right()), UnitConverter.toPixelY(a.bottom()),
                    hRuns, vRuns);
        }

        for (TableItem.CellLayout cell : item.cells()) {
            // lattice면 셀별 테두리는 건너뛰고 아래에서 일괄 처리
            renderCell(cell, !lattice, hRuns, vRuns);
        }

        if (lattice) {
            List<TableBorderRenderer.Cell> rects = new ArrayList<>();
            for (TableItem.CellLayout cell : item.cells()) {
                BorderFill bf = borderFillById(cell.cell().getListHeader().getBorderFillId());
                if (bf == null) continue;
                Area a = cell.area();
                rects.add(new TableBorderRenderer.Cell(
                        UnitConverter.toPixelX(a.left()), UnitConverter.toPixelY(a.top()),
                        UnitConverter.toPixelX(a.right()), UnitConverter.toPixelY(a.bottom()), bf));
            }
            Color saved = g2d.getColor();
            Stroke savedStroke = g2d.getStroke();
            Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            try {
                TableBorderRenderer.render(g2d, rects);
            } finally {
                g2d.setColor(saved);
                g2d.setStroke(savedStroke);
                if (savedAA != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
            }
        }
    }

    private void renderCell(TableItem.CellLayout cell, boolean drawBorder,
                            DashRunOrigins hRuns, DashRunOrigins vRuns) {
        Area area = cell.area();
        int borderFillId = cell.cell().getListHeader().getBorderFillId();
        BorderFill bf = borderFillById(borderFillId);

        // 셀 배경 채우기
        if (bf != null) {
            drawBorderFillBackground(bf, area);
        }

        // 셀 내용 텍스트 렌더링
        if (cell.cellDocument() != null) {
            renderInnerDocument(cell.cellDocument(), area, cell.verticalAlignmentOffset());
        }

        // 셀 테두리 그리기 (격자 모드가 아닐 때만)
        if (bf != null && drawBorder) {
            drawBorderFillBorder(bf, area, hRuns, vRuns);
        }

        // 대각선은 격자(공유 테두리)와 무관한 셀 고유 요소이므로 두 모드 모두에서 그린다.
        if (bf != null) {
            drawBorderFillDiagonal(bf, area);
        }
    }

    // ── 내부 Document 렌더링 ─────────────────────────────────────────────

    /**
     * 내부 Document를 clipArea에 클리핑하여 렌더링한다.
     * vOffsetHwp > 0이면 세로 정렬 오프셋만큼 아래로 밀어서 그린다 (Center/Bottom).
     */
    private void renderInnerDocument(Document innerDoc, Area clipArea, long vOffsetHwp) {
        if (innerDoc.pages().isEmpty()) return;

        Shape savedClip = g2d.getClip();
        // 클립은 translate 전에 device 공간 기준으로 설정
        g2d.setClip(
                UnitConverter.toPixelX(clipArea.left()),
                UnitConverter.toPixelY(clipArea.top()),
                UnitConverter.toPixel(clipArea.width()),
                UnitConverter.toPixel(clipArea.height()));

        AffineTransform savedTransform = g2d.getTransform();
        int vOffsetPx = UnitConverter.toPixel(vOffsetHwp);
        if (vOffsetPx != 0) g2d.translate(0, vOffsetPx);

        try {
            ImageRenderer innerRenderer = new ImageRenderer(g2d, reader);
            ControlRenderer innerCtrlRenderer = new ControlRenderer(g2d, reader);
            for (kr.dogfoot.hwpdrawer.document.page.Page innerPage : innerDoc.pages()) {
                innerCtrlRenderer.renderBehindText(innerPage);
                innerRenderer.renderContent(innerPage.content());
                innerCtrlRenderer.renderInFrontOfText(innerPage);
            }
        } finally {
            g2d.setTransform(savedTransform);
            g2d.setClip(savedClip);
        }
    }

    // ── 배경/테두리 공통 ─────────────────────────────────────────────────

    /**
     * 셀/문단 <b>배경</b>을 채운다. 크기를 <b>양 끝 좌표의 차</b>로 구해 맞닿은 이웃 셀과
     * 정확히 맞물리게 한다.
     *
     * <p>{@link UnitConverter#toPixel}은 버림이라 {@code trunc(left) + trunc(width)}가
     * {@code trunc(left + width)}보다 1 작을 수 있다. 폭을 따로 변환하면 맞닿은 두 셀의
     * 배경 사이에 1px 흰 틈이 생긴다 — 실문서 R14 제목 표의 파란 막대는
     * 셀 3개로 쪼개져 있어 경계마다 틈이 보였다(셀 테두리 격자가 이미 쓰는 방식과 동일).</p>
     *
     * <p>도형({@link #drawShapeFill(FillInfo, Area, int)})은 이 방식을 쓰지 <b>않는다</b> —
     * 도형은 이웃과 맞물릴 필요가 없고, 오히려 테두리({@code drawShapeBorder})와 같은
     * 사각형을 써야 둥근사각형·타원에서 채우기가 테두리 밖으로 삐져나오지 않는다.</p>
     */
    private void drawBackgroundFill(FillInfo fillInfo, Area area) {
        drawShapeFill(fillInfo, area, 0,
                UnitConverter.toPixelX(area.right()) - UnitConverter.toPixelX(area.left()),
                UnitConverter.toPixelY(area.bottom()) - UnitConverter.toPixelY(area.top()));
    }

    /**
     * 도형을 채운다. arc&gt;0이면 모서리를 둥글게(둥근 사각형) 채운다.
     * 크기는 테두리와 어긋나지 않도록 폭/높이를 그대로 변환한다.
     */
    private void drawShapeFill(FillInfo fillInfo, Area area, int arc) {
        drawShapeFill(fillInfo, area, arc,
                UnitConverter.toPixel(area.width()), UnitConverter.toPixel(area.height()));
    }

    private void drawShapeFill(FillInfo fillInfo, Area area, int arc, int pw, int ph) {
        if (fillInfo == null) return;
        int px = UnitConverter.toPixelX(area.left());
        int py = UnitConverter.toPixelY(area.top());
        if (fillInfo.getType().hasGradientFill()) {
            if (arc > 0) {
                Shape savedClip = g2d.getClip();
                Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.clip(new java.awt.geom.RoundRectangle2D.Float(px, py, pw, ph, arc, arc));
                try {
                    drawGradientFill(fillInfo.getGradientFill(), area, pw, ph);
                } finally {
                    g2d.setClip(savedClip);
                    if (savedAA != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
                }
            } else {
                drawGradientFill(fillInfo.getGradientFill(), area, pw, ph);
            }
        } else {
            Color back = patternBackColor(fillInfo);
            if (back != null) {
                g2d.setColor(back);
                if (arc > 0) {
                    Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2d.fillRoundRect(px, py, pw, ph, arc, arc);
                    if (savedAA != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
                } else {
                    g2d.fillRect(px, py, pw, ph);
                }
            }
        }
        if (fillInfo.getType().hasImageFill()) {
            drawImageFill(fillInfo.getImageFill(), area, pw, ph);
        }
    }

    /**
     * 무늬 채우기의 바탕색. 칠하지 않으면 null — 무늬 채우기가 아니거나 바탕색이 '색 없음'(0xFFFFFFFF)일 때다.
     * 문서 기본 테두리/배경(보통 2번)이 '색 없음'이라, 이를 흰색으로 칠하면 표 셀·글상자가 뒤에 깔린
     * 도형·그림을 흰 사각형으로 가린다 ({@link HwpColors}). 무늬(빗금 등)는 아직 그리지 않는다.
     */
    private static Color patternBackColor(FillInfo fillInfo) {
        if (fillInfo == null || !fillInfo.getType().hasPatternFill()) return null;
        PatternFill pf = fillInfo.getPatternFill();
        return pf == null ? null : HwpColors.paintOf(pf.getBackColor());
    }

    /**
     * 그러데이션 채우기(줄무늬형 한정). startAngle=90은 좌→우(가로) 방향으로 확인됨
     * — 실문서 R06 제목 밑줄 그러데이션의 실제 한글 출력과 대조해 각도 기준을 정함.
     * 원형/원뿔형/사각형 그러데이션은 아직 지원하지 않아 첫 색상으로 단색 채운다.
     */
    private void drawGradientFill(GradientFill gf, Area area, int pw, int ph) {
        if (gf == null || gf.getColorList().isEmpty()) return;
        int px = UnitConverter.toPixelX(area.left());
        int py = UnitConverter.toPixelY(area.top());
        if (pw <= 0 || ph <= 0) return;

        List<Color4Byte> colors = gf.getColorList();
        if (gf.getGradientType() != kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.GradientType.Stripe
                || colors.size() < 2) {
            g2d.setColor(color4ByteToColor(colors.get(0)));
            g2d.fillRect(px, py, pw, ph);
            return;
        }

        Color c1 = color4ByteToColor(colors.get(0));
        Color c2 = color4ByteToColor(colors.get(colors.size() - 1));
        double dirAngle = Math.toRadians(gf.getStartAngle()) - Math.PI / 2;
        float cx = px + pw / 2f;
        float cy = py + ph / 2f;
        float dx = (float) (Math.cos(dirAngle) * pw / 2.0);
        float dy = (float) (Math.sin(dirAngle) * ph / 2.0);

        Paint saved = g2d.getPaint();
        g2d.setPaint(new GradientPaint(cx - dx, cy - dy, c1, cx + dx, cy + dy, c2));
        g2d.fillRect(px, py, pw, ph);
        g2d.setPaint(saved);
    }

    private void drawImageFill(ImageFill imageFill, Area area, int pw, int ph) {
        if (imageFill == null || imageFill.getPictureInfo() == null) return;
        BufferedImage img = reader.getEmbeddedImage(imageFill.getPictureInfo().getBinItemID());
        if (img == null) return;
        g2d.drawImage(img,
                UnitConverter.toPixelX(area.left()),
                UnitConverter.toPixelY(area.top()),
                pw, ph, null);
    }

    /**
     * GSO 도형 테두리 두께(px). LineInfo.thickness는 HWPUNIT(1/7200인치) —
     * 실문서 R03 도표 박스 1mm(283)가 한글 출력 5px와 일치(283→4.72px). /100(구현 초기
     * 추정)은 절반 두께로 얇게 그려졌다.
     */
    private static float gsoBorderWidth(LineInfo lineInfo) {
        return Math.max(1f, UnitConverter.toPixelF((long) lineInfo.getThickness()));
    }

    /**
     * GSO 도형 테두리를 그린다.
     *
     * <p>선 종류(LineType)가 점선/파선(Dot·Dash·DashDot 등)이면 그에 맞는 dash 스트로크로
     * 그린다(응시원서 절취선·도장 점선 원 등). 도형 종류(gsoType)에 따라 타원/둥근사각형/
     * 사각형(=수평·수직선 포함)으로 그린다.</p>
     *
     * @param gsoType 도형 종류. Ellipse면 타원, 그 외는 (arc>0) 둥근사각형 / 사각형.
     */
    private void drawShapeBorder(LineInfo lineInfo, Area area, int arc, GsoControlType gsoType) {
        if (lineInfo == null) return;
        LineType lineType = lineInfo.getProperty().getLineType();
        if (lineType == LineType.None) return;

        float thickness = gsoBorderWidth(lineInfo);
        boolean patterned = lineType != LineType.Solid;   // 점선/파선 등은 안티앨리어싱 필요
        boolean rounded = gsoType == GsoControlType.Ellipse || arc > 0;

        Color savedColor = g2d.getColor();
        Stroke savedStroke = g2d.getStroke();
        Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        if (patterned || rounded) {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        try {
            g2d.setColor(color4ByteToColor(lineInfo.getColor()));
            g2d.setStroke(StrokeFactory.forGsoLine(lineType, thickness));
            int px = UnitConverter.toPixelX(area.left());
            int py = UnitConverter.toPixelY(area.top());
            int pw = UnitConverter.toPixel(area.width());
            int ph = UnitConverter.toPixel(area.height());
            if (gsoType == GsoControlType.Ellipse) {
                g2d.drawOval(px, py, pw, ph);
            } else if (arc > 0) {
                g2d.drawRoundRect(px, py, pw, ph, arc, arc);
            } else {
                g2d.drawRect(px, py, pw, ph);
            }
        } finally {
            g2d.setColor(savedColor);
            g2d.setStroke(savedStroke);
            if (savedAA != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
        }
    }

    private void drawBorderFillBackground(BorderFill bf, Area area) {
        FillInfo fi = bf.getFillInfo();
        if (fi == null) return;
        drawBackgroundFill(fi, area);
    }

    private void drawBorderFillBorder(BorderFill bf, Area area,
                                      DashRunOrigins hRuns, DashRunOrigins vRuns) {
        int x1 = UnitConverter.toPixelX(area.left());
        int y1 = UnitConverter.toPixelY(area.top());
        int x2 = UnitConverter.toPixelX(area.right());
        int y2 = UnitConverter.toPixelY(area.bottom());

        Color saved = g2d.getColor();
        Stroke savedStroke = g2d.getStroke();
        Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            // 네 변을 함께 넘겨 다중선(이중/삼중)의 모서리·교차점을 중첩 사각형으로 미터링.
            BorderRenderer.drawCellBorders(g2d, bf, x1, y1, x2, y2, hRuns, vRuns);
        } finally {
            g2d.setColor(saved);
            g2d.setStroke(savedStroke);
            if (savedAA != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
        }
    }

    /** 셀 대각선(슬래시/백슬래시)을 그린다. */
    private void drawBorderFillDiagonal(BorderFill bf, Area area) {
        int x1 = UnitConverter.toPixelX(area.left());
        int y1 = UnitConverter.toPixelY(area.top());
        int x2 = UnitConverter.toPixelX(area.right());
        int y2 = UnitConverter.toPixelY(area.bottom());

        Object savedAA = g2d.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        try {
            BorderRenderer.drawCellDiagonals(g2d, bf, x1, y1, x2, y2);
        } finally {
            if (savedAA != null) g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, savedAA);
        }
    }

    private BorderFill borderFillById(int borderFillId) {
        if (borderFillId <= 0) return null;
        try {
            return reader.hwpFile().getDocInfo().getBorderFillList().get(borderFillId - 1);
        } catch (Exception e) {
            return null;
        }
    }

    // ── 색상 변환 ─────────────────────────────────────────────────────────

    private static Color color4ByteToColor(kr.dogfoot.hwplib.object.etc.Color4Byte c) {
        if (c == null) return Color.BLACK;
        return new Color(c.getR(), c.getG(), c.getB());
    }

}
