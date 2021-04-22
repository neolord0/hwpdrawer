package kr.dogfoot.hwplib.drawer.paragraph.textflow;

import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

import java.util.ArrayList;

public class ForTopBottom {
    private ArrayList<TopBottomArea> topBottomAreas;

    public ForTopBottom() {
        topBottomAreas = new ArrayList<>();
    }

    public void addTopBottomArea(Area area, VertRelTo vertRelTo) {
        for (TopBottomArea tbArea : topBottomAreas) {
            if (tbArea.intersects(area)) {
                tbArea.merge(area, vertRelTo);
                return;
            }
        }
        topBottomAreas.add(new TopBottomArea(area, vertRelTo));
    }

    public void reset() {
        topBottomAreas.clear();
    }

    public Result calculate(Area textLineArea) {
        long yOffset = 0;
        VertRelTo vertRelTo = VertRelTo.Paper;

        int count = topBottomAreas.size();
        for (int index = 0; index < count; index++) {
            TopBottomArea tbArea = topBottomAreas.get(index);
            if (tbArea.intersects(textLineArea) &&
                    (index + 1 == count || topBottomAreas.get(index + 1).intersects(textLineArea) == false)) {
                if (vertRelTo != VertRelTo.Para) {
                    vertRelTo = tbArea.vertRelTo;
                }
                yOffset = tbArea.bottom - textLineArea.top();
            }
        }
        return new Result(yOffset, vertRelTo);
    }


    private static class TopBottomArea implements Comparable<TopBottomArea> {
        long top;
        long bottom;
        VertRelTo vertRelTo;

        public TopBottomArea(Area area, VertRelTo vertRelTo) {
            this.top = area.top();
            this.bottom = area.bottom();
            this.vertRelTo = vertRelTo;
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

        public void merge(Area area, VertRelTo vertRelTo) {
            top = Math.min(top, area.top());
            bottom = Math.max(bottom, area.bottom());
            if (this.vertRelTo != VertRelTo.Para) {
                this.vertRelTo = vertRelTo;
            }
        }
    }

    public static class Result {
        private long yOffset;
        private VertRelTo vertRelTo;

        public Result(long yOffset, VertRelTo vertRelTo) {
            this.yOffset = yOffset;
            this.vertRelTo = vertRelTo;
        }

        public long yOffset() {
            return yOffset;
        }

        public VertRelTo vertRelTo() {
            return vertRelTo;
        }
    }
}
