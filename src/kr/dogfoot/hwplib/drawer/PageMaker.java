package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.UnitConvertor;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PageMaker {
    private DrawingInfo info;

    private BufferedImage currentPageImage;
    private Graphics graphics;

    private int currentPageNo;

    public PageMaker(DrawingInfo info) {
        this.info = info;
    }

    public void newPage() throws IOException {
        if (haveCurrentPage()) {
            saveCurrentPage();
        }
        createNewPage();
    }

    private boolean haveCurrentPage() {
        return currentPageImage != null;
    }

    public void saveCurrentPage() throws IOException {
        currentPageNo++;

        File outputFile = new File(info.option().directoryToSave(), "page" + currentPageNo + ".png");
        ImageIO.write(currentPageImage, "png", outputFile);
    }

    private void createNewPage() {
        currentPageImage = new BufferedImage(
                UnitConvertor.fromHWPUnit(info.currentPageDef().getPaperWidth()),
                UnitConvertor.fromHWPUnit(info.currentPageDef().getPaperHeight()),
                BufferedImage.TYPE_INT_RGB);
        graphics = currentPageImage.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, currentPageImage.getWidth(), currentPageImage.getHeight());
        graphics.setColor(Color.RED);

        info.pageDrawArea().toConvertedRectangle();
        Rectangle rectangle = info.pageDrawArea().toConvertedRectangle();
        graphics.drawRect(rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }
}
