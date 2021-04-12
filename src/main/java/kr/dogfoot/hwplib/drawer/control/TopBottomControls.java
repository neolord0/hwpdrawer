package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.util.Area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

public class TopBottomControls {
    private TreeSet<ControlDrawer.ControlInfo> controlInfos;
    private ArrayList<TopBottomArea> topBottomAreas;

    public TopBottomControls() {
        controlInfos = new TreeSet<>();
        topBottomAreas = new ArrayList<>();
    }

    public void add(ControlDrawer.ControlInfo controlInfo) {
        controlInfos.add(controlInfo);
        addTopBottomArea(controlInfo.absoluteArea());
    }

    private void addTopBottomArea(Area area) {
        for (TopBottomArea tbArea : topBottomAreas) {
            if (tbArea.intersects(area)) {
                tbArea.merge(area);
                return;
            }
        }
        topBottomAreas.add(new TopBottomArea(area));
    }

    public void sortTopBottomAreas() {
        Collections.sort(topBottomAreas);
    }

    public ControlDrawer.ControlInfo[] controls() {
        return controlInfos.toArray(ControlDrawer.ControlInfo.Zero_Array);
    }

    public void clear() {
        controlInfos.clear();
        topBottomAreas.clear();
    }

    public long checkTextFlow(Area textLineArea) {
        long yOffset = 0;
        int count = topBottomAreas.size();
        for (int index = 0; index < count; index++) {
            TopBottomArea tbArea = topBottomAreas.get(index);
            if (tbArea.intersects(textLineArea) &&
                    (index + 1 == count || topBottomAreas.get(index + 1).intersects(textLineArea) == false)) {
                yOffset = tbArea.bottom - textLineArea.top();
            }
        }
        return yOffset;
    }

    private static class TopBottomArea implements Comparable<TopBottomArea> {
        long top;
        long bottom;

        public TopBottomArea(Area area) {
            this.top = area.top();
            this.bottom = area.bottom();
        }

        public int compareTo(TopBottomArea o) {
            if(top > o.top)
                return 1;
            else if (top == o.top)
                return 0;
            else
                return -1;
        }

        public boolean intersects(Area area) {
            if(area.bottom() < this.top || this.bottom < area.top())
                return false;
            return true;
        }

        public void merge(Area area) {
            top = (top < area.top()) ? top : area.top();
            bottom = (bottom > area.bottom()) ? bottom : area.bottom();
        }
    }


}
