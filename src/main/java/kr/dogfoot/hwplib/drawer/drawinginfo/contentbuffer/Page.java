package kr.dogfoot.hwplib.drawer.drawinginfo.contentbuffer;

import kr.dogfoot.hwplib.drawer.util.Area;

public class Page extends ContentBuffer {
    private Area paperArea;
    private Area pageArea;

    public Page(Area paperArea, Area pageArea) {
        this.paperArea = paperArea;
        this.pageArea = pageArea;
    }

    public Area paperArea() {
        return paperArea;
    }

    public Area pageArea() {
        return pageArea;
    }
}
