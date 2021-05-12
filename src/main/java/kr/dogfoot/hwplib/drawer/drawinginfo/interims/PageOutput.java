package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.drawinginfo.PageInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuilder;

public class PageOutput extends Output {
    private final int pageNo;
    private final Area paperArea;
    private final Area bodyArea;
    private final Area headerArea;
    private final Area footerArea;

    private final Content content;
    private HeaderOutput headerOutput;
    private FooterOutput footerOutput;


    public PageOutput(PageInfo pageInfo) {
        pageNo = pageInfo.pageNo();
        paperArea = pageInfo.paperArea();
        bodyArea = pageInfo.bodyArea();
        headerArea = pageInfo.headerArea();
        footerArea = pageInfo.footerArea();

        content = new Content();
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
        sb.tab(tabCount).append("page - {\n");
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
