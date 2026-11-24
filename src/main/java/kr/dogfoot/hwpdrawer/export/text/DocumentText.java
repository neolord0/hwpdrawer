package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.page.Footnote;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwplib.object.HWPFile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * 페이지 목록을 AI 검색(RAG)용 텍스트로 만든다.
 *
 * <pre>
 * &lt;!-- page 3 --&gt;
 *
 * 문단 하나는 한 줄이다. 화면의 줄바꿈은 없애고, 쪽·단·표 조각 경계에서 끊긴 문단은 다시 잇는다.
 *
 * | 구분 | 내용 |
 * | --- | --- |
 * | 셀 | 셀 안 문단은 &lt;br&gt;로 잇는다 |
 *
 * [각주] 1) 각주는 그 쪽 끝에 둔다. 각주 하나가 블록 하나다.
 * </pre>
 *
 * <ul>
 *   <li>읽는 순서: 행 → 단 → 단 안에서 위→아래. 표·글상자는 놓인 높이에서 문단 사이에 끼운다 ({@link ReadingOrder}).</li>
 *   <li>여러 쪽에 걸친 문단·표는 한꺼번에 둔다 — 문장과 표가 조각나지 않게. 문단은 시작한 쪽에, 표는 글자가 있는
 *       첫 조각의 쪽에.</li>
 *   <li>표 형식(Markdown 표 / 서식 행)은 {@link TableGrid}, 셀 글자는 {@link CellText}가 정한다.</li>
 *   <li>머리말·꼬리말·쪽 번호는 쪽마다 되풀이되는 군더더기라 넣지 않는다. 그림·도형도 글자가 없으니 넣지 않는다.</li>
 * </ul>
 */
final class DocumentText {
    private static final String BLOCK_SEPARATOR = "\n\n";
    private static final String FOOTNOTE_PREFIX = "[각주] ";

    private final ControlText controlText;
    private final HWPFile file;

    private DocumentText(ControlText controlText, HWPFile file) {
        this.controlText = controlText;
        this.file = file;
    }

    /** @param file 셀·도형 채우기를 읽을 문서 (보이지 않는 글자를 가린다) */
    static String of(List<Page> pages, HWPFile file) {
        return new DocumentText(new ControlText(TableSegments.collect(pages)), file).render(pages).text;
    }

    /** {@link #of}가 일부러 되풀이한 글자 ({@link TextBlock#repeatedText()}). 빠짐·중복 검사용. */
    static String repeatedTextOf(List<Page> pages, HWPFile file) {
        return new DocumentText(new ControlText(TableSegments.collect(pages)), file).render(pages).repeatedText;
    }

    /** {@link #of}가 보이지 않아 뺀 글자 ({@link TextBlock#droppedText()}). 빠짐·중복 검사용. */
    static String droppedTextOf(List<Page> pages, HWPFile file) {
        return new DocumentText(new ControlText(TableSegments.collect(pages)), file).render(pages).droppedText;
    }

    private Rendered render(List<Page> pages) {
        // 다음 쪽이 앞 쪽 마지막 문단을 이어 쓸 수 있으므로 모든 쪽을 다 모은 뒤에 문자열로 만든다.
        BlockList[] bodies = new BlockList[pages.size()];
        BlockList body = new BlockList();
        for (int i = 0; i < pages.size(); i++) {
            body = body.nextPage();
            Page page = pages.get(i);
            controlText.appendFlow(page.content(), page.controls(), body, FlowContext.BLOCK,
                    Backdrop.page(file, page.controls()));
            bodies[i] = body;
        }

        List<String> blocks = new ArrayList<>();
        StringBuilder repeated = new StringBuilder();
        StringBuilder dropped = new StringBuilder();
        for (int i = 0; i < pages.size(); i++) {
            Page page = pages.get(i);
            blocks.add(pageMarker(page.pageNo()));
            blocks.addAll(bodies[i].texts());
            repeated.append(bodies[i].repeatedText());
            dropped.append(bodies[i].droppedText());
            for (BlockList note : footnotesOf(page)) {
                blocks.add(FOOTNOTE_PREFIX + String.join("\n", note.texts()));
                repeated.append(note.repeatedText());
                dropped.append(note.droppedText());
            }
        }
        String text = blocks.isEmpty() ? "" : String.join(BLOCK_SEPARATOR, blocks) + "\n";
        return new Rendered(text, repeated.toString(), dropped.toString());
    }

    /**
     * 각주 하나 = 블록 하나 ("[각주] " + 문단들을 줄바꿈으로). 각주 번호 "1)"은 각주 본문에 이미 들어 있다.
     * 각주의 둘째 문단부터 따로 블록으로 쓰면 본문 조항처럼 보인다.
     */
    private List<BlockList> footnotesOf(Page page) {
        if (page.footnotes() == null) return Collections.emptyList();
        List<BlockList> notes = new ArrayList<>();
        for (Footnote note : page.footnotes().notes()) {
            BlockList one = new BlockList();
            controlText.appendFlow(note.content(), Collections.emptyList(), one, FlowContext.BLOCK,
                    Backdrop.page(file, page.controls()));
            if (!one.texts().isEmpty()) notes.add(one);
        }
        return notes;
    }

    private static String pageMarker(int pageNo) {
        return String.format(Locale.ROOT, "<!-- page %d -->", pageNo);
    }

    /** 만든 글자, 그 안에서 일부러 되풀이한 글자, 보이지 않아 뺀 글자. */
    private static final class Rendered {
        private final String text;
        private final String repeatedText;
        private final String droppedText;

        Rendered(String text, String repeatedText, String droppedText) {
            this.text = text;
            this.repeatedText = repeatedText;
            this.droppedText = droppedText;
        }
    }
}
