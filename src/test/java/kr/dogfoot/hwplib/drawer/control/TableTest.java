package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class TableTest {
    @Test
    public void test_일반() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "일반");
    }

    @Test
    public void test_셀안에컨트롤_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "셀안에컨트롤" + File.separator + "1");
    }

    @Test
    public void test_셀안에컨트롤_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "셀안에컨트롤" + File.separator + "2");
    }

    @Test
    public void test_셀테두리() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "셀테두리");
    }

    @Test
    public void test_표나누기_셀나누기_기본1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "기본1");
    }

    @Test
    public void test_표나누기_셀나누기_기본2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "기본2");
    }

    @Test
    public void test_표나누기_셀나누기_빈셀() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "빈셀");
    }

    @Test
    public void test_표나누기_셀나누기_병합된셀1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "병합된셀1");
    }

    @Test
    public void test_표나누기_셀나누기_병합된셀2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "표" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "병합된셀2");
    }
}
