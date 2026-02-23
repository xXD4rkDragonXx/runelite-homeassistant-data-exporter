package haexporterplugin.utils;

import com.google.common.net.UrlEscapers;
import lombok.experimental.UtilityClass;

@UtilityClass
public class ReplacementUtils {

    public Evaluable ofText(String text) {
        return new Text(text);
    }

    public Evaluable ofLink(String text, String link) {
        return link != null ? new TextWithLink(text, link) : ofText(text);
    }

    public Evaluable ofWiki(String text, String searchPhrase) {
        return ofLink(text, "https://oldschool.runescape.wiki/w/Special:Search?search=" + UrlEscapers.urlPathSegmentEscaper().escape(searchPhrase));
    }

    public Evaluable ofWiki(String phrase) {
        return ofWiki(phrase, phrase);
    }

    private record Text(String text) implements Evaluable {
            @Override
            public String evaluate(boolean rich) {
                return text;
            }
        }

    private record TextWithLink(String text, String link) implements Evaluable {
            @Override
            public String evaluate(boolean rich) {
                return rich ? String.format("[%s](%s)", text, link) : text;
            }
        }

}