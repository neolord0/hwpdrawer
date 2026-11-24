package kr.dogfoot.hwpdrawer.document.control;

import kr.dogfoot.hwpdrawer.util.Area;

/** 페이지에 배치되는 컨트롤(이미지/글상자/도형/표)의 공통 기반. */
public abstract class ControlItem {
    private final Area area;
    private final Area areaWithMargin;
    private final boolean behindText;
    private int zOrder;
    /** anchor TextLine의 top y (inner doc 좌표). 셀 컨트롤이 segment로 라우팅될 때 사용. */
    private long anchorLineTop = Long.MIN_VALUE;
    /** anchor ControlExtend 문자의 charIdx (paragraph 안). 라우팅 결정에 사용. */
    private int anchorCharIdx = -1;
    /**
     * 수직 위치가 anchor 문단/줄에 상대적인지(vertRelTo=Para·Line). true면 anchor 줄이
     * 후처리로 다른 Y/페이지로 옮겨질 때 컨트롤도 같은 delta만큼 따라 옮겨야 한다.
     * Paper·Page 기준(절대 좌표) 컨트롤은 false — anchor 줄 이동과 무관.
     */
    private boolean yRelativeToAnchor = false;

    protected ControlItem(Area area, Area areaWithMargin, boolean behindText) {
        this.area = area;
        this.areaWithMargin = areaWithMargin;
        this.behindText = behindText;
    }

    public Area area() { return area; }
    public Area areaWithMargin() { return areaWithMargin; }
    public boolean behindText() { return behindText; }
    public int zOrder() { return zOrder; }
    public void zOrder(int z) { this.zOrder = z; }
    public long anchorLineTop() { return anchorLineTop; }
    public void anchorLineTop(long y) { this.anchorLineTop = y; }
    public int anchorCharIdx() { return anchorCharIdx; }
    public void anchorCharIdx(int c) { this.anchorCharIdx = c; }
    public boolean yRelativeToAnchor() { return yRelativeToAnchor; }
    public void yRelativeToAnchor(boolean b) { this.yRelativeToAnchor = b; }

    /**
     * 셀이 페이지 경계를 가로지를 때 양쪽 segment 모두에 그리기 위해 새 area로
     * 같은 컨트롤 인스턴스를 복제한다. 원본의 컨트롤 참조와 zOrder 등 속성은 유지.
     */
    public abstract ControlItem cloneWithArea(Area newArea, Area newAreaWithMargin);

    /** 공통 속성 복사 — 서브클래스의 cloneWithArea 구현에서 호출. */
    protected final void copyCommonAttrs(ControlItem copy) {
        copy.zOrder = this.zOrder;
        copy.anchorLineTop = this.anchorLineTop;
        copy.anchorCharIdx = this.anchorCharIdx;
        copy.yRelativeToAnchor = this.yRelativeToAnchor;
    }
}
