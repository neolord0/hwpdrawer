package kr.dogfoot.hwpdrawer.drawer.para.withlineseg;

import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwplib.object.bodytext.control.ControlColumnDefine;
import kr.dogfoot.hwplib.object.bodytext.control.ControlSectionDefine;
import kr.dogfoot.hwplib.object.bodytext.paragraph.header.DivideSort;

public class DividingProcessor {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaDrawerWithLineSeg paraDrawer;
    private final CharAdder charAdder;

    public DividingProcessor(DrawingInput input,
                             InterimOutput output,
                             ParaDrawerWithLineSeg paraDrawer,
                             CharAdder charAdder) {
        this.input = input;
        this.output = output;
        this.paraDrawer = paraDrawer;
        this.charAdder = charAdder;
    }

    public void process() throws Exception {
        DivideSort divideSort = input.currentPara().getHeader().getDivideSort();
        if (divideSort.isDivideSection()) {
            onDividingSection();
        } else if (divideSort.isDivideMultiColumn()) {
            onDividingRow();
        } else if (divideSort.isDividePage()) {
            onDividingPage();
        } else if (divideSort.isDivideColumn()) {
            onDividingColumn();
        }
    }

    private void onDividingSection() {
        ControlSectionDefine sectionDefine = charAdder.sectionDefine();
        ControlColumnDefine columnDefine = charAdder.columnDefine();
        if (sectionDefine == null) {
            sectionDefine = charAdder.sectionDefine();
        }

        input.sectionDefine(sectionDefine);
        input.columnsInfo(columnDefine, input.pageInfo().bodyArea().top());

        input.nextPage();
        output.nextPage(input);

        if (input.currentColumnsInfo().isParallelMultiColumn()) {
            startParallelMultiColumnRow();
        }
    }

    private void startParallelMultiColumnRow() {
        input.parallelMultiColumnInfo()
                .startParallelMultiColumn(output.currentRowIndex(), output.currentOutput(), input.currentParaListInfo().cellInfo());
    }

    private void onDividingRow() throws Exception {
    }

    private void onDividingPage() throws Exception {
    }

    private void onDividingColumn() throws Exception {
    }

}
