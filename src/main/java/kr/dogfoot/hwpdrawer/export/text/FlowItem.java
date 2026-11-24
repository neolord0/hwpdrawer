package kr.dogfoot.hwpdrawer.export.text;

import kr.dogfoot.hwpdrawer.document.control.ControlItem;

/** 읽는 순서의 한 칸: 문단 조각 하나 또는 글자를 담은 컨트롤(표·글상자·묶음) 하나. */
final class FlowItem {
    private final ParagraphText paragraph;
    private final ControlItem control;

    private FlowItem(ParagraphText paragraph, ControlItem control) {
        this.paragraph = paragraph;
        this.control = control;
    }

    static FlowItem of(ParagraphText paragraph) {
        return new FlowItem(paragraph, null);
    }

    static FlowItem of(ControlItem control) {
        return new FlowItem(null, control);
    }

    boolean isParagraph() {
        return paragraph != null;
    }

    ParagraphText paragraph() {
        return paragraph;
    }

    ControlItem control() {
        return control;
    }
}
