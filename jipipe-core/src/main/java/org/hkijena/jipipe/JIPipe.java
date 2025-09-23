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

package org.hkijena.jipipe;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.ImmutableList;
import ij.IJ;
import net.imagej.ImageJ;
import net.imagej.updater.FilesCollection;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceMode;
import org.hkijena.jipipe.api.service.JIPipeServiceState;
import org.hkijena.jipipe.api.service.events.*;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.run.JIPipeRunnableLogEntry;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.JavaExtensionValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDefaultDataViewer;
import org.hkijena.jipipe.desktop.api.registries.JIPipeCustomMenuRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactAccelerationPreference;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeExtensionApplicationSettings;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.log.LogService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;
import org.scijava.service.AbstractService;

import javax.swing.*;
import javax.swing.Timer;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Wrapper/helper class around a static {@link JIPipeService}
 */
public final class JIPipe {

    /**
     * Resource manager for core JIPipe
     */
    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(JIPipe.class, "org/hkijena/jipipe");
    private static JIPipeService instance;

    private JIPipe() {

    }

    /**
     * Imports data of given data type from its output folder.
     * Generally, the output folder should conform to the data type's saveTo() function without 'forceName' enabled
     *
     * @param <T>          the data type
     * @param storage      the storage that contains the serialized data
     * @param klass        the data type
     * @param progressInfo the progress info
     * @return imported data
     */
    public static <T extends JIPipeData> T importData(JIPipeReadDataStorage storage, Class<T> klass, JIPipeProgressInfo progressInfo) {
        try {
            Method method = klass.getDeclaredMethod("importData", JIPipeReadDataStorage.class, JIPipeProgressInfo.class);
            return (T) method.invoke(null, storage, progressInfo);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns true if a JIPipe restart is in progress.
     * Can be utilized by methods to prevent the closing of the Java app
     *
     * @return if JIPipe is restarting
     */
    public static boolean isRestarting() {
        return IS_RESTARTING;
    }

    public static JIPipeParameterTypeRegistry getParameterTypes() {
        return instance.parameterTypeRegistry;
    }

    public static JIPipeExpressionRegistry getTableOperations() {
        return instance.tableOperationRegistry;
    }

    public static JIPipeCustomMenuRegistry getCustomMenus() {
        return instance.customMenuRegistry;
    }

    public static JIPipeImageJAdapterRegistry getImageJAdapters() {
        return instance.imageJDataAdapterRegistry;
    }

    public static JIPipeApplicationSettingsRegistry getSettings() {
        return instance.applicationSettingsRegistry;
    }

    public static JIPipeNodeRegistry getNodes() {
        return instance.nodeRegistry;
    }

    public static JIPipeDatatypeRegistry getDataTypes() {
        return instance.datatypeRegistry;
    }

    public static JIPipeArtifactsRegistry getArtifacts() {
        return instance.artifactsRegistry;
    }

    public static JIPipeNodeTemplateRegistry getNodeTemplates() {
        return instance.nodeTemplateRegistry;
    }

    /**
     * @return Singleton instance
     */
    public static JIPipe getInstance() {
        return instance;
    }

    public static void restartGUI() {

        // Save all settings first
        if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
            getSettings().save();
        }

        try {
            IS_RESTARTING = true;
            // Kill all JIPipe windows
            for (JIPipeDesktopProjectWindow openWindow : JIPipeDesktopProjectWindow.getOpenWindows()) {
                openWindow.dispose();
            }
            // Set the instance to null
            instance = null;
        } finally {
            IS_RESTARTING = false;
        }
        // Restart the GUI
        final ImageJ ij = new ImageJ();
        SwingUtilities.invokeLater(() -> ij.command().run(JIPipeGUICommand.class, true));
    }

    /**
     * Ensures that JIPipe is initialized and available.
     * Creates a new {@link ImageJ} instance to obtain a {@link Context} if JIPipe is not initialized already.
     *
     * @return the current instance
     */
    public static JIPipe ensureInstance() {
        if (getInstance() != null)
            return getInstance();
        final ImageJ ij = new ImageJ();
        Context context = ij.context();
        return ensureInstance(context);
    }

    /**
     * Ensures that JIPipe is initialized and available.
     *
     * @param context the context to initialize JIPipe
     * @return the current instance
     */
    public static JIPipe ensureInstance(Context context) {
        if (getInstance() != null)
            return getInstance();
        JIPipe instance = JIPipe.createInstance(context, JIPipeServiceMode.GUI);
        JIPipe.getInstance().initialize();
        return instance;
    }

    /**
     * Helper to create JIPipe from a context.
     * Will create a new JIPipe instance, so be careful.
     * We recommend using the ensureInstance() method.
     * <p>
     * Initializes JIPipe with GUI mode.
     *
     * @param context the context
     */
    public static JIPipe createInstance(Context context) {
        return createInstance(context, JIPipeServiceMode.GUI);
    }

    /**
     * Helper to create JIPipe from a context.
     * Will create a new JIPipe instance, so be careful.
     * We recommend using the ensureInstance() method.
     *
     * @param context the context
     * @param mode    the mode
     */
    public static JIPipe createInstance(Context context, JIPipeServiceMode mode) {
        PluginService pluginService = context.getService(PluginService.class);
        try {
            instance = (JIPipe) pluginService.getPlugin(JIPipe.class).createInstance();
            instance.mode = mode;
            context.inject(instance);
            instance.setContext(context);
        } catch (InstantiableException e) {
            throw new RuntimeException(e);
        }
        return instance;
    }

    public static boolean isInstantiated() {
        return instance != null;
    }

    /**
     * Compares two plugins and sorts them by priority
     *
     * @param p0 Plugin
     * @param p1 Plugin
     * @return Comparator result
     */
    public static int comparePlugins(PluginInfo<?> p0, PluginInfo<?> p1) {
        return -Double.compare(p0.getPriority(), p1.getPriority());
    }

    /**
     * Loads a project
     *
     * @param fileName     Project file
     * @param progressInfo the progress info
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeProgressInfo progressInfo) throws IOException {
        return loadProject(fileName, new JIPipeValidationReport(), new JIPipeNotificationInbox(), progressInfo);
    }

    /**
     * Loads a project
     *
     * @param fileName      Project file
     * @param notifications notifications for the user
     * @param progressInfo  the progress info
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException {
        return loadProject(fileName, new JIPipeValidationReport(), notifications, progressInfo);
    }

    /**
     * Loads a project
     *
     * @param fileName      Project file
     * @param report        Report whether the project is valid
     * @param notifications notifications for the user
     * @param progressInfo  the progress info
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReport report, JIPipeNotificationInbox notifications, JIPipeProgressInfo progressInfo) throws IOException {
        return JIPipeProject.loadProject(fileName, new UnspecifiedValidationReportContext(), report, notifications, progressInfo);
    }

    /**
     * Loads a project
     *
     * @param fileName     Project file
     * @param report       Report whether the project is valid
     * @param progressInfo the progress info
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) throws IOException {
        return JIPipeProject.loadProject(fileName, new UnspecifiedValidationReportContext(), report, new JIPipeNotificationInbox(), progressInfo);
    }

    /**
     * Runs a project in the current thread.
     * The progress will be put into the stdout
     * This will block the current thread.
     *
     * @param project      the project
     * @param outputFolder the output folder
     * @param threads      the number of threads (set to zero for using the default value)
     * @return the result
     */
    public static JIPipeGraphRun runProject(JIPipeProject project, Path outputFolder, int threads) {
        JIPipeGraphRunConfiguration settings = new JIPipeGraphRunConfiguration();
        settings.setOutputPath(outputFolder);
        if (threads > 0)
            settings.setNumThreads(threads);
        JIPipeGraphRun run = new JIPipeGraphRun(project, settings);
        run.run();
        return run;
    }

    /**
     * Runs a project in the current thread.
     * The progress will be put into the stdout
     * This will block the current thread.
     *
     * @param project  the project
     * @param settings settings for the run
     * @return the result
     */
    public static JIPipeGraphRun runProject(JIPipeProject project, JIPipeGraphRunConfiguration settings) {
        JIPipeGraphRun run = new JIPipeGraphRun(project, settings);
        run.run();
        return run;
    }

    /**
     * Runs a project in a different thread.
     * The progress will be put into the stdout
     *
     * @param project      the project
     * @param outputFolder the output folder
     * @param threads      the number of threads (set to zero for using the default value)
     * @return the future result. You have to check the {@link JIPipeRunnableQueue} to see if the run is finished.
     */
    public static JIPipeGraphRun enqueueProject(JIPipeProject project, Path outputFolder, int threads) {
        JIPipeGraphRunConfiguration settings = new JIPipeGraphRunConfiguration();
        settings.setOutputPath(outputFolder);
        if (threads > 0)
            settings.setNumThreads(threads);
        JIPipeGraphRun run = new JIPipeGraphRun(project, settings);
        JIPipeRunnableQueue.getInstance().enqueue(run);
        return run;
    }

    /**
     * Runs a project in the current thread.
     * The progress will be put into the stdout
     *
     * @param project  the project
     * @param settings settings for the run
     * @return the future result. You have to check the {@link JIPipeRunnableQueue} to see if the run is finished.
     */
    public static JIPipeGraphRun enqueueProject(JIPipeProject project, JIPipeGraphRunConfiguration settings) {
        JIPipeGraphRun run = new JIPipeGraphRun(project, settings);
        JIPipeRunnableQueue.getInstance().enqueue(run);
        return run;
    }

    /**
     * Creates a new node instance from its id
     *
     * @param id  Algorithm ID
     * @param <T> Algorithm class
     * @return Algorithm instance
     */
    public static <T extends JIPipeGraphNode> T createNode(String id) {
        return (T) getNodes().getInfoById(id).newInstance();
    }

    /**
     * Creates a new node instance from its class.
     * Please note that this might not work for all node types, as there is no 1:1 relation between node classes and their Ids
     *
     * @param klass node class
     * @param <T>   node class
     * @return the node
     */
    public static <T extends JIPipeGraphNode> T createNode(Class<T> klass) {
        Set<JIPipeNodeInfo> nodeInfos = getNodes().getNodeInfosFromClass(klass);
        if (nodeInfos.size() > 1)
            throw new RuntimeException("There are multiple node infos registered for " + klass);
        if (nodeInfos.isEmpty())
            throw new IndexOutOfBoundsException("No node infos registered for " + klass);
        return (T) nodeInfos.iterator().next().newInstance();
    }

    /**
     * Duplicates a {@link JIPipeGraphNode}
     *
     * @param node the node
     * @param <T>  the node class
     * @return a deep copy
     */
    public static <T extends JIPipeGraphNode> T duplicateNode(T node) {
        if (node.getInfo() == null) {
            System.err.println("Warning: Node " + node + " has no info attached. Create nodes via the static JIPipe method!");
            try {
                return (T) node.getClass().getConstructor(node.getClass()).newInstance(node);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } else {
            return (T) node.getInfo().duplicate(node);
        }
    }

    /**
     * Duplicates a value that is registered as parameter
     *
     * @param value the value. can be null
     * @param <T>   the type of the value
     * @return duplicate of the value
     */
    public static <T> T duplicateParameter(T value) {
        if (value == null)
            return null;
        JIPipeParameterTypeInfo parameterTypeInfo = getParameterTypes().getInfoByFieldClass(value.getClass());
        return (T) parameterTypeInfo.duplicate(value);
    }

    /**
     * Instantiates a data class with the provided parameters
     * This method is helpful if output data is constructed based on slot types
     *
     * @param klass                 The data class
     * @param constructorParameters Constructor parameters
     * @param <T>                   Data class
     * @return Data instance
     */
    public static <T extends JIPipeData> T createData(Class<T> klass, Object... constructorParameters) {
        try {
            return ConstructorUtils.invokeConstructor(klass, constructorParameters);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 InstantiationException e) {
            throw new JIPipeValidationRuntimeException(e, "Cannot create data instance!", "There is an error in the code that provides the annotation type.",
                    "Please contact the author of the plugin that provides the annotation type " + klass);
        }
    }

    /**
     * The current version of JIPipe according to the Maven-proved information
     *
     * @return the version string or 'Development' if none is available
     */
    public static String getJIPipeVersion() {
        return VersionUtils.getJIPipeVersion();
    }

    /**
     * Returns if the ID is a valid extension ID
     * Must have following structure: [group]:[artifact]
     * [group] should be lower-case and be a valid Maven group ID
     * [artifact] should be lower-case and a valid Maven artifact ID
     *
     * @param id the ID
     * @return if the id is a valid extension id
     */
    public static boolean isValidExtensionId(String id) {
        if (!StringUtils.isNullOrEmpty(id) && id.contains(":")) {
            if (!id.equals(id.toLowerCase(Locale.ROOT)))
                return false;
            String[] split = id.split(":");
            if (split.length != 2)
                return false;
            String groupId = split[0];
            if (groupId.startsWith(".") || groupId.endsWith(".") || groupId.contains(".."))
                return false;
            Pattern groupPattern = Pattern.compile("[a-z0-9-.]+");
            if (!groupPattern.matcher(groupId).matches())
                return false;
            String artifactId = split[1];
            Pattern artifactPattern = Pattern.compile("[a-z0-9-]+");
            if (!artifactPattern.matcher(artifactId).matches())
                return false;
            return true;
        }
        return false;
    }

    /**
     * Gets the JIPipe user-writable directory.
     * Can be overwritten by setting the JIPIPE_USER_DIR environment variable to deploy JIPipe into a read-only environment
     *
     * @return the JIPipe user directory
     */
    public static Path getJIPipeUserDir(boolean allowOldProfile) {
        return PathUtils.getJIPipeUserDir(allowOldProfile);
    }

    /**
     * Exits the Java application in 500ms
     * Prevents the lockup of Java under certain circumstances
     *
     * @param exitCode the exit code
     */
    public static void exitLater(int exitCode) {
        if (!JIPipe.NO_SETTINGS_AUTOSAVE) {
            JIPipe.getSettings().save();
        }
        Timer timer = new Timer(500, e -> {
//            System.exit(exitCode);
            // Context introduces a shutdown hook that causes a deadlock
            Runtime.getRuntime().halt(exitCode);
        });
        timer.start();
    }

    /**
     * Gets the global JIPipe core resource manager
     * Convenience wrapper
     * @return the resource manager
     */
    public static JIPipeResourceManager getResources() {
        return RESOURCES;
    }


}
