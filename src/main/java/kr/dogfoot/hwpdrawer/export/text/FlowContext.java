package kr.dogfoot.hwpdrawer.export.text;

/** 지금 글자를 쓰는 자리. 표를 Markdown 표로 쓸 수 있는지가 갈린다. */
enum FlowContext {
    /** 본문·각주·글상자: 표를 Markdown 표로 쓴다. */
    BLOCK,
    /** Markdown 표의 셀 안: 표 안에 표를 만들 수 없으므로 안쪽 표는 셀 글자만 차례로 쓴다. */
    CELL
}
