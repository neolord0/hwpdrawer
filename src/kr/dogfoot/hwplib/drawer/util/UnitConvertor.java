package kr.dogfoot.hwplib.drawer.util;

import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.awt.*;

public class UnitConvertor {
    private static int zoomRate;

    public static void zoomRate(int zoomRate) {
        UnitConvertor.zoomRate = zoomRate;
    }

    public static int fromHWPUnit(long hwpUnit) {
        return (int) (hwpUnit * 72 * 100 / 2560 / zoomRate);
    }

    public static float fontSize(int baseSize) {
        return baseSize * 72 * 100 / 2560 / zoomRate;
    }

    public static Color color(Color4Byte color4Byte) {
        return new Color(color4Byte.getR(), color4Byte.getG(), color4Byte.getB());
    }
}
