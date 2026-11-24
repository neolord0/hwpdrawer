package kr.dogfoot.hwpdrawer.util;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static java.util.Map.entry;

/**
 * 로컬 폰트 파일을 로드하는 싱글톤 클래스.
 */
public class FontLoader {
    private static FontLoader instance = new FontLoader();

    public static FontLoader instance() {
        return instance;
    }

    /**
     * 요청한 폰트가 폴더/시스템 어디에도 없을 때 대신 사용할 호환 폰트.
     * 새굴림은 굴림의 유니코드 확장판으로 글리프 모양·전각 metric이 호환된다.
     *
     * <p>아래 한양/HY 계열은 폰트 파일을 구할 수 없어 계열(고딕/명조·굵기)만 맞춘
     * 근사 대체다 — metric이 원본과 다를 수 있다.</p>
     *
     * <p>별칭이 없으면 Java 논리 폰트 Dialog로 그려진다. Dialog(macOS 대체)의 한글 폭은 0.865em이라 원래 글꼴(1em)보다
     * 글자가 좁게 찍히고, 양쪽 정렬 줄의 단어 사이가 크게 벌어진다. 그래서 대체는 되도록 한글 폭이 1em인 같은 계열
     * 글꼴로 고른다 (함초롬 0.97em·나눔고딕 0.94em은 줄바꿈이 어긋난다). 예외는 같은 글꼴의 확장판이거나 문서가
     * 대체 글꼴로 지정한 함초롬돋움 두 개, 그리고 예전부터 있던 한양중고딕 → 나눔고딕이다.
     * 근거: 전 문서 조사(2026-11-24) — HWP에 저장된 줄바꿈과 같게 나오는 문단이 61/542 → 452/542. 페이지 수 변화 0건.</p>
     *
     * <p>별칭 글꼴은 보통 굵기라, 굵은 디자인 글꼴(견명조·견고딕)은 그릴 때 굵게를 더한다 ({@link #HEAVY_DESIGN_FONTS}).</p>
     */
    private static final Map<String, String> FONT_ALIASES = Map.ofEntries(
            entry("굴림", "새굴림"),
            entry("굴림체", "새굴림"),
            entry("한양중고딕", "나눔고딕"),
            entry("HY견고딕", "맑은 고딕"),
            entry("한양신명조", "바탕"),
            entry("신명 순명조", "바탕"),
            // 한글 내장(HFT) 글꼴 — FaceName 글꼴 유형 계열 1=명조, 2=고딕, 4=필기·궁서.
            // 명조는 영문이 반각 고정폭이라 고정폭 바탕체가 저장된 줄바꿈을 가장 잘 재현한다(139/155, 바탕은 94/155).
            entry("명조", "바탕체"),
            entry("오이", "바탕"),
            entry("고딕", "HY중고딕"),
            entry("필기", "궁서체"),
            entry("한양궁서", "궁서"),
            entry("한양견명조", "바탕"),
            entry("한양견고딕", "맑은 고딕"),
            // 신명 계열 — 중명조·견명조·중고딕·태고딕은 문서의 FaceName이 대체 글꼴로 휴먼명조/휴먼고딕을 지정한다.
            // 신명조는 대체 글꼴 지정이 없지만 같은 명조 계열이고 저장된 줄바꿈 재현이 바탕보다 낫다(9/10 vs 8/10).
            entry("신명 신명조", "휴먼명조"),
            entry("신명 중명조", "휴먼명조"),
            entry("신명 견명조", "휴먼명조"),
            entry("신명 중고딕", "휴먼고딕"),
            entry("신명 태고딕", "휴먼고딕"),
            // 명조/바탕 계열 TTF — 한글 폭 1em인 바탕.
            entry("HY신명조", "바탕"),
            entry("한컴바탕", "바탕"),
            entry("문체부 바탕체", "바탕"),
            entry("-윤명조120", "바탕"),
            entry("KoPub바탕체_Pro Light", "바탕"),
            entry("KoPub바탕체_Pro Medium", "바탕"),
            entry("KoPub바탕체_Pro Bold", "바탕"),
            entry("KoPub바탕체 Light", "바탕"),
            entry("KoPub바탕체 Bold", "바탕"),
            // 고딕/돋움 계열 TTF — 한글 폭 1em인 고딕 HY중고딕 (돋움체는 굴림체처럼 새굴림).
            entry("돋움", "HY중고딕"),
            entry("돋움체", "새굴림"),
            entry("KoPub돋움체_Pro Light", "HY중고딕"),
            entry("KoPub돋움체_Pro Medium", "HY중고딕"),
            entry("KoPub돋움체_Pro Bold", "HY중고딕"),
            entry("KoPub돋움체 Light", "HY중고딕"),
            entry("한컴 윤고딕 230", "HY중고딕"),
            entry("한컴 윤고딕 240", "HY중고딕"),
            entry("G마켓 산스 TTF Medium", "HY중고딕"),
            // 같은 글꼴의 다른 굵기/확장판.
            entry("HY울릉도L", "HY울릉도M"),
            entry("HY울릉도B", "HY울릉도M"),
            entry("함초롬돋움 확장", "함초롬돋움"),
            // FaceName의 대체 글꼴이 함초롬돋움이고, 한글 참조 이미지(ok)도 고딕으로 그린다.
            entry("서울한강 장체 B", "함초롬돋움"));

    /**
     * 굵은 디자인의 표시용 글꼴(견명조·견고딕). 별칭으로 대신하는 바탕·휴먼명조·맑은 고딕은 보통 굵기라 그대로 그리면
     * 가늘어진다 — 숭실대 학위논문 양식의 '제 1 장' 제목(한양견명조 16pt)이 바로 아래 진하게 13pt 소제목보다 가볍게
     * 보였다. 그래서 이 글꼴을 별칭으로 그릴 때는 굵게를 더한다 ({@link #substituteStyle}).
     */
    private static final Set<String> HEAVY_DESIGN_FONTS = Set.of("한양견명조", "신명 견명조", "한양견고딕", "HY견고딕");

    /**
     * 폰트 이름을 비교할 때 JVM 기본 로캘과 함께 늘 보는 언어.
     *
     * <p>Java는 폰트 이름을 기본 로캘의 언어로만 돌려준다 — en_US에서는 바탕이 'Batang', HY중고딕이 'HYGothic-Medium'이다.
     * HWP 문서는 글꼴을 대부분 한글 이름으로 지정하므로, 기본 로캘만 보면 같은 폰트 폴더라도 서버 로캘에 따라 찾고 못 찾고가
     * 갈린다(en_US에서는 이름 테이블에 한글 이름만 있는 휴먼명조·휴먼고딕만 찾고 나머지는 Dialog로 떨어졌다).</p>
     */
    private static final List<Locale> NAME_LOCALES = List.of(Locale.KOREAN, Locale.ENGLISH);

    private String fontPath;
    /** 사용자가 지정한 대체 글꼴 (글꼴 이름 → 대신 쓸 글꼴 이름). 내장 {@link #FONT_ALIASES}보다 먼저 본다. */
    private Map<String, String> substitutes = Map.of();
    /** 어디에서도 찾지 못한 글꼴을 그릴 글꼴. null 이면 Java 기본 글꼴(Dialog). */
    private String fallbackFont;
    /** 지금 대체 글꼴을 찾고 있는 글꼴 이름 — 사용자 대체가 서로를 가리켜도(A→B, B→A) 끝나게 한다. */
    private final Set<String> substituting = new HashSet<>();
    private Map<String, Font> loadedFonts = new HashMap<>();
    /** 파일에서 읽은 폰트의 출처. 키는 {@link #sourceKey(Font)} (deriveFont해도, 기본 로캘이 바뀌어도 같다). */
    private Map<String, FontSource> fontSources = new HashMap<>();
    /** {@link #substituteStyle} 캐시: 굵은 디자인 글꼴 이름 → 별칭으로 그리는지. */
    private Map<String, Boolean> drawnByAlias = new HashMap<>();

    private FontLoader() {}

    /** 폰트 폴더. 바뀌면 이미 고른 폰트를 버린다 (다른 폴더의 폰트를 계속 쓰지 않게). */
    public void fontPath(String path) {
        if (Objects.equals(this.fontPath, path)) return;
        this.fontPath = path;
        clear();
    }

    public String fontPath() {
        return fontPath;
    }

    /**
     * 폴더·시스템에 없는 글꼴을 대신할 글꼴 (글꼴 이름 → 대신 쓸 글꼴 이름). 내장 대체보다 먼저 보고, 대신 쓸 글꼴도
     * 없으면 내장 대체로 넘어간다. 바뀌면 이미 고른 폰트를 버린다.
     */
    public void substitutes(Map<String, String> substitutes) {
        Map<String, String> copy = Map.copyOf(substitutes);
        if (copy.equals(this.substitutes)) return;
        this.substitutes = copy;
        clear();
    }

    /**
     * 폴더·시스템·대체 글꼴 어디에서도 찾지 못한 글꼴을 그릴 글꼴 (폰트 폴더나 시스템에 있어야 한다). null 이면 Java 기본
     * 글꼴(Dialog) — 한글 글꼴이 없는 시스템에서는 글자가 □로 나올 수 있다. 바뀌면 이미 고른 폰트를 버린다.
     */
    public void fallbackFont(String fontName) {
        if (Objects.equals(this.fallbackFont, fontName)) return;
        this.fallbackFont = fontName;
        clear();
    }

    /**
     * @deprecated style 인자를 명시한 {@link #loadFont(String, int)} 사용을 권장.
     */
    @Deprecated
    public Font loadFont(String fontName) {
        return loadFont(fontName, Font.PLAIN);
    }

    /**
     * fontName + style 조합에 해당하는 폰트 파일을 로드한다.
     * 스타일별 폰트 파일이 있으면 우선 사용 (예: NotoSans-Bold.ttf for BOLD),
     * 없으면 Regular 폰트를 deriveFont로 사용.
     */
    public Font loadFont(String fontName, int style) {
        String key = fontName + "_" + style;
        if (loadedFonts.containsKey(key)) {
            return loadedFonts.get(key);
        }

        Font font = tryLoadFromFile(fontName, style);
        if (font == null && !systemFontAvailable(fontName)) {
            font = trySubstitute(fontName, style);
            if (font == null) font = tryAlias(fontName, style);
            if (font == null || isUnresolved(font)) font = Optional.ofNullable(tryFallback(style)).orElse(font);
        }
        if (font == null) {
            // 시스템 폰트에서 찾기 (없으면 Dialog 폴백)
            font = new Font(fontName, style, 12);
        }
        loadedFonts.put(key, font);
        return font;
    }

    /** 폰트 이름이 시스템에 실제로 존재하는지 확인한다 (없으면 Dialog로 해석됨). */
    private boolean systemFontAvailable(String fontName) {
        return !new Font(fontName, Font.PLAIN, 12).getFamily().equals(Font.DIALOG);
    }

    /** 폴더에도 시스템에도 없어 Java 기본 글꼴(Dialog)로 그려질 폰트인지. */
    private static boolean isUnresolved(Font font) {
        return font.getFamily(Locale.ENGLISH).equals(Font.DIALOG);
    }

    /** 사용자가 지정한 대체 글꼴. 그것도 찾지 못하면 null (내장 대체로 넘어간다). */
    private Font trySubstitute(String fontName, int style) {
        String substitute = substitutes.get(fontName);
        if (substitute == null || !substituting.add(fontName)) return null;
        try {
            Font font = loadFont(substitute, style);
            return isUnresolved(font) ? null : font;
        } finally {
            substituting.remove(fontName);
        }
    }

    private Font tryAlias(String fontName, int style) {
        String alias = FONT_ALIASES.get(fontName);
        return alias == null ? null : loadFont(alias, style);
    }

    /** 마지막 대체 글꼴 — 폴더나 시스템에서만 찾는다 (대체를 다시 거치면 끝나지 않을 수 있다). */
    private Font tryFallback(int style) {
        if (fallbackFont == null) return null;
        Font font = tryLoadFromFile(fallbackFont, style);
        if (font == null && systemFontAvailable(fallbackFont)) font = new Font(fallbackFont, style, 12);
        return font;
    }

    /**
     * fontName의 글자를 그릴 때 CharShape 스타일에 더할 스타일: 굵은 디자인 글꼴({@link #HEAVY_DESIGN_FONTS})을
     * 원래 글꼴이 폴더·시스템에 없어 별칭으로 그리면 BOLD, 그 밖에는 PLAIN.
     *
     * <p>글자를 그릴 때만 쓴다. 글자 폭(배치)은 원래대로 CharShape 스타일로 잰다 — 이 굵기는 HWP '진하게' 속성이 아니라
     * 글꼴 디자인이라 한글의 배치를 바꾸지 않는다.</p>
     */
    public int substituteStyle(String fontName) {
        if (!HEAVY_DESIGN_FONTS.contains(fontName)) return Font.PLAIN;
        return drawnByAlias.computeIfAbsent(fontName, this::isDrawnByAlias) ? Font.BOLD : Font.PLAIN;
    }

    /** 사용자가 대체 글꼴을 지정했으면 그 글꼴의 굵기를 그대로 쓴다 — 굵은 대체 글꼴에 굵게를 또 더하지 않는다. */
    private boolean isDrawnByAlias(String fontName) {
        return FONT_ALIASES.containsKey(fontName) && !substitutes.containsKey(fontName)
                && tryLoadFromFile(fontName, Font.PLAIN) == null && !systemFontAvailable(fontName);
    }

    private Font tryLoadFromFile(String fontName, int style) {
        if (fontPath == null) return null;

        File dir = new File(fontPath);
        if (!dir.exists() || !dir.isDirectory()) return null;

        File[] files = dir.listFiles();
        if (files == null) return null;

        boolean wantBold = (style & Font.BOLD) != 0;
        boolean wantItalic = (style & Font.ITALIC) != 0;

        // 매칭 점수가 가장 높은 폰트 선택. (bold 일치=2, italic 일치=1)
        Font bestFont = null;
        FontSource bestSource = null;
        int bestScore = -1;

        for (File file : files) {
            String name = file.getName().toLowerCase(Locale.ROOT);
            if (name.endsWith(".ttf") || name.endsWith(".ttc") || name.endsWith(".otf")) {
                try {
                    Font[] fonts;
                    if (name.endsWith(".ttc")) {
                        fonts = Font.createFonts(file);
                    } else {
                        fonts = new Font[]{Font.createFont(Font.TRUETYPE_FONT, file)};
                    }
                    for (int collectionIndex = 0; collectionIndex < fonts.length; collectionIndex++) {
                        Font f = fonts[collectionIndex];
                        if (hasName(f, fontName)) {
                            boolean isBoldFont = fullNameContainsAny(f, "bold", "heavy", "black");
                            boolean isItalicFont = fullNameContainsAny(f, "italic", "oblique");

                            int score = 0;
                            if (isBoldFont == wantBold) score += 2;
                            if (isItalicFont == wantItalic) score += 1;
                            if (score > bestScore) {
                                bestScore = score;
                                bestFont = f;
                                bestSource = new FontSource(file, collectionIndex);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 무시하고 다음 파일 시도
                }
            }
        }
        if (bestFont != null) fontSources.put(sourceKey(bestFont), bestSource);
        return bestFont;
    }

    /** 패밀리 이름·전체 이름 중 하나가 name과 같은지 — 기본 로캘 이름에 더해 한국어·영어 이름도 본다. */
    private static boolean hasName(Font font, String name) {
        if (font.getName().equals(name)) return true;
        for (Locale locale : nameLocales()) {
            if (font.getFamily(locale).equals(name) || font.getFontName(locale).equals(name)) return true;
        }
        return false;
    }

    /** 굵기·기울임은 전체 이름의 낱말(Bold, Italic…)로 판단한다 — 어느 언어의 전체 이름에 들어 있어도 된다. */
    private static boolean fullNameContainsAny(Font font, String... words) {
        for (Locale locale : nameLocales()) {
            String fullName = font.getFontName(locale).toLowerCase(Locale.ROOT);
            for (String word : words) {
                if (fullName.contains(word)) return true;
            }
        }
        return false;
    }

    /** 기본 로캘을 맨 앞에 둔다 — 지금까지 찾던 이름은 그대로 찾고, 한국어·영어 이름을 더 찾는다. */
    private static List<Locale> nameLocales() {
        List<Locale> locales = new ArrayList<>(NAME_LOCALES.size() + 1);
        locales.add(Locale.getDefault());
        locales.addAll(NAME_LOCALES);
        return locales;
    }

    /** {@link #fontSources}의 키. 기본 로캘에 따라 바뀌지 않도록 영문 전체 이름을 쓴다. */
    private static String sourceKey(Font font) {
        return font.getFontName(Locale.ENGLISH);
    }

    /**
     * 폰트 폴더의 파일에서 읽은 폰트면 그 출처를 돌려준다.
     * 시스템 폰트나 대체(Dialog) 폰트처럼 파일을 모르는 폰트는 비어 있다.
     */
    public Optional<FontSource> sourceOf(Font font) {
        return Optional.ofNullable(fontSources.get(sourceKey(font)));
    }

    public void clear() {
        loadedFonts.clear();
        fontSources.clear();
        drawnByAlias.clear();
    }
}
