package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class TableTest {
    @Test
    public void test_표_일반() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "일반");
    }

    @Test
    public void test_표_셀안에컨트롤_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "셀안에컨트롤" + File.separator + "1");
    }

    @Test
    public void test_표_셀안에컨트롤_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "셀안에컨트롤" + File.separator + "2");
    }

    @Test
    public void test_표_셀테두리() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "셀테두리");
    }
}
