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
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.JIPipeDependency;
import org.hkijena.jipipe.api.SetJIPipeDocumentation;
import org.hkijena.jipipe.api.data.*;
import org.hkijena.jipipe.api.grouping.parameters.GraphNodeParameterReferenceGroupCollection;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.api.validation.*;
import org.hkijena.jipipe.api.validation.contexts.JsonNodeInfoValidationReportContext;
import org.hkijena.jipipe.api.validation.contexts.ParameterValidationReportContext;
import org.hkijena.jipipe.extensions.parameters.library.jipipe.DynamicCategoryEnumParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.parameters.library.references.IconRef;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.*;

/**
 * Info of a {@link JIPipeGraphWrapperAlgorithm}
 */
public class JsonNodeInfo extends AbstractJIPipeParameterCollection implements JIPipeNodeInfo, JIPipeValidatable, JIPipeGraph.GraphChangedEventListener {
    private final List<AddJIPipeInputSlot> inputSlots = new ArrayList<>();
    private final List<AddJIPipeOutputSlot> outputSlots = new ArrayList<>();
    private final Map<JIPipeDataSlot, String> exportedSlotNames = new HashMap<>();
    private String id;
    private String name;
    private HTMLText description = new HTMLText();
    private JIPipeGraph graph = new JIPipeGraph();
    private StringList menuPath = new StringList();
    private boolean hidden = false;
    private boolean deprecated =false;
    private IconRef customIcon = new IconRef();
    private GraphWrapperAlgorithmInput algorithmInput;
    private GraphWrapperAlgorithmOutput algorithmOutput;
    private GraphNodeParameterReferenceGroupCollection exportedParameters;
    private DynamicCategoryEnumParameter category = new DynamicCategoryEnumParameter();


    /**
     * Creates a new info
     */
    public JsonNodeInfo() {
        category.setValue((new MiscellaneousNodeTypeCategory()).getId());
        exportedParameters = new GraphNodeParameterReferenceGroupCollection();
        exportedParameters.setGraph(getGraph());
        graph.getGraphChangedEventEmitter().subscribe(this);
    }

    /**
     * Creates a new info from a {@link NodeGroup}
     *
     * @param group the node group. The graph will NOT be copied.
     */
    public JsonNodeInfo(NodeGroup group) {
        graph = new JIPipeGraph(group.getWrappedGraph());
        exportedParameters = new GraphNodeParameterReferenceGroupCollection(group.getExportedParameters());
        exportedParameters.setGraph(getGraph());
        graph.getGraphChangedEventEmitter().subscribe(this);
        category.setValue((new MiscellaneousNodeTypeCategory()).getId());
        setName(group.getName());
        setDescription(group.getCustomDescription());
        updateSlots();
    }

    @SetJIPipeDocumentation(name = "Algorithm ID", description = "An unique identifier for the algorithm. " +
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
        return new JsonAlgorithm((JIPipeGraphWrapperAlgorithm) algorithm);
    }

    public Map<JIPipeDataSlot, String> getExportedSlotNames() {
        return exportedSlotNames;
    }

    @Override
    @SetJIPipeDocumentation(name = "Name", description = "The algorithm name")
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
    @SetJIPipeDocumentation(name = "Description", description = "A description for the algorithm. You can use " +
            "HTML for your descriptions.")
    @JIPipeParameter(value = "description", uiOrder = 10)
    @StringParameterSettings(multiline = true)
    @JsonGetter("description")
    public HTMLText getDescription() {
        if (description == null)
            description = new HTMLText();
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

    @SetJIPipeDocumentation(name = "Category", description = "A general category for the algorithm. " +
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
    public List<AddJIPipeInputSlot> getInputSlots() {
        return inputSlots;
    }

    @Override
    public List<AddJIPipeOutputSlot> getOutputSlots() {
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
                this.graph.getGraphChangedEventEmitter().unsubscribe(this);
            }
            this.graph = graph;
            if (exportedParameters != null) {
                exportedParameters.setGraph(graph);
            }
            updateSlots();
            this.graph.getGraphChangedEventEmitter().unsubscribe(this);
        }
    }

    @SetJIPipeDocumentation(name = "Exported parameters", description = "Allows you to export parameters from the contained nodes into the custom node")
    @JIPipeParameter("exported-parameters")
    @JsonGetter("exported-parameters")
    public GraphNodeParameterReferenceGroupCollection getExportedParameters() {
        return exportedParameters;
    }

    @JIPipeParameter("exported-parameters")
    @JsonSetter("exported-parameters")
    public void setExportedParameters(GraphNodeParameterReferenceGroupCollection exportedParameters) {
        this.exportedParameters = exportedParameters;
        exportedParameters.setGraph(getGraph());
    }

    /**
     * Triggered when the wrapped graph changed
     *
     * @param event generated event
     */
    @Override
    public void onGraphChanged(JIPipeGraph.GraphChangedEvent event) {
        updateSlots();
    }

    private void updateSlots() {
        inputSlots.clear();
        outputSlots.clear();
        Set<String> usedSlotNames = new HashSet<>();
        JIPipeMutableSlotConfiguration inputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupInput().getSlotConfiguration();
        JIPipeMutableSlotConfiguration outputSlotConfiguration = (JIPipeMutableSlotConfiguration) getGroupOutput().getSlotConfiguration();
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : inputSlotConfiguration.getInputSlots().entrySet()) {
            if (entry.getValue().getSlotType() == JIPipeSlotType.Input) {
                inputSlots.add(new DefaultAddJIPipeInputSlot(entry.getValue().getDataClass(),
                        entry.getKey(),
                        "", true, false, JIPipeDataSlotRole.Data));
                usedSlotNames.add(entry.getKey());
            }
        }
        for (Map.Entry<String, JIPipeDataSlotInfo> entry : outputSlotConfiguration.getOutputSlots().entrySet()) {
            if (entry.getValue().getSlotType() == JIPipeSlotType.Output) {
                if (!usedSlotNames.contains(entry.getKey())) {
                    outputSlots.add(new DefaultAddJIPipeOutputSlot(entry.getValue().getDataClass(),
                            entry.getKey(),
                            "", null,
                            true, JIPipeDataSlotRole.Data));
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
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                if (node instanceof GraphWrapperAlgorithmInput) {
                    algorithmInput = (GraphWrapperAlgorithmInput) node;
                    break;
                }
            }
        }
        if (algorithmInput == null) {
            // Create if it doesn't exist
            algorithmInput = JIPipe.createNode("graph-wrapper:input");
            graph.insertNode(algorithmInput);
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
            for (JIPipeGraphNode node : graph.getGraphNodes()) {
                if (node instanceof GraphWrapperAlgorithmOutput) {
                    algorithmOutput = (GraphWrapperAlgorithmOutput) node;
                    break;
                }
            }
        }
        if (algorithmOutput == null) {
            // Create if it doesn't exist
            algorithmOutput = JIPipe.createNode("graph-wrapper:output");
            graph.insertNode(algorithmOutput);
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
                String newId = algorithm.getUUIDInParentGraph() + "/" + entry.getKey();
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
    public void reportValidity(JIPipeValidationReportContext reportContext, JIPipeValidationReport report) {
        if (id == null || id.isEmpty()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new JsonNodeInfoValidationReportContext(this),
                    "ID is null or empty!",
                    "Algorithms must have a unique and non-empty ID.",
                    "Please provide a valid algorithm ID."));
        }
        if (!getCategory().userCanCreate() || !getCategory().userCanDelete()) {
            report.add(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new JsonNodeInfoValidationReportContext(this),
                    "The selected category is reserved for internal usage!",
                    "This is reserved for algorithm nodes used by JIPipe to control program flow.",
                    "Please choose another algorithm category."));
        }
        report.report(new ParameterValidationReportContext(this, "Exported parameters", "exported-parameters"), exportedParameters);

        // Only check if the graph creates a valid group output
        report.report(new ParameterValidationReportContext(this, "Wrapped graph", "wrapped-graph"), getGraph());
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
    @SetJIPipeDocumentation(name = "Menu path", description = "Menu path where the algorithm is placed. " +
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
    @SetJIPipeDocumentation(name = "Is hidden", description = "If the algorithm should not appear in the list of available algorithms.")
    @Override
    public boolean isHidden() {
        return hidden;
    }

    @JIPipeParameter("hidden")
    public void setHidden(boolean hidden) {
        this.hidden = hidden;

    }

    @SetJIPipeDocumentation(name = "Icon", description = "A custom algorithm icon")
    @JIPipeParameter(value = "icon", uiOrder = 25)
    @JsonGetter("icon")
    public IconRef getCustomIcon() {
        if (customIcon == null)
            customIcon = new IconRef();
        return customIcon;
    }

    @JIPipeParameter("icon")
    public void setCustomIcon(IconRef customIcon) {
        this.customIcon = customIcon;

    }

    @SetJIPipeDocumentation(name = "Deprecated", description = "Marks the node as deprecated")
    @JIPipeParameter("deprecated")
    @JsonGetter("deprecated")
    @Override
    public boolean isDeprecated() {
        return deprecated;
    }

    @JIPipeParameter("deprecated")
    @JsonSetter("deprecated")
    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }
}
