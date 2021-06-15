package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.interimoutput.text.TextLine;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bindata.EmbeddedBinaryData;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.docinfo.BinData;
import kr.dogfoot.hwplib.object.docinfo.BorderFill;
import kr.dogfoot.hwplib.object.docinfo.CharShape;
import kr.dogfoot.hwplib.object.docinfo.ParaShape;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Stack;

public class DrawingInput {
    private HWPFile hwpFile;

    private final HashMap<Integer, BufferedImage> imageMap;

    private Section section;
    private final PageInfo pageInfo;
    private final ColumnsInfo columnsInfo;
    private int countOfHidingEmptyLineAfterNewPage;

    private ParagraphListInfo bodyTextParaListInfo;
    private final Stack<ParagraphListInfo> paraListInfoStack;

    public DrawingInput() {
        imageMap = new HashMap<>();
        pageInfo = new PageInfo();
        columnsInfo = new ColumnsInfo(pageInfo);
        paraListInfoStack = new Stack<>();
    }

    public HWPFile hwpFile() {
        return hwpFile;
    }

    public DrawingInput hwpFile(HWPFile hwpFile) {
        this.hwpFile = hwpFile;
        return this;
    }

    public BorderFill borderFill(int borderFillId) {
        return hwpFile.getDocInfo().getBorderFillList().get(borderFillId - 1);
    }

    public BufferedImage image(int binItemID) {
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
        for (EmbeddedBinaryData embeddedBinaryData : hwpFile.getBinData().getEmbeddedBinaryDataList()) {
            if (embeddedBinaryData.getName().startsWith(name)) {
                return embeddedBinaryData;
            }
        }
        return null;
    }

    public Section section() {
        return section;
    }

    public DrawingInput section(Section section) throws Exception {
        this.section = section;
        return this;
    }

    public void sectionDefine(ControlSectionDefine sectionDefine) {
        pageInfo.sectionDefine(sectionDefine);
    }

    public void newMultiColumn(ControlColumnDefine columnDefine, long startY) {
        Area multiColumnArea = new Area(pageInfo.bodyArea()).top(startY);
        columnsInfo.set(columnDefine, multiColumnArea);

        currentParaListInfo().bodyArea(columnsInfo.currentColumnArea());
    }

    public void newMultiColumnWithSameColumnDefine(long startY) {
        Area multiColumnArea = new Area(pageInfo.bodyArea()).top(startY);
        columnsInfo.setWithSameColumnDefine(multiColumnArea);

        currentParaListInfo().bodyArea(columnsInfo.currentColumnArea());
    }


    public PageInfo pageInfo() {
        return pageInfo;
    }

    public ColumnsInfo columnsInfo() {
        return columnsInfo;
    }

    public void newPage() {
        pageInfo
                .increasePageNo();
        columnsInfo.reset();

        if (pageInfo.pageNo() > 1 && pageInfo.isHideEmptyLine()) {
            countOfHidingEmptyLineAfterNewPage = 2;
        } else {
            countOfHidingEmptyLineAfterNewPage = 0;
        }

        if (bodyTextParaListInfo != null) {
            columnsInfo.set(new Area(pageInfo.bodyArea()));
            bodyTextParaListInfo.bodyArea(columnsInfo.currentColumnArea());
        }
    }

    public void nextColumn() {
        columnsInfo.nextColumn();
        currentParaListInfo().bodyArea(columnsInfo.currentColumnArea());
    }

    public void previousColumn() {
        columnsInfo.previousColumn();
        currentParaListInfo().bodyArea(columnsInfo.currentColumnArea());
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

    public DrawingInput startBodyTextParaList(Paragraph[] paras) {
        ParagraphListInfo paragraphListInfo = new ParagraphListInfo(this)
                .bodyText(true)
                .paras(paras);
        paraListInfoStack.push(paragraphListInfo);

        bodyTextParaListInfo = paragraphListInfo;
        return this;
    }

    public void endBodyTextParaList() {
        paraListInfoStack.pop();
        bodyTextParaListInfo = null;
    }

    public void startControlParaList(Area textArea, Paragraph[] paras) {
        ParagraphListInfo paraListInfo = new ParagraphListInfo(this, textArea)
                .bodyText(false)
                .paras(paras);
        paraListInfoStack.push(paraListInfo);
    }

    public long endControlParaList() {
        ParagraphListInfo paraListInfo = paraListInfoStack.pop();
        return paraListInfo.height();
    }

    public ParagraphListInfo currentParaListInfo() {
        return paraListInfoStack.peek();
    }

    public boolean isBodyText() {
        return currentParaListInfo().isBodyText();
    }

    public Area paraArea() {
        return currentParaListInfo().paraArea();
    }

    public boolean nextPara() {
        return currentParaListInfo().nextPara();
    }

    public void startPara() {
        currentParaListInfo().startPara();
    }

    public void endPara(long endY, long height) {
        currentParaListInfo().endPara(endY, height);
    }

    public Paragraph currentPara() {
        return currentParaListInfo().currentPara();
    }

    public int paraIndex() {
        return currentParaListInfo().paraIndex();
    }

    public ParaShape paraShape() {
        return currentParaListInfo().paraShape();
    }

    public boolean noText() {
        return currentPara().getText() == null || currentPara().getText().getCharList().size() == 0;
    }

    public boolean nextChar() {
        return currentParaListInfo().nextChar();
    }

    public HWPChar currentChar() {
        return currentParaListInfo().currentChar();
    }

    public boolean previousChar(int count) {
        for (int index = 0; index < count; index++) {
            if (!currentParaListInfo().previousChar()) {
                return false;
            }
        }
        return true;
    }

    public int charIndex() {
        return currentParaListInfo().charIndex();
    }

    public int charPosition() {
        return currentParaListInfo().charPosition();
    }

    public CharShape charShape() {
        return currentParaListInfo().charShape();
    }

    public void gotoCharInPara(CharInfo charInfo) {
        currentParaListInfo().gotoChar(charInfo.index(), charInfo.prePosition());
    }

    public void gotoChar(CharInfo charInfo) {
        currentParaListInfo().gotoPara(charInfo.paraIndex());
        currentParaListInfo().gotoChar(charInfo.index(), charInfo.prePosition());
    }


    public void gotoParaCharPosition(int paragraphIndex, int characterIndex, int characterPosition) {
        currentParaListInfo().gotoPara(paragraphIndex);
        currentParaListInfo().gotoChar(characterIndex, characterPosition);
    }

    public void gotoLineFirstChar(TextLine textLine) {
        assert(textLine.paraIndex() == textLine.firstChar().paraIndex());
        gotoParaCharPosition(textLine.paraIndex(),
                textLine.firstChar().index(),
                textLine.firstChar().prePosition());
    }

}
