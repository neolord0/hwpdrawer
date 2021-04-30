package kr.dogfoot.hwplib.drawer;

import org.junit.Test;

import java.io.File;

public class TableTest {
    @Test
    public void test_표_일반() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "일반");
    }

    @Test
    public void test_표_셀안에컨트롤() throws Exception {
        HWPTester.test("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "셀안에컨트롤");
    }
}
