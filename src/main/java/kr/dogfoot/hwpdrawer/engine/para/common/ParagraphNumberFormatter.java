package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwplib.object.docinfo.numbering.ParagraphNumberFormat;

/**
 * 문단 번호 하나(n번째)를 번호 종류에 맞는 글자로 바꾼다.
 *
 * <p>한글 출력과 대조해 확인한 종류: {@link ParagraphNumberFormat#Number 1·2·3},
 * {@link ParagraphNumberFormat#Hangul 가·나·다}, {@link ParagraphNumberFormat#LowercaseAlphabet a·b·c},
 * {@link ParagraphNumberFormat#CircledNumber ①·②}, {@link ParagraphNumberFormat#CircledHangul ㉮·㉯},
 * {@link ParagraphNumberFormat#CircledLowercaseAlphabet ⓐ·ⓑ} (문단번호/개요 테스트).
 * 나머지 종류는 한글 문서 서식의 통상 정의를 따랐을 뿐 <b>참조 출력으로 검증하지 않았다</b>.</p>
 *
 * <p>번호가 각 계열의 글자 수를 넘으면 순환한다 — 한글이 그 경우 무엇을 그리는지 확인할
 * 표본이 없어, 빈 문자열이나 깨진 글자를 내는 대신 순환을 택했다.</p>
 */
public final class ParagraphNumberFormatter {
    private ParagraphNumberFormatter() {}

    private static final String HANGUL = "가나다라마바사아자차카타파하";
    private static final String HANGUL_JAMO = "ㄱㄴㄷㄹㅁㅂㅅㅇㅈㅊㅋㅌㅍㅎ";
    private static final String SIBGAN_HANGUL = "갑을병정무기경신임계";
    private static final String SIBGAN_HANJA = "甲乙丙丁戊己庚辛壬癸";
    private static final String HANJA_DIGITS = "一二三四五六七八九十";
    private static final String HANGUL_DIGITS = "일이삼사오육칠팔구십";

    private static final String[] ROMAN_NUMERALS = {
            "M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
    private static final int[] ROMAN_VALUES = {
            1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};

    /**
     * n번째(1부터) 번호 문자열. {@code format}이 null이면 아라비아 숫자로 취급한다.
     */
    public static String format(long n, ParagraphNumberFormat format) {
        if (n < 1) n = 1;
        if (format == null) return String.valueOf(n);

        switch (format) {
            case Number:                    return String.valueOf(n);
            case CircledNumber:             return circled(n, 0x2460, 20);
            case UppercaseAlphabet:         return alphabet(n, 'A');
            case LowercaseAlphabet:         return alphabet(n, 'a');
            case CircledUppercaseAlphabet:  return circled(n, 0x24B6, 26);
            case CircledLowercaseAlphabet:  return circled(n, 0x24D0, 26);
            case Hangul:                    return cyclic(HANGUL, n);
            case CircledHangul:             return circled(n, 0x326E, 14);
            case HangulJamo:                return cyclic(HANGUL_JAMO, n);
            case CircledHangulJamo:         return circled(n, 0x3260, 14);
            case HangulNumber:              return cyclic(HANGUL_DIGITS, n);
            case HanjaNumber:               return cyclic(HANJA_DIGITS, n);
            case CircledHanjaNumber:        return circled(n, 0x3280, 10);
            case SibGanHangul:              return cyclic(SIBGAN_HANGUL, n);
            case SibGanHanja:               return cyclic(SIBGAN_HANJA, n);
            case UppercaseRomanNumber:      return roman(n);
            case LowercaseRomanNumber:      return roman(n).toLowerCase();
            default:                        return String.valueOf(n);
        }
    }

    /** 연속한 코드포인트로 배열된 원문자 계열(①②…, ⓐⓑ…, ㉮㉯…). */
    private static String circled(long n, int firstCodePoint, int count) {
        int index = (int) ((n - 1) % count);
        return new String(Character.toChars(firstCodePoint + index));
    }

    /** a·b·c… z 다음은 aa·bb… (한글도 같은 글자를 반복해 늘린다). */
    private static String alphabet(long n, char first) {
        int index = (int) ((n - 1) % 26);
        int repeat = (int) ((n - 1) / 26) + 1;
        StringBuilder sb = new StringBuilder(repeat);
        for (int i = 0; i < repeat; i++) sb.append((char) (first + index));
        return sb.toString();
    }

    private static String cyclic(String glyphs, long n) {
        return String.valueOf(glyphs.charAt((int) ((n - 1) % glyphs.length())));
    }

    private static String roman(long n) {
        StringBuilder sb = new StringBuilder();
        long rest = n;
        for (int i = 0; i < ROMAN_VALUES.length && rest > 0; i++) {
            while (rest >= ROMAN_VALUES[i]) {
                sb.append(ROMAN_NUMERALS[i]);
                rest -= ROMAN_VALUES[i];
            }
        }
        return sb.length() == 0 ? String.valueOf(n) : sb.toString();
    }
}
