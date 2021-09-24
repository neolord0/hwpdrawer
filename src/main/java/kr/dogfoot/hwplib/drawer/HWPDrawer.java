package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.drawer.paralist.ParaListDrawerForBodyText;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.painter.html.PagePainterForHTML;
import kr.dogfoot.hwplib.drawer.painter.image.PagePainterForImage;
import kr.dogfoot.hwplib.drawer.drawer.paralist.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.drawer.util.FontLoader;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;

public class HWPDrawer {
    public static int draw(HWPFile hwpFile, DrawingOption option) throws Exception {
        FontLoader.singleObject().fontPath(option.fontPath());
        FontManager.object().hwpFile(hwpFile);

        HWPDrawer drawer = new HWPDrawer();
        drawer.drawFile(hwpFile, option);

        FontManager.object().clear();
        return drawer.pageCount();
    }

    private DrawingInput input;
    private InterimOutput output;

    private int pageCount;

    private HWPDrawer() {
        input = null;
        output = null;
    }

    private void drawFile(HWPFile hwpFile, DrawingOption option) throws Exception {
        Convertor.option(option);

        input = new DrawingInput()
                .hwpFile(hwpFile);
        output = new InterimOutput();


        for (Section section : input.hwpFile().getBodyText().getSectionList()) {
            drawSection(section);
        }

        while (output.hasDrawingControls()) {
            drawAddedPage();
        }

        output(option);
    }

    private void drawSection(Section section) throws Exception {
        input.section(section);

        new ParaListDrawerForBodyText(input, output).draw(section);
    }

    private void drawAddedPage() throws Exception {
        input.pageInfo().increasePageNo();
        output.addEmptyPage(input);

        ParaListDrawer.drawHeaderFooter(input, output);
        output.addDrawingControls();
    }

    private void output(DrawingOption option) throws Exception {
        switch (option.outputType()) {
            case Image: {
                PagePainterForImage pagePainter = new PagePainterForImage(input, output)
                        .option(option);
                pagePainter.saveAllPages();
                pageCount = pagePainter.pageCount();
            }
            break;
            case HTML: {
                PagePainterForHTML pagePainter = new PagePainterForHTML(input, output)
                        .option(option);
                pagePainter.saveAllPages();
                pageCount = pagePainter.pageCount();
            }
            break;
        }
    }

    private int pageCount() {
        return pageCount;
    }
}

