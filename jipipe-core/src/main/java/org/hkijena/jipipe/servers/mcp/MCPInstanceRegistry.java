package org.hkijena.jipipe.servers.mcp;

import org.hkijena.jipipe.api.automation.endpoint.MCPInstanceRegistration;
import org.hkijena.jipipe.api.automation.endpoint.MCPToolDescriptor;
import org.hkijena.jipipe.api.servers.SharedInstanceRegistry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Registry of JIPipe instances registered at an MCP broker.
 * <p>
 * {@link #findByProjectId(String)} splits project ids of the form
 * {@code <instanceId>:<projectKey>} on the FIRST colon only: instance ids are UUIDs without
 * colons, while project keys may contain colons.
 */
public class MCPInstanceRegistry extends SharedInstanceRegistry<MCPInstanceRegistration> {

    /**
     * Controller ruling: {@code list_operations} is a registry meta-operation, not a tool.
     * It is registered as an automation operation, but the MCP {@code tools/list}
     * aggregation must exclude it (a registered schema does not make it a tool).
     */
    public static final Set<String> EXCLUDED_TOOL_IDS = Set.of("list_operations");

    /**
     * Union of the per-instance tool ids, minus {@link #EXCLUDED_TOOL_IDS}.
     */
    public Set<String> aggregateToolIds() {
        Set<String> aggregated = new TreeSet<>();
        for (MCPInstanceRegistration registration : getAll()) {
            if (registration.getToolIds() != null) {
                aggregated.addAll(registration.getToolIds());
            }
        }
        aggregated.removeAll(EXCLUDED_TOOL_IDS);
        return aggregated;
    }

    /**
     * Union of the registration-carried tool descriptors, keyed by descriptor id (first
     * registration wins), minus {@link #EXCLUDED_TOOL_IDS}. Empty when no registration
     * carries tools.
     */
    public List<MCPToolDescriptor> aggregateToolDescriptors() {
        Map<String, MCPToolDescriptor> aggregated = new TreeMap<>();
        for (MCPInstanceRegistration registration : getAll()) {
            if (registration.getTools() == null) {
                continue;
            }
            for (MCPToolDescriptor descriptor : registration.getTools()) {
                if (descriptor == null || descriptor.getId() == null) {
                    continue;
                }
                aggregated.putIfAbsent(descriptor.getId(), descriptor);
            }
        }
        for (String excluded : EXCLUDED_TOOL_IDS) {
            aggregated.remove(excluded);
        }
        return new ArrayList<>(aggregated.values());
    }

    /**
     * Resolves a project id of the form {@code <instanceId>:<projectKey>}.
     *
     * @return the registration, or null when the id has no colon or the instance is unknown
     * @throws NoSuchElementException when the instance is known but does not expose the project key
     */
    public MCPInstanceRegistration findByProjectId(String projectId) {
        int separator = projectId.indexOf(':');
        if (separator <= 0) {
            return null;
        }
        MCPInstanceRegistration registration = get(projectId.substring(0, separator));
        if (registration == null) {
            return null;
        }
        String projectKey = projectId.substring(separator + 1);
        boolean known = registration.getProjects().stream()
                .anyMatch(project -> projectKey.equals(project.getKey()));
        if (!known) {
            throw new NoSuchElementException("Unknown project key '" + projectKey + "' for instance "
                    + registration.getInstanceId() + ". Available projects: " + projectKeysOf(registration));
        }
        return registration;
    }

    private static String projectKeysOf(MCPInstanceRegistration registration) {
        StringBuilder builder = new StringBuilder();
        for (var project : registration.getProjects()) {
            if (!builder.isEmpty()) {
                builder.append(", ");
            }
            builder.append(project.getKey());
        }
        return builder.toString();
    }
}
