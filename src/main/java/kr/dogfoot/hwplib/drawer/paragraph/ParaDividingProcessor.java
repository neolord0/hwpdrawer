package kr.dogfoot.hwplib.drawer.paragraph;

import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.interimoutput.InterimOutput;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPChar;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharControlExtend;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.HWPCharType;

public class ParaDividingProcessor {
    private final static long MultiColumnGsp = 1200; // 약 4mm

    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaListDrawer paraListDrawer;
    private final DistributionMultiColumnRearranger distributionMultiColumnRearranger;

    public ParaDividingProcessor(DrawingInput input, InterimOutput output, ParaListDrawer paraListDrawer, DistributionMultiColumnRearranger distributionMultiColumnRearranger) {
        this.input = input;
        this.output = output;
        this.paraListDrawer = paraListDrawer;
        this.distributionMultiColumnRearranger = distributionMultiColumnRearranger;
    }

    public void process() throws Exception {
        DivideSort divideSort = input.currentPara().getHeader().getDivideSort();

        if (divideSort.isDivideSection()) {
            onSection();
        }  else if (divideSort.isDivideMultiColumn()) {
            onMultiColumn();
            return;
        }

        if (divideSort.isDividePage()) {
            paraListDrawer.newPage();
        } else if (divideSort.isDivideColumn()) {
            if (input.columnsInfo().isDistributionMultiColumn()) {
                onMultiColumn();
            } else {
                if (input.columnsInfo().lastColumn()) {
                    paraListDrawer.newPage();
                } else {
                    paraListDrawer.nextColumn();
                }
            }
        }
    }

    private void onSection() {
        input.nextChar();
        HWPChar firstChar = input.currentChar();
        input.nextChar();
        HWPChar secondChar = input.currentChar();

        if (firstChar.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) firstChar).isSectionDefine()) {
            input.sectionDefine((ControlSectionDefine) input.currentPara().getControlList().get(0));
            paraListDrawer.increaseControlExtendCharIndex();
        }

        if (secondChar.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) secondChar).isColumnDefine()) {
            input.newMultiColumn((ControlColumnDefine) input.currentPara().getControlList().get(1),
                    input.pageInfo().bodyArea().top());
            paraListDrawer.increaseControlExtendCharIndex();
        }

        input.newPage();
        output.newPage(input);
    }

    private void onMultiColumn() throws Exception {
        if (!output.hadRearrangedDistributionMultiColumn()) {
            if (!distributionMultiColumnRearranger.testing()) {
                gotoZeroColumn();
                distributionMultiColumnRearranger.endParaIndex(input.paraIndex() - 1);
                distributionMultiColumnRearranger.rearrangeFromCurrentColumn();
            } else {
                distributionMultiColumnRearranger.endParaIndex(input.paraIndex() - 1);
                throw new BreakDrawingException().forDividingColumn();
            }
        } else {
            distributionMultiColumnRearranger.endParaIndex(-1);

            setColumnDefine();

            output.newMultiColumn(input.columnsInfo());
            paraListDrawer.resetForNewColumn();
            throw new BreakDrawingException().forDividingColumn();
        }
    }

    private void gotoZeroColumn() {
        while (input.columnsInfo().currentColumnIndex() > 0) {
            input.previousColumn();
            output.previousColumn();
        }
    }

    private void setColumnDefine() {
        input.nextChar();
        HWPChar firstChar = input.currentChar();

        if (firstChar.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) firstChar).isColumnDefine()) {
            input.newMultiColumn((ControlColumnDefine) input.currentPara().getControlList().get(0), output.multiColumnBottom() + MultiColumnGsp);
            paraListDrawer.increaseControlExtendCharIndex();
        } else {
            input.newMultiColumnWithSameColumnDefine(output.multiColumnBottom() + MultiColumnGsp);
            input.previousChar(1);
        }
    }

}
