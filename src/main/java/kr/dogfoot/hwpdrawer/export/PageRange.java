package kr.dogfoot.hwpdrawer.export;

import kr.dogfoot.hwpdrawer.document.page.Page;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 내보낼 페이지 범위. 페이지 번호는 1부터 시작하며 시작/끝 페이지를 모두 포함한다.
 *
 * <p>끝 페이지를 정하지 않으면({@link #from(int)}) 문서의 마지막 페이지까지다.
 * 끝 페이지가 문서 페이지 수보다 커도 마지막 페이지까지만 내보낸다.
 * 시작 페이지가 문서 페이지 수보다 크면 내보낼 페이지가 없으므로 예외를 던진다.</p>
 */
public final class PageRange {
    private static final int FIRST_PAGE = 1;
    private static final int OPEN_END = Integer.MAX_VALUE;
    private static final PageRange ALL = new PageRange(FIRST_PAGE, OPEN_END);

    private final int startPage;
    private final int endPage;

    private PageRange(int startPage, int endPage) {
        this.startPage = startPage;
        this.endPage = endPage;
    }

    /** 문서 전체. */
    public static PageRange all() {
        return ALL;
    }

    /** startPage ~ endPage (양끝 포함). */
    public static PageRange of(int startPage, int endPage) {
        requireValidStart(startPage);
        if (endPage < startPage) {
            throw new IllegalArgumentException(String.format(
                    "끝 페이지(%d)는 시작 페이지(%d)보다 작을 수 없습니다.", endPage, startPage));
        }
        return new PageRange(startPage, endPage);
    }

    /** startPage ~ 마지막 페이지. */
    public static PageRange from(int startPage) {
        requireValidStart(startPage);
        return new PageRange(startPage, OPEN_END);
    }

    private static void requireValidStart(int startPage) {
        if (startPage < FIRST_PAGE) {
            throw new IllegalArgumentException(String.format(
                    "시작 페이지(%d)는 %d 이상이어야 합니다.", startPage, FIRST_PAGE));
        }
    }

    public int startPage() {
        return startPage;
    }

    /** 끝 페이지가 정해져 있으면 true. false면 마지막 페이지까지다. */
    public boolean hasEndPage() {
        return endPage != OPEN_END;
    }

    /**
     * 문서의 페이지 목록에서 이 범위에 해당하는 페이지만 고른다 (문서 순서 유지).
     *
     * @throws IllegalArgumentException 시작 페이지가 문서 페이지 수보다 클 때
     *         (빈 문서를 처음부터 내보내는 경우는 빈 목록)
     */
    public List<Page> select(List<Page> pages) {
        if (pages.isEmpty() && startPage == FIRST_PAGE) return Collections.emptyList();
        if (startPage > pages.size()) {
            throw new IllegalArgumentException(String.format(
                    "시작 페이지(%d)가 문서의 페이지 수(%d)보다 큽니다.", startPage, pages.size()));
        }
        int lastPage = Math.min(endPage, pages.size());
        return Collections.unmodifiableList(pages.subList(startPage - 1, lastPage));
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof PageRange)) return false;
        PageRange that = (PageRange) other;
        return startPage == that.startPage && endPage == that.endPage;
    }

    @Override
    public int hashCode() {
        return Objects.hash(startPage, endPage);
    }

    @Override
    public String toString() {
        return startPage + "~" + (hasEndPage() ? String.valueOf(endPage) : "끝");
    }
}
