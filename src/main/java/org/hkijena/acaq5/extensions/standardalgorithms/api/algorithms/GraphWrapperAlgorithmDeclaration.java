package org.hkijena.acaq5.extensions.standardalgorithms.api.algorithms;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQProjectMetadata;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.traits.ACAQDataSlotTraitConfiguration;
import org.hkijena.acaq5.api.data.traits.ACAQTraitModificationOperation;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.api.registries.ACAQTraitRegistry;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclaration;
import org.hkijena.acaq5.api.traits.ACAQTraitDeclarationRefList;
import org.hkijena.acaq5.extensions.parameters.collections.StringListParameter;
import org.hkijena.acaq5.extensions.parameters.editors.ACAQTraitParameterSettings;
import org.hkijena.acaq5.extensions.parameters.editors.StringParameterSettings;
import org.hkijena.acaq5.extensions.parameters.references.ACAQTraitDeclarationRef;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Declaration of a {@link GraphWrapperAlgorithm}
 */
public class GraphWrapperAlgorithmDeclaration implements ACAQAlgorithmDeclaration, ACAQValidatable, ACAQParameterCollection {

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
    private Map<ACAQDataSlot, String> exportedSlotNames = new HashMap<>();
    private StringListParameter menuPath = new StringListParameter();
    private boolean hidden = false;

    /**
     * Creates a new declaration
     */
    public GraphWrapperAlgorithmDeclaration() {
        graph.getEventBus().register(this);
    }

    @ACAQDocumentation(name = "Algorithm ID", description = "An unique identifier for the algorithm. " +
            "We recommend to make the ID follow a structuring schema that makes it easy to create extensions or alternatives to this algorithm. " +
            "For example filter-blur-gaussian2d")
    @ACAQParameter(value = "id", uiOrder = -999)
    @JsonGetter("id")
    @StringParameterSettings(monospace = true)
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
    public Class<? extends ACAQGraphNode> getAlgorithmClass() {
        return GraphWrapperAlgorithm.class;
    }

    @Override
    public ACAQGraphNode newInstance() {
        return new GraphWrapperAlgorithm(this);
    }

    @Override
    public ACAQGraphNode clone(ACAQGraphNode algorithm) {
        return new GraphWrapperAlgorithm((GraphWrapperAlgorithm) algorithm);
    }

    public Map<ACAQDataSlot, String> getExportedSlotNames() {
        return exportedSlotNames;
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

    @Override
    public Set<ACAQDependency> getDependencies() {
        Set<ACAQDependency> result = new HashSet<>(graph.getDependencies());
        for (ACAQTraitDeclaration declaration : preferredTraits) {
            result.add(ACAQTraitRegistry.getInstance().getSourceOf(declaration.getId()));
            result.addAll(declaration.getDependencies());
        }
        for (ACAQTraitDeclaration declaration : unwantedTraits) {
            result.add(ACAQTraitRegistry.getInstance().getSourceOf(declaration.getId()));
            result.addAll(declaration.getDependencies());
        }
        for (ACAQTraitDeclaration declaration : addedTraits) {
            result.add(ACAQTraitRegistry.getInstance().getSourceOf(declaration.getId()));
            result.addAll(declaration.getDependencies());
        }
        for (ACAQTraitDeclaration declaration : removedTraits) {
            result.add(ACAQTraitRegistry.getInstance().getSourceOf(declaration.getId()));
            result.addAll(declaration.getDependencies());
        }
        return result;
    }

    @ACAQDocumentation(name = "Preferred annotations", description = "Marks the algorithm as good for handling the specified annotations")
    @ACAQParameter(value = "preferred-traits", uiOrder = 100)
    @JsonGetter("preferred-traits")
    @ACAQTraitParameterSettings(showHidden = true)
    public ACAQTraitDeclarationRefList getPreferredTraitIds() {
        return new ACAQTraitDeclarationRefList(preferredTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter("preferred-traits")
    @JsonGetter("preferred-traits")
    public void setPreferredTraitIds(ACAQTraitDeclarationRefList ids) {
        preferredTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            preferredTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "preferred-traits"));
    }

    @ACAQDocumentation(name = "Unwanted annotations", description = "Marks the algorithm as bad for handling the specified annotations")
    @ACAQParameter(value = "unwanted-traits", uiOrder = 100)
    @JsonGetter("unwanted-traits")
    @ACAQTraitParameterSettings(showHidden = true)
    public ACAQTraitDeclarationRefList getUnwantedTraitIds() {
        return new ACAQTraitDeclarationRefList(unwantedTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter("unwanted-traits")
    @JsonGetter("unwanted-traits")
    public void setUnwantedTraitIds(ACAQTraitDeclarationRefList ids) {
        unwantedTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            unwantedTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "unwanted-traits"));
    }

    @ACAQDocumentation(name = "Added annotations", description = "Annotations that are added to the algorithm outputs")
    @ACAQParameter(value = "added-traits", uiOrder = 200)
    @JsonGetter("added-traits")
    @ACAQTraitParameterSettings(showHidden = true)
    public ACAQTraitDeclarationRefList getAddedTraitIds() {
        return new ACAQTraitDeclarationRefList(addedTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter("added-traits")
    @JsonGetter("added-traits")
    public void setAddedTraitIds(ACAQTraitDeclarationRefList ids) {
        addedTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            addedTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "added-traits"));
    }

    @ACAQDocumentation(name = "Removed annotations", description = "Annotations that are removed from algorithm inputs")
    @ACAQParameter("removed-traits")
    @JsonGetter("removed-traits")
    @ACAQTraitParameterSettings(showHidden = true)
    public ACAQTraitDeclarationRefList getRemovedTraitIds() {
        return new ACAQTraitDeclarationRefList(removedTraits.stream().map(ACAQTraitDeclarationRef::new).collect(Collectors.toList()));
    }

    @ACAQParameter(value = "removed-traits", uiOrder = 200)
    @JsonGetter("removed-traits")
    public void setRemovedTraitIds(ACAQTraitDeclarationRefList ids) {
        removedTraits.clear();
        for (ACAQTraitDeclarationRef declarationRef : ids) {
            removedTraits.add(declarationRef.getDeclaration());
        }
        getEventBus().post(new ParameterChangedEvent(this, "removed-traits"));
    }

    @JsonGetter("metadata")
    @ACAQParameter("metadata")
    @ACAQDocumentation(name = "Algorithm metadata", description = "Use the following settings to provide some general metadata about the algorithm.")
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
        if (graph != this.graph) {
            if (this.graph != null) {
                this.graph.getEventBus().unregister(this);
            }
            this.graph = graph;
            updateSlots();
            this.graph.getEventBus().register(this);
        }
    }

    /**
     * Triggered when the wrapped graph changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
        updateSlots();
        parameterCollectionVisibilities.setAvailableParameters(getAvailableParameters());
        getEventBus().post(new ParameterChangedEvent(this, "parameter-visibilities"));
    }

    /**
     * Triggered when the parameter structure of an algorithm is changed
     * Updates the list of available parameters
     *
     * @param event generated event
     */
    @Subscribe
    public void onGraphParameterStructureChanged(ParameterStructureChangedEvent event) {
        parameterCollectionVisibilities.setAvailableParameters(getAvailableParameters());
        getEventBus().post(new ParameterChangedEvent(this, "parameter-visibilities"));
    }

    private void updateSlots() {
        inputSlots.clear();
        outputSlots.clear();

        Set<String> existingSlots = new HashSet<>();
        Map<ACAQGraphNode, List<ACAQDataSlot>> groupedByAlgorithm = graph.getUnconnectedSlots().stream().collect(Collectors.groupingBy(ACAQDataSlot::getAlgorithm));
        exportedSlotNames.clear();
        for (Map.Entry<ACAQGraphNode, List<ACAQDataSlot>> entry : groupedByAlgorithm.entrySet()) {
            for (ACAQDataSlot slot : entry.getValue()) {
                if (slot.isInput()) {
                    String name = StringUtils.makeUniqueString(slot.getName(), " ", existingSlots::contains);
                    inputSlots.add(new DefaultAlgorithmInputSlot(slot.getAcceptedDataType(), name, false));
                    existingSlots.add(name);
                    exportedSlotNames.put(slot, name);
                } else if (slot.isOutput()) {
                    String name = StringUtils.makeUniqueString(slot.getName(), " ", existingSlots::contains);
                    outputSlots.add(new DefaultAlgorithmOutputSlot(slot.getAcceptedDataType(), name, "", false));
                    existingSlots.add(name);
                    exportedSlotNames.put(slot, name);
                }
            }
        }
    }

    /**
     * Gets the available parameters in the graph
     *
     * @return the parameters
     */
    public Map<String, ACAQParameterAccess> getAvailableParameters() {
        Map<String, ACAQParameterAccess> parameterAccessMap = new HashMap<>();
        for (ACAQGraphNode algorithm : graph.traverseAlgorithms()) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQTraversedParameterCollection.getParameters(algorithm).entrySet()) {
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
            report.reportIsInvalid("ID is null or empty!",
                    "Algorithms must have a unique and non-empty ID.",
                    "Please provide a valid algorithm ID.",
                    this);
        } else if (ACAQAlgorithmRegistry.getInstance().hasAlgorithmWithId(id) && ACAQAlgorithmRegistry.getInstance().getDeclarationById(id) != this) {
            report.reportIsInvalid("The ID already exists!",
                    "Algorithms must have a unique and non-empty ID.",
                    "Please provide a unique ID.",
                    this);
        }
        if (category == ACAQAlgorithmCategory.Internal) {
            report.reportIsInvalid("The category cannot be 'Internal'!",
                    "This is reserved for algorithm nodes used by ACAQ to control program flow.",
                    "Please choose another algorithm category.",
                    this);
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


    @Override
    @JsonGetter("menu-path")
    public String getMenuPath() {
        return String.join("\n", menuPath);
    }

    @JsonSetter("menu-path")
    public void setMenuPath(String value) {
        menuPath.clear();
        menuPath.addAll(Arrays.asList(StringUtils.getCleanedMenuPath(value).split("\n")));
    }

    @ACAQParameter("menu-path")
    @ACAQDocumentation(name = "Menu path", description = "Menu path where the algorithm is placed. " +
            "If you leave this empty, the menu item will be placed in the category's root menu.")
    public StringListParameter getMenuPathList() {
        return menuPath;
    }


    @ACAQParameter("menu-path")
    public void setMenuPathList(StringListParameter value) {
        this.menuPath = value;
        getEventBus().post(new ParameterChangedEvent(this, "menu-path"));
    }

    @ACAQParameter("hidden")
    @ACAQDocumentation(name = "Is hidden", description = "If the algorithm should not appear in the list of available algorithms.")
    @Override
    public boolean isHidden() {
        return hidden;
    }

    @ACAQParameter("hidden")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;
        getEventBus().post(new ParameterChangedEvent(this, "hidden"));
    }
}
