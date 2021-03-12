package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.charshape.CharPositionShapeIdPair;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

import java.util.Stack;

public class DrawingInfo {
    private HWPFile hwpFile;
    private DrawingOption option;

    private Section section;

    private Stack<Paragraph> paragraphStack;

    private PageMaker pageMaker;

    private ParagraphDrawer paragraphDrawer;

    private int currentCharIndex;
    private int currentCharPosition;
    private HWPChar currentChar;

    private int currentCharShapeIndex;
    private CharShape currentCharShape;

    private Painter painter;

    public DrawingInfo() {
        hwpFile = null;
        option = null;

        section = null;
        paragraphStack = new Stack<>();

        pageMaker = new PageMaker(this);

        paragraphDrawer = new ParagraphDrawer(this);

        painter = new Painter();
    }

    public HWPFile hwpFile() {
        return hwpFile;
    }

    public DrawingInfo hwpFile(HWPFile hwpFile) {
        this.hwpFile = hwpFile;
        return this;
    }

    public DrawingOption option() {
        return option;
    }


    public DrawingInfo option(DrawingOption option) {
        this.option = option;
        Convertor.zoomRate(option.zoomRate());
        return this;
    }

    public Section section() {
        return section;
    }

    public void section(Section section) throws Exception {
        this.section = section;

        setPageDef();
    }

    private void setPageDef() throws Exception {
        Paragraph firstPara = section.getParagraph(0);
        if (firstPara == null) {
            throw new Exception("섹션에는 하나 이상의 문단이 있어야 함.");
        } else {
            if (firstPara.getControlList() == null ||
                    firstPara.getControlList().get(0) == null ||
                    firstPara.getControlList().get(0).getType() != ControlType.SectionDefine) {
                throw new Exception("섹션의 첫 문단의 첫번째 컨트롤은 섹션 정의 컨트롤이어야 함.");
            }
        }

        ControlSectionDefine sectionDefine = (ControlSectionDefine) firstPara.getControlList().get(0);
        pageMaker.pageDef(sectionDefine.getPageDef());
    }

    public void startParagraph(Paragraph paragraph) {
        paragraphStack.push(paragraph);
        currentCharIndex = 0;
        currentCharPosition = 0;
        currentChar = null;

        currentCharShapeIndex = -1;
        currentCharShape = null;
    }

    public void endParagraph() {
        paragraphStack.pop();
    }

    public Paragraph currentParagraph() {
        return paragraphStack.peek();
    }

    public ParaShape currentParaShape() {
        if (currentParagraph() != null) {
            return hwpFile.getDocInfo().getParaShapeList().get(currentParagraph().getHeader().getParaShapeId());
        }
        return null;
    }

    public PageMaker pageMaker() {
        return pageMaker;
    }

    public ParagraphDrawer paragraphDrawer() {
        return paragraphDrawer;
    }

    public Area pageDrawArea() {
        return pageMaker.pageDrawArea();
    }

    public Area paragraphDrawArea() {
        Area area = new Area(pageMaker.pageDrawArea())
                .applyMargin(currentParaShape().getLeftMargin(),
                        currentParaShape().getTopParaSpace() + pageMaker.paragraphStartY(),
                        currentParaShape().getRightMargin(),
                        0);
        return area;
    }

    public boolean nextChar() {
        if (currentParagraph().getText() != null &&
                currentCharIndex < currentParagraph().getText().getCharList().size()) {

            currentChar = currentParagraph().getText().getCharList().get(currentCharIndex);
            currentCharIndex++;
            setCurrentCharShape();
            currentCharPosition += currentChar.getCharSize();
            return true;
        } else {
            return false;
        }
    }

    public boolean isLastChar() {
        return currentCharIndex >= currentParagraph().getText().getCharList().size();
    }

    private void setCurrentCharShape() {
        int charShapeIndex = currentCharShapeIndex();
        if (charShapeIndex != currentCharShapeIndex) {
            currentCharShapeIndex = charShapeIndex;
            currentCharShape = hwpFile.getDocInfo().getCharShapeList().get(currentCharShapeIndex);
        }
    }

    private int currentCharShapeIndex() {
        int charShapeIndex = (int) currentParagraph().getCharShape().getPositonShapeIdPairList().get(0).getShapeId();
        for (CharPositionShapeIdPair cpsip : currentParagraph ().getCharShape().getPositonShapeIdPairList()) {
            if (currentCharPosition >= cpsip.getPosition()) {
                charShapeIndex = (int) cpsip.getShapeId();
            } else {
                break;
            }
        }
        return charShapeIndex;
    }

    public CharShape currentCharShape() {
        return currentCharShape;
    }

    public HWPChar currentChar() {
        return currentChar;
    }

    public Painter painter() {
        return painter;
    }
}
