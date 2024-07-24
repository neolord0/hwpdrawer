package kr.dogfoot.hwpdrawer.drawer.para;

import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.control.ControlOutput;
import kr.dogfoot.hwpdrawer.util.CharPosition;
import kr.dogfoot.hwpdrawer.input.DrawingInput;

public abstract class ParaDrawer {
    protected final DrawingInput input;
    protected final InterimOutput output;

    protected ParaDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;
    }

    public void draw(boolean redraw) throws Exception {
        draw(redraw, null, null);
    }

    public abstract void draw(boolean redraw, CharPosition startPosition, ControlOutput[] childControlsCrossingPage) throws Exception;

    public abstract void gotoStartCharOfCurrentRow();
}
