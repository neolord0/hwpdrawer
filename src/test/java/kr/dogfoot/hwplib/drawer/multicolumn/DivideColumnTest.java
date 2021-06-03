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
    public void test_기본_3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "기본" + File.separator + "3");
    }

    @Test
    public void test_기본_4() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "기본" + File.separator + "4");
    }
}
