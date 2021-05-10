package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuffer;


public class PageOutput extends Output {
    private int pageNo;
    private Area paperArea;
    private Area pageArea;

    private Content content;

    public PageOutput(int pageNo, Area paperArea, Area pageArea) {
        this.pageNo = pageNo;
        this.paperArea = paperArea;
        this.pageArea = pageArea;

        content = new Content();
    }

    public Area paperArea() {
        return paperArea;
    }

    public Area pageArea() {
        return pageArea;
    }

    @Override
    public Content content() {
        return content;
    }

    @Override
    public Type type() {
        return Type.Page;
    }

    @Override
    public String test(int tabCount) {
        MyStringBuffer sb = new MyStringBuffer();
        sb.tab(tabCount).append("page - {\n");
        sb.append(content.test(tabCount + 1));
        sb.tab(tabCount).append("page - }\n");
        return sb.toString();
    }
}
