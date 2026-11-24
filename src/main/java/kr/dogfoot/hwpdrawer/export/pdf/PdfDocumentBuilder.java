package kr.dogfoot.hwpdrawer.export.pdf;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2D;
import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DLosslessImageEncoder;
import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.PageGeometry;
import kr.dogfoot.hwpdrawer.export.image.PagePainter;
import kr.dogfoot.hwpdrawer.export.vector.PortableImageGraphics2D;
import kr.dogfoot.hwpdrawer.export.vector.StrictClipGraphics2D;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.util.Matrix;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * PDF 문서 하나를 페이지 단위로 쌓아 저장한다.
 *
 * <p>각 페이지는 렌더러 픽셀 좌표계로 그린 Form XObject를 만든 뒤, 실제 용지 크기(pt)의 PDF 페이지에
 * 비율(pt/px)만큼 축소해 붙인다. 그래서 PDF 페이지 크기는 HWP 용지 크기와 정확히 같다.</p>
 */
final class PdfDocumentBuilder implements Closeable {
    private static final String PRODUCER = "HWP Drawer";

    private final PagePainter painter;
    private final PDDocument document = new PDDocument();
    private final PdfFontTextDrawer fonts = new PdfFontTextDrawer();
    private final PdfBoxGraphics2DLosslessImageEncoder images = new PdfBoxGraphics2DLosslessImageEncoder();

    PdfDocumentBuilder(PagePainter painter) {
        this.painter = painter;
    }

    /** {@code PageGeometry.of(page).hasArea()}인 페이지만 넘긴다. */
    void addPage(Page page) throws IOException {
        PageGeometry geometry = PageGeometry.of(page);
        PDPage pdfPage = new PDPage(new PDRectangle(geometry.widthPt(), geometry.heightPt()));
        document.addPage(pdfPage);

        PDFormXObject content = paint(page, geometry);
        float scale = geometry.pointsPerPixel();
        try (PDPageContentStream stream = new PDPageContentStream(document, pdfPage)) {
            stream.transform(Matrix.getScaleInstance(scale, scale));
            stream.drawForm(content);
        }
    }

    private PDFormXObject paint(Page page, PageGeometry geometry) throws IOException {
        PdfBoxGraphics2D pdfGraphics = newPageGraphics(geometry);
        Graphics2D g2d = new PortableImageGraphics2D(new StrictClipGraphics2D(pdfGraphics));
        try {
            g2d.setColor(Color.WHITE);
            g2d.fill(new Rectangle2D.Float(0, 0, geometry.exactWidthPx(), geometry.exactHeightPx()));
            painter.paint(page, g2d);
        } catch (RuntimeException | Error failure) {
            disposeAfterFailure(g2d, failure);
            throw failure;
        }
        g2d.dispose();
        return pdfGraphics.getXFormObject();
    }

    /** 페이지마다 새 Graphics2D를 만들되, 폰트와 그림 인코더는 문서 전체가 함께 쓴다 (같은 그림은 한 번만 넣는다). */
    private PdfBoxGraphics2D newPageGraphics(PageGeometry geometry) throws IOException {
        PdfBoxGraphics2D pdfGraphics = new PdfBoxGraphics2D(document,
                geometry.exactWidthPx(), geometry.exactHeightPx());
        pdfGraphics.setFontTextDrawer(fonts);
        pdfGraphics.setImageEncoder(images);
        return pdfGraphics;
    }

    /**
     * 그리다 실패하면 PdfBoxGraphics2D의 저장/복원 짝이 어긋나 dispose()도 IllegalStateException을 던진다.
     * 그 예외가 진짜 원인을 덮지 않도록 원래 예외에 덧붙인다.
     */
    private static void disposeAfterFailure(Graphics2D g2d, Throwable failure) {
        try {
            g2d.dispose();
        } catch (RuntimeException disposeFailure) {
            failure.addSuppressed(disposeFailure);
        }
    }

    void save(File file, String title) throws IOException {
        PDDocumentInformation info = document.getDocumentInformation();
        info.setTitle(title);
        info.setProducer(PRODUCER);
        info.setCreator(PRODUCER);
        document.save(file);
    }

    /** 문서를 닫은 뒤 폰트 자원을 닫는다 (저장 중 서브셋을 만들 때 폰트 파일을 다시 읽기 때문). */
    @Override
    public void close() throws IOException {
        try {
            document.close();
        } finally {
            fonts.close();
        }
    }
}
