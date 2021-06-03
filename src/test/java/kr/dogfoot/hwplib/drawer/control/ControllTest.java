package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.HWPTester;
import org.junit.Test;

import java.io.File;

public class ControllTest {
    @Test
    public void test_위치1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "위치" + File.separator + "1");
    }

    @Test
    public void test_위치2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "위치" + File.separator + "2");
    }

    @Test
    public void test_위치3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "위치" + File.separator + "3");
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
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "자리차지" + File.separator + "1");
    }

    @Test
    public void test_본문과의_배치_자리차지2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "자리차지" + File.separator + "2");
    }

    @Test
    public void test_본문과의_배치_어울림1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "1");
    }

    @Test
    public void test_본문과의_배치_어울림2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "2");
    }

    @Test
    public void test_본문과의_배치_어울림3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "3");
    }

    @Test
    public void test_본문과의_배치_어울림4() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "4");
    }

    @Test
    public void test_본문과의_배치_어울림5() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "5");
    }

    @Test
    public void test_본문과의_배치_어울림6() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "6");
    }

    @Test
    public void test_본문과의_배치_어울림7() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "7");
    }

    @Test
    public void test_본문과의_배치_어울림8() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림" + File.separator + "8");
    }

    @Test
    public void test_본문과의_배치_어울림_자리차지() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "본문과의 배치" + File.separator + "어울림_자리차지");
    }

    @Test
    public void test_글자처럼() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "글자처럼");
    }

    @Test
    public void test_이미지1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "컨트롤" + File.separator + "이미지");
    }
}
