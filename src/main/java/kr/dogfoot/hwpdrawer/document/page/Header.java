package kr.dogfoot.hwpdrawer.document.page;

import kr.dogfoot.hwpdrawer.document.Content;
import kr.dogfoot.hwpdrawer.util.Area;

public class Header {
    private Area area;
    private Content content;

    public Header() {
        this.area = new Area();
        this.content = new Content();
    }

    public Area area() { return area; }
    public Content content() { return content; }
}
