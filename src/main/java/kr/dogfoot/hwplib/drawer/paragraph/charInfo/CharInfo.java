package kr.dogfoot.hwplib.drawer.paragraph.charInfo;

import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharNormal;
import kr.dogfoot.hwplib.object.docinfo.CharShape;

public interface CharInfo {
    Type type();
    HWPChar character();
    double width();
    double widthAddingCharSpace();
    long height();
    long x();
    void x(long x);
    int index();
    int position();
    CharShape charShape();

    enum Type {
        Normal,
        Control
    }
}
