package kr.dogfoot.hwpdrawer.document.control;

import kr.dogfoot.hwplib.object.bodytext.control.gso.ControlPicture;
import kr.dogfoot.hwpdrawer.util.Area;

/** 이미지(GSO Picture) 컨트롤 아이템. */
public class GsoPictureItem extends ControlItem {
    private final ControlPicture picture;

    public GsoPictureItem(ControlPicture picture, Area area, Area areaWithMargin, boolean behindText) {
        super(area, areaWithMargin, behindText);
        this.picture = picture;
    }

    public ControlPicture picture() { return picture; }

    @Override
    public ControlItem cloneWithArea(Area newArea, Area newAreaWithMargin) {
        GsoPictureItem copy = new GsoPictureItem(picture, newArea, newAreaWithMargin, behindText());
        copyCommonAttrs(copy);
        return copy;
    }
}
