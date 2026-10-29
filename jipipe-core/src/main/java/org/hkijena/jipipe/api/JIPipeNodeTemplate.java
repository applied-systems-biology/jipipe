/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.data.JIPipeDataInfo;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.nodes.taxonomy.TaxonomyView;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.desktop.app.grapheditor.commons.JIPipeDesktopGraphCanvasUI;
import org.hkijena.jipipe.desktop.commons.components.parameters.JIPipeDesktopParameterFormPanel;
import org.hkijena.jipipe.plugins.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.plugins.parameters.library.markup.MarkdownText;
import org.hkijena.jipipe.plugins.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.plugins.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.plugins.parameters.library.references.IconRef;
import org.hkijena.jipipe.plugins.parameters.library.taxonomy.JIPipeTaxonomyFunctionRef;
import org.hkijena.jipipe.plugins.parameters.ui.library.JIPipeDesktopIconRefParameterEditorUI;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import javax.swing.*;
import java.net.URL;
import java.util.*;

/**
 * Contains the JSON data of a node that can be created by a user for sharing
 * An intermediate between copying a node and a proper plugin.
 */
public class JIPipeNodeTemplate extends AbstractJIPipeParameterCollection {
    public static final String SOURCE_USER = "User";
    public static final String SOURCE_EXTENSION = "Extension";
    private UUID uuid = UUID.randomUUID();
    private String name = "Unnamed template";
    private HTMLText description = new HTMLText();
    /**
     * Transient legacy capture of the removed {@code menu-path} key (R7):
     * kept on deserialization so {@link #migrateLegacyMenuPath()} can map it
     * to {@link #taxonomyFunction}, but never serialized (no getter) and no
     * longer a user-facing parameter. Empty after one save cycle.
     */
    private StringList menuPath = new StringList();
    private JIPipeTaxonomyFunctionRef taxonomyFunction = new JIPipeTaxonomyFunctionRef();
    private float colorHue = 40.0f / 360.0f;
    private IconRef icon = new IconRef("actions/configure.png");
    private String data;
    private JIPipeGraph graph;
    private String source = SOURCE_USER;

    public JIPipeNodeTemplate() {
    }

    public JIPipeNodeTemplate(JIPipeNodeTemplate other) {
        copyFrom(other);
    }

    public static Map<String, Set<JIPipeNodeTemplate>> groupByMenuPaths(Collection<JIPipeNodeTemplate> infos) {
        Map<String, Set<JIPipeNodeTemplate>> result = new HashMap<>();
        for (JIPipeNodeTemplate template : infos) {
            String menuPath = String.join("\n", template.getMenuPathLegacy());
            Set<JIPipeNodeTemplate> group = result.getOrDefault(menuPath, null);
            if (group == null) {
                group = new HashSet<>();
                result.put(menuPath, group);
            }
            group.add(template);
        }
        return result;
    }

    public static void create(JIPipeDesktopGraphCanvasUI canvasUI, Set<JIPipeGraphNode> algorithms) {
        JIPipeNodeTemplate template = new JIPipeNodeTemplate();

        if (algorithms.size() == 1) {
            JIPipeGraphNode node = algorithms.iterator().next();
            template.setName(node.getName());
            URL url = JIPipe.getNodes().getIconURLFor(node.getInfo());
            if (TaxonomyView.isDataSource(node.getInfo())) {
                if (!node.getOutputSlots().isEmpty()) {
                    url = JIPipe.getDataTypes().getIconURLFor(JIPipeDataInfo.getInstance(node.getOutputSlots().get(0).getAcceptedDataType()));
                }
            }
            if (url != null) {
                String urlString = url.toString();
                String iconName = null;
                for (String icon : JIPipeDesktopIconRefParameterEditorUI.getAvailableIcons()) {
                    if (urlString.endsWith(icon)) {
                        iconName = icon;
                        break;
                    }
                }
                template.getIcon().setIconName(iconName);
            }
        }

        JIPipeGraph graph = canvasUI.getGraph();
        JIPipeGraph subGraph = graph.extract(algorithms, false);
        template.setData(JsonUtils.toPrettyJsonString(subGraph));

        int result = JOptionPane.YES_OPTION;
        if (canvasUI.getGraph().getProject() != null) {
            result = JOptionPane.showOptionDialog(canvasUI.getDesktopWorkbench().getWindow(),
                    "Node templates can be stored globally or inside the project. Where should the template be stored?",
                    "Create node template",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    new Object[]{"Globally", "Inside project", "Cancel"},
                    "Globally");
        }
        if (result == JOptionPane.CANCEL_OPTION)
            return;

        if (JIPipeDesktopParameterFormPanel.showDialog(canvasUI.getDesktopWorkbench(), template, new MarkdownText("# Node templates\n\nUse this user interface to modify node templates."), "Create template",
                JIPipeDesktopParameterFormPanel.WITH_SCROLLING | JIPipeDesktopParameterFormPanel.WITH_SEARCH_BAR | JIPipeDesktopParameterFormPanel.WITH_DOCUMENTATION)) {
            if (result == JOptionPane.YES_OPTION) {
                // Store globally
                JIPipe.getNodeTemplates().addToGlobal(template);
            } else {
                // Store locally
                JIPipe.getNodeTemplates().addToProject(template, canvasUI.getGraph().getProject());
            }
        }
    }

    /**
     * Copies the properties to another node template
     *
     * @param other the other node template
     */
    public void copyFrom(JIPipeNodeTemplate other) {
        this.name = other.name;
        this.description = new HTMLText(other.description);
        this.data = other.data;
        this.graph = other.graph;
        this.icon = new IconRef(other.icon);
        this.taxonomyFunction = new JIPipeTaxonomyFunctionRef(other.taxonomyFunction);
        this.menuPath = new StringList(other.menuPath);
        this.source = other.source;
        this.colorHue = other.colorHue;
    }

    public boolean isFromExtension() {
        return JIPipe.getNodeTemplates().isPluginTemplate(this);
    }

    /**
     * Get the unique identifier of this template.
     * The UUID is generated on construction and persisted with the template.
     *
     * @return the UUID
     */
    @JsonGetter("uuid")
    public UUID getUUID() {
        return uuid;
    }

    @JsonSetter("uuid")
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    @SetJIPipeDocumentation(name = "Name", description = "Name of the template")
    @JIPipeParameter(value = "name", uiOrder = -100)
    @JsonGetter("name")
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @JIPipeParameter("name")
    public void setName(String name) {
        this.name = name;
    }

    @SetJIPipeDocumentation(name = "Description", description = "A custom description")
    @JIPipeParameter(value = "description", uiOrder = -90)
    @JsonGetter("description")
    public HTMLText getDescription() {
        return description;
    }

    @JsonSetter("description")
    @JIPipeParameter("description")
    public void setDescription(HTMLText description) {
        this.description = description;
    }

    /**
     * Captures the legacy {@code menu-path} key on deserialization (R7).
     * <p>
     * Intentionally <b>not</b> a {@code @JIPipeParameter} and intentionally
     * without a getter: the value is read in from old template files, consumed
     * by {@link #migrateLegacyMenuPath()} (which maps it onto
     * {@link #getTaxonomyFunction() taxonomyFunction} and appends an auditable
     * footer to the description), and then never written back - so the key
     * vanishes from a file after one save cycle.
     *
     * @param menuPath the legacy menu path (may be {@code null})
     */
    @JsonSetter("menu-path")
    public void setMenuPath(StringList menuPath) {
        this.menuPath = menuPath == null ? new StringList() : menuPath;
    }

    /**
     * Returns the captured legacy menu path (read-only, never serialized).
     * Package-private (not part of the public API) so same-package tests can
     * assert the capture is populated before, and emptied after,
     * {@link #migrateLegacyMenuPath()}.
     *
     * @return the legacy menu path, empty when absent
     */
    StringList getMenuPathLegacy() {
        return menuPath == null ? new StringList() : menuPath;
    }

    /**
     * One-shot migration of the legacy {@code menu-path} key to the
     * {@link #taxonomyFunction} reference (spec §9): when the captured
     * legacy path is non-empty and no taxonomy function is set yet, the first
     * menu segment is matched against the registered taxonomy functions
     * (case-insensitively, top-level branches before same-named leaves - the
     * childless export sub-leaves "Images"/"Tables" must not shadow the
     * top-level branches of the same name - with the legacy abbreviation
     * table), the old path is appended to the description as an auditable
     * italic footer ("*Previously: Image → Threshold*"), and the captured key
     * is cleared.
     * <p>
     * <b>Order-safe:</b> a file that already carries a resolved
     * {@code taxonomy-function} is left untouched (the function wins over the
     * legacy key, no footer is appended). <b>Idempotent:</b> after the first
     * call the captured key is empty, so later calls are no-ops (no double
     * footer). Must be called after deserialization, on both load paths
     * (global files and project metadata).
     */
    public void migrateLegacyMenuPath() {
        StringList oldPath = getMenuPathLegacy();
        if (oldPath.isEmpty()) {
            return;
        }
        if (taxonomyFunction != null && taxonomyFunction.getId() != null) {
            // An explicit taxonomy function already exists: it wins, nothing to migrate
            menuPath = new StringList();
            return;
        }
        String oldPathJoined = String.join(" → ", oldPath);
        taxonomyFunction = JIPipeNodeTemplateMigration.migrate(oldPath.get(0));
        description = JIPipeNodeTemplateMigration.appendPreviouslyFooter(description, oldPathJoined);
        menuPath = new StringList();
    }

    @SetJIPipeDocumentation(name = "Taxonomy function", description = "The taxonomy function this template is filed under")
    @JIPipeParameter(value = "taxonomy-function", uiOrder = -70)
    @JsonGetter("taxonomy-function")
    public JIPipeTaxonomyFunctionRef getTaxonomyFunction() {
        return taxonomyFunction;
    }

    @JIPipeParameter("taxonomy-function")
    @JsonSetter("taxonomy-function")
    public void setTaxonomyFunction(JIPipeTaxonomyFunctionRef taxonomyFunction) {
        this.taxonomyFunction = taxonomyFunction;
    }

    @SetJIPipeDocumentation(name = "Icon", description = "The icon assigned to the menu entry")
    @JIPipeParameter(value = "icon", uiOrder = -80)
    @JsonGetter("icon")
    public IconRef getIcon() {
        return icon;
    }

    @JIPipeParameter("icon")
    @JsonSetter("icon")
    public void setIcon(IconRef icon) {
        this.icon = icon;
    }

    @SetJIPipeDocumentation(name = "Color hue (0-1)", description = "The hue from 0 to 1 of the node")
    @JsonGetter("color-hue")
    @JIPipeParameter("color-hue")
    public float getColorHue() {
        return colorHue;
    }

    @JsonSetter("color-hue")
    @JIPipeParameter("color-hue")
    public void setColorHue(float colorHue) {
        this.colorHue = colorHue;
    }

    @SetJIPipeDocumentation(name = "Data", description = "The data contained inside the node template. Must be JSON representation of a graph.")
    @JsonGetter("data")
    @JIPipeParameter(value = "data", uiOrder = 999)
    @StringParameterSettings(monospace = true, multiline = true, visible = false)
    public String getData() {
        if (graph != null) {
            data = JsonUtils.toPrettyJsonString(graph);
        }
        return data;
    }

    @JsonSetter("data")
    @JIPipeParameter("data")
    public void setData(String data) {
        this.graph = null;
        this.data = data;
    }

    @SetJIPipeDocumentation(name = "Source", description = "Used for assigning the node templates to a source (e.g., remote repository). You can leave this parameter alone.")
    @JIPipeParameter(value = "source")
    @StringParameterSettings(monospace = true)
    public String getSource() {
        return source;
    }

    @JIPipeParameter("source")
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * Gets the node type stored inside the data.
     * Returns null if the node type could not be found.
     *
     * @return the node type or null if it could not be found
     */
    public JIPipeGraph getGraph() {
        if (graph == null) {
            try {
                graph = JsonUtils.readFromString(data, JIPipeGraph.class);
            } catch (Throwable e) {
            }
        }
        return graph;
    }

    //    @JIPipeDocumentation(name = "Paste from clipboard", description = "Sets the node data from clipboard.")
//    @JIPipeContextAction(iconURL = ResourceUtils.RESOURCE_BASE_PATH + "/icons/actions/edit-paste.png", iconDarkURL = ResourceUtils.RESOURCE_BASE_PATH + "/dark/icons/actions/edit-paste.png")
//    public void pasteDataFromClipboard(JIPipeWorkbench workbench) {
//        String json = UIUtils.getStringFromClipboard();
//        if (json != null) {
//            try {
//                JIPipeGraph graph = JsonUtils.getObjectMapper().readValue(json, JIPipeGraph.class);
//                setData(json);
//                triggerParameterChange("data");
//            } catch (JsonProcessingException e) {
//                e.printStackTrace();
//            }
//        }
//    }

    public void setGraph(JIPipeGraph graph) {
        setData(JsonUtils.toPrettyJsonString(graph));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JIPipeNodeTemplate template = (JIPipeNodeTemplate) o;
        return Objects.equals(name, template.name) && Objects.equals(description, template.description) && Objects.equals(data, template.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description, data);
    }

    public ImageIcon getIconImage() {
        ImageIcon icon = JIPipe.RESOURCES.getIcon16("actions/configure.png");
        if (getIcon() != null && !StringUtils.isNullOrEmpty(getIcon().getIconName())) {
            try {
                icon = JIPipe.RESOURCES.getIcon16(getIcon().getIconName());
            } catch (Throwable e) {

            }
        }
        return icon;
    }

    public String getLocationInfo() {
        if (taxonomyFunction != null && taxonomyFunction.isResolved()) {
            return ("Templates\n" + taxonomyFunction.getFunction().getName()).trim();
        }
        return "Templates";
    }

}
