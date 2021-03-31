package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.control.ControlDrawer;
import kr.dogfoot.hwplib.drawer.drawinginfo.DrawingInfo;
import kr.dogfoot.hwplib.drawer.painter.PageMaker;
import kr.dogfoot.hwplib.drawer.painter.Painter;
import kr.dogfoot.hwplib.drawer.paragraph.ParagraphDrawer;
import kr.dogfoot.hwplib.drawer.paragraph.TextLineDrawer;
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
    private PageMaker pageMaker;
    private ParagraphDrawer paragraphDrawer;
    private Painter painter;
    private TextLineDrawer textLineDrawer;
    private ControlDrawer controlDrawer;

    private HWPDrawer() {
        option = null;
        pageMaker = new PageMaker(this);
        paragraphDrawer = new ParagraphDrawer(this);
        textLineDrawer = new TextLineDrawer(this);
        controlDrawer = new ControlDrawer(this);
        painter = new Painter();
    }

    private void drawFile(HWPFile hwpFile, DrawingOption option) throws Exception {
        this.option = option;

        Convertor.option(option);
        painter.option(option);

        DrawingInfo info = new DrawingInfo();
        info
                .hwpFile(hwpFile);

        for (Section section : info.hwpFile().getBodyText().getSectionList()) {
            drawSection(section, info);
        }

        controlDrawer
                .drawControlsForFront()
                .removeControlsForFront();

        pageMaker.saveCurrentPage();
    }

    private void drawSection(Section section, DrawingInfo info) throws Exception {
        info.section(section);

        pageMaker.newPage(info);

        info.startParagraphList(true);

        for (Paragraph paragraph : info.section()) {
            paragraphDrawer.draw(paragraph, info);
        }

        info.endParagraphList();
    }

    private int pageCount() {
        return pageMaker.currentPageNo();
    }

    public DrawingOption option() {
        return option;
    }

    public PageMaker pageMaker() {
        return pageMaker;
    }

    public ParagraphDrawer paragraphDrawer() {
        return paragraphDrawer;
    }

    public TextLineDrawer textLineDrawer() {
        return textLineDrawer;
    }

    public ControlDrawer controlDrawer() {
        return controlDrawer;
    }

    public Painter painter() {
        return painter;
    }
}

