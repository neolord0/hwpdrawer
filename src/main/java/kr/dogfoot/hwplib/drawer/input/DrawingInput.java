package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.input.paralist.ColumnsInfo;
import kr.dogfoot.hwplib.drawer.input.paralist.ParagraphListInfo;
import kr.dogfoot.hwplib.drawer.input.paralist.ParallelMultiColumnInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bindata.EmbeddedBinaryData;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;
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
import java.util.Map;
import java.util.Stack;

public class DrawingInput {
    private HWPFile hwpFile;

    private final HashMap<Integer, BufferedImage> imageMap;

    private Section section;
    private final PageInfo pageInfo;

    private final Map<ParagraphListInterface, ColumnsInfo> columnsInfoMap;

    private ParagraphListInfo bodyTextParaListInfo;
    private final Stack<ParagraphListInfo> paraListInfoStack;

    public DrawingInput() {
        imageMap = new HashMap<>();
        pageInfo = new PageInfo();
        columnsInfoMap = new HashMap<>();
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

    public DrawingInput section(Section section) {
        this.section = section;
        return this;
    }

    public PageInfo pageInfo() {
        return pageInfo;
    }

    public void sectionDefine(ControlSectionDefine sectionDefine) {
        pageInfo.sectionDefine(sectionDefine);
    }

    public ColumnsInfo getColumnInfo(ParagraphListInterface paraList) {
        ColumnsInfo columnsInfo = columnsInfoMap.get(paraList);
        if (columnsInfo == null) {
            columnsInfo = new ColumnsInfo(pageInfo);
            columnsInfoMap.put(paraList, columnsInfo);
        }
        return columnsInfo;
    }

    public void nextPage() {
        pageInfo.increasePageNo();

        pageInfo.setCountOfHidingEmptyLineAfterNewPage();

        if (bodyTextParaListInfo != null) {
            bodyTextParaListInfo.setColumnInfoWithPreviousColumnDefine(new Area(pageInfo.bodyArea()));
        } else {
            currentColumnsInfo().reset();
        }
    }

    public ColumnsInfo currentColumnsInfo() {
        return columnsInfoMap.get(currentParaListInfo().paraList());
    }

    public void gotoPage(int pageNo) {
        pageInfo.pageNo(pageNo);
    }

    public void columnsInfo(ControlColumnDefine columnDefine, long startY) {
        if (columnDefine != null) {
            currentParaListInfo()
                    .setColumnInfo(columnDefine, new Area(currentColumnsInfo().textBoxArea()).top(startY));
        } else {
            currentParaListInfo()
                    .setColumnInfoWithPreviousColumnDefine(new Area(currentColumnsInfo().textBoxArea()).top(startY));
        }
    }

    public void nextColumn() {
        currentParaListInfo().nextColumn();
    }

    public void previousColumn() {
        currentParaListInfo().previousColumn();
    }

    public void gotoColumn(int columnIndex) {
        currentParaListInfo().gotoColumn(columnIndex);
    }

    public void gotoFirstColumn() {
        gotoColumn(0);
    }

    public DrawingInput startBodyTextParaList(ParagraphListInterface paraList) {
        ParagraphListInfo paragraphListInfo = new ParagraphListInfo(this, paraList)
                .forBodyText();
        paraListInfoStack.push(paragraphListInfo);

        bodyTextParaListInfo = paragraphListInfo;
        return this;
    }

    public void endBodyTextParaList() {
        paraListInfoStack.pop();
        bodyTextParaListInfo = null;
    }

    public void startControlParaList(Area textBoxArea, ParagraphListInterface paraList) {
        ParagraphListInfo paraListInfo = new ParagraphListInfo(this, paraList)
                .forControl(textBoxArea);
        paraListInfoStack.push(paraListInfo);
    }

    public long endControlParaList() {
        ParagraphListInfo paraListInfo = paraListInfoStack.pop();
        return paraListInfo.height();
    }

    public void startCellParaList(Area textBoxArea, ParagraphListInterface paraList, boolean canSplit, long topInPage, long bottomMargin, boolean split, int startTextColumnIndex) {
        ParagraphListInfo paraListInfo = new ParagraphListInfo(this, paraList)
                .forCell(textBoxArea, canSplit, topInPage, bottomMargin, split, startTextColumnIndex);
        paraListInfoStack.push(paraListInfo);
    }

    public long endCellParaList() {
        ParagraphListInfo paraListInfo = paraListInfoStack.pop();
        return paraListInfo.height();
    }

    public ParagraphListInfo currentParaListInfo() {
        return paraListInfoStack.peek();
    }

    public ParagraphListInfo.Sort sortOfText() {
        return currentParaListInfo().sort();
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

    public boolean noChar() {
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

    public void gotoCharPositionInPara(TextPosition position) {
        currentParaListInfo().gotoChar(position);
    }

    public void gotoParaCharPosition(TextPosition position) {
        currentParaListInfo().gotoPara(position);
        currentParaListInfo().gotoChar(position);
    }

    public void gotoParaWithIgnoreNextPara(TextPosition position) {
        currentParaListInfo().gotoPara(position);
        currentParaListInfo().ignoreNextPara();
    }

    public ParallelMultiColumnInfo parallelMultiColumnInfo() {
        return currentColumnsInfo().parallelMultiColumnInfo();
    }

}

