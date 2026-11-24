package kr.dogfoot.hwpdrawer.export.text;

import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * 문단 글자 정리: PUA 기호 변환, 공백 정리(여러 칸 → 한 칸, 앞뒤 공백 제거), 보이지 않는 글자 제거, NFC.
 * 문단 안 강제 줄나눔은 {@link #LINE_BREAK} 하나로 남긴다 (빈 줄은 없앤다).
 *
 * <p>글자가 아니라 선으로 쓰인 것은 뺀다: 같은 선 글자 4개 이상("_____", "════")은 공백 하나로, 선·자리표시
 * 글자만 있는 문단("════", 빈 칸의 "◦"·"❍", 날짜 자리 ". .")은 빈 문단으로. 단, "-" 하나는 표에서 "없음"이라는
 * 뜻이라 남긴다.</p>
 */
final class PlainText {
    static final char LINE_BREAK = '\n';

    /** 같은 글자 4개 이상: 밑줄·괘선·점선으로 그은 선. */
    private static final Pattern RULE_RUN = Pattern.compile("([_=.·‧∙─━═-])\\1{3,}");
    /**
     * 이 글자들만 있는 문단은 뜻이 없다 (빈 칸 자리표시 글머리, 날짜 자리 점, 칸 안내 "/" "- /"). 괘선(U+2500~U+257F)도 같다.
     * "○"는 표에서 '예/맞음'이라는 뜻으로 쓰이고, "·"는 세로 라벨의 구분자(화장장/·/납골당)라 넣지 않는다.
     */
    private static final String FILLER_CHARACTERS = "_-=./◦∘❍";
    private static final String NOTHING = "-";

    private PlainText() {
    }

    static String normalize(CharSequence raw) {
        String normalized = collapseSpaces(RULE_RUN.matcher(PrivateUseSymbols.replaceIn(raw)).replaceAll(" "));
        return isFiller(normalized) ? "" : normalized;
    }

    private static String collapseSpaces(String text) {
        StringBuilder out = new StringBuilder(text.length());
        char pending = 0;   // 앞 글자와 다음 글자 사이에 넣을 구분 (공백 또는 줄바꿈)
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (isInvisible(ch)) continue;
            if (ch == LINE_BREAK) {
                if (out.length() > 0) pending = LINE_BREAK;
                continue;
            }
            if (isSpace(ch)) {
                if (out.length() > 0 && pending != LINE_BREAK) pending = ' ';
                continue;
            }
            if (pending != 0) out.append(pending);
            pending = 0;
            out.append(ch);
        }
        return Normalizer.normalize(out, Normalizer.Form.NFC);
    }

    private static boolean isFiller(String text) {
        if (text.isEmpty() || text.equals(NOTHING)) return false;
        return text.codePoints().allMatch(cp -> (cp <= Character.MAX_VALUE && isSpace((char) cp))
                || FILLER_CHARACTERS.indexOf(cp) >= 0 || (cp >= 0x2500 && cp <= 0x257F));
    }

    /** 칸 띄우기로만 쓰이는 글자. 문서에서는 줄 맞춤용으로 여러 칸씩 쓰인다. */
    private static boolean isSpace(char ch) {
        return ch == ' ' || ch == '\u00A0' || ch == '\u3000' || ch == '\u202F' || ch == '\u205F'
                || (ch >= '\u2000' && ch <= '\u200A') || ch < ' ';
    }

    /** 폭 없는 공백·BOM·숨은 하이픈. */
    private static boolean isInvisible(char ch) {
        return ch == '\u200B' || ch == '\u2060' || ch == '\uFEFF' || ch == '\u00AD';
    }
}
