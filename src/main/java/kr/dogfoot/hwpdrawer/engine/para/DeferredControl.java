package kr.dogfoot.hwpdrawer.engine.para;

import kr.dogfoot.hwplib.object.bodytext.control.Control;
import kr.dogfoot.hwpdrawer.document.page.Page;

/**
 * 텍스트 overflow(75% 규칙)로 다음 페이지로 지연된 컨트롤.
 * 섹션 레이아웃 완료 후 LayoutEngine이 일괄 배치한다.
 */
public final class DeferredControl {
    public final Control control;
    public final Page anchorPage;
    public final int paraIdx;

    public DeferredControl(Control control, Page anchorPage, int paraIdx) {
        this.control = control;
        this.anchorPage = anchorPage;
        this.paraIdx = paraIdx;
    }
}
