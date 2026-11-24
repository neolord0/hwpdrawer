package kr.dogfoot.hwpdrawer.document.text;

import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.List;

/**
 * 텍스트 행. 다단인 경우 여러 TextColumn을 포함한다.
 */
public class TextRow {
    private Area area = new Area();
    private final List<TextColumn> columns = new ArrayList<>();

    public Area area() { return area; }

    public List<TextColumn> columns() { return columns; }

    public TextColumn addColumn() {
        TextColumn column = new TextColumn();
        columns.add(column);
        return column;
    }
}
