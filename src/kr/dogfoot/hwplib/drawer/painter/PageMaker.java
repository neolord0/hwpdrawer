package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageDef;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PageMaker {
    private DrawingInfo info;
    private PageDef pageDef;

    private BufferedImage currentPageImage;

    private int currentPageNo;

    private Area pageDrawArea;

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
                Convertor.fromHWPUnit(pageDef.getPaperWidth()),
                Convertor.fromHWPUnit(pageDef.getPaperHeight()),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = (Graphics2D) currentPageImage.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, currentPageImage.getWidth(), currentPageImage.getHeight());

        info.painter().graphics2D(graphics);
    }

    public PageDef pageDef() {
        return pageDef;
    }

    public void pageDef(PageDef pageDef) {
        this.pageDef = pageDef;
        calculatePageDrawArea();
    }

     private void calculatePageDrawArea() {
        pageDrawArea = new Area(0, 0, pageDef.getPaperWidth(), pageDef.getPaperHeight())
                .applyMargin(pageDef.getLeftMargin(),
                        pageDef.getTopMargin(),
                        pageDef.getRightMargin(),
                        pageDef.getBottomMargin())
                .applyMargin(0,
                        pageDef.getHeaderMargin(),
                        0,
                        pageDef.getFooterMargin());

    }

    public Area pageDrawArea() {
        return pageDrawArea;
    }
}
