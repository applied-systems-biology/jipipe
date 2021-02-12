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

package org.hkijena.jipipe.api.grouping;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeValidatable;
import org.hkijena.jipipe.api.JIPipeValidityReport;
import org.hkijena.jipipe.api.data.JIPipeDataSlot;
import org.hkijena.jipipe.api.data.JIPipeDataSlotInfo;
import org.hkijena.jipipe.api.data.JIPipeMutableSlotConfiguration;
import org.hkijena.jipipe.api.data.JIPipeSlotType;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameters;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.enums.DynamicCategoryEnumParameter;
import org.hkijena.jipipe.extensions.parameters.primitives.HTMLText;
import org.hkijena.jipipe.extensions.parameters.primitives.StringList;
import org.hkijena.jipipe.extensions.parameters.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.references.JIPipeAlgorithmIconRef;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Info of a {@link GraphWrapperAlgorithm}
 */
public class JsonNodeInfo implements JIPipeNodeInfo, JIPipeValidatable, JIPipeParameterCollection {

    private final EventBus eventBus = new EventBus();
    private String id;
    private String name;
    private HTMLText description;
    private List<JIPipeInputSlot> inputSlots = new ArrayList<>();
    private List<JIPipeOutputSlot> outputSlots = new ArrayList<>();
    private JIPipeGraph graph = new JIPipeGraph();
    private Map<JIPipeDataSlot, String> exportedSlotNames = new HashMap<>();
    private StringList menuPath = new StringList();
    private boolean hidden = false;
    private JIPipeAlgorithmIconRef icon = new JIPipeAlgorithmIconRef();
    private GraphWrapperAlgorithmInput algorithmInput;
    private GraphWrapperAlgorithmOutput algorithmOutput;
    private GraphNodeParameters exportedParameters;
    private DynamicCategoryEnumParameter category = new DynamicCategoryEnumParameter();


    /**
     * Creates a new info
     */
    public JsonNodeInfo() {
        category.setValue((new MiscellaneousNodeTypeCategory()).getId());
        exportedParameters = new GraphNodeParameters();
        exportedParameters.setGraph(getGraph());
        graph.getEventBus().register(this);
    }

    /**
     * Creates a new {@link JsonNodeInfo} from a {@link NodeGroup}
     *
     * @param group the node group. The graph will NOT be copied.
     */
    public JsonNodeInfo(NodeGroup group) {
        graph = new JIPipeGraph(group.getWrappedGraph());
        exportedParameters = new GraphNodeParameters();
        exportedParameters.setGraph(getGraph());
        graph.getEventBus().register(this);
        category.setValue((new MiscellaneousNodeTypeCategory()).getId());
        setName(group.getName());
        setDescription(group.getCustomDescription());
        updateSlots();
    }

    @JIPipeDocumentation(name = "Algorithm ID", description = "An unique identifier for the algorithm. " +
            "We recommend to make the ID follow a structuring schema that makes it easy to create extensions or alternatives to this algorithm. " +
            "For example filter-blur-gaussian2d")
    @JIPipeParameter(value = "id", uiOrder = -999)
    @JsonGetter("id")
    @StringParameterSettings(monospace = true)
    @Override
    public String getId() {
        return id;
    }

    @JIPipeParameter("id")
    @JsonSetter("id")
    public void setId(String id) {
        this.id = id;

    }

    @Override
    public Class<? extends JIPipeGraphNode> getInstanceClass() {
        return JsonAlgorithm.class;
    }

    @Override
    public JIPipeGraphNode newInstance() {
        return new JsonAlgorithm(this);
    }

    @Override
    public JIPipeGraphNode duplicate(JIPipeGraphNode algorithm) {
        return new JsonAlgorithm((GraphWrapperAlgorithm) algorithm);
    }

    public Map<JIPipeDataSlot, String> getExportedSlotNames() {
        return exportedSlotNames;
    }

    @Override
    @JIPipeDocumentation(name = "Name", description = "The algorithm name")
    @JIPipeParameter(value = "name", uiOrder = 0)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @Override
    @JIPipeDocumentation(name = "Description", description = "A description for the algorithm. You can use " +
            "HTML for your descriptions.")
    @JIPipeParameter(value = "description", uiOrder = 10)
    @StringParameterSettings(multiline = true)
    @JsonGetter("description")
    public HTMLText getDescription() {
        return description;
    }

    @JsonSetter("description")
    @JIPipeParameter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    @Override
    public JIPipeNodeTypeCategory getCategory() {
        if (category != null && category.getValue() != null && !StringUtils.isNullOrEmpty("" + category.getValue())) {
            if (JIPipe.getInstance() != null && JIPipe.getInstance().getNodeRegistry() != null) {
                JIPipeNodeTypeCategory result = JIPipe.getNodes().getRegisteredCategories().getOrDefault("" + category.getValue(), null);
                if (result != null)
                    return result;
            }
        }
        return new MiscellaneousNodeTypeCategory();
    }

    @JIPipeDocumentation(name = "Category", description = "A general category for the algorithm. " +
            "This will influence in which menu the algorithm is put.")
    @JIPipeParameter(value = "category", uiOrder = 20)
    @JsonGetter("category")
    public DynamicCategoryEnumParameter getCategoryParameter() {
        if (category != null) {
            if (JIPipe.getInstance() != null && JIPipe.getInstance().getNodeRegistry() != null) {
                if (category.getAllowedValues() == null)
                    category.setAllowedValues(new ArrayList<>());
                category.getAllowedValues().clear();
                category.getAllowedValues().addAll(JIPipe.getNodes().getRegisteredCategories().keySet());
            }
        }
        return category;
    }

    @JIPipeParameter("category")
    @JsonSetter("category")
    public void setCategoryParameter(DynamicCategoryEnumParameter parameter) {
        this.category = parameter;
    }

    @Override
    public List<JIPipeInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<JIPipeOutputSlot> getOutputSlots() {
        return outputSlots;
    }

    @Override
    public Set<JIPipeDependency> getDependencies() {
        return new HashSet<>(graph.getDependencies());
    }

    @JsonGetter("graph")
    public JIPipeGraph getGraph() {
        return graph;
    }

    @JsonSetter("graph")
    public void setGraph(JIPipeGraph graph) {
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
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
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
        JIPipeMutableSlotConfiguration inputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
        JIPipeMutableSlotConfiguration outputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : inputSlotConfiguration.getInputSlots().entrySet()) {
            if (entry.getValue().getSlotType() == JIPipeSlotType.Input) {
                inputSlots.add(new DefaultJIPipeInputSlot(entry.getValue().getDataClass(),
                        entry.getKey(),
                        true, false));
                usedSlotNames.add(entry.getKey());
            }
        }
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : outputSlotConfiguration.getOutputSlots().entrySet()) {
            if (entry.getValue().getSlotType() == JIPipeSlotType.Output) {
                if (!usedSlotNames.contains(entry.getKey())) {
                    outputSlots.add(new DefaultJIPipeOutputSlot(entry.getValue().getDataClass(),
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
            for (JIPipeGraphNode node : graph.getNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmInput) {
                    algorithmInput = (GraphWrapperAlgorithmInput) node;
                    break;
                }
            }
        }
        if (algorithmInput == null) {
            // Create if it doesn't exist
            algorithmInput = JIPipe.createNode("graph-wrapper:input", GraphWrapperAlgorithmInput.class);
            graph.insertNode(algorithmInput, JIPipeGraph.COMPARTMENT_DEFAULT);
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
            for (JIPipeGraphNode node : graph.getNodes().values()) {
                if (node instanceof GraphWrapperAlgorithmOutput) {
                    algorithmOutput = (GraphWrapperAlgorithmOutput) node;
                    break;
                }
            }
        }
        if (algorithmOutput == null) {
            // Create if it doesn't exist
            algorithmOutput = JIPipe.createNode("graph-wrapper:output", GraphWrapperAlgorithmOutput.class);
            graph.insertNode(algorithmOutput, JIPipeGraph.COMPARTMENT_DEFAULT);
        }
        return algorithmOutput;
    }

    /**
     * Gets the available parameters in the graph
     *
     * @return the parameters
     */
    public Map<String, JIPipeParameterAccess> getAvailableParameters() {
        Map<String, JIPipeParameterAccess> parameterAccessMap = new HashMap<>();
        for (JIPipeGraphNode algorithm : graph.traverse()) {
            for (Map.Entry<String, JIPipeParameterAccess> entry : JIPipeParameterTree.getParameters(algorithm).entrySet()) {
                String newId = algorithm.getIdInGraph() + "/" + entry.getKey();
                parameterAccessMap.put(newId, entry.getValue());
            }
        }
        return parameterAccessMap;
    }

    @JsonGetter("jipipe:project-type")
    public String getProjectType() {
        return "graph-wrapper-algorithm";
    }

    @Override
    public void reportValidity(JIPipeValidityReport report) {
        if (id == null || id.isEmpty()) {
            report.reportIsInvalid("ID is null or empty!",
                    "Algorithms must have a unique and non-empty ID.",
                    "Please provide a valid algorithm ID.",
                    this);
        }
        if (!getCategory().userCanCreate() || !getCategory().userCanDelete()) {
            report.reportIsInvalid("The selected category is reserved for internal usage!",
                    "This is reserved for algorithm nodes used by JIPipe to control program flow.",
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

    @JIPipeParameter(value = "menu-path", uiOrder = 30)
    @JIPipeDocumentation(name = "Menu path", description = "Menu path where the algorithm is placed. " +
            "If you leave this empty, the menu item will be placed in the category's root menu.")
    @StringParameterSettings(monospace = true)
    public StringList getMenuPathList() {
        return menuPath;
    }


    @JIPipeParameter("menu-path")
    public void setMenuPathList(StringList value) {
        this.menuPath = value;

    }

    @JIPipeParameter(value = "hidden", uiOrder = 40)
    @JIPipeDocumentation(name = "Is hidden", description = "If the algorithm should not appear in the list of available algorithms.")
    @Override
    public boolean isHidden() {
        return hidden;
    }

    @JIPipeParameter("hidden")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;

    }

    @JIPipeDocumentation(name = "Icon", description = "A custom algorithm icon")
    @JIPipeParameter(value = "icon", uiOrder = 25)
    @JsonGetter("icon")
    public JIPipeAlgorithmIconRef getIcon() {
        if (icon == null)
            icon = new JIPipeAlgorithmIconRef();
        return icon;
    }

    @JIPipeParameter("icon")
    public void setIcon(JIPipeAlgorithmIconRef icon) {
        this.icon = icon;

    }
}
