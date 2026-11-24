package kr.dogfoot.hwpdrawer.engine.para.nolineseg;

import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.CtrlHeaderGso;
import kr.dogfoot.hwplib.object.bodytext.control.gso.GsoControl;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.engine.control.ControlAreaCalculator;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForEnglish;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineDivideForHangul;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.engine.CharMetrics;
import kr.dogfoot.hwpdrawer.engine.CharMetricsMeasurer;
import kr.dogfoot.hwpdrawer.engine.ParaShapeHelper;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.engine.para.ParaLayoutContext;
import kr.dogfoot.hwpdrawer.engine.para.ParagraphLayouter;
import kr.dogfoot.hwpdrawer.engine.para.common.ParagraphIndent;
import kr.dogfoot.hwpdrawer.engine.para.common.TabStopHelper;
import kr.dogfoot.hwpdrawer.engine.para.nolineseg.obstacle.ParaObstacles;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import java.util.ArrayList;
import java.util.List;

/**
 * LineSeg 정보 없이 직접 줄 나누기를 계산하는 레이아웃터.
 *
 * <p>old_version의 구조를 그대로 옮겼다:
 * <pre>
 *   NoLineSegLayouter ≈ ParaDrawerWithoutLineSeg + CharAdder (문단 iteration/라인 열기닫기)
 *   WordLayouter      ≈ WordDrawer
 *   WordSplitter      ≈ WordSplitter
 *   LineWriter        ≈ TextLineDrawer
 *   LineFinisher      ≈ (saveToOutput + minSpace/alignment 마무리)
 * </pre>
 *
 * 핵심 흐름: 비공백은 {@link WordLayouter}의 버퍼(charsOfWord)에 쌓고,
 * 공백을 만나면 {@code addWordToLine()}이 버퍼된 word 폭을 보고
 *  (1) 그대로 들어가면 추가+공백, (2) 최소공백 축소로 맞으면 추가+축소+개행,
 *  (3) 그래도 안 들어가면 spanningWord로 언어 단위 분리.
 * 이미 줄에 들어간 글자를 되돌리는 rollback이 필요 없어 계산이 단순하다.</p>
 *
 * <p>{@link #layout} 호출마다 내부적으로 NoLineSegLayouter 인스턴스를 새로 만들어
 * 상태를 보유하고 {@link #run()}을 돌린다 — 공용 인스턴스(= ParagraphLayouter 구현체)
 * 자체는 상태를 갖지 않는다.</p>
 */
public class NoLineSegLayouter implements ParagraphLayouter {

    // ====================================================================
    // per-run 상태 (layout() 호출마다 새 인스턴스로 채워진다)
    // ====================================================================

    private Paragraph para;
    private int paraIdx;
    private ParaShape paraShape;
    private long leftMargin;
    private long rightMargin;
    private long lineWidthTotal;
    private long indent;
    private TextLine.Alignment alignment;
    private ParaLayoutContext ctx;
    private List<HWPChar> charList;
    private CharMetricsMeasurer measurer;
    private HWPFileReader reader;

    private boolean hangulByWord;
    private boolean englishByWord;

    private LineWriter line;
    private WordLayouter wordLayouter;

    private boolean firstLine = true;
    private int controlIdx = 0;
    /** run() 루프가 현재 처리 중인 글자 index — 다음 라인 높이 추정의 시작점. */
    private int currentCharIdx = 0;

    // 현재 라인의 수평 세그먼트(어울림 래핑). 항상 최소 1개 — 장애물이 없으면 길이 1.
    private List<ParaObstacles.Segment> currentSegments;
    private int currentSegmentIdx;
    /** 현재 라인을 연 시점의 Y. 세그먼트를 이어갈 때 동일 Y를 유지하는데 쓰인다. */
    private long currentLineTop;

    public NoLineSegLayouter() {
    }

    // ====================================================================
    // 진입점
    // ====================================================================

    @Override
    public void layout(Paragraph para, int paraIdx, ParaShape paraShape,
                       long leftMargin, long rightMargin, long indent,
                       TextLine.Alignment alignment, ParaLayoutContext ctx) {
        NoLineSegLayouter run = new NoLineSegLayouter();
        run.init(para, paraIdx, paraShape, leftMargin, rightMargin, indent, alignment, ctx);
        run.run();
    }

    private void init(Paragraph para, int paraIdx, ParaShape paraShape,
                      long leftMargin, long rightMargin, long indent,
                      TextLine.Alignment alignment, ParaLayoutContext ctx) {
        this.para = para;
        this.paraIdx = paraIdx;
        this.paraShape = paraShape;
        this.leftMargin = leftMargin;
        this.rightMargin = rightMargin;
        this.lineWidthTotal = rightMargin - leftMargin;
        this.indent = indent;
        this.alignment = alignment;
        this.ctx = ctx;
        this.charList = (para.getText() != null) ? para.getText().getCharList() : new ArrayList<>();
        this.measurer = ctx.measurer();
        this.reader = ctx.reader();

        LineDivideForHangul divideH = paraShape.getProperty1().getLineDivideForHangul();
        LineDivideForEnglish divideE = paraShape.getProperty1().getLineDivideForEnglish();
        this.hangulByWord = (divideH == LineDivideForHangul.ByWord);
        this.englishByWord = (divideE == LineDivideForEnglish.ByWord);

        this.controlIdx = 0;
        this.line = new LineWriter(paraShape);
        this.wordLayouter = new WordLayouter(this, line, paraShape,
                hangulByWord, englishByWord);
    }

    private void run() {
        openLine();

        int i = 0;
        while (i < charList.size()) {
            currentCharIdx = i;
            HWPChar hwpChar = charList.get(i);
            if (hwpChar.getType() == HWPCharType.Normal) {
                processNormalChar(hwpChar, i);
            } else {
                int code = hwpChar.getCode();
                if (code == 13) break;          // CR — 문단 끝
                if (code == 10) {               // LINE FEED — 강제 줄바꿈
                    wordLayouter.addWordToLine(null);
                    saveTextLineAndNewLine();
                } else if (code == 9 && hwpChar.getType() == HWPCharType.ControlInline) {
                    processTab(i);              // 탭 — 다음 탭 스톱까지 전진(줄바꿈 계산용)
                } else if (code == 11) {        // ControlExtend — 컨트롤 삽입
                    processControlExtend(i);
                } else if (HWPFileReader.isFixedWidthBlank(hwpChar)) {
                    processFixedWidthBlank(i); // 묶음/고정폭 빈칸 — 공백 폭, 비분배·비분리
                } else if (hwpChar.getType() == HWPCharType.ControlExtend) {
                    controlIdx++;              // 헤더/푸터 등 다른 ControlExtend도 인덱스 동기화
                }
            }
            i++;
        }

        wordLayouter.addWordToLine(null);

        if (line.noDrawingChar() && !firstLine) {
            ctx.currentColumn().textLines().remove(line.textLine());
            return;
        }
        finishLine(true);
    }

    private void processControlExtend(int charIdx) {
        java.util.List<Control> controls = para.getControlList();
        if (controls == null || controlIdx >= controls.size()) {
            controlIdx++;
            return;
        }
        Control ctrl = controls.get(controlIdx);
        controlIdx++;

        CtrlHeaderGso header = gsoHeaderOf(ctrl);
        if (header == null || !header.getProperty().isLikeWord()) return;

        long ctrlWidth = ControlAreaCalculator.resolveWidth(header, ctx.paperArea(), ctx.bodyArea());
        long ctrlHeight = ControlAreaCalculator.resolveHeight(header, ctx.paperArea(), ctx.bodyArea());
        long totalWidth = ctrlWidth + header.getOutterMarginLeft() + header.getOutterMarginRight();
        long totalHeight = ctrlHeight + header.getOutterMarginTop() + header.getOutterMarginBottom();

        // 주변 CharShape의 ascent를 baseSize에 실어 두면, 인라인 컨트롤만 있는 라인에서도
        // lineGap이 텍스트 기준으로 계산된다(HWP LineSeg와 동일한 lineHeight 산출).
        CharShape charShape = reader.getCharShape(para, charIdx);
        double shapeAscent = measurer.ascent(charShape);

        CharInfo ci = new CharInfo()
                .ch('\u0000')
                .width(totalWidth)
                .height(totalHeight)
                .descent(0)
                .baseSize(shapeAscent)
                .isSpace(false)
                .inlineControl(true);

        PendingChar pc = new PendingChar(ci, totalWidth, false, false, charIdx);
        wordLayouter.addCharOfWord(pc);
    }

    private static CtrlHeaderGso gsoHeaderOf(Control ctrl) {
        switch (ctrl.getType()) {
            case Table: return ((ControlTable) ctrl).getHeader();
            case Gso:   return ((GsoControl) ctrl).getHeader();
            default:    return null;
        }
    }

    private void processNormalChar(HWPChar hwpChar, int charIdx) {
        char ch = HWPFileReader.normalizeChar((char) hwpChar.getCode());
        CharShape charShape = reader.getCharShape(para, charIdx);
        CharMetrics m = measurer.measure(ch, charShape);

        boolean isSpace = (ch == ' ' || ch == '\t');
        double advance = m.width + measurer.charSpacing(charShape, ch, m.width);
        double shapeAscent = measurer.ascent(charShape);

        CharInfo charInfo = new CharInfo()
                .ch(ch)
                .charShape(charShape)
                .width(m.width)
                .height(m.height)
                .descent(m.descent)
                .baseSize(m.baseSize)
                .isSpace(isSpace);

        // baseSize 필드에 shapeAscent를 보존 — LineWriter.addChar에서 maxBaseSize 집계에 사용.
        charInfo.baseSize(shapeAscent);

        PendingChar pc = new PendingChar(
                charInfo, advance,
                HWPFileReader.isHangul(ch),
                Character.isDigit(ch),
                charIdx);

        if (isSpace) {
            wordLayouter.addWordToLine(pc);
        } else {
            wordLayouter.addCharOfWord(pc);
        }
    }

    /**
     * 묶음 빈칸(30)/고정폭 빈칸(31)을 일반 공백의 절반 폭(baseSize/4)의 단어 글자로
     * 배치한다 (한글 측정). isSpace=false + addCharOfWord 로 단어를 나누지 않고,
     * 양쪽정렬 분배 대상에서도 제외한다.
     */
    private void processFixedWidthBlank(int charIdx) {
        CharShape charShape = reader.getCharShape(para, charIdx);
        CharMetrics m = measurer.measure(' ', charShape);
        double width = m.width / 2.0;
        double advance = width + measurer.charSpacing(charShape, ' ', width);

        CharInfo charInfo = new CharInfo()
                .ch(' ')
                .charShape(charShape)
                .width(width)
                .height(m.height)
                .descent(m.descent)
                .baseSize(measurer.ascent(charShape))
                .isSpace(false);

        wordLayouter.addCharOfWord(new PendingChar(charInfo, advance, false, false, charIdx));
    }

    /**
     * 탭(인라인 컨트롤 code 9)을 처리한다.
     *
     * <p>NoLineSeg 경로는 이 섀도 패스로 줄바꿈 지점만 계산하고 실제 배치는
     * WithLineSegLayouter가 한다. 따라서 여기서는 정확한 탭 정렬(왼/가운데/오른쪽)이
     * 아니라 <b>탭 폭만큼 현재 x를 다음 탭 스톱으로 전진</b>시켜, 탭 뒤 셀이 세그먼트를
     * 넘칠 때 올바른 위치에서 줄바꿈되도록 한다.</p>
     *
     * <p>먼저 탭 앞 셀(버퍼된 word)을 라인에 반영한다(안 맞으면 통째로 다음 줄로).
     * 그다음 현재 x에서 다음 탭 스톱까지의 폭을 가진 탭 글자를 추가한다.</p>
     */
    private void processTab(int charIdx) {
        wordLayouter.addWordToLine(null);       // 탭 앞 셀 확정(넘치면 셀 통째로 다음 줄)

        double currentX = line.currentTextX(false);
        double tabStopX = TabStopHelper.nextTabStopX(reader, paraShape, currentX,
                ctx.defaultTabGap());
        // 탭 스톱이 세그먼트를 넘으면 이 탭(과 뒤따르는 셀)을 다음 줄로 넘긴다.
        // 한글은 탭 자체가 우측 경계를 넘으면 줄바꿈하고, 그 탭을 새 줄의 선행 채우기로 둔다
        // (오른쪽/가운데 탭에서 다음 줄 첫 셀이 탭 스톱에 정렬되도록).
        if (tabStopX > line.segmentWidth() && !line.noDrawingChar()) {
            saveTextLineAndNewLine();
            currentX = line.currentTextX(false);
            tabStopX = TabStopHelper.nextTabStopX(reader, paraShape, currentX,
                    ctx.defaultTabGap());
        }
        double tabWidth = Math.max(0, tabStopX - currentX);

        CharShape charShape = reader.getCharShape(para, charIdx);
        CharMetrics cm = measurer.measure(' ', charShape);
        CharInfo ci = new CharInfo()
                .ch('\t')
                .charShape(charShape)
                .width(tabWidth)
                .height(cm.height)
                .descent(cm.descent)
                .baseSize(measurer.ascent(charShape))
                .isSpace(false);
        line.addChar(new PendingChar(ci, tabWidth, false, false, charIdx));
    }

    /**
     * WordLayouter가 "라인을 닫고 새 라인 시작" 요청시 호출.
     * 현재 라인에 남은 세그먼트가 있으면 같은 Y에서 다음 세그먼트로 이어간다(어울림 래핑).
     */
    void saveTextLineAndNewLine() {
        if (line.justNewLine()) return;

        if (currentSegmentIdx + 1 < currentSegments.size()) {
            // 현재 파트를 마감하고, 같은 TextLine 안에서 다음 세그먼트로 이동.
            ParaObstacles.Segment prevSeg = currentSegments.get(currentSegmentIdx);
            LineFinisher.freezeSegment(line, paraShape,
                    Math.round(line.textPart().startX()),
                    (long) (line.textPart().startX() + line.segmentWidth()),
                    /*lastInPara*/ false);

            currentSegmentIdx++;
            ParaObstacles.Segment seg = currentSegments.get(currentSegmentIdx);
            TextPart nextPart = line.textLine().addPart();
            nextPart.startX(seg.left);
            line.beginSegment(nextPart, seg.width());
            return;
        }

        finishLine(false);
    }

    // -----------------------------------------------------------------
    // 라인 열기/닫기
    // -----------------------------------------------------------------

    private void openLine() {
        advanceColumnIfOverflow();
        ctx.ensureRowAndColumn();
        TextColumn column = ctx.currentColumn();

        // 장애물을 고려해 첫 세그먼트를 고른다. 완전히 막힌 Y는 건너뛴다.
        long estHeight = estimateLineHeight();
        long lineTop = ctx.currentY();
        List<ParaObstacles.Segment> segs = resolveSegmentsAt(lineTop, estHeight);
        ParaObstacles obs = ctx.obstacles();
        while (segs.isEmpty() && obs != null && !obs.isEmpty()) {
            long skipY = obs.nextSkipY(lineTop, lineTop + estHeight);
            if (skipY <= lineTop) break;
            if (skipY >= ctx.bodyArea().bottom()) break;
            lineTop = skipY;
            segs = resolveSegmentsAt(lineTop, estHeight);
        }
        if (segs.isEmpty()) {
            // 폴백: 최소한 라인 전체 폭 1개 세그먼트.
            segs = new ArrayList<>();
            segs.add(new ParaObstacles.Segment(leftMargin, rightMargin));
        }
        ctx.currentY(lineTop);
        currentLineTop = lineTop;
        currentSegments = segs;
        currentSegmentIdx = 0;

        TextLine tl = column.addTextLine();
        tl.paraIndex(paraIdx);
        tl.startCharIdx(0);
        tl.alignment(alignment);
        tl.firstInPara(firstLine);
        tl.borderFillId(paraShape.getBorderFillId());
        tl.paraLeft(leftMargin);
        tl.paraRight(rightMargin);
        tl.paraTopSpace(ParaShapeHelper.topSpace(paraShape));
        tl.paraBottomSpace(ParaShapeHelper.bottomSpace(paraShape));

        // 들여쓰기는 문단 left 경계를 포함하는 세그먼트에만 적용.
        ParaObstacles.Segment first = segs.get(0);
        double firstIndent = indentFor(first);
        double lineLeft = first.left + firstIndent;
        double segmentWidth = (first.right - first.left) - firstIndent;

        TextPart part = tl.addPart();
        part.startX(lineLeft);
        part.segmentLeft(first.left);
        part.segmentRight(first.right);
        tl.area().left(first.left);
        tl.area().top(lineTop);

        line.reset(tl, part, segmentWidth);
    }

    /**
     * NoLineSeg 다단: 현재 Y가 단 영역 하단을 넘으면 다음 단(올바른 폭)으로 미리 전진한다.
     * 넘침 판정은 {@code estimateLineHeight()}(글자 높이)를 쓴다 —
     * LineFinisher.moveLineToNextPageIfOverflow의 판정 기준(line.maxHeight)과 일치시켜
     * 단/페이지 채움 줄 수가 어긋나지 않게 한다.
     * 다단 columnAdvancer가 없으면(1단) 아무것도 하지 않는다.
     */
    private void advanceColumnIfOverflow() {
        long estHeight = estimateLineHeight();
        int guard = 0;
        while (ctx.currentY() + estHeight > ctx.bodyArea().bottom() && guard++ < 64) {
            if (!ctx.tryNextColumn()) break;
            recomputeMargins();
        }
    }

    /** 단 전환 후 현재 bodyArea 기준으로 좌우 여백·라인 폭을 다시 계산한다. */
    private void recomputeMargins() {
        this.leftMargin = ctx.bodyArea().left() + ParaShapeHelper.leftMargin(paraShape);
        this.rightMargin = ctx.bodyArea().right() - ParaShapeHelper.rightMargin(paraShape);
        this.lineWidthTotal = rightMargin - leftMargin;
    }

    /** 현재 세그먼트가 문단 left 경계를 포함하면 indent 적용. */
    private double indentFor(ParaObstacles.Segment seg) {
        if (seg.left > leftMargin) return 0;
        return ParagraphIndent.offsetFor(indent, firstLine);
    }

    private List<ParaObstacles.Segment> resolveSegmentsAt(long lineTop, long estHeight) {
        ParaObstacles obs = ctx.obstacles();
        if (obs == null || obs.isEmpty()) {
            List<ParaObstacles.Segment> one = new ArrayList<>();
            one.add(new ParaObstacles.Segment(leftMargin, rightMargin));
            return one;
        }
        return obs.computeSegments(lineTop, lineTop + estHeight, leftMargin, rightMargin);
    }

    /**
     * 장애물 교차 판정용 "텍스트 박스 높이" — line-spacing gap을 뺀 실제 글자 높이.
     * (old_version의 {@code maxCharHeight}에 해당). ParaShape lineHeight를 쓰면 gap 영역에 걸린
     * 장애물을 잘못 활성화해 어울림/2 4번째 라인 같은 오탐이 생긴다.
     */
    private long estimateLineHeight() {
        // 다음(현재) 라인의 시작 글자부터 측정 — 문단 첫 글자 기준이면 문단 안에서
        // 글자 크기가 커지는 경우(예: 첫 줄만 작음) 단 넘침 판정을 과소평가한다.
        for (int k = currentCharIdx; k < charList.size(); k++) {
            HWPChar hc = charList.get(k);
            if (hc.getType() == HWPCharType.Normal) {
                char ch = HWPFileReader.normalizeChar((char) hc.getCode());
                CharShape shape = reader.getCharShape(para, k);
                if (shape == null) continue;
                CharMetrics m = measurer.measure(ch, shape);
                return Math.round(m.height);
            }
        }
        CharShape shape = reader.getCharShape(para, 0);
        return (shape != null) ? shape.getBaseSize() : 1000;
    }

    private void finishLine(boolean lastInPara) {
        LineFinisher.finish(line, paraShape, rightMargin, ctx, lastInPara);

        firstLine = false;
        if (!lastInPara) {
            openLine();
        }
    }

    // -----------------------------------------------------------------
    // WordLayouter/WordSplitter 호출 시 필요한 콜백
    // -----------------------------------------------------------------

    boolean isAllLineDivideByWord() {
        return hangulByWord && englishByWord;
    }
}
