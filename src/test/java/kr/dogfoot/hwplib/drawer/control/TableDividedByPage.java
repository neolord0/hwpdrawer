package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class TableDividedByPage {
    @Test
    public void test_셀나누기_기본1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "기본" + File.separator + "1");
    }

    @Test
    public void test_셀나누기_기본2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "기본" + File.separator + "1");
    }

    @Test
    public void test_셀나누기_빈셀() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "빈셀");
    }

    @Test
    public void test_셀나누기_병합된셀1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "병합된셀" + File.separator + "1");
    }

    @Test
    public void test_셀나누기_병합된셀2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "병합된셀" + File.separator + "2");
    }

    @Test
    public void test_셀나누기_걸처진컨트롤1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "걸처진컨트롤" + File.separator + "1");
    }

    @Test
    public void test_셀나누기_걸처진컨트롤2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "걸처진컨트롤" + File.separator + "2");
    }

    @Test
    public void test_셀나누기_두페이지이상1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "두페이지이상" + File.separator + "1");
    }

    @Test
    public void test_셀나누기_다단1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "다단" + File.separator + "1");
    }

    @Test
    public void test_셀나누기_다단2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "다단" + File.separator + "2");
    }

    @Test
    public void test_셀나누기_다단3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "다단" + File.separator + "3");
    }

    @Test
    public void test_셀나누기_뒷페이지에_컨트롤_지워야_하는_경우() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀나누기" + File.separator + "뒷페이지에_컨트롤_지워야_하는_경우");
    }

    @Test
    public void test_셀단위로나누기_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀단위로나누기" + File.separator + "1");
    }

    @Test
    public void test_셀단위로나누기_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀단위로나누기" + File.separator + "2");
    }

    @Test
    public void test_셀단위로나누기_3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀단위로나누기" + File.separator + "3");
    }

    @Test
    public void test_셀단위로나누기_페이지_보다_큰_셀() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "셀단위로나누기" + File.separator + "페이지_보다_큰_셀");
    }

    @Test
    public void test_나누지않음_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "표나누기" + File.separator + "나누지않음" + File.separator + "1");
    }
}
