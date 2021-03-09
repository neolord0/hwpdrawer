package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.drawer.util.UnitConvertor;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

import java.awt.*;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class LineDrawer {
    private long y;
    private long currentX;

    private long maxCharHeight;

    private CharShape drawingCharShape;
    private ArrayList<CharDrawInfo> charDrawInfos;
    private ArrayList<CharDrawInfo> lastWordPartChar;

    public LineDrawer() {
        maxCharHeight = 0;
        charDrawInfos = new ArrayList<>();
        lastWordPartChar = new ArrayList<>();
    }

    public void start(long startX, long y) {
        maxCharHeight = 0;
        this.currentX = startX;
        this.y = y;
        charDrawInfos.clear();
        addLastWordPart();
    }

    private void addLastWordPart() {
        for(int index = lastWordPartChar.size() - 1; index >= 0; index--) {
            CharDrawInfo cdi = lastWordPartChar.get(index);
            addChar(cdi.charNormal, cdi.width,  cdi.charShape);
        }
        lastWordPartChar.clear();
    }

    public void addChar(HWPCharNormal charNormal, double width, CharShape charShape) {
        maxCharHeight = (charShape.getBaseSize() > maxCharHeight) ? charShape.getBaseSize() : maxCharHeight;
        charDrawInfos.add(new CharDrawInfo(charNormal, currentX, width, charShape));
        currentX += width;
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

    public void draw(Graphics g, boolean lastLine) throws UnsupportedEncodingException {
        if (lastLine == false) {
            splitLastWordPart();
        }

        drawingCharShape = null;
        int baseLine = UnitConvertor.fromHWPUnit(y + maxCharHeight);

        for (CharDrawInfo cdi : charDrawInfos) {
            if (drawingCharShape != cdi.charShape) {
                g.setFont(FontManager.object().drawingFont(cdi.charShape));
                g.setColor(UnitConvertor.color(cdi.charShape.getCharColor()));

                drawingCharShape = cdi.charShape;
            }

            g.drawString(cdi.charNormal.getCh(),
                    UnitConvertor.fromHWPUnit(cdi.x),
                    baseLine);
        }
    }

    private void splitLastWordPart() {
        for(int index = charDrawInfos.size() - 1; index >= 0; index--) {
            CharDrawInfo cdi = charDrawInfos.get(index);
            if (cdi.charNormal.getCode() == 32) {
                break;
            } else {
                lastWordPartChar.add(cdi);
            }
        }
        for(int index = lastWordPartChar.size() - 1; index >= 0; index--) {
            CharDrawInfo cdi = lastWordPartChar.get(index);
            charDrawInfos.remove(cdi);
        }
    }

    private static class CharDrawInfo {
        private HWPCharNormal charNormal;
        private long x;
        private double width;
        private CharShape charShape;

        public CharDrawInfo(HWPCharNormal charNormal, long x, double width, CharShape charShape) {
            this.charNormal = charNormal;
            this.x = x;
            this.width = width;
            this.charShape = charShape;
        }
    }
}
