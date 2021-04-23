package kr.dogfoot.hwplib.drawer;

import org.junit.Test;

import java.io.File;

public class ParagraphTest {
    @Test
    public void test_문단모양_여러문단() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "여러 문단");
    }

    @Test
    public void test_문단모양_줄간격() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "줄간격");
    }

    @Test
    public void test_문단모양_줄나눔() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "줄나눔");
    }

    @Test
    public void test_문단모양_줄나눔2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "줄나눔2");
    }

    @Test
    public void test_문단모양_줄나눔3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "줄나눔3");
    }

    @Test
    public void test_문단모양_최소공백() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "최소공백");
    }

    @Test
    public void test_문단모양_가로정렬() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "가로정렬");
    }

    @Test
    public void test_문단모양_왼쪽오른쪽여백() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "왼쪽오른쪽여백");
    }

    @Test
    public void test_문단모양_들여쓰기내어쓰기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "문단모양" + File.separator + "들여쓰기내어쓰기");
    }

}
