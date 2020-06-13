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
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.parameters.*;
import org.hkijena.acaq5.api.registries.ACAQAlgorithmRegistry;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.extensions.parameters.references.ACAQAlgorithmIconRef;
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
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();
    private ACAQParameterCollectionVisibilities parameterCollectionVisibilities = new ACAQParameterCollectionVisibilities();
    private ACAQAlgorithmGraph graph = new ACAQAlgorithmGraph();
    private Map<ACAQDataSlot, String> exportedSlotNames = new HashMap<>();
    private StringList menuPath = new StringList();
    private boolean hidden = false;
    private ACAQAlgorithmIconRef icon = new ACAQAlgorithmIconRef();

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
    public List<AlgorithmInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<AlgorithmOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public Set<ACAQDependency> getDependencies() {
        return new HashSet<>(graph.getDependencies());
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
    public StringList getMenuPathList() {
        return menuPath;
    }


    @ACAQParameter("menu-path")
    public void setMenuPathList(StringList value) {
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

    @ACAQDocumentation(name = "Icon", description = "A custom algorithm icon")
    @ACAQParameter("icon")
    @JsonGetter("icon")
    public ACAQAlgorithmIconRef getIcon() {
        return icon;
    }

    @ACAQParameter("icon")
    public void setIcon(ACAQAlgorithmIconRef icon) {
        this.icon = icon;
        getEventBus().post(new ParameterChangedEvent(this, "icon"));
    }
}
