package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataItemStore;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataTable;
import org.hkijena.jipipe.api.data.storage.JIPipeFileSystemWriteDataStorage;
import org.hkijena.jipipe.api.data.thumbnails.JIPipeThumbnailData;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.project.JIPipeProject;

import javax.imageio.ImageIO;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Built-in data operations: describe cached data entries and export them to the file system.
 */
public class DataOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Preview render size in pixels.
     */
    private static final int PREVIEW_SIZE = 256;

    private static ObjectNode error(String message) {
        return mapper.createObjectNode().put("error", message);
    }

    /**
     * Builds the parameter schema shared by both operations.
     *
     * @param withFormat if true, adds the required "format" and optional "target-dir" properties
     */
    private static JsonNode targetSchema(boolean withFormat) {
        if (withFormat) {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartment", "node", "slot", "format"],
                      "properties": {
                        "compartment": {"type": "string"},
                        "node": {"type": "string"},
                        "slot": {"type": "string"},
                        "entry": {"type": "integer"},
                        "format": {"type": "string"},
                        "target-dir": {"type": "string"}
                      },
                      "additionalProperties": false
                    }""");
        }
        return AutomationSchemaUtils.parseJson("""
                {
                  "type": "object",
                  "required": ["compartment", "node", "slot"],
                  "properties": {
                    "compartment": {"type": "string"},
                    "node": {"type": "string"},
                    "slot": {"type": "string"},
                    "entry": {"type": "integer"}
                  },
                  "additionalProperties": false
                }""");
    }

    /**
     * Resolution outcome shared by describe_data and export_data.
     * The error field is non-null if the entry could not be resolved.
     */
    private record ResolvedEntry(JsonNode error, JIPipeGraphNode node, String slotName, JIPipeDataTable table,
                                 int entry, JIPipeDataItemStore store, String dataTypeId, String dataTypeName) {
    }

    /**
     * Resolves the compartment, node, slot, cache entry, and data type of the requested entry.
     */
    private static ResolvedEntry resolveEntry(AutomationContext ctx, JsonNode params) {
        JIPipeProject project = ctx.getProject();
        if (project == null) {
            return new ResolvedEntry(error("No project loaded"), null, null, null, 0, null, null, null);
        }
        String compartmentRef = params.get("compartment").asText();
        var compartment = AutomationOperationUtils.findCompartment(project, compartmentRef);
        if (compartment == null) {
            return new ResolvedEntry(error("Compartment not found: " + compartmentRef), null, null, null, 0, null, null, null);
        }
        String nodeRef = params.get("node").asText();
        JIPipeGraphNode node = ctx.getGraph().findNode(nodeRef);
        if (node == null) {
            return new ResolvedEntry(error("Node not found: " + nodeRef), null, null, null, 0, null, null, null);
        }
        UUID nodeCompartment = ctx.getGraph().getCompartmentUUIDOf(node);
        if (!Objects.equals(compartment.getProjectCompartmentUUID(), nodeCompartment)) {
            return new ResolvedEntry(error("Node '" + nodeRef + "' is not in compartment '" + compartmentRef + "'"),
                    null, null, null, 0, null, null, null);
        }
        String slotName = params.get("slot").asText();
        JIPipeDataTable table = resolveDataTable(ctx, project, node, slotName);
        if (table == null) {
            return new ResolvedEntry(error("Slot not found: " + slotName), null, null, null, 0, null, null, null);
        }
        if (table.getRowCount() == 0) {
            return new ResolvedEntry(error("No cached data for slot '" + slotName + "' of node '" + nodeRef + "'"),
                    null, null, null, 0, null, null, null);
        }
        int entry = params.has("entry") ? params.get("entry").asInt() : 0;
        if (entry < 0 || entry >= table.getRowCount()) {
            return new ResolvedEntry(error("Cache entry " + entry + " out of range: slot '" + slotName + "' has "
                    + table.getRowCount() + " entries"), null, null, null, 0, null, null, null);
        }
        JIPipeDataItemStore store = table.getDataItemStore(entry);
        Class<? extends JIPipeData> dataClass = store.getDataClass();
        return new ResolvedEntry(null, node, slotName, table, entry, store,
                JIPipe.getDataTypes().getIdOf(dataClass), JIPipeData.getNameOf(dataClass));
    }

    /**
     * Returns the data table of a slot: the project cache if the node has a cached slot with that name,
     * otherwise the live output or input slot (the resolution used by query_data).
     */
    private static JIPipeDataTable resolveDataTable(AutomationContext ctx, JIPipeProject project,
                                                    JIPipeGraphNode node, String slotName) {
        Map<String, JIPipeDataTable> cached = project.getCache().query(node, node.getUUIDInParentGraph(),
                ctx.getProgressInfo());
        JIPipeDataTable cachedTable = cached.get(slotName);
        if (cachedTable != null) {
            return cachedTable;
        }
        JIPipeDataSlot slot = node.getOutputSlot(slotName);
        if (slot == null) {
            slot = node.getInputSlot(slotName);
        }
        return slot;
    }

    /**
     * Adds row/column information of table-like data to the result.
     */
    private static void putTableInfo(ObjectNode data, JIPipeData dataObj) {
        if (!(dataObj instanceof TableModel table)) {
            return;
        }
        data.put("rows", table.getRowCount());
        data.put("columns", table.getColumnCount());
        var columnNames = data.putArray("columnNames");
        for (int i = 0; i < table.getColumnCount(); i++) {
            String name = table.getColumnName(i);
            columnNames.add(name != null ? name : "");
        }
    }

    /**
     * Duck-types image dimensions via no-arg getters. Image data types live in optional modules,
     * so jipipe-core cannot reference them directly. Zero or inaccessible values are omitted.
     */
    private static void putImageDimensions(ObjectNode data, JIPipeData dataObj) {
        Integer width = invokeIntGetter(dataObj, "getWidth");
        Integer height = invokeIntGetter(dataObj, "getHeight");
        if (width == null || height == null || width <= 0 || height <= 0) {
            return;
        }
        data.put("width", width);
        data.put("height", height);
        Integer depth = invokeIntGetter(dataObj, "getNSlices");
        if (depth != null) {
            data.put("depth", depth);
        }
    }

    private static Integer invokeIntGetter(Object obj, String method) {
        try {
            Method getter = obj.getClass().getMethod(method);
            if (!getter.getReturnType().equals(int.class) && !getter.getReturnType().equals(Integer.class)) {
                return null;
            }
            Object value = getter.invoke(obj);
            return value instanceof Number number ? number.intValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Renders a cheap PNG preview of the data via its thumbnail and adds it as the
     * image-base64/mime envelope. Omits the fields if no thumbnail can be rendered.
     */
    private static void putPreview(ObjectNode data, JIPipeData dataObj, JIPipeProgressInfo progressInfo) {
        try {
            JIPipeThumbnailData thumbnail = dataObj.createThumbnail(PREVIEW_SIZE, PREVIEW_SIZE, progressInfo);
            if (thumbnail == null) {
                return;
            }
            Component component = thumbnail.renderToComponent(PREVIEW_SIZE, PREVIEW_SIZE);
            if (component == null) {
                return;
            }
            component.setSize(PREVIEW_SIZE, PREVIEW_SIZE);
            BufferedImage image = new BufferedImage(PREVIEW_SIZE, PREVIEW_SIZE, BufferedImage.TYPE_INT_ARGB);
            var graphics = image.createGraphics();
            try {
                component.print(graphics);
            } finally {
                graphics.dispose();
            }
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", stream);
            data.put("image-base64", Base64.getEncoder().encodeToString(stream.toByteArray()));
            data.put("mime", "image/png");
        } catch (Exception e) {
            // No cheap preview available: omit the image fields
        }
    }

    /**
     * Returns the accepted export format descriptors of a data type: the native JIPipe storage export,
     * the data type id, and the data type display name.
     */
    private static List<String> acceptedFormats(String dataTypeId, String dataTypeName) {
        LinkedHashSet<String> formats = new LinkedHashSet<>();
        formats.add("jipipe");
        if (dataTypeId != null) {
            formats.add(dataTypeId);
        }
        if (dataTypeName != null) {
            formats.add(dataTypeName);
        }
        return new ArrayList<>(formats);
    }

    /**
     * Resolution outcome of the export target guard. The path is null if the target was refused.
     */
    private record TargetDir(Path path, String errorMessage) {
    }

    /**
     * Resolves the export target directory against the allowed roots
     * ({@link AutomationOperationUtils#allowedRoots}). A blank target-dir defaults to the
     * jipipe-mcp-export system directory; the guard itself is shared with the screenshot
     * save-to targets.
     */
    private static TargetDir resolveTargetDir(JIPipeProject project, JsonNode params) {
        if (params.has("target-dir") && !params.get("target-dir").asText().isBlank()) {
            AutomationOperationUtils.TargetPath resolved = AutomationOperationUtils.resolveWriteTarget(
                    project, "target-dir", params.get("target-dir").asText());
            return new TargetDir(resolved.path(), resolved.errorMessage());
        }
        return new TargetDir(AutomationOperationUtils.systemExportRoot(), null);
    }

    public static class DescribeData implements AutomationOperation {
        @Override
        public String getId() {
            return "describe_data";
        }

        @Override
        public String getDescription() {
            return "Describe a cached data entry: data type, entry counts, table or image dimensions, and a PNG preview if available";
        }

        @Override
        public JsonNode getInputSchema() {
            return targetSchema(false);
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            ResolvedEntry resolved = resolveEntry(ctx, params);
            if (resolved.error() != null) {
                return resolved.error();
            }
            JIPipeData dataObj;
            try {
                dataObj = resolved.store().getData(ctx.getProgressInfo());
            } catch (Exception e) {
                return error("Data is not accessible: " + e.getMessage());
            }
            ObjectNode data = mapper.createObjectNode();
            data.put("node", ctx.getGraph().getAliasIdOf(resolved.node()));
            data.put("slot", resolved.slotName());
            data.put("entry", resolved.entry());
            data.put("entryCount", resolved.table().getRowCount());
            data.put("dataType", resolved.dataTypeId());
            data.put("dataTypeDisplayName", resolved.dataTypeName());
            putTableInfo(data, dataObj);
            putImageDimensions(data, dataObj);
            putPreview(data, dataObj, ctx.getProgressInfo());
            return data;
        }
    }

    public static class ExportData implements AutomationOperation {
        @Override
        public String getId() {
            return "export_data";
        }

        @Override
        public String getDescription() {
            return "Export a cached data entry into an allowed export directory (default: the jipipe-mcp-export "
                    + "directory in the system temp directory) and return the written location. All accepted "
                    + "formats (jipipe, the data type id, and the data type display name, matched "
                    + "case-insensitively) route to the data type's native export: the format only selects "
                    + "the accepted spellings, never a different writer. The saved-to field of the result "
                    + "reports the target directory the exported files were written into";
        }

        @Override
        public JsonNode getInputSchema() {
            return targetSchema(true);
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Query;
        }

        @Override
        public boolean isReadOnly() {
            return true;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeProject project = ctx.getProject();
            if (project == null) {
                return error("No project loaded");
            }
            ResolvedEntry resolved = resolveEntry(ctx, params);
            if (resolved.error() != null) {
                return resolved.error();
            }
            String format = params.get("format").asText();
            List<String> accepted = acceptedFormats(resolved.dataTypeId(), resolved.dataTypeName());
            String normalized = format.trim().toLowerCase();
            if (accepted.stream().noneMatch(candidate -> candidate.toLowerCase().equals(normalized))) {
                return error("Unknown export format '" + format + "'. Accepted formats (matched case-insensitively): "
                        + String.join(", ", accepted));
            }
            TargetDir targetDir = resolveTargetDir(project, params);
            if (targetDir.path() == null) {
                return error(targetDir.errorMessage());
            }
            Path target = targetDir.path();
            try {
                Files.createDirectories(target);
                resolved.store().getData(ctx.getProgressInfo()).exportData(
                        new JIPipeFileSystemWriteDataStorage(ctx.getProgressInfo(), target), "data", true,
                        ctx.getProgressInfo());
            } catch (Exception e) {
                return error("Export failed: " + e.getMessage());
            }
            ObjectNode data = mapper.createObjectNode();
            data.put("saved-to", target.toAbsolutePath().normalize().toString());
            data.put("dataType", resolved.dataTypeId());
            return data;
        }
    }
}
