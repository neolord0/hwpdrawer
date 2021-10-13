package kr.dogfoot.hwplib.drawer.drawer.paralist;

import kr.dogfoot.hwplib.drawer.drawer.BreakDrawingException;
import kr.dogfoot.hwplib.drawer.drawer.RedrawException;
import kr.dogfoot.hwplib.drawer.drawer.para.ParaDrawer;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.page.FooterOutput;

public class ParaListDrawer {
    protected final DrawingInput input;
    protected final InterimOutput output;

    protected final ParaDrawer paraDrawer;
    protected final DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    protected ParaListDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;

        paraDrawer = new ParaDrawer(input, output);
        distributionMultiColumnRearranger = new DistributionMultiColumnRearranger(input, output, this, paraDrawer);

        paraDrawer
                .distributionMultiColumnRearranger(distributionMultiColumnRearranger);
    }

    public static void drawHeaderFooter(DrawingInput input, InterimOutput output) throws Exception {
        drawHeader(input, output);
        drawFooter(input, output);
    }

    public void drawHeaderFooter() throws Exception {
        drawHeaderFooter(input, output);
    }

    private static void drawHeader(DrawingInput input, InterimOutput output) throws Exception {
        if (input.pageInfo().header(output.currentPage()) != null) {
            output.startHeader();

            new ParaListDrawerForControl(input, output)
                    .draw(input.pageInfo().header(output.currentPage()).getParagraphList(), input.pageInfo().headerArea().widthHeight());

            output.endHeader();
        }
    }

    private static void drawFooter(DrawingInput input, InterimOutput output) throws Exception {
        if (input.pageInfo().footer(output.currentPage()) != null) {
            FooterOutput footerOutput = output.startFooter();

            long calculatedContentHeight = new ParaListDrawerForControl(input, output)
                    .draw(input.pageInfo().footer(output.currentPage()).getParagraphList(), input.pageInfo().footerArea().widthHeight());

            footerOutput.calculatedContentHeight(calculatedContentHeight);
            output.endFooter();
        }
    }

    public void redraw(int endParaIndex) throws Exception {
        boolean redraw = true;
        boolean endingPara = false;

        while (redraw || (endingPara = !input.nextPara()) == false) {
            if (endParaIndex != -1 && input.paraIndex() > endParaIndex) {
                endingPara = true;
                break;
            }
            try {
                paraDrawer.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                if (e.type().isForOverTextBoxArea()) {
                    input.currentColumnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawer.gotoStartCharOfCurrentRow();
                    input.currentColumnsInfo().processLikeDistributionMultiColumn(true);
                    output.currentRow().increaseCalculationCount();
                    redraw = true;
                } else {
                    throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (endingPara == true
                    && !input.currentColumnsInfo().isLastColumn()
                    && (distributionMultiColumnRearranger.hasEmptyColumn()
                    || input.currentColumnsInfo().processLikeDistributionMultiColumn())) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }

        if (endingPara || input.nextPara() == false) {
            throw new BreakDrawingException().forEndingPara();
        }
    }
}
