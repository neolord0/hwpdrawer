package kr.dogfoot.hwplib.drawer.painter.html;

import kr.dogfoot.hwplib.drawer.DrawingOption;
import kr.dogfoot.hwplib.drawer.input.DrawingInput;
import kr.dogfoot.hwplib.drawer.output.InterimOutput;
import kr.dogfoot.hwplib.drawer.output.page.PageOutput;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class PagePainterForHTML {
    private final DrawingInput input;
    private final InterimOutput output;
    private DrawingOption option;

    private PainterForHTML painter;

    public PagePainterForHTML(DrawingInput input, InterimOutput output) {
        this.input = input;
        this.output = output;

        painter = new PainterForHTML(input);
    }


    public PagePainterForHTML option(DrawingOption option) {
        this.option = option;
        return this;
    }

    public void saveAllPages() throws Exception {
        painter.addDocType();
        painter.startHTML();
        painter.defaultHead();
        painter.startBodyAndPagesDiv();

        for (PageOutput pageOutput : output.pages()) {
            page(pageOutput);
        }

        painter.endBodyAndPagesDiv();
        painter.endHTML();

        saveFile();
    }

    private void page(PageOutput pageOutput) throws Exception {
        System.out.println(pageOutput.test(0));
        painter.startPage(pageOutput);

        if (pageOutput.headerOutput() != null) {
            painter.paintContent(pageOutput.headerOutput().content());
        }

        painter.paintContent(pageOutput.content());

        if (pageOutput.footerOutput() != null) {
            painter.paintContent(pageOutput.footerOutput().content());
        }

        painter.endPage();
    }

    private void saveFile() throws IOException {
        File file = new File(option.directoryToSave(), "result.html");
        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(painter.toString());
        writer.close();
    }

    public int pageCount() {
        return output.pages().length;
    }
}
