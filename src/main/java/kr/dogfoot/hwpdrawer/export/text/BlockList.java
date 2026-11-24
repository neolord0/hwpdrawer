package kr.dogfoot.hwpdrawer.export.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 읽는 순서대로 쌓이는 블록 목록 하나 (본문 한 쪽, 셀 하나, 글상자 하나).
 *
 * <p>쪽·단·표 조각 경계에서 끊긴 문단 조각은 아직 끝나지 않은 같은 문단에 이어 붙인다
 * ({@link ParagraphText#continues}). 바로 앞 블록만 보면 안 된다 — 평행 다단은 단마다 다른 문단이 쪽을
 * 넘어가므로 다음 쪽의 첫 단 조각은 앞 쪽 첫 단의 문단에 이어져야 한다. 글상자·셀처럼 다른 흐름의 블록은
 * {@link #addAll}로 끝난 블록만 받으므로 그 사이에 끼워도 이 흐름의 문단 잇기에 끼어들지 않는다.</p>
 */
final class BlockList {
    private final List<TextBlock> blocks = new ArrayList<>();
    /** 끝 줄이 아직 나오지 않은 문단 (문단 번호별). 같은 흐름의 다음 쪽 블록 목록과 함께 쓴다. */
    private final Map<Integer, ParagraphText> openParagraphs;

    BlockList() {
        this(new HashMap<>());
    }

    private BlockList(Map<Integer, ParagraphText> openParagraphs) {
        this.openParagraphs = openParagraphs;
    }

    /** 같은 흐름의 다음 쪽 블록 목록. 이 쪽에서 끝나지 않은 문단을 이어 쓸 수 있다. */
    BlockList nextPage() {
        return new BlockList(openParagraphs);
    }

    void addParagraph(ParagraphText paragraph) {
        ParagraphText open = openParagraphs.get(paragraph.paraIndex());
        if (open != null && paragraph.continues(open)) {
            open.append(paragraph);
            if (open.endsParagraph()) openParagraphs.remove(paragraph.paraIndex());
            return;
        }
        blocks.add(paragraph);
        if (paragraph.endsParagraph()) {
            openParagraphs.remove(paragraph.paraIndex());
        } else {
            openParagraphs.put(paragraph.paraIndex(), paragraph);
        }
    }

    /** 표처럼 다 만든 블록. */
    void addBlock(TextBlock block) {
        blocks.add(block);
    }

    /** 다른 흐름(글상자·셀)의 블록들. 이 흐름의 문단 잇기에는 끼어들지 않는다. */
    void addAll(BlockList other) {
        blocks.addAll(other.blocks);
    }

    /**
     * 빈 문단도 빈 문자열로 남긴 글자들 (끝의 빈 문단만 뺀다 — 앞쪽 빈 문단도 줄 자리다). 나란한 두 칸의
     * 목록(산업명 | 분류번호)은 빈 문단이 줄을 맞추는 자리라, 빼면 다음 줄부터 짝이 어긋난다.
     */
    List<String> textsKeepingEmptyParagraphs() {
        List<String> texts = new ArrayList<>();
        for (TextBlock block : blocks) texts.add(block.text());
        int to = texts.size();
        while (to > 0 && texts.get(to - 1).isEmpty()) to--;
        return new ArrayList<>(texts.subList(0, to));
    }

    /** 블록들 (빈 블록도 — 뺀 글자 기록을 잃지 않게. 출력할 때는 {@link #texts()}가 빈 블록을 거른다). */
    List<TextBlock> blocks() {
        return new ArrayList<>(blocks);
    }

    /** 블록들이 일부러 되풀이한 글자 ({@link TextBlock#repeatedText()}). */
    String repeatedText() {
        StringBuilder repeated = new StringBuilder();
        for (TextBlock block : blocks) repeated.append(block.repeatedText());
        return repeated.toString();
    }

    /** 블록들이 보이지 않아 뺀 글자 ({@link TextBlock#droppedText()}), 빈 블록 것까지. */
    String droppedText() {
        StringBuilder dropped = new StringBuilder();
        for (TextBlock block : blocks) dropped.append(block.droppedText());
        return dropped.toString();
    }

    /** 빈 블록을 뺀 글자들. */
    List<String> texts() {
        List<String> texts = new ArrayList<>();
        for (TextBlock block : blocks) {
            String text = block.text();
            if (!text.isEmpty()) texts.add(text);
        }
        return texts;
    }
}
