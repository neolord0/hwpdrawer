package kr.dogfoot.hwpdrawer.drawer.para.nolineseg;

import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfoControl;
import kr.dogfoot.hwpdrawer.drawer.charInfo.CharInfoNormal;
import kr.dogfoot.hwpdrawer.output.InterimOutput;
import kr.dogfoot.hwpdrawer.util.Area;
import kr.dogfoot.hwpdrawer.input.DrawingInput;
import kr.dogfoot.hwplib.object.bodytext.control.*;
import kr.dogfoot.hwplib.object.bodytext.paragraph.text.*;

import java.io.UnsupportedEncodingException;

public class CharAdder {
    private final DrawingInput input;
    private final InterimOutput output;
    private final ParaDrawerWithoutLineSeg paraDrawer;
    private final WordDrawer wordDrawer;

    private final CharInfoBuffer charInfoBuffer;

    private int controlExtendCharIndex;
    private Area currentTextArea;

    public CharAdder(DrawingInput input, InterimOutput output, ParaDrawerWithoutLineSeg paraDrawer, WordDrawer wordDrawer) {
        this.input = input;
        this.output = output;
        this.paraDrawer = paraDrawer;
        this.wordDrawer = wordDrawer;

        this.charInfoBuffer = new CharInfoBuffer();
    }

    public void resetAtStartingPara() {
        controlExtendCharIndex = 0;
    }

    public void addChar(HWPChar ch, Area currentTextArea) throws Exception {
        this.currentTextArea = currentTextArea;
        wordDrawer.continueAddingChar();

        switch (ch.getType()) {
            case Normal:
                normalChar((HWPCharNormal) ch);
                break;
            case ControlChar:
                controlChar((HWPCharControlChar) ch);
                break;
            case ControlInline:
                break;
            case ControlExtend:
                controlExtend((HWPCharControlExtend) ch);
                break;
        }
    }

    private void normalChar(HWPCharNormal ch) throws Exception {
        CharInfoNormal charInfo = normalCharInfo(ch);
        if (charInfo.character().isSpace()) {
            wordDrawer.addWordToLine(charInfo);
        } else {
            wordDrawer.addCharOfWord(charInfo);
        }
    }

    private CharInfoNormal normalCharInfo(HWPCharNormal ch) throws UnsupportedEncodingException {
        CharInfoNormal charInfo = (CharInfoNormal) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            charInfo = new CharInfoNormal(ch, input.charShape(), input.paraIndex(), input.charIndex(), input.charPosition())
                    .calculateWidth();
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
        }
        return charInfo;
    }

    private void controlChar(HWPCharControlChar ch) throws Exception {
        if (ch.isParaBreak() || ch.isLineBreak()) {
            wordDrawer.addWordToLine(null);
            paraDrawer.setNewLine();
            paraDrawer.saveTextLineAndNewLine();
        }
    }

    private void controlExtend(HWPCharControlExtend ch) {
        if (ch.getCode() == 11) {
            tableOrGso(ch);
        } else if (ch.getCode() == 16) {
            headerFooter(ch);
        } else {
            controlExtendCharIndex++;
        }
    }

    private void tableOrGso(HWPCharControlExtend ch) {
        CharInfoControl charInfo = (CharInfoControl) charInfoBuffer.get(input.paraIndex(), input.charIndex());
        if (charInfo == null) {
            Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
            charInfo = CharInfoControl.create(ch, control, input, currentTextArea);
            charInfoBuffer.add(input.paraIndex(), input.charIndex(), charInfo);
            controlExtendCharIndex++;
        } else {
            charInfo.area(input, currentTextArea);
        }

        wordDrawer.addCharOfWord(charInfo);
    }

    private void headerFooter(HWPCharControlExtend ch) {
        Control control = input.currentPara().getControlList().get(controlExtendCharIndex);
        if (control.getType() == ControlType.Header) {
            ControlHeader header = (ControlHeader) control;
            switch (header.getHeader().getApplyPage()) {
                case BothPage:
                    input.pageInfo().bothHeader(header);
                    break;
                case EvenPage:
                    input.pageInfo().evenHeader(header);
                    break;
                case OddPage:
                    input.pageInfo().oddHeader(header);
                    break;
            }
        } else if (control.getType() == ControlType.Footer) {
            ControlFooter footer = (ControlFooter) control;
            switch (footer.getHeader().getApplyPage()) {
                case BothPage:
                    input.pageInfo().bothFooter(footer);
                    break;
                case EvenPage:
                    input.pageInfo().evenFooter(footer);
                    break;
                case OddPage:
                    input.pageInfo().oddFooter(footer);
                    break;
            }
        }
        controlExtendCharIndex++;
    }

    public ControlSectionDefine sectionDefine() {
        input.nextChar();
        HWPChar ch = input.currentChar();

        ControlSectionDefine sectionDefine = null;
        if (ch.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) ch).isSectionDefine()) {
            sectionDefine = (ControlSectionDefine) input.currentPara().getControlList().get(controlExtendCharIndex);
            controlExtendCharIndex++;
        } else {
            input.previousChar(1);
        }
        return sectionDefine;
    }

    public ControlColumnDefine columnDefine() {
        input.nextChar();
        HWPChar ch = input.currentChar();

        ControlColumnDefine columnDefine = null;
        if (ch != null && ch.getType() == HWPCharType.ControlExtend &&
                ((HWPCharControlExtend) ch).isColumnDefine()) {
            columnDefine = (ControlColumnDefine) input.currentPara().getControlList().get(controlExtendCharIndex);
            controlExtendCharIndex++;
        } else {
            input.previousChar(1);
        }
        return columnDefine;
    }

    public void clearBufferUntilPreviousPara() {
        charInfoBuffer.clearUntilPreviousPara();
    }
}
