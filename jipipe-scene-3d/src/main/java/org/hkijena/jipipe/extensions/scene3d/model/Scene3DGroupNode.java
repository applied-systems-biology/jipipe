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

package org.hkijena.jipipe.extensions.scene3d.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.ArrayList;
import java.util.List;

public class Scene3DGroupNode implements Scene3DNode {
    private String name;

    private List<Scene3DNode> children = new ArrayList<>();

    public Scene3DGroupNode() {
    }

    public Scene3DGroupNode(Scene3DGroupNode other) {
        this.name = other.name;
        for (Scene3DNode child : other.children) {
            this.children.add(child.duplicate());
        }
    }

    @Override
    public Scene3DNode duplicate() {
        return new Scene3DGroupNode(this);
    }

    @JsonGetter("name")
    @Override
    public String getName() {
        return name;
    }

    @JsonSetter("name")
    @Override
    public void setName(String name) {
        this.name = name;
    }

    @JsonGetter("children")
    public List<Scene3DNode> getChildren() {
        return children;
    }

    @JsonSetter("children")
    public void setChildren(List<Scene3DNode> children) {
        this.children = children;
    }

    public void addChild(Scene3DNode node) {
        children.add(node);
    }
}
