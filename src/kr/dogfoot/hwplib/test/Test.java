package kr.dogfoot.hwplib.test;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.drawer.util.FontManager;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;

public class Test {
    public static void main(String[] arg) throws Exception {
        long startTime = System.currentTimeMillis();
        HWPFile hwpFile = HWPReader.fromFile("test/글자모양/밑줄취소선/test.hwp");

        HWPDrawer.draw(hwpFile,
                new DrawingOption()
                        .directoryToSave("test/글자모양/밑줄취소선")
                        .zoomRate(100)
        );

        long endTime = System.currentTimeMillis();
        System.out.println("Processing time :" + (endTime - startTime));
    }
}
