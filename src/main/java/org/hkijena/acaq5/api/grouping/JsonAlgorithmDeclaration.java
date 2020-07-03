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

package org.hkijena.acaq5.api.grouping;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQDependency;
import org.hkijena.acaq5.api.ACAQDocumentation;
import org.hkijena.acaq5.api.ACAQValidatable;
import org.hkijena.acaq5.api.ACAQValidityReport;
import org.hkijena.acaq5.api.algorithm.*;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.data.ACAQSlotDefinition;
import org.hkijena.acaq5.api.data.ACAQSlotType;
import org.hkijena.acaq5.api.events.AlgorithmGraphChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.ParameterStructureChangedEvent;
import org.hkijena.acaq5.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.acaq5.api.parameters.ACAQParameter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterCollection;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.extensions.parameters.primitives.StringList;
import org.hkijena.acaq5.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.acaq5.extensions.parameters.references.ACAQAlgorithmIconRef;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Declaration of a {@link GraphWrapperAlgorithm}
 */
public class JsonAlgorithmDeclaration implements ACAQAlgorithmDeclaration, ACAQValidatable, ACAQParameterCollection {

    private final EventBus eventBus = new EventBus();
    private String id;
    private String name;
    private String description;
    private ACAQAlgorithmCategory category = ACAQAlgorithmCategory.Miscellaneous;
    private List<AlgorithmInputSlot> inputSlots = new ArrayList<>();
    private List<AlgorithmOutputSlot> outputSlots = new ArrayList<>();
    private ACAQGraph graph = new ACAQGraph();
    private Map<ACAQDataSlot, String> exportedSlotNames = new HashMap<>();
    private StringList menuPath = new StringList();
    private boolean hidden = false;
    private ACAQAlgorithmIconRef icon = new ACAQAlgorithmIconRef();
    private GraphWrapperAlgorithmInput algorithmInput;
    private GraphWrapperAlgorithmOutput algorithmOutput;
    private GraphNodeParameters exportedParameters;


    /**
     * Creates a new declaration
     */
    public JsonAlgorithmDeclaration() {
        exportedParameters = new GraphNodeParameters();
        exportedParameters.setGraph(getGraph());
        graph.getEventBus().register(this);
    }

    /**
     * Creates a new {@link JsonAlgorithmDeclaration} from a {@link NodeGroup}
     *
     * @param group the node group. The graph will NOT be copied.
     */
    public JsonAlgorithmDeclaration(NodeGroup group) {
        graph = new ACAQGraph(group.getWrappedGraph());
        exportedParameters = new GraphNodeParameters();
        exportedParameters.setGraph(getGraph());
        graph.getEventBus().register(this);
        setName(group.getName());
        setDescription(group.getCustomDescription());
        updateSlots();
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

    }

    @Override
    public Class<? extends ACAQGraphNode> getAlgorithmClass() {
        return JsonAlgorithm.class;
    }

    @Override
    public ACAQGraphNode newInstance() {
        return new JsonAlgorithm(this);
    }

    @Override
    public ACAQGraphNode clone(ACAQGraphNode algorithm) {
        return new JsonAlgorithm((GraphWrapperAlgorithm) algorithm);
    }

    public Map<ACAQDataSlot, String> getExportedSlotNames() {
        return exportedSlotNames;
    }

    @Override
    @ACAQDocumentation(name = "Name", description = "The algorithm name")
    @ACAQParameter(value = "name", uiOrder = 0)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @ACAQParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @ACAQDocumentation(name = "Description", description = "A description for the algorithm. You can use " +
            "HTML for your descriptions.")
    @ACAQParameter(value = "description", uiOrder = 10)
    @StringParameterSettings(multiline = true)
    @JsonGetter("description")
    public String getDescription() {
        return description;
    }

    @JsonSetter("description")
    @ACAQParameter("description")
    public void setDescription(String description) {
        this.description = description;
    }

    @ACAQDocumentation(name = "Category", description = "A general category for the algorithm. " +
            "This will influence in which menu the algorithm is put.")
    @ACAQParameter(value = "category", uiOrder = 20)
    @JsonGetter("category")
    @Override
    public ACAQAlgorithmCategory getCategory() {
        return category;
    }

    @ACAQParameter("category")
    @JsonSetter("category")
    public void setCategory(ACAQAlgorithmCategory category) {
        this.category = category;

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

    @JsonGetter("graph")
    public ACAQGraph getGraph() {
        return graph;
    }

    @JsonSetter("graph")
    public void setGraph(ACAQGraph graph) {
        if (graph != this.graph) {
            if (this.graph != null) {
                this.graph.getEventBus().unregister(this);
            }
            this.graph = graph;
            if (exportedParameters != null) {
                exportedParameters.setGraph(graph);
            }
            updateSlots();
            this.graph.getEventBus().register(this);
        }
    }

    @JsonGetter("exported-parameters")
    public GraphNodeParameters getExportedParameters() {
        return exportedParameters;
    }

    @JsonSetter("exported-parameters")
    public void setExportedParameters(GraphNodeParameters exportedParameters) {
        this.exportedParameters = exportedParameters;
        exportedParameters.setGraph(getGraph());
    }

    /**
     * Triggered when the wrapped graph changed
     *
     * @param event generated event
     */
    @Subscribe
    public void onGraphChanged(AlgorithmGraphChangedEvent event) {
        updateSlots();
    }

    /**
     * Triggered when the parameter structure of an algorithm is changed
     * Updates the list of available parameters
     *
     * @param event generated event
     */
    @Subscribe
    public void onGraphParameterStructureChanged(ParameterStructureChangedEvent event) {
    }

    private void updateSlots() {
        inputSlots.clear();
        outputSlots.clear();
        Set<String> usedSlotNames = new HashSet<>();
        ACAQMutableSlotConfiguration inputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
        ACAQMutableSlotConfiguration outputSlotConfiguration = (ACAQMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();
        for (Map.Entry<String, ACAQSlotDefinition> entry : inputSlotConfiguration.getInputSlots().entrySet()) {
            if (entry.getValue().getSlotType() == ACAQSlotType.Input) {
                inputSlots.add(new DefaultAlgorithmInputSlot(entry.getValue().getDataClass(),
                        entry.getKey(),
                        true));
                usedSlotNames.add(entry.getKey());
            }
        }
        for (Map.Entry<String, ACAQSlotDefinition> entry : outputSlotConfiguration.getOutputSlots().entrySet()) {
            if (entry.getValue().getSlotType() == ACAQSlotType.Output) {
                if (!usedSlotNames.contains(entry.getKey())) {
                    outputSlots.add(new DefaultAlgorithmOutputSlot(entry.getValue().getDataClass(),
                            entry.getKey(),
                            null,
                            true));
                }
            }
        }
    }

    /**
     * Gets the graphs's input node
     *
     * @return the graph's input node
     */
    public GraphWrapperAlgorithmInput getGroupInput() {
        if (algorithmInput == null) {
            for (ACAQGraphNode node : graph.getAlgorithmNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmInput) {
                    algorithmInput = (GraphWrapperAlgorithmInput) node;
                    break;
                }
            }
        }
        if (algorithmInput == null) {
            // Create if it doesn't exist
            algorithmInput = ACAQAlgorithm.newInstance("graph-wrapper:input");
            graph.insertNode(algorithmInput, ACAQGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmInput;
    }

    /**
     * Gets the graphs's output node
     *
     * @return the graph's output node
     */
    public GraphWrapperAlgorithmOutput getGroupOutput() {
        if (algorithmOutput == null) {
            for (ACAQGraphNode node : graph.getAlgorithmNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmOutput) {
                    algorithmOutput = (GraphWrapperAlgorithmOutput) node;
                    break;
                }
            }
        }
        if (algorithmOutput == null) {
            // Create if it doesn't exist
            algorithmOutput = ACAQAlgorithm.newInstance("graph-wrapper:output");
            graph.insertNode(algorithmOutput, ACAQGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmOutput;
    }

    /**
     * Gets the available parameters in the graph
     *
     * @return the parameters
     */
    public Map<String, ACAQParameterAccess> getAvailableParameters() {
        Map<String, ACAQParameterAccess> parameterAccessMap = new HashMap<>();
        for (ACAQGraphNode algorithm : graph.traverseAlgorithms()) {
            for (Map.Entry<String, ACAQParameterAccess> entry : ACAQParameterTree.getParameters(algorithm).entrySet()) {
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
        }
        if (category == ACAQAlgorithmCategory.Internal) {
            report.reportIsInvalid("The category cannot be 'Internal'!",
                    "This is reserved for algorithm nodes used by ACAQ to control program flow.",
                    "Please choose another algorithm category.",
                    this);
        }
        report.forCategory("Exported parameters").report(exportedParameters);

        // Only check if the graph creates a valid group output
        getGraph().reportValidity(report.forCategory("Wrapped graph"), getGroupOutput(), Sets.newHashSet(getGroupInput()));
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

    @ACAQParameter(value = "menu-path", uiOrder = 30)
    @ACAQDocumentation(name = "Menu path", description = "Menu path where the algorithm is placed. " +
            "If you leave this empty, the menu item will be placed in the category's root menu.")
    @StringParameterSettings(monospace = true)
    public StringList getMenuPathList() {
        return menuPath;
    }


    @ACAQParameter("menu-path")
    public void setMenuPathList(StringList value) {
        this.menuPath = value;

    }

    @ACAQParameter(value = "hidden", uiOrder = 40)
    @ACAQDocumentation(name = "Is hidden", description = "If the algorithm should not appear in the list of available algorithms.")
    @Override
    public boolean isHidden() {
        return hidden;
    }

    @ACAQParameter("hidden")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;

    }

    @ACAQDocumentation(name = "Icon", description = "A custom algorithm icon")
    @ACAQParameter(value = "icon", uiOrder = 25)
    @JsonGetter("icon")
    public ACAQAlgorithmIconRef getIcon() {
        return icon;
    }

    @ACAQParameter("icon")
    public void setIcon(ACAQAlgorithmIconRef icon) {
        this.icon = icon;

    }
}
