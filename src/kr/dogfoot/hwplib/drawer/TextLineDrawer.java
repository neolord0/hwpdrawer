package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TextLineDrawer {
    private DrawingInfo info;

    private ArrayList<CharDrawInfo> charDrawInfos;

    private long maxCharHeight;
    private long baseLine;
    private long charX;
    private double spaceRate;
    private CharShape drawingCharShape;

    private UnderLineDrawer underLineDrawer;
    private StrikeLineDrawer strikeLineDrawer;

    public TextLineDrawer(DrawingInfo info) {
        this.info = info;

        charDrawInfos = new ArrayList<>();
        underLineDrawer = new UnderLineDrawer(info);
        strikeLineDrawer = new StrikeLineDrawer(info);
    }

    public void initialize() {
        charDrawInfos.clear();

        maxCharHeight = 0;
        baseLine = 0;
        charX = 0;
        spaceRate = 1.0;
        drawingCharShape = null;
    }

    public void addChar(HWPCharNormal ch, double width, CharShape charShape) {
        maxCharHeight = (charShape.getBaseSize() > maxCharHeight) ? charShape.getBaseSize() : maxCharHeight;
        charDrawInfos.add(new CharDrawInfo(ch, width, charShape));
    }

    public long maxCharHeight() {
        return maxCharHeight;
    }

    public void draw(long startX, long startY) throws UnsupportedEncodingException {
        charX = startX;
        baseLine = startY + maxCharHeight;
        drawingCharShape = null;

        drawText();
        drawUnder_StrikeLine();
    }

    private void drawText() throws UnsupportedEncodingException {
        short oldRatio = 100;
        double stretchRate = 1;

        for (CharDrawInfo cdi : charDrawInfos) {
            if (drawingCharShape != cdi.charShape) {
                info.painter().setDrawingFont(cdi.charShape);
                drawingCharShape = cdi.charShape;
            }

            if (oldRatio != cdi.charShape.getRatios().getHangul()) {
                oldRatio = cdi.charShape.getRatios().getHangul();
                stretchRate = info.painter().setStretch(oldRatio);
            }

            cdi.x(charX);
            if (cdi.ch.isSpace()) {
//                System.out.println(cdi.width + " " + spaceRate + " " +((cdi.width + (cdi.width * cdi.charShape.getCharSpaces().getHangul() / 100)) * spaceRate));
                charX += (cdi.width + (cdi.width * cdi.charShape.getCharSpaces().getHangul() / 100)) * spaceRate;
            } else {
                info.painter().string(cdi.ch.getCh(),
                        (long) (cdi.x / stretchRate),
                        getY(cdi));
                charX += cdi.width + (cdi.width * cdi.charShape.getCharSpaces().getHangul() / 100);
            }
        }
    }

    private long getY(CharDrawInfo cdi) {
        return baseLine + cdi.charShape.getBaseSize() * cdi.charShape.getCharOffsets().getHangul() / 100;
    }

    private void drawUnder_StrikeLine() throws UnsupportedEncodingException {
        underLineDrawer.initialize(baseLine, maxCharHeight);
        strikeLineDrawer.initialize(baseLine);

        int count = charDrawInfos.size();
        for (int index = 0; index < count; index++) {
            CharDrawInfo cdi = charDrawInfos.get(index);

            underLineDrawer.draw(cdi, (index == count - 1));
            strikeLineDrawer.draw(cdi, (index == count - 1));
        }
    }

    public boolean noChar() {
        return charDrawInfos.size() == 0;
    }

    public String text() throws UnsupportedEncodingException {
        StringBuffer sb = new StringBuffer();
        for (CharDrawInfo cdi : charDrawInfos) {
            sb.append(cdi.ch.getCh());
        }
        return sb.toString();
    }

    public void spaceRate(double spaceRate) {
        this.spaceRate = spaceRate;
    }

    public static class CharDrawInfo {
        public HWPCharNormal ch;
        public double width;
        public CharShape charShape;
        public long x;

        public CharDrawInfo(HWPCharNormal ch, double width, CharShape charShape) {
            this.ch = ch;
            this.width = width;
            this.charShape = charShape;
        }

        public void x(long x) {
            this.x = x;
        }
    }
}
