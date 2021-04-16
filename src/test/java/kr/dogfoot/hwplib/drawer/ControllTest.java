package kr.dogfoot.hwplib.drawer;

import org.junit.Test;

import java.io.File;

public class ControllTest {
    @Test
    public void test_위치() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "위치");
    }

    @Test
    public void test_ZOrder() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "ZOrder");
    }

    @Test
    public void test_본문과의_배치_앞으로_뒤로() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "앞으로_뒤로");
    }
    @Test
    public void test_본문과의_배치_자리차지1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "자리차지1");
    }

    @Test
    public void test_본문과의_배치_자리차지2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "자리차지2");
    }

    @Test
    public void test_본문과의_배치_어울림1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림1");
    }

    @Test
    public void test_본문과의_배치_어울림2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림2");
    }

    @Test
    public void test_본문과의_배치_어울림3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림3");
    }

    @Test
    public void test_본문과의_배치_어울림4() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림4");
    }

    @Test
    public void test_본문과의_배치_어울림5() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림5");
    }

    @Test
    public void test_본문과의_배치_어울림6() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림6");
    }

    @Test
    public void test_본문과의_배치_어울림_자리차지() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림_자리차지");
    }

    @Test
    public void test_글자처럼() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글자처럼" + File.separator + "1");
    }

    @Test
    public void test_글상자_일반() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글상자" + File.separator + "일반");
    }

    @Test
    public void test_글상자_세로정렬() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글상자" + File.separator + "세로정렬");
    }
}
