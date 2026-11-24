package kr.dogfoot.hwpdrawer.export.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 표 셀 하나의 글자와 칸 안에 쓰는 방법.
 *
 * <ul>
 *   <li>{@link #paragraphs()}: 셀 안 문단들 (안쪽 표는 셀 글자를 차례로 푼 것). 칸 하나에 쓸 때 쓴다.</li>
 *   <li>{@link #alignedParagraphs()}: 빈 문단도 빈 문자열로 남긴 것. 나란한 두 칸의 목록을 줄 맞춰 쓸 때 쓴다.</li>
 *   <li>{@link #blocks()}: 셀을 본문처럼 쓴 블록들 (안쪽 표는 표로). 칸에 넣을 수 없는 셀({@link #isComplex()})만 있다.</li>
 * </ul>
 */
final class CellText {
    /** Markdown 표 칸 안에서는 줄을 바꿀 수 없으므로 셀 안 문단·강제 줄나눔을 이렇게 잇는다. */
    private static final String MARKDOWN_LINE_BREAK = "<br>";
    /** 서식 행에서 항목 사이에 넣는다. 항목이 아니면(줄 바뀐 라벨·문장) 공백으로 잇는다. */
    private static final String FORM_ITEM_SEPARATOR = " / ";
    /** 목록 항목의 시작: 1. 1) (1) 4· 가. 가) ① - • □ ※ * 등, 또는 숫자·날짜(뒤에 단위 한두 글자)뿐인 문단. */
    private static final Pattern ITEM_START = Pattern.compile(
            "^(\\d+[.)]|\\(\\d+\\)|\\d+[·․ㆍ]\\s|[가-힣][.)](\\s|$)|[①-⑳❶-❿]|[-•▪◦∘○●□■◆◇❍❑※*▶▷►․ㆍ]"
                    + "|[\\d\\s.,~%:-]+[가-힣]{0,2}$)");
    /** 세로 라벨 조각: 한글 한 음절, 숫자, 동그라미 번호, 구분 기호 하나. */
    private static final Pattern STACK_PIECE = Pattern.compile("^([가-힣]|\\d{1,3}|[①-⑳]|[·‧∙․ㆍ/,])$");
    private static final Pattern SYLLABLE = Pattern.compile("^[가-힣]$");
    /** "항목 : 내용" 형식의 문단 시작. */
    private static final Pattern LABEL_COLON = Pattern.compile("^[^:：]{1,30}[:：]\\s");
    /** 항목 머리("1. 개인정보 활용 목적") 뒤 문단이 이보다 길면 그 본문으로 본다 (짧으면 머리가 줄 바뀐 것). */
    private static final int MIN_BODY_LENGTH = 20;
    /** 칸이 하나뿐인 행에서 이 문단 수 이하의 짧은 라벨("전유/부분")은 한 줄로 쓴다. */
    private static final int MAX_LABEL_PARAGRAPHS = 2;

    private final int row;
    private final int column;
    private final int rowSpan;
    private final int columnSpan;
    private final List<String> paragraphs;
    private final List<String> alignedParagraphs;
    private final List<TextBlock> blocks;
    private final boolean pageFrame;
    /** 칸에 쓴 문단에서 보이지 않아 뺀 글자 (칸에 쓰는 셀만 — 블록으로 쓰는 셀은 블록이 기록한다). */
    private final String droppedText;

    private CellText(int row, int column, int rowSpan, int columnSpan, List<String> paragraphs,
                     List<String> alignedParagraphs, List<TextBlock> blocks, boolean pageFrame,
                     String droppedText) {
        this.row = row;
        this.column = column;
        this.rowSpan = rowSpan;
        this.columnSpan = columnSpan;
        this.paragraphs = joinStackedLabel(paragraphs);
        this.alignedParagraphs = alignedParagraphs;
        this.blocks = blocks;
        this.pageFrame = pageFrame;
        this.droppedText = droppedText;
    }

    /**
     * 칸 안에 쓸 수 있는 셀.
     *
     * @param alignedParagraphs 빈 문단도 빈 문자열로 남긴 문단들
     */
    static CellText simple(TableCells.CellContent content, List<String> paragraphs, List<String> alignedParagraphs,
                           String droppedText) {
        return new CellText(content.row(), content.column(), content.rowSpan(), content.columnSpan(),
                paragraphs, alignedParagraphs, null, false, droppedText);
    }

    /**
     * 칸 안에 쓸 수 없는 셀: 안쪽에 표가 있는 셀, 또는 그 행에 혼자이면서 표 글자 대부분을 가진 쪽 틀(pageFrame).
     * 쪽 틀이 있는 행에서는 표를 끊는다 — 틀 앞뒤는 서로 다른 표다.
     */
    static CellText complex(TableCells.CellContent content, List<String> paragraphs, List<TextBlock> blocks,
                            boolean pageFrame) {
        return new CellText(content.row(), content.column(), content.rowSpan(), content.columnSpan(),
                paragraphs, paragraphs, blocks, pageFrame, "");
    }

    int row() {
        return row;
    }

    /** 왼쪽 첫 칸. */
    int column() {
        return column;
    }

    int rowSpan() {
        return rowSpan;
    }

    int columnSpan() {
        return columnSpan;
    }

    List<String> paragraphs() {
        return paragraphs;
    }

    List<String> alignedParagraphs() {
        return alignedParagraphs;
    }

    /** 문단이 여럿인가 (세로 라벨을 붙인 셀은 한 문단). */
    boolean isList() {
        return paragraphs.size() > 1;
    }

    boolean isComplex() {
        return blocks != null;
    }

    boolean isPageFrame() {
        return pageFrame;
    }

    String droppedText() {
        return droppedText;
    }

    List<TextBlock> blocks() {
        return blocks;
    }

    /** 같은 칸에 놓인 두 셀(잘못 만든 표)을 하나로 — 글자를 잃지 않게. */
    CellText joinedWith(CellText other) {
        List<String> joined = new ArrayList<>(paragraphs);
        joined.addAll(other.paragraphs);
        List<TextBlock> joinedBlocks = null;
        if (isComplex() || other.isComplex()) {
            joinedBlocks = new ArrayList<>(asBlocks());
            joinedBlocks.addAll(other.asBlocks());
        }
        return new CellText(row, column, rowSpan, columnSpan, joined, joined, joinedBlocks, pageFrame || other.pageFrame,
                droppedText + other.droppedText);
    }

    private List<TextBlock> asBlocks() {
        if (isComplex()) return blocks;
        List<TextBlock> result = new ArrayList<>();
        for (String paragraph : paragraphs) result.add(TextBlock.fixed(paragraph));
        return result;
    }

    /**
     * Markdown 표 칸: 문단·강제 줄나눔은 &lt;br&gt;, 역슬래시와 '|'는 이스케이프.
     *
     * @param aligned 같은 행의 나란한 목록과 줄을 맞춘다 (빈 문단도 &lt;br&gt; 자리로)
     */
    String asMarkdownCell(boolean aligned) {
        String text = String.join(MARKDOWN_LINE_BREAK, aligned && isList() ? alignedParagraphs : paragraphs);
        return text.replace("\\", "\\\\").replace("|", "\\|").replace("\n", MARKDOWN_LINE_BREAK);
    }

    /**
     * 서식 행의 칸 하나: 목록 항목(1. - □ ※ 등)이 시작되거나, 앞 문단이 문장으로 끝났거나, 앞 문단이 항목 머리
     * ("1. 개인정보 활용 목적")이고 다음 문단이 그 본문이면 " / ", 아니면 공백으로 잇는다. 좁은 칸에서 줄을 바꿔
     * 쓴 라벨("발달/재활/서비스")·문장을 " / "로 끊으면 목록처럼 읽힌다.
     */
    String asFormCell() {
        StringBuilder out = new StringBuilder();
        String previous = null;
        for (String paragraph : paragraphs) {
            String text = paragraph.replace('\n', ' ');
            if (previous != null) out.append(isNewItem(previous, text) ? FORM_ITEM_SEPARATOR : " ");
            out.append(text);
            previous = text;
        }
        return out.toString();
    }

    /** 칸이 하나뿐인 행에 쓸 줄들: 짧은 라벨은 한 줄로, 항목·문장이 여럿이면 문단마다 한 줄. */
    List<String> asOnlyCellOfRow() {
        if (paragraphs.size() > MAX_LABEL_PARAGRAPHS) return paragraphs;
        for (int i = 0; i < paragraphs.size(); i++) {
            if (endsSentence(paragraphs.get(i))) return paragraphs;
            if (i > 0 && isNewItem(paragraphs.get(i - 1), paragraphs.get(i))) return paragraphs;
        }
        return Collections.singletonList(asFormCell());
    }

    /**
     * next가 새 항목인가: 목록 표시·"항목 :"으로 시작, 앞 문단이 문장이나 체크 상자(□)로 끝남, 또는 앞 문단이 항목
     * 머리이고 next가 그 본문(긴 문단)이다. 짧은 항목 머리의 뒷부분("□노인" + "돌봄", "⑯ 공제대상" + "교육비부담액")은
     * 줄 바뀐 같은 항목이다.
     */
    private static boolean isNewItem(String previous, String next) {
        return startsItem(next) || LABEL_COLON.matcher(next).find() || endsSentence(previous)
                || endsWithCheckBox(previous) || (startsItem(previous) && next.length() > MIN_BODY_LENGTH);
    }

    private static boolean endsWithCheckBox(String paragraph) {
        char last = paragraph.charAt(paragraph.length() - 1);
        return last == '□' || last == '■';
    }

    private static boolean startsItem(String paragraph) {
        return ITEM_START.matcher(paragraph).find();
    }

    private static boolean endsSentence(String paragraph) {
        char last = paragraph.charAt(paragraph.length() - 1);
        return last == '.' || last == '?' || last == '!';
    }

    /**
     * 한 음절씩 문단을 나눠 세로로 쓴 라벨("읍/면/동/주/민/센/터", "⑩/설/치/자", "5/대/전/략/‧/20/개")은 붙여
     * 한 단어로. 공백을 넣으면 "읍 면 동"이 되어 검색어와 맞지 않는다. 두 음절 이상 조각("0년", "문중")이나
     * 숫자뿐인 셀(1/2/3)은 그대로 둔다 — 띄어쓰기를 알 수 없다.
     */
    private static List<String> joinStackedLabel(List<String> paragraphs) {
        if (paragraphs.size() < 2) return paragraphs;
        int syllables = 0;
        for (String paragraph : paragraphs) {
            if (!STACK_PIECE.matcher(paragraph).matches()) return paragraphs;
            if (SYLLABLE.matcher(paragraph).matches()) syllables++;
        }
        return syllables >= 2 ? Collections.singletonList(String.join("", paragraphs)) : paragraphs;
    }
}
