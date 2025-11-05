package org.hkijena.jipipe.api.nodes;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.awt.*;

public class JIPipeGraphEdgeControlPoint {

    @JsonProperty("x")
    private int x;
    @JsonProperty("y")
    private int y;

    public JIPipeGraphEdgeControlPoint() {
    }

    public JIPipeGraphEdgeControlPoint(JIPipeGraphEdgeControlPoint other) {
        this.x = other.x;
        this.y = other.y;
    }

    public JIPipeGraphEdgeControlPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public Point toPoint() {
        return new Point(x, y);
    }

    public void set(Point point) {
        this.x = point.x;
        this.y = point.y;
    }
}
