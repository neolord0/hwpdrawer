package kr.dogfoot.hwplib.drawer.util;

public class UnitConvertor {
    private static int zoomRate;

    public static void zoomRate(int zoomRate) {
        UnitConvertor.zoomRate = zoomRate;
    }

    public static int fromHWPUnit(long hwpUnit) {
        return (int) (hwpUnit * 72 * 100 / 2560 / zoomRate);
    }
}
