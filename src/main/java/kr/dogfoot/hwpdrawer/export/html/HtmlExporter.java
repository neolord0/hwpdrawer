package kr.dogfoot.hwpdrawer.export.html;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.DocumentExporter;
import kr.dogfoot.hwpdrawer.export.ExportTarget;
import kr.dogfoot.hwpdrawer.export.PageGeometry;
import kr.dogfoot.hwpdrawer.export.image.PagePainter;
import kr.dogfoot.hwpdrawer.export.svg.SvgPageRenderer;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 선택한 페이지를 모두 담은 HTML 파일 하나를 만든다: {@code {접두어}.html}.
 *
 * <p>각 페이지는 {@code <section class="page">} 안의 인라인 SVG다({@link SvgPageRenderer}). 화면에서는
 * 회색 바탕 위에 용지처럼 보이고, 인쇄하면 페이지마다 원래 용지 크기로 한 장씩 나온다.
 * 외부 파일(이미지·폰트·스크립트)이 없어 파일 하나만 옮겨도 그대로 열린다.</p>
 *
 * <p>XML 파서로도 읽을 수 있게 빈 요소를 {@code <meta … />}처럼 닫아 쓴다 (HTML5에서도 유효).</p>
 */
public class HtmlExporter implements DocumentExporter {
    private static final String EXTENSION = "html";

    private final SvgPageRenderer pageRenderer;
    private final ExportTarget target;
    private final String documentTitle;

    public HtmlExporter(HWPFileReader reader, ExportTarget target, String documentTitle) {
        this.pageRenderer = new SvgPageRenderer(new PagePainter(reader));
        this.target = target;
        this.documentTitle = documentTitle;
    }

    @Override
    public void export(List<Page> pages) throws IOException {
        target.prepareDirectory();
        List<Page> drawablePages = pages.stream()
                .filter(page -> PageGeometry.of(page).hasArea())
                .collect(Collectors.toList());
        PageSizeStyles sizeStyles = PageSizeStyles.of(drawablePages);
        target.writeDocumentFile(EXTENSION, file -> writeDocument(file, drawablePages, sizeStyles));
    }

    private void writeDocument(File file, List<Page> pages, PageSizeStyles sizeStyles) throws IOException {
        try (Writer out = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writeHead(out, sizeStyles);
            for (Page page : pages) {
                writePage(out, page, sizeStyles);
            }
            out.write("</main>\n</body>\n</html>\n");
        }
    }

    private void writeHead(Writer out, PageSizeStyles sizeStyles) throws IOException {
        out.write("<!DOCTYPE html>\n");
        out.write("<html lang=\"ko\">\n<head>\n");
        out.write("<meta charset=\"utf-8\" />\n");
        out.write("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n");
        out.write("<meta name=\"generator\" content=\"HWP Drawer\" />\n");
        out.write("<title>" + HtmlText.escape(documentTitle) + "</title>\n");
        out.write("<style>\n");
        out.write(HtmlStyle.BASE);
        out.write(sizeStyles.css());
        out.write("</style>\n</head>\n<body>\n<main class=\"document\">\n");
    }

    private void writePage(Writer out, Page page, PageSizeStyles sizeStyles) throws IOException {
        out.write(String.format(Locale.ROOT,
                "<section class=\"page %s\" id=\"page-%d\" data-page-no=\"%d\" aria-label=\"%d쪽\">\n",
                sizeStyles.classOf(page), page.pageNo(), page.pageNo(), page.pageNo()));
        out.write(pageRenderer.render(page));
        out.write("\n</section>\n");
    }
}
