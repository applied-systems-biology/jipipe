package edu.kit.datamanager.ro_crate.entities;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.kit.datamanager.ro_crate.objectmapper.MyObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class AbstractEntityTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    void mergeIdIntoValue_withInvalidId_returnsEmpty(String invalidId) {
        Optional<JsonNode> result = AbstractEntity.mergeIdIntoValue(invalidId, null);
        assertTrue(result.isEmpty(), "Should return empty Optional for invalid ID");

        Optional<JsonNode> result2 = AbstractEntity.mergeIdIntoValue(
                invalidId,
                MyObjectMapper.getMapper().createObjectNode()
        );
        assertTrue(result2.isEmpty(), "Should return empty Optional for invalid ID");
    }

    @Test
    void mergeIdIntoValue_withNullCurrentValue_returnsIdObject() {
        String id = "test-id";
        Optional<JsonNode> result = AbstractEntity.mergeIdIntoValue(id, null);

         // Should return a value for valid ID
        JsonNode node = result.orElseThrow();
        assertTrue(node.isObject(), "Should return an object");
        assertEquals(id, node.get("@id").asText(), "Should contain the ID");
    }

    @Test
    void mergeIdIntoValue_withExistingIdAsString_returnsEmpty() {
        String id = "test-id";
        JsonNode currentValue = MyObjectMapper.getMapper().valueToTree(id);

        Optional<JsonNode> result = AbstractEntity.mergeIdIntoValue(id, currentValue);
        assertTrue(result.isEmpty(), "Should return empty when ID already exists as string");
    }

    @Test
    void mergeIdIntoValue_withExistingIdObject_returnsEmpty() {
        String id = "test-id";
        ObjectNode currentValue = MyObjectMapper.getMapper().createObjectNode();
        currentValue.put("@id", id);

        Optional<JsonNode> result = AbstractEntity.mergeIdIntoValue(id, currentValue);
        assertTrue(result.isEmpty(), "Should return empty when ID already exists as object");
    }

    @Test
    void mergeIdIntoValue_withNonArrayValue_createsArray() {
        String id = "new-id";
        String existingId = "existing-id";
        ObjectNode existingValue = MyObjectMapper.getMapper().createObjectNode();
        existingValue.put("@id", existingId);

        Optional<JsonNode> result = AbstractEntity.mergeIdIntoValue(id, existingValue);

        assertTrue(result.isPresent(), "Should return a value");
        JsonNode node = result.get();
        assertTrue(node.isArray(), "Should be converted to array");
        assertEquals(2, node.size(), "Should contain both values");
        assertEquals(existingId, node.get(0).get("@id").asText(), "Should contain existing ID");
        assertEquals(id, node.get(1).get("@id").asText(), "Should contain new ID");
    }

    @Test
    void mergeIdIntoValue_withExistingArray_addsToArray() {
        String id = "new-id";
        String existingId = "existing-id";

        ArrayNode currentValue = MyObjectMapper.getMapper().createArrayNode();
        ObjectNode existingIdObj = MyObjectMapper.getMapper().createObjectNode();
        existingIdObj.put("@id", existingId);
        currentValue.add(existingIdObj);

        Optional<JsonNode> result = AbstractEntity.mergeIdIntoValue(id, currentValue);

        assertTrue(result.isPresent(), "Should return a value");
        JsonNode node = result.get();
        assertTrue(node.isArray(), "Should remain an array");
        assertEquals(2, node.size(), "Should contain both values");
        assertEquals(existingId, node.get(0).get("@id").asText(), "Should contain existing ID");
        assertEquals(id, node.get(1).get("@id").asText(), "Should contain new ID");
    }

    @Test
    void mergeIdIntoValue_withExistingArrayContainingId_returnsEmpty() {
        String id = "test-id";
        ArrayNode currentValue = MyObjectMapper.getMapper().createArrayNode();
        ObjectNode existingIdObj = MyObjectMapper.getMapper().createObjectNode();
        existingIdObj.put("@id", id);
        currentValue.add(existingIdObj);

        Optional<JsonNode> result = AbstractEntity.mergeIdIntoValue(id, currentValue);
        assertTrue(result.isEmpty(), "Should return empty when ID already exists in array");
    }
}

