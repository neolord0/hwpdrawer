package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

public class ParagraphListInfo {
    private final DrawingInput input;
    private Area bodyArea;

    private Paragraph currentPara;
    private Paragraph[] paras;
    private int paraIndex;

    private boolean isBodyText;
    private ParaShape paraShape;

    private long height;

    private long paraStartY;
    private Area paraArea;

    private int charIndex;
    private int charPosition;
    private HWPChar character;

    private int charShapeIndex;
    private CharShape charShape;

    public ParagraphListInfo(DrawingInput input) {
        this(input, new Area(input.pageInfo().columnArea()));
    }

    public ParagraphListInfo(DrawingInput input, Area bodyArea) {
        this.input = input;

        height = 0;

        bodyArea(bodyArea);
    }

    public void bodyArea(Area bodyArea) {
        paraStartY = 0;
        this.bodyArea = bodyArea;
        if (paraShape != null) {
            setParaArea();
        }
    }

    public ParagraphListInfo bodyText(boolean bodyText) {
        this.isBodyText = bodyText;
        return this;
    }

    public boolean bodyText() {
        return isBodyText;
    }

    public ParagraphListInfo paras(Paragraph[] paras) {
        this.paras = paras;
        this.paraIndex = 0;
        return this;
    }

    public boolean nextPara() {
        if (paraIndex < paras.length) {
            currentPara = paras[paraIndex];
            paraIndex++;
            return true;
        } else {
            return false;
        }
    }

    public boolean gotoPara(int paraIndex) {
        if (paraIndex < paras.length) {
            currentPara = paras[paraIndex];
            this.paraIndex = paraIndex + 1;
            paraIndex++;
            return true;
        } else {
            return false;
        }
    }

    public int paraIndex() {
        return paraIndex - 1;
    }

    public Paragraph currentPara() {
        return currentPara;
    }

    public void startPara() {
        setParaShape();
        setParaArea();

        charIndex = 0;
        charPosition = 0;
        character = null;

        charShapeIndex = -1;
        setCharShape();
    }

    private void setParaShape() {
        paraShape = input.hwpFile().getDocInfo().getParaShapeList()
                .get(currentPara().getHeader().getParaShapeId());
    }

    private void setParaArea() {
        paraArea = new Area(bodyArea)
                .applyMargin(paraShape.getLeftMargin() / 2,
                        paraShape.getTopParaSpace() / 2,
                        paraShape.getRightMargin() / 2,
                        0);

        paraArea.top(paraArea.top() + paraStartY);
    }

    private void setCharShape() {
        int newCharShapeIndex = charShapeIndex();
        if (newCharShapeIndex != charShapeIndex) {
            charShapeIndex = newCharShapeIndex;
            charShape = input.hwpFile().getDocInfo().getCharShapeList().get(charShapeIndex);
        }
    }

    private int charShapeIndex() {
        int charShapeIndex = (int) currentPara().getCharShape().getPositonShapeIdPairList().get(0).getShapeId();
        for (CharPositionShapeIdPair cpsip : currentPara().getCharShape().getPositonShapeIdPairList()) {
            if (charPosition >= cpsip.getPosition()) {
                charShapeIndex = (int) cpsip.getShapeId();
            } else {
                break;
            }
        }
        return charShapeIndex;
    }

    public void endPara(long endY, long height) {
        paraStartY += endY + paraShape().getBottomParaSpace() / 2;
        this.height += height;
    }

    public boolean isBodyText() {
        return isBodyText;
    }

    public ParaShape paraShape() {
        return paraShape;
    }

    public long height() {
        return height;
    }

    public Area paraArea() {
        return paraArea;
    }

    public void resetParaStartY() {
        paraStartY = 0;
        setParaArea();
    }

    public void resetParaStartY(long startY) {
        this.paraStartY = startY - bodyArea.top();
        setParaArea();
    }

    public CharShape charShape() {
        return charShape;
    }

    public boolean nextChar() {
        if (currentPara().getText() != null &&
                charIndex < currentPara().getText().getCharList().size()) {
            character = currentPara().getText().getCharList().get(charIndex);
            charIndex++;
            setCharShape();
            charPosition += character.getCharSize();
            return true;
        } else {
            return false;
        }
    }

    public HWPChar currentChar() {
        return character;
    }

    public boolean beforeChar() {
        if (currentPara().getText() != null &&
                charIndex - 2 >= 0) {
            charPosition -= character.getCharSize();
            charIndex--;
            character = currentPara().getText().getCharList().get(charIndex - 1);

            charPosition -= character.getCharSize();
            setCharShape();
            charPosition += character.getCharSize();
            return true;
        } else {
            return false;
        }
    }

    public int charIndex() {
        return charIndex - 1;
    }

    public int charPosition() {
        return charPosition;
    }

    public void gotoChar(int charIndex, int charPosition) {
        this.charIndex = charIndex;
        if (charIndex == 0) {
            character = null;
            charShapeIndex = -1;
            setCharShape();
        } else {
            character = currentPara().getText().getCharList().get(charIndex - 1);
            this.charPosition = charPosition - character.getCharSize();
            setCharShape();
            this.charPosition += character.getCharSize();
        }
    }

}
