package kr.dogfoot.hwpdrawer.export.pdf;

import de.rototor.pdfbox.graphics2d.PdfBoxGraphics2DFontTextDrawer;
import kr.dogfoot.hwpdrawer.util.FontLoader;
import kr.dogfoot.hwpdrawer.util.FontSource;
import kr.dogfoot.hwpdrawer.util.SyntheticStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * PDF에 글자를 쓸 때 Java Font를 PDF 폰트로 이어 준다.
 *
 * <p>{@link FontLoader}가 기록해 둔 원본 폰트 파일을 서브셋으로 임베드하므로 PDF의 글자는 선택·검색된다.
 * 다음 경우는 PDF 폰트를 돌려주지 않고(null), Graphics2D 브리지가 그 글자를 윤곽선 도형으로 그린다.
 * 모양은 PNG와 같다.</p>
 * <ul>
 *   <li>시스템 폰트·대체 폰트처럼 원본 파일을 모를 때</li>
 *   <li>파일에 없는 굵게/기울임을 Java가 합성해서 그리는 글자일 때 ({@link SyntheticStyle})</li>
 *   <li>임베드·서브셋이 금지되었거나 PDFBox가 서브셋할 수 없는 글꼴일 때 ({@link EmbeddableFontFiles})</li>
 *   <li>PDF 폰트로 인코딩할 수 없는 글자(탭 같은 제어 문자)일 때 ({@link #canDrawText})</li>
 * </ul>
 *
 * <p>PDF 문서 하나당 하나를 만들어 쓰고, 문서를 저장한 뒤 닫는다.</p>
 */
final class PdfFontTextDrawer extends PdfBoxGraphics2DFontTextDrawer {
    private final EmbeddableFontFiles fontFiles = new EmbeddableFontFiles();
    private final SyntheticStyle syntheticStyle = new SyntheticStyle();
    /** 키: 폰트 파일 경로 + 모음 안 순번. */
    private final Map<String, PDFont> embeddedFonts = new HashMap<>();
    private final Set<String> unembeddableFonts = new HashSet<>();

    /**
     * 기본 판정(폰트 매핑·글자 방향·Java 폰트의 canDisplay)에 더해, 임베드할 PDF 폰트가 그 글자를 실제로
     * 인코딩할 수 있는지 본다. Java는 탭(U+0009) 같은 제어 문자를 보이지 않는 글리프로 처리해 canDisplay가
     * true지만 PDF 폰트에는 글리프가 없을 수 있다 — 그대로 쓰면 PDFBox가 SEVERE 로그를 남기고 글자를 버린다.
     * 이런 글자는 윤곽선 경로(Java와 같은 결과)로 보낸다.
     */
    @Override
    public boolean canDrawText(AttributedCharacterIterator text, IFontTextDrawerEnv env)
            throws IOException, FontFormatException {
        AttributedCharacterIterator probe = (AttributedCharacterIterator) text.clone();
        return super.canDrawText(text, env) && canEncodeEveryRun(probe, env);
    }

    private boolean canEncodeEveryRun(AttributedCharacterIterator text, IFontTextDrawerEnv env) {
        int runStart = text.getBeginIndex();
        while (runStart < text.getEndIndex()) {
            text.setIndex(runStart);
            int runLimit = text.getRunLimit();
            PDFont pdfFont = mapFont(fontOf(text, env), env);
            if (pdfFont == null || !canEncode(pdfFont, textBetween(text, runStart, runLimit))) return false;
            runStart = runLimit;
        }
        return true;
    }

    private static Font fontOf(AttributedCharacterIterator text, IFontTextDrawerEnv env) {
        Font font = (Font) text.getAttribute(TextAttribute.FONT);
        return font != null ? font : env.getFont();
    }

    private static String textBetween(AttributedCharacterIterator text, int start, int limit) {
        StringBuilder run = new StringBuilder(limit - start);
        for (int index = start; index < limit; index++) {
            run.append(text.setIndex(index));
        }
        return run.toString();
    }

    private static boolean canEncode(PDFont font, String text) {
        try {
            font.encode(text);
            return true;
        } catch (IllegalArgumentException | IOException e) {
            return false;
        }
    }

    @Override
    protected PDFont mapFont(Font font, IFontTextDrawerEnv env) {
        Optional<FontSource> source = FontLoader.instance().sourceOf(font);
        if (!source.isPresent() || syntheticStyle.isSynthesized(font)) return null;
        return embeddedFontOf(source.get(), env.getDocument());
    }

    private PDFont embeddedFontOf(FontSource source, PDDocument document) {
        String key = source.file().getAbsolutePath() + "#" + source.collectionIndex();
        if (unembeddableFonts.contains(key)) return null;

        PDFont cached = embeddedFonts.get(key);
        if (cached != null) return cached;
        try {
            PDFont loaded = PDType0Font.load(document, fontFiles.open(source), true);
            embeddedFonts.put(key, loaded);
            return loaded;
        } catch (IOException | RuntimeException e) {
            // 임베드할 수 없는 글꼴(금지·형식·손상) — 이 글꼴의 글자는 모두 윤곽선으로 그린다.
            unembeddableFonts.add(key);
            return null;
        }
    }

    @Override
    public void close() {
        super.close();
        fontFiles.close();
        embeddedFonts.clear();
        unembeddableFonts.clear();
    }
}
