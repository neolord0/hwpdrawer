package kr.dogfoot.hwplib.drawer.drawinginfo;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

public class ParagraphListInfo {
    private DrawingInfo info;

    private Paragraph paragraph;
    private boolean isBodyText;
    private ParaShape paraShape;
    private long paragraphStartY;
    private Area paragraphArea;

    private int charIndex;
    private int charPosition;
    private HWPChar character;

    private int charShapeIndex;
    private CharShape charShape;

    public int savedCharIndex;
    public int savedCharPosition;


    public ParagraphListInfo(DrawingInfo info, boolean isBodyText) {
        this.info = info;
        this.isBodyText = isBodyText;
        paragraphStartY = 0;
    }

    public void startParagraph(Paragraph paragraph) {
        this.paragraph = paragraph;
        this.isBodyText = isBodyText;
        setParaShape();
        setParagraphArea();

        charIndex = 0;
        charPosition = 0;
        character = null;

        charShapeIndex = -1;
        setCharShape();
    }

    private void setParaShape() {
        paraShape = info.hwpFile().getDocInfo().getParaShapeList()
                .get(paragraph().getHeader().getParaShapeId());
    }

    private void setParagraphArea() {
        if (isBodyText) {
            paragraphArea = new Area(info.pageArea())
                    .applyMargin(paraShape().getLeftMargin(),
                            paraShape().getTopParaSpace() + paragraphStartY,
                            paraShape().getRightMargin(),
                            0);
        } else {
            // todo : control paragraph
        }
    }

    private void setCharShape() {
        int newCharShapeIndex = charShapeIndex();
        if (newCharShapeIndex != charShapeIndex) {
            charShapeIndex = newCharShapeIndex;
            charShape = info.hwpFile().getDocInfo().getCharShapeList().get(charShapeIndex);
        }
    }

    private int charShapeIndex() {
        int charShapeIndex = (int) paragraph().getCharShape().getPositonShapeIdPairList().get(0).getShapeId();
        for (CharPositionShapeIdPair cpsip : paragraph().getCharShape().getPositonShapeIdPairList()) {
            if (charPosition >= cpsip.getPosition()) {
                charShapeIndex = (int) cpsip.getShapeId();
            } else {
                break;
            }
        }
        return charShapeIndex;
    }

    public void endParagraph(long paragraphHeight) {
        paragraphStartY += paragraphHeight + paraShape().getBottomParaSpace();
    }

    public Paragraph paragraph() {
        return paragraph;
    }

    public boolean isBodyText() {
        return isBodyText;
    }

    public ParaShape paraShape() {
        return paraShape;
    }

    public Area paragraphArea() {
        return paragraphArea;
    }

    public long paragraphStartY() {
        return paragraphStartY;
    }

    public void resetParagraphStartY() {
        paragraphStartY = 0;
        setParagraphArea();
    }

    public CharShape charShape() {
        return charShape;
    }

    public boolean nextChar() {
        if (paragraph.getText() != null &&
                charIndex < paragraph().getText().getCharList().size()) {
            character = paragraph().getText().getCharList().get(charIndex);
            charIndex++;
            setCharShape();
            charPosition += character.getCharSize();
            return true;
        } else {
            return false;
        }
    }

    public HWPChar character() {
        return character;
    }

    public boolean isLastChar() {
        return charIndex >= paragraph().getText().getCharList().size();
    }

    public void saveCharPosition() {
        savedCharIndex = charIndex;
        savedCharPosition = charPosition;
    }

    public void rollbackCharPosition() {
        charIndex = savedCharIndex;
        charPosition = savedCharPosition;
        setCharShape();
    }

    public void addXX(int size) {
        rollbackCharPosition();
        for (int index = 0; index < size; index++) {
            nextChar();
        }
    }

    public boolean beforeChar() {
        if (paragraph.getText() != null &&
                charIndex - 1 >= 0) {
            charPosition -= character.getCharSize();
            setCharShape();
            charIndex--;
            character = paragraph().getText().getCharList().get(charIndex);
            return true;
        } else {
            return false;
        }
    }
}
