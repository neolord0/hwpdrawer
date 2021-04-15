package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
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
    private PagePainter pagePainter;


    private HWPDrawer() {
        option = null;
        info = null;
        pagePainter = null;
    }

    private void drawFile(HWPFile hwpFile, DrawingOption option) throws Exception {
        this.option = option;
        Convertor.option(option);

        info = new DrawingInfo()
                .hwpFile(hwpFile);

        pagePainter = new PagePainter(info)
                .option(option);


        for (Section section : info.hwpFile().getBodyText().getSectionList()) {
            drawSection(section);
        }

        pagePainter.saveCurrentPage();
    }

    private void drawSection(Section section) throws Exception {
        info
                .section(section)
                .newPage()
                .startBodyTextParagraphList();

        ParagraphDrawer paragraphDrawer = new ParagraphDrawer(pagePainter, info);
        for (Paragraph paragraph : info.section()) {
            paragraphDrawer.draw(paragraph);
        }

        info.endParagraphList();
    }

    private int pageCount() {
        return pagePainter.currentPageNo();
    }
}

