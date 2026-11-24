package kr.dogfoot.hwpdrawer.export;

import kr.dogfoot.hwpdrawer.document.page.Page;

import java.io.IOException;
import java.util.List;

/**
 * 레이아웃이 끝난 페이지들을 특정 형식(PNG/SVG/PDF/HTML)의 파일로 내보낸다.
 */
public interface DocumentExporter {
    /**
     * @param pages 내보낼 페이지 (문서 순서). {@link PageRange#select(List)}로 고른 일부일 수 있다.
     */
    void export(List<Page> pages) throws IOException;
}
