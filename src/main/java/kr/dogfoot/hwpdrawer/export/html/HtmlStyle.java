package kr.dogfoot.hwpdrawer.export.html;

/**
 * HTML 내보내기의 공통 스타일시트.
 *
 * <p>화면: 회색 바탕 가운데에 용지(흰 바탕 + 그림자)를 세로로 늘어놓는다. 창이 용지보다 좁으면
 * 용지를 창 폭에 맞춰 줄인다(SVG viewBox 비율 유지).
 * 인쇄: 여백·그림자 없이 페이지마다 한 장. 용지 크기는 {@link PageSizeStyles}의 {@code @page}가 정한다.</p>
 */
final class HtmlStyle {
    static final String BASE = String.join("\n",
            ":root { color-scheme: light; }",
            "* { box-sizing: border-box; }",
            "body { margin: 0; background: #e8e8e8; }",
            ".document { display: flex; flex-direction: column; align-items: center; gap: 24px;"
                    + " padding: 24px 16px; }",
            ".page { max-width: 100%; background: #ffffff; box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3); }",
            ".page > svg { display: block; width: 100%; height: auto; }",
            "@media print {",
            "  body { background: none; }",
            "  .document { display: block; padding: 0; }",
            "  .page { max-width: none; box-shadow: none; break-after: page; }",
            "  .page:last-child { break-after: auto; }",
            "}",
            "");

    private HtmlStyle() {}
}
