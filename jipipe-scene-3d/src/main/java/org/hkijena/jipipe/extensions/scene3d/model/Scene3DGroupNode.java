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
