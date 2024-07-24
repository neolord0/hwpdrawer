package kr.dogfoot.hwpdrawer.multicolumn;

import kr.dogfoot.hwpdrawer.util.HWPTester;
import org.junit.Test;

import java.io.File;

public class NormalColumnTest {
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
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "단나누기" + File.separator + "1");
    }

    @Test
    public void test_단나누기_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "단나누기" + File.separator + "2");
    }

    @Test
    public void test_다단설정나누기_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "다단설정나누기" + File.separator + "1");
    }

    @Test
    public void test_다단설정나누기_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "다단설정나누기" + File.separator + "2");
    }

    @Test
    public void test_다단설정나누기_3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "다단설정나누기" + File.separator + "3");
    }

    @Test
    public void test_컨트롤포함_단기준으로_앞으로() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "컨트롤 포함" + File.separator + "단기준으로" + File.separator + "앞으로");
    }

    @Test
    public void test_컨트롤포함_단기준으로_어울림_자리차지() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "컨트롤 포함" + File.separator + "단기준으로" + File.separator + "어울림_자리차지");
    }

    @Test
    public void test_컨트롤안_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "컨트롤 안" + File.separator + "1");
    }

    @Test
    public void test_컨트롤안_컨트롤높이보다큰다단_1() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "컨트롤 안" + File.separator + "컨트롤높이보다 큰 다단" + File.separator + "1");
    }

    @Test
    public void test_컨트롤안_컨트롤높이보다큰다단_2() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "컨트롤 안" + File.separator + "컨트롤높이보다 큰 다단" + File.separator + "2");
    }

    @Test
    public void test_컨트롤안_컨트롤높이보다큰다단_3() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "컨트롤 안" + File.separator + "컨트롤높이보다 큰 다단" + File.separator + "3");
    }

    @Test
    public void test_컨트롤안_컨트롤높이보다큰다단_4() throws Exception {
        HWPTester.testAndCompare("testingHWP" + File.separator + "다단" + File.separator + "일반" + File.separator + "컨트롤 안" + File.separator + "컨트롤높이보다 큰 다단" + File.separator + "4");
    }
}
