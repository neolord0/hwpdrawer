package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.NumberShape;
import kr.dogfoot.hwplib.object.docinfo.numbering.ParagraphNumberFormat;

/**
 * 각주/미주 번호 하나를 표기 문자열로 바꾼다.
 *
 * <p>번호 종류 enum이 문단번호와 <b>다르다</b> — 각주는
 * {@link NumberShape}(bodytext.control.sectiondefine), 문단번호는
 * {@link ParagraphNumberFormat}(docinfo.numbering)을 쓴다. 이름도 어긋난다
 * ({@code HangulSibgan} vs {@code SibGanHangul}), 각주에만 있는 종류도 있다
 * ({@code Symbol}, {@code UserChar}). 그래서 {@link ParagraphNumberFormatter}를
 * 직접 호출하지 못하고 여기서 매핑한 뒤 위임한다.</p>
 *
 * <p>{@code ParagraphNumberFormatter} 자체는 문단번호/개요 테스트가 직접 검증하므로 건드리지 않는다.</p>
 */
public final class FootnoteNumberFormatter {
    private FootnoteNumberFormatter() {}

    /**
     * 각주 번호 표기. {@code before}/{@code after}는 파일의 장식 문자다.
     *
     * <p>⚠ 장식 문자가 "없음"일 때 파일에는 빈 문자열이 아니라 <b>U+0000 한 글자</b>가 들어 있다
     * (실문서 R21 실측: before = {@code 00 00}, after = {@code 29 00} = ')').
     * 그대로 이어 붙이면 NUL이 문자열에 섞여 렌더에서 빈 칸으로 나타나므로 걸러낸다.</p>
     */
    public static String format(long number, NumberShape shape, String before, String after) {
        return strip(before) + ParagraphNumberFormatter.format(number, toParagraphFormat(shape)) + strip(after);
    }

    /** 제어문자(U+0000 등 장식 문자 "없음" 센티넬)를 제거한다. */
    private static String strip(String s) {
        if (s == null || s.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= ' ') sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 각주 번호 종류를 문단번호 종류로 매핑한다.
     *
     * <p>{@code Symbol}/{@code UserChar}는 대응이 없다 — 파일의 사용자 기호를 그대로 써야 하는데
     * 코퍼스에 표본이 없어 미구현이다. 아라비아 숫자로 폴백한다.</p>
     */
    private static ParagraphNumberFormat toParagraphFormat(NumberShape shape) {
        if (shape == null) return ParagraphNumberFormat.Number;
        switch (shape) {
            case Number:                    return ParagraphNumberFormat.Number;
            case CircledNumber:             return ParagraphNumberFormat.CircledNumber;
            case UppercaseRomanNumber:      return ParagraphNumberFormat.UppercaseRomanNumber;
            case LowercaseRomanNumber:      return ParagraphNumberFormat.LowercaseRomanNumber;
            case UppercaseAlphabet:         return ParagraphNumberFormat.UppercaseAlphabet;
            case LowercaseAlphabet:         return ParagraphNumberFormat.LowercaseAlphabet;
            case CircledUppercaseAlphabet:  return ParagraphNumberFormat.CircledUppercaseAlphabet;
            case CircledLowercaseAlphabet:  return ParagraphNumberFormat.CircledLowercaseAlphabet;
            case Hangul:                    return ParagraphNumberFormat.Hangul;
            case CircledHangul:             return ParagraphNumberFormat.CircledHangul;
            case HangulJamo:                return ParagraphNumberFormat.HangulJamo;
            case CircledHangulJamo:         return ParagraphNumberFormat.CircledHangulJamo;
            case HangulNumber:              return ParagraphNumberFormat.HangulNumber;
            case HanjaNumber:               return ParagraphNumberFormat.HanjaNumber;
            case CircledHanjaNumber:        return ParagraphNumberFormat.CircledHanjaNumber;
            case HangulSibgan:              return ParagraphNumberFormat.SibGanHangul;
            case HanjaSibgan:               return ParagraphNumberFormat.SibGanHanja;
            default:                        return ParagraphNumberFormat.Number;
        }
    }
}
