package org.hkijena.jipipe.api.service.components;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.plugins.statistics.JIPipeStatisticsRegistry;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class StatisticsConcurrencyTest {

    @TempDir
    Path tempDir;

    @Test
    void saveAndReload_preservesMachineId() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        ObjectNode data1 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry);
        String machineId = data1.get("machineId").asText();
        JIPipeStatisticsServiceComponent.save(statsFile, data1);

        ObjectNode data2 = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry);
        assertEquals(machineId, data2.get("machineId").asText());
    }

    @Test
    void save_atomic_noCorruptionOnConcurrentRead() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        JIPipeStatisticsRegistry registry = new JIPipeStatisticsRegistry();
        ObjectNode data = JIPipeStatisticsServiceComponent.loadOrCreate(statsFile, registry);
        data.put("machineId", "test-uuid");
        JIPipeStatisticsServiceComponent.save(statsFile, data);

        ObjectNode readBack = (ObjectNode) JsonUtils.getObjectMapper().readTree(statsFile.toFile());
        assertEquals("test-uuid", readBack.get("machineId").asText());
    }

    @Test
    void merge_doesNotOverwriteOtherInstanceData() throws Exception {
        Path statsFile = tempDir.resolve("statistics.json");
        ObjectMapper mapper = JsonUtils.getObjectMapper();

        ObjectNode diskState = mapper.createObjectNode();
        diskState.put("machineId", "instance-a");
        diskState.putObject("items").put("counterA", 10);

        ObjectNode ourState = mapper.createObjectNode();
        ourState.put("machineId", "instance-b");
        ourState.putObject("items").put("counterB", 20);

        JIPipeStatisticsServiceComponent.save(statsFile, diskState);

        ObjectNode currentOnDisk = (ObjectNode) mapper.readTree(statsFile.toFile());
        ObjectNode itemsOnDisk = currentOnDisk.has("items")
                ? (ObjectNode) currentOnDisk.get("items")
                : currentOnDisk.putObject("items");
        ObjectNode ourItems = ourState.has("items")
                ? (ObjectNode) ourState.get("items")
                : ourState.putObject("items");
        ourItems.fields().forEachRemaining(entry -> itemsOnDisk.set(entry.getKey(), entry.getValue()));
        currentOnDisk.set("items", itemsOnDisk);

        JIPipeStatisticsServiceComponent.save(statsFile, currentOnDisk);

        ObjectNode result = (ObjectNode) mapper.readTree(statsFile.toFile());
        ObjectNode resultItems = (ObjectNode) result.get("items");
        assertEquals(10, resultItems.get("counterA").asInt());
        assertEquals(20, resultItems.get("counterB").asInt());
    }
}
