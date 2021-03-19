package kr.dogfoot.hwplib.drawer;

import kr.dogfoot.hwplib.drawer.util.FontLoader;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;

public class HWPDrawer {
    public static void draw(HWPFile hwpFile, DrawingOption option) throws Exception {
        FontManager.object().hwpFile(hwpFile);

        HWPDrawer drawer = new HWPDrawer();
        drawer.drawFile(hwpFile, option);

        FontManager.object().clear();
    }

    private DrawingInfo info;

    private HWPDrawer() {
        info = new DrawingInfo();
    }

    private void drawFile(HWPFile hwpFile, DrawingOption option) throws Exception {
        info
                .hwpFile(hwpFile)
                .option(option);

        for (Section section : info.hwpFile().getBodyText().getSectionList()) {
            drawSection(section);
        }

        info.pageMaker().saveCurrentPage();
    }

    private void drawSection(Section section) throws Exception {
        info.section(section);
        info.newPage();

        for (Paragraph paragraph : info.section()) {
            info.paragraphDrawer()
                    .initialize(paragraph, true)
                    .draw();
        }
    }
}
