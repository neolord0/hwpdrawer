package kr.dogfoot.hwplib.drawer;

import com.github.romankh3.image.comparison.ImageComparison;
import com.github.romankh3.image.comparison.ImageComparisonUtil;
import com.github.romankh3.image.comparison.model.ImageComparisonResult;
import com.github.romankh3.image.comparison.model.ImageComparisonState;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;
import org.junit.Assert;

import java.awt.image.BufferedImage;
import java.io.File;

public class HWPTester {
    public static void testAndCompare(String path) throws Exception {
        long startTime = System.currentTimeMillis();
        HWPFile hwpFile = HWPReader.fromFile(path + "/test.hwp");

        int pageCount = HWPDrawer.draw(hwpFile,
                new DrawingOption()
                        .directoryToSave(path)
                        .zoomRate(80));

        long endTime = System.currentTimeMillis();
        System.out.println(path + " : " + (endTime - startTime) + "ms, pageCount = " + pageCount);
        comparePageImages(path, pageCount);
    }


    private static void comparePageImages(String path, int pageCount) {
        boolean succuess = true;
        for (int pageNo = 1; pageNo <= pageCount; pageNo++) {
            String expectedPng = path + File.separator + "ok_page" + pageNo + ".png";
            String actualPng = path + File.separator + "page" + pageNo + ".png";

            BufferedImage expectedImage = ImageComparisonUtil.readImageFromResources(expectedPng);
            BufferedImage actualImage = ImageComparisonUtil.readImageFromResources(actualPng);

            ImageComparisonResult result = new ImageComparison(expectedImage, actualImage).compareImages();
            if (result.getImageComparisonState() != ImageComparisonState.MATCH) {
                System.out.println("not match page " + pageNo + " " + result.getImageComparisonState());
                ImageComparisonUtil.saveImage(new File(path + File.separator + "error" + pageNo + ".png"), result.getResult());
                succuess = false;
            } else {
                System.out.println("match page " + pageNo);
            }
        }
            Assert.assertTrue(succuess);
    }

    public static void test(String path) throws Exception {
        long startTime = System.currentTimeMillis();
        HWPFile hwpFile = HWPReader.fromFile(path + "/test.hwp");

        int pageCount = HWPDrawer.draw(hwpFile,
                new DrawingOption()
                        .directoryToSave(path)
                        .zoomRate(80));

        long endTime = System.currentTimeMillis();
        System.out.println(path + " : " + (endTime - startTime) + "ms, pageCount = " + pageCount);
    }
}
