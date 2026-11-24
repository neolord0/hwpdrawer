package kr.dogfoot.hwpdrawer.export.pdf;

import kr.dogfoot.hwpdrawer.util.FontSource;
import org.apache.fontbox.ttf.OS2WindowsMetricsTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.OpenTypeFont;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeCollection;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBufferedFile;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 폰트 파일에서 PDF에 서브셋으로 임베드할 글꼴을 연다.
 *
 * <p>PDFBox는 임베드를 받아 준 뒤 <b>저장할 때</b> 서브셋을 만들다 실패하는 폰트가 있다 — CFF 윤곽선 폰트
 * ("OTF fonts do not have a glyf table")와 OS/2 fsType이 서브셋을 금지한 폰트. 그러면 PDF 전체 저장이
 * 실패하므로 미리 검사해 거절한다 (그 글자들은 윤곽선으로 그린다).</p>
 *
 * <p>연 파일은 저장 중 서브셋을 만들 때 다시 읽으므로 {@link #close()} 전까지 열어 둔다.
 * 폰트 모음(.ttc)은 파일마다 한 번만 연다.</p>
 */
final class EmbeddableFontFiles implements Closeable {
    private final Map<File, TrueTypeCollection> collections = new HashMap<>();
    private final List<TrueTypeFont> standaloneFonts = new ArrayList<>();

    /** @throws IOException 읽을 수 없거나 서브셋 임베드할 수 없는 글꼴일 때 */
    TrueTypeFont open(FontSource source) throws IOException {
        TrueTypeFont font = source.isCollection()
                ? faceOf(collectionOf(source.file()), source.collectionIndex())
                : parseStandalone(source.file());
        requireSubsettable(font, source.file());
        return font;
    }

    private TrueTypeCollection collectionOf(File file) throws IOException {
        TrueTypeCollection collection = collections.get(file);
        if (collection == null) {
            collection = new TrueTypeCollection(file);
            collections.put(file, collection);
        }
        return collection;
    }

    private static TrueTypeFont faceOf(TrueTypeCollection collection, int index) throws IOException {
        TrueTypeFont[] face = new TrueTypeFont[1];
        int[] position = {0};
        collection.processAllFonts(font -> {
            if (position[0]++ == index) face[0] = font;
        });
        if (face[0] == null) throw new IOException("폰트 모음에 " + index + "번 글꼴이 없습니다.");
        return face[0];
    }

    private TrueTypeFont parseStandalone(File file) throws IOException {
        TrueTypeFont font = parserFor(file).parse(new RandomAccessReadBufferedFile(file));
        standaloneFonts.add(font);
        return font;
    }

    private static TTFParser parserFor(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".otf") ? new OTFParser() : new TTFParser();
    }

    private static void requireSubsettable(TrueTypeFont font, File file) throws IOException {
        if (font instanceof OpenTypeFont && ((OpenTypeFont) font).isPostScript()) {
            throw new IOException("CFF 윤곽선 글꼴은 서브셋 임베드할 수 없습니다: " + file);
        }
        OS2WindowsMetricsTable os2 = font.getOS2Windows();
        if (os2 != null && (os2.getFsType() & OS2WindowsMetricsTable.FSTYPE_NO_SUBSETTING) != 0) {
            throw new IOException("서브셋을 금지한 글꼴입니다: " + file);
        }
    }

    @Override
    public void close() {
        for (TrueTypeCollection collection : collections.values()) closeQuietly(collection);
        for (TrueTypeFont font : standaloneFonts) closeQuietly(font);
        collections.clear();
        standaloneFonts.clear();
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            closeable.close();
        } catch (IOException ignored) {
            // 읽기 전용 파일 핸들 — 닫기 실패는 결과물에 영향이 없다.
        }
    }
}
