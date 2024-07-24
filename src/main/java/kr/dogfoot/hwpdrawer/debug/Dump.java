package kr.dogfoot.hwpdrawer.debug;

import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;

import java.io.UnsupportedEncodingException;

public class Dump {
    public static String toString(HWPChar hwpChar) throws UnsupportedEncodingException {
        switch (hwpChar.getType()) {
            case Normal:
                return ((HWPCharNormal) hwpChar).getCh();
            case ControlChar:
                return "{C:" + hwpChar.getCode()+ "}";
            case ControlInline:
                return "{I:" + hwpChar.getCode()+ "}";
            case ControlExtend:
                return "{E:" + hwpChar.getCode()+ "}";
        }
        return "{null}";
    }
}
