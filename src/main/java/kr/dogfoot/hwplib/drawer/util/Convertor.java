package kr.dogfoot.hwplib.drawer.util;

import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderThickness;
import kr.dogfoot.hwplib.object.docinfo.borderfill.BorderType;
import kr.dogfoot.hwplib.object.etc.Color4Byte;

import java.awt.*;

public class Convertor {
    private static int zoomRate;

    public static void zoomRate(int zoomRate) {
        Convertor.zoomRate = zoomRate;
    }

    public static int fromHWPUnit(long hwpUnit) {
        return (int) (hwpUnit * zoomRate / 40 / 100);
    }

    public static float fontSize(int baseSize) {
        return baseSize * zoomRate / 40 / 100;
    }

    public static Color color(Color4Byte color4Byte) {
        return new Color(color4Byte.getR(), color4Byte.getG(), color4Byte.getB());
    }

    public static Stroke stroke(BorderType type, BorderThickness thickness) {
        float lineThickness = lineThickness(thickness);
        switch (type) {
            case Solid:
                return new BasicStroke(lineThickness);
            case Dash:
            case Dot:
            case DashDot:
            case DashDotDot:
            case LongDash:
            case CircleDot:
                return new BasicStroke(lineThickness, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, linePattern(type, lineThickness), 0);
            case Double:
            case ThinThick:
            case ThinThickThin:
            case Wave:
            case DoubleWave:
            case Thick3D:
            case Thick3DReverseLighting:
            case Solid3D:
            case Solid3DReverseLighting:
                break;
        }
        return new BasicStroke(lineThickness);
    }

    private static float lineThickness(BorderThickness thickness) {
        float ret = 0;
        switch (thickness) {
            case MM0_1:
                ret = 0.1f;
                break;
            case MM0_12:
                ret = 0.12f;
                break;
            case MM0_15:
                ret = 0.15f;
                break;
            case MM0_2:
                ret = 0.2f;
                break;
            case MM0_25:
                ret = 0.25f;
                break;
            case MM0_3:
                ret = 0.3f;
                break;
            case MM0_4:
                ret = 0.4f;
                break;
            case MM0_5:
                ret = 0.5f;
                break;
            case MM0_6:
                ret = 0.6f;
                break;
            case MM0_7:
                ret = 0.7f;
                break;
            case MM1_0:
                ret = 1.0f;
                break;
            case MM1_5:
                ret = 1.5f;
                break;
            case MM2_0:
                ret = 2.0f;
                break;
            case MM3_0:
                ret = 3.0f;
                break;
            case MM4_0:
                ret = 4.0f;
                break;
            case MM5_0:
                ret = 5.0f;
                break;
        }
        return ret * 10 * zoomRate / 100;
    }

    private static float[] linePattern(BorderType type, float lineThickness) {
        switch (type) {
            case Dash:
                return new float[]{8.0f * lineThickness, 4.0f * lineThickness};
            case Dot:
                return new float[]{2.0f * lineThickness, 4.0f * lineThickness};
            case DashDot:
                return new float[]{14.0f * lineThickness, 4.0f * lineThickness, 2.0f * lineThickness, 4.0f * lineThickness};
            case DashDotDot:
                return new float[]{14.0f * lineThickness, 4.0f * lineThickness, 2.0f * lineThickness, 4.0f * lineThickness, 2.0f * lineThickness, 4.0f * lineThickness};
            case LongDash:
                return new float[]{18.0f * lineThickness, 4.0f * lineThickness};
            case CircleDot:
                return new float[]{2.0f * lineThickness, 4.0f * lineThickness};
        }
        return new float[]{10.0f * lineThickness};
    }

}
