package kr.dogfoot.hwpdrawer.document.page;

import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;

import java.util.ArrayList;
import java.util.List;

/**
 * 한 페이지의 각주 묶음과 그 위의 구분선.
 *
 * <p>선 정보를 문서 모델에 담고 Export가 그리는 방식은 {@link Page.ColumnSep}(단 분리선)의 선례를 따랐다.</p>
 */
public class FootnoteArea {
    private final List<Footnote> notes = new ArrayList<>();

    /** 구분선 y (HWP 단위). */
    private final long divideLineY;
    /** 구분선 좌/우 끝 x (HWP 단위). */
    private final long divideLineLeft;
    private final long divideLineRight;
    /** 구분선 스타일. null이면 그리지 않는다. */
    private final EachBorder divideLineBorder;

    public FootnoteArea(long divideLineY, long divideLineLeft, long divideLineRight,
                        EachBorder divideLineBorder) {
        this.divideLineY = divideLineY;
        this.divideLineLeft = divideLineLeft;
        this.divideLineRight = divideLineRight;
        this.divideLineBorder = divideLineBorder;
    }

    public List<Footnote> notes() { return notes; }
    public long divideLineY() { return divideLineY; }
    public long divideLineLeft() { return divideLineLeft; }
    public long divideLineRight() { return divideLineRight; }
    public EachBorder divideLineBorder() { return divideLineBorder; }
}
