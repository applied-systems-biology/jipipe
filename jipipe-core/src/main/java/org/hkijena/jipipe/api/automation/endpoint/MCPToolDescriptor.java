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

package org.hkijena.jipipe.api.automation.endpoint;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.automation.AutomationToolScope;

/**
 * Registration-carried description of one automation operation exposed as an MCP tool.
 * <p>
 * Serialized fields: {@code id}, {@code description}, {@code inputSchema} (the JSON schema
 * node embedded verbatim), {@code read-only}, and {@code tool-scope} (always emitted; parsed
 * as {@link AutomationToolScope#PROJECT_BOUND} when absent). Instances build the descriptors
 * from their {@link org.hkijena.jipipe.api.automation.AutomationOperationRegistry} at
 * registration time, so the broker can serve full tool metadata without knowing the
 * operation definitions locally.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class MCPToolDescriptor {
    private String id;
    private String description;
    private JsonNode inputSchema;
    private boolean readOnly;
    private AutomationToolScope toolScope = AutomationToolScope.PROJECT_BOUND;

    public MCPToolDescriptor() {
    }

    /**
     * Builds a descriptor from an operation (id, agent documentation, input schema,
     * read-only flag, tool scope).
     */
    public static MCPToolDescriptor of(AutomationOperation operation) {
        MCPToolDescriptor descriptor = new MCPToolDescriptor();
        descriptor.setId(operation.getId());
        descriptor.setDescription(operation.getAgentDocumentation());
        descriptor.setInputSchema(operation.getInputSchema());
        descriptor.setReadOnly(operation.isReadOnly());
        descriptor.setToolScope(operation.getToolScope());
        return descriptor;
    }

    @JsonGetter("id")
    public String getId() {
        return id;
    }

    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;
    }

    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @JsonGetter("inputSchema")
    public JsonNode getInputSchema() {
        return inputSchema;
    }

    @JsonSetter("inputSchema")
    public void setInputSchema(JsonNode inputSchema) {
        this.inputSchema = inputSchema;
    }

    @JsonGetter("read-only")
    public boolean isReadOnly() {
        return readOnly;
    }

    @JsonSetter("read-only")
    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @JsonGetter("tool-scope")
    public AutomationToolScope getToolScope() {
        return toolScope;
    }

    @JsonSetter("tool-scope")
    public void setToolScope(AutomationToolScope toolScope) {
        this.toolScope = toolScope;
    }
}
