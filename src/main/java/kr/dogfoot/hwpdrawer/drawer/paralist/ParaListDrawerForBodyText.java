package kr.dogfoot.hwpdrawer.drawer.paralist;

import kr.dogfoot.hwpdrawer.drawer.RedrawException;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;

public class ParaListDrawerForBodyText extends ParaListDrawer {
    public ParaListDrawerForBodyText(DrawingInput input, InterimOutput output) {
        super(input, output);
    }

    public void draw(ParagraphListInterface paraList) throws Exception {
        input.startBodyTextParaList(paraList);

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                paraDrawer().draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                input.gotoParaCharPosition(e.position());
                input.currentParaListInfo().resetParaStartY(e.startY());
                redraw = true;
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (input.currentColumnsInfo().isDistributionMultiColumn()
                    && !input.currentColumnsInfo().isLastColumn()) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }

        input.endBodyTextParaList();
        drawHeaderFooter();
    }
}
