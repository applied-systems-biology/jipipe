package edu.kit.datamanager.ro_crate.entities.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;

import java.io.IOException;
import java.util.Set;

/**
 * Class for serializing the hasPart property of a entity.
 */
public class HasPartSerializer extends StdSerializer<Set<String>> {

  public HasPartSerializer() {
    this(null);
  }

  public HasPartSerializer(Class<Set<String>> t) {
    super(t);
  }

  @Override
  public boolean isEmpty(SerializerProvider provider, Set<String> value) {
    return value.size() == 0;
  }

  @Override
  public void serialize(Set<String> value, JsonGenerator gen, SerializerProvider provider)
      throws IOException {
    ObjectMapper objectMapper = MyObjectMapper.getMapper();
    if (value.size() > 1) {
      gen.writeStartArray();
      for (String str : value) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("@id", str);
        gen.writeObject(node);
      }
      gen.writeEndArray();
    } else if (value.size() == 1) {
      gen.writeObject(objectMapper.createObjectNode().put("@id", value.iterator().next()));
    }
  }
}
