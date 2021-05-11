package kr.dogfoot.hwplib.drawer.drawinginfo.interims;

import kr.dogfoot.hwplib.drawer.drawinginfo.PageInfo;
import kr.dogfoot.hwplib.drawer.util.Area;
import kr.dogfoot.hwplib.drawer.util.MyStringBuffer;
import org.apache.poi.ss.usermodel.HeaderFooter;


public class PageOutput extends Output {
    private int pageNo;
    private Area paperArea;
    private Area bodyArea;
    private Area headerArea;
    private Area footerArea;

    private Content content;
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
        MyStringBuffer sb = new MyStringBuffer();
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
