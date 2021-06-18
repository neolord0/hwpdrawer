package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.PageOutput;
import kr.dogfoot.hwplib.drawer.util.Convertor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PagePainter {
    private final DrawingInput input;
    private final InterimOutput output;
    private final Painter painter;
    private int pageCount;

    public PagePainter(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
        painter = new Painter(input);
        pageCount = 0;
    }

    public PagePainter option(DrawingOption option) {
        painter.option(option);
        return this;
    }

    public void saveAllPages() throws Exception {
        for (PageOutput pageOutput : output.pages()) {
            savePage(pageOutput);
        }
    }

    private void savePage(PageOutput pageOutput) throws Exception {
        BufferedImage pageImage = createPageImage(pageOutput);

        // System.out.println(pageOutput.test(0));

        painter
                .graphics2D((Graphics2D) pageImage.getGraphics());

        if (pageOutput.headerOutput() != null) {
            painter.paintContent(pageOutput.headerOutput().content());
        }

        painter.paintContent(pageOutput.content());

        if (pageOutput.footerOutput() != null) {
            painter.paintContent(pageOutput.footerOutput().content());
        }

        savePngFile(pageImage, pageOutput.pageNo());
        pageCount++;
    }

    public void saveCurrentPage() throws Exception {
        savePage(output.currentPage());
    }

    private BufferedImage createPageImage(PageOutput pageOutput) {
        BufferedImage pageImage = new BufferedImage(
                Convertor.fromHWPUnit(pageOutput.paperArea().width()),
                Convertor.fromHWPUnit(pageOutput.paperArea().height()),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = (Graphics2D) pageImage.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, pageImage.getWidth(), pageImage.getHeight());

        return pageImage;
    }

    private void savePngFile(BufferedImage pageImage, int pageNo) throws IOException {
        File outputFile = new File(painter.option().directoryToSave(), "page" + pageNo + ".png");
        ImageIO.write(pageImage, "png", outputFile);
    }

    public int pageCount() {
        return pageCount;
    }
}
