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

package org.hkijena.pipelinej;

import com.google.common.eventbus.EventBus;
import org.hkijena.pipelinej.api.ACAQAuthorMetadata;
import org.hkijena.pipelinej.api.ACAQMetadata;
import org.hkijena.pipelinej.api.ACAQValidityReport;
import org.hkijena.pipelinej.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.pipelinej.api.algorithm.ACAQGraphNode;
import org.hkijena.pipelinej.api.compat.ImageJDatatypeAdapter;
import org.hkijena.pipelinej.api.data.ACAQData;
import org.hkijena.pipelinej.api.data.ACAQDataConverter;
import org.hkijena.pipelinej.api.parameters.ACAQDefaultParameterTypeDeclaration;
import org.hkijena.pipelinej.api.parameters.ACAQParameterCollection;
import org.hkijena.pipelinej.api.parameters.ACAQParameterTypeDeclaration;
import org.hkijena.pipelinej.api.registries.ACAQAlgorithmRegistrationTask;
import org.hkijena.pipelinej.api.registries.ACAQJavaAlgorithmRegistrationTask;
import org.hkijena.pipelinej.extensions.tables.ColumnOperation;
import org.hkijena.pipelinej.extensions.parameters.collections.ListParameter;
import org.hkijena.pipelinej.extensions.parameters.primitives.EnumParameterTypeDeclaration;
import org.hkijena.pipelinej.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.pipelinej.ui.extension.MenuExtension;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterEditorUI;
import org.hkijena.pipelinej.ui.parameters.ACAQParameterGeneratorUI;
import org.hkijena.pipelinej.ui.registries.ACAQUIParameterTypeRegistry;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQResultDataSlotCellUI;
import org.hkijena.pipelinej.ui.resultanalysis.ACAQResultDataSlotRowUI;
import org.hkijena.pipelinej.utils.ReflectionUtils;
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
 * Default implementation of {@link ACAQJavaExtension}
 */
public abstract class ACAQDefaultJavaExtension extends AbstractService implements ACAQJavaExtension {

    private EventBus eventBus = new EventBus();
    private ACAQMetadata metadata;
    private ACAQDefaultRegistry registry;

    /**
     * Creates a new instance
     */
    public ACAQDefaultJavaExtension() {
        metadata = new ACAQMetadata();
        metadata.setName(getName());
        metadata.setDescription(getDescription());
        metadata.setAuthors(new ACAQAuthorMetadata.List(getAuthors()));
        metadata.setCitation(getCitation());
        metadata.setLicense(getLicense());
        metadata.setWebsite(getWebsite());
    }

    /**
     * @return The citation
     */
    public abstract String getCitation();

    @Override
    public ACAQMetadata getMetadata() {
        return metadata;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
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
    public abstract List<ACAQAuthorMetadata> getAuthors();

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
    public void reportValidity(ACAQValidityReport report) {

    }

    @Override
    public ACAQDefaultRegistry getRegistry() {
        return registry;
    }

    @Override
    public void setRegistry(ACAQDefaultRegistry registry) {
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
     * Please note that ACAQ will prefer trivial conversions (using Java inheritance) to custom converters.
     *
     * @param converter the converter
     */
    public void registerDatatypeConversion(ACAQDataConverter converter) {
        registry.getDatatypeRegistry().registerConversion(converter);
    }

    /**
     * Registers a new data type
     *
     * @param id        Data type id
     * @param dataClass Data class
     * @param icon      Icon for the data type. Can be null.
     * @param rowUI     Results analyzer row UI for the data type. Can be null.
     * @param cellUI    Results table cell UI. Can be null.
     */
    public void registerDatatype(String id, Class<? extends ACAQData> dataClass, URL icon, Class<? extends ACAQResultDataSlotRowUI> rowUI, ACAQResultDataSlotCellUI cellUI) {
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
     * Registers a new algorithm. The {@link ACAQAlgorithmDeclaration} is generated as {@link org.hkijena.pipelinej.api.algorithm.ACAQJavaAlgorithmDeclaration}.
     *
     * @param id             Algorithm ID
     * @param algorithmClass Algorithm class
     */
    public void registerAlgorithm(String id, Class<? extends ACAQGraphNode> algorithmClass) {
        registerAlgorithm(new ACAQJavaAlgorithmRegistrationTask(id, algorithmClass, this, null));
    }

    /**
     * Registers a new algorithm. The {@link ACAQAlgorithmDeclaration} is generated as {@link org.hkijena.pipelinej.api.algorithm.ACAQJavaAlgorithmDeclaration}.
     *
     * @param id             Algorithm ID
     * @param algorithmClass Algorithm class
     * @param icon           custom icon
     */
    public void registerAlgorithm(String id, Class<? extends ACAQGraphNode> algorithmClass, URL icon) {
        registerAlgorithm(new ACAQJavaAlgorithmRegistrationTask(id, algorithmClass, this, icon));
    }

    /**
     * Registers a new algorithm. It is assumed that all dependencies are met.
     * If the dependency situation is unclear, register an {@link ACAQAlgorithmRegistrationTask} instead
     *
     * @param declaration Algorithm declaration
     */
    public void registerAlgorithm(ACAQAlgorithmDeclaration declaration) {
        registry.getAlgorithmRegistry().register(declaration, this);
    }

    /**
     * Registers a new algorithm. It is assumed that all dependencies are met.
     * If the dependency situation is unclear, register an {@link ACAQAlgorithmRegistrationTask} instead
     *
     * @param declaration Algorithm declaration
     * @param icon        custom algorithm icon
     */
    public void registerAlgorithm(ACAQAlgorithmDeclaration declaration, URL icon) {
        registry.getAlgorithmRegistry().register(declaration, this);
        registry.getUIAlgorithmRegistry().registerIcon(declaration, icon);
    }

    /**
     * Registers a new algorithm with additional dependencies.
     * Actual registration happens when all dependencies are met.-
     *
     * @param task Algorithm registration task
     */
    public void registerAlgorithm(ACAQAlgorithmRegistrationTask task) {
        registry.getAlgorithmRegistry().scheduleRegister(task);
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
        registerParameterType(new EnumParameterTypeDeclaration(id, parameterClass, name, description), null);
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
    public void registerParameterType(String id, Class<?> parameterClass, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description, Class<? extends ACAQParameterEditorUI> uiClass) {
        ACAQDefaultParameterTypeDeclaration declaration = new ACAQDefaultParameterTypeDeclaration(id,
                parameterClass,
                newInstanceGenerator != null ? newInstanceGenerator : () -> ReflectionUtils.newInstance(parameterClass),
                duplicateFunction != null ? duplicateFunction : o -> ReflectionUtils.newInstance(parameterClass, o),
                name,
                description);
        registerParameterType(declaration, uiClass);
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
    public <T> void registerParameterType(String id, Class<T> parameterClass, Class<? extends ListParameter<T>> listClass, Supplier<Object> newInstanceGenerator, Function<Object, Object> duplicateFunction, String name, String description, Class<? extends ACAQParameterEditorUI> uiClass) {
        ACAQDefaultParameterTypeDeclaration declaration = new ACAQDefaultParameterTypeDeclaration(id,
                parameterClass,
                newInstanceGenerator != null ? newInstanceGenerator : () -> ReflectionUtils.newInstance(parameterClass),
                duplicateFunction != null ? duplicateFunction : o -> ReflectionUtils.newInstance(parameterClass, o),
                name,
                description);
        registerParameterType(declaration, uiClass);
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
     * @param declaration the declaration
     * @param uiClass     Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(ACAQParameterTypeDeclaration declaration, Class<? extends ACAQParameterEditorUI> uiClass) {
        registry.getParameterTypeRegistry().register(declaration);
        if (uiClass != null) {
            registerParameterEditor(declaration.getFieldClass(), uiClass);
        }
    }

    /**
     * Registers a new parameter type and respective editor
     *
     * @param declaration the declaration
     * @param listClass   a parameter type that is a list of the registered type
     * @param uiClass     Parameter editor UI. Can be null if the editor is already provided.
     */
    public void registerParameterType(ACAQParameterTypeDeclaration declaration, Class<?> listClass, Class<? extends ACAQParameterEditorUI> uiClass) {
        registry.getParameterTypeRegistry().register(declaration);
        if (uiClass != null) {
            registerParameterEditor(declaration.getFieldClass(), uiClass);
        }
        if (listClass != null) {
            registerParameterType(declaration.getId() + "-list", listClass, () -> ReflectionUtils.newInstance(listClass),
                    o -> ReflectionUtils.newInstance(listClass, o),
                    "List of " + declaration.getName(),
                    declaration.getDescription(),
                    null);
        }
    }

    /**
     * Registers an editor for any parameter type that inherits from the provided parameter class.
     * Please use this with caution, as unregistered parameters are rejected by components that require a unique
     * parameter type ID (for example {@link org.hkijena.pipelinej.api.parameters.ACAQDynamicParameterCollection})
     *
     * @param parameterClass the parameter class
     * @param uiClass        the editor class
     */
    public void registerParameterEditor(Class<?> parameterClass, Class<? extends ACAQParameterEditorUI> uiClass) {
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
    public void registerParameterGenerator(Class<?> parameterClass, Class<? extends ACAQParameterGeneratorUI> uiClass, String name, String description) {
        ACAQUIParameterTypeRegistry parametertypeRegistry = registry.getUIParameterTypeRegistry();
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
     * Registers an adapter between ImageJ and ACAQ5 data types
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
    public void registerSettingsSheet(String id, String name, String category, Icon categoryIcon, ACAQParameterCollection parameterCollection) {
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
