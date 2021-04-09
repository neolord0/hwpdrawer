package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;

import java.util.ArrayList;

public class TextPart {
    private ArrayList<CharInfo> charInfos;
    private Area area;
    private boolean lastLine;
    private int spaceCount;
    private double spaceRate;
    private boolean hasNormalChar;

    public TextPart(Area area) {
        charInfos = new ArrayList<>();
        this.area = area;

        lastLine = false;
        spaceCount = 0;
        spaceRate = 1.0;
        hasNormalChar = false;
    }

    public ArrayList<CharInfo> charInfos() {
        return charInfos;
    }

    public void addCharInfo(CharInfo charInfo) {
        charInfos.add(charInfo);
        if (charInfo.character().isSpace()) {
            spaceCount++;
        }
        if (charInfo.type() == CharInfo.Type.Normal) {
            hasNormalChar = true;
        }
    }

    public CharInfo lastChar() {
        int count = charInfos.size();
        return (count == 0) ? null : charInfos.get(count - 1);
    }

    public boolean lastLine() {
        return lastLine;
    }

    public void lastLine(boolean lastLine) {
        this.lastLine = lastLine;
    }

    public int spaceCount() {
        return spaceCount;
    }

    public int spaceCountWithExceptingLastSpace() {
        if (charInfos.size() > 0) {
            CharInfo lastCharInfo = charInfos.get(charInfos.size() - 1);
            if (lastCharInfo.character().isSpace()) {
                return spaceCount - 1;
            }
        }
        return spaceCount;
    }

    public Area area() {
        return area;
    }

    public void area(Area textLineArea) {
        this.area = textLineArea;
    }

    public double spaceRate() {
        return spaceRate;
    }

    public void spaceRate(double spaceRate) {
        this.spaceRate = spaceRate;
    }

    public long textWidthWithExceptingLastSpace() {
        long width = 0;
        int count = charInfos.size();
        for (int index = 0; index < count; index++) {
            CharInfo charInfo = charInfos.get(index);
            if (charInfo.character().isSpace()) {
                if (index < count - 1) {
                    width += charInfo.widthAddingCharSpace() * spaceRate;
                }
            } else {
                width += charInfo.widthAddingCharSpace();
            }
        }
        return width;
    }

    public int charCountWithExceptingLastSpace() {
        int textCount = charInfos.size();
        if (textCount > 0) {
            CharInfo lastCharInfo = charInfos.get(textCount - 1);
            if (lastCharInfo.character().isSpace()) {
                return textCount - 1;
            }
        }
        return textCount;
    }

    public boolean hasNormalChar() {
        return hasNormalChar;
    }
}
