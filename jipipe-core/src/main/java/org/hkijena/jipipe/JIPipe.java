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
import com.google.common.eventbus.EventBus;
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
import org.hkijena.jipipe.api.exceptions.UserFriendlyRuntimeException;
import org.hkijena.jipipe.api.nodes.JIPipeAlgorithm;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.registries.*;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataDisplayOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicDataImportOperationIdEnumParameter;
import org.hkijena.jipipe.extensions.settings.*;
import org.hkijena.jipipe.ui.ijupdater.IJProgressAdapter;
import org.hkijena.jipipe.ui.ijupdater.JIPipeImageJPluginManager;
import org.hkijena.jipipe.ui.registries.JIPipeCustomMenuRegistry;
import org.hkijena.jipipe.ui.running.JIPipeRunnerQueue;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * A scijava service that discovers JIPipe plugins in the classpath
 */
@Plugin(type = JIPipeRegistry.class)
public class JIPipe extends AbstractService implements JIPipeRegistry {
    private static JIPipe instance;
    private final EventBus eventBus = new EventBus();
    private final Set<String> registeredExtensionIds = new HashSet<>();
    private final List<JIPipeDependency> registeredExtensions = new ArrayList<>();
    private final List<JIPipeDependency> failedExtensions = new ArrayList<>();
    private final JIPipeNodeRegistry nodeRegistry = new JIPipeNodeRegistry();
    private final JIPipeDatatypeRegistry datatypeRegistry = new JIPipeDatatypeRegistry();
    private final JIPipeImageJAdapterRegistry imageJDataAdapterRegistry = new JIPipeImageJAdapterRegistry();
    private final JIPipeCustomMenuRegistry customMenuRegistry = new JIPipeCustomMenuRegistry();
    private final JIPipeParameterTypeRegistry parameterTypeRegistry = new JIPipeParameterTypeRegistry();
    private final JIPipeSettingsRegistry settingsRegistry = new JIPipeSettingsRegistry();
    private final JIPipeExpressionRegistry tableOperationRegistry = new JIPipeExpressionRegistry();
    private final JIPipeUtilityRegistry utilityRegistry = new JIPipeUtilityRegistry();
    private final JIPipeExternalEnvironmentRegistry externalEnvironmentRegistry = new JIPipeExternalEnvironmentRegistry();
    private FilesCollection imageJPlugins = null;
    private boolean initializing = false;

    @Parameter
    private LogService logService;

    @Parameter
    private PluginService pluginService;

    public JIPipe() {
    }

    /**
     * Imports data of given data type from its output folder.
     * Generally, the output folder should conform to the data type's saveTo() function without 'forceName' enabled
     *
     * @param <T>          the data type
     * @param outputFolder the folder that contains the data
     * @param klass        the data type
     * @param progressInfo the progress info
     * @return imported data
     */
    public static <T extends JIPipeData> T importData(Path outputFolder, Class<T> klass, JIPipeProgressInfo progressInfo) {
        try {
            Method method = klass.getDeclaredMethod("importFrom", Path.class, JIPipeProgressInfo.class);
            return (T) method.invoke(null, outputFolder, progressInfo);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Imports data from a slot folder
     *
     * @param slotFolder   a folder that contains data-table.json
     * @param progressInfo the progress info
     * @return the imported data slot
     */
    public static JIPipeDataSlot importDataSlot(Path slotFolder, JIPipeProgressInfo progressInfo) {
        return JIPipeDataSlot.loadFromStoragePath(slotFolder, progressInfo);
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

    /**
     * @return Singleton instance
     */
    public static JIPipe getInstance() {
        return instance;
    }

    /**
     * Ensures that JIPipe is initialized and available.
     * Creates a new {@link ImageJ} instance to obtain a {@link Context} if JIPipe is not initialized already.
     *
     * @return the {@link JIPipe} instance
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
     * @return the {@link JIPipe} instance
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
        return loadProject(fileName, new JIPipeIssueReport());
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
        return JIPipeProject.loadProject(fileName, report);
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
    public static JIPipeRun runProject(JIPipeProject project, Path outputFolder, int threads) {
        JIPipeRunSettings settings = new JIPipeRunSettings();
        settings.setOutputPath(outputFolder);
        if (threads > 0)
            settings.setNumThreads(threads);
        JIPipeRun run = new JIPipeRun(project, settings);
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
    public static JIPipeRun runProject(JIPipeProject project, JIPipeRunSettings settings) {
        JIPipeRun run = new JIPipeRun(project, settings);
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
    public static JIPipeRun enqueueProject(JIPipeProject project, Path outputFolder, int threads) {
        JIPipeRunSettings settings = new JIPipeRunSettings();
        settings.setOutputPath(outputFolder);
        if (threads > 0)
            settings.setNumThreads(threads);
        JIPipeRun run = new JIPipeRun(project, settings);
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
    public static JIPipeRun enqueueProject(JIPipeProject project, JIPipeRunSettings settings) {
        JIPipeRun run = new JIPipeRun(project, settings);
        JIPipeRunnerQueue.getInstance().enqueue(run);
        return run;
    }

    /**
     * Creates a new node instance from its id
     *
     * @param id    Algorithm ID
     * @param klass the node type
     * @param <T>   Algorithm class
     * @return Algorithm instance
     */
    public static <T extends JIPipeGraphNode> T createNode(String id, Class<T> klass) {
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
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        } else {
            return (T) node.getInfo().duplicate(node);
        }
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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            throw new UserFriendlyRuntimeException(e, "Cannot create data instance!", "Undefined", "There is an error in the code that provides the annotation type.",
                    "Please contact the author of the plugin that provides the annotation type " + klass);
        }
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
        IJ.showStatus("Initializing JIPipe ...");
        nodeRegistry.installEvents();
        List<PluginInfo<JIPipeJavaExtension>> pluginList = pluginService.getPluginsOfType(JIPipeJavaExtension.class).stream()
                .sorted(JIPipe::comparePlugins).collect(Collectors.toList());
        List<JIPipeDependency> javaExtensions = new ArrayList<>();
        logService.info("[1/3] Pre-initialization phase ...");
        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<JIPipeJavaExtension> info = pluginList.get(i);
            IJ.showProgress(i + 1, pluginList.size());
            try {
                JIPipeJavaExtension extension = info.createInstance();
                getContext().inject(extension);
                extension.setRegistry(this);
                if (extension instanceof AbstractService) {
                    ((AbstractService) extension).setContext(getContext());
                }
                javaExtensions.add(extension);
                eventBus.post(new ExtensionDiscoveredEvent(this, extension));
            } catch (NoClassDefFoundError | InstantiableException e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(info);
            }
        }

        logService.info("[2/3] Registration-phase ...");
        for (int i = 0; i < pluginList.size(); ++i) {
            PluginInfo<JIPipeJavaExtension> info = pluginList.get(i);
            IJ.showProgress(i + 1, pluginList.size());
            logService.info("JIPipe: Registering plugin " + info);
            JIPipeJavaExtension extension = null;
            try {
                extension = (JIPipeJavaExtension) javaExtensions.get(i);
                extension.register();
                registeredExtensions.add(extension);
                registeredExtensionIds.add(extension.getDependencyId());
                eventBus.post(new ExtensionRegisteredEvent(this, extension));
            } catch (NoClassDefFoundError | Exception e) {
                e.printStackTrace();
                issues.getErroneousPlugins().add(info);
                if (extension != null)
                    failedExtensions.add(extension);
            }
        }

        for (JIPipeNodeRegistrationTask task : nodeRegistry.getScheduledRegistrationTasks()) {
            logService.error("Could not register: " + task.toString());
        }

        // Check for errors
        logService.info("[3/3] Error-checking-phase ...");
        validateDataTypes(issues);
        if (extensionSettings.isValidateNodeTypes()) {
            validateNodeTypes(issues);
        }

        // Check for update sites
        if (extensionSettings.isValidateImageJDependencies())
            checkUpdateSites(issues, javaExtensions, new IJProgressAdapter());

        // Create settings for default importers
        createDefaultImporterSettings();
        createDefaultCacheDisplaySettings();

        // Reload settings
        logService.debug("Loading settings ...");
        settingsRegistry.reload();

        // Required as the reload deletes the allowed values
        updateDefaultImporterSettings();
        updateDefaultCacheDisplaySettings();

        // Postprocessing
        for (JIPipeDependency extension : registeredExtensions) {
            if (!failedExtensions.contains(extension) && extension instanceof JIPipeJavaExtension) {
                logService.info("Postprocess: " + extension.getDependencyId());
                ((JIPipeJavaExtension) extension).postprocess();
            }
        }

        // Check recent projects and backups
        logService.info("Checking recent projects ...");
        ProjectsSettings projectsSettings = ProjectsSettings.getInstance();
        List<Path> invalidRecentProjects = projectsSettings.getRecentProjects().stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
        if (!invalidRecentProjects.isEmpty()) {
            projectsSettings.getRecentProjects().removeAll(invalidRecentProjects);
        }

        // Check the backups
        logService.info("Checking backups ...");
        AutoSaveSettings autoSaveSettings = AutoSaveSettings.getInstance();
        List<Path> invalidBackups = autoSaveSettings.getLastSaves().stream().filter(path -> !Files.exists(path)).collect(Collectors.toList());
        if (!invalidBackups.isEmpty()) {
            autoSaveSettings.getLastSaves().removeAll(invalidBackups);
        }

        logService.info("JIPipe loading finished");
        initializing = false;
    }

    private void validateDataTypes(JIPipeRegistryIssues issues) {
        for (Class<? extends JIPipeData> dataType : datatypeRegistry.getRegisteredDataTypes().values()) {
            if (dataType.isInterface() || Modifier.isAbstract(dataType.getModifiers()))
                continue;
            // Check if we can find a method "import"
            try {
                Method method = dataType.getDeclaredMethod("importFrom", Path.class, JIPipeProgressInfo.class);
                if (!Modifier.isStatic(method.getModifiers())) {
                    throw new IllegalArgumentException("Import method is not static!");
                }
                if (!JIPipeData.class.isAssignableFrom(method.getReturnType())) {
                    throw new IllegalArgumentException("Import method does not return JIPipeData!");
                }
            } catch (NoClassDefFoundError | Exception e) {
                // Unregister node
                logService.warn("Data type '" + dataType + "' cannot be instantiated.");
                logService.warn("Ensure that a method static JIPipeData importFrom(Path, JIPipeProgressInfo) is present!");
                issues.getErroneousDataTypes().add(dataType);
                e.printStackTrace();
            }
            JIPipeDataInfo info = JIPipeDataInfo.getInstance(dataType);
            if (info.getStorageDocumentation() == null) {
                logService.warn("Data type '" + dataType + "' has no storage documentation.");
                issues.getErroneousDataTypes().add(dataType);
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
                    throw new UserFriendlyRuntimeException(e1,
                            "A plugin is invalid!",
                            "JIPipe plugin checker",
                            "There is an error in the plugin's code that prevents the saving of a node.",
                            "Please contact the plugin author for further help.");
                }

                // Test cache state generation
                try {
                    if (algorithm instanceof JIPipeAlgorithm) {
                        ((JIPipeAlgorithm) algorithm).getStateId();
                    }
                } catch (Exception e1) {
                    throw new UserFriendlyRuntimeException(e1,
                            "A plugin is invalid!",
                            "JIPipe plugin checker",
                            "There is an error in the plugin's code that prevents the cache state generation of a node.",
                            "Please contact the plugin author for further help.");
                }

                logService.debug("OK: Algorithm '" + info.getId() + "'");
            } catch (NoClassDefFoundError | Exception e) {
                // Unregister node
                logService.warn("Unregistering node with id '" + info.getId() + "' as it cannot be instantiated, duplicated, serialized, or cached.");
                nodeRegistry.unregister(info.getId());
                issues.getErroneousNodes().add(info);
                e.printStackTrace();
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
     */
    public void checkUpdateSites(JIPipeRegistryIssues issues, List<JIPipeDependency> extensions, Progress progressAdapter) {
        Set<JIPipeImageJUpdateSiteDependency> dependencies = new HashSet<>();
        Set<JIPipeImageJUpdateSiteDependency> missingSites = new HashSet<>();
        for (JIPipeDependency extension : extensions) {
            dependencies.addAll(extension.getImageJUpdateSiteDependencies());
            missingSites.addAll(extension.getImageJUpdateSiteDependencies());
        }
        if (!dependencies.isEmpty()) {
            logService.info("Following ImageJ update site dependencies were requested: ");
            for (JIPipeImageJUpdateSiteDependency dependency : dependencies) {
                logService.info("  - " + dependency.getName() + " @ " + dependency.getUrl());
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

                    imageJPlugins = new FilesCollection(JIPipeImageJPluginManager.getImageJRoot().toFile());
                    AvailableSites.initializeAndAddSites(imageJPlugins);
                    imageJPlugins.downloadIndexAndChecksum(progressAdapter);
                } catch (Exception e) {
                    logService.error("Unable to check update sites!");
                    e.printStackTrace();
                    missingSites.clear();
                    logService.info("No ImageJ update site check is applied.");
                }
                if (imageJPlugins != null) {
                    logService.info("Following ImageJ update sites are currently active: ");
                    for (UpdateSite updateSite : imageJPlugins.getUpdateSites(true)) {
                        if (updateSite.isActive()) {
                            logService.info("  - " + updateSite.getName() + " @ " + updateSite.getURL());
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
     * @param extension The extension
     */
    public void register(JIPipeJsonExtension extension) {
        logService.info("JIPipe: Registering Json Extension " + extension.getDependencyId());
        extension.setRegistry(this);
        extension.register();
        registeredExtensions.add(extension);
        registeredExtensionIds.add(extension.getDependencyId());
        eventBus.post(new ExtensionRegisteredEvent(this, extension));
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
    public EventBus getEventBus() {
        return eventBus;
    }

    @Override
    public Set<String> getRegisteredExtensionIds() {
        return registeredExtensionIds;
    }

    @Override
    public void reportValidity(JIPipeIssueReport report) {
        report.resolve("Algorithms").report(nodeRegistry);
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

    /**
     * Triggered when a new data type is registered
     */
    public static class DatatypeRegisteredEvent {
        private String id;

        /**
         * @param id the data type id
         */
        public DatatypeRegisteredEvent(String id) {
            this.id = id;
        }

        public String getId() {
            return id;
        }
    }

    /**
     * Generated when content is added to an {@link JIPipeJsonExtension}
     */
    public static class ExtensionContentAddedEvent {
        private JIPipeJsonExtension extension;
        private Object content;

        /**
         * @param extension event source
         * @param content   the new content
         */
        public ExtensionContentAddedEvent(JIPipeJsonExtension extension, Object content) {
            this.extension = extension;
            this.content = content;
        }

        public JIPipeJsonExtension getExtension() {
            return extension;
        }

        public Object getContent() {
            return content;
        }
    }

    /**
     * Generated when content is removed from an {@link JIPipeJsonExtension}
     */
    public static class ExtensionContentRemovedEvent {
        private JIPipeJsonExtension extension;
        private Object content;

        /**
         * @param extension event source
         * @param content   removed content
         */
        public ExtensionContentRemovedEvent(JIPipeJsonExtension extension, Object content) {
            this.extension = extension;
            this.content = content;
        }

        public JIPipeJsonExtension getExtension() {
            return extension;
        }

        public Object getContent() {
            return content;
        }
    }

    /**
     * Triggered when a new extension was discovered
     */
    public static class ExtensionDiscoveredEvent {
        private final JIPipe registry;
        private final JIPipeDependency extension;

        public ExtensionDiscoveredEvent(JIPipe registry, JIPipeDependency extension) {
            this.registry = registry;
            this.extension = extension;
        }

        public JIPipe getRegistry() {
            return registry;
        }

        public JIPipeDependency getExtension() {
            return extension;
        }
    }

    /**
     * Triggered by {@link JIPipeRegistry} when an extension is registered
     */
    public static class ExtensionRegisteredEvent {
        private JIPipeRegistry registry;
        private JIPipeDependency extension;

        /**
         * @param registry  event source
         * @param extension registered extension
         */
        public ExtensionRegisteredEvent(JIPipeRegistry registry, JIPipeDependency extension) {
            this.registry = registry;
            this.extension = extension;
        }

        public JIPipeRegistry getRegistry() {
            return registry;
        }

        public JIPipeDependency getExtension() {
            return extension;
        }
    }

    /**
     * Triggered when an algorithm is registered
     */
    public static class NodeInfoRegisteredEvent {
        private JIPipeNodeInfo nodeInfo;

        /**
         * @param nodeInfo the algorithm type
         */
        public NodeInfoRegisteredEvent(JIPipeNodeInfo nodeInfo) {
            this.nodeInfo = nodeInfo;
        }

        public JIPipeNodeInfo getNodeInfo() {
            return nodeInfo;
        }
    }
}
