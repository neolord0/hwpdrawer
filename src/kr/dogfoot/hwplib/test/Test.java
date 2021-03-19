package kr.dogfoot.hwplib.test;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.HWPDrawer;
import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.reader.HWPReader;

public class Test {
    public static void main(String[] arg) throws Exception {
//        test("test/글자모양/글자위치");
//        test("test/글자모양/밑줄취소선");
//        test("test/글자모양/상대크기");
//        test("test/글자모양/자간");
//        test("test/글자모양/장평");
//        test("test/글자모양");
//        test("test/기본/여러 문단");
//        test("test/문단모양/줄간격");
//        test("test/문단모양/줄나눔");
//        test("test/문단모양/줄나눔2");
        test("test/문단모양/줄나눔3");
//        test("test/문단모양/최소공백");
    }

    public static void test(String path) throws Exception {
        long startTime = System.currentTimeMillis();
        HWPFile hwpFile = HWPReader.fromFile(path + "/test.hwp");

        HWPDrawer.draw(hwpFile,
                new DrawingOption()
                        .directoryToSave(path)
                        .zoomRate(100)
                        .offset(0, -10));

        long endTime = System.currentTimeMillis();
        System.out.println(path + " : " + (endTime - startTime) + "ms");

    }
}
