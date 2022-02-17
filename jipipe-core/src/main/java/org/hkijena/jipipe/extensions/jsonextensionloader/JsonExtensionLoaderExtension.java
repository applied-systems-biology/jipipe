/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.extensions.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.eventbus.Subscribe;
import ij.Menus;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeJavaExtension;
import org.hkijena.jipipe.JIPipeJsonExtension;
import org.hkijena.jipipe.api.JIPipeIssueReport;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.extensions.JIPipePrepackagedDefaultJavaExtension;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
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
 * Extension that loads {@link JIPipeJsonExtension}
 */
@Plugin(type = JIPipeJavaExtension.class)
public class JsonExtensionLoaderExtension extends JIPipePrepackagedDefaultJavaExtension {

    private Set<JsonExtensionRegistrationTask> registrationTasks = new HashSet<>();

    public static Path getPluginDirectory() {
        String path = Menus.getPlugInsPath();
        if (path == null)
            return Paths.get("plugins");
        return Paths.get(path);
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
        registry.getEventBus().register(this);
    }

    @Override
    public String getDependencyId() {
        return "jipipe:json-extension-loader";
    }

    @Override
    public String getDependencyVersion() {
        return "1.64.0";
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
            JIPipeJsonExtension extension = JsonUtils.getObjectMapper().readerFor(JIPipeJsonExtension.class).readValue(task.getJsonNode());
            getRegistry().register(extension);
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
    public void reportValidity(JIPipeIssueReport report) {
        super.reportValidity(report);
        for (JsonExtensionRegistrationTask task : registrationTasks) {
            report.resolve("Unregistered JSON extensions").report(task);
        }
    }

    /**
     * Triggered when an extension is registered.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(JIPipe.ExtensionRegisteredEvent event) {
        updateRegistrationTasks();
    }
}
