package kr.dogfoot.hwpdrawer.output.page;

import kr.dogfoot.hwpdrawer.output.Content;
import kr.dogfoot.hwpdrawer.output.Output;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.util.MyStringBuilder;
import kr.dogfoot.hwpdrawer.input.PageInfo;
import kr.dogfoot.hwpdrawer.input.paralist.ColumnsInfo;

public class PageOutput extends Output {
    public final static PageOutput[] Zero_Array = new PageOutput[0];

    private final int pageNo;
    private final Area paperArea;
    private final Area bodyArea;
    private final Area headerArea;
    private final Area footerArea;

    private final Content content;
    private HeaderOutput headerOutput;
    private FooterOutput footerOutput;

    public PageOutput(PageInfo pageInfo, ColumnsInfo columnsInfo) {
        pageNo = pageInfo.pageNo();
        paperArea = pageInfo.paperArea();
        bodyArea = pageInfo.bodyArea();
        headerArea = pageInfo.headerArea();
        footerArea = pageInfo.footerArea();

        content = new Content(columnsInfo);
        headerOutput = null;
        footerOutput = null;
    }

    public int pageNo() {
        return pageNo;
    }

    public Area paperArea() {
        return paperArea;
    }

    public Area bodyArea() {
        return bodyArea;
    }

    @Override
    public Content content() {
        return content;
    }

    public boolean empty() {
        return content.empty();
    }

    public HeaderOutput createHeaderOutput() {
        headerOutput = new HeaderOutput(headerArea);
        return headerOutput;
    }

    public HeaderOutput headerOutput() {
        return headerOutput;
    }

    public FooterOutput createFooterOutput() {
        footerOutput = new FooterOutput(footerArea);
        return footerOutput;
    }

    public FooterOutput footerOutput() {
        return footerOutput;
    }

    @Override
    public Type type() {
        return Type.Page;
    }

    @Override
    public String test(int tabCount) {
        MyStringBuilder sb = new MyStringBuilder();
        sb.tab(tabCount).append("page ").append(Integer.toString(pageNo)).append(" - {\n");
        if (headerOutput != null) {
            sb.append(headerOutput.test(tabCount + 1));
        }
        sb.append(content.test(tabCount + 1));
        if (footerOutput != null) {
            sb.append(footerOutput.test(tabCount + 1));
        }
        sb.tab(tabCount).append("page - }\n");
        return sb.toString();
    }

}
