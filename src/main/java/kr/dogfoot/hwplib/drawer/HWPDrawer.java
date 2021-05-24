package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
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
    }

    private void drawSection(Section section) throws Exception {
        input
                .section(section)
                .startBodyTextParaList(section.getParagraphs())
                .newPage();

        output.newPageOutput(input.pageInfo());

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(input, output, pagePainter);
        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                paragraphDrawer.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                System.out.println("exception ");
                input.gotoParaCharPosition(e.paraIndex(), e.charIndex(), e.charPosition());
                input.currentParaListInfo().resetParaStartY(e.startY());
                redraw = true;
            }
        }

        input.endBodyTextParaList();
    }

    private int pageCount() {
        return input.pageInfo().pageNo();
    }
}

