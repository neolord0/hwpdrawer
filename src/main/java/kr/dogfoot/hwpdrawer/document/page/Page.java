package kr.dogfoot.hwpdrawer.document.page;

import kr.dogfoot.hwpdrawer.document.Content;
import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.borderfill.EachBorder;

import java.util.ArrayList;
import java.util.List;

/**
 * 문서의 한 페이지를 나타내는 클래스.
 */
public class Page {
    private int pageNo;
    private Area paperArea;     // 용지 전체 크기
    private Area bodyArea;      // 본문 영역 (여백 제외)
    private Content content;    // 본문 콘텐츠
    private Header header;      // 머리글 (optional)
    private Footer footer;      // 꼬리글 (optional)
    private FootnoteArea footnotes;  // 각주 영역 (optional)
    private final List<ControlItem> controls = new ArrayList<>();
    private final List<ColumnSep> columnSeps = new ArrayList<>();
    /** 쪽 번호 표시(PageNumberPosition). null이면 표시 안 함. */
    private String pageNumberText;
    private CharShape pageNumberCharShape;
    private long pageNumberCenterX;    // HWP 단위 (가로 중심)
    private long pageNumberBaselineY;  // HWP 단위 (baseline)

    /** 단 분리선 정보: gap 양쪽 내측 경계 x(HWP 단위), 상단/하단 y, 선 스타일. */
    public static class ColumnSep {
        public final long innerA;  // gap 한쪽 내측 경계 (좌우 레이아웃에서 왼쪽 단 오른쪽 끝)
        public final long innerB;  // gap 반대쪽 내측 경계 (좌우 레이아웃에서 오른쪽 단 왼쪽 끝)
        public final long top;
        public long bottom;
        public final EachBorder border;
        public ColumnSep(long innerA, long innerB, long top, long bottom, EachBorder border) {
            this.innerA = innerA; this.innerB = innerB;
            this.top = top; this.bottom = bottom; this.border = border;
        }
    }

    public Page(int pageNo) {
        this.pageNo = pageNo;
        this.paperArea = new Area();
        this.bodyArea = new Area();
        this.content = new Content();
    }

    public int pageNo() { return pageNo; }

    public Area paperArea() { return paperArea; }
    public Page paperArea(Area area) { this.paperArea = area; return this; }

    public Area bodyArea() { return bodyArea; }
    public Page bodyArea(Area area) { this.bodyArea = area; return this; }

    public Content content() { return content; }

    public Header header() { return header; }
    public Page header(Header header) { this.header = header; return this; }

    public Footer footer() { return footer; }
    public Page footer(Footer footer) { this.footer = footer; return this; }

    public FootnoteArea footnotes() { return footnotes; }
    public Page footnotes(FootnoteArea area) { this.footnotes = area; return this; }

    public List<ControlItem> controls() { return controls; }
    public void addControl(ControlItem item) { controls.add(item); }

    public List<ColumnSep> columnSeps() { return columnSeps; }
    public void addColumnSep(ColumnSep sep) { columnSeps.add(sep); }

    public String pageNumberText() { return pageNumberText; }
    public Page pageNumberText(String t) { this.pageNumberText = t; return this; }
    public CharShape pageNumberCharShape() { return pageNumberCharShape; }
    public Page pageNumberCharShape(CharShape cs) { this.pageNumberCharShape = cs; return this; }
    public long pageNumberCenterX() { return pageNumberCenterX; }
    public Page pageNumberCenterX(long x) { this.pageNumberCenterX = x; return this; }
    public long pageNumberBaselineY() { return pageNumberBaselineY; }
    public Page pageNumberBaselineY(long y) { this.pageNumberBaselineY = y; return this; }
}
