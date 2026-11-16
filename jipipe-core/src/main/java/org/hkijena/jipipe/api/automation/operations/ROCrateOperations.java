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
                      "required": ["outputPath"],
                      "properties": {
                        "outputPath": {"type": "string"},
                        "dockerImage": {"type": "string"},
                        "dockerTag": {"type": "string"},
                        "dockerEnvVars": {"type": "array"},
                        "userPathOverrides": {"type": "object"}
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
            String outputPath = params.get("outputPath").asText();

            ROCrateDockerSettings dockerSettings;
            if (params.has("dockerImage") || params.has("dockerTag") || params.has("dockerEnvVars")) {
                dockerSettings = ROCrateApplicationSettings.getInstance().toDockerSettings();
                if (params.has("dockerImage")) {
                    dockerSettings.setDockerImage(params.get("dockerImage").asText());
                }
                if (params.has("dockerTag")) {
                    dockerSettings.setDockerTag(params.get("dockerTag").asText());
                }
                if (params.has("dockerEnvVars")) {
                    StringAndStringPairParameterList envVars = new StringAndStringPairParameterList();
                    for (JsonNode entry : params.get("dockerEnvVars")) {
                        envVars.add(new StringAndStringPairParameter(
                                entry.get("key").asText(),
                                entry.get("value").asText()));
                    }
                    dockerSettings.setDockerEnvVars(envVars);
                }
            } else {
                dockerSettings = ROCrateApplicationSettings.getInstance().toDockerSettings();
            }

            Map<String, JIPipeProjectUserPaths.Role> userPathOverrides = null;
            if (params.has("userPathOverrides")) {
                userPathOverrides = new HashMap<>();
                for (Map.Entry<String, JsonNode> entry : com.google.common.collect.ImmutableList.copyOf(
                        params.get("userPathOverrides").fields())) {
                    String roleStr = entry.getValue().asText();
                    try {
                        JIPipeProjectUserPaths.Role role = JIPipeProjectUserPaths.Role.valueOf(roleStr);
                        userPathOverrides.put(entry.getKey(), role);
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException("Invalid user path role: " + roleStr +
                                ". Valid values: Unspecified, Input, Output, Ignored");
                    }
                }
            }

            return JIPipeAutomationAPI.createROCrate(ctx, outputPath, dockerSettings, userPathOverrides);
        }
    }
}
