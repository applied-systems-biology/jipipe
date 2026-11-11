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

package org.hkijena.jipipe.desktop.app.grapheditor.commons;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonSetter;

import java.util.Objects;

/**
 * The persisted state of the graph editor's floating map (the successor of
 * the former "Navigator" dock panel).
 * <p>
 * Instances are stored in the application settings via
 * {@link org.hkijena.jipipe.plugins.settings.SettingsKeys#PIPELINE_MAP_STATE}
 * and {@link org.hkijena.jipipe.plugins.settings.SettingsKeys#COMPARTMENTS_MAP_STATE}.
 * The stored JSON shape is:
 * <pre>{@code
 * {"visible":true,"position":{"x":470,"y":16},"size":{"w":280,"h":200},"locked":false,"layers":{"nodes":true,"edges":true,"notes":true}}
 * }</pre>
 * Missing fields fall back to the field's default value. A stored position of
 * {@code 0,0} means that the user has not moved the map yet: the map panel
 * then applies its initial anchor itself.
 */
@JsonPropertyOrder({"visible", "position", "size", "locked", "layers"})
public class JIPipeDesktopMapState {

    /**
     * The default map width.
     */
    public static final int MAP_DEFAULT_WIDTH = 280;

    /**
     * The default map height.
     */
    public static final int MAP_DEFAULT_HEIGHT = 200;

    private boolean visible = true;
    private Position position = new Position();
    private Size size = new Size();
    private boolean locked = false;
    private Layers layers = new Layers();

    public JIPipeDesktopMapState() {
    }

    /**
     * Creates a deep copy of a source state.
     *
     * @param source the state to copy
     */
    public JIPipeDesktopMapState(JIPipeDesktopMapState source) {
        this.visible = source.visible;
        this.position = new Position(source.position);
        this.size = new Size(source.size);
        this.locked = source.locked;
        this.layers = new Layers(source.layers);
    }

    /**
     * @return the default state used when the user has no stored map state
     */
    public static JIPipeDesktopMapState defaultState() {
        return new JIPipeDesktopMapState();
    }

    @JsonGetter("visible")
    public boolean isVisible() {
        return visible;
    }

    @JsonSetter("visible")
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @JsonGetter("position")
    public Position getPosition() {
        return position;
    }

    @JsonSetter("position")
    public void setPosition(Position position) {
        this.position = position;
    }

    @JsonGetter("size")
    public Size getSize() {
        return size;
    }

    @JsonSetter("size")
    public void setSize(Size size) {
        this.size = size;
    }

    @JsonGetter("locked")
    public boolean isLocked() {
        return locked;
    }

    @JsonSetter("locked")
    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    @JsonGetter("layers")
    public Layers getLayers() {
        return layers;
    }

    @JsonSetter("layers")
    public void setLayers(Layers layers) {
        this.layers = layers;
    }

    /**
     * Convenience accessor for the position's x coordinate.
     * Not part of the JSON shape.
     */
    @JsonIgnore
    public int getX() {
        return position.getX();
    }

    /**
     * Convenience accessor for the position's y coordinate.
     * Not part of the JSON shape.
     */
    @JsonIgnore
    public int getY() {
        return position.getY();
    }

    /**
     * Moves the map (changes the stored position).
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     */
    public void setPosition(int x, int y) {
        position.setPosition(x, y);
    }

    /**
     * Convenience accessor for the size's width.
     * Not part of the JSON shape.
     */
    @JsonIgnore
    public int getWidth() {
        return size.getW();
    }

    /**
     * Convenience accessor for the size's height.
     * Not part of the JSON shape.
     */
    @JsonIgnore
    public int getHeight() {
        return size.getH();
    }

    /**
     * Resizes the map (changes the stored size).
     *
     * @param w the new width
     * @param h the new height
     */
    public void setSize(int w, int h) {
        size.setSize(w, h);
    }

    /**
     * Convenience accessor for whether the node layer is rendered.
     * Not part of the JSON shape.
     */
    @JsonIgnore
    public boolean isLayerNodes() {
        return layers.isNodes();
    }

    /**
     * Convenience accessor for whether the edge layer is rendered.
     * Not part of the JSON shape.
     */
    @JsonIgnore
    public boolean isLayerEdges() {
        return layers.isEdges();
    }

    /**
     * Convenience accessor for whether the note layer is rendered.
     * Not part of the JSON shape.
     */
    @JsonIgnore
    public boolean isLayerNotes() {
        return layers.isNotes();
    }

    /**
     * Shows or hides the node layer.
     *
     * @param nodes whether the node layer is rendered
     */
    public void setLayerNodes(boolean nodes) {
        layers.setNodes(nodes);
    }

    /**
     * Shows or hides the edge layer.
     *
     * @param edges whether the edge layer is rendered
     */
    public void setLayerEdges(boolean edges) {
        layers.setEdges(edges);
    }

    /**
     * Shows or hides the note layer.
     *
     * @param notes whether the note layer is rendered
     */
    public void setLayerNotes(boolean notes) {
        layers.setNotes(notes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        JIPipeDesktopMapState that = (JIPipeDesktopMapState) o;
        return visible == that.visible
                && locked == that.locked
                && Objects.equals(position, that.position)
                && Objects.equals(size, that.size)
                && Objects.equals(layers, that.layers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visible, position, size, locked, layers);
    }

    /**
     * The map position (top-left corner).
     */
    @JsonPropertyOrder({"x", "y"})
    public static class Position {
        private int x = 0;
        private int y = 0;

        public Position() {
        }

        /**
         * Creates a copy of a source position.
         *
         * @param source the position to copy
         */
        public Position(Position source) {
            this.x = source.x;
            this.y = source.y;
        }

        @JsonGetter("x")
        public int getX() {
            return x;
        }

        @JsonSetter("x")
        public void setX(int x) {
            this.x = x;
        }

        @JsonGetter("y")
        public int getY() {
            return y;
        }

        @JsonSetter("y")
        public void setY(int y) {
            this.y = y;
        }

        /**
         * Moves the position.
         *
         * @param x the new x coordinate
         * @param y the new y coordinate
         */
        public void setPosition(int x, int y) {
            setX(x);
            setY(y);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Position position = (Position) o;
            return x == position.x && y == position.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * The map size.
     */
    @JsonPropertyOrder({"w", "h"})
    public static class Size {
        private int w = MAP_DEFAULT_WIDTH;
        private int h = MAP_DEFAULT_HEIGHT;

        public Size() {
        }

        /**
         * Creates a copy of a source size.
         *
         * @param source the size to copy
         */
        public Size(Size source) {
            this.w = source.w;
            this.h = source.h;
        }

        @JsonGetter("w")
        public int getW() {
            return w;
        }

        @JsonSetter("w")
        public void setW(int w) {
            this.w = w;
        }

        @JsonGetter("h")
        public int getH() {
            return h;
        }

        @JsonSetter("h")
        public void setH(int h) {
            this.h = h;
        }

        /**
         * Sets the size.
         *
         * @param w the new width
         * @param h the new height
         */
        public void setSize(int w, int h) {
            setW(w);
            setH(h);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Size size = (Size) o;
            return w == size.w && h == size.h;
        }

        @Override
        public int hashCode() {
            return Objects.hash(w, h);
        }
    }

    /**
     * Which layers of the graph the map renders.
     */
    @JsonPropertyOrder({"nodes", "edges", "notes"})
    public static class Layers {
        private boolean nodes = true;
        private boolean edges = true;
        private boolean notes = true;

        public Layers() {
        }

        /**
         * Creates a copy of a source layer state.
         *
         * @param source the layer state to copy
         */
        public Layers(Layers source) {
            this.nodes = source.nodes;
            this.edges = source.edges;
            this.notes = source.notes;
        }

        @JsonGetter("nodes")
        public boolean isNodes() {
            return nodes;
        }

        @JsonSetter("nodes")
        public void setNodes(boolean nodes) {
            this.nodes = nodes;
        }

        @JsonGetter("edges")
        public boolean isEdges() {
            return edges;
        }

        @JsonSetter("edges")
        public void setEdges(boolean edges) {
            this.edges = edges;
        }

        @JsonGetter("notes")
        public boolean isNotes() {
            return notes;
        }

        @JsonSetter("notes")
        public void setNotes(boolean notes) {
            this.notes = notes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Layers layers = (Layers) o;
            return nodes == layers.nodes && edges == layers.edges && notes == layers.notes;
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodes, edges, notes);
        }
    }
}
