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
import org.apache.commons.compress.utils.Sets;
import org.hkijena.jipipe.api.*;
import org.hkijena.jipipe.api.compat.ImageJDataExporter;
import org.hkijena.jipipe.api.compat.ImageJDataExporterUI;
import org.hkijena.jipipe.api.compat.ImageJDataImporter;
import org.hkijena.jipipe.api.compat.ImageJDataImporterUI;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.api.data.JIPipeLegacyDataImportOperation;
import org.hkijena.jipipe.api.data.JIPipeLegacyDataOperation;
import org.hkijena.jipipe.api.environments.JIPipeArtifactEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironment;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentArchetype;
import org.hkijena.jipipe.api.environments.JIPipeEnvironmentSetupTool;
import org.hkijena.jipipe.api.grapheditortool.JIPipeGraphEditorTool;
import org.hkijena.jipipe.api.metadata.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNode;
import org.hkijena.jipipe.api.nodes.annotation.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.api.nodes.infos.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeDefaultMutableParameterTypeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterArchetype;
import org.hkijena.jipipe.api.parameters.JIPipeParameterGenerator;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.project.JIPipeProjectMetadata;
import org.hkijena.jipipe.api.project.JIPipeProjectTemplate;
import org.hkijena.jipipe.api.service.JIPipeService;
import org.hkijena.jipipe.api.service.components.JIPipeParameterTypesServiceComponent;
import org.hkijena.jipipe.api.service.components.nodes.JIPipeJavaNodeRegistrationTask;
import org.hkijena.jipipe.api.service.components.nodes.JIPipeNodeRegistrationTask;
import org.hkijena.jipipe.api.settings.JIPipeApplicationSettingsSheet;
import org.hkijena.jipipe.api.settings.JIPipeProjectSettingsSheet;
import org.hkijena.jipipe.api.validation.JIPipeValidationReport;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportContext;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportSettings;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopGraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopMenuExtension;
import org.hkijena.jipipe.desktop.api.JIPipeDesktopParameterEditorUI;
import org.hkijena.jipipe.desktop.api.data.JIPipeDesktopDataDisplayOperation;
import org.hkijena.jipipe.desktop.api.dataviewer.JIPipeDesktopDataViewer;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.contextmenu.GraphInteractiveObjectUIContextAction;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultDataSlotPreview;
import org.hkijena.jipipe.desktop.app.resultanalysis.JIPipeDesktopResultDataSlotRowUI;
import org.hkijena.jipipe.desktop.commons.components.filechoosernext.JIPipeDesktopFileChooserNext;
import org.hkijena.jipipe.plugins.core.CorePlugin;
import org.hkijena.jipipe.plugins.expressions.ExpressionFunction;
import org.hkijena.jipipe.plugins.expressions.functions.ColumnOperationAdapterFunction;
import org.hkijena.jipipe.plugins.parameters.api.collections.JIPipeListParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameter;
import org.hkijena.jipipe.plugins.parameters.api.enums.JIPipeEnumParameterTypeInfo;
import org.hkijena.jipipe.plugins.parameters.api.optional.JIPipeOptionalParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameter;
import org.hkijena.jipipe.plugins.parameters.library.jipipe.PluginCategoriesEnumParameterList;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.parameters.tools.EnumParameterGenerator;
import org.hkijena.jipipe.plugins.tables.ColumnOperation;
import org.hkijena.jipipe.utils.*;
import org.hkijena.jipipe.utils.json.JsonUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.scijava.service.AbstractService;

import javax.swing.*;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link JIPipeJavaPlugin}
 */
public abstract class JIPipeDefaultJavaPlugin extends AbstractService implements JIPipeJavaPlugin {

    private final JIPipeStandardMetadata metadata;
    private final List<Runnable> postprocessingTasks = new ArrayList<>();
    private JIPipeService service;
    private boolean reachedPostprocessing;

    /**
     * Creates a new instance
     */
    public JIPipeDefaultJavaPlugin() {
        metadata = new JIPipeStandardMetadata();
        metadata.setName(getName());
        metadata.setDescription(getDescription());
        metadata.setSummary(getSummary());
        metadata.setAuthors(new JIPipeAuthorMetadata.List(getAuthors()));
        metadata.setDependencyCitations(new StringList(getDependencyCitations()));
        metadata.setCitation(getCitation());
        metadata.setLicense(getLicense());
        metadata.setWebsite(getWebsite());
        metadata.setAcknowledgements(getAcknowledgements());
        metadata.setCategories(getCategories());
    }

    /**
     * The categories of this extension (see {@link PluginCategoriesEnumParameter} for predefined values)
     *
     * @return the categories
     */
    public PluginCategoriesEnumParameterList getCategories() {
        PluginCategoriesEnumParameterList result = new PluginCategoriesEnumParameterList();
        if (isCorePlugin()) {
            result.add(new PluginCategoriesEnumParameter("Core"));
        }
        return result;
    }

    /**
     * Returns additionally cited authors
     *
     * @return the authors
     */
    public JIPipeAuthorMetadata.List getAcknowledgements() {
        return new JIPipeAuthorMetadata.List();
    }


    @Override
    public Set<JIPipeDependency> getDependencies() {
        return Sets.newHashSet(CorePlugin.AS_DEPENDENCY);
    }

    /**
     * Returns all dependent work
     *
     * @return dependent work
     */
    public abstract StringList getDependencyCitations();

    /**
     * @return The citation
     */
    public abstract String getCitation();

    @Override
    public JIPipeStandardMetadata getMetadata() {
        return metadata;
    }

    /**
     * @return The extension name
     */
    public abstract String getName();

    /**
     * @return The extension description
     */
    public abstract HTMLText getDescription();

    /**
     * A short summary
     *
     * @return a short summary
     */
    public HTMLText getSummary() {
        return getDescription();
    }

    /**
     * @return The extension authors
     */
    public abstract List<JIPipeAuthorMetadata> getAuthors();

    /**
     * @return The extension website
     */
    public abstract String getWebsite();

    /**
     * @return The extension license
     */
    public abstract String getLicense();


    @Override
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReportSettings reportSettings, JIPipeValidationReport report, JIPipeProgressInfo progressInfo) {

    }

    @Override
    public String getDependencyVersion() {
        return VersionUtils.getVersionString(getClass());
    }


    /**
     * Registers a custom menu entry.
     *
     * @param klass The menu entry
     */
    public void registerMenuExtension(Class<? extends JIPipeDesktopMenuExtension> klass) {
        service.getCustomMenuItems().registerMenu(klass);
    }

    /**
     * Registers a custom button for the graph editor toolbar
     *
     * @param klass the toolbar button class
     */
    public void registerGraphEditorToolBarButtonExtension(Class<? extends JIPipeDesktopGraphEditorToolBarButtonExtension> klass) {
        service.getCustomMenuItems().registerGraphEditorToolBarButton(klass);
    }

    /**
     * Registers a custom context menu action
     *
     * @param action the action
     */
    public void registerContextMenuAction(GraphInteractiveObjectUIContextAction action) {
        service.getCustomMenuItems().registerContextMenuAction(action);
    }

    /**
     * Registers a non-trivial conversion between data types.
     * Please note that JIPipe will prefer trivial conversions (using Java inheritance) to custom converters.
     *
     * @param converter the converter
     */
    public void registerDatatypeConversion(JIPipeDataConverter converter) {
        service.getDataTypes().registerConversion(converter);
    }

    /**
     * Registers a new annotation node type and associated {@link JIPipeGraphEditorTool}
     *
     * @param id             the ID of the node type (must be unique)
     * @param nodeClass      the node class
     * @param graphToolClass the graph editor class
     * @param icon           the icon URL
     * @param <T>            the node class
     */
    public <T extends JIPipeAnnotationGraphNode> void registerAnnotationNodeType(String id, Class<T> nodeClass, Class<? extends JIPipeAnnotationGraphNodeTool<T>> graphToolClass, URL icon) {
        registerNodeType(id, nodeClass, icon);
        registerGraphEditorTool(graphToolClass);
    }

    /**
     * Registers a new data type
     *
     * @param id         Data type id
     * @param dataClass  Data class
     * @param icon       Icon for the data type. Can be null.
     * @param rowUI      Results analyzer row UI for the data type. Can be null. If null, it will use the default row UI that manages {@link JIPipeLegacyDataImportOperation} instances.
     * @param cellUI     Results table cell UI. Can be null.
     * @param operations list of operations to register. passed to registerDatatypeOperation.
     */
    public void registerDatatype(String id, Class<? extends JIPipeData> dataClass, URL icon, Class<? extends JIPipeDesktopResultDataSlotRowUI> rowUI, Class<? extends JIPipeDesktopResultDataSlotPreview> cellUI, JIPipeLegacyDataOperation... operations) {
        service.getDataTypes().register(id, dataClass, this);
        if (icon != null) {
            service.getDataTypes().registerIcon(dataClass, icon);
        }
        if (rowUI != null) {
            service.getDataTypes().registerResultSlotUI(dataClass, rowUI);
        }
        if (cellUI != null) {
            service.getDataTypes().registerResultTableCellUI(dataClass, cellUI);
        }
        registerDatatypeOperation(id, operations);
    }

    /**
     * Registers a new data type
     *
     * @param id         Data type id
     * @param dataClass  Data class
     * @param icon       Icon for the data type. Can be null.
     * @param operations list of operations to register. passed to registerDatatypeOperation.
     */
    public void registerDatatype(String id, Class<? extends JIPipeData> dataClass, URL icon, JIPipeLegacyDataOperation... operations) {
        service.getDataTypes().register(id, dataClass, this);
        if (icon != null) {
            service.getDataTypes().registerIcon(dataClass, icon);
        }
        registerDatatypeOperation(id, operations);
    }

    /**
     * Shortcut for registering import and display operations.
     * If an instance is both, it is registered for both.
     *
     * @param dataTypeId the data type id. it is not required that the data type is registered, yet. If empty, the operations are applying to all data.
     * @param operations operations
     */
    public void registerDatatypeOperation(String dataTypeId, JIPipeLegacyDataOperation... operations) {
        for (JIPipeLegacyDataOperation operation : operations) {
            if (operation instanceof JIPipeLegacyDataImportOperation) {
                registerDatatypeImportOperation(dataTypeId, (JIPipeLegacyDataImportOperation) operation);
            }
            if (operation instanceof JIPipeDesktopDataDisplayOperation) {
                registerDatatypeDisplayOperation(dataTypeId, (JIPipeDesktopDataDisplayOperation) operation);
            }
        }
    }

    /**
     * Registers a default data viewer for the provided data class
     * Overrides any existing data viewer
     *
     * @param dataClass       the data class
     * @param dataViewerClass the viewer class
     */
    public void registerDefaultDataTypeViewer(Class<? extends JIPipeData> dataClass, Class<? extends JIPipeDesktopDataViewer> dataViewerClass) {
        service.getDataTypes().registerDefaultDataViewer(dataClass, dataViewerClass);
    }

    /**
     * Registers an import operation for the data type.
     * This is not used if the data type is assigned a non-default row UI
     *
     * @param dataTypeId the data type id. it is not required that the data type is registered, yet. If empty, the operations are applying to all data.
     * @param operation  the operation
     */
    public void registerDatatypeImportOperation(String dataTypeId, JIPipeLegacyDataImportOperation operation) {
        service.getDataTypes().registerImportOperation(dataTypeId, operation);
    }

    /**
     * Registers an additional non-default display operation for the data type. Used in the cache browser.
     *
     * @param dataTypeId the data type id. it is not required that the data type is registered, yet. If empty, the operations are applying to all data.
     * @param operation  the operation
     */
    public void registerDatatypeDisplayOperation(String dataTypeId, JIPipeDesktopDataDisplayOperation operation) {
        service.getDataTypes().registerDisplayOperation(dataTypeId, operation);
    }

    /**
     * Registers a node type category
     *
     * @param category the category
     */
    public void registerNodeTypeCategory(JIPipeNodeTypeCategory category) {
        service.getNodes().registerCategory(category);
    }

    /**
     * Registers a new node type. The {@link JIPipeNodeInfo} is generated as {@link JIPipeJavaNodeInfo}.
     *
     * @param id        Algorithm ID
     * @param nodeClass Algorithm class
     */
    public void registerNodeType(String id, Class<? extends JIPipeGraphNode> nodeClass) {
        registerNodeType(new JIPipeJavaNodeRegistrationTask(id, nodeClass, this, null));
    }

    /**
     * Registers a new node type. The {@link JIPipeNodeInfo} is generated as {@link JIPipeJavaNodeInfo}.
     *
     * @param id        Algorithm ID
     * @param nodeClass Algorithm class
     * @param icon      custom icon
     */
    public void registerNodeType(String id, Class<? extends JIPipeGraphNode> nodeClass, URL icon) {
        registerNodeType(new JIPipeJavaNodeRegistrationTask(id, nodeClass, this, icon));
    }

    /**
     * Registers a new node type. It is assumed that all dependencies are met.
     * If the dependency situation is unclear, register an {@link JIPipeNodeRegistrationTask} instead
     *
     * @param info Algorithm info
     */
    public void registerNodeType(JIPipeNodeInfo info) {
        service.getNodes().register(info, this);
    }

    /**
     * Registers a new node type. It is assumed that all dependencies are met.
     * If the dependency situation is unclear, register an {@link JIPipeNodeRegistrationTask} instead
     *
     * @param info Algorithm info
     * @param icon custom algorithm icon
     */
    public void registerNodeType(JIPipeNodeInfo info, URL icon) {
        service.getNodes().register(info, this);
        service.getNodes().registerIcon(info, icon);
    }

    /**
     * Registers a new node type with additional dependencies.
     * Actual registration happens when all dependencies are met.-
     *
     * @param task Algorithm registration task
     */
    public void registerNodeType(JIPipeNodeRegistrationTask task) {
        service.getNodes().scheduleRegister(task);
    }

    /**
     * Registers an {@link Enum} as parameter
     *
     * @param id             Unique ID of this parameter type
     * @param parameterClass Parameter class
     * @param name           Parameter class name
     * @param description    Description for the parameter type
     */
    public void registerEnumParameterType(String id, Class<? extends Enum<?>> parameterClass, String name, String description) {
        registerParameterType(new JIPipeEnumParameterTypeInfo(id, parameterClass, name, description), null);
        registerParameterGenerator(parameterClass, new EnumParameterGenerator());
    }

    /**
     * Registers a new parameter type and respective editors
     *
     * @param id                   Unique ID of this parameter type
     * @param parameterClass       Parameter class
     * @param archetype            the archetype
     * @param newInstanceGenerator Function that creates a new instance
     * @param duplicateFunction    Function that copies an existing instance
     * @param name                 Parameter class name
     * @param description          Description for the parameter type
     * @param uiClass              Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(String id, Class<?> parameterClass, JIPipeParameterArchetype archetype, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description, Class<? extends JIPipeDesktopParameterEditorUI> uiClass) {
        JIPipeDefaultMutableParameterTypeInfo info = new JIPipeDefaultMutableParameterTypeInfo(id,
                parameterClass,
                newInstanceGenerator != null ? newInstanceGenerator : () -> ReflectionUtils.newInstance(parameterClass),
                duplicateFunction != null ? duplicateFunction : o -> ReflectionUtils.newInstance(parameterClass, o),
                name,
                description,
                archetype);
        if (JIPipeEnumParameter.class.isAssignableFrom(parameterClass)) {
            try {
                JIPipeEnumParameter parameter = (JIPipeEnumParameter) parameterClass.newInstance();
                info.setAllowedValues(parameter.getAllowedValueInfos());
            } catch (Exception e) {
                JIPipe.getInstance().getProgressInfo().log(e);
            }
        }
        registerParameterType(info, uiClass);
    }

    /**
     * Registers a new parameter type that already has an existing editor.
     * Must have a default constructor and a deep copy constructor
     *
     * @param id             Unique ID of this parameter type
     * @param parameterClass Parameter class
     * @param archetype      the archetype
     * @param name           Parameter class name
     * @param description    Description for the parameter type
     */
    public void registerParameterType(String id, Class<?> parameterClass, JIPipeParameterArchetype archetype, String name, String description) {
        registerParameterType(id, parameterClass, archetype, null, null, name, description, null);
    }

    /**
     * Registers a new parameter type.
     * Must have a default constructor and a deep copy constructor
     *
     * @param id             Unique ID of this parameter type
     * @param parameterClass Parameter class
     * @param archetype      the archetype
     * @param name           Parameter class name
     * @param description    Description for the parameter type
     * @param uiClass        Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(String id, Class<?> parameterClass, JIPipeParameterArchetype archetype, String name, String description, Class<? extends JIPipeDesktopParameterEditorUI> uiClass) {
        registerParameterType(id, parameterClass, archetype, null, null, name, description, uiClass);
    }

    /**
     * Registers a new parameter type and respective editors
     *
     * @param <T>                  parameter class
     * @param id                   Unique ID of this parameter type
     * @param parameterClass       Parameter class
     * @param archetype            the archetype
     * @param listClass            Optional list class. If not null this creates a registration entry for the equivalent list entry
     * @param newInstanceGenerator Function that creates a new instance. If null, the function calls the default constructor
     * @param duplicateFunction    Function that copies an existing instance. If null, the function calls the copy constructor
     * @param name                 Parameter class name
     * @param description          Description for the parameter type
     * @param uiClass              Parameter editor UI. Can be null if the editor is already provided.
     */
    public <T> void registerParameterType(String id, Class<T> parameterClass, JIPipeParameterArchetype archetype, Class<? extends JIPipeListParameter<T>> listClass, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description, Class<? extends JIPipeDesktopParameterEditorUI> uiClass) {
        JIPipeDefaultMutableParameterTypeInfo info = new JIPipeDefaultMutableParameterTypeInfo(id,
                parameterClass,
                newInstanceGenerator != null ? newInstanceGenerator : () -> ReflectionUtils.newInstance(parameterClass),
                duplicateFunction != null ? duplicateFunction : o -> ReflectionUtils.newInstance(parameterClass, o),
                name,
                description, archetype);
        if (JIPipeEnumParameter.class.isAssignableFrom(parameterClass)) {
            try {
                JIPipeEnumParameter parameter = (JIPipeEnumParameter) parameterClass.newInstance();
                info.setAllowedValues(parameter.getAllowedValueInfos());
            } catch (Exception e) {
                JIPipe.getInstance().getProgressInfo().log(e);
            }
        }
        registerParameterType(info, uiClass);
        if (listClass != null) {
            registerParameterType(id + "-list", listClass, JIPipeParameterArchetype.List, () -> ReflectionUtils.newInstance(listClass),
                    o -> ReflectionUtils.newInstance(listClass, o),
                    "List of " + name,
                    description,
                    null);
        }
    }

    /**
     * Registers a new parameter type and respective editor
     *
     * @param info    the info
     * @param uiClass Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(JIPipeParameterTypeInfo info, Class<? extends JIPipeDesktopParameterEditorUI> uiClass) {
        service.getParameterTypes().register(info);
        if (uiClass != null) {
            registerParameterEditor(info.getFieldClass(), uiClass);
        }
    }

    /**
     * Registers a new parameter type and respective editor
     *
     * @param info      the info
     * @param listClass a parameter type that is a list of the registered type
     * @param uiClass   Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(JIPipeParameterTypeInfo info, Class<?> listClass, Class<? extends JIPipeDesktopParameterEditorUI> uiClass) {
        service.getParameterTypes().register(info);
        if (uiClass != null) {
            registerParameterEditor(info.getFieldClass(), uiClass);
        }
        if (listClass != null) {
            registerParameterType(info.getId() + "-list", listClass, JIPipeParameterArchetype.List, () -> ReflectionUtils.newInstance(listClass),
                    o -> ReflectionUtils.newInstance(listClass, o),
                    "List of " + info.getName(),
                    info.getDescription(),
                    null);
        }
    }

    /**
     * Registers an editor for any parameter type that inherits from the provided parameter class.
     * Please use this with caution, as unregistered parameters are rejected by components that require a unique
     * parameter type ID (for example {@link org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection})
     *
     * @param parameterClass the parameter class
     * @param uiClass        the editor class
     */
    public void registerParameterEditor(Class<?> parameterClass, Class<? extends JIPipeDesktopParameterEditorUI> uiClass) {
        service.getParameterTypes().registerParameterEditor(parameterClass, uiClass);
    }

    /**
     * Registers a UI that can generate parameters
     *
     * @param parameterClass Parameter class
     * @param generator      The generator object
     */
    public void registerParameterGenerator(Class<?> parameterClass, JIPipeParameterGenerator generator) {
        JIPipeParameterTypesServiceComponent parametertypeRegistry = service.getParameterTypes();
        parametertypeRegistry.registerGenerator(parameterClass, generator);
    }

    /**
     * Registers a table column operation. Those operations are used in various places that handle tabular data
     *
     * @param id          operation id
     * @param operation   the operation instance
     * @param name        the name
     * @param shortName   a short name (like min, max, avg, ...)
     * @param description a description
     */
    public void registerTableColumnOperation(String id, ColumnOperation operation, String name, String shortName, String description) {
        service.getExpressionFunctions().registerColumnOperation(id, operation, name, shortName, description);
    }

    /**
     * Registers a table column operation. Those operations are used in various places that handle tabular data.
     * Also registers an expression function that is generated via an adapter class (its ID will be the upper-case of the the short name with spaces replaced by underscores)
     *
     * @param id          operation id
     * @param operation   the operation instance
     * @param name        the name
     * @param shortName   a short name (like min, max, avg, ...)
     * @param description a description
     */
    public void registerTableColumnOperationAndExpressionFunction(String id, ColumnOperation operation, String name, String shortName, String description) {
        service.getExpressionFunctions().registerColumnOperation(id, operation, name, shortName, description);
        registerExpressionFunction(new ColumnOperationAdapterFunction(operation, shortName.toUpperCase(Locale.ROOT).replace(' ', '_')), name, description);
    }

    /**
     * Registers a function that can be used within expressions.
     *
     * @param function    the function. its internal name property must be unique
     * @param name        human-readable name
     * @param description the description
     */
    public void registerExpressionFunction(ExpressionFunction function, String name, String description) {
        service.getExpressionFunctions().registerExpressionFunction(function, name, description);
    }

    /**
     * Registers a function that can be used within expressions. The name and description are taken from {@link SetJIPipeDocumentation} annotations.
     *
     * @param function the function. its internal name property must be unique
     */
    public void registerExpressionFunction(ExpressionFunction function) {
        SetJIPipeDocumentation documentation = function.getClass().getAnnotation(SetJIPipeDocumentation.class);
        service.getExpressionFunctions().registerExpressionFunction(function, documentation.name(),
                DocumentationUtils.getDocumentationDescription(documentation));
    }

    /**
     * Sets the default ImageJ adapters
     *
     * @param dataClass         the data type
     * @param defaultImporterId the ID of the importer. the importer must be registered. can be null (ignore value)
     * @param defaultExporterId the ID of the exporter. the exporter must be registered. can be null (ignore value)
     */
    public void configureDefaultImageJAdapters(Class<? extends JIPipeData> dataClass, String defaultImporterId, String defaultExporterId) {
        if (!StringUtils.isNullOrEmpty(defaultImporterId))
            service.getImageJDataAdapters().setDefaultImporterFor(dataClass, defaultImporterId);
        if (!StringUtils.isNullOrEmpty(defaultExporterId))
            service.getImageJDataAdapters().setDefaultExporterFor(dataClass, defaultExporterId);
    }

    /**
     * Sets the default ImageJ adapters
     *
     * @param dataClass       the data type
     * @param defaultImporter the importer. the importer must be registered. can be null (ignore value)
     * @param defaultExporter the exporter. the exporter must be registered. can be null (ignore value)
     */
    public void configureDefaultImageJAdapters(Class<? extends JIPipeData> dataClass, ImageJDataImporter defaultImporter, ImageJDataExporter defaultExporter) {
        if (defaultImporter != null)
            service.getImageJDataAdapters().setDefaultImporterFor(dataClass, service.getImageJDataAdapters().getIdOf(defaultImporter));
        if (defaultExporter != null)
            service.getImageJDataAdapters().setDefaultExporterFor(dataClass, service.getImageJDataAdapters().getIdOf(defaultExporter));
    }

    /**
     * Registers an importer for data from ImageJ
     *
     * @param id              the unique ID
     * @param dataImporter    the importer instance
     * @param importerUIClass the UI (can be null to fall back to {@link org.hkijena.jipipe.api.compat.DefaultImageJDataImporterUI})
     */
    public void registerImageJDataImporter(String id, ImageJDataImporter dataImporter, Class<? extends ImageJDataImporterUI> importerUIClass) {
        service.getImageJDataAdapters().register(id, dataImporter, importerUIClass);
    }

    /**
     * Registers an importer for data from ImageJ
     *
     * @param id           the unique ID
     * @param dataExporter the exporter instance
     * @param uiClass      the UI (can be null to fall back to {@link org.hkijena.jipipe.api.compat.DefaultImageJDataExporterUI})
     */
    public void registerImageJDataExporter(String id, ImageJDataExporter dataExporter, Class<? extends ImageJDataExporterUI> uiClass) {
        service.getImageJDataAdapters().register(id, dataExporter, uiClass);
    }

    /**
     * Registers a new application settings sheet
     *
     * @param sheet the settings sheet
     */
    public void registerApplicationSettingsSheet(JIPipeApplicationSettingsSheet sheet) {
        service.getApplicationSettings().register(sheet);
    }

    /**
     * Registers a new project settings sheet
     *
     * @param settingsSheetClass the settings sheet class. must have a default constructor.
     */
    public void registerProjectSettingsSheet(Class<? extends JIPipeProjectSettingsSheet> settingsSheetClass) {
        service.getProjectSettings().register(settingsSheetClass);
    }

    /**
     * Registers an arbitrary utility associated to a category class.
     * There can be multiple utilities per category
     * The exact type of utility class depends on the utility implementation
     *
     * @param categoryClass the category class
     * @param utilityClass  the utility class
     */
    public void registerUtility(Class<?> categoryClass, Class<?> utilityClass) {
        service.getUtilityClasses().register(categoryClass, utilityClass);
    }

    /**
     * Registers a new metadata object type, which are JSON-serializable objects with a defined type identifier string
     *
     * @param objectClass    the object class
     * @param id             the ID of the object class
     * @param alternativeIds alternative Ids that are captured during the reading process
     */
    public void registerMetadataObjectType(Class<? extends JIPipeMetadataObject> objectClass, String id, String... alternativeIds) {
        service.getMetadataTypes().register(objectClass, id, alternativeIds);
    }

    /**
     * Registers a node template as example for a node.
     * Silently fails if the template does not contain exactly one node
     *
     * @param template the template
     */
    public void registerNodeExample(JIPipeNodeTemplate template) {
        service.getNodes().scheduleRegisterExample(template);
    }

    /**
     * Registers node examples from plugin resources via a {@link JIPipeResourceManager}.
     * Will detect *.json files and attempt to load them (fails silently)
     *
     * @param resourceManager the resource manager
     * @param subDirectory    the directory within the resource manager's base path
     */
    public void registerNodeExamplesFromResources(JIPipeResourceManager resourceManager, String subDirectory) {
        registerNodeExamplesFromResources(resourceManager.getResourceClass(), JIPipeResourceManager.formatBasePath(resourceManager.getBasePath() + "/" + subDirectory));
    }

    /**
     * Registers a node as node example based on a node instance
     * The configurator function is run prior to the registration
     * Can be run in postprocess() or during registration (in this case the method is postponed until postprocessing)
     *
     * @param nodeClass    the node class
     * @param name         the name of the example
     * @param configurator executed on an instance of the node
     * @param <T>          the node type
     */
    public <T extends JIPipeGraphNode> void registerNodeExample(Class<T> nodeClass, String name, Consumer<T> configurator) {
        Runnable task = () -> {
            JIPipeGraph graph = new JIPipeGraph();
            T node = JIPipe.createNode(nodeClass);
            configurator.accept(node);
            graph.insertNode(node);
            JIPipeNodeTemplate template = new JIPipeNodeTemplate();
            template.setName(name);
            template.setData(JsonUtils.toJsonString(graph));
            registerNodeExample(template);
        };
        if (reachedPostprocessing) {
            task.run();
        } else {
            postprocessingTasks.add(task);
        }
    }

    /**
     * Registers a node example based on the node instance
     * The name of the example is taken from the node custom name
     * Must be run within postprocess()
     *
     * @param node the example node
     */
    public void registerNodeExample(JIPipeGraphNode node) {
        JIPipeGraph graph = new JIPipeGraph();
        graph.insertNode(node.duplicate());
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();
        template.setName(node.getName());
        template.setData(JsonUtils.toJsonString(graph));
        registerNodeExample(template);
    }

    /**
     * Registers node examples from plugin resources.
     * Will detect *.json files and attempt to load them (fails silently)
     *
     * @param resourceClass the resource class
     * @param directory     the directory within the resources
     */
    public void registerNodeExamplesFromResources(Class<?> resourceClass, String directory) {
        JIPipe.getInstance().getProgressInfo().log("Scanning for node examples within " + resourceClass + " -> " + directory);
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(resourceClass))
                .setScanners(new ResourcesScanner()));

        Set<String> jsonResources = reflections.getResources(Pattern.compile(".*\\.json"));
        jsonResources = jsonResources.stream().map(s -> {
            if (!s.startsWith("/"))
                return "/" + s;
            else
                return s;
        }).collect(Collectors.toSet());
        for (String resource : jsonResources) {
            if (resource.startsWith(directory)) {
                JIPipe.getInstance().getProgressInfo().log("Loading node example list " + resource);
                try {
                    try (InputStream stream = resourceClass.getResourceAsStream(resource)) {
                        JIPipeNodeTemplateList templates = JsonUtils.getObjectMapper().readerFor(JIPipeNodeTemplateList.class).readValue(stream);
                        for (JIPipeNodeTemplate template : templates) {
                            registerNodeExample(template);
                        }
                    }
                } catch (Throwable throwable) {
                    JIPipe.getInstance().getProgressInfo().log("Error: " + throwable + " @ " + resource);
                }
            }
        }
    }

    /**
     * Registers a node template
     *
     * @param template the template
     */
    public void registerNodeTemplate(JIPipeNodeTemplate template) {
        service.getNodes().scheduleRegisterTemplate(template);
    }

    /**
     * Registers node templates from plugin resources via a {@link JIPipeResourceManager}.
     * Will detect *.json files and attempt to load them (fails silently)
     *
     * @param resourceManager the resource manager
     * @param subDirectory    the directory within the resource manager's base path
     */
    public void registerNodeTemplatesFromResources(JIPipeResourceManager resourceManager, String subDirectory) {
        registerNodeTemplatesFromResources(resourceManager.getResourceClass(), JIPipeResourceManager.formatBasePath(resourceManager.getBasePath() + "/" + subDirectory));
    }

    /**
     * Registers node examples from plugin resources.
     * Will detect *.json files and attempt to load them (fails silently)
     *
     * @param resourceClass the resource class
     * @param directory     the directory within the resources
     */
    public void registerNodeTemplatesFromResources(Class<?> resourceClass, String directory) {
        JIPipe.getInstance().getProgressInfo().log("Scanning for node templates within " + resourceClass + " -> " + directory);
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(resourceClass))
                .setScanners(new ResourcesScanner()));

        Set<String> jsonResources = reflections.getResources(Pattern.compile(".*\\.json"));
        jsonResources = jsonResources.stream().map(s -> {
            if (!s.startsWith("/"))
                return "/" + s;
            else
                return s;
        }).collect(Collectors.toSet());
        for (String resource : jsonResources) {
            if (resource.startsWith(directory)) {
                JIPipe.getInstance().getProgressInfo().log("Loading node template list " + resource);
                try {
                    try (InputStream stream = resourceClass.getResourceAsStream(resource)) {
                        JIPipeNodeTemplateList templates = JsonUtils.getObjectMapper().readerFor(JIPipeNodeTemplateList.class).readValue(stream);
                        for (JIPipeNodeTemplate template : templates) {
                            registerNodeTemplate(template);
                        }
                    }
                } catch (Throwable throwable) {
                    JIPipe.getInstance().getProgressInfo().log("Error: " + throwable + " @ " + resource);
                }
            }
        }
    }

    /**
     * Registers project templates from plugin resources via a {@link JIPipeResourceManager}.
     * Will detect *.jip files and attempt to load them (fails silently)
     *
     * @param resourceManager the resource manager
     * @param subDirectory    the directory within the resource manager's base path
     */
    public void registerProjectTemplatesFromResources(JIPipeResourceManager resourceManager, String subDirectory) {
        registerProjectTemplatesFromResources(resourceManager.getResourceClass(), JIPipeResourceManager.formatBasePath(resourceManager.getBasePath() + "/" + subDirectory));
    }

    /**
     * Registers project templates from plugin resources
     * Will detect *.jip files and attempt to load them (fails silently)
     *
     * @param resourceClass the resource class
     * @param directory     the directory within the resources
     */
    public void registerProjectTemplatesFromResources(Class<?> resourceClass, String directory) {
        JIPipe.getInstance().getProgressInfo().log("Scanning for project templates within " + resourceClass + " -> " + directory);
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(ClasspathHelper.forClass(resourceClass))
                .setScanners(new ResourcesScanner()));

        Set<String> jsonResources = reflections.getResources(Pattern.compile(".*\\.jip"));
        jsonResources = jsonResources.stream().map(s -> {
            if (!s.startsWith("/"))
                return "/" + s;
            else
                return s;
        }).collect(Collectors.toSet());
        for (String resource : jsonResources) {
            if (resource.startsWith(directory)) {
                JIPipe.getInstance().getProgressInfo().log("Loading project template " + resource);
                try {
                    try (InputStream stream = resourceClass.getResourceAsStream(resource)) {
                        String id = "resource:/" + getDependencyId() + "/" + resource;
                        JsonNode node = JsonUtils.getObjectMapper().readerFor(JsonNode.class).readValue(stream);
                        JIPipeProjectMetadata templateMetadata = JsonUtils.getObjectMapper().readerFor(JIPipeProjectMetadata.class).readValue(node.get("metadata"));
                        JIPipeProjectTemplate template = new JIPipeProjectTemplate(id, node, templateMetadata, null, null);
                        service.getProjectTemplates().register(template);
                    }
                } catch (Throwable throwable) {
                    JIPipe.getInstance().getProgressInfo().log("Error: " + throwable + " @ " + resource);
                }
            }
        }
    }

    public void registerGraphEditorTool(Class<? extends JIPipeGraphEditorTool> klass) {
        service.getGraphEditorTools().register(klass);
    }

    /**
     * Registers a new environment type.
     * This will also register the environment class and environment list class a valid parameters.
     * Requires that the environment class has a default constructor and a deep copy constructor.
     * Will also register a settings page for the environment
     *
     * @param <T>                      environment class
     * @param <U>                      list of environment class
     * @param id                       the ID of the environment class. Will be used as parameter type ID
     * @param artifactQuery            the artifact query for this environment. can be null. typically is [group].[name]:*, for example "com.github.mouseland.cellpose:*"
     * @param archetype                the environment's archetype
     * @param environmentClass         the environment class. Must be JSON-serializable. Will be registered as parameter type
     * @param optionalEnvironmentClass optional environment class. Must be JSON-serializable. Will be registered as parameter type optional-[id].
     * @param environmentListClass     the list. Will be registered as parameter type with ID [id]-list
     * @param name                     the name of the environment
     * @param description              the description of the environment
     */
    public <T extends JIPipeArtifactEnvironment, U extends JIPipeListParameter<T>, V extends JIPipeOptionalParameter<T>> void registerArtifactEnvironment(String id,
                                                                                                                                                          String artifactQuery,
                                                                                                                                                          JIPipeEnvironmentArchetype archetype, Class<T> environmentClass,
                                                                                                                                                          Class<V> optionalEnvironmentClass,
                                                                                                                                                          Class<U> environmentListClass,
                                                                                                                                                          String name,
                                                                                                                                                          String description,
                                                                                                                                                          Icon icon,
                                                                                                                                                          JIPipeEnvironmentSetupTool... setupTools) {
        service.getEnvironments().registerEnvironment(id, artifactQuery, archetype, environmentClass, optionalEnvironmentClass, environmentListClass, name, description, icon);
        for (JIPipeEnvironmentSetupTool setupTool : setupTools) {
            if(setupTool.accepts(environmentClass)) {
                service.getEnvironments().registerSetupTool(setupTool);
            }
            else {
                throw new IllegalArgumentException("Tried to register environment setup tool for environment ID=" + id + ", but tool " + setupTool + " does not accept environment class");
            }
        }
    }

    /**
     * Registers a new environment type.
     * This will also register the environment class and environment list class a valid parameters.
     * Requires that the environment class has a default constructor and a deep copy constructor.
     * Will also register a settings page for the environment
     *
     * @param <T>                      environment class
     * @param <U>                      list of environment class
     * @param id                       the ID of the environment class. Will be used as parameter type ID
     * @param archetype                the environment's archetype
     * @param environmentClass         the environment class. Must be JSON-serializable. Will be registered as parameter type
     * @param optionalEnvironmentClass optional environment class. Must be JSON-serializable. Will be registered as parameter type optional-[id].
     * @param environmentListClass     the list. Will be registered as parameter type with ID [id]-list
     * @param name                     the name of the environment
     * @param description              the description of the environment
     * @param setupTools tools that are compatible with that environment
     */
    public <T extends JIPipeEnvironment, U extends JIPipeListParameter<T>, V extends JIPipeOptionalParameter<T>> void registerEnvironment(String id, JIPipeEnvironmentArchetype archetype, Class<T> environmentClass,
                                                                                                                                          Class<V> optionalEnvironmentClass,
                                                                                                                                          Class<U> environmentListClass,
                                                                                                                                          String name,
                                                                                                                                          String description,
                                                                                                                                          Icon icon,
                                                                                                                                          JIPipeEnvironmentSetupTool... setupTools) {
        service.getEnvironments().registerEnvironment(id, null, archetype, environmentClass, optionalEnvironmentClass, environmentListClass, name, description, icon);
        for (JIPipeEnvironmentSetupTool setupTool : setupTools) {
            if(setupTool.accepts(environmentClass)) {
                service.getEnvironments().registerSetupTool(setupTool);
            }
            else {
                throw new IllegalArgumentException("Tried to register environment setup tool for environment ID=" + id + ", but tool " + setupTool + " does not accept environment class");
            }
        }
    }

    /**
     * Registers an environment setup tool
     * @param setupTool the tool
     */
    public void registerEnvironmentSetupTool(JIPipeEnvironmentSetupTool setupTool) {
        service.getEnvironments().registerSetupTool(setupTool);
    }

    /**
     * Registers file type metadata for the file chooser.
     * This is only for UX improvements with files and has no functional impact.
     *
     * @param name            the name
     * @param icon            the icon. If 32x32, the icon itself is used. Otherwise, it is combined with the default file icon
     * @param extension       the extension (including dot)
     * @param otherExtensions other extensions (including dot)
     */
    public void registerFileChooserKnownFileType(String name, Icon icon, String extension, String... otherExtensions) {
        JIPipeDesktopFileChooserNext.registerKnownFileType(name, icon, extension, otherExtensions);
    }

    /**
     * Registers file type metadata for the file chooser.
     * This is only for UX improvements with files and has no functional impact.
     *
     * @param name            the name
     * @param icon16Name      the icon in JIPipe's standard library. Combined with the default file icon
     * @param extension       the extension (including dot)
     * @param otherExtensions other extensions (including dot)
     */
    public void registerFileChooserKnownFileType(String name, String icon16Name, String extension, String... otherExtensions) {
        JIPipeDesktopFileChooserNext.registerKnownFileType(name, JIPipe.RESOURCES.getIcon16(icon16Name), extension, otherExtensions);
    }

    /**
     * Registers directory type metadata for the file chooser.
     * This is only for UX improvements with files and has no functional impact.
     *
     * @param name            the name
     * @param icon            the icon. If 32x32, the icon itself is used. Otherwise, it is combined with the default file icon
     * @param extension       the extension (including dot)
     * @param otherExtensions other extensions (including dot)
     */
    public void registerFileChooserKnownDirectoryType(String name, Icon icon, String extension, String... otherExtensions) {
        JIPipeDesktopFileChooserNext.registerKnownDirectoryType(name, icon, extension, otherExtensions);
    }

    /**
     * Registers directory type metadata for the file chooser.
     * This is only for UX improvements with files and has no functional impact.
     *
     * @param name            the name
     * @param icon16Name      the icon in JIPipe's standard library. Combined with the default file icon
     * @param extension       the extension (including dot)
     * @param otherExtensions other extensions (including dot)
     */
    public void registerFileChooserKnownDirectoryType(String name, String icon16Name, String extension, String... otherExtensions) {
        JIPipeDesktopFileChooserNext.registerKnownDirectoryType(name, JIPipe.RESOURCES.getIcon16(icon16Name), extension, otherExtensions);
    }

    @Override
    public void postprocess(JIPipeProgressInfo progressInfo) {
        this.reachedPostprocessing = true;
        JIPipeJavaPlugin.super.postprocess(progressInfo);

        if (!postprocessingTasks.isEmpty()) {
            progressInfo.log("Running " + postprocessingTasks.size() + " scheduled postprocessing tasks ...");
            for (Runnable task : postprocessingTasks) {
                task.run();
            }
        }
    }

    @Override
    public Path getDependencyLocation() {
        try {
            return Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }

    @Override
    public JIPipeService getService() {
        return service;
    }

    @Override
    public void setService(JIPipeService service) {
        this.service = service;
    }
}
