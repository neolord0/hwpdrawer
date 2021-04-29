package kr.dogfoot.hwplib.drawer.drawinginfo;

import kr.dogfoot.hwplib.drawer.drawinginfo.contentbuffer.OutputContent;
import kr.dogfoot.hwplib.drawer.drawinginfo.contentbuffer.ControlContent;
import kr.dogfoot.hwplib.drawer.drawinginfo.contentbuffer.PageContent;
import kr.dogfoot.hwplib.drawer.paragraph.TextPart;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bindata.EmbeddedBinaryData;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlType;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageDef;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.BinData;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

import javax.imageio.ImageIO;
import javax.sound.sampled.Line;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;

public class DrawingInfo {
    private HWPFile hwpFile;
    private Section section;
    private ControlSectionDefine sectionDefine;
    private Area paperArea;
    private Area pageArea;

    private HashMap<Integer, BufferedImage> imageMap;

    private int pageNo;
    private int countOfHidingEmptyLineAfterNewPage;

    private ParagraphListInfo bodyTextParagraphListInfo;
    private PageContent pageContent;

    private Stack<ParagraphListInfo> paragraphListInfoStack;
    private Stack<OutputContent> outputContentStack;

    public DrawingInfo() {
        imageMap = new HashMap<>();
        paragraphListInfoStack = new Stack<>();
        outputContentStack = new Stack<>();
    }

    public HWPFile hwpFile() {
        return hwpFile;
    }

    public DrawingInfo hwpFile(HWPFile hwpFile) {
        this.hwpFile = hwpFile;
        return this;
    }


    public BorderFill getBorderFill(int borderFillId) {
        return hwpFile.getDocInfo().getBorderFillList().get(borderFillId - 1);
    }

    public BufferedImage getImage(int binItemID) {
        BinData binData = hwpFile.getDocInfo().getBinDataList().get(binItemID - 1);

        if (binData.getBinDataID() > 0) {
            BufferedImage image = imageMap.get(binData.getBinDataID());
            if (image != null) {
                return image;
            }

            EmbeddedBinaryData embeddedBinaryData = embeddedBinaryData(binData.getBinDataID());
            if (embeddedBinaryData == null || embeddedBinaryData.getData() == null) {
                return null;
            }

            try {
                InputStream is = new ByteArrayInputStream(embeddedBinaryData.getData());
                image = ImageIO.read(is);

                imageMap.put(binData.getBinDataID(), image);
            } catch (IOException e) {
                e.printStackTrace();
                image = null;
            }
            return image;
        }
        return null;
    }

    private EmbeddedBinaryData embeddedBinaryData(int binDataID) {
        String name = "BIN" + String.format("%04X", binDataID);
        for (EmbeddedBinaryData embeddedBinaryData  : hwpFile.getBinData().getEmbeddedBinaryDataList()) {
            if (embeddedBinaryData.getName().startsWith(name)) {
                return embeddedBinaryData;
            }
        }
        return null;
    }


    public Section section() {
        return section;
    }

    public DrawingInfo section(Section section) throws Exception {
        this.section = section;
        setSectionDefine();
        calculatePaperPageArea();
        return this;
    }

    private void setSectionDefine() throws Exception {
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

        sectionDefine = (ControlSectionDefine) firstPara.getControlList().get(0);
    }

    private void calculatePaperPageArea() {
        PageDef pageDef = sectionDefine.getPageDef();
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

    public DrawingInfo startBodyTextParagraphList() {
        ParagraphListInfo paragraphListInfo = new ParagraphListInfo(this)
                .bodyText(true);
        bodyTextParagraphListInfo = paragraphListInfo;

        paragraphListInfoStack.push(paragraphListInfo);
        return this;
    }

    public void endBodyTextParagraphList() {
        paragraphListInfoStack.pop();
    }

    public void newPage() {
        pageNo++;
        if (pageNo > 1 && sectionDefine.getHeader().getProperty().isHideEmptyLine()) {
            countOfHidingEmptyLineAfterNewPage = 2;
        } else {
            countOfHidingEmptyLineAfterNewPage = 0;
        }

        pageContent = new PageContent(pageNo, paperArea, pageArea);

        if (bodyTextParagraphListInfo.paragraph() != null) {
            bodyTextParagraphListInfo.resetParagraphStartY();
        }

        outputContentStack.add(pageContent);
    }

    public int pageNo() {
        return pageNo;
    }

    public boolean checkHidingEmptyLineAfterNewPage() {
        return countOfHidingEmptyLineAfterNewPage > 0;
    }

    public void descendCountOfHidingEmptyLineAfterNewPage() {
        countOfHidingEmptyLineAfterNewPage--;
    }

    public void resetCountOfHidingEmptyLineAfterNewPage() {
        countOfHidingEmptyLineAfterNewPage = 0;
    }

    public PageContent pageContent() {
        return pageContent;
    }

    public void startContentContentAndParagraphList(Area controlArea, Area textArea) {
        ControlContent controlContent = new ControlContent(controlArea, textArea);

        outputContent().addChildControlContents(controlContent);
        outputContentStack.push(controlContent);

        ParagraphListInfo paragraphListInfo = new ParagraphListInfo(this, textArea)
                .bodyText(false);
        paragraphListInfoStack.push(paragraphListInfo);
    }

    public ControlContent endControlContentAndParagraphList() {
        ControlContent controlContent = (ControlContent) outputContentStack.pop();
        ParagraphListInfo paragraphListInfo = paragraphListInfoStack.pop();

        controlContent.height(paragraphListInfo.height());
        return controlContent;
    }

    public OutputContent outputContent() {
        return outputContentStack.peek();
    }

    public void startParagraph(Paragraph paragraph) {
        paragraphListInfo().startParagraph(paragraph);
    }

    public void endParagraph(long endY, long height) throws IOException {
        outputContent().setLastTextPartToLastLine();
        ParagraphListInfo paragraphListInfo = paragraphListInfo();
        paragraphListInfo.endParagraph(endY, height);
    }

    public ParagraphListInfo paragraphListInfo() {
        return paragraphListInfoStack.peek();
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
        return paragraph().getText() == null || paragraph().getText().getCharList().size() == 0;
    }

    public boolean nextChar() {
        return paragraphListInfo().nextChar();
    }

    public HWPChar character() {
        return paragraphListInfo().character();
    }

    public boolean beforeChar(int count) {
        for (int index = 0; index < count; index++) {
            if (!paragraphListInfo().beforeChar()) {
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
