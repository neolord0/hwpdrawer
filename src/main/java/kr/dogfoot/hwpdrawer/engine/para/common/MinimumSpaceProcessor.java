package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;

import java.util.List;

/**
 * 최소공백(minimumSpace) 축소 처리. 모든 폭 계산은 double로 수행.
 */
public final class MinimumSpaceProcessor {
    private MinimumSpaceProcessor() {}

    public static double apply(TextPart part, ParaShape paraShape, double segmentWidth) {
        int minSpaceProp = paraShape.getProperty1().getMinimumSpace();
        if (minSpaceProp <= 0) return 0;

        List<CharInfo> chars = part.charInfos();
        if (chars.isEmpty()) return 0;

        int lastNonSpaceIdx = CharInfoListOps.lastNonSpaceIndex(chars);
        if (lastNonSpaceIdx <= 0) return 0;

        double wordsWidth = CharInfoListOps.sumNonSpacesUpTo(chars, lastNonSpaceIdx);
        double spacesWidth = CharInfoListOps.sumSpacesUpTo(chars, lastNonSpaceIdx);
        if (spacesWidth == 0 || wordsWidth + spacesWidth <= segmentWidth) return 0;

        double rate = (segmentWidth - wordsWidth) / spacesWidth;
        double minRate = (100.0 - minSpaceProp) / 100.0;
        if (rate < minRate) rate = minRate;
        if (rate >= 1.0) return 0;

        return shrinkSpaces(chars, lastNonSpaceIdx, rate);
    }

    private static double shrinkSpaces(List<CharInfo> chars, int lastNonSpaceIdx, double rate) {
        double shift = 0;
        for (int i = 0; i < chars.size(); i++) {
            CharInfo ci = chars.get(i);
            ci.x(ci.x() - shift);
            if (ci.isSpace() && i <= lastNonSpaceIdx) {
                double newWidth = ci.width() * rate;
                shift += ci.width() - newWidth;
                ci.width(newWidth);
            }
        }
        return shift;
    }
}
