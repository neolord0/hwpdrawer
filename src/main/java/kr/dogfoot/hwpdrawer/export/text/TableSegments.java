package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.Document;
import kr.dogfoot.hwpdrawer.document.control.ControlItem;
import kr.dogfoot.hwpdrawer.document.control.GsoContainerItem;
import kr.dogfoot.hwpdrawer.document.control.GsoTextBoxItem;
import kr.dogfoot.hwpdrawer.document.control.TableItem;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.document.text.TextColumn;
import kr.dogfoot.hwpdrawer.document.text.TextLine;
import kr.dogfoot.hwpdrawer.document.text.TextRow;
import kr.dogfoot.hwplib.object.bodytext.control.ControlTable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * 여러 쪽으로 나뉜 표의 조각들. 엔진은 쪽마다 같은 표({@link ControlTable})의 {@link TableItem}을 하나씩 두고,
 * 셀 내용도 그 쪽에 들어간 줄만 담는다. 표 하나를 한 번에 쓰려고 조각을 쪽 순서로 모아 둔다
 * (셀·글상자 안의 표도 함께 — 나뉜 셀 안의 표도 나뉜다).
 */
final class TableSegments {
    private final Map<ControlTable, List<TableItem>> segments = new IdentityHashMap<>();
    /** 본문 쪽에 바로 놓인 조각이 있는 쪽의 컨트롤들 (셀·글상자 안 조각은 없다). */
    private final Map<TableItem, List<ControlItem>> pageControls = new IdentityHashMap<>();

    private TableSegments() {
    }

    static TableSegments collect(List<Page> pages) {
        TableSegments tableSegments = new TableSegments();
        for (Page page : pages) {
            tableSegments.recordPage(page.controls());
            tableSegments.collectControls(page.controls());
        }
        return tableSegments;
    }

    /**
     * 표를 이 조각 자리에 쓰는가 — 표는 한 번만, 글자가 있는 첫 조각 자리에 쓴다. 쪽 끝에 머리 테두리만 걸치고
     * 내용은 다음 쪽부터 나오는 표는 다음 쪽에 써야 쪽 번호가 맞는다.
     */
    boolean isWrittenAt(TableItem item) {
        List<TableItem> list = segments.get(item.table());
        return list == null || writtenSegmentOf(list) == item;
    }

    private static TableItem writtenSegmentOf(List<TableItem> list) {
        for (TableItem segment : list) {
            if (hasContent(segment)) return segment;
        }
        return list.get(0);
    }

    /** 이 조각의 셀에 글자가 있는가 (안쪽 표·글상자·묶음 안까지). 그림·도형만 있으면 없다. */
    private static boolean hasContent(TableItem segment) {
        for (TableItem.CellLayout cell : segment.cells()) {
            if (hasText(cell.cellDocument())) return true;
        }
        return false;
    }

    private static boolean hasText(Document document) {
        if (document == null) return false;
        for (Page page : document.pages()) {
            for (TextRow row : page.content().rows()) {
                for (TextColumn column : row.columns()) {
                    for (TextLine line : column.textLines()) {
                        if (LineText.hasVisibleText(line)) return true;
                    }
                }
            }
            for (ControlItem control : page.controls()) {
                if (hasText(control)) return true;
            }
        }
        return false;
    }

    private static boolean hasText(ControlItem control) {
        if (control instanceof TableItem) return hasContent((TableItem) control);
        if (control instanceof GsoTextBoxItem) return hasText(((GsoTextBoxItem) control).innerDocument());
        if (control instanceof GsoContainerItem) {
            for (ControlItem child : ((GsoContainerItem) control).children()) {
                if (hasText(child)) return true;
            }
        }
        return false;
    }

    /** 본문 쪽에 바로 놓인 조각이면 그 쪽의 컨트롤들, 셀·글상자 안 조각이면 null. */
    List<ControlItem> pageControlsOf(TableItem segment) {
        return pageControls.get(segment);
    }

    private void recordPage(List<ControlItem> controls) {
        for (ControlItem control : controls) {
            if (control instanceof TableItem) pageControls.put((TableItem) control, controls);
        }
    }

    /** item이 속한 표의 모든 조각 (쪽 순서). */
    List<TableItem> segmentsOf(TableItem item) {
        List<TableItem> list = segments.get(item.table());
        return list != null ? list : Collections.singletonList(item);
    }

    private void collectControls(List<ControlItem> controls) {
        for (ControlItem control : controls) {
            if (control instanceof TableItem) {
                collect((TableItem) control);
            } else if (control instanceof GsoTextBoxItem) {
                collect(((GsoTextBoxItem) control).innerDocument());
            } else if (control instanceof GsoContainerItem) {
                collectControls(((GsoContainerItem) control).children());
            }
        }
    }

    private void collect(TableItem item) {
        List<TableItem> list = segments.computeIfAbsent(item.table(), table -> new ArrayList<>());
        if (containsSame(list, item)) return;
        list.add(item);
        for (TableItem.CellLayout cell : item.cells()) {
            collect(cell.cellDocument());
        }
    }

    private void collect(Document document) {
        if (document == null) return;
        for (Page page : document.pages()) {
            collectControls(page.controls());
        }
    }

    private static boolean containsSame(List<TableItem> list, TableItem item) {
        for (TableItem each : list) {
            if (each == item) return true;
        }
        return false;
    }
}
