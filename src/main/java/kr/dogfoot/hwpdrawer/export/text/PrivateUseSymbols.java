package kr.dogfoot.hwpdrawer.export.text;

import java.util.HashMap;
import java.util.Map;

/**
 * 사용자 영역(PUA) 글자를 표준 유니코드 글자로 바꾼다.
 *
 * <p>한글 문서는 한컴 글꼴(함초롬 등)이 PUA에 넣어 둔 기호(네모 번호 [1], 동그라미 글자 (인), 괘선 등)와
 * Word에서 붙여 온 Wingdings 글머리표(U+F0xx)를 그대로 쓴다. 이 글자는 해당 글꼴 밖에서는 빈 네모나 엉뚱한
 * 글자로 보이고 검색어와도 맞지 않는다. 그래서 대응을 아는 것은 표준 글자로 바꾸고, 모르는 것은 버린다.
 * 대응은 함초롬돋움의 PUA 글리프를 직접 그려 보고 정했다.</p>
 *
 * <p>두 자리 번호는 조각 두 글자로 들어온다 — 예: U+F0289(①) + U+F0297(5) = ⑮.</p>
 */
final class PrivateUseSymbols {
    /** 네모·동그라미 글자에 쓰인 64음절 (한컴 PUA의 같은 순서). */
    private static final String SYLLABLES =
            "감관귀글금남낮높단답대도동뒤끝말명목문미반범변보복부북비상서소속수신압약여역영예오원월유의음인일입전접조존주준증참토특표해형화활";
    private static final String CONSONANT_ROW_SYLLABLES = "가나다라마바사아자차카타파하";
    private static final String VOWELS = "ㅏㅑㅓㅕㅗㅛㅜㅠㅡㅣ";

    private static final Map<Integer, String> SYMBOLS = new HashMap<>();

    static {
        // Wingdings 글머리표 (Word에서 붙여 온 문서)
        put(0xF06C, "●");
        put(0xF06E, "■");
        put(0xF06F, "□");
        put(0xF071, "❑");
        put(0xF075, "◆");
        put(0xF076, "❖");
        put(0xF077, "⬥");
        put(0xF09F, "•");
        put(0xF0A7, "▪");
        put(0xF0D8, "➢");
        put(0xF0E8, "➔");
        put(0xF0FC, "✔");
        put(0xF0FE, "☑");
        put(0xF53A, "한");   // '한글'의 '한' 로고 글자

        // 화살표
        putEach(0xF0034, "←↑↓");
        putEach(0xF0039, "⬅⬆⬇");

        // 동그라미·네모 글자
        put(0xF0095, "㉾");  // 동그라미 '우' (우편번호)
        put(0xF0097, "(품)");
        put(0xF0098, "(전)");
        put(0xF0099, "(인)");
        putEnclosed(0xF009B, CONSONANT_ROW_SYLLABLES, "[", "]");
        putEnclosed(0xF00A9, VOWELS, "[", "]");
        putEnclosed(0xF00B3, SYLLABLES, "[", "]");
        putEnclosed(0xF00F3, VOWELS, "(", ")");
        putEnclosed(0xF00FD, SYLLABLES, "(", ")");
        putEnclosed(0xF0281, "日月火水木金土", "[", "]");

        // 도형
        putEach(0xF02EA, "□□◇◇◆◆△▲▲▽▼▼◁◀◀▷▶▶►◄▲▼");

        // 괘선
        putEach(0xF0800, "─│━┃│─┌┬┐├┼┤└┴┘─│┏┳┓┣╋┫┗┻┛━┃┄┆╔╦╗╠╬╣╚╩╝═║╔╦╗╠╬╣╚╩╝═║"
                + "╒╤╕╞╪╡╘╧╛═│╓╥╖╟╫╢╙╨╜─║");
        putEach(0xF0850, "‘’“”『』");
    }

    private PrivateUseSymbols() {
    }

    static boolean isPrivateUse(int codePoint) {
        return (codePoint >= 0xE000 && codePoint <= 0xF8FF)
                || (codePoint >= 0xF0000 && codePoint <= 0x10FFFD);
    }

    /** PUA 글자를 바꾼 문자열. PUA가 아닌 글자는 그대로 둔다. */
    static String replaceIn(CharSequence text) {
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            int codePoint = Character.codePointAt(text, i);
            i += Character.charCount(codePoint);
            if (!isPrivateUse(codePoint)) {
                out.appendCodePoint(codePoint);
                continue;
            }
            NumberPieces pieces = NumberPieces.withTens(codePoint);
            int next = i < text.length() ? Character.codePointAt(text, i) : -1;
            if (pieces != null && pieces.isUnits(next)) {
                out.append(pieces.format(pieces.digitOf(codePoint) * 10 + pieces.unitOf(next)));
                i += Character.charCount(next);
            } else if (pieces != null) {
                out.append(pieces.format(pieces.digitOf(codePoint)));
            } else {
                out.append(singleOf(codePoint));
            }
        }
        return out.toString();
    }

    private static String singleOf(int codePoint) {
        String symbol = SYMBOLS.get(codePoint);
        if (symbol != null) return symbol;
        NumberPieces pieces = NumberPieces.withUnits(codePoint);
        return pieces != null ? String.valueOf(pieces.unitOf(codePoint)) : "";
    }

    private static void put(int codePoint, String text) {
        SYMBOLS.put(codePoint, text);
    }

    /** from부터 차례로 symbols의 글자 하나씩. */
    private static void putEach(int from, String symbols) {
        int codePoint = from;
        for (int i = 0; i < symbols.length(); ) {
            int symbol = symbols.codePointAt(i);
            i += Character.charCount(symbol);
            put(codePoint++, new String(Character.toChars(symbol)));
        }
    }

    /** from부터 차례로 letters의 글자 하나씩을 open·close로 감싸서. */
    private static void putEnclosed(int from, String letters, String open, String close) {
        for (int i = 0; i < letters.length(); i++) {
            put(from + i, open + letters.charAt(i) + close);
        }
    }

    /**
     * 번호 글자 한 벌. 십의 자리 조각(한 자리 번호로도 쓰인다) 다음에 일의 자리 조각이 오면 두 자리 번호다.
     */
    private enum NumberPieces {
        CIRCLED(0xF0288, 0, 0xF0292) {
            @Override
            String format(int number) {
                if (number == 0) return "⓪";
                if (number <= 20) return codePoint(0x2460 + number - 1);
                if (number <= 35) return codePoint(0x3251 + number - 21);
                if (number <= 50) return codePoint(0x32B1 + number - 36);
                return "(" + number + ")";
            }
        },
        NEGATIVE_CIRCLED(0xF029C, 0, 0xF02A6) {
            @Override
            String format(int number) {
                if (number == 0) return "⓿";
                if (number <= 10) return codePoint(0x2776 + number - 1);
                if (number <= 20) return codePoint(0x24EB + number - 11);
                return "(" + number + ")";
            }
        },
        SQUARED(0xF02B0, 0, 0xF02C3),
        SQUARED_TENS(0xF02BA, 1, 0xF02C3),
        NEGATIVE_SQUARED(0xF02CD, 0, 0xF02E0),
        NEGATIVE_SQUARED_TENS(0xF02D7, 1, 0xF02E0);

        private final int tensFrom;
        private final int firstDigit;
        private final int unitsFrom;

        NumberPieces(int tensFrom, int firstDigit, int unitsFrom) {
            this.tensFrom = tensFrom;
            this.firstDigit = firstDigit;
            this.unitsFrom = unitsFrom;
        }

        static NumberPieces withTens(int codePoint) {
            for (NumberPieces pieces : values()) {
                if (pieces.isTens(codePoint)) return pieces;
            }
            return null;
        }

        static NumberPieces withUnits(int codePoint) {
            for (NumberPieces pieces : values()) {
                if (pieces.isUnits(codePoint)) return pieces;
            }
            return null;
        }

        String format(int number) {
            return "[" + number + "]";
        }

        int digitOf(int tensPiece) {
            return firstDigit + tensPiece - tensFrom;
        }

        int unitOf(int unitsPiece) {
            return unitsPiece - unitsFrom;
        }

        boolean isUnits(int codePoint) {
            return codePoint >= unitsFrom && codePoint < unitsFrom + 10;
        }

        private boolean isTens(int codePoint) {
            return codePoint >= tensFrom && codePoint < tensFrom + 10 - firstDigit;
        }

        private static String codePoint(int codePoint) {
            return new String(Character.toChars(codePoint));
        }
    }
}
