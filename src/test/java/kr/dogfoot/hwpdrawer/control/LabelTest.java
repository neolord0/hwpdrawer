package kr.dogfoot.hwpdrawer.control;

import kr.dogfoot.hwpdrawer.util.HWPTester;
import org.junit.Test;

import java.io.File;

public class LabelTest {
    @Test
    public void test_일반() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글상자" + File.separator + "일반");
    }

    @Test
    public void test_세로정렬() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글상자" + File.separator + "세로정렬");
    }

    @Test
    public void test_글상자안에글상자() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글상자" + File.separator + "글상자안에글상자");
    }
}
