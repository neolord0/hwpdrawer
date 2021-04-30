package kr.dogfoot.hwplib.drawer.drawinginfo.outputcontent;

import kr.dogfoot.hwplib.drawer.util.Area;

public class PageContent extends OutputContent {
    private int pageNo;
    private Area paperArea;
    private Area pageArea;

    public PageContent(int pageNo, Area paperArea, Area pageArea) {
        this.pageNo = pageNo;
        this.paperArea = paperArea;
        this.pageArea = pageArea;
    }

    public Area paperArea() {
        return paperArea;
    }

    public Area pageArea() {
        return pageArea;
    }

    @Override
    public Type type() {
        return Type.Page;
    }
}
