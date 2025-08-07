package edu.kit.datamanager.ro_crate.entities.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import edu.kit.datamanager.ro_crate.special.JsonUtilFunctions;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Serialization class used with jackson to serialize the entity.
 *
 * @author Nikola Tzotchev on 4.2.2022 г.
 * @version 1
 */
public class ObjectNodeSerializer extends StdSerializer<ObjectNode> {

  public ObjectNodeSerializer() {
    this(null);
  }

  protected ObjectNodeSerializer(Class<ObjectNode> t) {
    super(t);
  }

  @Override
  public void serialize(ObjectNode value, JsonGenerator jgen, SerializerProvider provider)
      throws IOException {

    JsonNode node = JsonUtilFunctions.unwrapSingleArray(value);
    final Iterator<Entry<String, JsonNode>> fields = node.fields();

    while (fields.hasNext()) {
      Map.Entry<String, JsonNode> field = fields.next();
      final String fieldName = field.getKey();
      JsonNode fieldValue = field.getValue();
      if (fieldValue.isObject() && fieldValue.size() == 0) {
        continue;
      }
      if (fieldValue.isNull()) {
        continue;
      }
      // if the type array contains only one type set it as String
      //  if (fieldName.equals("@type")) {
      if (fieldValue.isArray()) {
        if (fieldValue.isEmpty()) {
          continue;
        }
        ArrayNode arrayNode = (ArrayNode) fieldValue;
        int size = arrayNode.size();
        for (int i = size - 1; i >= 0; i--) {
          var element = arrayNode.get(i);
          if (element.isObject() && element.isEmpty()) {
            arrayNode.remove(i);
          }
        }
        if (arrayNode.isEmpty()) {
          continue;
        }
      }
      jgen.writeFieldName(fieldName);
      jgen.writeTree(fieldValue);
    }
  }

  @Override
  public boolean isUnwrappingSerializer() {
    return true;
  }

}
