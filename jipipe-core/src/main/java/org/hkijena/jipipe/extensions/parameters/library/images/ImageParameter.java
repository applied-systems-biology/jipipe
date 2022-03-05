package org.hkijena.jipipe.extensions.parameters.library.images;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.scif.img.IO;
import org.hkijena.jipipe.utils.UIUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;

@JsonSerialize(using = ImageParameter.Serializer.class)
@JsonDeserialize(using = ImageParameter.Deserializer.class)
public class ImageParameter {

    private BufferedImage image;

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public static class Serializer extends JsonSerializer<ImageParameter> {
        @Override
        public void serialize(ImageParameter imageParameter, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            if(imageParameter.image == null) {
                jsonGenerator.writeNullField("data");
            }
            else {
                jsonGenerator.writeStringField("data", UIUtils.imageToBase64(imageParameter.image, "png"));
            }
            jsonGenerator.writeEndObject();
        }
    }

    public static class Deserializer extends JsonDeserializer<ImageParameter> {
        @Override
        public ImageParameter deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            ImageParameter parameter = new ImageParameter();
            JsonNode node = jsonParser.readValueAsTree();
            if(node.has("data") && !node.get("data").isNull()) {
                try {
                    parameter.image = UIUtils.base64ToImage(node.get("data").textValue());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return parameter;
        }
    }
}
