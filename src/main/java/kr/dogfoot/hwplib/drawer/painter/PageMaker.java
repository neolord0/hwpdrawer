package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.HWPDrawer;
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

public class PageMaker {
    private HWPDrawer drawer;
    private BufferedImage currentPageImage;

    private int currentPageNo;

    public PageMaker(HWPDrawer drawer) {
        this.drawer = drawer;
    }

    public void newPage(DrawingInfo info) throws IOException {
        if (haveCurrentPage()) {

            saveCurrentPage();
        }

        createNewPage(info);
        info.processAtNewPage();
    }

    private boolean haveCurrentPage() {
        return currentPageImage != null;
    }

    public void saveCurrentPage() throws IOException {
       drawer.controlDrawer()
                .drawControlsForSquare()
                .removeControlsForSquare();

        currentPageNo++;
        File outputFile = new File(drawer.option().directoryToSave(), "page" + currentPageNo + ".png");
        ImageIO.write(currentPageImage, "png", outputFile);
    }

    private void createNewPage(DrawingInfo info) {
        currentPageImage = new BufferedImage(
                Convertor.fromHWPUnit(info.paperArea().width()),
                Convertor.fromHWPUnit(info.paperArea().height()),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = (Graphics2D) currentPageImage.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, currentPageImage.getWidth(), currentPageImage.getHeight());

        drawer.painter()
                .graphics2D(graphics)
                .setLineStyle(BorderType.Solid, BorderThickness.MM0_15, new Color4Byte(255,0, 0))
                .rectangle(info.pageArea(), false);

    }

    public int currentPageNo() {
        return currentPageNo;
    }
}
