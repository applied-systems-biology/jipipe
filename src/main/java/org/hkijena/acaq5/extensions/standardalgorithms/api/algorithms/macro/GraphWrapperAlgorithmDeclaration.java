package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms.macro;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefCollection;
import org.hkijena.acaq5.extensions.standardparametereditors.ui.parametereditors.StringParameterSettings;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class GraphWrapperAlgorithmDeclaration implements ACAQAlgorithmDeclaration, ACAQValidatable, ACAQParameterHolder {

    private EventBus eventBus = new EventBus();
    private String id;
    private ACAQProjectMetadata metadata = new ACAQProjectMetadata();
    private ACAQAlgorithmCategory category = ACAQAlgorithmCategory.Miscellaneous;
    private Set<ACAQTraitDeclaration> preferredTraits = new HashSet<>();
    private Set<ACAQTraitDeclaration> unwantedTraits = new HashSet<>();
    private Set<ACAQTraitDeclaration> addedTraits = new HashSet<>();
    private Set<ACAQTraitDeclaration> removedTraits = new HashSet<>();
    private ACAQDataSlotTraitConfiguration dataSlotTraitConfiguration;
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();
    private ACAQParameterCollectionVisibilities parameterCollectionVisibilities = new ACAQParameterCollectionVisibilities();
    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
    private String menuPath = "";

    public GraphWrapperAlgorithmDeclaration() {
    }

    @ACAQDocumentation(name = "Algorithm ID", description = "An unique identifier for the algorithm")
    @ACAQParameter("id")
    @JsonGetter("id")
    @Override
    public String getId() {
        return id;
    }

    @ACAQParameter("id")
    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;
        getEventBus().post(new ParameterChangedEvent(this, "id"));
    }

    @Override
    public Class<? extends ACAQAlgorithm> getAlgorithmClass() {
        return GraphWrapperAlgorithm.class;
    }

    @Override
    public ACAQAlgorithm newInstance() {
        return new GraphWrapperAlgorithm(this);
    }

    @Override
    public ACAQAlgorithm clone(ACAQAlgorithm algorithm) {
        return new GraphWrapperAlgorithm((GraphWrapperAlgorithm) algorithm);
    }

    @Override
    public String getName() {
        return metadata.getName();
    }

    @Override
    public String getDescription() {
        return metadata.getDescription();
    }

    @ACAQDocumentation(name = "Algorithm category", description = "A general category for the algorithm.")
    @ACAQParameter("category")
    @JsonGetter("category")
    @Override
    public ACAQAlgorithmCategory getCategory() {
        return category;
    }

    @ACAQParameter("category")
    @JsonSetter("category")
    public void setCategory(ACAQAlgorithmCategory category) {
        this.category = category;
        getEventBus().post(new ParameterChangedEvent(this, "category"));
    }

    @Override
    public Set<ACAQTraitDeclaration> getPreferredTraits() {
        return preferredTraits;
    }

    public void setPreferredTraits(Set<ACAQTraitDeclaration> preferredTraits) {
        this.preferredTraits = preferredTraits;
    }

    @Override
    public Set<ACAQTraitDeclaration> getUnwantedTraits() {
        return unwantedTraits;
    }

    public void setUnwantedTraits(Set<ACAQTraitDeclaration> unwantedTraits) {
        this.unwantedTraits = unwantedTraits;
    }

    @Override
    public ACAQDataSlotTraitConfiguration getSlotTraitConfiguration() {
        if (dataSlotTraitConfiguration == null) {
            dataSlotTraitConfiguration = new ACAQDataSlotTraitConfiguration();
            for (ACAQTraitDeclaration addedTrait : addedTraits) {
                dataSlotTraitConfiguration.set(addedTrait, ACAQTraitModificationOperation.Add);
            }
            for (ACAQTraitDeclaration removedTrait : removedTraits) {
                dataSlotTraitConfiguration.set(removedTrait, ACAQTraitModificationOperation.RemoveCategory);
            }
        }
        return dataSlotTraitConfiguration;
    }

    @Override
    public List<AlgorithmInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @ACAQDocumentation(name = "Preferred annotations", description = "Marks the algorithm as good for handling the specified annotations")
    @ACAQParameter("preferred-traits")
    @JsonGetter("preferred-traits")
    public ACAQTraitDeclarationRefCollection getPreferredTraitIds() {
        return new ACAQTraitDeclarationRefCollection(preferredTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter("preferred-traits")
    @JsonGetter("preferred-traits")
    public void setPreferredTraitIds(ACAQTraitDeclarationRefCollection ids) {
        preferredTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            preferredTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "preferred-traits"));
    }

    @ACAQDocumentation(name = "Unwanted annotations", description = "Marks the algorithm as bad for handling the specified annotations")
    @ACAQParameter("unwanted-traits")
    @JsonGetter("unwanted-traits")
    public ACAQTraitDeclarationRefCollection getUnwantedTraitIds() {
        return new ACAQTraitDeclarationRefCollection(unwantedTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter("unwanted-traits")
    @JsonGetter("unwanted-traits")
    public void setUnwantedTraitIds(ACAQTraitDeclarationRefCollection ids) {
        unwantedTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            unwantedTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "unwanted-traits"));
    }

    @ACAQDocumentation(name = "Added annotations", description = "Annotations that are added to the algorithm outputs")
    @ACAQParameter("added-traits")
    @JsonGetter("added-traits")
    public ACAQTraitDeclarationRefCollection getAddedTraitIds() {
        return new ACAQTraitDeclarationRefCollection(addedTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter("added-traits")
    @JsonGetter("added-traits")
    public void setAddedTraitIds(ACAQTraitDeclarationRefCollection ids) {
        addedTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            addedTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "added-traits"));
    }

    @ACAQDocumentation(name = "Removed annotations", description = "Annotations that are removed from algorithm inputs")
    @ACAQParameter("removed-traits")
    @JsonGetter("removed-traits")
    public ACAQTraitDeclarationRefCollection getRemovedTraitIds() {
        return new ACAQTraitDeclarationRefCollection(removedTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter("removed-traits")
    @JsonGetter("removed-traits")
    public void setRemovedTraitIds(ACAQTraitDeclarationRefCollection ids) {
        removedTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            removedTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "removed-traits"));
    }

    @JsonGetter("metadata")
    @ACAQSubParameters("metadata")
    @ACAQDocumentation(name = "Algorithm metadata", description = "General metadata")
    public ACAQProjectMetadata getMetadata() {
        return metadata;
    }

    @JsonSetter("metadata")
    public void setMetadata(ACAQProjectMetadata metadata) {
        this.metadata = metadata;
    }

    @JsonGetter("graph")
    public ACAQAlgorithmGraph getGraph() {
        return graph;
    }

    @JsonSetter("graph")
    public void setGraph(ACAQAlgorithmGraph graph) {
        this.graph = graph;
        updateSlots();
    }

    private void updateSlots() {
        inputSlots.clear();
        outputSlots.clear();

        Set<String> existingSlots = new HashSet<>();
        for (ACAQDataSlot slot : graph.getUnconnectedSlots()) {
            if (slot.isInput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), " ", existingSlots::contains);
                inputSlots.add(new DefaultAlgorithmInputSlot(slot.getAcceptedDataType(), name, false));
                existingSlots.add(name);
            } else if (slot.isOutput()) {
                String name = StringUtils.makeUniqueString(slot.getName(), " ", existingSlots::contains);
                outputSlots.add(new DefaultAlgorithmOutputSlot(slot.getAcceptedDataType(), name, false));
                existingSlots.add(name);
            }
        }
    }

    /**
     * Gets the available parameters in the graph
     *
     * @return
     */
    public Map<String, ACAQParameterAccess> getAvailableParameters() {
        Map<String, ACAQParameterAccess> parameterAccessMap = new HashMap<>();
        for (ACAQAlgorithm algorithm : graph.traverseAlgorithms()) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQParameterAccess.getParameters(algorithm).entrySet()) {
                String newId = algorithm.getIdInGraph() + "/" + entry.getKey();
                parameterAccessMap.put(newId, entry.getValue());
            }
        }
        return parameterAccessMap;
    }

    @JsonGetter("acaq:project-type")
    public String getProjectType() {
        return "graph-wrapper-algorithm";
    }

    @Override
    public void reportValidity(ACAQValidityReport report) {
        if (id == null || id.isEmpty()) {
            report.reportIsInvalid("ID is null or empty! Please provide a valid algorithm ID.");
        } else if (ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id)) {
            report.reportIsInvalid("The ID already exists! Please provide a unique ID.");
        }
        if (category == ACAQAlgorithmCategory.Internal) {
            report.reportIsInvalid("The category cannot be 'Internal'! Please choose another algorithm category.");
        }
    }

    @ACAQDocumentation(name = "Exported parameters", description = "Determines which parameters are exported to the users.")
    @ACAQParameter("parameter-visibilities")
    @JsonGetter("parameter-visibilities")
    public ACAQParameterCollectionVisibilities getParameterCollectionVisibilities() {
        if (graph != null && parameterCollectionVisibilities.getAvailableParameters() == null)
            parameterCollectionVisibilities.setAvailableParameters(getAvailableParameters());
        return parameterCollectionVisibilities;
    }

    @ACAQParameter("parameter-visibilities")
    @JsonSetter("parameter-visibilities")
    public void setParameterCollectionVisibilities(ACAQParameterCollectionVisibilities parameterCollectionVisibilities) {
        this.parameterCollectionVisibilities = parameterCollectionVisibilities;
        getEventBus().post(new ParameterChangedEvent(this, "parameter-visibilities"));
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
    }

    @ACAQParameter("menu-path")
    @JsonGetter("menu-path")
    @ACAQDocumentation(name = "Menu path", description = "Menu path where the algorithm is placed. Each new line corresponds to one menu item")
    @StringParameterSettings(multiline = true)
    @Override
    public String getMenuPath() {
        return menuPath;
    }

    @ACAQParameter("menu-path")
    @JsonSetter("menu-path")
    public void setMenuPath(String menuPath) {
        this.menuPath = menuPath;
    }
}
