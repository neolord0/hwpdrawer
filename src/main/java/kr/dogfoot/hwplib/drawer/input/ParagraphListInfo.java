package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

public class ParagraphListInfo {
    private final DrawingInput input;
    private Area textBoxArea;
    private ColumnsInfo columnsInfo;

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

    private ParallelMultiColumnInfo parallelMultiColumnInfo;

    public ParagraphListInfo(DrawingInput input, Paragraph[] paras) {
        this.input = input;
        columnsInfo = new ColumnsInfo(input.pageInfo());

        height = 0;
        paraArea = new Area();

        parallelMultiColumnInfo = new ParallelMultiColumnInfo();

        this.paras = paras;
        this.paraIndex = 0;
    }

    public ParagraphListInfo forBodyText() {
        this.isBodyText = true;
        return this;
    }

    public ParagraphListInfo forControl(Area textBoxArea) {
        this.isBodyText = false;
        columnsInfo.set(null, textBoxArea);
        textBoxArea(textBoxArea);
        return this;
    }

    public void textBoxArea(Area textBoxArea) {
        this.textBoxArea = textBoxArea;
        paraStartY = 0;
        if (paraShape != null) {
            setParaArea();
        }
    }

    public boolean bodyText() {
        return isBodyText;
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
        paraArea.set(textBoxArea)
                .applyMargin(paraShape.getLeftMargin() / 2,
                        paraShape.getTopParaSpace() / 2,
                        paraShape.getRightMargin() / 2,
                        0)
                .top(paraArea.top() + paraStartY);
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
        setParaArea();
        return paraArea;
    }

    public void resetParaStartY() {
        paraStartY = 0;
        setParaArea();
    }

    public void resetParaStartY(long startY) {
        this.paraStartY = startY - textBoxArea.top();
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

    public boolean previousChar() {
        if (currentPara().getText() != null &&
                charIndex - 2 >= -1) {
            charPosition -= character.getCharSize();
            charIndex--;
            if (charIndex - 1 >= 0) {
                character = currentPara().getText().getCharList().get(charIndex - 1);
                charPosition -= character.getCharSize();
                setCharShape();
                charPosition += character.getCharSize();
            } else {
                charIndex = 0;
                charPosition = 0;
                character = null;

                charShapeIndex = -1;
                setCharShape();
            }

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

    public ParallelMultiColumnInfo parallelMultiColumnInfo() {
        return parallelMultiColumnInfo;
    }

    public ColumnsInfo columnsInfo() {
        return columnsInfo;
    }

    public void setTextBoxAreaToColumnArea() {
        textBoxArea(columnsInfo().currentColumnArea());
    }
}
