package org.hkijena.jipipe.extensions.parameters.primitives;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.commons.lang.WordUtils;
import org.hkijena.jipipe.ui.components.MarkdownDocument;

import java.io.IOException;

@JsonDeserialize(using = HTMLText.Deserializer.class)
@JsonSerialize(using = HTMLText.Serializer.class)
public class HTMLText {
    private String html;
    private String body;

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
                return "";
            }
            body = html.substring(bodyLocation + "<body>".length()).replace("</body>", "").replace("</html>", "").trim();
        }
        return body;
    }

    @Override
    public String toString() {
        return html;
    }

    public MarkdownDocument toMarkdown() {
        return new MarkdownDocument(getBody());
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
