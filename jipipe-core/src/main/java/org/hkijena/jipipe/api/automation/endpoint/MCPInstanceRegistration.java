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
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.api.servers.SharedInstanceRegistration;

import java.util.ArrayList;
import java.util.List;

/**
 * Registration payload a JIPipe instance sends to the MCP broker: endpoint coordinates,
 * secret, app version, and the projects exposed by the instance.
 */
public class MCPInstanceRegistration extends SharedInstanceRegistration {
    private int endpointPort;
    private String endpointSecret;
    private String appVersion;
    private List<String> toolIds = new ArrayList<>();
    private List<MCPProjectEntry> projects = new ArrayList<>();

    public MCPInstanceRegistration() {
    }

    public MCPInstanceRegistration(MCPInstanceRegistration other) {
        super(other);
        this.endpointPort = other.endpointPort;
        this.endpointSecret = other.endpointSecret;
        this.appVersion = other.appVersion;
        this.toolIds = other.toolIds == null ? new ArrayList<>() : new ArrayList<>(other.toolIds);
        this.projects = other.projects == null ? new ArrayList<>() : new ArrayList<>(other.projects);
    }

    @JsonGetter("endpoint-port")
    public int getEndpointPort() {
        return endpointPort;
    }

    @JsonSetter("endpoint-port")
    public void setEndpointPort(int endpointPort) {
        this.endpointPort = endpointPort;
    }

    @JsonGetter("endpoint-secret")
    public String getEndpointSecret() {
        return endpointSecret;
    }

    @JsonSetter("endpoint-secret")
    public void setEndpointSecret(String endpointSecret) {
        this.endpointSecret = endpointSecret;
    }

    @JsonGetter("app-version")
    public String getAppVersion() {
        return appVersion;
    }

    @JsonSetter("app-version")
    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    /**
     * Automation operation ids the instance exposes as MCP tools (set at registration time).
     */
    @JsonGetter("tool-ids")
    public List<String> getToolIds() {
        return toolIds;
    }

    @JsonSetter("tool-ids")
    public void setToolIds(List<String> toolIds) {
        this.toolIds = toolIds == null ? new ArrayList<>() : new ArrayList<>(toolIds);
    }

    @JsonGetter("projects")
    public List<MCPProjectEntry> getProjects() {
        return projects;
    }

    @JsonSetter("projects")
    public void setProjects(List<MCPProjectEntry> projects) {
        this.projects = projects == null ? new ArrayList<>() : new ArrayList<>(projects);
    }
}
