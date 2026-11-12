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
import org.hkijena.jipipe.plugins.parameters.library.roi.Anchor;

import java.awt.Point;
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
 * {"visible":true,"position":{"x":470,"y":16},"size":{"w":280,"h":200},"locked":false,"anchor":"Auto","anchorOffset":{"x":0,"y":0},"layers":{"nodes":true,"edges":true,"notes":true}}
 * }</pre>
 * Missing fields fall back to the field's default value. A stored position of
 * {@code 0,0} means that the user has not moved the map yet: the map panel
 * then applies its initial anchor itself. The {@code anchor} selects one of
 * the nine snapping points (or {@code Auto}, where the snapping point is
 * detected from the position); the {@code anchorOffset} keeps the map's
 * distance to the anchored side(s) so the position survives overlay resizes.
 */
@JsonPropertyOrder({"visible", "position", "size", "locked", "anchor", "anchorOffset", "layers"})
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
    private AnchorMode anchor = AnchorMode.Auto;
    private AnchorOffset anchorOffset = new AnchorOffset();
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
        this.anchor = source.anchor;
        this.anchorOffset = new AnchorOffset(source.anchorOffset);
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

    /**
     * @return the anchor mode of the map
     */
    @JsonGetter("anchor")
    public AnchorMode getAnchor() {
        return anchor;
    }

    /**
     * Sets the anchor mode. A {@code null} value (from a hand-edited
     * settings file) falls back to {@link AnchorMode#Auto}.
     *
     * @param anchor the anchor mode
     */
    @JsonSetter("anchor")
    public void setAnchor(AnchorMode anchor) {
        this.anchor = anchor == null ? AnchorMode.Auto : anchor;
    }

    /**
     * @return the offset of the map relative to its anchor
     */
    @JsonGetter("anchorOffset")
    public AnchorOffset getAnchorOffset() {
        return anchorOffset;
    }

    /**
     * Sets the offset. A {@code null} value (from a hand-edited settings
     * file) falls back to the zero offset.
     *
     * @param anchorOffset the offset
     */
    @JsonSetter("anchorOffset")
    public void setAnchorOffset(AnchorOffset anchorOffset) {
        this.anchorOffset = anchorOffset == null ? new AnchorOffset() : anchorOffset;
    }

    /**
     * Sets the offset values.
     * Convenience accessor for the anchor offset.
     * Not part of the JSON shape.
     *
     * @param x the offset x value
     * @param y the offset y value
     */
    @JsonIgnore
    public void setAnchorOffset(int x, int y) {
        anchorOffset.setPosition(x, y);
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
                && anchor == that.anchor
                && Objects.equals(anchorOffset, that.anchorOffset)
                && Objects.equals(layers, that.layers);
    }

    @Override
    public int hashCode() {
        return Objects.hash(visible, position, size, locked, anchor, anchorOffset, layers);
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

    /**
     * The offset of the map relative to its anchor: the distance to the
     * anchored side(s). On centered axes the value is stored but ignored by
     * the placement (the axis is aligned to the exact center).
     */
    @JsonPropertyOrder({"x", "y"})
    public static class AnchorOffset {
        private int x = 0;
        private int y = 0;

        public AnchorOffset() {
        }

        /**
         * Creates a copy of a source offset.
         *
         * @param source the offset to copy
         */
        public AnchorOffset(AnchorOffset source) {
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
         * Sets the offset values.
         *
         * @param x the offset x value
         * @param y the offset y value
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
            AnchorOffset offset = (AnchorOffset) o;
            return x == offset.x && y == offset.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * The anchor mode of the floating map: either {@link #Auto} (the anchor
     * is detected automatically) or one of the nine snapping points of the
     * existing {@link org.hkijena.jipipe.plugins.parameters.library.roi.Anchor}
     * parameter.
     * <p>
     * Per axis, an anchor is {@code relative == 0} (left/top: the offset is
     * the distance to the left/top edge), {@code relative == 0.5} (centered:
     * the axis is aligned to the exact center and the offset is ignored), or
     * {@code relative == 1} (right/bottom: the offset is the distance to the
     * right/bottom edge).
     */
    public enum AnchorMode {
        Auto(null),
        TopLeft(Anchor.TopLeft),
        TopCenter(Anchor.TopCenter),
        TopRight(Anchor.TopRight),
        CenterLeft(Anchor.CenterLeft),
        CenterCenter(Anchor.CenterCenter),
        CenterRight(Anchor.CenterRight),
        BottomLeft(Anchor.BottomLeft),
        BottomCenter(Anchor.BottomCenter),
        BottomRight(Anchor.BottomRight);

        private final Anchor anchor;

        /**
         * Per axis, the maximum difference between the two edge distances at
         * which the axis counts as centered.
         */
        private static final int CENTER_TOLERANCE = 24;

        AnchorMode(Anchor anchor) {
            this.anchor = anchor;
        }

        /**
         * @return the underlying snapping point, or {@code null} for {@link #Auto}
         */
        public Anchor getAnchor() {
            return anchor;
        }

        /**
         * Computes the map position for this anchor inside an overlay.
         *
         * @param parentWidth  the overlay width
         * @param parentHeight the overlay height
         * @param width        the map width
         * @param height       the map height
         * @param offsetX      the captured offset of the horizontal axis
         * @param offsetY      the captured offset of the vertical axis
         * @return the map position (top-left corner)
         */
        public Point place(int parentWidth, int parentHeight, int width, int height, int offsetX, int offsetY) {
            requireAnchor();
            return new Point(placeAxis(anchor.getRelativeX(), parentWidth, width, offsetX),
                    placeAxis(anchor.getRelativeY(), parentHeight, height, offsetY));
        }

        /**
         * Places one axis: left/top keeps the offset, centered aligns to the
         * exact center, right/bottom measures the offset from the far edge.
         */
        private static int placeAxis(double relative, int parentSize, int size, int offset) {
            if (relative == 0) {
                return offset;
            }
            if (relative == 0.5) {
                return (parentSize - size) / 2;
            }
            return parentSize - size - offset;
        }

        /**
         * Detects the snapping point that best matches a map position: per
         * axis, the closer edge wins; an axis whose two edge distances differ
         * by at most a small tolerance is centered.
         *
         * @param parentWidth  the overlay width
         * @param parentHeight the overlay height
         * @param width        the map width
         * @param height       the map height
         * @param x            the map x coordinate
         * @param y            the map y coordinate
         * @return the detected anchor (never {@link #Auto})
         */
        public static AnchorMode detect(int parentWidth, int parentHeight, int width, int height, int x, int y) {
            double relativeX = detectAxis(parentWidth, width, x);
            double relativeY = detectAxis(parentHeight, height, y);
            for (AnchorMode mode : values()) {
                if (mode.anchor != null
                        && mode.anchor.getRelativeX() == relativeX
                        && mode.anchor.getRelativeY() == relativeY) {
                    return mode;
                }
            }
            throw new IllegalStateException("No anchor matches " + relativeX + ", " + relativeY);
        }

        /**
         * Detects one axis: the closer edge wins; an axis whose two edge
         * distances differ by at most {@link #CENTER_TOLERANCE} is centered.
         */
        private static double detectAxis(int parentSize, int size, int coordinate) {
            int low = coordinate;
            int high = parentSize - size - coordinate;
            if (Math.abs(low - high) <= CENTER_TOLERANCE) {
                return 0.5;
            }
            return low < high ? 0 : 1;
        }

        /**
         * Captures the offset of a map position for this anchor: the
         * distances to the anchored sides (raw coordinates for centered
         * axes, where the offset is ignored by {@link #place}).
         *
         * @param parentWidth  the overlay width
         * @param parentHeight the overlay height
         * @param width        the map width
         * @param height       the map height
         * @param x            the map x coordinate
         * @param y            the map y coordinate
         * @return the offset for this anchor
         */
        public Point captureOffset(int parentWidth, int parentHeight, int width, int height, int x, int y) {
            requireAnchor();
            return new Point(captureAxis(anchor.getRelativeX(), parentWidth, width, x),
                    captureAxis(anchor.getRelativeY(), parentHeight, height, y));
        }

        /**
         * Captures one axis: right/bottom measures from the far edge; the raw
         * coordinate is kept for left/top and centered axes (centered axes
         * ignore the offset on placement).
         */
        private static int captureAxis(double relative, int parentSize, int size, int coordinate) {
            if (relative == 1) {
                return parentSize - size - coordinate;
            }
            return coordinate;
        }

        private void requireAnchor() {
            if (anchor == null) {
                throw new UnsupportedOperationException("Auto does not have a fixed snapping point");
            }
        }
    }
}
