package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.events.CompartmentChangedEvent;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;

/**
 * Built-in modification operations.
 */
public class ModificationOperations {

    public static class AddNode implements AutomationOperation {
        @Override
        public String getId() {
            return "add_node";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Add a node to a compartment";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-type-id"],
                      "properties": {
                        "compartment-id": {"type": "string", "description": "Compartment to add the node to (UUID or alias); omit to add the node without a compartment"},
                        "node-type-id": {"type": "string", "description": "The id of the node type to add, as returned by list_algorithms"},
                        "x": {"type": "integer", "description": "Initial node position on the canvas (default 0)"},
                        "y": {"type": "integer", "description": "Initial node position on the canvas (default 0)"}
                      },
                      "additionalProperties": false
                    }""");
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
            String compartment = params.has("compartment-id") ? params.get("compartment-id").asText() : null;
            String nodeType = params.get("node-type-id").asText();
            int x = params.has("x") ? params.get("x").asInt() : 0;
            int y = params.has("y") ? params.get("y").asInt() : 0;
            String alias = JIPipeAutomationAPI.addNode(ctx, compartment, nodeType, x, y);
            // Result payload key: the kebab-case convention applies to input parameters only
            ObjectNode result = new ObjectMapper().createObjectNode();
            result.put("nodeId", alias);
            return result;
        }
    }

    public static class RemoveNode implements AutomationOperation {
        @Override
        public String getId() {
            return "remove_node";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Remove a node from the graph";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"}
                      },
                      "additionalProperties": false
                    }""");
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
            JIPipeAutomationAPI.removeNode(ctx, params.get("node-id").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class MoveNode implements AutomationOperation {
        @Override
        public String getId() {
            return "move_node";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Move a node to a grid position (1 cell = 25 px). Works on any node, including canvas notes. No overlap checks are applied: notes may be placed on top of nodes. Use arrange_nodes for automatic topological layout. An open graph canvas shows the new position after its next refresh";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node", "x", "y"],
                      "properties": {
                        "node": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"},
                        "x": {"type": "integer", "description": "Canvas column in grid cells (1 cell = 25 px)"},
                        "y": {"type": "integer", "description": "Canvas row in grid cells (1 cell = 25 px)"}
                      },
                      "additionalProperties": false
                    }""");
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
            return JIPipeAutomationAPI.moveNode(ctx, params.get("node").asText(), params.get("x").asInt(), params.get("y").asInt());
        }
    }

    public static class CreateNote implements AutomationOperation {
        @Override
        public String getId() {
            return "create_note";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Create a canvas note in one call: a text box (title, content, size, position) or a comment (title is the node name). Sizes and positions are grid cells (1 cell = 25 px). Edit the note later with set_parameter (text-title, text-content, grid-width, grid-height) and delete it with remove_node. The response reports width/height as grid cells; query_notes reports the same sizes in pixels";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartment"],
                      "properties": {
                        "compartment": {"type": "string", "description": "Compartment to create the note in (UUID, alias, or name); required, the note is never created without a compartment"},
                        "kind": {"type": "string", "enum": ["text-box", "comment"], "default": "text-box", "description": "The note kind: a text box has title, content, size, and position; a comment carries its title as the node name"},
                        "title": {"type": "string", "description": "The note title (default: Text box for a text box, Comment for a comment)"},
                        "content": {"type": "string", "description": "Text-box content; only valid for kind text-box"},
                        "x": {"type": "integer", "default": 0, "description": "Canvas column in grid cells (1 cell = 25 px)"},
                        "y": {"type": "integer", "default": 0, "description": "Canvas row in grid cells (1 cell = 25 px)"},
                        "width": {"type": "integer", "default": 4, "description": "Text-box width in grid cells; only valid for kind text-box"},
                        "height": {"type": "integer", "default": 3, "description": "Text-box height in grid cells; only valid for kind text-box"}
                      },
                      "additionalProperties": false
                    }""");
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
        public boolean appliesSchemaDefaults() {
            // The schema declares width/height defaults (they document the text-box behavior),
            // but the execute body passes absent keys as null on purpose, so createNote can
            // refuse text-box-only parameters on a comment. Injecting the defaults would turn
            // every comment creation without width/height into "width is only supported for
            // kind text-box". The schema's width/height defaults are a spec violation to fix
            // once the pre-migration snapshot no longer pins this schema.
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            // Omitted optional parameters are passed as null (not as defaults): the
            // static method must see WHICH text-box-only keys were provided so it can
            // refuse them on a comment, and it applies the defaults itself
            String kind = params.has("kind") ? params.get("kind").asText() : "text-box";
            String title = params.has("title") ? params.get("title").asText() : null;
            String content = params.has("content") ? params.get("content").asText() : null;
            int x = params.has("x") ? params.get("x").asInt() : 0;
            int y = params.has("y") ? params.get("y").asInt() : 0;
            Integer width = params.has("width") ? params.get("width").asInt() : null;
            Integer height = params.has("height") ? params.get("height").asInt() : null;
            return JIPipeAutomationAPI.createNote(ctx, params.get("compartment").asText(), kind, title, content, x, y, width, height);
        }
    }

    public static class AddConnection implements AutomationOperation {
        @Override
        public String getId() {
            return "add_connection";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Connect two slots";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["source", "source-slot", "target", "target-slot"],
                      "properties": {
                        "source": {"type": "string", "description": "The source node id as used by query_graph (alias id or UUID)"},
                        "source-slot": {"type": "string", "description": "Name of the source node's output slot"},
                        "target": {"type": "string", "description": "The target node id as used by query_graph (alias id or UUID)"},
                        "target-slot": {"type": "string", "description": "Name of the target node's input slot"}
                      },
                      "additionalProperties": false
                    }""");
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
            JIPipeAutomationAPI.addConnection(ctx,
                    params.get("source").asText(), params.get("source-slot").asText(),
                    params.get("target").asText(), params.get("target-slot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class RemoveConnection implements AutomationOperation {
        @Override
        public String getId() {
            return "remove_connection";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Disconnect two slots";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["source", "source-slot", "target", "target-slot"],
                      "properties": {
                        "source": {"type": "string", "description": "The source node id as used by query_graph (alias id or UUID)"},
                        "source-slot": {"type": "string", "description": "Name of the source node's output slot"},
                        "target": {"type": "string", "description": "The target node id as used by query_graph (alias id or UUID)"},
                        "target-slot": {"type": "string", "description": "Name of the target node's input slot"}
                      },
                      "additionalProperties": false
                    }""");
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
            JIPipeAutomationAPI.removeConnection(ctx,
                    params.get("source").asText(), params.get("source-slot").asText(),
                    params.get("target").asText(), params.get("target-slot").asText());
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }

    public static class SetParameter implements AutomationOperation {
        @Override
        public String getId() {
            return "set_parameter";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Set a parameter on a node. The response carries the effective value after conversion - verify it matches your intent.";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["node-id", "key", "value"],
                      "properties": {
                        "node-id": {"type": "string", "description": "The node id as used by query_graph (alias id or UUID)"},
                        "key": {"type": "string", "description": "Parameter key as reported by query_node or get_node_docs"},
                        "value": {"type": "string", "description": "New value as text; it is converted to the parameter type (numbers, booleans, enum names, paths)"}
                      },
                      "additionalProperties": false
                    }""");
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
            ObjectNode parameter = JIPipeAutomationAPI.setParameter(ctx,
                    params.get("node-id").asText(), params.get("key").asText(), params.get("value").asText());
            ObjectNode result = new ObjectMapper().createObjectNode().put("success", true);
            result.set("parameter", parameter);
            return result;
        }
    }

    public static class SetProjectMetadata implements AutomationOperation {

        /**
         * Schema of one authors/acknowledgements entry. Shared by both array
         * parameters so the two shapes cannot drift apart.
         */
        private static final String AUTHOR_ENTRY_SCHEMA = """
                {
                  "type": "object",
                  "description": "One author entry: firstName and lastName are required (non-blank), all other fields are optional",
                  "properties": {
                    "firstName": {"type": "string", "description": "Author first name (required, non-blank)"},
                    "lastName": {"type": "string", "description": "Author last name (required, non-blank)"},
                    "orcid": {"type": "string", "description": "ORCID (URL or ID)"},
                    "email": {"type": "string", "description": "E-Mail address"},
                    "website": {"type": "string", "description": "Author website"},
                    "firstAuthor": {"type": "boolean", "description": "Marks the author as first author"},
                    "correspondingAuthor": {"type": "boolean", "description": "Marks the author as corresponding author"},
                    "affiliations": {
                      "type": "array",
                      "description": "Author affiliations",
                      "items": {
                        "type": "object",
                        "description": "One affiliation: name is required (non-blank), ror and website are optional",
                        "required": ["name"],
                        "properties": {
                          "name": {"type": "string", "description": "Organization name (required, non-blank)"},
                          "ror": {"type": "string", "description": "ROR ID or URL"},
                          "website": {"type": "string", "description": "Organization website"}
                        },
                        "additionalProperties": false
                      }
                    }
                  },
                  "additionalProperties": false
                }""";

        @Override
        public String getId() {
            return "set_project_metadata";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Set project name, description, license, summary, website, citation, authors, or acknowledgements. The FAIR check needs license, summary, and at least one author - see check_fair.";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "properties": {
                        "name": {"type": "string", "description": "New project name; the current name is kept when omitted"},
                        "description": {"type": "string", "description": "New project description; the current description is kept when omitted"},
                        "license": {"type": "string", "description": "New license name like MIT or GPL v3; the current license is kept when omitted; a blank string is stored as-is (clears the field)"},
                        "summary": {"type": "string", "description": "New short summary; the current summary is kept when omitted; a blank string is stored as-is (clears the field)"},
                        "website": {"type": "string", "description": "New project website; the current website is kept when omitted; a blank string is stored as-is (clears the field)"},
                        "citation": {"type": "string", "description": "New citation of the work where the project is published; the current citation is kept when omitted; a blank string is stored as-is (clears the field)"},
                        "authors": {"type": "array", "description": "Replaces the whole author list (no merge): a present array, even an empty one, replaces the current list, only an omitted key keeps it. Each entry needs a non-blank firstName and lastName; one invalid entry refuses the whole call and leaves the project untouched", "items": %s},
                        "acknowledgements": {"type": "array", "description": "Replaces the whole acknowledgements list (no merge): a present array, even an empty one, replaces the current list, only an omitted key keeps it. Each entry needs a non-blank firstName and lastName; one invalid entry refuses the whole call and leaves the project untouched", "items": %s}
                      },
                      "additionalProperties": false
                    }""".formatted(AUTHOR_ENTRY_SCHEMA, AUTHOR_ENTRY_SCHEMA));
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
            String name = params.has("name") ? params.get("name").asText() : null;
            String description = params.has("description") ? params.get("description").asText() : null;
            String license = params.has("license") ? params.get("license").asText() : null;
            String summary = params.has("summary") ? params.get("summary").asText() : null;
            String website = params.has("website") ? params.get("website").asText() : null;
            String citation = params.has("citation") ? params.get("citation").asText() : null;
            // Both arrays are parsed and fully validated before anything is applied,
            // so an invalid entry refuses the whole call
            JIPipeAuthorMetadata.List authors = params.has("authors")
                    ? parseAuthors(params.get("authors"), "authors") : null;
            JIPipeAuthorMetadata.List acknowledgements = params.has("acknowledgements")
                    ? parseAuthors(params.get("acknowledgements"), "acknowledgements") : null;
            JIPipeAutomationAPI.setProjectMetadata(ctx, name, description, license, summary, website, citation,
                    authors, acknowledgements);
            return new ObjectMapper().createObjectNode().put("success", true);
        }

        /**
         * Parses an authors or acknowledgements array parameter into a typed list.
         * The whole array is validated before anything is built: every entry must
         * carry a non-blank firstName and lastName, and the first invalid entry
         * refuses the whole call with an error naming the index and the missing
         * field, so the project keeps its current lists.
         */
        private static JIPipeAuthorMetadata.List parseAuthors(JsonNode array, String listName) {
            if (!array.isArray()) {
                throw new IllegalArgumentException(listName + " must be an array");
            }
            for (int i = 0; i < array.size(); i++) {
                JsonNode entry = array.get(i);
                for (String field : new String[]{"firstName", "lastName"}) {
                    JsonNode value = entry.get(field);
                    if (value == null || !value.isTextual() || value.asText().trim().isEmpty()) {
                        throw new IllegalArgumentException("Invalid " + listName + " entry at index " + i
                                + ": " + field + " is required");
                    }
                }
            }
            JIPipeAuthorMetadata.List list = new JIPipeAuthorMetadata.List();
            for (JsonNode entry : array) {
                JIPipeAuthorMetadata author = new JIPipeAuthorMetadata();
                author.setFirstName(entry.get("firstName").asText());
                author.setLastName(entry.get("lastName").asText());
                String orcid = optionalString(entry, "orcid");
                if (orcid != null) author.setOrcid(orcid);
                String email = optionalString(entry, "email");
                if (email != null) author.setEmail(email);
                String website = optionalString(entry, "website");
                if (website != null) author.setWebsite(website);
                if (entry.has("firstAuthor")) author.setFirstAuthor(entry.get("firstAuthor").asBoolean());
                if (entry.has("correspondingAuthor")) {
                    author.setCorrespondingAuthor(entry.get("correspondingAuthor").asBoolean());
                }
                author.setAffiliations(parseAffiliations(entry.get("affiliations"), listName));
                list.add(author);
            }
            return list;
        }

        private static JIPipeOrganizationMetadata.List parseAffiliations(JsonNode array, String listName) {
            JIPipeOrganizationMetadata.List list = new JIPipeOrganizationMetadata.List();
            if (array == null || !array.isArray()) {
                return list;
            }
            for (int i = 0; i < array.size(); i++) {
                JsonNode entry = array.get(i);
                String name = optionalString(entry, "name");
                if (name == null || name.trim().isEmpty()) {
                    throw new IllegalArgumentException("Invalid " + listName + " affiliation at index " + i
                            + ": name is required");
                }
                JIPipeOrganizationMetadata organization = new JIPipeOrganizationMetadata();
                organization.setName(name);
                organization.setRor(optionalString(entry, "ror"));
                organization.setWebsite(optionalString(entry, "website"));
                list.add(organization);
            }
            return list;
        }

        /**
         * Reads an optional string field of a JSON object; non-textual values are
         * treated as absent.
         */
        private static String optionalString(JsonNode entry, String field) {
            JsonNode value = entry.get(field);
            return value != null && value.isTextual() ? value.asText() : null;
        }
    }

    public static class AddCompartment implements AutomationOperation {
        @Override
        public String getId() {
            return "add_compartment";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Create a new compartment";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["name"],
                      "properties": {
                        "name": {"type": "string", "description": "Name of the new compartment"}
                      },
                      "additionalProperties": false
                    }""");
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
            String id = JIPipeAutomationAPI.addCompartment(ctx, params.get("name").asText());
            // Result payload key: the kebab-case convention applies to input parameters only
            return new ObjectMapper().createObjectNode().put("compartmentId", id);
        }
    }

    public static class RenameCompartment implements AutomationOperation {
        @Override
        public String getId() {
            return "rename_compartment";
        }

        @Override
        public boolean requiresEdt() {
            return true;
        }

        @Override
        public String getDescription() {
            return "Rename a compartment";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["compartment-id", "name"],
                      "properties": {
                        "compartment-id": {"type": "string", "description": "Compartment to rename (UUID or alias)"},
                        "name": {"type": "string", "description": "New compartment name"}
                      },
                      "additionalProperties": false
                    }""");
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
            var project = ctx.getProject();
            if (project == null) throw new IllegalStateException("No project available");
            String compartment = params.get("compartment-id").asText();
            String name = params.get("name").asText();
            var comp = project.findCompartment(compartment);
            if (comp == null) throw new IllegalArgumentException("Compartment not found: " + compartment);
            comp.setCustomName(name);
            ctx.getEventBus().publish(new CompartmentChangedEvent(
                    ctx.getProjectId(), comp.getProjectCompartmentUUID().toString(), name, "renamed"));
            return new ObjectMapper().createObjectNode().put("success", true);
        }
    }
}
