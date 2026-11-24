package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;

import java.util.List;

/**
 * CharInfo 리스트에 대한 순수 조회 유틸리티 모음.
 *
 * <p>Clean Code: 여러 곳에서 반복되는 "trailing 공백 찾기", "공백 개수 세기" 등을
 * 이름 있는 메서드로 추출해서 의도를 드러낸다.</p>
 */
public final class CharInfoListOps {
    private CharInfoListOps() {}

    /** Trailing(끝쪽) 공백을 제외한 마지막 non-space 글자의 인덱스. 없으면 -1. */
    public static int lastNonSpaceIndex(List<CharInfo> chars) {
        for (int i = chars.size() - 1; i >= 0; i--) {
            if (!chars.get(i).isSpace()) return i;
        }
        return -1;
    }

    /** Leading(앞쪽) 공백을 지난 첫 non-space 글자의 인덱스. 없으면 -1. */
    public static int firstNonSpaceIndex(List<CharInfo> chars) {
        for (int i = 0; i < chars.size(); i++) {
            if (!chars.get(i).isSpace()) return i;
        }
        return -1;
    }

    /** [0..lastIdx] 범위의 공백 개수. */
    public static int countSpacesUpTo(List<CharInfo> chars, int lastIdx) {
        int count = 0;
        for (int i = 0; i <= lastIdx; i++) {
            if (chars.get(i).isSpace()) count++;
        }
        return count;
    }

    /** (firstIdx..lastIdx] 범위(첫 non-space 이후)의 공백 개수. */
    public static int countSpacesBetween(List<CharInfo> chars, int firstIdx, int lastIdx) {
        int count = 0;
        for (int i = firstIdx + 1; i <= lastIdx; i++) {
            if (chars.get(i).isSpace()) count++;
        }
        return count;
    }

    /** [0..lastIdx] 범위의 공백 width 합. */
    public static double sumSpacesUpTo(List<CharInfo> chars, int lastIdx) {
        double sum = 0;
        for (int i = 0; i <= lastIdx; i++) {
            if (chars.get(i).isSpace()) sum += chars.get(i).width();
        }
        return sum;
    }

    /** [0..lastIdx] 범위의 비-공백 width 합. */
    public static double sumNonSpacesUpTo(List<CharInfo> chars, int lastIdx) {
        double sum = 0;
        for (int i = 0; i <= lastIdx; i++) {
            if (!chars.get(i).isSpace()) sum += chars.get(i).width();
        }
        return sum;
    }
}
