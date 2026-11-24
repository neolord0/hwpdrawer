package kr.dogfoot.hwpdrawer.export.pdf;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.DocumentExporter;
import kr.dogfoot.hwpdrawer.export.ExportTarget;
import kr.dogfoot.hwpdrawer.export.PageGeometry;
import kr.dogfoot.hwpdrawer.export.image.PagePainter;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import java.io.IOException;
import java.util.List;

/**
 * 선택한 페이지를 모두 담은 PDF 파일 하나를 만든다: {@code {접두어}.pdf}.
 *
 * <p>폰트는 서브셋으로 임베드해 글자를 선택·검색할 수 있다. 임베드할 수 없는 글자는 윤곽선으로 그린다
 * ({@link PdfFontTextDrawer}).</p>
 */
public class PdfExporter implements DocumentExporter {
    private static final String EXTENSION = "pdf";

    private final PagePainter painter;
    private final ExportTarget target;
    private final String documentTitle;

    public PdfExporter(HWPFileReader reader, ExportTarget target, String documentTitle) {
        this.painter = new PagePainter(reader);
        this.target = target;
        this.documentTitle = documentTitle;
    }

    @Override
    public void export(List<Page> pages) throws IOException {
        target.prepareDirectory();
        try (PdfDocumentBuilder builder = new PdfDocumentBuilder(painter)) {
            for (Page page : pages) {
                if (PageGeometry.of(page).hasArea()) builder.addPage(page);
            }
            target.writeDocumentFile(EXTENSION, file -> builder.save(file, documentTitle));
        }
    }
}
