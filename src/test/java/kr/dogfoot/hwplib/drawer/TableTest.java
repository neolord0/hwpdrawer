package kr.dogfoot.hwplib.drawer;

import org.junit.Test;

import java.io.File;

public class TableTest {
    @Test
    public void test_표_일반() throws Exception {
        HWPTester.test("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "일반");
    }
}
