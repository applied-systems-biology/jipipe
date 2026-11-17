package org.hkijena.jipipe.api.automation.operations;

import com.fasterxml.jackson.databind.JsonNode;
import org.hkijena.jipipe.api.automation.AutomationOperationCategory;
import org.hkijena.jipipe.api.automation.AutomationSchemaUtils;
import org.hkijena.jipipe.api.automation.JIPipeAutomationAPI;
import org.hkijena.jipipe.api.automation.AutomationContext;
import org.hkijena.jipipe.api.automation.AutomationOperation;
import org.hkijena.jipipe.api.project.JIPipeProjectUserPaths;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameter;
import org.hkijena.jipipe.plugins.parameters.library.pairs.StringAndStringPairParameterList;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateApplicationSettings;
import org.hkijena.jipipe.plugins.publish.rocrate.ROCrateDockerSettings;

import java.util.HashMap;
import java.util.Map;

public class ROCrateOperations {

    public static class CreateROCrate implements AutomationOperation {
        @Override
        public String getId() {
            return "create_ro_crate";
        }

        @Override
        public String getDescription() {
            return "Create a Workflow RO-Crate from the currently selected project";
        }

        @Override
        public JsonNode getInputSchema() {
            return AutomationSchemaUtils.parseJson("""
                    {
                      "type": "object",
                      "required": ["output-path"],
                      "properties": {
                        "output-path": {"type": "string"},
                        "docker-image": {"type": "string"},
                        "docker-tag": {"type": "string"},
                        "docker-env-vars": {"type": "array"},
                        "user-path-overrides": {"type": "object"}
                      },
                      "additionalProperties": false
                    }""");
        }

        @Override
        public AutomationOperationCategory getCategory() {
            return AutomationOperationCategory.Execute;
        }

        @Override
        public boolean isReadOnly() {
            return false;
        }

        @Override
        public JsonNode execute(AutomationContext ctx, JsonNode params) {
            String outputDir = params.get("output-path").asText();

            ROCrateDockerSettings dockerSettings;
            if (params.has("docker-image") || params.has("docker-tag") || params.has("docker-env-vars")) {
                dockerSettings = ROCrateApplicationSettings.getInstance().toDockerSettings();
                if (params.has("docker-image")) {
                    dockerSettings.setDockerImage(params.get("docker-image").asText());
                }
                if (params.has("docker-tag")) {
                    dockerSettings.setDockerTag(params.get("docker-tag").asText());
                }
                if (params.has("docker-env-vars")) {
                    StringAndStringPairParameterList envVars = new StringAndStringPairParameterList();
                    for (JsonNode entry : params.get("docker-env-vars")) {
                        envVars.add(new StringAndStringPairParameter(
                                entry.get("key").asText(),
                                entry.get("value").asText()));
                    }
                    dockerSettings.setDockerEnvVars(envVars);
                }
            } else {
                dockerSettings = ROCrateApplicationSettings.getInstance().toDockerSettings();
            }

            Map<String, JIPipeProjectUserPaths.Role> pathOverrides = null;
            if (params.has("user-path-overrides")) {
                pathOverrides = new HashMap<>();
                for (Map.Entry<String, JsonNode> entry : com.google.common.collect.ImmutableList.copyOf(
                        params.get("user-path-overrides").fields())) {
                    String roleStr = entry.getValue().asText();
                    try {
                        JIPipeProjectUserPaths.Role role = JIPipeProjectUserPaths.Role.valueOf(roleStr);
                        pathOverrides.put(entry.getKey(), role);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid user path role: " + roleStr +
                                ". Valid values: Unspecified, Input, Output, Ignored");
                    }
                }
            }

            return JIPipeAutomationAPI.createROCrate(ctx, outputDir, dockerSettings, pathOverrides);
        }
    }
}
