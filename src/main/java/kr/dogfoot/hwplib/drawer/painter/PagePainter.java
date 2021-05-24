package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.util.Area;
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
    private final DrawingInput input;
    private final InterimOutput output;
    private final Painter painter;

    public PagePainter(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
        painter = new Painter(input);
    }

    public PagePainter option(DrawingOption option) {
        painter.option(option);
        return this;
    }

    public void saveCurrentPage() throws Exception {
        if (input.pageInfo().header() != null) {
            drawHeader();
        }
        if (input.pageInfo().footer() != null) {
            drawFooter();
        }

        System.out.println(output.page().test(0));

        BufferedImage pageImage = createPageImage();
        painter
                .graphics2D((Graphics2D) pageImage.getGraphics());


        painter.setLineStyle(BorderType.Solid, BorderThickness.MM0_15, new Color4Byte(255, 0, 0));
        for (Area columnArea : output.page().columnAreas()) {
               painter.rectangle(columnArea, false);
        }

        if (output.page().headerOutput() != null) {
            painter.paintContent(output.page().headerOutput().content());
        }
        painter.paintContent(output.page().content());
        if (output.page().footerOutput() != null) {
            painter.paintContent(output.page().footerOutput().content());
        }

        savePngFile(pageImage);
    }

    private void drawHeader() throws Exception {
        output.startHeader();
        input.startControlParaList(input.pageInfo().headerArea().widthHeight(),
                input.pageInfo().header().getParagraphList().getParagraphs());
        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(input, output);

        while (input.nextPara()) {
            paragraphDrawer.draw(false);
        }

        input.endControlParaList();
        output.endHeader();
    }


    private void drawFooter() throws Exception {
        FooterOutput footerOutput = output.startFooter();
        input.startControlParaList(input.pageInfo().footerArea().widthHeight(),
                input.pageInfo().footer().getParagraphList().getParagraphs());

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(input, output);
        while (input.nextPara()) {
            paragraphDrawer.draw(false);
        }

        long calculatedContentHeight = input.endControlParaList();
        footerOutput.calculatedContentHeight(calculatedContentHeight);

        output.endFooter();
    }


    private BufferedImage createPageImage() {
        BufferedImage pageImage = new BufferedImage(
                Convertor.fromHWPUnit(output.page().paperArea().width()),
                Convertor.fromHWPUnit(output.page().paperArea().height()),
                BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = (Graphics2D) pageImage.getGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, pageImage.getWidth(), pageImage.getHeight());

        return pageImage;
    }

    private void savePngFile(BufferedImage pageImage) throws IOException {
        File outputFile = new File(painter.option().directoryToSave(), "page" + input.pageInfo().pageNo() + ".png");
        ImageIO.write(pageImage, "png", outputFile);
    }
}
