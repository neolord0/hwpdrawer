package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.ParaListDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.RedrawException;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;

public class HWPDrawer {
    public static int draw(HWPFile hwpFile, DrawingOption option) throws Exception {
        FontManager.object().hwpFile(hwpFile);

        HWPDrawer drawer = new HWPDrawer();
        drawer.drawFile(hwpFile, option);

        FontManager.object().clear();
        return drawer.pageCount();
    }

    private DrawingOption option;
    private DrawingInput input;
    private InterimOutput output;
    private PagePainter pagePainter;

    private HWPDrawer() {
        option = null;
        input = null;
        output = null;
        pagePainter = null;
    }

    private void drawFile(HWPFile hwpFile, DrawingOption option) throws Exception {
        this.option = option;
        Convertor.option(option);

        input = new DrawingInput()
                .hwpFile(hwpFile);
        output = new InterimOutput();

        pagePainter = new PagePainter(input, output)
                .option(option);

        for (Section section : input.hwpFile().getBodyText().getSectionList()) {
            drawSection(section);
        }

        pagePainter.saveCurrentPage();
        if (output.hasControlMovedToNextPage()) {
            input.columnsInfo().reset();
            input.pageInfo()
                    .increasePageNo();

            output.newPageOutput(input);
            pagePainter.saveCurrentPage();
        }
    }

    private void drawSection(Section section) throws Exception {
        input
                .section(section)
                .newPage();

        output.newPageOutput(input);

        ParaListDrawer paraListDrawer = new ParaListDrawer(input, output, pagePainter);
        paraListDrawer.drawForBodyText(section);
    }

    private int pageCount() {
        return input.pageInfo().pageNo();
    }
}

