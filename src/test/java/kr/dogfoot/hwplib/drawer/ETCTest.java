package kr.dogfoot.hwplib.drawer;

import org.junit.Test;

import java.io.File;

public class ETCTest {
    @Test
    public void test_빈_줄_감추기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "기타" + File.separator + "빈 줄 감추기");
    }

    @Test
    public void test_헤더풋터() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "기타" + File.separator + "헤더풋터");
    }
}
