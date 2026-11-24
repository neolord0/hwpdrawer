package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;
import kr.dogfoot.hwpdrawer.util.FontManager;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.List;

/**
 * 한 라인에 가로정렬(Left/Right/Center/Justify)을 적용한다.
 * 모든 위치/폭은 double (HWP 단위).
 */
public final class LineAlignmentProcessor {
    private LineAlignmentProcessor() {}

    public static double stretchJustify(TextPart part, TextLine.Alignment alignment,
                                        boolean isLastLineInPara, double segmentWidth,
                                        double visibleCharX) {
        boolean isJustify = (alignment == TextLine.Alignment.Justify);
        boolean isDistribute = (alignment == TextLine.Alignment.Distribute);
        boolean isDivide = (alignment == TextLine.Alignment.Divide);
        if (!isJustify && !isDistribute && !isDivide) return 0;
        if (isJustify && isLastLineInPara) {
            // 양쪽정렬 문단의 마지막 라인은 왼쪽정렬로 처리하되,
            // bold 공백은 실제 폰트 advance 기준으로 폭을 보정한다.
            return widenBoldSpacesForLastLine(part);
        }

        List<CharInfo> chars = part.charInfos();
        if (chars.isEmpty()) return 0;

        // 탭이 포함된 줄은 탭 스톱이 글자 위치를 고정하므로 양쪽정렬 스트레치를 하지 않는다.
        // (한글도 탭 줄은 늘리지 않음 — 늘리면 다음 줄의 같은 탭 컬럼과 정렬이 깨진다.)
        if (containsTab(chars)) return 0;

        double extraWidth = segmentWidth - visibleCharX;
        // Justify는 음수 extra(자연 폭 > 세그먼트 폭)도 처리해서 공백을 압축한다.
        // Distribute/Divide는 늘리기 전용.
        if (extraWidth == 0) return 0;
        if (!isJustify && extraWidth < 0) return 0;

        int lastNonSpaceIdx = CharInfoListOps.lastNonSpaceIndex(chars);
        if (lastNonSpaceIdx <= 0) return 0;
        // 한글은 줄 선두 공백(들여쓰기용)은 늘리지 않는다 — Distribute 의 글자 간격 분배에서도
        // 마찬가지다. 첫 비공백 글자 앞의 공백은 폭·위치 모두 자연값 그대로 둔다.
        int firstNonSpaceIdx = CharInfoListOps.firstNonSpaceIndex(chars);

        if (isDistribute) {
            return distributeToInterCharGaps(chars, firstNonSpaceIdx, lastNonSpaceIdx, extraWidth);
        }

        int spaceCount = CharInfoListOps.countSpacesBetween(chars, firstNonSpaceIdx, lastNonSpaceIdx);
        if (isDivide) {
            return (spaceCount > 0)
                    ? distributeToSpaces(chars, firstNonSpaceIdx, lastNonSpaceIdx, spaceCount, extraWidth)
                    : 0;
        }
        // Justify
        return (spaceCount > 0)
                ? distributeToSpaces(chars, firstNonSpaceIdx, lastNonSpaceIdx, spaceCount, extraWidth)
                : distributeToInterCharGaps(chars, firstNonSpaceIdx, lastNonSpaceIdx, extraWidth);
    }

    public static void applyAlignmentOffset(TextPart part, TextLine.Alignment alignment,
                                             double segmentWidth, double visibleCharX, double lineLeft) {
        double extra = segmentWidth - visibleCharX;
        if (extra <= 0) return;

        double offset = offsetForAlignment(alignment, extra);
        if (offset != 0) {
            part.startX(lineLeft + offset);
        }
    }

    private static double offsetForAlignment(TextLine.Alignment alignment, double extra) {
        switch (alignment) {
            case Right:  return extra;
            case Center: return extra / 2.0;
            default:     return 0;
        }
    }

    /** (firstNonSpaceIdx..lastNonSpaceIdx] 범위의 공백에만 여분 폭을 분배한다 (선두 공백 제외). */
    private static double distributeToSpaces(List<CharInfo> chars, int firstNonSpaceIdx,
                                              int lastNonSpaceIdx, int spaceCount, double extraWidth) {
        double perSpace = extraWidth / spaceCount;

        double cumulativeShift = 0;
        for (int i = 0; i < chars.size(); i++) {
            CharInfo ci = chars.get(i);
            ci.x(ci.x() + cumulativeShift);
            if (ci.isSpace() && i > firstNonSpaceIdx && i <= lastNonSpaceIdx) {
                ci.width(ci.width() + perSpace);
                cumulativeShift += perSpace;
            }
        }
        return cumulativeShift;
    }

    /**
     * 양쪽정렬 마지막 라인의 bold 공백 폭을 실제 폰트 기준으로 보정한다.
     * 측정 시 공백은 baseSize/2로 계산되지만, bold의 자연 advance는 더 넓으므로
     * 왼쪽정렬처럼 렌더링될 때 한컴 출력과 맞추기 위해 확장한다.
     */
    private static double widenBoldSpacesForLastLine(TextPart part) {
        List<CharInfo> chars = part.charInfos();
        if (chars.isEmpty()) return 0;

        FontRenderContext frc = new FontRenderContext(null, true, true);
        double cumulativeShift = 0;
        for (CharInfo ci : chars) {
            ci.x(ci.x() + cumulativeShift);
            if (!ci.isSpace() || ci.ch() != ' ') continue;
            if (ci.charShape() == null || !ci.charShape().getProperty().isBold()) continue;

            Font f = FontManager.instance().calculatingFont(ci.charShape());
            double boldSpaceAdvance = f.getStringBounds(" ", frc).getWidth();
            double extra = boldSpaceAdvance - ci.width();
            if (extra > 0) {
                ci.width(ci.width() + extra);
                cumulativeShift += extra;
            }
        }
        return cumulativeShift;
    }

    /** 줄에 탭 글자(ch=='\t')가 하나라도 있는지. */
    private static boolean containsTab(List<CharInfo> chars) {
        for (CharInfo ci : chars) {
            if (ci.ch() == '\t') return true;
        }
        return false;
    }

    /**
     * 여분 폭을 글자 사이 간격에 균등 분배한다(배분정렬, 공백 없는 양쪽정렬 줄).
     *
     * <p><b>선두 공백은 간격으로 세지 않는다.</b> 한글의 배분정렬은 여분 전체를
     * [첫 비공백, 마지막 비공백) 사이의 간격에만 나누고, 줄 머리의 들여쓰기용 공백은
     * 폭·위치 모두 자연값 그대로 둔다. 실문서 R01 라벨 셀에서 역산으로
     * 확정한 규칙이다 — "&nbsp;&nbsp;&nbsp;아동"(선두 공백 3)의 참조 렌더는 '아'가 자연
     * 위치(x=169)에 있고 여분은 아↔동 사이 1개 간격에만 들어간다. 선두 공백까지 세면
     * '아'가 13px 오른쪽으로 밀린다(수정 전 실측 x=182). 42개 글자·12개 줄 검산에서
     * 이 규칙의 잔차는 평균 +1.00px(알려진 toPixel 절삭·전 글자 공통), 표준편차 0.74px.
     * 후행 공백 제외(lastNonSpaceIdx)는 원래부터 참조와 일치한다.</p>
     */
    private static double distributeToInterCharGaps(List<CharInfo> chars, int firstNonSpaceIdx,
                                                     int lastNonSpaceIdx, double extraWidth) {
        int gapCount = lastNonSpaceIdx - firstNonSpaceIdx;
        if (gapCount <= 0) return 0;

        double perGap = extraWidth / gapCount;

        double cumulativeShift = 0;
        for (int i = 0; i < chars.size(); i++) {
            CharInfo ci = chars.get(i);
            ci.x(ci.x() + cumulativeShift);
            if (i >= firstNonSpaceIdx && i < lastNonSpaceIdx) {
                cumulativeShift += perGap;
            }
        }
        return cumulativeShift;
    }
}
