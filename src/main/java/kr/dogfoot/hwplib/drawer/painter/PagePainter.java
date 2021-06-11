package kr.dogfoot.hwplib.drawer.painter;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.paragraph.ParaListDrawer;
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

   //     System.out.println(output.page().test(0));

        BufferedImage pageImage = createPageImage();
        painter
                .graphics2D((Graphics2D) pageImage.getGraphics());

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

        ParaListDrawer paraListDrawer = new ParaListDrawer(input, output);
        paraListDrawer.drawForControl(input.pageInfo().header().getParagraphList(), input.pageInfo().headerArea().widthHeight());

        output.endHeader();
    }

    private void drawFooter() throws Exception {
        FooterOutput footerOutput = output.startFooter();

        ParaListDrawer paraListDrawer = new ParaListDrawer(input, output);
        long calculatedContentHeight = paraListDrawer.drawForControl(input.pageInfo().footer().getParagraphList(), input.pageInfo().footerArea().widthHeight());

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
