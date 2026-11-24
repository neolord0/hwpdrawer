package kr.dogfoot.hwpdrawer.document.page;

import kr.dogfoot.hwpdrawer.document.Content;
import kr.dogfoot.hwpdrawer.util.Area;

/** 각주 하나. 머리말/꼬리말({@link Header}/{@link Footer})과 같은 "본문 밖 콘텐츠" 구조다. */
public class Footnote {
    private Area area;
    private Content content;

    public Footnote() {
        this.area = new Area();
        this.content = new Content();
    }

    public Area area() { return area; }
    public Content content() { return content; }
}
