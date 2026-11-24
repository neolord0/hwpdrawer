package kr.dogfoot.hwpdrawer.export.image;

import kr.dogfoot.hwpdrawer.document.page.Page;
import kr.dogfoot.hwpdrawer.export.DocumentExporter;
import kr.dogfoot.hwpdrawer.export.ExportTarget;
import kr.dogfoot.hwpdrawer.export.PageGeometry;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

/**
 * 페이지마다 PNG 파일 하나를 만든다: {@code {접두어}{쪽 번호 3자리}.png}.
 */
public class ImageExporter implements DocumentExporter {
    private static final String EXTENSION = "png";

    private final PagePainter painter;
    private final ExportTarget target;

    public ImageExporter(HWPFileReader reader, ExportTarget target) {
        this.painter = new PagePainter(reader);
        this.target = target;
    }

    @Override
    public void export(List<Page> pages) throws IOException {
        target.prepareDirectory();
        for (Page page : pages) {
            exportPage(page);
        }
    }

    private void exportPage(Page page) throws IOException {
        PageGeometry geometry = PageGeometry.of(page);
        if (!geometry.hasArea()) return;

        BufferedImage image = render(page, geometry);
        ImageIO.write(image, EXTENSION, target.pageFile(page.pageNo(), EXTENSION));
    }

    private BufferedImage render(Page page, PageGeometry geometry) {
        int width = geometry.widthPx();
        int height = geometry.heightPx();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();
        try {
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, width, height);
            RasterRendering.applyHints(g2d);
            painter.paint(page, g2d);
        } finally {
            g2d.dispose();
        }
        return image;
    }
}
