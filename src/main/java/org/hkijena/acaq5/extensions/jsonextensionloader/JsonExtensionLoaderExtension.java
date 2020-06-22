package org.hkijena.acaq5.extensions.jsonextensionloader;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.eventbus.Subscribe;
import ij.Menus;
import org.hkijena.acaq5.ACAQDefaultRegistry;
import org.hkijena.acaq5.ACAQJavaExtension;
import org.hkijena.acaq5.ACAQJsonExtension;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.events.ExtensionRegisteredEvent;
import org.hkijena.acaq5.extensions.ACAQPrepackagedDefaultJavaExtension;
import org.hkijena.acaq5.utils.JsonUtils;
import org.hkijena.acaq5.utils.ResourceUtils;
import org.scijava.plugin.Plugin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extension that loads {@link ACAQJsonExtension}
 */
@Plugin(type = ACAQJavaExtension.class)
public class JsonExtensionLoaderExtension extends ACAQPrepackagedDefaultJavaExtension {

    private Set<JsonExtensionRegistrationTask> registrationTasks = new HashSet<>();

    @Override
    public String getName() {
        return "JSON Extension loader";
    }

    @Override
    public String getDescription() {
        return "Loads JSON extensions";
    }

    @Override
    public void register() {
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
    public void setRegistry(ACAQDefaultRegistry registry) {
        super.setRegistry(registry);
        registry.getEventBus().register(this);
    }

    @Override
    public String getDependencyId() {
        return "acaq:json-extension-loader";
    }

    @Override
    public String getDependencyVersion() {
        return "1.0.0";
    }

    /**
     * Tries to register more extensions waiting for dependencies
     */
    public void updateRegistrationTasks() {
        if (registrationTasks.isEmpty())
            return;
        System.out.println("[ACAQ5 Json Extension] There are still " + registrationTasks.size() + " unregistered extensions left");
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
            ACAQJsonExtension extension = JsonUtils.getObjectMapper().readerFor(ACAQJsonExtension.class).readValue(task.getJsonNode());
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
        JsonNode projectTypeNode = jsonNode.path("acaq:project-type");
        if (projectTypeNode.isMissingNode() || !projectTypeNode.isTextual() || !"json-extension".equals(projectTypeNode.textValue())) {
            return;
        }

        JsonExtensionRegistrationTask task = new JsonExtensionRegistrationTask(getRegistry(), filePath, jsonNode);
        registrationTasks.add(task);
        updateRegistrationTasks();
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        super.reportValidity(report);
        for (JsonExtensionRegistrationTask task : registrationTasks) {
            report.forCategory("Unregistered JSON extensions").report(task);
        }
    }

    /**
     * Triggered when an extension is registered.
     *
     * @param event Generated event
     */
    @Subscribe
    public void onExtensionRegistered(ExtensionRegisteredEvent event) {
        updateRegistrationTasks();
    }

    public static Path getPluginDirectory() {
        String path = Menus.getPlugInsPath();
        if (path == null)
            return Paths.get("plugins");
        return Paths.get(path);
    }
}
