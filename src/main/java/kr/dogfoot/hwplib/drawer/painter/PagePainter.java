package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PagePainter {
    private DrawingInfo info;

    private Painter painter;
    private int currentPageNo;

    public PagePainter(DrawingInfo info) {
        this.info = info;
        painter = new Painter(info);
        currentPageNo = 0;
    }

    public PagePainter option(DrawingOption option) {
        painter.option(option);
        return this;
    }

    public void saveCurrentPage() throws Exception {
        BufferedImage pageImage = createPageImage();
        painter
                .graphics2D((Graphics2D) pageImage.getGraphics())
                .setLineStyle(BorderType.Solid, BorderThickness.MM0_15, new Color4Byte(255, 0, 0))
                .rectangle(info.page().pageArea(), false);

        paintPage();

        savePngFile(pageImage);
    }

    private BufferedImage createPageImage() {
        BufferedImage pageImage = new BufferedImage(
                Convertor.fromHWPUnit(info.page().paperArea().width()),
                Convertor.fromHWPUnit(info.page().paperArea().height()),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = (Graphics2D) pageImage.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, pageImage.getWidth(), pageImage.getHeight());

        return pageImage;
    }

    private void paintPage() throws Exception {

        painter.controlPainter().paintControls(info.page().behindControls());
        painter.textDrawer().paintTextParts(info.page().textParts());
        painter.controlPainter().paintControls(info.page().notBehindControls());
    }

    private void savePngFile(BufferedImage pageImage) throws IOException {
        currentPageNo++;
        File outputFile = new File(painter.option().directoryToSave(), "page" + currentPageNo + ".png");
        ImageIO.write(pageImage, "png", outputFile);
    }

    public int currentPageNo() {
        return currentPageNo;
    }
}
