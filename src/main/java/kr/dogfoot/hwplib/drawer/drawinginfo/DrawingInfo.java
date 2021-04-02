package kr.dogfoot.hwplib.drawer.drawinginfo;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageDef;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Stack;

public class DrawingInfo {
    private HWPFile hwpFile;
    private Section section;
    private PageDef pageDef;
    private Area paperArea;
    private Area pageArea;

    private Stack<ParagraphListInfo> paragraphInfoStack;
    private ParagraphListInfo bodyTextParagraphListInfo;

    public DrawingInfo() {
        hwpFile = null;
        section = null;
        pageDef = null;

        paragraphInfoStack = new Stack<>();
        bodyTextParagraphListInfo = null;
    }

    public HWPFile hwpFile() {
        return hwpFile;
    }

    public DrawingInfo hwpFile(HWPFile hwpFile) {
        this.hwpFile = hwpFile;
        return this;
    }

    public Section section() {
        return section;
    }

    public void section(Section section) throws Exception {
        this.section = section;
        setPageDef();
        calculatePaperPageArea();
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
        pageDef = sectionDefine.getPageDef();
    }

    private void calculatePaperPageArea() {
        paperArea = new Area(0, 0, pageDef.getPaperWidth(), pageDef.getPaperHeight());
        pageArea = new Area(paperArea)
                .applyMargin(pageDef.getLeftMargin(),
                        pageDef.getTopMargin(),
                        pageDef.getRightMargin(),
                        pageDef.getBottomMargin())
                .applyMargin(0,
                        pageDef.getHeaderMargin(),
                        0,
                        pageDef.getFooterMargin());
    }

    public Area paperArea() {
        return paperArea;
    }

    public Area pageArea() {
        return pageArea;
    }

    public void processAtNewPage() {
        if (bodyTextParagraphListInfo != null) {
            bodyTextParagraphListInfo.resetParagraphStartY();
        }
    }

    public void startParagraphList(boolean isTextBody) {
        ParagraphListInfo paragraphListInfo = new ParagraphListInfo(this, isTextBody);
        paragraphInfoStack.push(paragraphListInfo);
        if (isTextBody) {
            bodyTextParagraphListInfo = paragraphListInfo;
        }
    }

    public void endParagraphList() throws IOException {
        paragraphInfoStack.pop();
    }

    public void startParagraph(Paragraph paragraph) {
        paragraphListInfo().startParagraph(paragraph);
    }

    public boolean endParagraph(long paragraphHeight) throws IOException {
        ParagraphListInfo paragraphListInfo = paragraphListInfo();
        paragraphListInfo.endParagraph(paragraphHeight);
        if (paragraphListInfo.isBodyText()) {
            return checkNewPage(paragraphListInfo);
        }
        return false;
    }

    private boolean checkNewPage(ParagraphListInfo paragraphListInfo) throws IOException {
        return paragraphListInfo.paragraphStartY() > pageArea().height();
    }

    public ParagraphListInfo paragraphListInfo() {
        return paragraphInfoStack.peek();
    }

    public boolean isBodyText() {
        return paragraphListInfo().isBodyText();
    }

    public Area paragraphArea() {
        return paragraphListInfo().paragraphArea();
    }

    public Paragraph paragraph() {
        return paragraphListInfo().paragraph();
    }

    public ParaShape paraShape() {
        return paragraphListInfo().paraShape();
    }

    public CharShape charShape() {
        return paragraphListInfo().charShape();
    }

    public boolean noText() {
        return paragraph().getText() == null;
    }

    public boolean nextChar() {
        return paragraphListInfo().nextChar();
    }

    public HWPChar character() {
        return paragraphListInfo().character();
    }

    public boolean isLastChar() {
        return paragraphListInfo().isLastChar();
    }

    public boolean beforeChar(int count) {
        for (int index = 0; index < count; index++) {
            if(!paragraphListInfo().beforeChar()) {
                return false;
            }
        }
        return true;
    }

    public int charIndex() {
        return paragraphListInfo().charIndex();
    }

    public int charPosition() {
        return paragraphListInfo().charPosition();
    }

    public void gotoCharPosition(int lineFirstCharIndex, int lineFirstCharPosition) {
        paragraphListInfo().gotoCharPosition(lineFirstCharIndex, lineFirstCharPosition);
    }
}
