package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.document.control.GsoPictureItem;
import kr.dogfoot.hwpdrawer.document.control.GsoPolygonItem;
import kr.dogfoot.hwpdrawer.document.control.GsoShapeItem;
import kr.dogfoot.hwpdrawer.document.control.GsoTextBoxItem;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponent;
import kr.dogfoot.hwplib.object.bodytext.control.gso.shapecomponent.ShapeComponentNormal;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.borderfill.fillinfo.FillInfo;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

/**
 * 칸·도형을 칠한 것 — 글자가 보이는지 따질 때의 바탕: 무늬 채우기는 바탕색(0xFFFFFFFF는 색 없음 = 칠하지
 * 않음 — 렌더러의 {@code image/HwpColors}와 같은 규칙), 그라데이션·그림 채우기와 그림 개체는 색을 알 수 없는 칠.
 */
final class Fill {
    /** 칠하지 않음 — 뒤가 비친다. */
    static final Fill NONE = new Fill(false, -1);
    /** 칠했지만 색을 모른다 (그림, 그라데이션). 그 위 글자는 보인다고 본다. */
    static final Fill UNKNOWN = new Fill(true, -1);
    /** HWP 색 값 0xFFFFFFFF는 '색 없음'이다 (기본 테두리/배경, 글자 음영 없음). */
    private static final long NO_COLOR = 0xFFFFFFFFL;

    private final boolean painted;
    private final int rgb;

    private Fill(boolean painted, int rgb) {
        this.painted = painted;
        this.rgb = rgb;
    }

    static Fill solid(int rgb) {
        return new Fill(true, rgb & 0xFFFFFF);
    }

    /** 색 하나로 칠함. '색 없음'(0xFFFFFFFF)이면 칠하지 않음. */
    static Fill of(Color4Byte color) {
        if (color == null || isNoColor(color)) return NONE;
        return solid((color.getR() << 16) | (color.getG() << 8) | color.getB());
    }

    static boolean isNoColor(Color4Byte color) {
        return (color.getValue() & 0xFFFFFFFFL) == NO_COLOR;
    }

    static Fill of(FillInfo fillInfo) {
        if (fillInfo == null || fillInfo.getType() == null) return NONE;
        if (fillInfo.getType().hasImageFill() || fillInfo.getType().hasGradientFill()) return UNKNOWN;
        if (fillInfo.getType().hasPatternFill() && fillInfo.getPatternFill() != null) {
            return of(fillInfo.getPatternFill().getBackColor());
        }
        return NONE;
    }

    /** 테두리/배경 (문서 정보의 BorderFill, 1부터) — 표 셀·문단·글자·쪽이 쓴다. */
    static Fill ofBorderFill(HWPFile file, int borderFillId) {
        if (file == null || borderFillId <= 0) return NONE;
        try {
            BorderFill borderFill = file.getDocInfo().getBorderFillList().get(borderFillId - 1);
            return of(borderFill.getFillInfo());
        } catch (IndexOutOfBoundsException e) {
            return NONE;
        }
    }

    /** 그리기 개체가 칠하는 것. 그림은 색을 모르고, 표·묶음은 여기서 다루지 않는다(셀마다, 자식마다 본다). */
    static Fill ofControl(ControlItem control) {
        if (control instanceof GsoPictureItem) return UNKNOWN;
        if (control instanceof GsoTextBoxItem) return ofShape(((GsoTextBoxItem) control).rectangle());
        if (control instanceof GsoShapeItem) return ofShape(((GsoShapeItem) control).gso());
        if (control instanceof GsoPolygonItem) return ofShape(((GsoPolygonItem) control).gso());
        return NONE;
    }

    private static Fill ofShape(GsoControl gso) {
        if (gso == null) return NONE;
        ShapeComponent component = gso.getShapeComponent();
        return component instanceof ShapeComponentNormal ? of(((ShapeComponentNormal) component).getFillInfo()) : NONE;
    }

    boolean isPainted() {
        return painted;
    }

    boolean isKnown() {
        return rgb >= 0;
    }

    int rgb() {
        return rgb;
    }
}
