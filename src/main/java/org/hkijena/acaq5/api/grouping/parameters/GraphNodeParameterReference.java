package org.hkijena.acaq5.api.grouping.parameters;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.hkijena.acaq5.api.parameters.ACAQParameterAccess;
import org.hkijena.acaq5.api.parameters.ACAQParameterTree;
import org.hkijena.acaq5.utils.StringUtils;

import java.util.Objects;

/**
 * References a parameter in a graph
 */
public class GraphNodeParameterReference {
    private String customName;
    private String customDescription;
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
    public GraphNodeParameterReference(ACAQParameterAccess access, ACAQParameterTree tree) {
        this.path = tree.getUniqueKey(access);
    }

    /**
     * Gets the name in the original graph
     * Returns null if resolve() is unsuccessful
     *
     * @param tree the tree
     * @return name
     */
    public String getOriginalName(ACAQParameterTree tree) {
        ACAQParameterAccess access = resolve(tree);
        if(access != null) {
            String name = access.getName();
            if (name == null)
                name = access.getKey();
            return name;
        }
        else {
            return null;
        }
    }

    /**
     * Gets the name or custom name
     *
     * @param tree the tree
     * @return name or custom name
     */
    public String getName(ACAQParameterTree tree) {
        if (StringUtils.isNullOrEmpty(customName)) {
            ACAQParameterAccess access = resolve(tree);
            if(access != null) {
                String name = access.getName();
                if (name == null)
                    name = access.getKey();
                return name;
            }
            else {
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
    public String getDescription(ACAQParameterTree tree) {
        if (StringUtils.isNullOrEmpty(customDescription)) {
            ACAQParameterAccess access = resolve(tree);
            if(access != null) {
                return access.getDescription();
            }
            else {
                return null;
            }
        } else {
            return customDescription;
        }
    }

    /**
     * Tries to resolve the path
     *
     * @param tree the tree
     * @return the parameter access or null if it does not exist
     */
    public ACAQParameterAccess resolve(ACAQParameterTree tree) {
        return tree.getParameters().getOrDefault(path, null);
    }

    @JsonGetter("custom-name")
    public String getCustomName() {
        return customName;
    }

    @JsonSetter("custom-name")
    public void setCustomName(String customName) {
        this.customName = customName;
    }

    @JsonGetter("custom-description")
    public String getCustomDescription() {
        return customDescription;
    }

    @JsonSetter("custom-description")
    public void setCustomDescription(String customDescription) {
        this.customDescription = customDescription;
    }

    @JsonGetter("path")
    public String getPath() {
        return path;
    }

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
