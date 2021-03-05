package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;

public class ParagraphDrawer {
    private DrawingInfo info;

    public ParagraphDrawer(DrawingInfo info) {
        this.info = info;
    }

    public void draw(Paragraph paragraph) {
        info.startCurrentParagraph(paragraph);

        info.endCurrentParagraph();
    }
}
