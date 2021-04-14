package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.painter.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.util.Convertor;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;

public class HWPDrawer {
    public static int draw(HWPFile hwpFile, DrawingOption option) throws Exception {
        FontManager.object().hwpFile(hwpFile);

        HWPDrawer drawer = new HWPDrawer();
        drawer.drawFile(hwpFile, option);

        FontManager.object().clear();
        return drawer.pageCount();
    }


    private DrawingOption option;
    private DrawingInfo info;
    private Painter painter;


    private HWPDrawer() {
        option = null;
        info = null;
        painter = null;
    }

    private void drawFile(HWPFile hwpFile, DrawingOption option) throws Exception {
        this.option = option;
        Convertor.option(option);

        info = new DrawingInfo();
        painter = new Painter(info).option(option);

        info
                .hwpFile(hwpFile);

        for (Section section : info.hwpFile().getBodyText().getSectionList()) {
            drawSection(section);
        }

        painter.pageMaker().saveCurrentPage();
    }

    private void drawSection(Section section) throws Exception {
        info.section(section);

        painter.pageMaker().newPage(info);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(painter, info);
        info.startBodyTextParagraphList();

        for (Paragraph paragraph : info.section()) {
            paragraphDrawer.draw(paragraph);
        }

        info.endParagraphList();
    }

    private int pageCount() {
        return painter.pageMaker().currentPageNo();
    }
}

