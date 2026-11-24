package kr.dogfoot.hwpdrawer.document;

import kr.dogfoot.hwpdrawer.document.text.TextRow;

import java.util.ArrayList;
import java.util.List;

/**
 * 콘텐츠 컨테이너.
 * TextRow 목록을 포함한다. 본문, 머리글, 꼬리글, 글상자, 셀 등에서 사용.
 */
public class Content {
    private final List<TextRow> rows = new ArrayList<>();

    public List<TextRow> rows() {
        return rows;
    }

    public TextRow addRow() {
        TextRow row = new TextRow();
        rows.add(row);
        return row;
    }
}
