package kr.dogfoot.hwpdrawer.export.html;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.PageGeometry;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 문서에 나오는 용지 크기마다 CSS 클래스와 인쇄용 이름 있는 페이지({@code @page})를 하나씩 만든다.
 *
 * <p>한 문서 안에 세로 A4와 가로 A4가 섞여 있어도, 인쇄할 때 각 페이지가 자기 용지 크기로 나오게 한다.
 * 화면에서는 클래스의 폭(mm)으로 용지 크기를 보여 준다.</p>
 */
final class PageSizeStyles {
    private static final String CLASS_PREFIX = "size-";

    /** 등장 순서대로의 고유 용지 크기 (mm, 소수 둘째 자리 문자열 — 같은 크기 판정 기준). */
    private final List<String> sizes;

    private PageSizeStyles(List<String> sizes) {
        this.sizes = sizes;
    }

    static PageSizeStyles of(List<Page> pages) {
        List<String> sizes = new ArrayList<>();
        for (Page page : pages) {
            String size = sizeKey(page);
            if (!sizes.contains(size)) sizes.add(size);
        }
        return new PageSizeStyles(sizes);
    }

    String classOf(Page page) {
        return CLASS_PREFIX + sizes.indexOf(sizeKey(page));
    }

    String css() {
        StringBuilder css = new StringBuilder();
        for (int i = 0; i < sizes.size(); i++) {
            String[] widthAndHeight = sizes.get(i).split(" ");
            String name = CLASS_PREFIX + i;
            css.append(String.format(Locale.ROOT, ".%s { width: %smm; page: %s; }\n",
                    name, widthAndHeight[0], name));
            css.append(String.format(Locale.ROOT, "@page %s { size: %smm %smm; margin: 0; }\n",
                    name, widthAndHeight[0], widthAndHeight[1]));
        }
        return css.toString();
    }

    private static String sizeKey(Page page) {
        PageGeometry geometry = PageGeometry.of(page);
        return String.format(Locale.ROOT, "%.2f %.2f", geometry.widthMm(), geometry.heightMm());
    }
}
