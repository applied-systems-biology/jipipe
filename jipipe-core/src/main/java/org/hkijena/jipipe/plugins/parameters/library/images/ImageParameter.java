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

package org.hkijena.jipipe.plugins.parameters.library.images;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.hkijena.jipipe.utils.BufferedImageUtils;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;

@JsonSerialize(using = ImageParameter.Serializer.class)
@JsonDeserialize(using = ImageParameter.Deserializer.class)
public class ImageParameter {

    private BufferedImage image;

    public ImageParameter() {
    }

    public ImageParameter(BufferedImage image) {
        this.image = image;
    }

    public ImageParameter(ImageParameter other) {
        if (other.image != null) {
            this.image = BufferedImageUtils.copyBufferedImage(other.image);
        }
    }

    public ImageParameter(URL resource) {
        this(BufferedImageUtils.toBufferedImage(new ImageIcon(resource).getImage(), BufferedImage.TYPE_INT_ARGB));
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImageParameter that = (ImageParameter) o;
        return Objects.equals(image, that.image);
    }

    @Override
    public int hashCode() {
        return Objects.hash(image);
    }

    public static class Serializer extends JsonSerializer<ImageParameter> {
        @Override
        public void serialize(ImageParameter imageParameter, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            if (imageParameter.image == null) {
                jsonGenerator.writeNullField("data");
            } else {
                jsonGenerator.writeStringField("data", BufferedImageUtils.imageToBase64(imageParameter.image, "png"));
            }
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ImageParameter> {
        @Override
        public ImageParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ImageParameter parameter = new ImageParameter();
            JsonNode node = jsonParser.readValueAsTree();
            if (node.has("data") && !node.get("data").isNull()) {
                try {
                    parameter.image = BufferedImageUtils.base64ToImage(node.get("data").textValue());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return parameter;
        }
    }
}
