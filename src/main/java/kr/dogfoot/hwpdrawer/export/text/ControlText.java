package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.Content;
import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.document.control.GsoContainerItem;
import kr.dogfoot.hwpdrawer.document.control.GsoTextBoxItem;
import kr.dogfoot.hwpdrawer.document.control.TableItem;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 한 흐름(본문 한 쪽, 셀, 글상자)의 문단과 컨트롤을 읽는 순서대로 블록 목록에 쓴다.
 *
 * <ul>
 *   <li>표: 본문에서는 {@link TableGrid} (자료 표는 Markdown 표, 서식은 행마다 한 줄). 글자가 든 셀이 하나뿐이면
 *       (테두리 상자로 쓴 표) 그 셀 내용을 문단으로 쓴다. 안쪽에 표가 있는 셀·본문을 담은 큰 셀(쪽 틀)은 칸에 넣지
 *       않고 그 자리에 본문처럼 쓴다. 나뉜 표는 글자가 있는 첫 조각 자리에 한 번만 쓴다.
 *       Markdown 셀 안의 표는 셀 글자를 차례로 쓴다. 여러 쪽으로 나뉜 표는 첫 조각 자리에 한 번만 쓴다.</li>
 *   <li>글상자: 안의 문단을 그 자리에 쓴다 (같은 내용의 복제본은 한 번만). 묶음 개체는 안의 글상자·표를 위→아래로.</li>
 * </ul>
 */
final class ControlText {
    /** 그 행에 혼자이고, 이 문단 수 이상이며 표 글자의 절반 이상을 가진 셀은 표 칸이 아니라 본문 틀로 본다. */
    private static final int MIN_PARAGRAPHS_OF_DOMINANT_CELL = 5;
    private final TableSegments tableSegments;
    /** 표마다 글자가 든 셀들 ({@link #filledCellsOf}). 같은 표는 늘 같은 결과라 한 번만 만든다. */
    private final Map<ControlTable, List<FilledCell>> filledCells = new IdentityHashMap<>();
    /** 지금 쓰는 흐름에서 이미 쓴 글상자 내용. 흐름(셀 내용·글상자 내용)마다 새로 시작한다. */
    private Set<Document> writtenTextBoxes = newIdentitySet();

    ControlText(TableSegments tableSegments) {
        this.tableSegments = tableSegments;
    }

    /** @param backdrop 이 흐름의 글자 뒤 바탕 — 바탕과 같은 색이라 보이지 않는 글자는 빼고 쓴다 */
    void appendFlow(Content content, List<ControlItem> controls, BlockList out, FlowContext context,
                    Backdrop backdrop) {
        for (FlowItem item : ReadingOrder.of(content, controls, backdrop)) {
            if (item.isParagraph()) {
                out.addParagraph(item.paragraph());
            } else {
                appendControl(item.control(), out, context, backdrop);
            }
        }
    }

    private void appendControl(ControlItem control, BlockList out, FlowContext context, Backdrop backdrop) {
        if (control instanceof TableItem) {
            appendTable((TableItem) control, out, context, backdrop);
        } else if (control instanceof GsoTextBoxItem) {
            appendTextBox((GsoTextBoxItem) control, out, context, backdrop.inControl(control));
        } else if (control instanceof GsoContainerItem) {
            for (ControlItem child : ReadingOrder.textControlsOf(((GsoContainerItem) control).children())) {
                appendControl(child, out, context, backdrop);
            }
        }
    }

    /**
     * 글상자는 내용(innerDocument)마다 한 번만 쓴다. 쪽을 넘어 나뉜 표 셀에 걸친 글상자는 엔진이 셀 조각마다
     * 복제하는데, 복제본이 같은 내용을 가리킨다.
     */
    private void appendTextBox(GsoTextBoxItem box, BlockList out, FlowContext context, Backdrop boxBackdrop) {
        Document inner = box.innerDocument();
        if (inner == null || !writtenTextBoxes.add(inner)) return;
        out.addAll(blocksOf(Collections.singletonList(inner), Collections.singletonList(boxBackdrop), context));
    }

    private void appendTable(TableItem item, BlockList out, FlowContext context, Backdrop backdrop) {
        if (!tableSegments.isWrittenAt(item)) return;
        List<FilledCell> filled = filledCellsOf(item, backdrop);
        if (filled.isEmpty()) return;

        if (context == FlowContext.CELL) {
            for (FilledCell cell : filled) out.addAll(cell.blocks);
        } else if (filled.size() == 1) {
            FilledCell only = filled.get(0);
            out.addAll(blocksOf(only.content.documents(), only.backdrops, FlowContext.BLOCK));
        } else {
            TableCells table = TableCells.of(tableSegments.segmentsOf(item));
            TableGrid grid = new TableGrid(table.rowCount(), table.columnCount(), table.cellCount(), table.isRegular());
            for (FilledCell cell : filled) grid.put(cellTextOf(cell, filled));
            for (TextBlock block : grid.render()) out.addBlock(block);
        }
    }

    /**
     * 글자가 든 셀들과 그 셀 내용 (안쪽 표는 셀 글자를 차례로 푼 것). 표마다 한 번만 만든다 — 안쪽 표가 있는지
     * 볼 때와 쓸 때 같은 셀을 다시 쓰면 표가 겹칠수록 일이 곱절로 는다.
     */
    private List<FilledCell> filledCellsOf(TableItem item, Backdrop backdrop) {
        List<FilledCell> filled = filledCells.get(item.table());
        if (filled != null) return filled;
        filled = new ArrayList<>();
        for (TableCells.CellContent cell : TableCells.of(tableSegments.segmentsOf(item)).cells()) {
            List<Backdrop> backdrops = backdropsOf(cell, item, backdrop);
            BlockList blocks = blocksOf(cell.documents(), backdrops, FlowContext.CELL);
            List<String> paragraphs = blocks.texts();
            if (!paragraphs.isEmpty()) filled.add(new FilledCell(cell, backdrops, blocks, paragraphs));
        }
        filledCells.put(item.table(), filled);
        return filled;
    }

    /**
     * 셀 내용 조각마다의 바탕. 나뉜 표의 다른 조각은 다른 쪽에 놓이므로, 본문 쪽에 바로 놓인 조각이면 그 쪽의 칠한
     * 개체를 본다 (셀·글상자 안에서 나뉜 표는 지금 흐름의 바탕으로 갈음한다).
     *
     * @param item     지금 쓰는 조각 — 그 조각의 내용은 지금 흐름의 바탕 위에 있다
     * @param backdrop 지금 흐름의 바탕
     */
    private List<Backdrop> backdropsOf(TableCells.CellContent cell, TableItem item, Backdrop backdrop) {
        List<Backdrop> backdrops = new ArrayList<>();
        for (int i = 0; i < cell.documents().size(); i++) {
            TableItem segment = cell.segmentOf(i);
            List<ControlItem> segmentPage = segment == item ? null : tableSegments.pageControlsOf(segment);
            Backdrop flow = segmentPage == null ? backdrop : backdrop.onPage(segmentPage);
            backdrops.add(flow.inCell(cell.borderFillId(), cell.verticalOffsetOf(i)));
        }
        return backdrops;
    }

    /**
     * 칸에 넣을 셀인지, 본문처럼 블록으로 쓸 셀인지 정한다. 안쪽에 칸이 둘 이상 찬 표가 있는 셀(쪽 테두리로 쓴
     * 바깥 표), 그리고 그 행에 혼자이면서 표 글자 대부분을 가진 큰 셀(본문 전체를 담은 틀)은 블록으로 — 칸 하나에
     * 몰아 쓰면 안쪽 표의 행·열과 조항 구분이 없어진다. 같은 행에 다른 칸이 있으면(○/× 대조표, 조직표) 큰 셀도
     * 그 행의 칸이다.
     */
    private CellText cellTextOf(FilledCell cell, List<FilledCell> filled) {
        TableCells.CellContent content = cell.content;
        boolean pageFrame = isPageFrame(cell, filled);
        if (pageFrame || containsGridTable(content.documents(), cell.backdrops)) {
            return CellText.complex(content, cell.paragraphs,
                    blocksOf(content.documents(), cell.backdrops, FlowContext.BLOCK).blocks(), pageFrame);
        }
        return CellText.simple(content, cell.paragraphs, cell.blocks.textsKeepingEmptyParagraphs(),
                cell.blocks.droppedText());
    }

    private static boolean isPageFrame(FilledCell cell, List<FilledCell> filled) {
        if (cell.paragraphs.size() < MIN_PARAGRAPHS_OF_DOMINANT_CELL) return false;
        int tableLength = 0;
        for (FilledCell each : filled) {
            tableLength += each.length();
            if (each != cell && each.content.row() == cell.content.row()) return false;
        }
        return cell.length() * 2 >= tableLength;
    }

    /** 안쪽(글상자·한 칸짜리 틀 표 안 포함)에 글자가 든 셀이 둘 이상인 표가 있는가. */
    private boolean containsGridTable(List<Document> documents, List<Backdrop> backdrops) {
        for (int i = 0; i < documents.size(); i++) {
            Document document = documents.get(i);
            if (document == null) continue;
            for (Page page : document.pages()) {
                if (hasGridTableIn(page.controls(), backdrops.get(i).withPage(page.controls()))) return true;
            }
        }
        return false;
    }

    private boolean hasGridTableIn(List<ControlItem> controls, Backdrop backdrop) {
        for (ControlItem control : controls) {
            if (control instanceof TableItem) {
                List<FilledCell> filled = filledCellsOf((TableItem) control, backdrop);
                if (filled.size() > 1) return true;
                if (filled.size() == 1 && containsGridTable(filled.get(0).content.documents(), filled.get(0).backdrops)) {
                    return true;
                }
            } else if (control instanceof GsoTextBoxItem) {
                Document inner = ((GsoTextBoxItem) control).innerDocument();
                if (inner != null && containsGridTable(Collections.singletonList(inner),
                        Collections.singletonList(backdrop.inControl(control)))) {
                    return true;
                }
            } else if (control instanceof GsoContainerItem) {
                if (hasGridTableIn(((GsoContainerItem) control).children(), backdrop)) return true;
            }
        }
        return false;
    }

    /** 글자가 든 셀 하나: 셀 안 문단(본문 흐름 안 표는 풀어 쓴 것)과 그 블록. */
    private static final class FilledCell {
        private final TableCells.CellContent content;
        /** content.documents()와 같은 순서의 조각별 바탕. */
        private final List<Backdrop> backdrops;
        private final BlockList blocks;
        private final List<String> paragraphs;

        FilledCell(TableCells.CellContent content, List<Backdrop> backdrops, BlockList blocks, List<String> paragraphs) {
            this.content = content;
            this.backdrops = backdrops;
            this.blocks = blocks;
            this.paragraphs = paragraphs;
        }

        int length() {
            int length = 0;
            for (String paragraph : paragraphs) length += paragraph.length();
            return length;
        }
    }

    /**
     * 셀·글상자 내용 (backdrops는 documents와 같은 순서의 조각별 바탕). 나뉜 셀의 쪽 조각들은 한 흐름이라 끊긴
     * 문단을 잇고, 조각마다 복제된 글상자는 한 번만 쓴다.
     * 같은 셀을 여러 번 써 볼 수 있으므로(비었는지, 안에 표가 있는지) 글상자 기록은 이 흐름 안에서만 유효하다.
     */
    private BlockList blocksOf(List<Document> documents, List<Backdrop> backdrops, FlowContext context) {
        Set<Document> outer = writtenTextBoxes;
        writtenTextBoxes = newIdentitySet();
        try {
            BlockList blocks = new BlockList();
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                if (document == null) continue;
                for (Page page : document.pages()) {
                    appendFlow(page.content(), page.controls(), blocks, context,
                            backdrops.get(i).withPage(page.controls()));
                }
            }
            return blocks;
        } finally {
            writtenTextBoxes = outer;
        }
    }

    private static Set<Document> newIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
