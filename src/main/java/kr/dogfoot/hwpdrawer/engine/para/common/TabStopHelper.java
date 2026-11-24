package kr.dogfoot.hwpdrawer.engine.para.common;

import kr.dogfoot.hwplib.object.docinfo.ParaShape;
import kr.dogfoot.hwplib.object.docinfo.TabDef;
import kr.dogfoot.hwplib.object.docinfo.tabdef.TabInfo;
import kr.dogfoot.hwpdrawer.reader.HWPFileReader;

import java.util.List;

/**
 * 탭 스톱 계산 공유 헬퍼.
 *
 * <p>WithLineSegLayouter(실제 배치)와 NoLineSegLayouter(줄바꿈 계산용 섀도 패스)가
 * 동일한 탭 스톱을 쓰도록 한 곳에서 계산한다.</p>
 *
 * <p><b>탭 위치 스케일</b>: 탭 정의(TabInfo.position)와 섹션 기본 탭 간격(defaultTabGap)에
 * 저장된 값은 실제 렌더링 좌표의 2배로 저장돼 있어 1/2을 곱해야 한다
 * (한글 프로그램의 탭 위치와 픽셀 비교로 검증).</p>
 */
public final class TabStopHelper {
    private TabStopHelper() {}

    public static final double TAB_SCALE = 0.5;

    /** HWP 표준 기본 탭 간격(저장값, 40pt). */
    private static final double DEFAULT_TAB_GAP = 4000.0;

    /**
     * {@code afterX}(렌더링 좌표) 이후 첫 명시적 탭 스톱을 반환한다(없으면 null).
     * TabInfo.position은 저장값(렌더링 좌표의 2배)이므로 TAB_SCALE을 곱해 비교한다.
     */
    public static TabInfo findTabStop(HWPFileReader reader, ParaShape paraShape, double afterX) {
        int tabDefId = paraShape.getTabDefId();
        List<TabDef> defs = reader.hwpFile().getDocInfo().getTabDefList();
        if (tabDefId < 0 || tabDefId >= defs.size()) return null;
        TabInfo best = null;
        for (TabInfo ti : defs.get(tabDefId).getTabInfoList()) {
            if (ti.getPosition() * TAB_SCALE > afterX + 1
                    && (best == null || ti.getPosition() < best.getPosition())) {
                best = ti;
            }
        }
        return best;
    }

    /**
     * {@code currentX}(렌더링 좌표)에서 탭을 만났을 때 다음 탭 스톱 위치(렌더링 좌표)를 반환한다.
     * 명시적 탭 정의를 넘어선 탭은 섹션 기본 탭 간격의 배수로 진행한다.
     */
    public static double nextTabStopX(HWPFileReader reader, ParaShape paraShape,
                                       double currentX, long defaultTabGap) {
        TabInfo tab = findTabStop(reader, paraShape, currentX);
        if (tab != null) {
            return tab.getPosition() * TAB_SCALE;
        }
        double defaultTab = (defaultTabGap > 0 ? defaultTabGap : DEFAULT_TAB_GAP) * TAB_SCALE;
        return Math.ceil((currentX + 1) / defaultTab) * defaultTab;
    }
}
