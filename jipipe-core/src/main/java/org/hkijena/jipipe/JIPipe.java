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
import ij.Prefs;
import net.imagej.ImageJ;
import net.imagej.ui.swing.updater.SwingAuthenticator;
import net.imagej.updater.FilesCollection;
import net.imagej.updater.UpdateSite;
import net.imagej.updater.util.AvailableSites;
import net.imagej.updater.util.Progress;
import net.imagej.updater.util.UpdaterUtil;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.api.JIPipeNodeTemplate;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.data.JIPipeLegacyDataImportOperation;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
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
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDefaultDataViewer;
import org.hkijena.jipipe.desktop.api.registries.JIPipeCustomMenuRegistry;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopProjectWindow;
import org.hkijena.jipipe.desktop.app.running.logs.JIPipeDesktopRunnableLogsCollection;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactAccelerationPreference;
import org.hkijena.jipipe.plugins.artifacts.JIPipeArtifactApplicationSettings;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.primitives.vectors.Vector2iParameter;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultCacheDisplayApplicationSettings;
import org.hkijena.jipipe.plugins.settings.JIPipeDefaultResultImporterApplicationSettings;
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
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.Timer;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Authenticator;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * A scijava service that discovers JIPipe plugins in the classpath
 */
@Plugin(type = JIPipeService.class)
public class JIPipe extends AbstractService implements JIPipeService {

    /**
     * Resource manager for core JIPipe
     */
    public static final JIPipeResourceManager RESOURCES = new JIPipeResourceManager(JIPipe.class, "org/hkijena/jipipe");

    /**
     * To be used with an alternative start mode not involving ImageJ
     */
    public static boolean NO_IMAGEJ = false;

    /**
     * Prevent saving settings
     */
    public static boolean NO_SETTINGS_AUTOSAVE = false;

    /**
     * Allows to override the directory where profiles are located (needs to be done before init!)
     * Overridden by the JIPIPE_OVERRIDE_USER_DIR_BASE environment variable
     */
    public static Path OVERRIDE_USER_DIR_BASE = null;


    private static JIPipe instance;
    private static boolean IS_RESTARTING = false;
    private final JIPipeProgressInfo progressInfo = new JIPipeProgressInfo();
    private final Set<String> registeredExtensionIds = new HashSet<>();
    private final List<JIPipeDependency> registeredExtensions = new ArrayList<>();
    private final List<JIPipeDependency> failedExtensions = new ArrayList<>();
    private final JIPipeNodeRegistry nodeRegistry;
    private final JIPipeDatatypeRegistry datatypeRegistry;
    private final JIPipeImageJAdapterRegistry imageJDataAdapterRegistry;
    private final JIPipeCustomMenuRegistry customMenuRegistry;
    private final JIPipeParameterTypeRegistry parameterTypeRegistry;
    private final JIPipeApplicationSettingsRegistry applicationSettingsRegistry;
    private final JIPipeProjectSettingsRegistry projectSettingsRegistry;
    private final JIPipeExpressionRegistry tableOperationRegistry;
    private final JIPipeUtilityRegistry utilityRegistry;
    private final JIPipeExternalEnvironmentRegistry externalEnvironmentRegistry;
    private final JIPipePluginRegistry pluginRegistry;
    private final JIPipeGraphEditorToolRegistry graphEditorToolRegistry;
    private final JIPipeProjectTemplateRegistry projectTemplateRegistry;
    private final JIPipeArtifactsRegistry artifactsRegistry;
    private final JIPipeNodeTemplateRegistry nodeTemplateRegistry;
    private final JIPipeRecentProjectsRegistry recentProjectsRegistry;

    private final JIPipeMetadataRegistry metadataRegistry;
    private final DatatypeRegisteredEventEmitter datatypeRegisteredEventEmitter = new DatatypeRegisteredEventEmitter();
    private final ExtensionContentAddedEventEmitter extensionContentAddedEventEmitter = new ExtensionContentAddedEventEmitter();
    private final ExtensionContentRemovedEventEmitter extensionContentRemovedEventEmitter = new ExtensionContentRemovedEventEmitter();
    private final ExtensionDiscoveredEventEmitter extensionDiscoveredEventEmitter = new ExtensionDiscoveredEventEmitter();
    private final ExtensionRegisteredEventEmitter extensionRegisteredEventEmitter = new ExtensionRegisteredEventEmitter();
    private final NodeInfoRegisteredEventEmitter nodeInfoRegisteredEventEmitter = new NodeInfoRegisteredEventEmitter();
    private FilesCollection imageJPlugins = null;
    private boolean initializing = false;
    @Parameter
    private LogService logService;
    @Parameter
    private PluginService pluginService;
    private JIPipeMode mode = JIPipeMode.GUI;

    public JIPipe() {
        recentProjectsRegistry = new JIPipeRecentProjectsRegistry(this);
        nodeRegistry = new JIPipeNodeRegistry(this);
        datatypeRegistry = new JIPipeDatatypeRegistry(this);
        imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry(this);
        customMenuRegistry = new JIPipeCustomMenuRegistry(this);
        parameterTypeRegistry = new JIPipeParameterTypeRegistry(this);
        applicationSettingsRegistry = new JIPipeApplicationSettingsRegistry(this);
        projectSettingsRegistry = new JIPipeProjectSettingsRegistry(this);
        tableOperationRegistry = new JIPipeExpressionRegistry(this);
        utilityRegistry = new JIPipeUtilityRegistry(this);
        externalEnvironmentRegistry = new JIPipeExternalEnvironmentRegistry(this);
        pluginRegistry = new JIPipePluginRegistry(this);
        projectTemplateRegistry = new JIPipeProjectTemplateRegistry(this);
        graphEditorToolRegistry = new JIPipeGraphEditorToolRegistry(this);
        metadataRegistry = new JIPipeMetadataRegistry(this);
        artifactsRegistry = new JIPipeArtifactsRegistry(this);
        nodeTemplateRegistry = new JIPipeNodeTemplateRegistry(this);
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
        JIPipe instance = JIPipe.createInstance(context, JIPipeMode.GUI);
        JIPipe.getInstance().initialize();
        return instance;
    }

    /**
     * Helper to create JIPipe from a context.
     * Will create a new JIPipe instance, so be careful.
     * We recommend using the ensureInstance() method.
     *
     * Initializes JIPipe with GUI mode.
     *
     * @param context the context
     */
    public static JIPipe createInstance(Context context) {
        return createInstance(context, JIPipeMode.GUI);
    }

    /**
     * Helper to create JIPipe from a context.
     * Will create a new JIPipe instance, so be careful.
     * We recommend using the ensureInstance() method.
     *
     * @param context the context
     * @param mode the mode
     */
    public static JIPipe createInstance(Context context, JIPipeMode mode) {
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

    /**
     * Create a new JIPipe using initializeLibNoImageJ, which is suitable for using limited functions from within non-ImageJ applications.
     * Avoids interacting with SciJava as much as possible.
     *
     * @param plugins list of plugins to load
     * @return the JIPipe instance
     */
    public static JIPipe createLibNoImageJInstance(List<Class<? extends JIPipeJavaPlugin>> plugins) {
        NO_IMAGEJ = true;
        NO_SETTINGS_AUTOSAVE = true;
        instance = new JIPipe();
        instance.setContext(new Context());
        instance.initializeLibNoImageJ(plugins);
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
     * @param fileName Project file
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName) throws IOException {
        return loadProject(fileName, new JIPipeValidationReport(), new JIPipeNotificationInbox());
    }

    /**
     * Loads a project
     *
     * @param fileName      Project file
     * @param notifications notifications for the user
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeNotificationInbox notifications) throws IOException {
        return loadProject(fileName, new JIPipeValidationReport(), notifications);
    }

    /**
     * Loads a project
     *
     * @param fileName      Project file
     * @param report        Report whether the project is valid
     * @param notifications notifications for the user
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReport report, JIPipeNotificationInbox notifications) throws IOException {
        return JIPipeProject.loadProject(fileName, new UnspecifiedValidationReportContext(), report, notifications);
    }

    /**
     * Loads a project
     *
     * @param fileName Project file
     * @param report   Report whether the project is valid
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeValidationReport report) throws IOException {
        return JIPipeProject.loadProject(fileName, new UnspecifiedValidationReportContext(), report, new JIPipeNotificationInbox());
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
    public static Path getJIPipeUserDir() {
        return PathUtils.getJIPipeUserDir();
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


    @Override
    public DatatypeRegisteredEventEmitter getDatatypeRegisteredEventEmitter() {
        return datatypeRegisteredEventEmitter;
    }

    @Override
    public ExtensionContentAddedEventEmitter getExtensionContentAddedEventEmitter() {
        return extensionContentAddedEventEmitter;
    }

    @Override
    public ExtensionContentRemovedEventEmitter getExtensionContentRemovedEventEmitter() {
        return extensionContentRemovedEventEmitter;
    }

    @Override
    public ExtensionDiscoveredEventEmitter getExtensionDiscoveredEventEmitter() {
        return extensionDiscoveredEventEmitter;
    }

    @Override
    public ExtensionRegisteredEventEmitter getExtensionRegisteredEventEmitter() {
        return extensionRegisteredEventEmitter;
    }

    @Override
    public NodeInfoRegisteredEventEmitter getNodeInfoRegisteredEventEmitter() {
        return nodeInfoRegisteredEventEmitter;
    }

    public JIPipeExpressionRegistry getTableOperationRegistry() {
        return tableOperationRegistry;
    }

    @Override
    public JIPipeGraphEditorToolRegistry getGraphEditorToolRegistry() {
        return graphEditorToolRegistry;
    }

    @Override
    public JIPipeProjectTemplateRegistry getProjectTemplateRegistry() {
        return projectTemplateRegistry;
    }

    public JIPipeProgressInfo getProgressInfo() {
        return progressInfo;
    }

    public List<JIPipeDependency> getFailedExtensions() {
        return Collections.unmodifiableList(failedExtensions);
    }

    /**
     * Initializes JIPipe as part of a library with a custom initialization step
     * No settings are loaded and SciJava services are not used.
     * No dependency checking, init, and activation protection (plugins are initialized as-is in the given order)
     * No validation is applied
     *
     * @param plugins the list of plugins to load
     */
    public void initializeLibNoImageJ(List<Class<? extends JIPipeJavaPlugin>> plugins) {
        initializing = true;

        progressInfo.setProgress(0, 5);
        nodeRegistry.installEvents();
        pluginRegistry.initialize(); // Init extension registry
        pluginRegistry.load();
        progressInfo.setProgress(1);
        progressInfo.log("Pre-initialization phase ...");

        List<JIPipeJavaPlugin> pluginInstances = new ArrayList<>();
        for (Class<? extends JIPipeJavaPlugin> pluginClass : plugins) {
            try {
                JIPipeJavaPlugin extension = pluginClass.newInstance();
                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }

                pluginInstances.add(extension);
                extensionDiscoveredEventEmitter.emit(new ExtensionDiscoveredEvent(this, extension));
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        progressInfo.setProgress(2);
        JIPipeProgressInfo registerFeaturesProgress = progressInfo.resolveAndLog("Register features");

        for (JIPipeJavaPlugin extension : pluginInstances) {
            extension.register(this, getContext(), registerFeaturesProgress.resolve(extension.getDependencyId()));
            registeredExtensions.add(extension);
            registeredExtensionIds.add(extension.getDependencyId());
            extensionRegisteredEventEmitter.emit(new ExtensionRegisteredEvent(this, extension));
        }

        registerFeaturesProgress.log("Registering remaining " + nodeRegistry.getScheduledRegistrationTasks().size() + " features ...");
        for (JIPipeNodeRegistrationTask task : nodeRegistry.getScheduledRegistrationTasks()) {
            try {
                task.register();
            } catch (Throwable ex) {
                logService.error("Could not register: " + task.toString() + " -> " + ex);
                registerFeaturesProgress.log("Could not register: " + task + " -> " + ex);
            }
        }

        // Create settings for default importers
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();

        // Required as the reload deletes the allowed values
        updateDefaultImporterSettings();
        updateDefaultCacheDisplaySettings();

        // Postprocessing
        progressInfo.setProgress(5);
        JIPipeProgressInfo postprocessingProgress = progressInfo.resolveAndLog("Postprocessing");
        for (JIPipeDependency extension : registeredExtensions) {
            if (!failedExtensions.contains(extension) && extension instanceof JIPipeJavaPlugin) {
                ((JIPipeJavaPlugin) extension).postprocess(postprocessingProgress.resolveAndLog(extension.getDependencyId()));
            }
        }
        postprocessingProgress.log("Converting display operations to import operations ...");
        datatypeRegistry.convertDisplayOperationsToImportOperations();
        postprocessingProgress.log("Registering examples ...");
        nodeRegistry.executeScheduledRegisterExamples();
        postprocessingProgress.log("Registering extension-provided templates ...");
        nodeRegistry.executeScheduledRegisterTemplates();

        initializing = false;

        // Push progress into log
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry("JIPipe initialization",
                LocalDateTime.now(),
                progressInfo.getLog().toString(),
                new JIPipeNotificationInbox(), true));

        // Mark log as read
        JIPipeDesktopRunnableLogsCollection.getInstance().markAllAsRead();
    }

    /**
     * Initializes JIPipe. Uses the default extension settings and discards any detected issues.
     */
    public void initialize() {
        if (NO_IMAGEJ) {
            return;
        }
        initialize(JIPipeExtensionApplicationSettings.getInstanceFromRaw(), new JIPipeRegistryIssues(), true);
    }

    /**
     * Initializes JIPipe
     *
     * @param extensionSettings extension settings
     * @param issues            if no windows should be opened
     * @param verbose           if all steps should be logged (otherwise the initialization will be silent)
     */
    public void initialize(JIPipeExtensionApplicationSettings extensionSettings, JIPipeRegistryIssues issues, boolean verbose) {
        initializing = true;

        progressInfo.setProgress(0, 5);
        if (verbose) {
            progressInfo.getStatusUpdatedEventEmitter().subscribeLambda((emitter, event) -> {
                logService.info(event.getMessage());
            });
        }

        IJ.showStatus("Initializing JIPipe ...");
        nodeRegistry.installEvents();
        List<PluginInfo<JIPipeJavaPlugin>> pluginList = pluginService.getPluginsOfType(JIPipeJavaPlugin.class).stream()
                .sorted(JIPipe::comparePlugins).collect(Collectors.toList());
        pluginRegistry.initialize(); // Init extension registry
        pluginRegistry.load();
        progressInfo.setProgress(1);
        progressInfo.log("Legacy settings conversion ...");
        copyTemplatesFromPropertiesToLegacyProfile(progressInfo.resolve("Legacy conversion"));
        if (applyProfileUpgrades(progressInfo.resolve("Preparing profiles"))) {
            progressInfo.log("Upgrade was applied. Reloading extension settings.");
            extensionSettings = JIPipeExtensionApplicationSettings.getInstanceFromRaw();
        }

        progressInfo.log("Pre-initialization phase ...");

        // Creating instances of extensions
        Map<String, JIPipeJavaExtensionInitializationInfo> allJavaExtensionsByID = new HashMap<>();

        for (PluginInfo<JIPipeJavaPlugin> pluginInfo : pluginList) {
            try {
                progressInfo.log("Creating instance of " + pluginInfo + " ...");
                JIPipeJavaPlugin extension = pluginInfo.createInstance();

                // Validate ID
                if (!isValidExtensionId(extension.getDependencyId())) {
                    System.err.println("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + ". REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");
                    progressInfo.log("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + ". REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");

                    continue;
                } else {
                    if (allJavaExtensionsByID.containsKey(extension.getDependencyId())) {
                        System.err.println("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder. REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");
                        progressInfo.log("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder. REFUSING TO REGISTER AS OF JIPIPE VERSION 3!");

                        continue;
                    }
                }

                JIPipeJavaExtensionInitializationInfo initializationInfo = new JIPipeJavaExtensionInitializationInfo();
                initializationInfo.setPluginInfo(pluginInfo);
                initializationInfo.setInstance(extension);
                allJavaExtensionsByID.put(extension.getDependencyId(), initializationInfo);

            } catch (Throwable e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(pluginInfo);
            }
        }

        // First loading check
        progressInfo.log("Determining extensions to be loaded ...");
        Set<String> impliedLoadedJavaExtensions = new HashSet<>();
        boolean impliedLoadedJavaExtensionsChanged;
        do {
            impliedLoadedJavaExtensionsChanged = false;
            for (JIPipeJavaExtensionInitializationInfo initializationInfo : allJavaExtensionsByID.values()) {
                JIPipeJavaPlugin extension = initializationInfo.getInstance();
                if (extension == null) {
                    continue;
                }
                if (extension.isCorePlugin() || impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                    if (isValidExtensionId(extension.getDependencyId())) {
                        if (!impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                            progressInfo.log("-> Core/User: " + extension.getDependencyId());
                            impliedLoadedJavaExtensions.add(extension.getDependencyId());
                            impliedLoadedJavaExtensionsChanged = true;
                        }
                    }
                    for (JIPipeDependency dependency : extension.getDependencies()) {
                        if (isValidExtensionId(dependency.getDependencyId())) {
                            if (!impliedLoadedJavaExtensions.contains(dependency.getDependencyId())) {
                                impliedLoadedJavaExtensions.add(dependency.getDependencyId());
                                impliedLoadedJavaExtensionsChanged = true;
                                progressInfo.log("-> Required by " + extension.getDependencyId() + ": " + dependency.getDependencyId());
                            }
                        }
                    }
                }
            }
        }
        while (impliedLoadedJavaExtensionsChanged);

        boolean preActivationScheduledSave = false;

        ImmutableList<JIPipeJavaExtensionInitializationInfo> allJavaExtensionsList = ImmutableList.copyOf(allJavaExtensionsByID.values());
        for (int i = 0; i < allJavaExtensionsList.size(); i++) {
            JIPipeJavaExtensionInitializationInfo initializationInfo = allJavaExtensionsList.get(i);
            IJ.showProgress(i + 1, allJavaExtensionsList.size());
            JIPipeJavaPlugin extension = initializationInfo.getInstance();

            if (extension == null) {
                continue;
            }
            try {
                pluginRegistry.registerKnownPlugin(extension);

                // Check if the extension should be loaded
                if (!extension.isCorePlugin() && pluginRegistry.getSettings().getDeactivatedPlugins().contains(extension.getDependencyId()) && !impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                    progressInfo.log("Extension with ID " + extension.getDependencyId() + " will not be loaded (deactivated in extension manager)");
                    initializationInfo.setLoaded(false);
                    continue;
                }

                // Extension self-check
                JIPipeValidationReport preActivationIssues = new JIPipeValidationReport();
                issues.getPreActivationIssues().put(extension.getDependencyId(), preActivationIssues);
                if (!extension.canActivate(preActivationIssues, progressInfo.resolve("Pre-activation check").resolve(extension.getDependencyId()))) {
                    if (!extensionSettings.isIgnorePreActivationChecks()) {
                        preActivationIssues.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Warning,
                                new JavaExtensionValidationReportContext(extension),
                                "Extension '" + extension.getMetadata().getName() + "' refuses to activate!",
                                "The extension's pre-activation check failed. It will not be activated. Please refer to the other items if available.",
                                null,
                                null));
                        progressInfo.log("Extension with ID " + extension.getDependencyId() + " will not be loaded (pre-activation check failed; extension refuses to activate)");
                        initializationInfo.setLoaded(false);
                        if (!StringUtils.isNullOrEmpty(extension.getDependencyId())) {
                            progressInfo.log("Extension with ID " + extension.getDependencyId() + " was removed from the list of activated extensions");
                            pluginRegistry.getSettings().getDeactivatedPlugins().add(extension.getDependencyId());
                            preActivationScheduledSave = true;
                        }
                        continue;
                    } else {
                        progressInfo.log("Extension with ID " + extension.getDependencyId() + " indicated that its pre-activation checks failed. WILL BE LOADED anyway DUE TO APPLICATION SETTINGS!");
                    }
                }

                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }
                initializationInfo.setLoaded(true);
                extensionDiscoveredEventEmitter.emit(new ExtensionDiscoveredEvent(this, extension));
            } catch (Throwable e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(initializationInfo.getPluginInfo());
            }
        }

        // Save extension settings
        if (preActivationScheduledSave && !NO_SETTINGS_AUTOSAVE) {
            pluginRegistry.save();
        }

        progressInfo.setProgress(2);
        JIPipeProgressInfo registerFeaturesProgress = progressInfo.resolveAndLog("Register features");
        for (int i = 0; i < allJavaExtensionsList.size(); i++) {
            JIPipeJavaExtensionInitializationInfo initializationInfo = allJavaExtensionsList.get(i);

            if (!initializationInfo.isLoaded()) {
                registerFeaturesProgress.log("Skipping (deactivated in extension manager or is refusing to activate)");
                continue;
            }

            PluginInfo<JIPipeJavaPlugin> info = initializationInfo.getPluginInfo();

            IJ.showProgress(i + 1, allJavaExtensionsList.size());
            registerFeaturesProgress.log("Registering plugin " + info);
            JIPipeJavaPlugin extension = null;
            try {
                extension = initializationInfo.getInstance();
                registerFeaturesProgress.log("ID=" + extension.getDependencyId());
                extension.register(this, getContext(), progressInfo.resolve(extension.getDependencyId()));
                registeredExtensions.add(extension);
                registeredExtensionIds.add(extension.getDependencyId());
                extensionRegisteredEventEmitter.emit(new ExtensionRegisteredEvent(this, extension));
            } catch (NoClassDefFoundError | Exception e) {
                progressInfo.log("[!] ERROR: Unable to instantiate extension " + info);
                e.printStackTrace();
                progressInfo.log(e.toString());
                issues.getErroneousPlugins().add(info);
                if (extension != null)
                    failedExtensions.add(extension);
            }
        }

        registerFeaturesProgress.log("Registering remaining " + nodeRegistry.getScheduledRegistrationTasks().size() + " features ...");
        for (JIPipeNodeRegistrationTask task : nodeRegistry.getScheduledRegistrationTasks()) {
            try {
                task.register();
            } catch (Throwable ex) {
                logService.error("Could not register: " + task.toString() + " -> " + ex);
                registerFeaturesProgress.log("Could not register: " + task + " -> " + ex);
            }
        }

        // Check for errors

        progressInfo.setProgress(3);
        progressInfo.log("Validating node types ...");
        validateDataTypes(issues);
        if (extensionSettings.isValidateNodeTypes()) {
            validateNodeTypes(issues);
        }
        progressInfo.log("Validating parameter types ...");
        validateParameterTypes(issues);

        // Create dependency graph
        progressInfo.log("Creating dependency graph ...");
        pluginRegistry.getDependencyGraph();

        // Create settings for default importers
        progressInfo.log("Creating dynamic settings ...");
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();
        registerNodeExamplesFromFileSystem();
        registerProjectTemplatesFromFileSystem();

        // Reload settings
        progressInfo.setProgress(4);
        progressInfo.log("Loading settings ...");
        applicationSettingsRegistry.reload();

        // Required as the reload deletes the allowed values
        updateDefaultImporterSettings();
        updateDefaultCacheDisplaySettings();

        // Postprocessing
        progressInfo.setProgress(5);
        JIPipeProgressInfo postprocessingProgress = progressInfo.resolveAndLog("Postprocessing");
        for (JIPipeDependency extension : registeredExtensions) {
            if (!failedExtensions.contains(extension) && extension instanceof JIPipeJavaPlugin) {
                ((JIPipeJavaPlugin) extension).postprocess(postprocessingProgress.resolveAndLog(extension.getDependencyId()));
            }
        }
        postprocessingProgress.log("Converting display operations to import operations ...");
        datatypeRegistry.convertDisplayOperationsToImportOperations();
        postprocessingProgress.log("Registering examples ...");
        nodeRegistry.executeScheduledRegisterExamples();
        postprocessingProgress.log("Registering extension-provided templates ...");
        nodeRegistry.executeScheduledRegisterTemplates();

        // Check recent projects and backups
        progressInfo.setProgress(6);
        progressInfo.log("Checking recent projects ...");
        recentProjectsRegistry.reload();
        recentProjectsRegistry.cleanup();
        recentProjectsRegistry.migrateFromLegacy();

        // Check artifacts
        progressInfo.setProgress(7);
        artifactsRegistry.updateCachedArtifacts(progressInfo.resolve("Updating artifacts"));

        // Check acceleration
        if (JIPipeArtifactApplicationSettings.getInstance().isAutoConfigureAccelerationOnNextStartup()) {
            progressInfo.log("Determining acceleration profile ...");
            try {

                if (CUDAUtils.hasCudaSupport()) {
                    progressInfo.log("Determining acceleration profile ... CUDA support detected");
                    JIPipeArtifactApplicationSettings.getInstance().setAccelerationPreference(JIPipeArtifactAccelerationPreference.CUDA);

                    try {
                        JIPipeArtifactApplicationSettings.getInstance().setAccelerationPreferenceVersions(new Vector2iParameter(
                                CUDAUtils.getMinimumCudaVersion(),
                                0  // Broken due to Nvidia-SMI hanging on Linux -> have to use 0
                        ));
                        progressInfo.log("Determined CUDA version limits as " + JIPipeArtifactApplicationSettings.getInstance().getAccelerationPreferenceVersions());
                    } catch (Exception e) {
                        progressInfo.log(e);
                    }
                }

                JIPipeArtifactApplicationSettings.getInstance().setAutoConfigureAccelerationOnNextStartup(false);
                applicationSettingsRegistry.save();
            } catch (Exception e) {
                progressInfo.log(e);
            }
        }

        // Load templates
        progressInfo.log("Loading node templates ...");
        nodeTemplateRegistry.reloadGlobalTemplates(progressInfo.resolve("Node templates"));

        progressInfo.setProgress(8);
        progressInfo.log("JIPipe loading finished");
        initializing = false;

        // Check if we have viewers for everything
        for (Class<? extends JIPipeData> dataClass : datatypeRegistry.getRegisteredDataTypes().values()) {
            Class<? extends JIPipeDesktopDataViewer> defaultDataViewer = datatypeRegistry.getDefaultDataViewer(dataClass);
            if (defaultDataViewer == JIPipeDesktopDefaultDataViewer.class) {
                progressInfo.log("Info: Data type " + datatypeRegistry.getIdOf(dataClass) + " does not have a default data viewer");
            }
        }

        // Check for new extensions
        pluginRegistry.findNewPlugins();
        for (String newExtension : pluginRegistry.getNewPlugins()) {
            progressInfo.log("New extension found: " + newExtension);
        }

        // Push progress into log
        JIPipeDesktopRunnableLogsCollection.getInstance().pushToLog(new JIPipeRunnableLogEntry("JIPipe initialization",
                LocalDateTime.now(),
                progressInfo.getLog().toString(),
                new JIPipeNotificationInbox(), true));

        // Mark log as read
        JIPipeDesktopRunnableLogsCollection.getInstance().markAllAsRead();
    }

    private boolean applyProfileUpgrades(JIPipeProgressInfo progressInfo) {
        boolean settingsExist = Files.isRegularFile(PathUtils.getJIPipeUserDir().resolve("settings.json"));
        boolean childDirsExist = !PathUtils.listSubDirectories(PathUtils.getJIPipeUserDir()).isEmpty();

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
                progressInfo.log("Upgrading from profile " + oldProfileDirectory);
                PathUtils.copyDirectory(oldProfileDirectory, newProfileDirectory, dir -> !dir.equals(oldProfileBackupsDirectory) && !dir.startsWith(oldProfileBackupsDirectory), progressInfo.resolve("Copy profile"));
                progressInfo.log("Profile upgrade successful. Continuing.");
                return true;
            }

            // Check if we have a legacy profile that can be upgraded
            Path legacyProfileDirectory = PathUtils.getLegacyJIPipeUserDir();
            if (Files.isDirectory(legacyProfileDirectory)) {

                // Delete old 3rd party software
                progressInfo.log("Removing EasyInstaller directories in " + legacyProfileDirectory);
                for (Path subDirectory : PathUtils.listSubDirectories(legacyProfileDirectory)) {
                    if (subDirectory.getFileName().toString().startsWith("easyinstall-")) {
                        PathUtils.deleteDirectoryRecursively(subDirectory, progressInfo.resolve("Cleanup old 3rd party software"));
                    }
                }

                // Copy the profile
                progressInfo.log("Upgrading from profile " + legacyProfileDirectory);
                Path oldProfileBackupsDirectory = legacyProfileDirectory.resolve("backups");
                Path newProfileDirectory = profileBasePath.resolve(currentVersion);
                PathUtils.copyDirectory(legacyProfileDirectory, newProfileDirectory, dir -> !dir.equals(oldProfileBackupsDirectory) && !dir.startsWith(oldProfileBackupsDirectory), progressInfo.resolve("Copy profile"));
                progressInfo.log("Profile upgrade successful. Continuing.");
            }
            return true;
        } else {
            progressInfo.log(PathUtils.getJIPipeUserDir() + " already exists. No profile upgrades are needed.");
            return false;
        }
    }

    /**
     * Copies templates from the old storage inside jipipe.properties.json into the legacy template directory
     *
     * @param progressInfo the progress info
     */
    private void copyTemplatesFromPropertiesToLegacyProfile(JIPipeProgressInfo progressInfo) {
        Path legacySettingsPath = PathUtils.getImageJDir().resolve("jipipe.properties.json");

        // Convert node templates
        if (Files.isRegularFile(legacySettingsPath)) {
            progressInfo.log("Reading legacy settings " + legacySettingsPath);
            try {
                JsonNode jsonNode = JsonUtils.readFromFile(legacySettingsPath, JsonNode.class);

                JsonNode nodeTemplatesListNode = jsonNode.path("node-templates/node-templates");
                if (!nodeTemplatesListNode.isMissingNode()) {
                    progressInfo.log("Found legacy node templates!");
                    Path targetDir = nodeTemplateRegistry.getLegacyStoragePath();
                    Files.createDirectories(targetDir);
                    for (JsonNode node : ImmutableList.copyOf(nodeTemplatesListNode.elements())) {
                        Path targetFile = targetDir.resolve(UUID.randomUUID() + ".json");
                        progressInfo.log("Writing legacy node template: " + targetFile);
                        JsonUtils.saveToFile(node, targetFile);
                    }
                }
            } catch (Throwable e) {
                progressInfo.log("Unable to copy settings!");
                progressInfo.log(ExceptionUtils.getStackTrace(e));
            }
        }
    }

    private void registerProjectTemplatesFromFileSystem() {
        Path examplesDir = PathUtils.getJIPipeUserDir().resolve("templates");
        try {
            if (!Files.isDirectory(examplesDir))
                Files.createDirectories(examplesDir);
            Files.walk(examplesDir).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    if (UIUtils.EXTENSION_FILTER_JIP.accept(path.toFile()) || UIUtils.EXTENSION_FILTER_ZIP.accept(path.toFile())) {
                        try {
                            progressInfo.log("[Project templates] Importing template from " + path);
                            projectTemplateRegistry.register(path);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            progressInfo.log("Error while loading project template from " + path + ": " + e);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            progressInfo.log("Error while loading project templates from " + examplesDir + ": " + e);
        }
    }

    private void registerNodeExamplesFromFileSystem() {
        Path examplesDir = PathUtils.getJIPipeUserDir().resolve("examples");
        try {
            if (!Files.isDirectory(examplesDir))
                Files.createDirectories(examplesDir);
            Files.walk(examplesDir).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    if (UIUtils.EXTENSION_FILTER_JSON.accept(path.toFile())) {
                        try {
                            progressInfo.log("[Node examples] Importing node template list from " + path);
                            for (JIPipeNodeTemplate template : JsonUtils.getObjectMapper().readValue(path.toFile(), JIPipeNodeTemplate.List.class)) {
                                nodeRegistry.registerExample(template);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            progressInfo.log("Error while loading node examples from " + path + ": " + e);
                        }
                    }
                }
            });
        } catch (Throwable e) {
            e.printStackTrace();
            progressInfo.log("Error while loading node examples from " + examplesDir + ": " + e);
        }
    }

    private void validateParameterTypes(JIPipeRegistryIssues issues) {
        for (Map.Entry<String, JIPipeParameterTypeInfo> entry : parameterTypeRegistry.getRegisteredParameters().entrySet()) {
            try {
                entry.getValue().newInstance();
            } catch (Throwable t) {
                logService.warn("Parameter type '" + entry.getKey() + "' cannot be initialized.");
                issues.getErroneousParameterTypes().add(entry.getValue());
                t.printStackTrace();
            }
            try {
                Object o = entry.getValue().newInstance();
                entry.getValue().duplicate(o);
            } catch (Throwable t) {
                logService.warn("Parameter type '" + entry.getKey() + "' cannot be duplicated.");
                issues.getErroneousParameterTypes().add(entry.getValue());
                t.printStackTrace();
            }
        }
    }

    private void validateDataTypes(JIPipeRegistryIssues issues) {
        for (Class<? extends JIPipeData> dataType : datatypeRegistry.getRegisteredDataTypes().values()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(dataType);
            if (info.getStorageDocumentation() == null) {
                logService.warn("Data type '" + dataType + "' has no storage documentation.");
                issues.getErroneousDataTypes().add(dataType);
            }
            if (dataType.isInterface() || Modifier.isAbstract(dataType.getModifiers()))
                continue;
            // Check if we can find a method "import"
            try {
                Method method = dataType.getDeclaredMethod("importData", JIPipeReadDataStorage.class, JIPipeProgressInfo.class);
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("Import method is not static!");
                }
                if (!JIPipeData.class.isAssignableFrom(method.getReturnType())) {
                    throw new IllegalArgumentException("Import method does not return JIPipeData!");
                }
            } catch (NoClassDefFoundError | Exception e) {
                // Unregister node
                logService.warn("Data type '" + dataType + "' cannot be instantiated.");
                logService.warn("Ensure that a method static JIPipeData importData(Path, JIPipeProgressInfo) is present!");
                issues.getErroneousDataTypes().add(dataType);
                e.printStackTrace();
            }
        }
    }

    private void validateNodeTypes(JIPipeRegistryIssues issues) {
        for (JIPipeNodeInfo info : ImmutableList.copyOf(nodeRegistry.getRegisteredNodeInfos().values())) {
            try {
                // Test instantiation
                JIPipeGraphNode algorithm = info.newInstance();

                // Test parameters
                JIPipeParameterTree collection = new JIPipeParameterTree(algorithm);
                for (Map.Entry<String, JIPipeParameterAccess> entry : collection.getParameters().entrySet()) {
                    if (JIPipe.getParameterTypes().getInfoByFieldClass(entry.getValue().getFieldClass()) == null) {
                        progressInfo.log("[!] ERROR: Unregistered parameter found: " + entry.getValue().getFieldClass() + " @ "
                                + algorithm + " -> " + entry.getKey());
                        throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                                new UnspecifiedValidationReportContext(),
                                "A plugin is invalid!",
                                "Unregistered parameter found: " + entry.getValue().getFieldClass() + " @ "
                                        + algorithm + " -> " + entry.getKey(),
                                "There is an error in the plugin's code that makes it use an unsupported parameter type.",
                                "Please contact the plugin author for further help."));
                    }
                }

                // Test duplication
                try {
                    algorithm.duplicate();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new JIPipeValidationRuntimeException(e1,
                            "A plugin is invalid!",
                            "There is an error in the plugin's code that prevents the copying of a node.",
                            "Please contact the plugin author for further help.");
                }

                // Test serialization
                try {
                    JsonUtils.toJsonString(algorithm);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new JIPipeValidationRuntimeException(e1,
                            "A plugin is invalid!",
                            "There is an error in the plugin's code that prevents the saving of a node.",
                            "Please contact the plugin author for further help.");
                }

                // Test cache state generation
                try {
                    if (!algorithm.functionallyEquals(algorithm)) {
                        throw new RuntimeException("Node " + algorithm.getInfo().getId() + " is not functionally equal to itself!");
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new JIPipeValidationRuntimeException(e1,
                            "A plugin is invalid!",
                            "There is an error in the plugin's code that prevents the cache state generation of a node.",
                            "Please contact the plugin author for further help.");
                }

                logService.debug("OK: Algorithm '" + info.getId() + "'");
            } catch (NoClassDefFoundError | Exception e) {
                e.printStackTrace();
                // Unregister node
                logService.warn("Unregistering node with id '" + info.getId() + "' as it cannot be instantiated, duplicated, serialized, or cached.");
                nodeRegistry.unregister(info.getId());
                issues.getErroneousNodes().add(info);
            }
        }
    }

    public boolean isInitializing() {
        return initializing;
    }

    /**
     * Creates settings for each known data type, so users can change how they will be imported
     */
    private void createDefaultImporterSettings() {
        JIPipeDefaultResultImporterApplicationSettings settings = applicationSettingsRegistry.getById(JIPipeDefaultResultImporterApplicationSettings.ID, JIPipeDefaultResultImporterApplicationSettings.class);
        for (String id : datatypeRegistry.getRegisteredDataTypes().keySet()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(id);
            JIPipeMutableParameterAccess access = settings.addParameter(id, DynamicDataImportOperationIdEnumParameter.class);
            access.setName(info.getName());
            access.setDescription("Defines which importer is used by default when importing the selected data type.");
        }
    }

    /**
     * Creates settings for each known data type, so users can change how they will be imported
     */
    private void createDefaultCacheDisplaySettings() {
        JIPipeDefaultCacheDisplayApplicationSettings settings = applicationSettingsRegistry.getById(JIPipeDefaultCacheDisplayApplicationSettings.ID, JIPipeDefaultCacheDisplayApplicationSettings.class);
        for (String id : datatypeRegistry.getRegisteredDataTypes().keySet()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(id);
            JIPipeMutableParameterAccess access = settings.addParameter(id, DynamicDataDisplayOperationIdEnumParameter.class);
            access.setName(info.getName());
            access.setDescription("Defines which cache display method is used by default for the type.");
        }
    }

    private void updateDefaultImporterSettings() {
        JIPipeDefaultResultImporterApplicationSettings settings = applicationSettingsRegistry.getById(JIPipeDefaultResultImporterApplicationSettings.ID, JIPipeDefaultResultImporterApplicationSettings.class);
        for (String id : datatypeRegistry.getRegisteredDataTypes().keySet()) {
            List<JIPipeLegacyDataImportOperation> operations = datatypeRegistry.getSortedImportOperationsFor(id);
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

    private void updateDefaultCacheDisplaySettings() {
        JIPipeDefaultCacheDisplayApplicationSettings settings = applicationSettingsRegistry.getById(JIPipeDefaultCacheDisplayApplicationSettings.ID, JIPipeDefaultCacheDisplayApplicationSettings.class);
        for (String id : datatypeRegistry.getRegisteredDataTypes().keySet()) {
            List<JIPipeDesktopDataDisplayOperation> operations = datatypeRegistry.getSortedDisplayOperationsFor(id);
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

    private Document readXMLGZ(Path path) throws ParserConfigurationException, IOException, SAXException {
        try (InputStream inputStream = new GZIPInputStream(new FileInputStream(path.toFile()))) {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(inputStream);
            doc.getDocumentElement().normalize();
            return doc;
        }
    }

    /**
     * Checks the update sites of all extensions and stores the results in the issues
     *
     * @param issues          the results
     * @param extensions      list of known extensions
     * @param progressAdapter the adapter that takes the progress
     * @param progressInfo    the progress info
     */
    public void checkUpdateSites(JIPipeRegistryIssues issues, List<JIPipeDependency> extensions, Progress progressAdapter, JIPipeProgressInfo progressInfo) {
        Set<JIPipeImageJUpdateSiteDependency> dependencies = new HashSet<>();
        Set<JIPipeImageJUpdateSiteDependency> missingSites = new HashSet<>();
        for (JIPipeDependency extension : extensions) {
            if (extension == null)
                continue;
            dependencies.addAll(extension.getImageJUpdateSiteDependencies());
            missingSites.addAll(extension.getImageJUpdateSiteDependencies());
        }
        if (!dependencies.isEmpty()) {
            progressInfo.log("Following ImageJ update site dependencies were requested: ");
            for (JIPipeImageJUpdateSiteDependency dependency : dependencies) {
                progressInfo.log("  - " + dependency.getName() + " @ " + dependency.getUrl());
            }

            // Try to use the existing database
            Path dbPath = Paths.get(Prefs.getImageJDir()).resolve("db.xml.gz");
            boolean dbPathSuccess = false;
            if (Files.isRegularFile(dbPath)) {
                try {
                    Document document = readXMLGZ(dbPath);
                    NodeList activeSitesNodes = document.getElementsByTagName("update-site");
                    for (int i = 0; i < activeSitesNodes.getLength(); i++) {
                        String name = activeSitesNodes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                        missingSites.removeIf(site -> Objects.equals(site.getName(), name));
                    }
                    if (missingSites.isEmpty()) {
                        dbPathSuccess = true;
                    }
                } catch (Exception e) {
                    logService.warn("Unable to read " + dbPath);
                }
            }

            // Query update sites again (via ImageJ)
            if (!dbPathSuccess) {
                try {
                    UpdaterUtil.useSystemProxies();
                    Authenticator.setDefault(new SwingAuthenticator());

                    imageJPlugins = new FilesCollection(CoreImageJUtils.getImageJUpdaterRoot().toFile());
                    AvailableSites.initializeAndAddSites(imageJPlugins);
                    imageJPlugins.downloadIndexAndChecksum(progressAdapter);
                } catch (Exception e) {
                    logService.error("Unable to check update sites!");
                    e.printStackTrace();
                    missingSites.clear();
                    progressInfo.log("No ImageJ update site check is applied.");
                }
                if (imageJPlugins != null) {
                    progressInfo.log("Following ImageJ update sites are currently active: ");
                    for (UpdateSite updateSite : imageJPlugins.getUpdateSites(true)) {
                        if (updateSite.isActive()) {
                            progressInfo.log("  - " + updateSite.getName() + " @ " + updateSite.getURL());
                            missingSites.removeIf(site -> Objects.equals(site.getName(), updateSite.getName()));
                        }
                    }
                } else {
                    System.err.println("No update sites available! Skipping.");
                    missingSites.clear();
                }
            }
        }

        if (!missingSites.isEmpty()) {
            logService.warn("Following ImageJ update site dependencies are missing: ");
            for (JIPipeImageJUpdateSiteDependency dependency : missingSites) {
                logService.warn("  - " + dependency.getName() + " @ " + dependency.getUrl());
            }
        }

        issues.setMissingImageJSites(missingSites);
    }

    /**
     * Registers a JSON extension
     *
     * @param extension    The extension
     * @param progressInfo the progress info
     */
    public void register(JIPipeJsonPlugin extension, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Registering Json Extension " + extension.getDependencyId());
        extension.setRegistry(this);
        extension.register();
        registeredExtensions.add(extension);
        registeredExtensionIds.add(extension.getDependencyId());
        extensionRegisteredEventEmitter.emit(new ExtensionRegisteredEvent(this, extension));
    }

    @Override
    public JIPipePluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    @Override
    public JIPipeNodeRegistry getNodeRegistry() {
        return nodeRegistry;
    }

    @Override
    public JIPipeDatatypeRegistry getDatatypeRegistry() {
        return datatypeRegistry;
    }

    @Override
    public JIPipeImageJAdapterRegistry getImageJDataAdapterRegistry() {
        return imageJDataAdapterRegistry;
    }

    @Override
    public List<JIPipeDependency> getRegisteredExtensions() {
        return Collections.unmodifiableList(registeredExtensions);
    }

    @Override
    public JIPipeCustomMenuRegistry getCustomMenuRegistry() {
        return customMenuRegistry;
    }

    @Override
    public JIPipeNodeTemplateRegistry getNodeTemplateRegistry() {
        return nodeTemplateRegistry;
    }

    @Override
    public Set<String> getRegisteredExtensionIds() {
        return registeredExtensionIds;
    }

    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        report.report(reportContext, nodeRegistry);
        for (JIPipeDependency extension : failedExtensions) {
            if (extension != null) {
                report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                        new JavaExtensionValidationReportContext(extension),
                        "Error during loading the extension!",
                        "There was an error while loading the extension. Please refer to the message that you get on restarting JIPipe. Please refer to the message that you get on restarting JIPipe.",
                        null,
                        null));
            }
        }
        for (JIPipeDependency extension : registeredExtensions) {
            report.report(reportContext, extension);
        }
    }

    @Override
    public JIPipeDependency findExtensionById(String dependencyId) {
        return registeredExtensions.stream().filter(d -> Objects.equals(dependencyId, d.getDependencyId())).findFirst().orElse(null);
    }

    public PluginService getPluginService() {
        return pluginService;
    }

    @Override
    public JIPipeParameterTypeRegistry getParameterTypeRegistry() {
        return parameterTypeRegistry;
    }

    @Override
    public JIPipeApplicationSettingsRegistry getApplicationSettingsRegistry() {
        return applicationSettingsRegistry;
    }

    @Override
    public JIPipeProjectSettingsRegistry getProjectSettingsRegistry() {
        return projectSettingsRegistry;
    }

    @Override
    public JIPipeMetadataRegistry getMetadataRegistry() {
        return metadataRegistry;
    }

    @Override
    public JIPipeExpressionRegistry getExpressionRegistry() {
        return tableOperationRegistry;
    }

    public FilesCollection getImageJPlugins() {
        return imageJPlugins;
    }

    public LogService getLogService() {
        return logService;
    }

    @Override
    public JIPipeExternalEnvironmentRegistry getExternalEnvironmentRegistry() {
        return externalEnvironmentRegistry;
    }

    @Override
    public JIPipeUtilityRegistry getUtilityRegistry() {
        return utilityRegistry;
    }

    @Override
    public JIPipeArtifactsRegistry getArtifactsRegistry() {
        return artifactsRegistry;
    }

    @Override
    public JIPipeRecentProjectsRegistry getRecentProjectsRegistry() {
        return recentProjectsRegistry;
    }

    public JIPipeMode getMode() {
        return mode;
    }
}
