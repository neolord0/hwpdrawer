package kr.dogfoot.hwpdrawer.drawer.paralist;

import kr.dogfoot.hwpdrawer.drawer.BreakDrawingException;
import kr.dogfoot.hwpdrawer.drawer.RedrawException;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;

public class ParaListDrawerForControl extends ParaListDrawer {
    public ParaListDrawerForControl(DrawingInput input, InterimOutput output) {
        super(input, output);
    }

    public long draw(ParagraphListInterface paraList, Area textBoxArea) throws Exception {
        input.startControlParaList(textBoxArea, paraList);

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                paraDrawer().draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                if (e.type().isForOverTextBoxArea()) {
                    input.currentColumnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawer().gotoStartCharOfCurrentRow();
                    input.currentColumnsInfo().processLikeDistributionMultiColumn(true);
                    redraw = true;
                } else {
                    throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (!input.currentColumnsInfo().isLastColumn()
                    && (input.currentColumnsInfo().isDistributionMultiColumn()
                    || input.currentColumnsInfo().processLikeDistributionMultiColumn())) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }
        input.endControlParaList();
        return output.currentContent().height();
    }
}
