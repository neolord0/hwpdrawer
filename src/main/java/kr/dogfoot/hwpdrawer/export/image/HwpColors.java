package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.awt.Color;

/**
 * HWP 색 값(Color4Byte)을 칠할 색으로 바꾼다.
 *
 * <p>HWP는 <b>'색 없음'</b>(칠하지 않음 — 뒤가 비친다)을 0xFFFFFFFF로 저장한다. 문서 기본 테두리/배경
 * (무늬 채우기의 바탕색)과 '글자 음영 없음'이 모두 이 값이다. 흰색은 0x00FFFFFF로 따로 있다.
 * 이 값을 흰색으로 칠하면 표·글상자 뒤에 깔린 도형·그림을 흰 사각형으로 가린다.</p>
 */
final class HwpColors {
    private static final long NO_COLOR = 0xFFFFFFFFL;

    private HwpColors() {
    }

    static boolean isNoColor(Color4Byte color) {
        return (color.getValue() & 0xFFFFFFFFL) == NO_COLOR;
    }

    /** 칠할 색. 값이 없거나 '색 없음'이면 null — 칠하지 않는다. */
    static Color paintOf(Color4Byte color) {
        if (color == null || isNoColor(color)) return null;
        return new Color(color.getR(), color.getG(), color.getB());
    }
}
