package kr.dogfoot.hwplib.drawer.multicolumn;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class GeneralColumnTest {
    @Test
    public void test_기본_같은크기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "기본" + File.separator + "같은크기");
    }

    @Test
    public void test_기본_다른크기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "기본" + File.separator + "다른크기");
    }

    @Test
    public void test_방향_오른쪽부터() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "방향" + File.separator + "오른쪽부터");
    }

    @Test
    public void test_방향_맞쪽() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "방향" + File.separator + "맞쪽");
    }

    @Test
    public void test_단나누기_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "단나누기" + File.separator + "1");
    }

    @Test
    public void test_단나누기_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "단나누기" + File.separator + "2");
    }

    @Test
    public void test_일반_단나누기설정_1() throws Exception {
        HWPTester.test("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "단나누기설정" + File.separator + "1");
    }

    @Test
    public void test_일반_단나누기설정_2() throws Exception {
        HWPTester.test("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "단나누기설정" + File.separator + "2");
    }
}
