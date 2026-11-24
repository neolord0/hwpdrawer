package kr.dogfoot.hwpdrawer.document.control;

import kr.dogfoot.hwpdrawer.util.Area;

import java.util.ArrayList;
import java.util.List;

/**
 * 묶음 개체(그룹) 컨트롤 아이템.
 *
 * <p>자식 도형들은 그룹 좌표계에 정의되고, 그룹 전체가 페이지 상의 area로 확대/배치된다.
 * 레이아웃 단계에서 각 자식을 페이지 좌표로 매핑한 {@link ControlItem}으로 미리 만들어
 * children에 담고, 렌더링은 children을 등록 순서(뒤→앞)대로 그린다.</p>
 */
public class GsoContainerItem extends ControlItem {
    private final List<ControlItem> children = new ArrayList<>();

    public GsoContainerItem(Area area, Area areaWithMargin, boolean behindText) {
        super(area, areaWithMargin, behindText);
    }

    public List<ControlItem> children() { return children; }
    public void addChild(ControlItem child) { children.add(child); }

    @Override
    public ControlItem cloneWithArea(Area newArea, Area newAreaWithMargin) {
        GsoContainerItem copy = new GsoContainerItem(newArea, newAreaWithMargin, behindText());
        copyCommonAttrs(copy);
        copy.children.addAll(children);
        return copy;
    }
}
