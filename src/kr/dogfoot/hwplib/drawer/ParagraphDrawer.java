package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ParagraphDrawer {
    private DrawingInfo info;

    private Area paraArea;
    private TextLineDrawer textLineDrawer;

    public ParagraphDrawer(DrawingInfo info) {
        this.info = info;
        textLineDrawer = new TextLineDrawer(info);
    }

    public void draw(Paragraph paragraph) throws Exception {
        info.startParagraph(paragraph);

        paraArea = info.paragraphDrawArea();
        textLineDrawer.start(paraArea.left(), paraArea.top());

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

        textLineDrawer.draw(true);

        info.endParagraph();
    }

    private void normalChar() throws IOException {
        HWPCharNormal chn = (HWPCharNormal) info.currentChar();
        double charWidth = charWidth(chn, info.currentCharShape());

        if (chn.getCode() != 32/*Space*/ && isNewLine(charWidth)) {
            if (isNewPage()) {
                info.pageMaker().newPage();

                paraArea = info.paragraphDrawArea();
                textLineDrawer.start(paraArea.left(), paraArea.top());
            }

            textLineDrawer.draw(info.isLastChar());
            textLineDrawer.start(paraArea.left(), nextLineY());
        }

        textLineDrawer.addChar(chn, charWidth, info.currentCharShape());
     }

    private boolean isNewPage() {
        return nextLineY() > paraArea.bottom();
    }

    private long nextLineY() {
        return textLineDrawer.y() + (textLineDrawer.maxCharHeight() * info.currentParaShape().getLineSpace() / 100);
    }

    private boolean isNewLine(double charWidth) {
        return textLineDrawer.currentX() + charWidth > paraArea.right();
    }

    private double charWidth(HWPCharNormal chn, CharShape charShape) throws UnsupportedEncodingException {
        double charWidth;
        if (chn.getCode() == 32/*Space*/) {
            charWidth = charShape.getBaseSize() / 2;
        } else {
            charWidth = info.painter().getCharWidth(chn.getCh(), charShape);
        }
        charWidth = charWidth * charShape.getRelativeSizes().getHangul() / 100;
        charWidth = charWidth * charShape.getRatios().getHangul() / 100;
        return charWidth;
    }
}
