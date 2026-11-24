package kr.dogfoot.hwpdrawer.document.text;

import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.List;

/**
 * 한 줄의 텍스트.
 * 하나 이상의 TextPart로 구성된다.
 */
public class TextLine {
    public enum Alignment {
        Justify, Left, Right, Center, Distribute, Divide
    }

    private int paraIndex;
    /** 이 라인이 문단 내에서 시작하는 글자 index (charList 기준). LineSegSynthesizer가 사용. */
    private int startCharIdx;
    private Area area = new Area();
    private Alignment alignment = Alignment.Left;
    private long maxCharHeight;
    private long maxCharDescent;
    private long maxCharBaseSize;
    private boolean firstInPara;
    private boolean lastInPara;
    private boolean endsWithLineBreak;   // 강제 줄나눔(Shift+Enter)으로 끝난 줄. 글자로는 남지 않는다.
    /** 문단 테두리를 그리기 위한 BorderFill ID (1-based). 0이면 테두리 없음. */
    private int borderFillId;
    /** 문단 좌측 끝 X (문단 테두리 좌측 가장자리 계산에 사용). */
    private long paraLeft;
    /** 문단 우측 끝 X (문단 테두리 우측 가장자리 계산에 사용). */
    private long paraRight;
    /** 문단 위쪽 여백(topParaSpace). 첫 라인의 top에서 이만큼 위로 확장해 테두리를 그린다. */
    private long paraTopSpace;
    /** 문단 아래쪽 여백(bottomParaSpace). 마지막 라인의 bottom에서 이만큼 아래로 확장해 테두리를 그린다. */
    private long paraBottomSpace;
    /** 이 라인의 line gap (=LineSegItem.lineSpace). 마지막 라인의 경우 문단 테두리를 아래로 확장할 때 사용. */
    private long lineGap;
    private final List<TextPart> parts = new ArrayList<>();

    public int paraIndex() { return paraIndex; }
    public TextLine paraIndex(int index) { this.paraIndex = index; return this; }

    public int startCharIdx() { return startCharIdx; }
    public TextLine startCharIdx(int idx) { this.startCharIdx = idx; return this; }

    public Area area() { return area; }

    public Alignment alignment() { return alignment; }
    public TextLine alignment(Alignment alignment) { this.alignment = alignment; return this; }

    public long maxCharHeight() { return maxCharHeight; }
    public TextLine maxCharHeight(long h) { this.maxCharHeight = h; return this; }

    public long maxCharDescent() { return maxCharDescent; }
    public TextLine maxCharDescent(long d) { this.maxCharDescent = d; return this; }

    public long maxCharBaseSize() { return maxCharBaseSize; }
    public TextLine maxCharBaseSize(long s) { this.maxCharBaseSize = s; return this; }

    public boolean firstInPara() { return firstInPara; }
    public TextLine firstInPara(boolean first) { this.firstInPara = first; return this; }

    public boolean lastInPara() { return lastInPara; }
    public boolean endsWithLineBreak() { return endsWithLineBreak; }
    public TextLine endsWithLineBreak(boolean b) { this.endsWithLineBreak = b; return this; }
    public TextLine lastInPara(boolean last) { this.lastInPara = last; return this; }

    public int borderFillId() { return borderFillId; }
    public TextLine borderFillId(int id) { this.borderFillId = id; return this; }

    public long paraLeft() { return paraLeft; }
    public TextLine paraLeft(long x) { this.paraLeft = x; return this; }

    public long paraRight() { return paraRight; }
    public TextLine paraRight(long x) { this.paraRight = x; return this; }

    public long paraTopSpace() { return paraTopSpace; }
    public TextLine paraTopSpace(long s) { this.paraTopSpace = s; return this; }

    public long paraBottomSpace() { return paraBottomSpace; }
    public TextLine paraBottomSpace(long s) { this.paraBottomSpace = s; return this; }

    public long lineGap() { return lineGap; }
    public TextLine lineGap(long g) { this.lineGap = g; return this; }

    public List<TextPart> parts() { return parts; }

    public TextPart addPart() {
        TextPart part = new TextPart();
        parts.add(part);
        return part;
    }
}
