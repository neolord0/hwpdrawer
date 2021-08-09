package kr.dogfoot.hwplib.drawer.input.paralist;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
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

    private ParagraphListInfoSort sort;

    private ParaShape paraShape;

    private long height;

    private long paraStartY;
    private Area paraArea;

    private int charIndex;
    private int charPosition;
    private HWPChar character;

    private int charShapeIndex;
    private CharShape charShape;

    private boolean ignoreNextPara;

    private CellInfo cellInfo;

    public ParagraphListInfo(DrawingInput input, Paragraph[] paras) {
        this.input = input;
        columnsInfo = new ColumnsInfo(input.pageInfo());

        height = 0;
        paraArea = new Area();

        this.paras = paras;
        this.paraIndex = 0;
        ignoreNextPara = false;

        cellInfo = null;
    }

    public ParagraphListInfo forBodyText() {
        sort = ParagraphListInfoSort.ForBody;
        return this;
    }

    public ParagraphListInfo forControl(Area textBoxArea) {
        columnsInfo.set(null, textBoxArea);
        textBoxArea(textBoxArea);

        sort = ParagraphListInfoSort.ForControl;
        return this;
    }

    public ParagraphListInfo forCell(Area textBoxArea, boolean canSplit, long topInPage, long bottomMargin) {
        columnsInfo.set(null, textBoxArea);
        textBoxArea(textBoxArea);

        this.cellInfo = new CellInfo(canSplit, topInPage, bottomMargin);

        sort = ParagraphListInfoSort.ForCell;
        return this;
    }

    public void textBoxArea(Area textBoxArea) {
        this.textBoxArea = textBoxArea;
        paraStartY = 0;
        if (paraShape != null) {
            setParaArea();
        }
    }

    public boolean nextPara() {
        if (ignoreNextPara) {
            ignoreNextPara = false;
            return true;
        } else {
            if (paraIndex < paras.length) {
                currentPara = paras[paraIndex];
                paraIndex++;
                return true;
            } else {
                return false;
            }
        }
    }

    public void ignoreNextPara() {
        ignoreNextPara = true;
    }

    public boolean gotoPara(TextPosition position) {
        if (position.paraIndex() < paras.length) {
            currentPara = paras[position.paraIndex()];
            this.paraIndex = position.paraIndex() + 1;
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
        return sort == ParagraphListInfoSort.ForBody;
    }

    public boolean isCellText() {
        return sort == ParagraphListInfoSort.ForCell;
    }

    public CellInfo cellInfo() {
        return cellInfo;
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

    public void gotoChar(TextPosition position) {
        charIndex = position.charIndex();
        if (charIndex == 0) {
            character = null;
            charShapeIndex = -1;
            setCharShape();
        } else {
            character = currentPara().getText().getCharList().get(charIndex - 1);
            this.charPosition = position.charPosition() - character.getCharSize();
            setCharShape();
            this.charPosition += character.getCharSize();
        }
    }

    public ColumnsInfo columnsInfo() {
        return columnsInfo;
    }

    public void setColumnInfoWithPreviousColumnDefine(Area textBoxArea) {
        columnsInfo.setWithPreviousColumnDefine(textBoxArea);
        textBoxArea(columnsInfo.currentColumnArea());
    }

    public void setColumnInfo(ControlColumnDefine columnDefine, Area textBoxArea) {
        columnsInfo.set(columnDefine, textBoxArea);
        textBoxArea(columnsInfo.currentColumnArea());
    }

    public void setColumnInfoWithPreviousInfo() {
        columnsInfo.setWithPreviousInfo();
        textBoxArea(columnsInfo.currentColumnArea());
    }

    public void nextColumn() {
        columnsInfo.nextColumn();
        textBoxArea(columnsInfo.currentColumnArea());
    }

    public void previousColumn() {
        columnsInfo.previousColumn();
        textBoxArea(columnsInfo.currentColumnArea());
    }

    public void gotoColumn(int columnIndex) {
        columnsInfo.currentColumnIndex(columnIndex);
        textBoxArea(columnsInfo.currentColumnArea());
    }

    public static class CellInfo {
        private boolean canSplit;
        private long topInPage;
        private long bottomMargin;

        public CellInfo(boolean canSplit, long topInPage, long bottomMargin) {
            this.canSplit = canSplit;
            this.topInPage = topInPage;
            this.bottomMargin = bottomMargin;
        }

        public boolean canSplit() {
            return canSplit;
        }

        public long topInPage() {
            return topInPage;
        }

        public long bottomMargin() {
            return bottomMargin;
        }
    }


    private  enum ParagraphListInfoSort {
        ForBody,
        ForControl,
        ForCell
    }
}
