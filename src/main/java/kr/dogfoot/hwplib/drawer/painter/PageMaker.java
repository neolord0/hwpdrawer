package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.control.ControlClassifier;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.TreeSet;

public class PageMaker {
    private Painter painter;
    private BufferedImage currentPageImage;
    private TreeSet<ControlClassifier.ControlInfo> controlsForFrontInPage;
    private int currentPageNo;

    public PageMaker(Painter painter) {
        this.painter = painter;
        controlsForFrontInPage = new TreeSet<>();
    }

    public void reset() {
        currentPageImage = null;
        controlsForFrontInPage.clear();
        currentPageNo = 0;
    }

    public void addFrontControls(TreeSet<ControlClassifier.ControlInfo> controlsForFront) {
        for (ControlClassifier.ControlInfo controlInfo : controlsForFront) {
            controlsForFrontInPage.add(controlInfo);
        }
    }


    public void newPage(DrawingInfo info) throws Exception {
        if (haveCurrentPage()) {
            saveCurrentPage();
        }

        createNewPage(info);
        info.processAtNewPage();
    }

    private boolean haveCurrentPage() {
        return currentPageImage != null;
    }

    public void saveCurrentPage() throws Exception {
        ControlDrawer.singleObject().drawControls(controlsForFrontInPage);
        controlsForFrontInPage.clear();

        currentPageNo++;
        File outputFile = new File(painter.option().directoryToSave(), "page" + currentPageNo + ".png");
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

        painter
                .graphics2D(graphics)
                .setLineStyle(BorderType.Solid, BorderThickness.MM0_15, new Color4Byte(255,0, 0))
                .rectangle(info.pageArea(), false);

    }

    public int currentPageNo() {
        return currentPageNo;
    }

}
