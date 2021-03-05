package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.UnitConvertor;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageDef;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import org.w3c.dom.css.Rect;

import java.awt.*;
import java.util.Stack;

public class DrawingInfo {
    private HWPFile hwpFile;
    private DrawingOption option;

    private Section currentSection;
    private PageDef currentPageDef;
    private Area pageDrawArea;

    private Stack<Paragraph> currentParagraphStack;


    private PageMaker pageMaker;

    private ParagraphDrawer paragraphDrawer;

    public DrawingInfo() {
        hwpFile = null;
        option = null;

        currentSection = null;
        currentParagraphStack = new Stack<>();

        pageMaker = new PageMaker(this);

        paragraphDrawer = new ParagraphDrawer(this);
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
        UnitConvertor.zoomRate(option.zoomRate());
        return this;
    }

    public Section currentSection() {
        return currentSection;
    }

    public void currentSection(Section currentSection) throws Exception {
        this.currentSection = currentSection;

        setCurrentPageDef();
    }

    private void setCurrentPageDef() throws Exception {
        Paragraph firstPara = currentSection.getParagraph(0);
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

        currentPageDef = sectionDefine.getPageDef();
        calculatePageDrawArea();
    }

    private void calculatePageDrawArea() {
        pageDrawArea = new Area(0, 0, currentPageDef.getPaperWidth(), currentPageDef.getPaperHeight())
                .applyMargin(currentPageDef.getLeftMargin(),
                        currentPageDef.getTopMargin(),
                        currentPageDef.getRightMargin(),
                        currentPageDef.getBottomMargin())
                .applyMargin(0,
                        currentPageDef.getHeaderMargin(),
                        0,
                        currentPageDef.getFooterMargin());
    }

    public PageDef currentPageDef() {
        return currentPageDef;
    }

    public Area pageDrawArea() {
        return pageDrawArea;
    }


    public void startCurrentParagraph(Paragraph currentParagraph) {
        currentParagraphStack.push(currentParagraph);
    }

    public void endCurrentParagraph() {
        currentParagraphStack.pop();
    }

    public Paragraph currentParagraph() {
        return currentParagraphStack.peek();
    }


    public PageMaker pageMaker() {
        return pageMaker;
    }

    public ParagraphDrawer paragraphDrawer() {
        return paragraphDrawer;
    }
}
