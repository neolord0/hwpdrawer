package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextPart;
import kr.dogfoot.hwpdrawer.engine.charinfo.CharInfo;

/**
 * 한 줄의 CharInfo들을 글자로 바꾼다. 화면에서 자연스럽게 넘어간 줄은 그대로 잇고(줄 끝 공백은 글자로 남아 있다),
 * 강제 줄나눔으로 끝난 줄만 줄바꿈을 남긴다. 공백 정리·PUA 기호 변환은 문단 단위로 {@link PlainText}가 한다.
 */
final class LineText {
    private LineText() {
    }

    /**
     * @param hidden   바탕과 같은 색이라 보이지 않아 뺀 글자를 여기에 모은다
     * @param backdrop 글자 뒤의 바탕 ({@link Backdrop#hides})
     */
    static void appendTo(TextLine line, StringBuilder out, StringBuilder hidden, Backdrop backdrop) {
        for (TextPart part : line.parts()) {
            for (CharInfo ch : part.charInfos()) {
                if (isVisibleGlyph(ch) && backdrop.hides(ch, line, part)) {
                    append(ch, hidden);
                    continue;
                }
                append(ch, out);
                // 문단 머리("1.", "가)", "•")는 본문 들여쓰기 폭을 차지하는 CharInfo 하나라 뒤에 공백 글자가 없다.
                if (ch.paragraphHead()) out.append(' ');
            }
        }
        if (line.endsWithLineBreak()) out.append(PlainText.LINE_BREAK);
    }

    /** 보이는 글자(공백·탭·표 자리·제어 문자가 아닌 것)가 있는가. */
    static boolean hasVisibleText(TextLine line) {
        for (TextPart part : line.parts()) {
            for (CharInfo ch : part.charInfos()) {
                if (isVisibleGlyph(ch)) return true;
            }
        }
        return false;
    }

    /** 보이는(바탕에 숨지 않은) 글자가 있는가. */
    static boolean hasShownText(TextLine line, Backdrop backdrop) {
        for (TextPart part : line.parts()) {
            for (CharInfo ch : part.charInfos()) {
                if (isVisibleGlyph(ch) && !backdrop.hides(ch, line, part)) return true;
            }
        }
        return false;
    }

    /** 잉크로 그리는 글자인가 (공백·탭·표 자리·제어 문자가 아님). */
    static boolean isVisibleGlyph(CharInfo ch) {
        return !ch.inlineControl() && !ch.dotLeader() && !ch.isSpace() && ch.ch() > ' ';
    }

    private static void append(CharInfo ch, StringBuilder out) {
        if (ch.inlineControl()) return;               // 글자처럼 취급한 표·그림 자리. 표는 따로 쓴다.
        if (ch.dotLeader() || ch.ch() == '\t') {      // 탭(점선 채우기 포함)은 칸 띄우기일 뿐이다.
            out.append(' ');
            return;
        }
        String text = ch.text() != null ? ch.text() : String.valueOf(ch.ch());
        if (isControlCharacter(text)) return;
        out.append(text);
    }

    private static boolean isControlCharacter(String text) {
        return text.length() == 1 && text.charAt(0) < ' ';
    }
}
