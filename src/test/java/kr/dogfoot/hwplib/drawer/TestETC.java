package kr.dogfoot.hwplib.drawer;

import org.junit.Test;

import java.io.File;

public class TestETC {
	/*
    @Test
    public void test_2() throws Exception {
        HWPFile hwp = HWPReader.fromFile("etc/a.hwp");
    }


    @Test
    public void test_3() throws Exception {
        HWPTester.test("etc" + File.separator + "1");
    }



    @Test
    public void test_4() throws Exception {
        HWPTester.test("etc" + File.separator + "4");
    }

    @Test
    public void test_5() throws Exception {
        HWPTester.test("etc" + File.separator + "5");
    }
    */

    @Test
    public void test_html() throws Exception {

        HWPTester.testHTML("etc" + File.separator + "html");
    }

    /*
    @org.junit.Test
    public void overlapArea() {
        Area a  = new Area(23096,38558, 43569, 43724);
        Area b = new Area(8504, 38720, 51024, 39720);
        System.out.println(a.overlap(b));
        System.out.println(b.overlap(a));
    }

     */

    /*
    @org.junit.Test
    public void test_2() throws Exception {
        HWPTester.test("testingHWP/오류/4");
    }
     */

}
