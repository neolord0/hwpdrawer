package kr.dogfoot.hwplib.drawer.control;

import kr.dogfoot.hwplib.drawer.util.Area;

import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeSet;

public class SquareControls {
    private TreeSet<ControlDrawer.ControlInfo> controlInfos;
    private ArrayList<SquareArea> squareAreas;

    public SquareControls() {
        controlInfos = new TreeSet<>();
        squareAreas = new ArrayList<>();
    }

    public void add(ControlDrawer.ControlInfo controlInfo) {
        controlInfos.add(controlInfo);
    }

    public ControlDrawer.ControlInfo[] controls() {
        return controlInfos.toArray(ControlDrawer.ControlInfo.Zero_Array);
    }

    public Area[] checkSquareTextFlow(Area textLineArea) {
        setSquareAreas(textLineArea);
        if (squareAreas.size() == 0) {
           Area[] ret = new Area[1];
           ret[0] = textLineArea;
           return ret;
        }
        return divideArea(textLineArea);
    }

    private void setSquareAreas(Area textLineArea) {
        squareAreas.clear();
        for (ControlDrawer.ControlInfo controlInfo : controlInfos) {
            if (controlInfo.absoluteArea().intersects(textLineArea)) {
                addSquareArea(controlInfo.absoluteArea());
            }
        }
        Collections.sort(squareAreas);
    }

    private Area[] divideArea(Area textLineArea) {
        ArrayList<Area> areas = new ArrayList<>();
        SquareArea squareArea;
        SquareArea oldSquareArea = null;

        int count = squareAreas.size();
        for (int index = 0; index < count; index++) {
            squareArea = squareAreas.get(index);
            if (index == 0) {
                if (squareArea.left > textLineArea.left()) {
                    areas.add(new Area(textLineArea.left(),
                            textLineArea.top(),
                            squareArea.left,
                            textLineArea.bottom()));
                }
            } else {
                areas.add(new Area(oldSquareArea.right,
                        textLineArea.top(),
                        squareArea.left,
                        textLineArea.bottom()));

                if (index == count - 1) {
                    if (squareArea.right < textLineArea.right()) {
                        areas.add(new Area(squareArea.right,
                                textLineArea.top(),
                                textLineArea.right(),
                                textLineArea.bottom()));
                    }
                }
            }

            oldSquareArea = squareArea;
        }
        return areas.toArray(Area.Zero_Array);
    }

    private void addSquareArea(Area area) {
        for (SquareArea sArea : squareAreas) {
            if (sArea.intersects(area)) {
                sArea.merge(area);
                return;
            }
        }
        squareAreas.add(new SquareArea(area));
    }

    private class SquareArea implements Comparable<SquareArea> {
        public long left;
        public long right;

        public SquareArea(Area area) {
            left = area.left();
            right = area.right();
        }

        public int compareTo(SquareArea o) {
            if(left > o.left)
                return 1;
            else if (left == o.left)
                return 0;
            else
                return -1;
        }

        public boolean intersects(Area area) {
            if(area.right() < this.left || this.right < area.left())
                return false;
            return true;
        }

        public void merge(Area area) {
            left = (left < area.left()) ? left : area.left();
            right = (right > area.right()) ? right : area.right();
        }

    }
}
