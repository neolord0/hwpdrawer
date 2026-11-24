package kr.dogfoot.hwpdrawer.engine.para.nolineseg;

import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import java.util.List;

/**
 * 현재 라인의 상태(wordsWidth/spacesWidth/metrics)를 보유.
 *
 * <p>≈ old_version {@code TextLineDrawer}. 공백 폭과 비공백(word) 폭을 분리 누적해
 * 최소공백 축소 계산(`currentTextX(true)`)에 활용한다.</p>
 */
final class LineWriter {
    private final ParaShape paraShape;

    private TextLine textLine;
    private TextPart textPart;
    private double segmentWidth;

    private double wordsWidth;
    private double spacesWidth;
    private PendingChar lastAdded;
    private double maxHeight;
    private double maxDescent;
    private double maxBaseSize;
    private double maxAscent;
    private double maxTextHeight;
    private double maxTextDescent;
    private boolean justNewLine;

    LineWriter(ParaShape paraShape) {
        this.paraShape = paraShape;
    }

    void reset(TextLine textLine, TextPart textPart, double segmentWidth) {
        this.textLine = textLine;
        this.textPart = textPart;
        this.segmentWidth = segmentWidth;
        this.wordsWidth = 0;
        this.spacesWidth = 0;
        this.lastAdded = null;
        this.maxHeight = 0;
        this.maxDescent = 0;
        this.maxBaseSize = 0;
        this.maxAscent = 0;
        this.maxTextHeight = 0;
        this.maxTextDescent = 0;
        this.justNewLine = true;
    }

    /**
     * 같은 TextLine 안에서 다음 세그먼트로 전환.
     * word/space 누적은 새 세그먼트 기준으로 초기화하되, 라인 높이 관련 max 메트릭은 유지한다.
     */
    void beginSegment(TextPart textPart, double segmentWidth) {
        this.textPart = textPart;
        this.segmentWidth = segmentWidth;
        this.wordsWidth = 0;
        this.spacesWidth = 0;
        // max* 메트릭은 라인 전체 누적 — 유지.
        // justNewLine: 이전 세그먼트에 글자가 있으면 라인 자체는 비어있지 않다. 하지만 새 세그먼트는
        // 비어있으므로 이어지는 save 요청에서 "빈 세그먼트 save"가 되는 걸 막기 위해 true로 둔다.
        this.justNewLine = true;
    }

    /** 지금 라인의 남은 폭으로 주어진 width의 글자/단어가 들어가는지 검사. */
    boolean isOverWidth(double width, boolean applyMinimumSpace) {
        return currentTextX(applyMinimumSpace) + width > segmentWidth;
    }

    double currentTextX(boolean applyMinimumSpace) {
        if (applyMinimumSpace) {
            int minSpaceProp = paraShape.getProperty1().getMinimumSpace();
            return wordsWidth + spacesWidth * (100.0 - minSpaceProp) / 100.0;
        }
        return wordsWidth + spacesWidth;
    }

    /** 라인에 글자 하나 추가. */
    void addChar(PendingChar pc) {
        CharInfo ci = pc.charInfo;
        // 라인의 첫 글자이면 startCharIdx를 이 글자의 index로 확정한다.
        // 다중 세그먼트에서 두 번째 이후 세그먼트가 시작 인덱스를 덮어쓰지 않도록,
        // 이 TextPart가 textLine의 첫 번째 파트일 때만 설정.
        if (textPart.charInfos().isEmpty() && textLine.parts().get(0) == textPart) {
            textLine.startCharIdx(pc.charIdx);
        }
        ci.x(currentTextX(false));
        ci.charIdx(pc.charIdx);
        textPart.addCharInfo(ci);

        if (ci.height() > maxHeight) maxHeight = ci.height();
        if (ci.descent() > maxDescent) maxDescent = ci.descent();
        // baseSize 필드에 shapeAscent를 넣어두었으므로 그대로 비교.
        if (ci.baseSize() > maxBaseSize) maxBaseSize = ci.baseSize();
        // char별 ascent (= 베이스라인 위 높이) max — 인라인 컨트롤을 "큰 글자"로 취급해
        // 베이스라인을 아래로 내리는 효과를 준다.
        double charAscent = ci.height() - ci.descent();
        if (charAscent > maxAscent) maxAscent = charAscent;
        // 텍스트 문자만의 max height/descent — 인라인 컨트롤은 제외.
        // lineGap 공식은 텍스트 기준으로 계산되어야 함(HWP 컨벤션). 거대 인라인 컨트롤이
        // 라인에 있어도 gap은 주변 글자 크기에 따라 결정된다.
        if (!ci.inlineControl()) {
            if (ci.height() > maxTextHeight) maxTextHeight = ci.height();
            if (ci.descent() > maxTextDescent) maxTextDescent = ci.descent();
        }

        if (ci.isSpace()) {
            spacesWidth += pc.advance;
        } else {
            wordsWidth += pc.advance;
        }
        lastAdded = pc;
        justNewLine = false;
    }

    /**
     * 마지막 추가된 글자를 라인에서 제거하고 반환한다.
     * 라인이 비어 있거나 마지막 글자가 유일한 글자면 null을 반환(무한루프 방지).
     */
    PendingChar popLastChar() {
        List<CharInfo> charInfos = textPart.charInfos();
        if (charInfos.size() <= 1) return null;   // 첫 글자는 팝하지 않는다
        charInfos.remove(charInfos.size() - 1);
        PendingChar removed = lastAdded;
        if (removed != null) {
            if (removed.charInfo.isSpace()) spacesWidth -= removed.advance;
            else                            wordsWidth  -= removed.advance;
        }
        lastAdded = null;
        return removed;
    }

    boolean noDrawingChar() { return textPart.charInfos().isEmpty(); }
    boolean justNewLine() { return justNewLine; }

    TextLine textLine() { return textLine; }
    TextPart textPart() { return textPart; }
    double segmentWidth() { return segmentWidth; }

    double wordsWidth() { return wordsWidth; }
    double spacesWidth() { return spacesWidth; }
    double maxHeight() { return maxHeight; }
    double maxDescent() { return maxDescent; }
    double maxBaseSize() { return maxBaseSize; }
    double maxAscent() { return maxAscent; }
    double maxTextHeight() { return maxTextHeight; }
    double maxTextDescent() { return maxTextDescent; }
}
