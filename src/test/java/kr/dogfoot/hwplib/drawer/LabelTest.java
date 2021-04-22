package kr.dogfoot.hwplib.drawer;

import org.junit.Test;

import java.io.File;

public class LabelTest {
    @Test
    public void test_글상자_일반() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글상자" + File.separator + "일반");
    }

    @Test
    public void test_글상자_세로정렬() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글상자" + File.separator + "세로정렬");
    }
}
