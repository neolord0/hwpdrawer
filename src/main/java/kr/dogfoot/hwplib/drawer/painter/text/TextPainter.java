package kr.dogfoot.hwplib.drawer.painter.text;

import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.interimoutput.text.TextPart;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.NormalCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

public class TextPainter {
    private final Painter painter;

    private long baseLine;
    private CharShape drawingCharShape;
    private long charX;
    private long[] spaceAddings;
    private long[] charAddings;

    private final UnderLinePainter underLinePainter;
    private final StrikeLinePainter strikeLinePainter;

    public TextPainter(Painter painter) {
        this.painter = painter;

        underLinePainter = new UnderLinePainter(painter);
        strikeLinePainter = new StrikeLinePainter(painter);
    }

    public void paintTextLines(TextLine[] lines) throws Exception {
        for (TextLine line : lines) {
            paintTextLine(line);
        }
    }

    private void paintTextLine(TextLine line) throws Exception {
        for (TextPart part : line) {
            paintTextPart(part);
        }
    }

    private void paintTextPart(TextPart part) throws Exception {
        baseLine = part.textLineArea().top() + part.maxCharHeight();
        drawingCharShape = null;

        switch (part.alignment()) {
            case Justify:
                justify(part);
                break;
            case Left:
                left(part);
                break;
            case Right:
                right(part);
                break;
            case Center:
                center(part);
                break;
            case Distribute:
                distribute(part);
                break;
            case Divide:
                divide(part);
                break;
        }
        paintUnder_StrikeLine(part);
    }

    private void justify(TextPart part) throws Exception {
        charX = part.textLineArea().left() + part.startX();
        if (!part.lastLine()) {
            if (part.spaceCountWithExceptingLastSpace() != 0) {
                spaceAddings = spaceAddings(part);
                charAddings = null;
            } else {
                spaceAddings = null;
                charAddings = charAddings(part);
            }
        } else {
            spaceAddings = null;
            charAddings = null;
        }
        paintInOrder(part);
    }

    private long[] spaceAddings(TextPart part) {
        long extra = part.width() - part.textWidthWithExceptingLastSpace();
        int spaceCount = part.spaceCountWithExceptingLastSpace();
        if (spaceCount == 0) {
            return null;
        }
        long extraBySpace = extra / spaceCount;
        long rest = extra % spaceCount;

        long[] spaceAddings = null;
        if (spaceCount > 0) {
            spaceAddings = new long[spaceCount];
            for (int index = 0; index < spaceCount; index++) {
                if (index <= rest) {
                    spaceAddings[index] = extraBySpace + 1;
                } else {
                    spaceAddings[index] = extraBySpace;
                }
            }
        }
        return spaceAddings;
    }

    private long[] charAddings(TextPart part) {
        long extra = part.width() - part.textWidthWithExceptingLastSpace();
        int charCount = part.charCountWithExceptingLastSpace() - 1;
        if (charCount <= 0) {
            return null;
        }
        long extraByChar = extra / charCount;
        long rest = extra % charCount;
        long[] charAddings = new long[charCount];
        for (int index = 0; index < charCount; index++) {
            if (index <= rest) {
                charAddings[index] = extraByChar + 1;
            } else {
                charAddings[index] = extraByChar;
            }
        }
        return charAddings;
    }

    private void left(TextPart part) throws Exception {
        charX = part.textLineArea().left() + part.startX();
        spaceAddings = null;
        charAddings = null;
        paintInOrder(part);
    }

    private void right(TextPart part) throws Exception {
        charX = part.textLineArea().left() + part.endX() - part.textWidthWithExceptingLastSpace();
        spaceAddings = null;
        charAddings = null;
        paintInOrder(part);
    }

    private void center(TextPart part) throws Exception {
        charX = part.textLineArea().left() + part.startX() + (part.width() - part.textWidthWithExceptingLastSpace()) / 2;
        spaceAddings = null;
        charAddings = null;
        paintInOrder(part);
    }

    private void distribute(TextPart part) throws Exception {
        charX = part.textLineArea().left() + part.startX();
        spaceAddings = null;
        charAddings = charAddings(part);
        paintInOrder(part);
    }

    private void divide(TextPart part) throws Exception {
        charX = part.textLineArea().left() + part.startX();
        spaceAddings = spaceAddings(part);
        charAddings = null;
        paintInOrder(part);
    }

    private void paintInOrder(TextPart part) throws Exception {
        short oldRatio = 100;
        double stretchRate = 1;

        int spaceIndex = 0;
        int charIndex = 0;
        for (CharInfo charInfo : part.charInfos()) {
            if (drawingCharShape != charInfo.charShape()) {
                painter.setDrawingFont(charInfo.charShape());
                drawingCharShape = charInfo.charShape();
            }

            if (oldRatio != charInfo.charShape().getRatios().getHangul()) {
                oldRatio = charInfo.charShape().getRatios().getHangul();
                stretchRate = painter.setStretch(oldRatio);
            }

            charInfo.x(charX);
            if (charInfo.character().isSpace()) {
                charX += charInfo.widthAddingCharSpace() * part.spaceRate();
                if (spaceAddings != null && spaceIndex < spaceAddings.length) {
                    charX += spaceAddings[spaceIndex];
                    spaceIndex++;
                }
            } else {
                if (charInfo.type() == CharInfo.Type.Normal) {
                    painter.string(((NormalCharInfo) charInfo).normalCharacter().getCh(),
                            (long) (charInfo.x() / stretchRate),
                            getY(charInfo));
                } else if (charInfo.type() == CharInfo.Type.Control
                        && ((ControlCharInfo) charInfo).control() != null
                        && ((ControlCharInfo) charInfo).isLikeLetter()) {
                    ControlCharInfo controlCharInfo = (ControlCharInfo) charInfo;
                    Area area = controlArea(part, controlCharInfo);
                    // todo table
                    if (controlCharInfo.output() != null) {
                        controlCharInfo.output().controlArea(area);
                        painter.controlPainter().paintControl(controlCharInfo.output());
                    }
                }

                charX += charInfo.widthAddingCharSpace();
            }

            if (charAddings != null && charIndex < charAddings.length) {
                charX += charAddings[charIndex];
                charIndex++;
            }
        }
    }

    private Area controlArea(TextPart part, ControlCharInfo controlCharInfo) {
        return controlCharInfo.areaWithoutOuterMargin().widthHeight()
                .move(controlCharInfo.x() - (controlCharInfo.areaWithOuterMargin().left() - controlCharInfo.areaWithoutOuterMargin().left()),
                        part.textLineArea().bottom() - controlCharInfo.areaWithoutOuterMargin().height() - (controlCharInfo.areaWithOuterMargin().bottom() - controlCharInfo.areaWithoutOuterMargin().bottom()));
    }

    private long getY(CharInfo charInfo) {
        return (long) (baseLine
                - painter.textOffsetY(((NormalCharInfo) charInfo)))
                + charInfo.height() * charInfo.charShape().getCharOffsets().getHangul() / 100;
    }

    private void paintUnder_StrikeLine(TextPart part) {
        underLinePainter.initialize(baseLine, part.maxCharHeight());
        strikeLinePainter.initialize(baseLine);

        int count = part.charInfos().size();
        for (int index = 0; index < count; index++) {
            CharInfo charInfo = part.charInfos().get(index);

            underLinePainter.paint(charInfo, (index == count - 1));
            strikeLinePainter.paint(charInfo, (index == count - 1));
        }
    }
}
