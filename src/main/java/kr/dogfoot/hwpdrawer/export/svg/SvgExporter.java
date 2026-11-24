package kr.dogfoot.hwpdrawer.export.svg;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.DocumentExporter;
import kr.dogfoot.hwpdrawer.export.ExportTarget;
import kr.dogfoot.hwpdrawer.export.PageGeometry;
import kr.dogfoot.hwpdrawer.export.image.PagePainter;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * 페이지마다 SVG 파일 하나를 만든다: {@code {접두어}{쪽 번호 3자리}.svg}.
 */
public class SvgExporter implements DocumentExporter {
    private static final String EXTENSION = "svg";
    private static final String XML_DECLARATION = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";

    private final SvgPageRenderer pageRenderer;
    private final ExportTarget target;

    public SvgExporter(HWPFileReader reader, ExportTarget target) {
        this.pageRenderer = new SvgPageRenderer(new PagePainter(reader));
        this.target = target;
    }

    @Override
    public void export(List<Page> pages) throws IOException {
        target.prepareDirectory();
        for (Page page : pages) {
            if (PageGeometry.of(page).hasArea()) exportPage(page);
        }
    }

    private void exportPage(Page page) throws IOException {
        String svgElement = pageRenderer.render(page);
        try (Writer out = Files.newBufferedWriter(
                target.pageFile(page.pageNo(), EXTENSION).toPath(), StandardCharsets.UTF_8)) {
            out.write(XML_DECLARATION);
            out.write(svgElement);
            out.write('\n');
        }
    }
}
