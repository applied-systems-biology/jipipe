package org.hkijena.jipipe.api;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.eventbus.EventBus;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.categories.MiscellaneousNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeContextAction;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterCollection;
import org.hkijena.jipipe.extensions.parameters.api.collections.ListParameter;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.extensions.parameters.library.primitives.list.StringList;
import org.hkijena.jipipe.extensions.parameters.library.references.IconRef;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.utils.ResourceUtils;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.json.JsonUtils;

import java.awt.*;
import java.util.*;

/**
 * Contains the JSON data of a node that can be created by a user for sharing
 * An intermediate between copying a node and a proper plugin.
 */
public class JIPipeNodeTemplate implements JIPipeParameterCollection {
    private final EventBus eventBus = new EventBus();
    private String name = "Unnamed template";
    private HTMLText description = new HTMLText();
    private StringList menuPath = new StringList();
    private IconRef icon = new IconRef("actions/configure.png");
    private String data;
    private JIPipeGraph graph;
    private Color fillColor = MiscellaneousNodeTypeCategory.FILL_COLOR;
    private Color borderColor = MiscellaneousNodeTypeCategory.BORDER_COLOR;

    public JIPipeNodeTemplate() {
    }

    public JIPipeNodeTemplate(JIPipeNodeTemplate other) {
        copyFrom(other);
    }

    public static Map<String, Set<JIPipeNodeTemplate>> groupByMenuPaths(Collection<JIPipeNodeTemplate> infos) {
        Map<String, Set<JIPipeNodeTemplate>> result = new HashMap<>();
        for (JIPipeNodeTemplate info : infos) {
            String menuPath = String.join("\n", info.getMenuPath());
            Set<JIPipeNodeTemplate> group = result.getOrDefault(menuPath, null);
            if (group == null) {
                group = new HashSet<>();
                result.put(menuPath, group);
            }
            group.add(info);
        }
        return result;
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
        this.menuPath = new StringList(other.menuPath);
        this.fillColor = other.fillColor;
        this.borderColor = other.borderColor;
    }

    @JIPipeDocumentation(name = "Name", description = "Name of the template")
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

    @JIPipeDocumentation(name = "Description", description = "A custom description")
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

    @JIPipeDocumentation(name = "Menu path", description = "The path inside the 'Templates' menu")
    @JIPipeParameter(value = "menu-path", uiOrder = -70)
    @JsonGetter("menu-path")
    public StringList getMenuPath() {
        return menuPath;
    }

    @JIPipeParameter("menu-path")
    @JsonSetter("menu-path")
    public void setMenuPath(StringList menuPath) {
        this.menuPath = menuPath;
    }

    @JIPipeDocumentation(name = "Icon", description = "The icon assigned to the menu entry")
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

    @JIPipeDocumentation(name = "Fill color", description = "The fill color of the icon shown inside the template list")
    @JIPipeParameter(value = "fill-color", uiOrder = -50)
    @JsonGetter("fill-color")
    public Color getFillColor() {
        return fillColor;
    }

    @JIPipeParameter("fill-color")
    @JsonSetter("fill-color")
    public void setFillColor(Color fillColor) {
        this.fillColor = fillColor;
    }

    @JIPipeDocumentation(name = "Border color", description = "The border color of the icon shown inside the template list")
    @JIPipeParameter(value = "border-color", uiOrder = -40)
    @JsonGetter("border-color")
    public Color getBorderColor() {
        return borderColor;
    }

    @JIPipeParameter("border-color")
    @JsonSetter("border-color")
    public void setBorderColor(Color borderColor) {
        this.borderColor = borderColor;
    }

    @JIPipeDocumentation(name = "Data", description = "The data contained inside the node template. Must be JSON representation of a graph.")
    @JsonGetter("data")
    @JIPipeParameter(value = "data", uiOrder = 999)
    @StringParameterSettings(monospace = true, multiline = true, visible = false)
    public String getData() {
        return data;
    }

    @JsonSetter("data")
    @JIPipeParameter("data")
    public void setData(String data) {
        this.graph = null;
        this.data = data;
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
            } catch (Exception e) {
            }
        }
        return graph;
    }

    @Override
    public EventBus getEventBus() {
        return eventBus;
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

    public static class List extends ListParameter<JIPipeNodeTemplate> {
        /**
         * Creates a new instance
         */
        public List() {
            super(JIPipeNodeTemplate.class);
        }

        /**
         * Creates a copy
         *
         * @param other the original
         */
        public List(JIPipeNodeTemplate.List other) {
            super(JIPipeNodeTemplate.class);
            for (JIPipeNodeTemplate template : other) {
                add(new JIPipeNodeTemplate(template));
            }
        }
    }
}
