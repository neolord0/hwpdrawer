package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ParagraphDrawer {
    private DrawingInfo info;

    private Area paraArea;
    private LineDrawer lineDrawer;

    public ParagraphDrawer(DrawingInfo info) {
        this.info = info;
        lineDrawer = new LineDrawer();
    }

    public void draw(Paragraph paragraph) throws Exception {
        info.startParagraph(paragraph);

        paraArea = info.paragraphDrawArea();
        lineDrawer.start(paraArea.left(), paraArea.top());

        while (info.nextChar() == true) {
            switch (info.currentChar().getType()) {
                case Normal:
                    normalChar();
                    break;
                case ControlChar:
                case ControlExtend:
                case ControlInline:
            }
        }

        lineDrawer.draw(info.graphics(), true);

        info.endParagraph();
    }

    private void normalChar() throws IOException {
        HWPCharNormal chn = (HWPCharNormal) info.currentChar();
        double charWidth = charWidth(chn);

        if (chn.getCode() != 32/*Space*/ && isNewLine(charWidth)) {
            if (isNewPage()) {
                info.pageMaker().newPage();

                paraArea = info.paragraphDrawArea();
                lineDrawer.start(paraArea.left(), paraArea.top());
            }

            lineDrawer.draw(info.graphics(), info.isLastChar());
            lineDrawer.start(paraArea.left(), nextLineY());
        }

        lineDrawer.addChar(chn, charWidth, info.currentCharShape());
     }

    private boolean isNewPage() {
        return nextLineY() > paraArea.bottom();
    }

    private long nextLineY() {
        return lineDrawer.y() + (lineDrawer.maxCharHeight() * info.currentParaShape().getLineSpace() / 100);
    }

    private boolean isNewLine(double charWidth) {
        return lineDrawer.currentX() + charWidth > paraArea.right();
    }

    private double charWidth(HWPCharNormal chn) throws UnsupportedEncodingException {
        if (chn.getCode() == 32/*Space*/) {
            return info.currentCharShape().getBaseSize() / 2;
        } else {
            info.graphics().setFont(FontManager.object().calculatingFont(info.currentCharShape()));
            return info.graphics().getFontMetrics().stringWidth(chn.getCh());
        }
    }
}
