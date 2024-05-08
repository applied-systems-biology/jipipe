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

package org.hkijena.jipipe.plugins.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import ij.Menus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaPlugin;
import org.hkijena.jipipe.JIPipeJsonPlugin;
import org.hkijena.jipipe.JIPipeService;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.CustomValidationReportContext;
import org.hkijena.jipipe.plugins.JIPipePrepackagedDefaultJavaPlugin;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extension that loads {@link JIPipeJsonPlugin}
 */
@Plugin(type = JIPipeJavaPlugin.class)
public class JsonExtensionLoaderPlugin extends JIPipePrepackagedDefaultJavaPlugin implements JIPipeService.ExtensionRegisteredEventListener {

    private final Set<JsonExtensionRegistrationTask> registrationTasks = new HashSet<>();

    public static Path getPluginDirectory() {
        String path = Menus.getPlugInsPath();
        if (path == null)
            return Paths.get("plugins");
        return Paths.get(path);
    }

    @Override
    public StringList getDependencyProvides() {
        return new StringList();
    }

    @Override
    public StringList getDependencyCitations() {
        return new StringList();
    }

    @Override
    public String getName() {
        return "JSON Extension loader";
    }

    @Override
    public HTMLText getDescription() {
        return new HTMLText("Loads JSON extensions");
    }

    @Override
    public void register(JIPipe jiPipe, Context context, JIPipeProgressInfo progressInfo) {
        // Register from resources
        Set<String> algorithmFiles = ResourceUtils.walkInternalResourceFolder("jsonextensions");
        for (String resourceFile : algorithmFiles) {
            if (resourceFile.endsWith(".json"))
                registerJsonExtensionFromResource(resourceFile);
        }

        // Register from plugin directory
        if (Files.exists(getPluginDirectory())) {
            try {
                for (Path path : Files.walk(getPluginDirectory()).collect(Collectors.toSet())) {
                    if (Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                        registerJsonExtensionFromFile(path);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void setRegistry(JIPipe registry) {
        super.setRegistry(registry);
        registry.getExtensionRegisteredEventEmitter().subscribe(this);
    }

    @Override
    public String getDependencyId() {
        return "jipipe:json-extension-loader";
    }

    /**
     * Tries to register more extensions waiting for dependencies
     */
    public void updateRegistrationTasks() {
        if (registrationTasks.isEmpty())
            return;
        JIPipe.getInstance().getLogService().debug("[JIPipe Json Extension] There are still " + registrationTasks.size() + " unregistered extensions left");
        Set<JsonExtensionRegistrationTask> runnable = new HashSet<>();
        for (JsonExtensionRegistrationTask task : registrationTasks) {
            if (task.canRegister()) {
                runnable.add(task);
            }
        }
        if (!runnable.isEmpty()) {
            registrationTasks.removeAll(runnable);
            for (JsonExtensionRegistrationTask task : runnable) {
                runRegistrationTask(task);
            }
        }
    }

    /**
     * Immediately runs a registration task.
     * Dependencies are not checked.
     *
     * @param task the task
     */
    public void runRegistrationTask(JsonExtensionRegistrationTask task) {
        try {
            JIPipeJsonPlugin extension = JsonUtils.getObjectMapper().readerFor(JIPipeJsonPlugin.class).readValue(task.getJsonNode());
            JIPipe.getInstance().getPluginRegistry().registerKnownPlugin(extension);
            if (!JIPipe.isValidExtensionId(extension.getDependencyId())) {
                System.err.println("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension.");
                getRegistry().getProgressInfo().log("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension.");
            }
            if (!JIPipe.getInstance().getPluginRegistry().getStartupPlugins().contains(extension.getDependencyId())) {
                getRegistry().getProgressInfo().log("Skipping registration of JSON extension " + extension.getDependencyId() + " (deactivated in extension manager)");
                return;
            }
            getRegistry().register(extension, getRegistry().getProgressInfo());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Schedules a registration task for the file path
     *
     * @param filePath extension file
     */
    public void registerJsonExtensionFromFile(Path filePath) {
        try {
            scheduleRegisterJsonExtension(filePath, JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(filePath.toFile()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Schedules a registration task for a resource path
     *
     * @param resourcePath the resource
     */
    public void registerJsonExtensionFromResource(String resourcePath) {
        try {
            JsonNode node = JsonUtils.getObjectMapper().readValue(ResourceUtils.class.getResource(resourcePath), JsonNode.class);
            scheduleRegisterJsonExtension(null, node);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Schedules the registration of an extension
     * Invalid JSON data is skipped
     *
     * @param filePath the file path where the JSON was loaded. This is only used for information. Can be null.
     * @param jsonNode JSON data that contains the serialized extension
     */
    public void scheduleRegisterJsonExtension(Path filePath, JsonNode jsonNode) {
        JsonNode projectTypeNode = jsonNode.path("jipipe:project-type");
        if (projectTypeNode.isMissingNode() || !projectTypeNode.isTextual() || !"json-extension".equals(projectTypeNode.textValue())) {
            return;
        }

        JsonExtensionRegistrationTask task = new JsonExtensionRegistrationTask(getRegistry(), filePath, jsonNode);
        registrationTasks.add(task);
        updateRegistrationTasks();
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        super.reportValidity(reportContext, report);
        for (JsonExtensionRegistrationTask task : registrationTasks) {
            report.report(new CustomValidationReportContext(reportContext, "Unregistered JSON extensions"), task);
        }
    }

    @Override
    public boolean isCorePlugin() {
        return true;
    }

    @Override
    public void onJIPipeExtensionRegistered(JIPipeService.ExtensionRegisteredEvent event) {
        updateRegistrationTasks();
    }
}
