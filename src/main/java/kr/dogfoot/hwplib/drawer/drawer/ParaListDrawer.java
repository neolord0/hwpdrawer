package kr.dogfoot.hwplib.drawer.drawer;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.control.ControlOutput;
import kr.dogfoot.hwplib.drawer.output.page.FooterOutput;
import kr.dogfoot.hwplib.drawer.drawer.control.table.CellResult;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.TextPosition;
import kr.dogfoot.hwplib.object.bodytext.ParagraphListInterface;

public class ParaListDrawer {
    private final DrawingInput input;
    private final InterimOutput output;

    private final ParaDrawer paraDrawer;
    private final CharInfoBuffer charInfoBuffer;
    private final DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    public ParaListDrawer(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;

        paraDrawer = new ParaDrawer(input, output, this);

        charInfoBuffer = new CharInfoBuffer();
        distributionMultiColumnRearranger = new DistributionMultiColumnRearranger(input, output, this, paraDrawer);
    }

    public void drawForBodyText(ParagraphListInterface paraList) throws Exception {
        input.startBodyTextParaList(paraList);

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
            if (input.currentColumnsInfo().isDistributionMultiColumn()
                    && !input.currentColumnsInfo().lastColumn()) {
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
        input.startControlParaList(textBoxArea, paraList);

        boolean redraw = false;
        while (redraw || input.nextPara()) {
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
                    redraw = true;
                } else {
                    throw e;
                }
            }
        }

        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (!input.currentColumnsInfo().lastColumn()
                    && (input.currentColumnsInfo().isDistributionMultiColumn()
                            || input.currentColumnsInfo().processLikeDistributionMultiColumn())) {
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            }
        }
        input.endControlParaList();
        return output.currentContent().height();
    }

    public CellResult drawForCell(ParagraphListInterface paraList,
                                  Area textBoxArea,
                                  boolean canSplit,
                                  long topInPage,
                                  long bottomMargin,
                                  TextPosition fromPosition,
                                  ControlOutput[] childControlsCrossingPage) throws Exception {

        boolean split = fromPosition != null;
        input.startCellParaList(textBoxArea, paraList, canSplit,  topInPage, bottomMargin, split);

        if (split) {
            if (!input.currentColumnsInfo().isParallelMultiColumn()) {
                input.currentColumnsInfo().processLikeDistributionMultiColumn(true);
                output.nextRow(input.currentColumnsInfo());
            }
            input.gotoParaWithIgnoreNextPara(fromPosition);
        }

        CellResult result = new CellResult();

        boolean redraw = false;
        while (redraw || input.nextPara()) {
            try {
                if (fromPosition != null && fromPosition.paraIndex() == input.paraIndex()) {

                    paraDrawer.draw(redraw, fromPosition, childControlsCrossingPage);
                } else {
                    paraDrawer.draw(redraw);
                }
                redraw = false;
            } catch (RedrawException e) {
                redraw = true;
            } catch (BreakDrawingException e) {
                if (e.type().isForOverTextBoxArea()) {
                    input.currentColumnsInfo().textBoxArea().bottom(input.pageInfo().bodyArea().bottom());
                    paraDrawer.gotoStartCharOfCurrentRow();
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
            if (!input.currentColumnsInfo().lastColumn()) {
                if (input.currentColumnsInfo().isDistributionMultiColumn()
                        || input.currentColumnsInfo().processLikeDistributionMultiColumn()){
                    distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
                }
            }
        }

        if (input.currentColumnsInfo().isParallelMultiColumn()
                && input.currentColumnsInfo().isFirstColumn()
                && split) {
            input.parallelMultiColumnInfo().addParentInfo(output.currentOutput(), input.currentParaListInfo().cellInfo());
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
                    && !input.currentColumnsInfo().lastColumn()
                    && (distributionMultiColumnRearranger.hasEmptyColumn()
                            || input.currentColumnsInfo().processLikeDistributionMultiColumn())) {
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
