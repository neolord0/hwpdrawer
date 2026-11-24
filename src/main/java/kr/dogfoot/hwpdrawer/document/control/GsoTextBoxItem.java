package kr.dogfoot.hwpdrawer.document.control;

import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlRectangle;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.util.Area;

/** 글상자(텍스트 박스) 컨트롤 아이템. innerDocument에 내부 문단 레이아웃 결과가 담긴다. */
public class GsoTextBoxItem extends ControlItem {
    private final ControlRectangle rectangle;
    private Document innerDocument;
    /** 세로 정렬 오프셋 (HWP 단위). Top=0, Center=(innerH-contentH)/2, Bottom=innerH-contentH */
    private long verticalAlignmentOffset;

    public GsoTextBoxItem(ControlRectangle rectangle, Area area, Area areaWithMargin, boolean behindText) {
        super(area, areaWithMargin, behindText);
        this.rectangle = rectangle;
    }

    public ControlRectangle rectangle() { return rectangle; }
    public Document innerDocument() { return innerDocument; }
    public void innerDocument(Document doc) { this.innerDocument = doc; }
    public long verticalAlignmentOffset() { return verticalAlignmentOffset; }
    public void verticalAlignmentOffset(long offset) { this.verticalAlignmentOffset = offset; }

    @Override
    public ControlItem cloneWithArea(Area newArea, Area newAreaWithMargin) {
        GsoTextBoxItem copy = new GsoTextBoxItem(rectangle, newArea, newAreaWithMargin, behindText());
        copyCommonAttrs(copy);
        copy.innerDocument = this.innerDocument;
        copy.verticalAlignmentOffset = this.verticalAlignmentOffset;
        return copy;
    }
}
