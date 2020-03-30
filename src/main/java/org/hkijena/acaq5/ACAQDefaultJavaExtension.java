package org.hkijena.acaq5;

import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDefaultDocumentation;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.compat.ImageJDatatypeAdapter;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.registries.ACAQJavaAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.traits.ACAQJavaTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.ui.compat.ImageJDatatypeImporterUI;
import org.hkijena.acaq5.ui.grapheditor.settings.ACAQParameterEditorUI;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlot;
import org.hkijena.acaq5.ui.plotbuilder.ACAQPlotSettingsUI;
import org.hkijena.acaq5.ui.registries.ACAQUIParametertypeRegistry;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultDataSlotCellUI;
import org.hkijena.acaq5.ui.resultanalysis.ACAQResultDataSlotRowUI;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperation;
import org.hkijena.acaq5.ui.tableanalyzer.ACAQTableVectorOperationUI;
import org.scijava.service.AbstractService;

import javax.swing.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class ACAQDefaultJavaExtension extends AbstractService implements ACAQJavaExtension {

    private EventBus eventBus = new EventBus();
    private ACAQProjectMetadata metadata;
    private ACAQDefaultRegistry registry;

    public ACAQDefaultJavaExtension() {
        metadata = new ACAQProjectMetadata();
        metadata.setName(getName());
        metadata.setDescription(getDescription());
        metadata.setAuthors(getAuthors());
        metadata.setCitation(getCitation());
        metadata.setLicense(getLicense());
        metadata.setWebsite(getWebsite());
    }

    public abstract String getCitation();

    @Override
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    public abstract String getName();

    public abstract String getDescription();

    public abstract String getAuthors();

    public abstract String getWebsite();

    public abstract String getLicense();

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
     * Registers a new annotation type. The {@link ACAQTraitDeclaration} is generated from the class as {@link ACAQJavaTraitDeclaration}.
     * It is assumed that all dependencies are met.
     *
     * @param id
     * @param traitClass
     * @param icon
     */
    public void registerTrait(String id, Class<? extends ACAQTrait> traitClass, URL icon) {
        registerTrait(new ACAQJavaTraitDeclaration(id, traitClass), icon);
    }

    /**
     * Registers a new annotation type. It is assumed that all dependencies are met.
     *
     * @param traitDeclaration
     * @param icon
     */
    public void registerTrait(ACAQTraitDeclaration traitDeclaration, URL icon) {
        registry.getTraitRegistry().register(traitDeclaration, this);
        if (icon != null) {
            registry.getUITraitRegistry().registerIcon(traitDeclaration, icon);
        }
    }

    /**
     * Registers a new data type
     *
     * @param id
     * @param dataClass
     * @param icon
     * @param rowUI
     * @param cellUI
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
     * Registers a new algorithm. The {@link ACAQAlgorithmDeclaration} is generated as {@link org.hkijena.acaq5.api.algorithm.ACAQJavaAlgorithmDeclaration}.
     *
     * @param id
     * @param algorithmClass
     */
    public void registerAlgorithm(String id, Class<? extends ACAQAlgorithm> algorithmClass) {
        registerAlgorithm(new ACAQJavaAlgorithmRegistrationTask(id, algorithmClass, this));
    }

    /**
     * Registers a new algorithm. It is assumed that all dependencies are met.
     * If the dependency situation is unclear, register an {@link ACAQAlgorithmRegistrationTask} instead
     *
     * @param declaration
     */
    public void registerAlgorithm(ACAQAlgorithmDeclaration declaration) {
        registry.getAlgorithmRegistry().register(declaration, this);
    }

    /**
     * Registers a new algorithm with additional dependencies.
     * Actual registration happens when all dependencies are met.-
     *
     * @param task
     */
    public void registerAlgorithm(ACAQAlgorithmRegistrationTask task) {
        registry.getAlgorithmRegistry().scheduleRegister(task);
    }

    /**
     * Registers a new parameter type and respective editors
     *
     * @param parameterClass
     * @param uiClass
     * @param name
     * @param description
     */
    public void registerParameterType(Class<?> parameterClass, Class<? extends ACAQParameterEditorUI> uiClass, String name, String description) {
        ACAQUIParametertypeRegistry parametertypeRegistry = registry.getUIParametertypeRegistry();
        parametertypeRegistry.registerParameterEditor(parameterClass, uiClass);
        parametertypeRegistry.registerDocumentation(parameterClass, new ACAQDefaultDocumentation(name, description));
    }

    /**
     * Registers a new plot type
     *
     * @param plotClass
     * @param plotSettingsUIClass
     * @param name
     * @param icon
     */
    public void registerPlot(Class<? extends ACAQPlot> plotClass, Class<? extends ACAQPlotSettingsUI> plotSettingsUIClass, String name, ImageIcon icon) {
        registry.getPlotBuilderRegistry().register(plotClass, plotSettingsUIClass, name, icon);
    }

    /**
     * Registers a new table operation
     *
     * @param operationClass
     * @param uiClass
     * @param name
     * @param shortcut
     * @param description
     * @param icon
     */
    public void registerTableOperation(Class<? extends ACAQTableVectorOperation> operationClass,
                                       Class<? extends ACAQTableVectorOperationUI> uiClass,
                                       String name,
                                       String shortcut,
                                       String description,
                                       Icon icon) {
        registry.getTableAnalyzerUIOperationRegistry().register(operationClass, uiClass, name, shortcut, description, icon);
    }

    /**
     * Registers an adapter between ImageJ and ACAQ5 data types
     *
     * @param adapter
     * @param importerUIClass User interface class used for importing ImageJ data
     */
    public void registerImageJDataAdapter(ImageJDatatypeAdapter adapter, Class<? extends ImageJDatatypeImporterUI> importerUIClass) {
        registry.getImageJDataAdapterRegistry().register(adapter);
        registry.getUIImageJDatatypeAdapterRegistry().registerImporterFor(adapter.getImageJDatatype(), importerUIClass);
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
