package kr.dogfoot.hwpdrawer.document.control;

import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwpdrawer.util.Area;

/** 도형(GSO) 컨트롤 아이템. 이미지/글상자 이외의 GSO(사각형 테두리 없는 도형, 타원 등). */
public class GsoShapeItem extends ControlItem {
    private final GsoControl gso;

    public GsoShapeItem(GsoControl gso, Area area, Area areaWithMargin, boolean behindText) {
        super(area, areaWithMargin, behindText);
        this.gso = gso;
    }

    public GsoControl gso() { return gso; }

    @Override
    public ControlItem cloneWithArea(Area newArea, Area newAreaWithMargin) {
        GsoShapeItem copy = new GsoShapeItem(gso, newArea, newAreaWithMargin, behindText());
        copyCommonAttrs(copy);
        return copy;
    }
}
