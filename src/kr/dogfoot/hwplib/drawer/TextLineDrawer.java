package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TextLineDrawer {
    private DrawingInfo info;

    private long y;
    private long currentX;

    private long maxCharHeight;

    private CharShape drawingCharShape;
    private ArrayList<CharDrawInfo> charDrawInfos;
    private ArrayList<CharDrawInfo> lastWordPartChar;

    private UnderLineDrawer underLineDrawer;
    private StrikeLineDrawer strikeLineDrawer;

    public TextLineDrawer(DrawingInfo info) {
        this.info = info;

        maxCharHeight = 0;
        charDrawInfos = new ArrayList<>();
        lastWordPartChar = new ArrayList<>();
        underLineDrawer = new UnderLineDrawer(info);
        strikeLineDrawer = new StrikeLineDrawer(info);
    }

    public void start(long startX, long y) {
        maxCharHeight = 0;
        this.currentX = startX;
        this.y = y;
        charDrawInfos.clear();
        addLastWordPart();
    }

    private void addLastWordPart() {
        for(CharDrawInfo cdi : lastWordPartChar) {
            addChar(cdi.charNormal, cdi.width,  cdi.charShape);
        }
        lastWordPartChar.clear();
    }

    public void addChar(HWPCharNormal charNormal, double width, CharShape charShape) {
        maxCharHeight = (charShape.getBaseSize() > maxCharHeight) ? charShape.getBaseSize() : maxCharHeight;
        charDrawInfos.add(new CharDrawInfo(charNormal, currentX, width, charShape));
        currentX += width + width * charShape.getCharSpaces().getHangul() / 100;
     }

    public long currentX() {
        return currentX;
    }

    public long y() {
        return y;
    }

    public long maxCharHeight() {
        return maxCharHeight;
    }

    public void draw(boolean lastLine) throws UnsupportedEncodingException {
        if (lastLine == false) {
            splitLastWordPart();
        }

        drawingCharShape = null;
        long baseLine = y + maxCharHeight;

        underLineDrawer.initialize(baseLine, maxCharHeight);
        strikeLineDrawer.initialize(baseLine);

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

            long y = baseLine + cdi.charShape.getBaseSize() * cdi.charShape.getCharOffsets().getHangul() / 100;

            info.painter().drawString(cdi.charNormal.getCh(),
                    (long) (cdi.x / stretchRate),
                    y);
        }

        int count = charDrawInfos.size();
        for (int index = 0; index < count; index++) {
            CharDrawInfo cdi = charDrawInfos.get(index);

            underLineDrawer.draw(cdi, (index == count - 1));
            strikeLineDrawer.draw(cdi, (index == count - 1));
        }
    }


    private void splitLastWordPart() {
        int lastSpaceIndex = getLastSpaceIndex();
        if (lastSpaceIndex != -1) {
            int count = charDrawInfos.size();
            for (int index = lastSpaceIndex + 1; index < count; index++) {
                CharDrawInfo cdi = charDrawInfos.remove(lastSpaceIndex + 1);
                lastWordPartChar.add(cdi);
            }
       }
    }

    private int getLastSpaceIndex() {
        for(int index = charDrawInfos.size() - 1; index >= 0; index--) {
            CharDrawInfo cdi = charDrawInfos.get(index);
            if (cdi.charNormal.getCode() == 32) {
                return index;
            }
        }
        return -1;
    }

    public static class CharDrawInfo {
        public HWPCharNormal charNormal;
        public long x;
        public double width;
        public CharShape charShape;

        public CharDrawInfo(HWPCharNormal charNormal, long x, double width, CharShape charShape) {
            this.charNormal = charNormal;
            this.x = x;
            this.width = width;
            this.charShape = charShape;
        }
    }
}
