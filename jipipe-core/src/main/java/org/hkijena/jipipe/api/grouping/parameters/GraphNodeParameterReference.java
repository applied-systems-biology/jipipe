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

package org.hkijena.jipipe.api.grouping.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.jipipe.JIPipe;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.nodes.JIPipeGraph;
import org.hkijena.jipipe.api.nodes.JIPipeGraphNode;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameterTree;
import org.hkijena.jipipe.extensions.parameters.library.markup.HTMLText;
import org.hkijena.jipipe.extensions.parameters.library.primitives.StringParameterSettings;
import org.hkijena.jipipe.utils.StringUtils;

import java.util.Objects;
import java.util.UUID;

/**
 * References a parameter in a graph
 */
public class GraphNodeParameterReference extends AbstractJIPipeParameterCollection {
    private String customName;
    private HTMLText customDescription;
    private String path;

    /**
     * Creates a new empty instance
     */
    public GraphNodeParameterReference() {
    }

    /**
     * Creates a copy
     *
     * @param other the original
     */
    public GraphNodeParameterReference(GraphNodeParameterReference other) {
        this.customName = other.customName;
        this.customDescription = other.customDescription;
        this.path = other.path;
    }

    /**
     * Instantiates with an existing access within a tree
     *
     * @param access the access
     * @param tree   the tree
     */
    public GraphNodeParameterReference(JIPipeParameterAccess access, JIPipeParameterTree tree) {
        this.path = tree.getUniqueKey(access);
    }

    /**
     * Gets the name in the original graph
     * Returns null if resolve() is unsuccessful
     *
     * @param tree the tree
     * @return name
     */
    public String getOriginalName(JIPipeParameterTree tree) {
        JIPipeParameterAccess access = resolve(tree);
        if (access != null) {
            String name = access.getName();
            if (name == null)
                name = access.getKey();
            return name;
        } else {
            return null;
        }
    }

    /**
     * Gets the name or custom name
     *
     * @param tree the tree
     * @return name or custom name
     */
    public String getName(JIPipeParameterTree tree) {
        if (StringUtils.isNullOrEmpty(customName)) {
            JIPipeParameterAccess access = resolve(tree);
            if (access != null) {
                String name = access.getName();
                if (name == null)
                    name = access.getKey();
                return name;
            } else {
                return "[No name]";
            }
        } else {
            return customName;
        }
    }

    /**
     * Gets the name or custom name.
     * Returns null if resolve() is unsuccessful and no custom description is set
     *
     * @param tree the tree
     * @return name or custom name
     */
    public String getDescription(JIPipeParameterTree tree) {
        if (StringUtils.isNullOrEmpty(customDescription)) {
            JIPipeParameterAccess access = resolve(tree);
            if (access != null) {
                return access.getDescription();
            } else {
                return null;
            }
        } else {
            return customDescription.getBody();
        }
    }

    /**
     * Tries to resolve the path
     *
     * @param tree the tree
     * @return the parameter access or null if it does not exist
     */
    public JIPipeParameterAccess resolve(JIPipeParameterTree tree) {
        JIPipeParameterAccess result = tree.getParameters().getOrDefault(path, null);

        // There might be a legacy parameter access
        if (result == null) {
            int i = path.indexOf('/');
            if (i == -1)
                return null;
            String uuidOrAlias = path.substring(0, i);
            String subPath = path.substring(i + 1);
            UUID uuid = null;
            for (JIPipeParameterAccess access : tree.getParameters().values()) {
                if (access.getSource() instanceof JIPipeGraphNode) {
                    JIPipeGraph graph = ((JIPipeGraphNode) access.getSource()).getParentGraph();
                    if (graph == null)
                        break;
                    uuid = graph.findNodeUUID(uuidOrAlias);
                    if (uuid != null)
                        break;
                }
            }

            if (uuid != null) {
                JIPipe.getInstance().getLogService().info("[Project format conversion] Updating parameter reference " + path + " to " + uuid + "/" + subPath);
                path = uuid + "/" + subPath;
                return tree.getParameters().getOrDefault(path, null);
            }
            return null;
        } else {
            return result;
        }
    }

    @JIPipeDocumentation(name = "Custom name", description = "A custom name for the parameter reference. If left empty, the name of the referenced parameter is utilized.")
    @JIPipeParameter(value = "custom-name", uiOrder = -100)
    @JsonGetter("custom-name")
    public String getCustomName() {
        return customName;
    }

    @JIPipeParameter("custom-name")
    @JsonSetter("custom-name")
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @JIPipeDocumentation(name = "Custom description", description = "A custom description for the referenced parameter.")
    @JIPipeParameter(value = "custom-description", uiOrder = -90)
    @JsonGetter("custom-description")
    public HTMLText getCustomDescription() {
        return customDescription;
    }

    @JIPipeParameter("custom-description")
    @JsonSetter("custom-description")
    public void setCustomDescription(HTMLText customDescription) {
        this.customDescription = customDescription;
    }

    @JIPipeDocumentation(name = "Path", description = "The path to the referenced parameter. <strong>If you do not know which values are valid, leave this setting alone</strong>")
    @JIPipeParameter(value = "path", uiOrder = 100)
    @JsonGetter("path")
    @StringParameterSettings(monospace = true)
    public String getPath() {
        return path;
    }

    @JIPipeParameter("path")
    @JsonSetter("path")
    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GraphNodeParameterReference reference = (GraphNodeParameterReference) o;
        return Objects.equals(path, reference.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }
}
