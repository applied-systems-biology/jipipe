package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaSpec;
import org.hkijena.jipipe.api.automation.events.CompartmentChangedEvent;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.metadata.JIPipeOrganizationMetadata;
import org.hkijena.jipipe.utils.json.schema.JsonArraySchema;
import org.hkijena.jipipe.utils.json.schema.JsonBooleanSchema;
import org.hkijena.jipipe.utils.json.schema.JsonObjectSchema;
import org.hkijena.jipipe.utils.json.schema.JsonStringSchema;

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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("compartment-id", "Compartment to add the node to (UUID or alias); omit to add the node without a compartment")
                .string("node-type-id", "The id of the node type to add, as returned by list_algorithms").required()
                .integer("x", "Initial node position on the canvas (default 0)")
                .integer("y", "Initial node position on the canvas (default 0)")
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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("node-id", "The node id as used by query_graph (alias id or UUID)").required()
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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("node", "The node id as used by query_graph (alias id or UUID)").required()
                .integer("x", "Canvas column in grid cells (1 cell = 25 px)").required()
                .integer("y", "Canvas row in grid cells (1 cell = 25 px)").required()
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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("compartment", "Compartment to create the note in (UUID, alias, or name); required, the note is never created without a compartment").required()
                .enumeration("kind", "The note kind: a text box has title, content, size, and position; a comment carries its title as the node name", "text-box", "comment").defaultsTo("text-box")
                .string("title", "The note title (default: Text box for a text box, Comment for a comment)")
                .string("content", "Text-box content; only valid for kind text-box")
                .integer("x", "Canvas column in grid cells (1 cell = 25 px)").defaultsTo(0)
                .integer("y", "Canvas row in grid cells (1 cell = 25 px)").defaultsTo(0)
                .integer("width", "Text-box width in grid cells; only valid for kind text-box").defaultsTo(4)
                .integer("height", "Text-box height in grid cells; only valid for kind text-box").defaultsTo(3)
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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("source", "The source node id as used by query_graph (alias id or UUID)").required()
                .string("source-slot", "Name of the source node's output slot").required()
                .string("target", "The target node id as used by query_graph (alias id or UUID)").required()
                .string("target-slot", "Name of the target node's input slot").required()
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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("source", "The source node id as used by query_graph (alias id or UUID)").required()
                .string("source-slot", "Name of the source node's output slot").required()
                .string("target", "The target node id as used by query_graph (alias id or UUID)").required()
                .string("target-slot", "Name of the target node's input slot").required()
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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("node-id", "The node id as used by query_graph (alias id or UUID)").required()
                .string("key", "Parameter key as reported by query_node or get_node_docs").required()
                .string("value", "New value as text; it is converted to the parameter type (numbers, booleans, enum names, paths)").required()
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
            ObjectNode parameter = JIPipeAutomationAPI.setParameter(ctx,
                    params.get("node-id").asText(), params.get("key").asText(), params.get("value").asText());
            ObjectNode result = new ObjectMapper().createObjectNode().put("success", true);
            result.set("parameter", parameter);
            return result;
        }
    }

    public static class SetProjectMetadata implements AutomationOperation {

        /**
         * Schema of one affiliations entry inside an author entry: name is required
         * (non-blank), ror and website are optional.
         */
        private static final JsonObjectSchema AFFILIATION_ENTRY = JsonObjectSchema.object()
                .description("One affiliation: name is required (non-blank), ror and website are optional")
                .property("name", JsonStringSchema.string().description("Organization name (required, non-blank)"), true)
                .property("ror", JsonStringSchema.string().description("ROR ID or URL"))
                .property("website", JsonStringSchema.string().description("Organization website"))
                .build();

        /**
         * Schema of one authors/acknowledgements entry. Shared by both array
         * parameters so the two shapes cannot drift apart.
         * <p>
         * Fidelity note: the entry-level firstName/lastName requirement is NOT a schema
         * keyword (the pre-migration wire schema declares no entry-level required list);
         * it is enforced by {@link #parseAuthors} at execute time, which produces the
         * dedicated "is required" error naming the index. The affiliation entry's
         * {@code name} IS schema-required, exactly as before.
         */
        private static final JsonObjectSchema AUTHOR_ENTRY = JsonObjectSchema.object()
                .description("One author entry: firstName and lastName are required (non-blank), all other fields are optional")
                .property("firstName", JsonStringSchema.string().description("Author first name (required, non-blank)"))
                .property("lastName", JsonStringSchema.string().description("Author last name (required, non-blank)"))
                .property("orcid", JsonStringSchema.string().description("ORCID (URL or ID)"))
                .property("email", JsonStringSchema.string().description("E-Mail address"))
                .property("website", JsonStringSchema.string().description("Author website"))
                .property("firstAuthor", JsonBooleanSchema.bool().description("Marks the author as first author"))
                .property("correspondingAuthor", JsonBooleanSchema.bool().description("Marks the author as corresponding author"))
                .property("affiliations", JsonArraySchema.array(AFFILIATION_ENTRY).description("Author affiliations"))
                .build();

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("name", "New project name; the current name is kept when omitted")
                .string("description", "New project description; the current description is kept when omitted")
                .string("license", "New license name like MIT or GPL v3; the current license is kept when omitted; a blank string is stored as-is (clears the field)")
                .string("summary", "New short summary; the current summary is kept when omitted; a blank string is stored as-is (clears the field)")
                .string("website", "New project website; the current website is kept when omitted; a blank string is stored as-is (clears the field)")
                .string("citation", "New citation of the work where the project is published; the current citation is kept when omitted; a blank string is stored as-is (clears the field)")
                .arrayOfObjects("authors", "Replaces the whole author list (no merge): a present array, even an empty one, replaces the current list, only an omitted key keeps it. Each entry needs a non-blank firstName and lastName; one invalid entry refuses the whole call and leaves the project untouched", AUTHOR_ENTRY)
                .arrayOfObjects("acknowledgements", "Replaces the whole acknowledgements list (no merge): a present array, even an empty one, replaces the current list, only an omitted key keeps it. Each entry needs a non-blank firstName and lastName; one invalid entry refuses the whole call and leaves the project untouched", AUTHOR_ENTRY)
                .build();

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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("name", "Name of the new compartment").required()
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

        private static final AutomationSchemaSpec SPEC = AutomationSchemaSpec.object()
                .string("compartment-id", "Compartment to rename (UUID or alias)").required()
                .string("name", "New compartment name").required()
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
