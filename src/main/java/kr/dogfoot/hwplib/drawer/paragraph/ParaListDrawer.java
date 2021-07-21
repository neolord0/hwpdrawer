package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.drawer.interimoutput.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.painter.PagePainter;
import kr.dogfoot.hwplib.drawer.paragraph.charInfo.CharInfoBuffer;
import kr.dogfoot.hwplib.drawer.paragraph.control.table.CellDrawResult;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;

public class ParaListDrawer {
    private final DrawingInput input;
    private final InterimOutput output;
    private final PagePainter pagePainter;

    private final ParaDrawer paraDrawer;
    private final CharInfoBuffer charInfoBuffer;
    private final DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    public ParaListDrawer(DrawingInput input, InterimOutput output) {
        this(input, output, null);
    }

    public ParaListDrawer(DrawingInput input, InterimOutput output, PagePainter pagePainter) {
        this.input = input;
        this.output = output;
        this.pagePainter = pagePainter;

        paraDrawer = new ParaDrawer(input, output, pagePainter, this);

        charInfoBuffer = new CharInfoBuffer();
        distributionMultiColumnRearranger = new DistributionMultiColumnRearranger(input, output, this, paraDrawer);
    }

    public void drawForBodyText(ParagraphListInterface paraList) throws Exception {
        input.startBodyTextParaList(paraList.getParagraphs());

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                paraDrawer.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                input.gotoParaCharPosition(e.position());
                input.currentParaListInfo().resetParaStartY(e.startY());
                redraw = true;
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (input.columnsInfo().isDistributionMultiColumn()
                    && !input.columnsInfo().lastColumn()) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }

        input.endBodyTextParaList();
        drawHeaderFooter();
    }

    public static void drawHeaderFooter(DrawingInput input, InterimOutput output) throws Exception {
        drawHeader(input, output);
        drawFooter(input, output);
    }

    public void drawHeaderFooter() throws Exception {
        drawHeader(input, output);
        drawFooter(input, output);
    }

    private static void drawHeader(DrawingInput input, InterimOutput output) throws Exception {
        if (input.pageInfo().header(output.currentPage()) != null) {
            output.startHeader();

            ParaListDrawer paraListDrawer = new ParaListDrawer(input, output);
            paraListDrawer.drawForControl(input.pageInfo().header(output.currentPage()).getParagraphList(), input.pageInfo().headerArea().widthHeight());

            output.endHeader();
        }
    }

    private static void drawFooter(DrawingInput input, InterimOutput output) throws Exception {
        if (input.pageInfo().footer(output.currentPage()) != null) {
            FooterOutput footerOutput = output.startFooter();

            ParaListDrawer paraListDrawer = new ParaListDrawer(input, output);
            long calculatedContentHeight = paraListDrawer.drawForControl(input.pageInfo().footer(output.currentPage()).getParagraphList(), input.pageInfo().footerArea().widthHeight());

            footerOutput.calculatedContentHeight(calculatedContentHeight);
            output.endFooter();
        }
    }

    public long drawForControl(ParagraphListInterface paraList, Area textBoxArea) throws Exception {
        input.startControlParaList(textBoxArea, paraList.getParagraphs());

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                paraDrawer.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                if (e.type().isForOverTextBoxArea()) {
                    input.columnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawer.gotoStartCharOfCurrentRow();
                    input.columnsInfo().processLikeDistributionMultiColumn(true);
                    redraw = true;
                } else {
                    throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (!input.columnsInfo().lastColumn()
                    && (input.columnsInfo().isDistributionMultiColumn()
                            || input.columnsInfo().processLikeDistributionMultiColumn())) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }
        input.endControlParaList();
        return output.currentContent().height();
    }

    public CellDrawResult drawForCell(ParagraphListInterface paraList, Area textBoxArea, long cellTopInPage) throws Exception {
        input.startCellParaList(textBoxArea, paraList.getParagraphs(), cellTopInPage);

        CellDrawResult result = new CellDrawResult();
        boolean redraw = false;
        boolean boolOverPage = false;
        while (redraw || input.nextPara()) {
            try {
                paraDrawer.draw(redraw);
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                if (e.type().isForOverTextBoxArea()) {
                    input.columnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawer.gotoStartCharOfCurrentRow();
                    input.columnsInfo().processLikeDistributionMultiColumn(true);
                    redraw = true;
                } else if (e.type().isForOverPage()) {
                    result
                            .split(true)
                            .splitPosition(e.position());
                    break;
                } else {
                    throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (!input.columnsInfo().lastColumn()) {
                if (input.columnsInfo().isDistributionMultiColumn()
                        || input.columnsInfo().processLikeDistributionMultiColumn()){
                    distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
                }

            }
        }

        input.endCellParaList();
        return result.height(output.currentContent().height());
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
                    input.columnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawer.gotoStartCharOfCurrentRow();
                    input.columnsInfo().processLikeDistributionMultiColumn(true);
                    output.currentRow().increaseCalculationCount();
                    redraw = true;
                } else {
                    throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (endingPara == true
                    && !input.columnsInfo().lastColumn()
                    && (distributionMultiColumnRearranger.hasEmptyColumn()
                            || input.columnsInfo().processLikeDistributionMultiColumn())) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }

        if (endingPara || input.nextPara() == false) {
            throw new BreakDrawingException().forEndingPara();
        }
    }

    public CharInfoBuffer charInfoBuffer() {
        return charInfoBuffer;
    }

    public DistributionMultiColumnRearranger distributionMultiColumnRearranger() {
        return distributionMultiColumnRearranger;
    }
}
