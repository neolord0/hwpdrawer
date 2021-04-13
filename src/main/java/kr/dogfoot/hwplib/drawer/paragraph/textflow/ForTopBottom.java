package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.paragraph.control.ControlClassifier;
import kr.dogfoot.hwplib.drawer.util.Area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

public class ForTopBottom {
    private ArrayList<TopBottomArea> topBottomAreas;

    public ForTopBottom() {
        topBottomAreas = new ArrayList<>();
    }

    public void controls(TreeSet<ControlClassifier.ControlInfo> controls) {
        topBottomAreas.clear();
        for (ControlClassifier.ControlInfo controlInfo : controls) {
            addTopBottomArea(controlInfo.absoluteArea());
        }
        Collections.sort(topBottomAreas);
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

    public long calculate(Area textLineArea) {
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

    public void clear() {
        topBottomAreas.clear();
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
