package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaSpec;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Built-in project user path operations: the named directories stored in the
 * project metadata that nodes can reference by key.
 */
public class UserPathOperations {

    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Reads a string parameter of a user path entry; missing parameters and null
     * values become the empty string (the pinned list convention).
     */
    private static String stringValue(JIPipeDynamicParameterCollection entry, String key) {
        var access = entry.get(key);
        if (access == null) {
            return "";
        }
        String value = access.get(String.class);
        return value != null ? value : "";
    }

    public static class ListUserPaths implements AutomationOperation {
        @Override
        public String getId() {
            return "list_user_paths";
        }

        @Override
        public String getDescription() {
            return "List the user paths of the current project: named directories that nodes can reference by key";
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
                return mapper.createObjectNode().put("error", "No project loaded");
            }
            ObjectNode data = mapper.createObjectNode();
            ArrayNode userPaths = data.putArray("userPaths");
            for (JIPipeDynamicParameterCollection entry : project.getMetadata().getUserPaths().getPaths()) {
                ObjectNode item = userPaths.addObject();
                item.put("key", stringValue(entry, "key"));
                item.put("name", stringValue(entry, "name"));
                item.put("description", stringValue(entry, "description"));
                var pathAccess = entry.get("path");
                Path path = pathAccess != null ? pathAccess.get(Path.class) : null;
                item.put("path", path != null ? path.toString() : "");
                var roleAccess = entry.get("role");
                JIPipeProjectUserPaths.Role role = roleAccess != null
                        ? roleAccess.get(JIPipeProjectUserPaths.Role.class) : null;
                item.put("role", role != null ? role.name() : JIPipeProjectUserPaths.Role.Unspecified.name());
            }
            return data;
        }
    }

    public static class SetUserPath implements AutomationOperation {
        @Override
        public String getId() {
            return "set_user_path";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Create or update a user path entry of the current project, matched by its key. An existing key "
                    + "updates its entry in place (only provided fields change, the path is always overwritten); "
                    + "a new key appends an entry. CAUTION: a role of 'Output' makes the path a pipeline write target, "
                    + "so setting the role decides where pipelines may write. Paths are not restricted to export roots: "
                    + "the permission prompt of this operation is the consent gate";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("key", "The user path key, used by nodes to reference the path; an existing key updates its entry, a new key creates one").required()
                .string("path", "The directory or file path; it is stored as an absolute, normalized path").required()
                .string("name", "Name as displayed in the UI; the current name is kept when omitted")
                .string("description", "Description as displayed in the UI; the current description is kept when omitted")
                .enumeration("role", "Role of the path; 'Output' makes it a pipeline write target. New entries default to 'Unspecified'; on an update the current role is kept when omitted", "Input", "Output", "Unspecified", "Ignored")
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeProject project = ctx.getProject();
            if (project == null) throw new IllegalStateException("No project available");
            String key = params.get("key").asText();
            if (key.trim().isEmpty()) {
                throw new IllegalArgumentException("key must not be blank");
            }
            String pathText = params.get("path").asText();
            if (pathText.trim().isEmpty()) {
                throw new IllegalArgumentException("path must not be blank");
            }
            Path path = Path.of(pathText).toAbsolutePath().normalize();
            String name = params.has("name") ? params.get("name").asText() : null;
            String description = params.has("description") ? params.get("description").asText() : null;
            JIPipeProjectUserPaths.Role role = params.has("role")
                    ? JIPipeProjectUserPaths.Role.valueOf(params.get("role").asText()) : null;

            JIPipeProjectUserPaths userPaths = project.getMetadata().getUserPaths();

            // Upsert: match by key and set the fields directly on the entry (never
            // via setUserPath, which would drop name/description/role changes)
            boolean created = true;
            for (JIPipeDynamicParameterCollection entry : userPaths.getPaths()) {
                if (Objects.equals(key, entry.get("key").get(String.class))) {
                    entry.setParameter("path", path);
                    if (name != null) entry.setParameter("name", name);
                    if (description != null) entry.setParameter("description", description);
                    if (role != null) entry.setParameter("role", role);
                    created = false;
                    break;
                }
            }
            if (created) {
                JIPipeDynamicParameterCollection entry = userPaths.getPaths().addNewInstance();
                entry.setParameter("key", key);
                entry.setParameter("path", path);
                if (name != null) entry.setParameter("name", name);
                if (description != null) entry.setParameter("description", description);
                entry.setParameter("role", role != null ? role : JIPipeProjectUserPaths.Role.Unspecified);
            }

            ObjectNode result = mapper.createObjectNode();
            result.put("key", key);
            result.put("created", created);
            return result;
        }
    }

    public static class RemoveUserPath implements AutomationOperation {
        @Override
        public String getId() {
            return "remove_user_path";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Remove a user path entry of the current project by its key";
        }

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("key", "The user path key to remove, as reported by list_user_paths").required()
                .build();

        @Override
        public AutomationSchemaSpec getSchemaSpec() {
            return SPEC;
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Modify;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            JIPipeProject project = ctx.getProject();
            if (project == null) throw new IllegalStateException("No project available");
            String key = params.get("key").asText();

            List<String> knownKeys = new ArrayList<>();
            JIPipeDynamicParameterCollection matched = null;
            for (JIPipeDynamicParameterCollection entry : project.getMetadata().getUserPaths().getPaths()) {
                String currentKey = entry.get("key").get(String.class);
                if (matched == null && Objects.equals(key, currentKey)) {
                    matched = entry;
                }
                if (currentKey != null) {
                    knownKeys.add(currentKey);
                }
            }
            if (matched == null) {
                throw new IllegalArgumentException("Unknown user path key '" + key
                        + "'. Known keys: " + String.join(", ", knownKeys));
            }
            project.getMetadata().getUserPaths().getPaths().remove(matched);

            return mapper.createObjectNode().put("removed", true);
        }
    }
}
