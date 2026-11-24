package kr.dogfoot.hwpdrawer.document.text;

import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.List;

/**
 * 텍스트 열 (단). TextLine 목록을 포함한다.
 */
public class TextColumn {
    private Area area = new Area();
    private final List<TextLine> textLines = new ArrayList<>();

    public Area area() { return area; }

    public List<TextLine> textLines() { return textLines; }

    public TextLine addTextLine() {
        TextLine line = new TextLine();
        textLines.add(line);
        return line;
    }
}
