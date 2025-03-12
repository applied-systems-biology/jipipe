/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.parameters.library.markup;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.text.WordUtils;
import org.jsoup.Jsoup;

import java.io.IOException;
import java.util.Objects;

@JsonDeserialize(using = HTMLText.Deserializer.class)
@JsonSerialize(using = HTMLText.Serializer.class)
public class HTMLText {

    public static final HTMLText EMPTY = new HTMLText();

    private final String html;
    private String body;
    private String plainText;

    public HTMLText() {
        html = "<html><head></head><body></body></html>";
    }

    public HTMLText(String html) {
        if (html == null)
            html = "";
        if (!html.contains("<html>"))
            html = "<html><head></head><body>" + html + "</body></html>";
        this.html = html;
    }

    public HTMLText(HTMLText other) {
        this.html = other.html;
    }


    /**
     * Returns a copy of this text that introduces line-breaks
     *
     * @param columns number of columns to break at
     * @return new HTMLText instance
     */
    public HTMLText wrap(int columns) {
        String body = getBody();
        if (body.isEmpty())
            return new HTMLText();
        body = body.replace("\r", "").replace("\n", "");
        StringBuilder builder = new StringBuilder();
        for (String s : body.split("<br/>")) {
            builder.append(WordUtils.wrap(s, columns, "<br/>", true)).append("<br/>");
        }
        return new HTMLText(builder.toString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HTMLText htmlText = (HTMLText) o;
        return Objects.equals(html, htmlText.html);
    }

    @Override
    public int hashCode() {
        return Objects.hash(html);
    }

    /**
     * @return the full HTML with the root tag
     */
    public String getHtml() {
        return html;
    }

    /**
     * @return HTML without the root tag
     */
    public String getBody() {
        if (html == null) {
            return "";
        }
        if (body == null) {
            int bodyLocation = html.indexOf("<body>");
            if (bodyLocation < 0) {
                // Does not contain a body tag -> it's directly in <html>
                int start = "<html>".length();
                int end = html.indexOf("</html>");
                if (end < 0) {
                    body = "";
                    return body;
                }
                body = html.substring(start, end);
            } else {
                // Contains a body tag
                body = html.substring(bodyLocation + "<body>".length()).replace("</body>", "").replace("</html>", "").trim();
            }
        }
        return body;
    }

    @Override
    public String toString() {
        return html;
    }

    public MarkdownText toMarkdown() {
        return new MarkdownText(getBody());
    }

    public String toPlainText() {
        if (plainText == null) {
            try {
                plainText = Jsoup.parse(getHtml()).text();
            } catch (Throwable ignored) {
                plainText = getHtml();
            }
        }
        return plainText;
    }

    public static class Deserializer extends JsonDeserializer<HTMLText> {
        @Override
        public HTMLText deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            JsonNode node = p.readValueAsTree();
            return new HTMLText(node.asText());
        }
    }

    public static class Serializer extends JsonSerializer<HTMLText> {
        @Override
        public void serialize(HTMLText value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            gen.writeString(value.html);
        }
    }
}
