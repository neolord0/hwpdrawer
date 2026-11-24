package kr.dogfoot.hwpdrawer.engine;

import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.parashape.LineSpaceSort;
import kr.dogfoot.hwplib.object.docinfo.parashape.ParaShapeProperty1;
import kr.dogfoot.hwpdrawer.document.text.TextLine;

/**
 * ParaShape에서 레이아웃에 필요한 속성을 추출하는 정적 유틸.
 */
public final class ParaShapeHelper {
    private ParaShapeHelper() {}

    // ParaShape의 margin/space/indent 필드는 HWPSUBUNIT(= 2 × HWPUNIT)으로 저장된다.
    // 좌표계가 HWPUNIT인 bodyArea/LineSeg와 맞추기 위해 모두 2로 나눈다.
    // (old_version의 ParaDrawer*에서도 동일하게 / 2 처리한다.)
    public static long leftMargin(ParaShape ps) { return ps.getLeftMargin() / 2; }
    public static long rightMargin(ParaShape ps) { return ps.getRightMargin() / 2; }
    public static long indent(ParaShape ps) { return ps.getIndent() / 2; }
    public static long topSpace(ParaShape ps) { return ps.getTopParaSpace() / 2; }
    public static long bottomSpace(ParaShape ps) { return ps.getBottomParaSpace() / 2; }

    /**
     * 한 라인의 실제 높이를 계산한다 (HWP 단위).
     *
     * HWP legacy lineSpace는 %-based (100=100%). 라인의 자연 글자 높이에 이 비율을 곱한다.
     * (lineSpace2는 타입 필드가 함께 필요하여 해석이 복잡하므로 현재는 lineSpace만 사용.)
     *
     * @param naturalHeight 해당 라인의 자연 글자 높이 (최대 글자의 높이, HWP 단위)
     */
    /**
     * 한 라인의 실제 라인 높이(line height)를 계산한다 (HWP 단위).
     *
     * old_version의 TextLineDrawer.getLineGap + setLineHeight 공식을 그대로 옮긴 것:
     *
     *   lineHeight = (maxCharHeight - maxCharDescent) + lineGap
     *              = ascent + lineGap
     *
     * lineGap은 lineSpaceSort에 따라 다르게 계산:
     * - RatioForLetter: 글자에 비례 (lineSpace == lineSpace2이면 maxBaseSize2 × (% - 100) / 100)
     * - FixedValue: lineSpace / 2 - maxBaseSize2
     * - OnlyMargin: lineSpace / 2
     *
     * @param paraShape       문단 서식
     * @param maxCharHeight   라인 내 max charInfo.height (= effective height)
     * @param maxCharDescent  라인 내 max charInfo.descent
     * @param hasDrawingChar  라인에 그릴 글자가 있는지 여부
     * @param defaultBaseSize hasDrawingChar=false일 때 사용할 기본 baseSize
     */
    public static long lineHeight(ParaShape paraShape,
                                   long maxCharHeight, long maxCharDescent,
                                   boolean hasDrawingChar, long defaultBaseSize) {
        long ascent = hasDrawingChar
                ? (maxCharHeight - maxCharDescent)
                : defaultBaseSize;
        long lineGap = lineGap(paraShape, maxCharHeight, hasDrawingChar, defaultBaseSize);
        return ascent + lineGap;
    }


    private static long lineGap(ParaShape paraShape, long maxCharHeight,
                                 boolean hasDrawingChar, long defaultBaseSize) {
        long maxBaseSize2 = hasDrawingChar ? maxCharHeight : defaultBaseSize;
        LineSpaceSort sort = paraShape.getProperty1().getLineSpaceSort();
        if (sort == null) sort = LineSpaceSort.RatioForLetter;
        long lineSpace = paraShape.getLineSpace();
        long lineSpace2 = paraShape.getLineSpace2();

        switch (sort) {
            case RatioForLetter:
                if (lineSpace == lineSpace2) {
                    return maxBaseSize2 * lineSpace / 100 - maxBaseSize2;
                } else {
                    return Math.max(maxBaseSize2, lineSpace2 / 2) - maxBaseSize2;
                }
            case FixedValue:
                return lineSpace / 2 - maxBaseSize2;
            case OnlyMargin:
                return lineSpace / 2;
            default:
                return 0;
        }
    }

    public static TextLine.Alignment alignment(ParaShape ps) {
        ParaShapeProperty1 prop = ps.getProperty1();
        switch (prop.getAlignment()) {
            case Justify: return TextLine.Alignment.Justify;
            case Left: return TextLine.Alignment.Left;
            case Right: return TextLine.Alignment.Right;
            case Center: return TextLine.Alignment.Center;
            case Distribute: return TextLine.Alignment.Distribute;
            case Divide: return TextLine.Alignment.Divide;
            default: return TextLine.Alignment.Left;
        }
    }
}
