package kr.dogfoot.hwpdrawer.document;

import kr.dogfoot.hwpdrawer.document.page.Page;

import java.util.ArrayList;
import java.util.List;

/**
 * Engine이 생성한 중간 결과물.
 * 페이지 목록을 포함하며, Export 단계에서 소비된다.
 */
public class Document {
    private final List<Page> pages = new ArrayList<>();

    public List<Page> pages() {
        return pages;
    }

    public Page addPage() {
        Page page = new Page(pages.size() + 1);
        pages.add(page);
        return page;
    }

    public Page currentPage() {
        if (pages.isEmpty()) return null;
        return pages.get(pages.size() - 1);
    }

    public int pageCount() {
        return pages.size();
    }

    public void removeLastPage() {
        if (!pages.isEmpty()) pages.remove(pages.size() - 1);
    }
}
