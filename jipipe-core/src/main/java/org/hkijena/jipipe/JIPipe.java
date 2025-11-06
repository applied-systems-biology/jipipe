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

import net.imagej.ImageJ;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.project.JIPipeProject;
import org.hkijena.jipipe.api.run.JIPipeGraphRun;
import org.hkijena.jipipe.api.run.JIPipeGraphRunConfiguration;
import org.hkijena.jipipe.api.run.JIPipeRunnableQueue;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.JIPipeServiceInitializationSettings;
import org.hkijena.jipipe.api.service.components.*;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.UnspecifiedValidationReportContext;
import org.hkijena.jipipe.plugins.parameters.library.primitives.optional.OptionalPathParameter;
import org.hkijena.jipipe.plugins.settings.application.JIPipeRuntimeApplicationSettings;
import org.hkijena.jipipe.utils.JIPipeResourceManager;
import org.hkijena.jipipe.utils.PathUtils;
import org.hkijena.jipipe.utils.VersionUtils;
import org.scijava.Context;
import org.scijava.InstantiableException;
import org.scijava.plugin.PluginInfo;
import org.scijava.plugin.PluginService;

import javax.swing.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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

    public static JIPipeParameterTypesServiceComponent getParameterTypes() {
        return instance.getParameterTypes();
    }

    public static JIPipeExpressionFunctionsServiceComponent getTableOperations() {
        return instance.getExpressionFunctions();
    }

    public static JIPipeCustomMenuItemsServiceComponent getCustomMenus() {
        return instance.getCustomMenuItems();
    }

    public static JIPipeImageJAdaptersServiceComponent getImageJAdapters() {
        return instance.getImageJDataAdapters();
    }

    public static JIPipeApplicationSettingsServiceComponent getSettings() {
        return instance.getApplicationSettings();
    }

    public static JIPipeNodesServiceComponent getNodes() {
        return instance.getNodes();
    }

    public static JIPipeDatatypesServiceComponent getDataTypes() {
        return instance.getDataTypes();
    }

    public static JIPipeArtifactsServiceComponent getArtifacts() {
        return instance.getArtifacts();
    }

    public static JIPipeNodeTemplatesServiceComponent getNodeTemplates() {
        return instance.getNodeTemplates();
    }

    /**
     * @return Singleton instance
     */
    public static JIPipeService getInstance() {
        return instance;
    }

    /**
     * Ensures that JIPipe is initialized and available.
     * Creates a new {@link ImageJ} instance to obtain a {@link Context} if JIPipe is not initialized already.
     *
     * @return the current instance
     */
    public static JIPipeService ensureInstance() {
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
    public static JIPipeService ensureInstance(Context context) {
        if (getInstance() != null)
            return getInstance();
        JIPipeService instance = JIPipe.createInstance(context);
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
    public static JIPipeService createInstance(Context context) {
        return createInstance(context, new JIPipeServiceInitializationSettings());
    }

    /**
     * Helper to create JIPipe from a context.
     * Will create a new JIPipe instance, so be careful.
     * We recommend using the ensureInstance() method.
     *
     * @param context  the context
     * @param settings the initialization settings
     */
    public static JIPipeService createInstance(Context context, JIPipeServiceInitializationSettings settings) {
        PluginService pluginService = context.getService(PluginService.class);
        try {
            instance = (JIPipeService) pluginService.getPlugin(JIPipeService.class).createInstance();
            instance.setInitializationSettings(settings);
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
     * Gets the JIPipe user-writable path.
     * Can be overwritten by setting the JIPIPE_USER_DIR environment variable to deploy JIPipe into a read-only environment
     *
     * @return the JIPipe user path
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
        if (instance != null && instance.isAutosaveSettings()) {
            instance.getApplicationSettings().save();
        }
        if (instance != null) {
            instance.dispose();
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
     *
     * @return the resource manager
     */
    public static JIPipeResourceManager getResources() {
        return RESOURCES;
    }


    /**
     * Auto-saves application settings if they are enabled
     */
    public static void autoSaveSettings() {
        if (instance != null && instance.isAutosaveSettings()) {
            instance.getApplicationSettings().save();
        }
    }

    public static Path getTemporaryBaseDirectory() {
        if (getInstance() == null || !getInstance().getApplicationSettings().getRegisteredSheets().containsKey(JIPipeRuntimeApplicationSettings.ID)) {
            return PathUtils.createGlobalTempDirectory("JIPipe");
        }
        OptionalPathParameter tempDirectory = JIPipeRuntimeApplicationSettings.getInstance().getTempDirectory();
        if (tempDirectory.isEnabled()) {
            try {
                if (tempDirectory.getContent().isAbsolute()) {
                    Files.createDirectories(tempDirectory.getContent());
                    return tempDirectory.getContent();
                } else {
                    Path absPath = Files.createDirectories(PathUtils.getJIPipeUserDir().resolve(tempDirectory.getContent()));
                    Files.createDirectories(absPath);
                    return absPath;
                }
            } catch (IOException e) {
                System.err.println("Fallback temporary directory due to following error:");
                e.printStackTrace();
                return PathUtils.createGlobalTempDirectory("JIPipe");
            }
        } else {
            return PathUtils.createGlobalTempDirectory("JIPipe");
        }
    }

    /**
     * Generates a temporary directory
     *
     * @param baseName optional base name
     * @return a temporary directory
     */
    public static Path getTemporaryDirectory(String baseName) {
        return PathUtils.createTempSubDirectory(getTemporaryBaseDirectory(), baseName);
    }

    /**
     * Generates a temporary directory
     *
     * @param prefix prefix
     * @param suffix suffix
     * @return a temporary directory
     */
    public static Path getTemporaryFile(String prefix, String suffix) {
        return PathUtils.createSubTempFilePath(getTemporaryBaseDirectory(), prefix, suffix);
    }

    public static JIPipeEnvironmentsServiceComponent getEnvironments() {
        return getInstance().getEnvironments();
    }
}
