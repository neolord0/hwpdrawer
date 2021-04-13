package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.control.ControlDrawer;
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

    private HWPDrawer() {
        option = null;
    }

    private void drawFile(HWPFile hwpFile, DrawingOption option) throws Exception {
        this.option = option;

        Convertor.option(option);
        Painter.singleObject()
                .reset()
                .option(option);

        DrawingInfo info = new DrawingInfo();
        ControlDrawer.singleObject().info(info);

        info
                .hwpFile(hwpFile);

        for (Section section : info.hwpFile().getBodyText().getSectionList()) {
            drawSection(section, info);
        }

        Painter.singleObject().pageMaker().saveCurrentPage();
    }

    private void drawSection(Section section, DrawingInfo info) throws Exception {
        info.section(section);

        Painter.singleObject().pageMaker().newPage(info);

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer();
        info.startBodyTextParagraphList();

        for (Paragraph paragraph : info.section()) {
            paragraphDrawer.draw(paragraph, info);
        }

        info.endParagraphList();
    }

    private int pageCount() {
        return Painter.singleObject().pageMaker().currentPageNo();
    }

    public DrawingOption option() {
        return option;
    }

}

