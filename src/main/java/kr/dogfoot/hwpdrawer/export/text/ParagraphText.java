package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.text.TextLine;

import java.util.List;

/**
 * 한 단 안에 이어진 같은 문단의 줄들 (문단 하나, 또는 쪽·단 경계에서 끊긴 문단 조각 하나).
 * 화면의 줄바꿈은 없애고 한 줄로 쓴다.
 */
final class ParagraphText implements TextBlock {
    private final int paraIndex;
    private final boolean startsParagraph;
    private boolean endsParagraph;
    private final long top;
    private final StringBuilder raw = new StringBuilder();
    /** 바탕과 같은 색이라 뺀 글자 ({@link #droppedText()}). */
    private final StringBuilder hidden = new StringBuilder();

    private ParagraphText(int paraIndex, boolean startsParagraph, boolean endsParagraph, long top) {
        this.paraIndex = paraIndex;
        this.startsParagraph = startsParagraph;
        this.endsParagraph = endsParagraph;
        this.top = top;
    }

    /**
     * @param lines    같은 문단의 연속된 줄 (하나 이상)
     * @param backdrop 글자 뒤의 바탕 ({@link Backdrop#hides}). 숨은 글자는 보이는 글자와 한 문단에 섞여 있을 때만
     *                 뺀다 — 문단 전체가 밝은 글자면 이 엔진이 못 그리는 바탕(배지·띠 그림) 위의 글자일 수 있다.
     */
    static ParagraphText of(List<TextLine> lines, Backdrop backdrop) {
        TextLine first = lines.get(0);
        TextLine last = lines.get(lines.size() - 1);
        ParagraphText paragraph = new ParagraphText(first.paraIndex(), first.firstInPara(), endsParagraph(last),
                topOfText(lines));
        paragraph.fill(lines, hasShownText(lines, backdrop) ? backdrop : Backdrop.KEEP_ALL);
        if (paragraph.hidden.length() > 0 && paragraph.text().isEmpty()) {
            // 남은 글자가 괘선·자리표시뿐이라 지워지면 숨은 글자만 있던 문단이다 — 통째로 남긴다
            paragraph.fill(lines, Backdrop.KEEP_ALL);
        }
        return paragraph;
    }

    private void fill(List<TextLine> lines, Backdrop backdrop) {
        raw.setLength(0);
        hidden.setLength(0);
        for (TextLine line : lines) {
            LineText.appendTo(line, raw, hidden, backdrop);
        }
    }

    private static boolean hasShownText(List<TextLine> lines, Backdrop backdrop) {
        for (TextLine line : lines) {
            if (LineText.hasShownText(line, backdrop)) return true;
        }
        return false;
    }

    /**
     * 문단의 진짜 끝 줄인가. 엔진은 강제 줄나눔(Shift+Enter)으로 끝난 줄에도 정렬 때문에 {@code lastInPara}를 켜므로
     * 그런 줄은 끝으로 보지 않는다 — 쪽 끝 줄이 강제 줄나눔이면 다음 쪽 조각이 이어 붙어야 한다.
     */
    private static boolean endsParagraph(TextLine line) {
        return line.lastInPara() && !line.endsWithLineBreak();
    }

    /**
     * 글자가 있는 첫 줄의 위쪽. 표를 앵커한 문단은 첫 줄이 표 자리뿐이고 글자는 표 아래 줄에 있기도 해서
     * (자리차지 표가 문단 나머지를 밀어낸다), 빈 첫 줄로 순서를 정하면 표 아래 글자가 표 앞에 온다.
     */
    private static long topOfText(List<TextLine> lines) {
        for (TextLine line : lines) {
            if (LineText.hasVisibleText(line)) return line.area().top();
        }
        return lines.get(0).area().top();
    }

    /** 글자가 있는 첫 줄의 위쪽 (HWP 단위). 표·글상자를 문단 사이에 끼울 자리를 정한다. */
    long top() {
        return top;
    }

    int paraIndex() {
        return paraIndex;
    }

    /** 이 조각까지 문단 끝 줄이 나왔는가. */
    boolean endsParagraph() {
        return endsParagraph;
    }

    /** 이 조각이 previous 문단의 뒷부분인가 — 앞 조각이 문단 끝 줄로 끝나지 않았고, 이 조각이 문단 첫 줄로 시작하지 않는다. */
    boolean continues(ParagraphText previous) {
        return !startsParagraph && !previous.endsParagraph && previous.paraIndex == paraIndex;
    }

    void append(ParagraphText next) {
        raw.append(next.raw);
        hidden.append(next.hidden);
        endsParagraph = next.endsParagraph;
    }

    @Override
    public String text() {
        return PlainText.normalize(raw);
    }

    @Override
    public String droppedText() {
        return hidden.toString();
    }
}
