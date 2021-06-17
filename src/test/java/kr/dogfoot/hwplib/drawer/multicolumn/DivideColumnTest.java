package kr.dogfoot.hwplib.drawer.multicolumn;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class DivideColumnTest {
    @Test
    public void test_기본_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "기본" + File.separator + "1");
    }

    @Test
    public void test_기본_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "기본" + File.separator + "2");
    }

    @Test
    public void test_기본_페이지아래까지() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "기본" + File.separator + "페이지아래까지");
    }

    @Test
    public void test_단나누기_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "단나누기" + File.separator + "1");
    }

    @Test
    public void test_단나누기_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "단나누기" + File.separator + "2");
    }

    @Test
    public void test_다단설정나누기_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "다단설정나누기" + File.separator + "1");
    }

    @Test
    public void test_다단설정나누기_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "다단설정나누기" + File.separator + "2");
    }

    @Test
    public void test_빈다단() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "빈 다단");
    }

    @Test
    public void test_페이지나누기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "페이지나누기");
    }
}

