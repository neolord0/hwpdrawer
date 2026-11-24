package kr.dogfoot.hwpdrawer.export.html;

/** HTML 본문/속성에 넣을 문자열 이스케이프. */
final class HtmlText {
    private HtmlText() {}

    static String escape(String text) {
        StringBuilder escaped = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            switch (ch) {
                case '&':  escaped.append("&amp;"); break;
                case '<':  escaped.append("&lt;"); break;
                case '>':  escaped.append("&gt;"); break;
                case '"':  escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;
                default:   escaped.append(ch);
            }
        }
        return escaped.toString();
    }
}
