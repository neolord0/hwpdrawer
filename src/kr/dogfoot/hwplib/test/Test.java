package kr.dogfoot.hwplib.test;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;

import java.io.File;

public class Test {
    public static void main(String[] arg) throws Exception {
        HWPFile hwpFile = HWPReader.fromFile("test" + File.separator + "사업신청서.hwp");

        HWPDrawer.draw(hwpFile,
                new DrawingOption()
                        .directoryToSave("test")
                        .zoomRate(100)
        );
    }
}
