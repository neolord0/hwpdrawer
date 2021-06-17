package kr.dogfoot.hwplib.drawer.multicolumn;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class ParallelColumnTest {
    @Test
    public void test_페이지넘어() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "평행" + File.separator + "페이지넘어");
    }
}
