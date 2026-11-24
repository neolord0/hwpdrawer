package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;
import kr.dogfoot.hwplib.object.bodytext.control.table.Cell;
import kr.dogfoot.hwplib.object.bodytext.control.table.Row;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.ParagraphList;
import kr.dogfoot.hwplib.object.docinfo.DocInfo;
import kr.dogfoot.hwplib.object.docinfo.Numbering;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.numbering.LevelNumbering;
import kr.dogfoot.hwplib.object.docinfo.parashape.ParaHeadShape;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 문단 번호를 문서 순서대로 매긴다.
 *
 * <p>번호는 앞 문단들에 의존하는 <b>누적 상태</b>라, 문단 하나만 보고는 정할 수 없다.
 * 그래서 레이아웃 전에 문서를 한 번 훑어 문단마다 번호 문자열을 미리 확정해 둔다.
 * 레이아웃 도중에 세는 방식은 쓰지 않는다 — 페이지 넘침 재시도나 LineSeg 합성처럼
 * 같은 문단을 두 번 배치하는 경로가 있어 번호가 중복 증가한다.</p>
 *
 * <p><b>규칙</b>(문단번호/개요 참조 출력의 47개 문단 전부와 대조해 확인):</p>
 * <ul>
 *   <li>수준 L의 번호 문단이 나오면 counter[L]을 1 늘린다. 그 수준이 처음이거나
 *       리셋된 뒤면 {@link LevelNumbering#getStartNumber()}부터 시작한다.</li>
 *   <li>그보다 <b>깊은 수준은 전부</b> 리셋한다(한 단계만이 아니다).</li>
 *   <li>번호 문단이 아닌 문단은 카운터를 건드리지 않는다.</li>
 *   <li>카운터는 페이지 경계와 무관하게 이어진다.</li>
 * </ul>
 */
public final class ParagraphNumberer {

    /** hwplib의 수준 개수. */
    private static final int LEVEL_COUNT = 10;

    private final DocInfo docInfo;
    private final Map<Paragraph, String> headTexts = new IdentityHashMap<>();
    private final Map<Integer, Counter> counters = new HashMap<>();

    private ParagraphNumberer(DocInfo docInfo) {
        this.docInfo = docInfo;
    }

    /** 섹션들을 문서 순서로 훑어 문단별 번호를 확정한다. */
    public static ParagraphNumberer of(DocInfo docInfo, List<Section> sections) {
        ParagraphNumberer numberer = new ParagraphNumberer(docInfo);
        if (docInfo == null || sections == null) return numberer;
        for (Section section : sections) {
            for (int i = 0; i < section.getParagraphCount(); i++) {
                numberer.visit(section.getParagraph(i));
            }
        }
        return numberer;
    }

    /** 이 문단에 붙일 번호 문자열. 번호 문단이 아니거나 서식이 없으면 null. */
    public String headTextOf(Paragraph para) {
        return headTexts.get(para);
    }

    private void visit(Paragraph para) {
        assignNumber(para);
        descendIntoTables(para);
    }

    /** 표 안의 문단도 문서 순서에 포함된다. */
    private void descendIntoTables(Paragraph para) {
        if (para.getControlList() == null) return;
        for (Control control : para.getControlList()) {
            if (!(control instanceof ControlTable)) continue;
            for (Row row : ((ControlTable) control).getRowList()) {
                for (Cell cell : row.getCellList()) {
                    ParagraphList cellParas = cell.getParagraphList();
                    for (int i = 0; i < cellParas.getParagraphCount(); i++) {
                        visit(cellParas.getParagraph(i));
                    }
                }
            }
        }
    }

    private void assignNumber(Paragraph para) {
        ParaShape paraShape = paraShapeOf(para);
        if (paraShape == null) return;

        ParaHeadShape headShape = paraShape.getProperty1().getParaHeadShape();
        if (headShape != ParaHeadShape.Numbering && headShape != ParaHeadShape.Outline) return;

        Numbering numbering = numberingOf(paraShape.getParaHeadId());
        if (numbering == null) return;

        List<LevelNumbering> levels = numbering.getLevelNumberingList();
        int level = levelOf(paraShape);
        if (level < 0 || level >= levels.size()) return;

        Counter counter = counters.computeIfAbsent(paraShape.getParaHeadId(), id -> new Counter());
        counter.advance(level, levels);

        String text = expand(levels, level, counter);
        if (text != null && !text.isEmpty()) headTexts.put(para, text);
    }

    /**
     * 문단 수준 — 두 필드 중 <b>큰 값</b>을 쓴다. 둘 다 불완전해서 한쪽만으로는 안 된다.
     *
     * <ul>
     *   <li>{@code property1.getParaLevel()}(비트 25~27)은 한글이 <b>6에서 잘라</b> 기록해
     *       실제 7·8수준이 모두 6으로 들어온다(이 문서 psId 26·27에서 실측).</li>
     *   <li>{@link ParaShape#getParaLevel()}(레코드 끝 UINT32)은 정확하지만 <b>HWP 5.0.x
     *       파일에는 필드 자체가 없어</b> 항상 0이다(테스트 문서 289개 중 205개가 그렇다).
     *       이쪽만 쓰면 그 문서들의 수준이 전부 0으로 무너진다.</li>
     * </ul>
     */
    private static int levelOf(ParaShape paraShape) {
        return Math.max((int) paraShape.getParaLevel(), paraShape.getProperty1().getParaLevel());
    }

    /**
     * 번호 서식의 자리표시자 {@code ^N}을 <b>N번째 수준의 현재 번호</b>로 바꾼다.
     * 번호 글자 종류는 그 수준의 {@link kr.dogfoot.hwplib.object.docinfo.numbering.ParagraphNumberFormat}을 따른다.
     * 예: 수준 1의 서식 {@code "^2)"} + Hangul → {@code "나)"}.
     */
    private static String expand(List<LevelNumbering> levels, int level, Counter counter) {
        LevelNumbering levelNumbering = levels.get(level);
        if (levelNumbering.getNumberFormat() == null) return null;
        String format = levelNumbering.getNumberFormat().toUTF16LEString();
        if (format == null || format.isEmpty()) return null;

        StringBuilder out = new StringBuilder(format.length());
        for (int i = 0; i < format.length(); i++) {
            char c = format.charAt(i);
            int refLevel = (c == '^' && i + 1 < format.length())
                    ? Character.digit(format.charAt(i + 1), 10) - 1 : -1;
            if (refLevel < 0 || refLevel >= levels.size()) {
                out.append(c);
                continue;
            }
            out.append(ParagraphNumberFormatter.format(
                    counter.valueOf(refLevel, levels),
                    levels.get(refLevel).getParagraphHeadInfo().getProperty().getParagraphNumberFormat()));
            i++;   // 자리표시자의 숫자까지 소비
        }
        return out.toString();
    }

    private ParaShape paraShapeOf(Paragraph para) {
        try {
            return docInfo.getParaShapeList().get((int) para.getHeader().getParaShapeId());
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /** paraHeadId는 1부터 시작한다(0 = 없음). */
    private Numbering numberingOf(int paraHeadId) {
        if (paraHeadId <= 0) return null;
        try {
            return docInfo.getNumberingList().get(paraHeadId - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }

    /** 한 Numbering의 수준별 번호 상태. */
    private static final class Counter {
        private final long[] values = new long[LEVEL_COUNT];
        private final boolean[] started = new boolean[LEVEL_COUNT];

        /** 수준 L의 번호를 하나 진행하고, 그보다 깊은 수준을 전부 리셋한다. */
        void advance(int level, List<LevelNumbering> levels) {
            if (level >= LEVEL_COUNT) return;
            if (started[level]) {
                values[level]++;
            } else {
                values[level] = levels.get(level).getStartNumber();
                started[level] = true;
            }
            for (int deeper = level + 1; deeper < LEVEL_COUNT; deeper++) started[deeper] = false;
        }

        /** 수준 L의 현재 번호. 아직 시작 전이면 그 수준의 시작 번호. */
        long valueOf(int level, List<LevelNumbering> levels) {
            if (level >= LEVEL_COUNT) return 1;
            if (started[level]) return values[level];
            return level < levels.size() ? levels.get(level).getStartNumber() : 1;
        }
    }
}
