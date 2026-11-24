package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwplib.object.HWPFile;
import kr.dogfoot.hwplib.object.bodytext.Section;
import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.control.sectiondefine.PageBorderFill;
import kr.dogfoot.hwplib.object.bodytext.paragraph.Paragraph;

/**
 * 쪽 바탕 — 글자 뒤 맨 아래. 보통 흰 종이다. 구역에 쪽 배경 채우기나 바탕쪽이 있으면 이 엔진이 그리지 않으므로
 * 색을 모르는 바탕으로 본다 (그 위의 밝은 글자는 남긴다). 문서 안 어느 구역에라도 있으면 문서 전체를 그렇게 본다.
 */
final class Paper {
    private Paper() {
    }

    static Fill of(HWPFile file) {
        if (file == null || file.getBodyText() == null) return Fill.NONE;
        for (Section section : file.getBodyText().getSectionList()) {
            ControlSectionDefine define = sectionDefineOf(section);
            if (define != null && paintsPage(file, define)) return Fill.UNKNOWN;
        }
        return Fill.NONE;
    }

    private static boolean paintsPage(HWPFile file, ControlSectionDefine define) {
        if (define.getBatangPageInfoList() != null && !define.getBatangPageInfoList().isEmpty()) return true;
        return isPainted(file, define.getBothPageBorderFill()) || isPainted(file, define.getEvenPageBorderFill())
                || isPainted(file, define.getOddPageBorderFill());
    }

    private static boolean isPainted(HWPFile file, PageBorderFill pageBorderFill) {
        return pageBorderFill != null && Fill.ofBorderFill(file, pageBorderFill.getBorderFillId()).isPainted();
    }

    private static ControlSectionDefine sectionDefineOf(Section section) {
        if (section.getParagraphCount() == 0) return null;
        Paragraph first = section.getParagraph(0);
        if (first.getControlList() == null) return null;
        for (Control control : first.getControlList()) {
            if (control instanceof ControlSectionDefine) return (ControlSectionDefine) control;
        }
        return null;
    }
}
