package kr.dogfoot.hwpdrawer.multicolumn;

import kr.dogfoot.hwpdrawer.util.HWPTester;
import org.junit.Test;

import java.io.File;

public class ParallelColumnTest {
    @Test
    public void test_기본() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "평행" + File.separator + "기본");
    }

    @Test
    public void test_다단나누기설정() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "평행" + File.separator + "다단나누기설정");
    }

    @Test
    public void test_페이지넘어1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "평행" + File.separator + "페이지넘어" + File.separator + "1");
    }

    @Test
    public void test_페이지넘어2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "평행" + File.separator + "페이지넘어" + File.separator + "2");
    }

    @Test
    public void test_페이지넘어3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "평행" + File.separator + "페이지넘어" + File.separator + "3");
    }
}
