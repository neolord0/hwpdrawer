package kr.dogfoot.hwplib.drawer.interimoutput.text;

import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;
import kr.dogfoot.hwplib.object.docinfo.parashape.Alignment;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

public class TextPart {
    public static final TextPart[] Zero_Array = new TextPart[0];

    private TextLine textLine;
    private long startX;
    private long width;

    private final ArrayList<CharInfo> charInfos;
    private int spaceCount;
    private double spaceRate;

    public TextPart(TextLine textLine, long startX, long width) {
        this.textLine = textLine;
        this.startX = startX;
        this.width = width;

        charInfos = new ArrayList<>();
        spaceCount = 0;
        spaceRate = 1.0;
    }

    public Area textLineArea() {
        return textLine.area();
    }

    public Alignment alignment() {
        return textLine.alignment();
    }

    public boolean lastLine() {
        return textLine.lastInPara();
    }

    public long maxCharHeight() {
        return textLine.maxCharHeight();
    }

    public long startX() {
        return startX;
    }

    public long width() {
        return width;
    }

    public long endX() {
        return startX + width;
    }

    public ArrayList<CharInfo> charInfos() {
        return charInfos;
    }

    public void addCharInfo(CharInfo charInfo) {
        charInfos.add(charInfo);
        if (charInfo.character().isSpace()) {
            spaceCount++;
        }
        if (charInfo.type() == CharInfo.Type.Normal
                || (charInfo.type() == CharInfo.Type.Control && ((ControlCharInfo) charInfo).isLikeLetter())) {
            textLine.hasDrawingChar(true);
        }
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

    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();

        sb.tab(tabCount).append("{ ").append(Long.toString(startX)).append(",").append(Long.toString(width)).append(" : ");
        for (CharInfo charInfo : charInfos) {
            if (charInfo.type() == CharInfo.Type.Normal) {
                NormalCharInfo normalCharInfo = (NormalCharInfo) charInfo;
                try {
                    sb
                            .append(normalCharInfo.normalCharacter().getCh());
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            } else {
                ControlCharInfo controlCharInfo = (ControlCharInfo) charInfo;
                if (controlCharInfo.control() == null) {
                    sb
                            .append("[")
                            .append(Integer.toString(controlCharInfo.character().getCode()))
                            .append("]");

                } else {
                    sb
                            .append("[")
                            .append(controlCharInfo.control().getType().toString())
                            .append("]");
                }

            }
        }
        sb.append(" }(").append(Integer.toString(charInfos.size())).append(")");
        return sb.toString();
    }
}
