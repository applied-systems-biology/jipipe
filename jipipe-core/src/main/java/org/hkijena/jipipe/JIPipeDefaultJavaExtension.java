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

import org.hkijena.jipipe.api.JIPipeAuthorMetadata;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.environments.ExternalEnvironment;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentInstaller;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentParameterEditorUI;
import org.hkijena.jipipe.api.environments.ExternalEnvironmentSettings;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDefaultParameterTypeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.registries.JIPipeJavaNodeRegistrationTask;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistrationTask;
import org.hkijena.jipipe.api.registries.JIPipeParameterTypeRegistry;
import org.hkijena.jipipe.extensions.expressions.ExpressionFunction;
import org.hkijena.jipipe.extensions.expressions.functions.ColumnOperationAdapterFunction;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.EnumParameterTypeInfo;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.tables.ColumnOperation;
import org.hkijena.jipipe.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.jipipe.ui.extension.GraphEditorToolBarButtonExtension;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.grapheditor.contextmenu.NodeUIContextAction;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterGeneratorUI;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotPreview;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotRowUI;
import org.hkijena.jipipe.utils.ReflectionUtils;
import org.scijava.service.AbstractService;

import javax.swing.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Default implementation of {@link JIPipeJavaExtension}
 */
public abstract class JIPipeDefaultJavaExtension extends AbstractService implements JIPipeJavaExtension {

    private JIPipeMetadata metadata;
    private JIPipe registry;

    /**
     * Creates a new instance
     */
    public JIPipeDefaultJavaExtension() {
        metadata = new JIPipeMetadata();
        metadata.setName(getName());
        metadata.setDescription(getDescription());
        metadata.setAuthors(new JIPipeAuthorMetadata.List(getAuthors()));
        metadata.setDependencyCitations(new StringList(getDependencyCitations()));
        metadata.setCitation(getCitation());
        metadata.setLicense(getLicense());
        metadata.setWebsite(getWebsite());
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
    public JIPipeMetadata getMetadata() {
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

    /**
     * @return The extension logo
     */
    public abstract URL getLogo();

    @Override
    public void reportValidity(JIPipeValidityReport report) {

    }

    @Override
    public JIPipe getRegistry() {
        return registry;
    }

    @Override
    public void setRegistry(JIPipe registry) {
        this.registry = registry;
    }

    /**
     * Registers a custom menu entry.
     *
     * @param klass The menu entry
     */
    public void registerMenuExtension(Class<? extends MenuExtension> klass) {
        registry.getCustomMenuRegistry().registerMenu(klass);
    }

    /**
     * Registers a custom button for the graph editor toolbar
     *
     * @param klass the toolbar button class
     */
    public void registerGraphEditorToolBarButtonExtension(Class<? extends GraphEditorToolBarButtonExtension> klass) {
        registry.getCustomMenuRegistry().registerGraphEditorToolBarButton(klass);
    }

    /**
     * Registers a custom context menu action
     *
     * @param action the action
     */
    public void registerContextMenuAction(NodeUIContextAction action) {
        registry.getCustomMenuRegistry().registerContextMenuAction(action);
    }

    /**
     * Registers a non-trivial conversion between data types.
     * Please note that JIPipe will prefer trivial conversions (using Java inheritance) to custom converters.
     *
     * @param converter the converter
     */
    public void registerDatatypeConversion(JIPipeDataConverter converter) {
        registry.getDatatypeRegistry().registerConversion(converter);
    }

    /**
     * Registers a new data type
     *
     * @param id         Data type id
     * @param dataClass  Data class
     * @param icon       Icon for the data type. Can be null.
     * @param rowUI      Results analyzer row UI for the data type. Can be null. If null, it will use the default row UI that manages {@link org.hkijena.jipipe.api.data.JIPipeDataImportOperation} instances.
     * @param cellUI     Results table cell UI. Can be null.
     * @param operations list of operations to register. passed to registerDatatypeOperation.
     */
    public void registerDatatype(String id, Class<? extends JIPipeData> dataClass, URL icon, Class<? extends JIPipeResultDataSlotRowUI> rowUI, Class<? extends JIPipeResultDataSlotPreview> cellUI, JIPipeDataOperation... operations) {
        registry.getDatatypeRegistry().register(id, dataClass, this);
        if (icon != null) {
            registry.getDatatypeRegistry().registerIcon(dataClass, icon);
        }
        if (rowUI != null) {
            registry.getDatatypeRegistry().registerResultSlotUI(dataClass, rowUI);
        }
        if (cellUI != null) {
            registry.getDatatypeRegistry().registerResultTableCellUI(dataClass, cellUI);
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
    public void registerDatatypeOperation(String dataTypeId, JIPipeDataOperation... operations) {
        for (JIPipeDataOperation operation : operations) {
            if (operation instanceof JIPipeDataImportOperation) {
                registerDatatypeImportOperation(dataTypeId, (JIPipeDataImportOperation) operation);
            }
            if (operation instanceof JIPipeDataDisplayOperation) {
                registerDatatypeDisplayOperation(dataTypeId, (JIPipeDataDisplayOperation) operation);
            }
        }
    }

    /**
     * Registers an import operation for the data type.
     * This is not used if the data type is assigned a non-default row UI
     *
     * @param dataTypeId the data type id. it is not required that the data type is registered, yet. If empty, the operations are applying to all data.
     * @param operation  the operation
     */
    public void registerDatatypeImportOperation(String dataTypeId, JIPipeDataImportOperation operation) {
        registry.getDatatypeRegistry().registerImportOperation(dataTypeId, operation);
    }

    /**
     * Registers an additional non-default display operation for the data type. Used in the cache browser.
     *
     * @param dataTypeId the data type id. it is not required that the data type is registered, yet. If empty, the operations are applying to all data.
     * @param operation  the operation
     */
    public void registerDatatypeDisplayOperation(String dataTypeId, JIPipeDataDisplayOperation operation) {
        registry.getDatatypeRegistry().registerDisplayOperation(dataTypeId, operation);
    }

    /**
     * Registers a node type category
     *
     * @param category the category
     */
    public void registerNodeTypeCategory(JIPipeNodeTypeCategory category) {
        registry.getNodeRegistry().registerCategory(category);
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
        registry.getNodeRegistry().register(info, this);
    }

    /**
     * Registers a new node type. It is assumed that all dependencies are met.
     * If the dependency situation is unclear, register an {@link JIPipeNodeRegistrationTask} instead
     *
     * @param info Algorithm info
     * @param icon custom algorithm icon
     */
    public void registerNodeType(JIPipeNodeInfo info, URL icon) {
        registry.getNodeRegistry().register(info, this);
        registry.getNodeRegistry().registerIcon(info, icon);
    }

    /**
     * Registers a new node type with additional dependencies.
     * Actual registration happens when all dependencies are met.-
     *
     * @param task Algorithm registration task
     */
    public void registerNodeType(JIPipeNodeRegistrationTask task) {
        registry.getNodeRegistry().scheduleRegister(task);
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
        registerParameterType(new EnumParameterTypeInfo(id, parameterClass, name, description), null);
    }

    /**
     * Registers a new parameter type and respective editors
     *
     * @param id                   Unique ID of this parameter type
     * @param parameterClass       Parameter class
     * @param newInstanceGenerator Function that creates a new instance
     * @param duplicateFunction    Function that copies an existing instance
     * @param name                 Parameter class name
     * @param description          Description for the parameter type
     * @param uiClass              Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(String id, Class<?> parameterClass, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description, Class<? extends JIPipeParameterEditorUI> uiClass) {
        JIPipeDefaultParameterTypeInfo info = new JIPipeDefaultParameterTypeInfo(id,
                parameterClass,
                newInstanceGenerator != null ? newInstanceGenerator : () -> ReflectionUtils.newInstance(parameterClass),
                duplicateFunction != null ? duplicateFunction : o -> ReflectionUtils.newInstance(parameterClass, o),
                name,
                description);
        registerParameterType(info, uiClass);
    }

    /**
     * Registers a new parameter type that already has an existing editor.
     * Must have a default constructor and a deep copy constructor
     * @param id Unique ID of this parameter type
     * @param parameterClass Parameter class
     * @param name Parameter class name
     * @param description Description for the parameter type
     */
    public void registerParameterType(String id, Class<?> parameterClass, String name, String description) {
        registerParameterType(id, parameterClass, null, null, name, description, null);
    }

    /**
     * Registers a new parameter type.
     * Must have a default constructor and a deep copy constructor
     * @param id Unique ID of this parameter type
     * @param parameterClass Parameter class
     * @param name Parameter class name
     * @param description Description for the parameter type
     * @param uiClass              Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(String id, Class<?> parameterClass, String name, String description, Class<? extends JIPipeParameterEditorUI> uiClass) {
        registerParameterType(id, parameterClass, null, null, name, description, uiClass);
    }

    /**
     * Registers a new parameter type and respective editors
     *
     * @param id                   Unique ID of this parameter type
     * @param parameterClass       Parameter class
     * @param listClass            Optional list class. If not null this creates a registration entry for the equivalent list entry
     * @param newInstanceGenerator Function that creates a new instance. If null, the function calls the default constructor
     * @param duplicateFunction    Function that copies an existing instance. If null, the function calls the copy constructor
     * @param name                 Parameter class name
     * @param description          Description for the parameter type
     * @param uiClass              Parameter editor UI. Can be null if the editor is already provided.
     * @param <T>                  parameter class
     */
    public <T> void registerParameterType(String id, Class<T> parameterClass, Class<? extends ListParameter<T>> listClass, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description, Class<? extends JIPipeParameterEditorUI> uiClass) {
        JIPipeDefaultParameterTypeInfo info = new JIPipeDefaultParameterTypeInfo(id,
                parameterClass,
                newInstanceGenerator != null ? newInstanceGenerator : () -> ReflectionUtils.newInstance(parameterClass),
                duplicateFunction != null ? duplicateFunction : o -> ReflectionUtils.newInstance(parameterClass, o),
                name,
                description);
        registerParameterType(info, uiClass);
        if (listClass != null) {
            registerParameterType(id + "-list", listClass, () -> ReflectionUtils.newInstance(listClass),
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
    public void registerParameterType(JIPipeParameterTypeInfo info, Class<? extends JIPipeParameterEditorUI> uiClass) {
        registry.getParameterTypeRegistry().register(info);
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
    public void registerParameterType(JIPipeParameterTypeInfo info, Class<?> listClass, Class<? extends JIPipeParameterEditorUI> uiClass) {
        registry.getParameterTypeRegistry().register(info);
        if (uiClass != null) {
            registerParameterEditor(info.getFieldClass(), uiClass);
        }
        if (listClass != null) {
            registerParameterType(info.getId() + "-list", listClass, () -> ReflectionUtils.newInstance(listClass),
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
    public void registerParameterEditor(Class<?> parameterClass, Class<? extends JIPipeParameterEditorUI> uiClass) {
        registry.getParameterTypeRegistry().registerParameterEditor(parameterClass, uiClass);
    }

    /**
     * Registers a UI that can generate parameters
     *
     * @param parameterClass Parameter class
     * @param uiClass        The generator UI class
     * @param name           Generator name
     * @param description    Description for the generator
     */
    public void registerParameterGenerator(Class<?> parameterClass, Class<? extends JIPipeParameterGeneratorUI> uiClass, String name, String description) {
        JIPipeParameterTypeRegistry parametertypeRegistry = registry.getParameterTypeRegistry();
        parametertypeRegistry.registerGenerator(parameterClass, uiClass, name, description);
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
        registry.getExpressionRegistry().registerColumnOperation(id, operation, name, shortName, description);
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
        registry.getExpressionRegistry().registerColumnOperation(id, operation, name, shortName, description);
        registerExpressionFunction(new ColumnOperationAdapterFunction(operation, shortName.toUpperCase().replace(' ', '_')), name, description);
    }

    /**
     * Registers a function that can be used within expressions.
     *
     * @param function    the function. its internal name property must be unique
     * @param name        human-readable name
     * @param description the description
     */
    public void registerExpressionFunction(ExpressionFunction function, String name, String description) {
        registry.getExpressionRegistry().registerExpressionFunction(function, name, description);
    }

    /**
     * Registers a function that can be used within expressions. The name and description are taken from {@link org.hkijena.jipipe.api.JIPipeDocumentation} annotations.
     *
     * @param function the function. its internal name property must be unique
     */
    public void registerExpressionFunction(ExpressionFunction function) {
        JIPipeDocumentation documentation = function.getClass().getAnnotation(JIPipeDocumentation.class);
        registry.getExpressionRegistry().registerExpressionFunction(function, documentation.name(), documentation.description());
    }

    /**
     * Registers an adapter between ImageJ and JIPipe data types
     *
     * @param adapter         An adapter
     * @param importerUIClass User interface class used for importing ImageJ data
     */
    public void registerImageJDataAdapter(ImageJDatatypeAdapter adapter, Class<? extends ImageJDatatypeImporterUI> importerUIClass) {
        registry.getImageJDataAdapterRegistry().register(adapter);
        registry.getImageJDataAdapterRegistry().registerImporterFor(adapter.getImageJDatatype(), importerUIClass);
    }

    /**
     * Registers a new settings sheet
     *
     * @param id                  unique ID
     * @param name                sheet name
     * @param icon                sheet icon
     * @param category            sheet category (if null defaults to "General")
     * @param categoryIcon        category icon (if null defaults to a predefined icon)
     * @param parameterCollection the settings
     */
    public void registerSettingsSheet(String id, String name, Icon icon, String category, Icon categoryIcon, JIPipeParameterCollection parameterCollection) {
        registry.getSettingsRegistry().register(id, name, icon, category, categoryIcon, parameterCollection);
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
        registry.getUtilityRegistry().register(categoryClass, utilityClass);
    }

    /**
     * Registers a new environment type.
     * This will also register the environment class and environment list class a valid parameters.
     * Requires that the environment class has a default constructor and a deep copy constructor.
     * Will also register a settings page for the environment
     *
     * @param environmentClass the environment class. Must be JSON-serializable. Will be registered as parameter type
     * @param listClass        the list list. Will be registered as parameter type with ID [id]-list
     * @param settings         Settings page that stores the user's presets
     * @param id               the ID of the environment class. Will be used as parameter type ID
     * @param name             the name of the environment
     * @param description      the description of the environment
     * @param <T>              environment class
     * @param <U>              list of environment class
     */
    public <T extends ExternalEnvironment, U extends ListParameter<T>> void registerEnvironment(Class<T> environmentClass,
                                                                                                Class<U> listClass,
                                                                                                ExternalEnvironmentSettings settings,
                                                                                                String id,
                                                                                                String name,
                                                                                                String description,
                                                                                                Icon icon) {
        registerParameterType(id, environmentClass, listClass, null, null, name, description, ExternalEnvironmentParameterEditorUI.class);
        registry.getExternalEnvironmentRegistry().registerEnvironment(environmentClass, settings);
    }

    /**
     * Registers an installer for a given environment
     *
     * @param environmentClass the environment type
     * @param installerClass   the installer class
     * @param icon             icon for the installer
     */
    public void registerEnvironmentInstaller(Class<? extends ExternalEnvironment> environmentClass, Class<? extends ExternalEnvironmentInstaller> installerClass, Icon icon) {
        registry.getExternalEnvironmentRegistry().registerInstaller(environmentClass, installerClass, icon);
    }

    @Override
    public Path getDependencyLocation() {
        try {
            return Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
