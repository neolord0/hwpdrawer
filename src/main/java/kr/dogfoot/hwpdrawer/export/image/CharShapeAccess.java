package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

/**
 * CharShape에서 글자 종류(한글/영문)에 따라 다른 값을 반환하는 접근자 모음.
 *
 * <p>Clean Code: 각 CharShape getter에 있는 반복적인 "isHangul" 분기를 한 곳에 모아 중복 제거.</p>
 */
final class CharShapeAccess {
    private CharShapeAccess() {}

    static short relativeSize(CharShape cs, char ch) {
        return HWPFileReader.isHangul(ch)
                ? cs.getRelativeSizes().getHangul()
                : cs.getRelativeSizes().getLatin();
    }

    static short ratio(CharShape cs, char ch) {
        return HWPFileReader.isHangul(ch)
                ? cs.getRatios().getHangul()
                : cs.getRatios().getLatin();
    }

    /** 글자 수직 오프셋 — baseSize의 백분율로 저장되어 있음. */
    static long charOffsetY(CharShape cs, char ch) {
        byte offsetPercent = HWPFileReader.isHangul(ch)
                ? cs.getCharOffsets().getHangul()
                : cs.getCharOffsets().getLatin();
        return (long) (cs.getBaseSize() * offsetPercent / 100.0);
    }
}
