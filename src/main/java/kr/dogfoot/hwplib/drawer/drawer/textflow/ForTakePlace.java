package kr.dogfoot.hwplib.drawer.drawer.textflow;

import kr.dogfoot.hwplib.drawer.drawer.charInfo.ControlCharInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.control.ctrlheader.gso.VertRelTo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ForTakePlace {
    private final Map<ControlCharInfo, Area> charInfos;
    private final ArrayList<TakePlaceArea> takePlaceAreas;

    public ForTakePlace() {
        charInfos = new HashMap<>();
        takePlaceAreas = new ArrayList<>();
    }

    public void add(ControlCharInfo charInfo, Area areaWithOuterMargin) {
        charInfos.put(charInfo, areaWithOuterMargin);
        addTakePlaceArea(areaWithOuterMargin, charInfo.header().getProperty().getVertRelTo());
    }

    public boolean alreadyAdded(ControlCharInfo charInfo) {
        return charInfos.containsKey(charInfo);
    }

    private void addTakePlaceArea(Area area, VertRelTo vertRelTo) {
        for (TakePlaceArea tbArea : takePlaceAreas) {
            if (tbArea.intersects(area)) {
                tbArea.merge(area, vertRelTo);
                return;
            }
        }
        takePlaceAreas.add(new TakePlaceArea(area, vertRelTo));
    }

    public void reset() {
        charInfos.clear();
        takePlaceAreas.clear();
    }

    public Result calculate(Area textLineArea) {
        long yOffset = 0;
        VertRelTo vertRelTo = VertRelTo.Paper;

        int count = takePlaceAreas.size();
        for (int index = 0; index < count; index++) {
            TakePlaceArea tbArea = takePlaceAreas.get(index);
            if (tbArea.intersects(textLineArea) &&
                    (index + 1 == count || takePlaceAreas.get(index + 1).intersects(textLineArea) == false)) {
                if (vertRelTo != VertRelTo.Para) {
                    vertRelTo = tbArea.vertRelTo;
                }
                yOffset = tbArea.bottom - textLineArea.top();
            }
        }
        return new Result(yOffset, vertRelTo);
    }

    public void delete(ControlCharInfo charInfo) {
        charInfos.remove(charInfo);
        updateTakePlaceAreas();
    }

    private void updateTakePlaceAreas() {
        takePlaceAreas.clear();
        for (ControlCharInfo charInfo : charInfos.keySet()) {
            addTakePlaceArea(charInfos.get(charInfo), charInfo.header().getProperty().getVertRelTo());
        }
    }

    private static class TakePlaceArea implements Comparable<TakePlaceArea> {
        long top;
        long bottom;
        VertRelTo vertRelTo;

        public TakePlaceArea(Area area, VertRelTo vertRelTo) {
            this.top = area.top();
            this.bottom = area.bottom();
            this.vertRelTo = vertRelTo;
        }

        public int compareTo(TakePlaceArea o) {
            if (top > o.top)
                return 1;
            else if (top == o.top)
                return 0;
            else
                return -1;
        }

        public boolean intersects(Area area) {
            if (area.bottom() < this.top || this.bottom < area.top())
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
        private final long yOffset;
        private final VertRelTo vertRelTo;

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
