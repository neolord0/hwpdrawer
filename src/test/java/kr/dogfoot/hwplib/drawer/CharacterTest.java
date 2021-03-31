package kr.dogfoot.hwplib.drawer;

import org.junit.Test;
import java.io.File;

public class CharacterTest {
    @Test
    public void test_글자모양_긒자위치() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "글자모양" + File.separator + "글자위치");
    }

    @Test
    public void test_글자모양_글자크기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "글자모양" + File.separator + "글자크기");
    }

    @Test
    public void test_글자모양_밑줄취소선() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "글자모양" + File.separator + "밑줄취소선");
    }

    @Test
    public void test_글자모양_상대크기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "글자모양" + File.separator + "상대크기");
    }

    @Test
    public void test_글자모양_자간() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "글자모양" + File.separator + "자간");
    }

    @Test
    public void test_글자모양_장평() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "글자모양" + File.separator + "장평");
    }

    @Test
    public void test_글자모양_글자색_크기() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "글자모양" + File.separator + "글자색_크기");
    }
}
