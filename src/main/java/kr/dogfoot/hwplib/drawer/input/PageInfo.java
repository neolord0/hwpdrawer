package kr.dogfoot.hwplib.drawer.input;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlFooter;
import kr.dogfoot.hwplib.object.bodytext.control.ControlHeader;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnInfo;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.columndefine.ColumnSort;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageDef;

public class PageInfo {
    private ControlSectionDefine sectionDefine;
    private Area paperArea;
    private Area headerArea;
    private Area footerArea;
    private Area bodyArea;

    private ControlHeader evenHeader;
    private ControlHeader oddHeader;
    private ControlFooter evenFooter;
    private ControlFooter oddFooter;
    private int pageNo;

    public PageInfo() {
        pageNo = 0;
    }

    public void sectionDefine(ControlSectionDefine sectionDefine) {
        this.sectionDefine = sectionDefine;
        calculateArea();
        evenHeader = null;
        oddHeader = null;
        evenFooter = null;
        oddFooter = null;
    }

    private void calculateArea() {
        PageDef pageDef = sectionDefine.getPageDef();
        paperArea = new Area(0, 0, pageDef.getPaperWidth(), pageDef.getPaperHeight());
        bodyArea = new Area(paperArea)
                .applyMargin(pageDef.getLeftMargin(),
                        pageDef.getTopMargin(),
                        pageDef.getRightMargin(),
                        pageDef.getBottomMargin())
                .applyMargin(0,
                        pageDef.getHeaderMargin(),
                        0,
                        pageDef.getFooterMargin());
        headerArea = new Area(paperArea)
                .applyMargin(pageDef.getLeftMargin(),
                        pageDef.getTopMargin(),
                        pageDef.getRightMargin(),
                        pageDef.getBottomMargin())
                .bottom(pageDef.getTopMargin() + pageDef.getHeaderMargin());
        footerArea = new Area(paperArea)
                .applyMargin(pageDef.getLeftMargin(),
                        pageDef.getTopMargin(),
                        pageDef.getRightMargin(),
                        pageDef.getBottomMargin())
                .top(paperArea.bottom() - pageDef.getBottomMargin() - pageDef.getFooterMargin());
    }

    public Area paperArea() {
        return paperArea;
    }

    public Area bodyArea() {
        return bodyArea;
    }

    public Area headerArea() {
        return headerArea;
    }

    public Area footerArea() {
        return footerArea;
    }

    public void evenHeader(ControlHeader evenHeader) {
        this.evenHeader = evenHeader;
    }

    public void oddHeader(ControlHeader oddHeader) {
        this.oddHeader = oddHeader;
    }

    public void bothHeader(ControlHeader bothHeader) {
        evenHeader = oddHeader = bothHeader;
    }

    public void evenFooter(ControlFooter evenFooter) {
        this.evenFooter = evenFooter;
    }

    public void oddFooter(ControlFooter oddFooter) {
        this.oddFooter = oddFooter;
    }

    public void bothFooter(ControlFooter bothFooter) {
        evenFooter = oddFooter = bothFooter;
    }

    public ControlHeader header() {
        if (pageNo % 2 == 0) {
            return evenHeader;
        }
        return oddHeader;
    }

    public ControlFooter footer() {
        if (pageNo % 2 == 0) {
            return evenFooter;
        }
        return oddFooter;
    }

    public int pageNo() {
        return pageNo;
    }

    public void increasePageNo() {
        pageNo++;
    }

    public boolean isHideEmptyLine() {
        return sectionDefine.getHeader().getProperty().isHideEmptyLine();
    }
}
