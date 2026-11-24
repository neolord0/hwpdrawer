package kr.dogfoot.hwpdrawer.engine.para.nolineseg;

import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.engine.ParaShapeHelper;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.engine.para.ParaLayoutContext;
import kr.dogfoot.hwpdrawer.engine.para.common.LineAlignmentProcessor;
import kr.dogfoot.hwpdrawer.engine.para.common.MinimumSpaceProcessor;

import java.util.List;

/**
 * 라인 마감 시 min-space/alignment/page-overflow 처리.
 */
final class LineFinisher {
    private LineFinisher() {}

    /**
     * 세그먼트 전환 시 이전 TextPart를 마감한다(= "다중 세그먼트 라인"에서 이전 파트 처리).
     * min-space 축소 + 정렬(Left가 아닐 때) 적용 후 파트 width를 확정.
     */
    static void freezeSegment(LineWriter line, ParaShape paraShape,
                               long segLeft, long segRight, boolean lastInPara) {
        TextPart part = line.textPart();
        double segmentWidth = segRight - segLeft;
        MinimumSpaceProcessor.apply(part, paraShape, segmentWidth);
        double visibleCharX = computeVisibleCharX(part);

        double stretchShift = LineAlignmentProcessor.stretchJustify(
                part, line.textLine().alignment(), lastInPara, segmentWidth, visibleCharX);
        visibleCharX += stretchShift;

        LineAlignmentProcessor.applyAlignmentOffset(
                part, line.textLine().alignment(), segmentWidth, visibleCharX, segLeft);

        part.width(lastInPara ? visibleCharX : segmentWidth);
    }

    static void finish(LineWriter line, ParaShape paraShape,
                       double rightMargin, ParaLayoutContext ctx, boolean lastInPara) {
        TextLine tl = line.textLine();
        TextPart part = line.textPart();
        // 마지막 세그먼트 기준 폭/좌측 — 다중 세그먼트 라인에서는 전체 라인이 아니라 마지막 파트 기준.
        double segmentWidth = line.segmentWidth();
        double segmentLeft = part.startX();

        double charX = line.wordsWidth() + line.spacesWidth();

        double shrinkShift = MinimumSpaceProcessor.apply(part, paraShape, segmentWidth);
        double adjustedCharX = charX - shrinkShift;

        double visibleCharX = computeVisibleCharX(part);

        double stretchShift = LineAlignmentProcessor.stretchJustify(
                part, tl.alignment(), lastInPara, segmentWidth, visibleCharX);
        adjustedCharX += stretchShift;
        visibleCharX += stretchShift;

        LineAlignmentProcessor.applyAlignmentOffset(
                part, tl.alignment(), segmentWidth, visibleCharX, segmentLeft);

        writeLineMetadata(tl, part, line, adjustedCharX, visibleCharX, lastInPara);

        long lineHeight = computeLineHeight(paraShape, line, part);
        recordLineGap(tl, lineHeight, line, part);

        tl.area().right(Math.round(rightMargin));
        tl.area().bottom(tl.area().top() + lineHeight);
        ctx.currentY(ctx.currentY() + lineHeight);

        moveLineToNextPageIfOverflow(tl, ctx, line.maxHeight());
    }

    private static double computeVisibleCharX(TextPart part) {
        List<CharInfo> chars = part.charInfos();
        for (int i = chars.size() - 1; i >= 0; i--) {
            CharInfo ci = chars.get(i);
            if (!ci.isSpace() && !ci.inlineControl()) return ci.x() + ci.width();
        }
        return 0;
    }

    private static void writeLineMetadata(TextLine tl, TextPart part, LineWriter line,
                                          double adjustedCharX, double visibleCharX, boolean lastInPara) {
        part.width(lastInPara ? visibleCharX : line.segmentWidth());
        tl.maxCharHeight(Math.round(line.maxHeight()));
        tl.maxCharDescent(Math.round(line.maxDescent()));
        tl.maxCharBaseSize(computeBaseDistance(line));
        tl.lastInPara(lastInPara);
    }

    /**
     * HWP 컨벤션: baseDistance(= DistanceBaseLineToLineVerticalPosition)
     * = textPartHeight × textFontAscentRatio.
     *
     * <p>텍스트 문자의 ascent/height 비율을 라인의 maxHeight(인라인 컨트롤 포함)에 적용해,
     * 거대한 인라인 컨트롤도 "큰 글자"처럼 동일한 ascent 비율로 베이스라인을 계산한다.
     * 텍스트가 없는 라인은 한글 폰트 기본 비율 0.85로 fallback.
     */
    private static long computeBaseDistance(LineWriter line) {
        double ratio;
        if (line.maxTextHeight() > 0 && line.maxBaseSize() > 0) {
            ratio = line.maxBaseSize() / line.maxTextHeight();
        } else {
            ratio = 0.85;
        }
        return Math.round(line.maxHeight() * ratio);
    }

    private static long computeLineHeight(ParaShape paraShape, LineWriter line, TextPart part) {
        boolean hasDrawingChar = !part.charInfos().isEmpty();
        boolean hasTextChar = line.maxTextHeight() > 0;

        // 텍스트 기준 lineHeight — gap 공식에 인라인 컨트롤 높이를 넣지 않기 위함.
        // 텍스트가 없는 라인(인라인 컨트롤만)은 maxBaseSize로 fallback.
        long gapBase = hasTextChar ? Math.round(line.maxTextHeight()) : Math.round(line.maxBaseSize());
        long textLineHeight = ParaShapeHelper.lineHeight(
                paraShape,
                gapBase,
                hasTextChar ? Math.round(line.maxTextDescent()) : 0,
                hasDrawingChar,
                Math.round(line.maxBaseSize()));

        // 텍스트 기준 ascent/gap 분리 — 인라인 컨트롤을 수용하기 위해 ascent만 확장.
        long textAscent = hasTextChar
                ? Math.round(line.maxTextHeight() - line.maxTextDescent())
                : Math.round(line.maxBaseSize());
        long gap = textLineHeight - textAscent;

        // 실제 ascent = 각 char별 ascent의 최대값. 인라인 컨트롤은 "큰 글자"로 취급되어
        // 베이스라인을 아래로 내림 — char.height가 그대로 ascent가 된다(descent=0).
        long ascent = Math.round(line.maxAscent());
        if (ascent == 0) ascent = textAscent;

        return ascent + gap;
    }

    private static void recordLineGap(TextLine tl, long lineHeight, LineWriter line, TextPart part) {
        boolean hasDrawingChar = !part.charInfos().isEmpty();
        double ascent = hasDrawingChar
                ? (line.maxHeight() - line.maxDescent())
                : line.maxBaseSize();
        long gap = Math.round(lineHeight - (ascent + line.maxDescent()));
        tl.lineGap(Math.max(0, gap));
    }

    /**
     * 라인이 bodyArea 하단을 넘으면 다음 단/페이지로 옮긴다.
     * 판정 기준: 라인 top + maxEm(= 글자 전체 높이) ≤ bodyArea.bottom.
     *
     * <p>다단(신문식)에서는 다음 단으로 이동하며, 이때 단의 X 위치가 바뀌므로
     * 라인 전체를 X·Y 만큼 평행 이동한다. 마지막 단이면 새 페이지로 이동한다.</p>
     */
    private static void moveLineToNextPageIfOverflow(TextLine line, ParaLayoutContext ctx,
                                                     double maxEm) {
        long visibleBottom = Math.round(line.area().top() + maxEm);
        if (visibleBottom <= ctx.bodyArea().bottom() || line.parts().isEmpty()) return;

        TextColumn currentCol = ctx.currentColumn();
        currentCol.textLines().remove(line);

        long oldLeft = ctx.bodyArea().left();
        long oldTop = line.area().top();
        if (!ctx.tryNextColumn()) {
            ctx.createNewPage();
        }
        ctx.ensureRowAndColumn();
        TextColumn newCol = ctx.currentColumn();
        newCol.textLines().add(line);

        shiftLine(line, ctx.bodyArea().left() - oldLeft, ctx.bodyArea().top() - oldTop);
        ctx.currentY(line.area().bottom());
    }

    /** 라인 전체(area + 각 TextPart)를 dx·dy 만큼 평행 이동한다. */
    private static void shiftLine(TextLine line, long dx, long dy) {
        line.area().left(line.area().left() + dx);
        line.area().right(line.area().right() + dx);
        line.area().top(line.area().top() + dy);
        line.area().bottom(line.area().bottom() + dy);
        for (TextPart p : line.parts()) {
            p.startX(p.startX() + dx);
            p.segmentLeft(p.segmentLeft() + dx);
            p.segmentRight(p.segmentRight() + dx);
        }
    }
}
