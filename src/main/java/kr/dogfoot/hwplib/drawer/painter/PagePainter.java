package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.drawinginfo.interims.FooterOutput;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class PagePainter {
    private DrawingInfo info;

    private Painter painter;

    public PagePainter(DrawingInfo info) {
        this.info = info;
        painter = new Painter(info);
    }

    public PagePainter option(DrawingOption option) {
        painter.option(option);
        return this;
    }

    public void saveCurrentPage() throws Exception {
        if (info.pageInfo().header() != null) {
            drawHeader();
        }
        if (info.pageInfo().footer() != null) {
            drawFooter();
        }

        System.out.println(info.output().page().test(0));

        BufferedImage pageImage = createPageImage();
        painter
                .graphics2D((Graphics2D) pageImage.getGraphics());

        painter
                .setLineStyle(BorderType.Solid, BorderThickness.MM0_15, new Color4Byte(255, 0, 0))
                .rectangle(info.pageOutput().bodyArea(), false);

        if (info.pageOutput().headerOutput() != null) {
            painter.paintContent(info.pageOutput().headerOutput().content());
        }
        painter.paintContent(info.pageOutput().content());
        if (info.pageOutput().footerOutput() != null) {
            painter.paintContent(info.pageOutput().footerOutput().content());
        }

        savePngFile(pageImage);
    }

    private void drawHeader() throws Exception {
        info.output().startHeader();
        info.startControlParagraphList(info.pageInfo().headerArea().widthHeight());
        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);

        for (Paragraph paragraph : info.pageInfo().header().getParagraphList()) {
            paragraphDrawer.draw(paragraph);
        }

        info.endControlParagraphList();
        info.output().endHeader();
    }


    private void drawFooter() throws Exception {
        FooterOutput footerOutput = info.output().startFooter();
        info.startControlParagraphList(info.pageInfo().footerArea().widthHeight());

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(info);
        for (Paragraph paragraph : info.pageInfo().footer().getParagraphList()) {
            paragraphDrawer.draw(paragraph);
        }

        long calculatedContentHeight = info.endControlParagraphList();
        footerOutput.calculatedContentHeight(calculatedContentHeight);

        info.output().endFooter();
    }


    private BufferedImage createPageImage() {
        BufferedImage pageImage = new BufferedImage(
                Convertor.fromHWPUnit(info.pageOutput().paperArea().width()),
                Convertor.fromHWPUnit(info.pageOutput().paperArea().height()),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = (Graphics2D) pageImage.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, pageImage.getWidth(), pageImage.getHeight());

        return pageImage;
    }

    private void savePngFile(BufferedImage pageImage) throws IOException {
        File outputFile = new File(painter.option().directoryToSave(), "page" + info.pageInfo().pageNo() + ".png");
        ImageIO.write(pageImage, "png", outputFile);
    }
}
