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
import org.hkijena.jipipe.api.JIPipeMetadata;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.compat.ImageJDatatypeAdapter;
import org.hkijena.jipipe.api.data.JIPipeData;
import org.hkijena.jipipe.api.data.JIPipeDataConverter;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.JIPipeJavaNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeInfo;
import org.hkijena.jipipe.api.nodes.JIPipeNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDefaultParameterTypeInfo;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTypeInfo;
import org.hkijena.jipipe.api.registries.JIPipeJavaNodeRegistrationTask;
import org.hkijena.jipipe.api.registries.JIPipeNodeRegistrationTask;
import org.hkijena.jipipe.extensions.parameters.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.EnumParameterTypeInfo;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.tables.ColumnOperation;
import org.hkijena.jipipe.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.jipipe.ui.extension.MenuExtension;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterEditorUI;
import org.hkijena.jipipe.ui.parameters.JIPipeParameterGeneratorUI;
import org.hkijena.jipipe.ui.registries.JIPipeUIParameterTypeRegistry;
import org.hkijena.jipipe.ui.resultanalysis.JIPipeResultDataSlotPreviewUI;
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
    private JIPipeDefaultRegistry registry;

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
    public abstract String getDescription();

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
    public JIPipeDefaultRegistry getRegistry() {
        return registry;
    }

    @Override
    public void setRegistry(JIPipeDefaultRegistry registry) {
        this.registry = registry;
    }

    /**
     * Registers a custom menu entry.
     *
     * @param klass The menu entry
     */
    public void registerMenuExtension(Class<? extends MenuExtension> klass) {
        registry.getUIMenuServiceRegistry().register(klass);
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
     *  @param id        Data type id
     * @param dataClass Data class
     * @param icon      Icon for the data type. Can be null.
     * @param rowUI     Results analyzer row UI for the data type. Can be null.
     * @param cellUI    Results table cell UI. Can be null.
     */
    public void registerDatatype(String id, Class<? extends JIPipeData> dataClass, URL icon, Class<? extends JIPipeResultDataSlotRowUI> rowUI, Class<? extends JIPipeResultDataSlotPreviewUI> cellUI) {
        registry.getDatatypeRegistry().register(id, dataClass, this);
        if (icon != null) {
            registry.getUIDatatypeRegistry().registerIcon(dataClass, icon);
        }
        if (rowUI != null) {
            registry.getUIDatatypeRegistry().registerResultSlotUI(dataClass, rowUI);
        }
        if (cellUI != null) {
            registry.getUIDatatypeRegistry().registerResultTableCellUI(dataClass, cellUI);
        }
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
        registry.getUIAlgorithmRegistry().registerIcon(info, icon);
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
        registry.getUIParameterTypeRegistry().registerParameterEditor(parameterClass, uiClass);
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
        JIPipeUIParameterTypeRegistry parametertypeRegistry = registry.getUIParameterTypeRegistry();
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
        registry.getTableRegistry().registerColumnOperation(id, operation, name, shortName, description);
    }

    /**
     * Registers an adapter between ImageJ and JIPipe data types
     *
     * @param adapter         An adapter
     * @param importerUIClass User interface class used for importing ImageJ data
     */
    public void registerImageJDataAdapter(ImageJDatatypeAdapter adapter, Class<? extends ImageJDatatypeImporterUI> importerUIClass) {
        registry.getImageJDataAdapterRegistry().register(adapter);
        registry.getUIImageJDatatypeAdapterRegistry().registerImporterFor(adapter.getImageJDatatype(), importerUIClass);
    }

    /**
     * Registers a new settings sheet
     *
     * @param id                  unique ID
     * @param name                sheet name
     * @param category            sheet category (if null defaults to "General")
     * @param categoryIcon        category icon (if null defaults to a predefined icon)
     * @param parameterCollection the settings
     */
    public void registerSettingsSheet(String id, String name, String category, Icon categoryIcon, JIPipeParameterCollection parameterCollection) {
        registry.getSettingsRegistry().register(id, name, category, categoryIcon, parameterCollection);
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
