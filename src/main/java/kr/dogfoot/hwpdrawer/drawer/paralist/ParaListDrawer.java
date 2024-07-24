package kr.dogfoot.hwpdrawer.drawer.paralist;

import kr.dogfoot.hwpdrawer.drawer.BreakDrawingException;
import kr.dogfoot.hwpdrawer.drawer.RedrawException;
import kr.dogfoot.hwpdrawer.drawer.para.ParaDrawer;
import kr.dogfoot.hwpdrawer.drawer.para.nolineseg.ParaDrawerWithoutLineSeg;
import kr.dogfoot.hwpdrawer.drawer.para.withlineseg.ParaDrawerWithLineSeg;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.output.page.FooterOutput;

public class ParaListDrawer {
    protected final DrawingInput input;
    protected final InterimOutput output;

    protected final ParaDrawerWithoutLineSeg paraDrawerWithoutLineSeg;
    protected final ParaDrawerWithLineSeg paraDrawerWithLineSeg;
    protected final DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    protected ParaListDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;

        paraDrawerWithoutLineSeg = new ParaDrawerWithoutLineSeg(input, output);
        paraDrawerWithLineSeg = new ParaDrawerWithLineSeg(input, output);

        distributionMultiColumnRearranger = new DistributionMultiColumnRearranger(input, output, this, paraDrawerWithoutLineSeg);

        paraDrawerWithoutLineSeg
                .distributionMultiColumnRearranger(distributionMultiColumnRearranger);
    }

    public static void drawHeaderFooter(DrawingInput input, InterimOutput output) throws Exception {
        drawHeader(input, output);
        drawFooter(input, output);
    }

    public void drawHeaderFooter() throws Exception {
        drawHeaderFooter(input, output);
    }

    public ParaDrawer paraDrawer() {
        if (input.currentPara().getLineSeg() != null && input.currentPara().getLineSeg().getLineSegItemList().size() > 0) {
            return paraDrawerWithoutLineSeg;
        } else {
            return paraDrawerWithoutLineSeg;
        }
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
                paraDrawerWithoutLineSeg.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                if (e.type().isForOverTextBoxArea()) {
                    input.currentColumnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawerWithoutLineSeg.gotoStartCharOfCurrentRow();
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
