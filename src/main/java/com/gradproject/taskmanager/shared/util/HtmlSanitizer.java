package com.gradproject.taskmanager.shared.util;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private static final Safelist RICH_TEXT_SAFELIST = createRichTextSafelist();

    private static Safelist createRichTextSafelist() {
        return new Safelist()
                .addTags("p", "br", "strong", "b", "em", "i", "u", "s", "strike")
                .addTags("h1", "h2", "h3")
                .addTags("ul", "ol", "li")
                .addTags("blockquote", "pre", "code", "hr")
                .addTags("a")
                .addTags("span", "label", "input", "div")
                .addAttributes("a", "href", "target", "rel")
                .addAttributes(":all", "class")
                .addAttributes("span", "data-type", "data-id", "data-label")
                .addAttributes("input", "data-checked", "type", "checked")
                .addAttributes("li", "data-checked", "data-type")
                .addAttributes("ul", "data-type")
                .addProtocols("a", "href", "http", "https", "mailto");
    }

    public String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, RICH_TEXT_SAFELIST);
    }

    public String stripAll(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        return Jsoup.clean(html, Safelist.none());
    }
}
