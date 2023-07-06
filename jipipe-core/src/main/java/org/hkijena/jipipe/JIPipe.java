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

package org.hkijena.jipipe;

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
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.data.storage.JIPipeReadDataStorage;
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.notifications.JIPipeNotification;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationAction;
import org.hkijena.jipipe.api.notifications.JIPipeNotificationInbox;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.extensions.nodetemplate.NodeTemplatesRefreshedEventEmitter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.settings.*;
import org.hkijena.jipipe.ui.JIPipeProjectWindow;
import org.hkijena.jipipe.ui.JIPipeProjectWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.ijupdater.JIPipeProgressAdapter;
import org.hkijena.jipipe.ui.registries.JIPipeCustomMenuRegistry;
import org.hkijena.jipipe.ui.running.JIPipeLogs;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
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
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.*;
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
import java.util.List;
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
    private final JIPipeSettingsRegistry settingsRegistry;
    private final JIPipeExpressionRegistry tableOperationRegistry;
    private final JIPipeUtilityRegistry utilityRegistry;
    private final JIPipeExternalEnvironmentRegistry externalEnvironmentRegistry;
    private final JIPipeExtensionRegistry extensionRegistry;

    private final JIPipeGraphEditorToolRegistry graphEditorToolRegistry;

    private final JIPipeProjectTemplateRegistry projectTemplateRegistry;
    private FilesCollection imageJPlugins = null;
    private boolean initializing = false;
    @Parameter
    private LogService logService;

    @Parameter
    private PluginService pluginService;

    private final DatatypeRegisteredEventEmitter datatypeRegisteredEventEmitter = new DatatypeRegisteredEventEmitter();

    private final ExtensionContentAddedEventEmitter extensionContentAddedEventEmitter = new ExtensionContentAddedEventEmitter();

    private final ExtensionContentRemovedEventEmitter extensionContentRemovedEventEmitter = new ExtensionContentRemovedEventEmitter();

    private final ExtensionDiscoveredEventEmitter extensionDiscoveredEventEmitter = new ExtensionDiscoveredEventEmitter();

    private final ExtensionRegisteredEventEmitter extensionRegisteredEventEmitter = new ExtensionRegisteredEventEmitter();

    private final NodeInfoRegisteredEventEmitter nodeInfoRegisteredEventEmitter = new NodeInfoRegisteredEventEmitter();

    private final NodeTemplatesRefreshedEventEmitter nodeTemplatesRefreshedEventEmitter = new NodeTemplatesRefreshedEventEmitter();

    public JIPipe() {
        nodeRegistry = new JIPipeNodeRegistry(this);
        datatypeRegistry = new JIPipeDatatypeRegistry(this);
        imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry(this);
        customMenuRegistry = new JIPipeCustomMenuRegistry(this);
        parameterTypeRegistry = new JIPipeParameterTypeRegistry(this);
        settingsRegistry = new JIPipeSettingsRegistry(this);
        tableOperationRegistry = new JIPipeExpressionRegistry(this);
        utilityRegistry = new JIPipeUtilityRegistry(this);
        externalEnvironmentRegistry = new JIPipeExternalEnvironmentRegistry(this);
        extensionRegistry = new JIPipeExtensionRegistry(this);
        projectTemplateRegistry = new JIPipeProjectTemplateRegistry(this);
        graphEditorToolRegistry = new JIPipeGraphEditorToolRegistry(this);
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

    public static JIPipeSettingsRegistry getSettings() {
        return instance.settingsRegistry;
    }

    public static JIPipeNodeRegistry getNodes() {
        return instance.nodeRegistry;
    }

    public static JIPipeDatatypeRegistry getDataTypes() {
        return instance.datatypeRegistry;
    }

    public NodeTemplatesRefreshedEventEmitter getNodeTemplatesRefreshedEventEmitter() {
        return nodeTemplatesRefreshedEventEmitter;
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

    /**
     * @return Singleton instance
     */
    public static JIPipe getInstance() {
        return instance;
    }

    public static void restartGUI() {

        // Save all settings first
        getSettings().save();

        try {
            IS_RESTARTING = true;
            // Kill all JIPipe windows
            for (JIPipeProjectWindow openWindow : JIPipeProjectWindow.getOpenWindows()) {
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
        JIPipe instance = JIPipe.createInstance(context);
        JIPipe.getInstance().initialize();
        return instance;
    }

    /**
     * Helper to create JIPipe from a context.
     * Will create a new JIPipe instance, so be careful.
     * We recommend using the ensureInstance() method.
     *
     * @param context the context
     */
    public static JIPipe createInstance(Context context) {
        PluginService pluginService = context.getService(PluginService.class);
        try {
            instance = (JIPipe) pluginService.getPlugin(JIPipe.class).createInstance();
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
     * @param fileName Project file
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName) throws IOException {
        return loadProject(fileName, new JIPipeIssueReport(), new JIPipeNotificationInbox());
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
        return loadProject(fileName, new JIPipeIssueReport(), notifications);
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
    public static JIPipeProject loadProject(Path fileName, JIPipeIssueReport report, JIPipeNotificationInbox notifications) throws IOException {
        return JIPipeProject.loadProject(fileName, report, notifications);
    }

    /**
     * Loads a project
     *
     * @param fileName Project file
     * @param report   Report whether the project is valid
     * @return the project
     * @throws IOException thrown if the file could not be read or the file is corrupt
     */
    public static JIPipeProject loadProject(Path fileName, JIPipeIssueReport report) throws IOException {
        return JIPipeProject.loadProject(fileName, report, new JIPipeNotificationInbox());
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
    public static JIPipeProjectRun runProject(JIPipeProject project, Path outputFolder, int threads) {
        JIPipeRunSettings settings = new JIPipeRunSettings();
        settings.setOutputPath(outputFolder);
        if (threads > 0)
            settings.setNumThreads(threads);
        JIPipeProjectRun run = new JIPipeProjectRun(project, settings);
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
    public static JIPipeProjectRun runProject(JIPipeProject project, JIPipeRunSettings settings) {
        JIPipeProjectRun run = new JIPipeProjectRun(project, settings);
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
     * @return the future result. You have to check the {@link JIPipeRunnerQueue} to see if the run is finished.
     */
    public static JIPipeProjectRun enqueueProject(JIPipeProject project, Path outputFolder, int threads) {
        JIPipeRunSettings settings = new JIPipeRunSettings();
        settings.setOutputPath(outputFolder);
        if (threads > 0)
            settings.setNumThreads(threads);
        JIPipeProjectRun run = new JIPipeProjectRun(project, settings);
        JIPipeRunnerQueue.getInstance().enqueue(run);
        return run;
    }

    /**
     * Runs a project in the current thread.
     * The progress will be put into the stdout
     *
     * @param project  the project
     * @param settings settings for the run
     * @return the future result. You have to check the {@link JIPipeRunnerQueue} to see if the run is finished.
     */
    public static JIPipeProjectRun enqueueProject(JIPipeProject project, JIPipeRunSettings settings) {
        JIPipeProjectRun run = new JIPipeProjectRun(project, settings);
        JIPipeRunnerQueue.getInstance().enqueue(run);
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
            throw new UserFriendlyRuntimeException(e, "Cannot create data instance!", "Undefined", "There is an error in the code that provides the annotation type.",
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
     * Initializes JIPipe. Uses the default extension settings and discards any detected issues.
     */
    public void initialize() {
        initialize(ExtensionSettings.getInstanceFromRaw(), new JIPipeRegistryIssues());
    }

    /**
     * Initializes JIPipe
     *
     * @param extensionSettings extension settings
     * @param issues            if no windows should be opened
     */
    public void initialize(ExtensionSettings extensionSettings, JIPipeRegistryIssues issues) {
        initializing = true;

        progressInfo.setProgress(0, 5);
        progressInfo.getStatusUpdatedEventEmitter().subscribeLambda((emitter, event) -> {
            logService.info(event.getMessage());
        });

        IJ.showStatus("Initializing JIPipe ...");
        nodeRegistry.installEvents();
        List<PluginInfo<JIPipeJavaExtension>> pluginList = pluginService.getPluginsOfType(JIPipeJavaExtension.class).stream()
                .sorted(JIPipe::comparePlugins).collect(Collectors.toList());
        List<JIPipeDependency> loadedJavaExtensions = new ArrayList<>();
        extensionRegistry.initialize(); // Init extension registry
        extensionRegistry.load();
        progressInfo.setProgress(1);
        progressInfo.log("Pre-initialization phase ...");

        // Creating instances of extensions
        List<JIPipeJavaExtension> allJavaExtensionInstances = new ArrayList<>();
        List<PluginInfo<JIPipeJavaExtension>> allJavaExtensionPluginInfos = new ArrayList<>();
        Map<String, JIPipeJavaExtension> allJavaExtensionsByID = new HashMap<>();

        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<JIPipeJavaExtension> info = pluginList.get(i);
            try {
                progressInfo.log("Creating instance of " + info + " ...");
                JIPipeJavaExtension extension = info.createInstance();
                allJavaExtensionInstances.add(extension);
                allJavaExtensionPluginInfos.add(info);

                // Validate ID
                if (!isValidExtensionId(extension.getDependencyId())) {
                    System.err.println("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension);
                    progressInfo.log("Invalid extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension);
                }
                else {
                    if(!allJavaExtensionsByID.containsKey(extension.getDependencyId())) {
                        allJavaExtensionsByID.put(extension.getDependencyId(), extension);
                    }
                    else {
                        System.err.println("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder");
                        progressInfo.log("Duplicate extension ID: " + extension.getDependencyId() + ". Please contact the developer of the extension " + extension + " or check your ImageJ folder");
                    }
                }

            } catch (Throwable e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(info);
            }
        }

        // First loading check
        progressInfo.log("Determining extensions to be loaded ...");
        Set<String> impliedLoadedJavaExtensions = new HashSet<>();
        boolean impliedLoadedJavaExtensionsChanged;
        do {
            impliedLoadedJavaExtensionsChanged = false;
            for (JIPipeJavaExtension extension : allJavaExtensionInstances) {
                if (extension.isCoreExtension() || extensionRegistry.getStartupExtensions().contains(extension.getDependencyId()) || impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                    if(isValidExtensionId(extension.getDependencyId())) {
                        if(!impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                            progressInfo.log("-> Core/User: " + extension.getDependencyId());
                            impliedLoadedJavaExtensions.add(extension.getDependencyId());
                            impliedLoadedJavaExtensionsChanged = true;
                        }
                    }
                    for (JIPipeDependency dependency : extension.getDependencies()) {
                        if(isValidExtensionId(dependency.getDependencyId())) {
                            if(!impliedLoadedJavaExtensions.contains(dependency.getDependencyId())) {
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

        for (int i = 0; i < allJavaExtensionInstances.size(); i++) {
            IJ.showProgress(i + 1, pluginList.size());
            JIPipeJavaExtension extension = allJavaExtensionInstances.get(i);
            try {
                extensionRegistry.registerKnownExtension(extension);

                // Check if the extension should be loaded
                if (!extension.isCoreExtension() && !extensionRegistry.getStartupExtensions().contains(extension.getDependencyId()) && !impliedLoadedJavaExtensions.contains(extension.getDependencyId())) {
                    progressInfo.log("Extension with ID " + extension.getDependencyId() + " will not be loaded (deactivated in extension manager)");
                    loadedJavaExtensions.add(null);
                    continue;
                }

                // Extension self-check
                JIPipeIssueReport preActivationIssues = new JIPipeIssueReport();
                issues.getPreActivationIssues().put(extension.getDependencyId(), preActivationIssues);
                if (!extension.canActivate(preActivationIssues, progressInfo.resolve("Pre-activation check").resolve(extension.getDependencyId()))) {
                    if (!extensionSettings.isIgnorePreActivationChecks()) {
                        preActivationIssues.resolve("Registry").reportIsInvalid("Extension '" + extension.getMetadata().getName() + "' refuses to activate!",
                                "The extension's pre-activation check failed. It will not be activated.",
                                "Please refer to the other items if available.",
                                extension);
                        progressInfo.log("Extension with ID " + extension.getDependencyId() + " will not be loaded (pre-activation check failed; extension refuses to activate)");
                        loadedJavaExtensions.add(null);
                        if(!StringUtils.isNullOrEmpty(extension.getDependencyId())) {
                            progressInfo.log("Extension with ID " + extension.getDependencyId() + " was removed from the list of activated extensions");
                            extensionRegistry.getSettings().getActivatedExtensions().remove(extension.getDependencyId());
                            preActivationScheduledSave = true;
                        }
                        continue;
                    } else {
                        progressInfo.log("Extension with ID " + extension.getDependencyId() + " indicated that its pre-activation checks failed. WILL BE LOADED ANYWAYS DUE TO APPLICATION SETTINGS!");
                    }
                }

                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }
                loadedJavaExtensions.add(extension);
                extensionDiscoveredEventEmitter.emit(new ExtensionDiscoveredEvent(this, extension));
            } catch (Throwable e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(allJavaExtensionPluginInfos.get(i));
            }
        }

        // Save extension settings
        if(preActivationScheduledSave) {
            extensionRegistry.save();
        }

        progressInfo.setProgress(2);
        JIPipeProgressInfo registerFeaturesProgress = progressInfo.resolveAndLog("Register features");
        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<JIPipeJavaExtension> info = pluginList.get(i);
            IJ.showProgress(i + 1, pluginList.size());
            registerFeaturesProgress.log("Registering plugin " + info);
            JIPipeJavaExtension extension = null;
            try {
                extension = (JIPipeJavaExtension) loadedJavaExtensions.get(i);

                if (extension == null) {
                    registerFeaturesProgress.log("Skipping (deactivated in extension manager or is refusing to activate)");
                    continue;
                }

                extension.register(this, getContext(), progressInfo.resolve(extension.getDependencyId()));
                registeredExtensions.add(extension);
                registeredExtensionIds.add(extension.getDependencyId());
                extensionRegisteredEventEmitter.emit(new ExtensionRegisteredEvent(this, extension));
            } catch (NoClassDefFoundError | Exception e) {
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
        progressInfo.log("Error-checking-phase ...");
        validateDataTypes(issues);
        if (extensionSettings.isValidateNodeTypes()) {
            validateNodeTypes(issues);
        }
        validateParameterTypes(issues);

        // Create dependency graph
        extensionRegistry.getDependencyGraph();

        // Check for update sites
        if (extensionSettings.isValidateImageJDependencies()) {
            JIPipeProgressInfo dependencyProgress = progressInfo.resolve("ImageJ dependencies").detachProgress();
            checkUpdateSites(issues, loadedJavaExtensions,
                    new JIPipeProgressAdapter(dependencyProgress), dependencyProgress);
        }

        // Create settings for default importers
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();
        registerNodeExamplesFromFileSystem();
        registerProjectTemplatesFromFileSystem();

        // Reload settings
        progressInfo.setProgress(4);
        progressInfo.log("Loading settings ...");
        settingsRegistry.reload();

        // Required as the reload deletes the allowed values
        updateDefaultImporterSettings();
        updateDefaultCacheDisplaySettings();

        // Postprocessing
        progressInfo.setProgress(5);
        JIPipeProgressInfo postprocessingProgress = progressInfo.resolveAndLog("Postprocessing");
        for (JIPipeDependency extension : registeredExtensions) {
            if (!failedExtensions.contains(extension) && extension instanceof JIPipeJavaExtension) {
                postprocessingProgress.log(extension.getDependencyId());
                ((JIPipeJavaExtension) extension).postprocess(postprocessingProgress);
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
        ProjectsSettings projectsSettings = ProjectsSettings.getInstance();
        List<Path> invalidRecentProjects = projectsSettings.getRecentProjects().stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
        if (!invalidRecentProjects.isEmpty()) {
            projectsSettings.getRecentProjects().removeAll(invalidRecentProjects);
        }

        // Check the backups
        progressInfo.setProgress(7);
        progressInfo.log("Checking backups ...");
        AutoSaveSettings autoSaveSettings = AutoSaveSettings.getInstance();
        List<Path> invalidBackups = new ArrayList<>();
        {
            long lastTime = System.currentTimeMillis();
            long maxDifference = Math.max(0, autoSaveSettings.getMaxBackupCheckTimeSeconds() * 1000);
            for (Path path : autoSaveSettings.getLastBackups()) {
                if(!Files.exists(path)) {
                    invalidBackups.add(path);
                }
                long currentTime = System.currentTimeMillis();
                long timeDifference = currentTime - lastTime;
                if(maxDifference > 0 && timeDifference > maxDifference) {

                    progressInfo.log("Backup checking was cancelled. The time of " + (timeDifference / 1000.0) + " exceeded " + autoSaveSettings.getMaxBackupCheckTimeSeconds() + "s");

                    // Create notification
                    JIPipeNotification notification = new JIPipeNotification("org.hkijena.jipipe.core:check-backup-max-time-exceeded");
                    notification.setHeading("Checking backups took very long");
                    notification.setDescription("Checking the backups took " + (timeDifference / 1000.0) + "s, which is higher than the limit of " + autoSaveSettings.getMaxBackupCheckTimeSeconds() + "s.\n\n" +
                            "You might want to clean your backups by cleaning duplicate backups. If this does not help, you can also open the backup directory manually.\n\n" +
                            "The limit can be changed by navigating to Project/Application settings/General/Backup/Maximum backup checking time (s)");
                    notification.getActions().add(new JIPipeNotificationAction("Ignore", "Ignores the message", UIUtils.getIconFromResources("actions/archive-remove.png"), workbench -> {}));
                    notification.getActions().add(new JIPipeNotificationAction("Remove duplicate backups", "Opens a tool to detect and remove duplicate backups", UIUtils.getIconFromResources("actions/clear-brush.png"),
                            workbench -> AutoSaveSettings.getInstance().removeDuplicateBackups(workbench)));
                    notification.getActions().add(new JIPipeNotificationAction("Open backup directory", "Opens the directory that contains the backups", UIUtils.getIconFromResources("actions/folder-open.png"),
                            workbench -> {
                                try {
                                    Desktop.getDesktop().open(autoSaveSettings.getCurrentSavePath().toFile());
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            }));
                    JIPipeNotificationInbox.getInstance().push(notification);
                    break;
                }
            }
        }

        autoSaveSettings.getLastBackups().stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
        if (!invalidBackups.isEmpty()) {
            autoSaveSettings.getLastBackups().removeAll(invalidBackups);
        }

        progressInfo.setProgress(8);
        progressInfo.log("JIPipe loading finished");
        initializing = false;

        // Check for new extensions
        extensionRegistry.findNewExtensions();
        for (String newExtension : extensionRegistry.getNewExtensions()) {
            progressInfo.log("New extension found: " + newExtension);
        }
        if (!extensionRegistry.getNewExtensions().isEmpty()) {
            JIPipeNotification notification = new JIPipeNotification("org.hkijena.jipipe.core:new-extension");
            notification.setHeading("New extensions available");
            String nameList = extensionRegistry.getNewExtensions().stream().map(id -> extensionRegistry.getKnownExtensionById(id).getMetadata().getName()).collect(Collectors.joining(", "));
            if (extensionRegistry.getNewExtensions().size() != 1) {
                notification.setDescription("There are " + extensionRegistry.getNewExtensions().size() + " new extensions available: " + nameList + ".\n" +
                        "You can ignore these or open the extension manager to activate the new extensions.\n\n" +
                        "For more information, please visit https://www.jipipe.org/installation/extensions/");
            } else {
                notification.setDescription("There is 1 new extension available: " + nameList + ".\n" +
                        "You can ignore these or open the extension manager to activate the new extensions.\n\n" +
                        "For more information, please visit https://www.jipipe.org/installation/extensions/");
            }
            notification.getActions().add(new JIPipeNotificationAction("Ignore", "Ignores the newly available extensions. You will not be warned again about them.", UIUtils.getIconFromResources("actions/archive-remove.png"), workbench -> {
                extensionRegistry.dismissNewExtensions();
            }));
            notification.getActions().add(new JIPipeNotificationAction("Open in extension manager", "Opens the extension manager. You will not be warned again about the new extensions.", UIUtils.getIconFromResources("actions/plugins.png"), workbench -> {
                extensionRegistry.dismissNewExtensions();
                workbench.getDocumentTabPane().selectSingletonTab(JIPipeProjectWorkbench.TAB_PLUGIN_MANAGER);
            }));
            JIPipeNotificationInbox.getInstance().push(notification);
        }


        // Push progress into log
        JIPipeLogs.getInstance().pushToLog(new JIPipeLogs.LogEntry("JIPipe initialization",
                LocalDateTime.now(),
                progressInfo.getLog().toString(),
                true));
    }

    private void registerProjectTemplatesFromFileSystem() {
        Path examplesDir = PathUtils.getImageJDir().resolve("jipipe").resolve("templates");
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
        Path examplesDir = PathUtils.getImageJDir().resolve("jipipe").resolve("examples");
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
                        throw new UserFriendlyRuntimeException("Unregistered parameter found: " + entry.getValue().getFieldClass() + " @ "
                                + algorithm + " -> " + entry.getKey(),
                                "A plugin is invalid!",
                                "JIPipe plugin checker",
                                "There is an error in the plugin's code that makes it use an unsupported parameter type.",
                                "Please contact the plugin author for further help.");
                    }
                }

                // Test duplication
                try {
                    algorithm.duplicate();
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new UserFriendlyRuntimeException(e1,
                            "A plugin is invalid!",
                            "JIPipe plugin checker",
                            "There is an error in the plugin's code that prevents the copying of a node.",
                            "Please contact the plugin author for further help.");
                }

                // Test serialization
                try {
                    JsonUtils.toJsonString(algorithm);
                } catch (Exception e1) {
                    e1.printStackTrace();
                    throw new UserFriendlyRuntimeException(e1,
                            "A plugin is invalid!",
                            "JIPipe plugin checker",
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
                    throw new UserFriendlyRuntimeException(e1,
                            "A plugin is invalid!",
                            "JIPipe plugin checker",
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
        DefaultResultImporterSettings settings = settingsRegistry.getSettings(DefaultResultImporterSettings.ID, DefaultResultImporterSettings.class);
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
        DefaultCacheDisplaySettings settings = settingsRegistry.getSettings(DefaultCacheDisplaySettings.ID, DefaultCacheDisplaySettings.class);
        for (String id : datatypeRegistry.getRegisteredDataTypes().keySet()) {
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(id);
            JIPipeMutableParameterAccess access = settings.addParameter(id, DynamicDataDisplayOperationIdEnumParameter.class);
            access.setName(info.getName());
            access.setDescription("Defines which cache display method is used by default for the type.");
        }
    }

    private void updateDefaultImporterSettings() {
        DefaultResultImporterSettings settings = settingsRegistry.getSettings(DefaultResultImporterSettings.ID, DefaultResultImporterSettings.class);
        for (String id : datatypeRegistry.getRegisteredDataTypes().keySet()) {
            List<JIPipeDataImportOperation> operations = datatypeRegistry.getSortedImportOperationsFor(id);
            JIPipeMutableParameterAccess access = (JIPipeMutableParameterAccess) settings.get(id);

            Object currentParameterValue = access.get(Object.class);
            DynamicDataImportOperationIdEnumParameter parameter;
            if (currentParameterValue instanceof DynamicDataImportOperationIdEnumParameter) {
                parameter = (DynamicDataImportOperationIdEnumParameter) currentParameterValue;
            } else {
                parameter = new DynamicDataImportOperationIdEnumParameter();
                parameter.setValue("jipipe:show");
            }

            for (JIPipeDataImportOperation operation : operations) {
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
        DefaultCacheDisplaySettings settings = settingsRegistry.getSettings(DefaultCacheDisplaySettings.ID, DefaultCacheDisplaySettings.class);
        for (String id : datatypeRegistry.getRegisteredDataTypes().keySet()) {
            List<JIPipeDataDisplayOperation> operations = datatypeRegistry.getSortedDisplayOperationsFor(id);
            JIPipeMutableParameterAccess access = (JIPipeMutableParameterAccess) settings.get(id);

            Object currentParameterValue = access.get(Object.class);
            DynamicDataDisplayOperationIdEnumParameter parameter;
            if (currentParameterValue instanceof DynamicDataDisplayOperationIdEnumParameter) {
                parameter = (DynamicDataDisplayOperationIdEnumParameter) currentParameterValue;
            } else {
                parameter = new DynamicDataDisplayOperationIdEnumParameter();
                parameter.setValue("jipipe:show");
            }

            for (JIPipeDataDisplayOperation operation : operations) {
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
    public void register(JIPipeJsonExtension extension, JIPipeProgressInfo progressInfo) {
        progressInfo.log("Registering Json Extension " + extension.getDependencyId());
        extension.setRegistry(this);
        extension.register();
        registeredExtensions.add(extension);
        registeredExtensionIds.add(extension.getDependencyId());
        extensionRegisteredEventEmitter.emit(new ExtensionRegisteredEvent(this, extension));
    }

    @Override
    public JIPipeExtensionRegistry getExtensionRegistry() {
        return extensionRegistry;
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
    public Set<String> getRegisteredExtensionIds() {
        return registeredExtensionIds;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        report.resolve("Nodes").report(nodeRegistry);
        for (JIPipeDependency extension : failedExtensions) {
            if (extension != null) {
                report.resolve("Extensions").resolve(extension.getDependencyId()).reportIsInvalid("Error during loading the extension!",
                        "There was an error while loading the extension. Please refer to the message that you get on restarting JIPipe.",
                        "Please refer to the message that you get on restarting JIPipe.",
                        failedExtensions);
            }
        }
        for (JIPipeDependency extension : registeredExtensions) {
            report.resolve("Extensions").resolve(extension.getDependencyId()).report(extension);
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
    public JIPipeSettingsRegistry getSettingsRegistry() {
        return settingsRegistry;
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
}
