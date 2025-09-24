package org.hkijena.jipipe.api.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeLegacyDataImportOperation;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.service.components.JIPipeEnvironmentsServiceComponent;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultCacheDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultEnvironmentsApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultResultImporterApplicationSettings;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Base class for all initialization tasks related to JIPipe
 */
public abstract class JIPipeServiceInitializer extends JIPipeServiceComponent {

    public JIPipeServiceInitializer(JIPipeService service) {
        super(service);
    }

    public abstract void runInitialization();

    /**
     * Copies templates from the old storage inside jipipe.properties.json into the legacy template directory
     *
     * @param progressInfo the progress info
     */
    protected void copyTemplatesFromPropertiesToLegacyProfile(JIPipeProgressInfo progressInfo) {
        Path legacySettingsPath = PathUtils.getImageJDir().resolve("jipipe.properties.json");

        // Convert node templates
        if (Files.isRegularFile(legacySettingsPath)) {
            getProgressInfo().log("Reading legacy settings " + legacySettingsPath);
            try {
                JsonNode jsonNode = JsonUtils.readFromFile(legacySettingsPath, JsonNode.class);

                JsonNode nodeTemplatesListNode = jsonNode.path("node-templates/node-templates");
                if (!nodeTemplatesListNode.isMissingNode()) {
                    getProgressInfo().log("Found legacy node templates!");
                    Path targetDir = getService().getNodeTemplates().getLegacyStoragePath();
                    Files.createDirectories(targetDir);
                    for (JsonNode node : ImmutableList.copyOf(nodeTemplatesListNode.elements())) {
                        Path targetFile = targetDir.resolve(UUID.randomUUID() + ".json");
                        getProgressInfo().log("Writing legacy node template: " + targetFile);
                        JsonUtils.saveToFile(node, targetFile);
                    }
                }
            } catch (Throwable e) {
                getProgressInfo().log("Unable to copy settings!");
                getProgressInfo().log(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    protected boolean applyProfileUpgrades(JIPipeProgressInfo progressInfo) {
        boolean settingsExist = Files.isRegularFile(PathUtils.getJIPipeUserDir(false).resolve("settings.json"));
        boolean childDirsExist = !PathUtils.listSubDirectories(PathUtils.getJIPipeUserDir(false)).isEmpty();

        if (!settingsExist && !childDirsExist) {
            // Check for a profile from an older version
            Path profileBasePath = PathUtils.getJIPipeUserDirBase();
            PathUtils.createDirectories(profileBasePath);

            // Collect all profile directories
            String currentVersion = VersionUtils.getJIPipeVersion();
            Map<String, Path> allProfileDirectories = new HashMap<>();
            for (Path path : PathUtils.listSubDirectories(profileBasePath)) {
                if (!Objects.equals(currentVersion, path.getFileName().toString()) && StringUtils.compareVersions(currentVersion, path.getFileName().toString()) > 0) {
                    allProfileDirectories.put(path.getFileName().toString(), path);
                }
            }

            // Find the newest version
            List<String> sortedAllVersions = allProfileDirectories.keySet().stream().sorted(StringUtils::compareVersions).collect(Collectors.toList());
            if (!sortedAllVersions.isEmpty()) {
                String previousVersion = sortedAllVersions.get(sortedAllVersions.size() - 1);
                Path oldProfileDirectory = profileBasePath.resolve(previousVersion);
                Path newProfileDirectory = profileBasePath.resolve(currentVersion);
                Path oldProfileBackupsDirectory = oldProfileDirectory.resolve("backups");
                getProgressInfo().log("Upgrading from profile " + oldProfileDirectory);
                PathUtils.copyDirectory(oldProfileDirectory, newProfileDirectory, dir -> !dir.equals(oldProfileBackupsDirectory) && !dir.startsWith(oldProfileBackupsDirectory), getProgressInfo().resolve("Copy profile"));
                getProgressInfo().log("Profile upgrade successful. Continuing.");
                return true;
            }

            // Check if we have a legacy profile that can be upgraded
            Path legacyProfileDirectory = PathUtils.getLegacyJIPipeUserDir();
            if (Files.isDirectory(legacyProfileDirectory)) {

                // Delete old 3rd party software
                getProgressInfo().log("Removing EasyInstaller directories in " + legacyProfileDirectory);
                for (Path subDirectory : PathUtils.listSubDirectories(legacyProfileDirectory)) {
                    if (subDirectory.getFileName().toString().startsWith("easyinstall-")) {
                        PathUtils.deleteDirectoryRecursively(subDirectory, getProgressInfo().resolve("Cleanup old 3rd party software"));
                    }
                }

                // Copy the profile
                getProgressInfo().log("Upgrading from profile " + legacyProfileDirectory);
                Path oldProfileBackupsDirectory = legacyProfileDirectory.resolve("backups");
                Path newProfileDirectory = profileBasePath.resolve(currentVersion);
                PathUtils.copyDirectory(legacyProfileDirectory, newProfileDirectory, dir -> !dir.equals(oldProfileBackupsDirectory) && !dir.startsWith(oldProfileBackupsDirectory), getProgressInfo().resolve("Copy profile"));
                getProgressInfo().log("Profile upgrade successful. Continuing.");
            }
            return true;
        } else {
            getProgressInfo().log(PathUtils.getJIPipeUserDir() + " already exists. No profile upgrades are needed.");
            return false;
        }
    }

    protected void createDefaultEnvironmentSettings() {
        JIPipeDefaultEnvironmentsApplicationSettings settings = getService().getApplicationSettings().getById(JIPipeDefaultEnvironmentsApplicationSettings.ID, JIPipeDefaultEnvironmentsApplicationSettings.class);
        for (Map.Entry<String, JIPipeEnvironmentsServiceComponent.EnvironmentInfo> entry : getService().getEnvironments().getInfosById().entrySet()) {
            JIPipeEnvironmentsServiceComponent.EnvironmentInfo info = entry.getValue();
            if(info.getArchetype() == JIPipeEnvironmentArchetype.Managed) {
                settings.addParameter(entry.getKey(), info.getOptionalEnvironmentClass(), info.getName(), info.getDescription() + ". " +
                        "Allows to override which environment is used if both node and project overrides are disabled. " +
                        "If disabled, JIPipe will automatically select the newest available and compatible environment on project creation.");
            }
        }
    }

    /**
     * Creates settings for each known data type, so users can change how they will be imported
     */
    protected void createDefaultImporterSettings() {
        JIPipeDefaultResultImporterApplicationSettings settings = getService().getApplicationSettings().getById(JIPipeDefaultResultImporterApplicationSettings.ID, JIPipeDefaultResultImporterApplicationSettings.class);
        for (String id : getService().getDataTypes().getRegisteredDataTypes().keySet()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(id);
            JIPipeMutableParameterAccess access = settings.addParameter(id, DynamicDataImportOperationIdEnumParameter.class);
            access.setName(info.getName());
            access.setDescription("Defines which importer is used by default when importing the selected data type.");
        }
    }

    /**
     * Creates settings for each known data type, so users can change how they will be imported
     */
    protected void createDefaultCacheDisplaySettings() {
        JIPipeDefaultCacheDisplayApplicationSettings settings = getService().getApplicationSettings().getById(JIPipeDefaultCacheDisplayApplicationSettings.ID, JIPipeDefaultCacheDisplayApplicationSettings.class);
        for (String id : getService().getDataTypes().getRegisteredDataTypes().keySet()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(id);
            JIPipeMutableParameterAccess access = settings.addParameter(id, DynamicDataDisplayOperationIdEnumParameter.class);
            access.setName(info.getName());
            access.setDescription("Defines which cache display method is used by default for the type.");
        }
    }

    protected void updateDefaultImporterSettings() {
        JIPipeDefaultResultImporterApplicationSettings settings = getService().getApplicationSettings().getById(JIPipeDefaultResultImporterApplicationSettings.ID, JIPipeDefaultResultImporterApplicationSettings.class);
        for (String id : getService().getDataTypes().getRegisteredDataTypes().keySet()) {
            List<JIPipeLegacyDataImportOperation> operations = getService().getDataTypes().getSortedImportOperationsFor(id);
            JIPipeMutableParameterAccess access = (JIPipeMutableParameterAccess) settings.get(id);

            Object currentParameterValue = access.get(Object.class);
            DynamicDataImportOperationIdEnumParameter parameter;
            if (currentParameterValue instanceof DynamicDataImportOperationIdEnumParameter) {
                parameter = (DynamicDataImportOperationIdEnumParameter) currentParameterValue;
            } else {
                parameter = new DynamicDataImportOperationIdEnumParameter();
                parameter.setValue("jipipe:show");
            }

            for (JIPipeLegacyDataImportOperation operation : operations) {
                parameter.getAllowedValues().add(operation.getId());
            }
            if (parameter.getValue() == null || !parameter.getAllowedValues().contains(parameter.getValue())) {
                parameter.setValue("jipipe:show");
            }
            parameter.setDataTypeId(id);
            access.set(parameter);
        }
    }

    protected void updateDefaultCacheDisplaySettings() {
        JIPipeDefaultCacheDisplayApplicationSettings settings = getService().getApplicationSettings().getById(JIPipeDefaultCacheDisplayApplicationSettings.ID, JIPipeDefaultCacheDisplayApplicationSettings.class);
        for (String id : getService().getDataTypes().getRegisteredDataTypes().keySet()) {
            List<JIPipeDesktopDataDisplayOperation> operations = getService().getDataTypes().getSortedDisplayOperationsFor(id);
            JIPipeMutableParameterAccess access = (JIPipeMutableParameterAccess) settings.get(id);

            Object currentParameterValue = access.get(Object.class);
            DynamicDataDisplayOperationIdEnumParameter parameter;
            if (currentParameterValue instanceof DynamicDataDisplayOperationIdEnumParameter) {
                parameter = (DynamicDataDisplayOperationIdEnumParameter) currentParameterValue;
            } else {
                parameter = new DynamicDataDisplayOperationIdEnumParameter();
                parameter.setValue("jipipe:show");
            }

            for (JIPipeDesktopDataDisplayOperation operation : operations) {
                parameter.getAllowedValues().add(operation.getId());
            }
            if (parameter.getValue() == null || !parameter.getAllowedValues().contains(parameter.getValue())) {
                parameter.setValue("jipipe:show");
            }
            parameter.setDataTypeId(id);
            access.set(parameter);
        }
    }

    protected void registerNodeExamplesFromFileSystem() {
        Path examplesDir = PathUtils.getJIPipeUserDir().resolve("examples");
        try {
            if (!Files.isDirectory(examplesDir))
                Files.createDirectories(examplesDir);
            Files.walk(examplesDir).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    if (PathUtils.EXTENSION_FILTER_JSON.accept(path.toFile())) {
                        try {
                            getProgressInfo().log("[Node examples] Importing node template list from " + path);
                            for (JIPipeNodeTemplate template : JsonUtils.getObjectMapper().readValue(path.toFile(), JIPipeNodeTemplate.List.class)) {
                                getService().getNodes().registerExample(template);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            getProgressInfo().log("Error while loading node examples from " + path + ": " + e);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            getProgressInfo().log("Error while loading node examples from " + examplesDir + ": " + e);
        }
    }

    protected void registerProjectTemplatesFromFileSystem() {
        Path examplesDir = PathUtils.getJIPipeUserDir().resolve("templates");
        try {
            if (!Files.isDirectory(examplesDir))
                Files.createDirectories(examplesDir);
            Files.walk(examplesDir).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    if (PathUtils.EXTENSION_FILTER_JIP.accept(path.toFile()) || PathUtils.EXTENSION_FILTER_ZIP.accept(path.toFile())) {
                        try {
                            getProgressInfo().log("[Project templates] Importing template from " + path);
                            getService().getProjectTemplates().register(path);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            getProgressInfo().log("Error while loading project template from " + path + ": " + e);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            getProgressInfo().log("Error while loading project templates from " + examplesDir + ": " + e);
        }
    }

    public boolean isVerbose() {
        return getService().getInitializationSettings().isVerbose();
    }

    public abstract void runPostprocessing();
}
