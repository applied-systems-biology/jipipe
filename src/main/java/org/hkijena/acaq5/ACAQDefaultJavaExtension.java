package org.hkijena.acaq5;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDefaultDocumentation;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.registries.ACAQJavaAlgorithmRegistrationTask;
import org.hkijena.acaq5.api.traits.ACAQJavaTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTrait;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
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
import java.net.URL;

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
    public ACAQDefaultRegistry getRegistry() {
        return registry;
    }

    @Override
    public void setRegistry(ACAQDefaultRegistry registry) {
        this.registry = registry;
    }

    public void registerTrait(String id, Class<? extends ACAQTrait> traitClass, URL icon) {
        registerTrait(new ACAQJavaTraitDeclaration(id, traitClass), icon);
    }

    public void registerTrait(ACAQTraitDeclaration traitDeclaration, URL icon) {
        registry.getTraitRegistry().register(traitDeclaration, this);
        if (icon != null) {
            registry.getUITraitRegistry().registerIcon(traitDeclaration, icon);
        }
    }

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

    public void registerAlgorithm(String id, Class<? extends ACAQAlgorithm> algorithmClass) {
        registerAlgorithm(new ACAQJavaAlgorithmRegistrationTask(id, algorithmClass, this));
    }

    public void registerAlgorithm(ACAQAlgorithmDeclaration declaration) {
        registry.getAlgorithmRegistry().register(declaration, this);
    }

    public void registerAlgorithm(ACAQAlgorithmRegistrationTask task) {
        registry.getAlgorithmRegistry().scheduleRegister(task);
    }

    public void registerParameterType(Class<?> parameterClass, Class<? extends ACAQParameterEditorUI> uiClass, String name, String description) {
        ACAQUIParametertypeRegistry parametertypeRegistry = registry.getUIParametertypeRegistry();
        parametertypeRegistry.registerParameterEditor(parameterClass, uiClass);
        parametertypeRegistry.registerDocumentation(parameterClass, new ACAQDefaultDocumentation(name, description));
    }

    public void registerPlot(Class<? extends ACAQPlot> plotClass, Class<? extends ACAQPlotSettingsUI> plotSettingsUIClass, String name, ImageIcon icon) {
        registry.getPlotBuilderRegistry().register(plotClass, plotSettingsUIClass, name, icon);
    }

    public void registerTableOperation(Class<? extends ACAQTableVectorOperation> operationClass,
                                       Class<? extends ACAQTableVectorOperationUI> uiClass,
                                       String name,
                                       String shortcut,
                                       String description,
                                       Icon icon) {
        registry.getTableAnalyzerUIOperationRegistry().register(operationClass, uiClass, name, shortcut, description, icon);
    }
}
