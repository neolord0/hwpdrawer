package kr.dogfoot.hwplib.drawer.multicolumn;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class DivideColumnTest {
    @Test
    public void test_기본_2() throws Exception {
        HWPTester.test("testingHWP" + File.separator + "다단" + File.separator + "배분" + File.separator + "기본" + File.separator + "2");
    }
}
