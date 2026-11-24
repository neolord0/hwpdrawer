package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.document.control.GsoContainerItem;
import kr.dogfoot.hwpdrawer.document.control.GsoTextBoxItem;
import kr.dogfoot.hwpdrawer.document.control.TableItem;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.charshape.CharShapeProperty;
import kr.dogfoot.hwplib.object.docinfo.charshape.OutterLineSort;
import kr.dogfoot.hwplib.object.docinfo.charshape.ShadowSort;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 한 흐름(본문 한 쪽, 셀, 글상자)의 글자 뒤에 있는 것 — 밝은 글자가 밝은 바탕 위라 안 보이는지 가린다
 * (흰 바탕의 흰 글씨: 서식의 "장년 월 일"에서 흰 "장").
 *
 * <p>바탕은 글자 한가운데 점 아래를 위에 그려진 것부터 본다: 글자 음영 → 글자 테두리/배경 → 문단 테두리/배경 →
 * 그 흐름 쪽에 칠한 개체(도형·그림·글상자·셀 채우기) → 흐름 자신의 채우기(셀·글상자) → 바깥 흐름 → 쪽 바탕
 * ({@link Paper}, 보통 흰 종이). 색을 모르는 칠(그림, 그라데이션, 쪽 배경·바탕쪽)이면 보인다고 본다 — 잘못
 * 빼느니 남긴다.</p>
 *
 * <p>밝은 글자만 본다. 어두운 글자가 어두운 칠과 겹치는 것은 이 엔진이 칠을 잘못 읽었을 가능성이 더 크다
 * (반투명 칠 등). 그림자·양각·음각·외곽선이 있는 글자는 바탕과 같은 색이어도 보인다. 문단 전체가 숨은
 * 글자인지는 {@link ParagraphText}가 따로 본다.</p>
 */
final class Backdrop {
    /** 글자와 바탕의 밝기 차가 이보다 작으면 보이지 않는다 (흰 글씨 + 흰 바탕 = 0, 연회색 안내글 + 흰 바탕 ≈ 0.25). */
    private static final double MIN_VISIBLE_CONTRAST = 0.1;
    /** 이보다 밝은 글자만 숨은 글자일 수 있다. */
    private static final double MIN_LIGHT_INK = 0.85;
    private static final int PAPER = 0xFFFFFF;

    /** 아무 글자도 숨기지 않는 바탕 (문단 전체가 밝은 글자일 때). */
    static final Backdrop KEEP_ALL = new Backdrop(null, Fill.UNKNOWN, Collections.emptyList(), null, 0, Fill.UNKNOWN);

    private final HWPFile file;
    private final Fill fill;
    private final List<PaintedArea> painted;
    private final Backdrop outer;
    /** 이 흐름이 바깥 흐름 좌표에서 아래로 밀려 그려지는 양 (셀·글상자의 세로 정렬). */
    private final long yOffset;
    /** 맨 아래 쪽 바탕 (바깥 흐름이 없을 때). */
    private final Fill paper;

    private Backdrop(HWPFile file, Fill fill, List<PaintedArea> painted, Backdrop outer, long yOffset, Fill paper) {
        this.file = file;
        this.fill = fill;
        this.painted = painted;
        this.outer = outer;
        this.yOffset = yOffset;
        this.paper = paper;
    }

    /** 쪽 하나 (본문·각주). file이 null이면 채우기를 모른다고 보고 흰 종이 위 칠한 개체만 본다. */
    static Backdrop page(HWPFile file, List<ControlItem> pageControls) {
        return new Backdrop(file, Fill.NONE, paintedAreasOf(file, pageControls), null, 0, Paper.of(file));
    }

    /** 같은 문서의 다른 쪽 (여러 쪽으로 나뉜 표의 다른 조각이 놓인 쪽). */
    Backdrop onPage(List<ControlItem> pageControls) {
        Backdrop root = this;
        while (root.outer != null) root = root.outer;
        return new Backdrop(file, Fill.NONE, paintedAreasOf(file, pageControls), null, 0, root.paper);
    }

    /**
     * 이 흐름 안의 셀 내용. 셀 채우기(BorderFill, 나뉜 셀은 조각마다 같다)가 바탕이다.
     *
     * @param yOffset 셀 내용의 세로 정렬로 아래로 밀려 그려지는 양
     */
    Backdrop inCell(int borderFillId, long yOffset) {
        return new Backdrop(file, Fill.ofBorderFill(file, borderFillId), Collections.emptyList(), this, yOffset, paper);
    }

    /** 이 흐름 안의 글상자·그리기 개체 내용. 개체 채우기가 바탕이다. */
    Backdrop inControl(ControlItem control) {
        long offset = control instanceof GsoTextBoxItem ? ((GsoTextBoxItem) control).verticalAlignmentOffset() : 0;
        return new Backdrop(file, Fill.ofControl(control), Collections.emptyList(), this, offset, paper);
    }

    /** 이 흐름의 쪽 하나 (셀·글상자 안 문서의 쪽). 그 쪽의 개체가 칠한 곳을 더한다. */
    Backdrop withPage(List<ControlItem> pageControls) {
        if (pageControls.isEmpty()) return this;
        return new Backdrop(file, fill, paintedAreasOf(file, pageControls), outer, yOffset, paper);
    }

    /** 이 글자가 바탕과 같은 색이라 보이지 않는가. */
    boolean hides(CharInfo ch, TextLine line, TextPart part) {
        CharShape shape = ch.charShape();
        if (shape == null || shape.getCharColor() == null || hasVisibleEffect(shape)) return false;
        int ink = rgbOf(shape.getCharColor());
        if (luminance(ink) < MIN_LIGHT_INK) return false;
        Fill under = firstPainted(Fill.of(shape.getShadeColor()), Fill.ofBorderFill(file, shape.getBorderFillId()),
                Fill.ofBorderFill(file, line.borderFillId()));
        if (under.isPainted()) return hidesOn(ink, under);
        double x = part.startX() + ch.x() + ch.width() / 2;
        double y = (line.area().top() + line.area().bottom()) / 2.0;
        return hides(ink, x, y);
    }

    /** 그림자·양각·음각·외곽선은 바탕과 같은 색의 글자도 보이게 한다. */
    private static boolean hasVisibleEffect(CharShape shape) {
        CharShapeProperty property = shape.getProperty();
        if (property == null) return false;
        return (property.getShadowSort() != null && property.getShadowSort() != ShadowSort.None)
                || property.isEmboss() || property.isEngrave()
                || (property.getOutterLineSort() != null && property.getOutterLineSort() != OutterLineSort.None);
    }

    private static Fill firstPainted(Fill... fills) {
        for (Fill each : fills) {
            if (each.isPainted()) return each;
        }
        return Fill.NONE;
    }

    /** 위에 그려진 것부터: 이 흐름 쪽의 칠한 개체 → 흐름 자신의 채우기 → 바깥 흐름 → 쪽 바탕. */
    private boolean hides(int ink, double x, double y) {
        boolean coveredBySimilar = false;
        for (PaintedArea area : painted) {
            if (!area.contains(x, y)) continue;
            if (!hidesOn(ink, area.fill)) return false;
            coveredBySimilar = true;
        }
        if (coveredBySimilar) return true;
        if (fill.isPainted()) return hidesOn(ink, fill);
        if (outer != null) return outer.hides(ink, x, y + yOffset);
        return paper.isPainted() ? hidesOn(ink, paper) : isSimilar(ink, PAPER);
    }

    private static boolean hidesOn(int ink, Fill under) {
        return under.isKnown() && isSimilar(ink, under.rgb());
    }

    private static boolean isSimilar(int a, int b) {
        return Math.abs(luminance(a) - luminance(b)) < MIN_VISIBLE_CONTRAST;
    }

    private static double luminance(int rgb) {
        return (0.2126 * (rgb >> 16 & 0xFF) + 0.7152 * (rgb >> 8 & 0xFF) + 0.0722 * (rgb & 0xFF)) / 255;
    }

    private static int rgbOf(Color4Byte color) {
        return (color.getR() << 16) | (color.getG() << 8) | color.getB();
    }

    /** 쪽의 개체가 칠한 곳: 도형·그림·글상자는 개체 영역, 표는 셀마다, 묶음은 자식마다. */
    private static List<PaintedArea> paintedAreasOf(HWPFile file, List<ControlItem> controls) {
        List<PaintedArea> areas = new ArrayList<>();
        for (ControlItem control : controls) addPainted(file, control, areas);
        return areas;
    }

    private static void addPainted(HWPFile file, ControlItem control, List<PaintedArea> areas) {
        if (control instanceof TableItem) {
            for (TableItem.CellLayout cell : ((TableItem) control).cells()) {
                Fill cellFill = Fill.ofBorderFill(file, cell.cell().getListHeader().getBorderFillId());
                if (cellFill.isPainted()) areas.add(new PaintedArea(cell.area(), cellFill));
            }
        } else if (control instanceof GsoContainerItem) {
            for (ControlItem child : ((GsoContainerItem) control).children()) addPainted(file, child, areas);
        } else {
            Fill controlFill = Fill.ofControl(control);
            if (controlFill.isPainted()) areas.add(new PaintedArea(control.area(), controlFill));
        }
    }

    /** 칠한 사각형 하나 (HWP 단위). */
    private static final class PaintedArea {
        private final Area area;
        private final Fill fill;

        PaintedArea(Area area, Fill fill) {
            this.area = area;
            this.fill = fill;
        }

        boolean contains(double x, double y) {
            return x >= area.left() && x < area.right() && y >= area.top() && y < area.bottom();
        }
    }
}
