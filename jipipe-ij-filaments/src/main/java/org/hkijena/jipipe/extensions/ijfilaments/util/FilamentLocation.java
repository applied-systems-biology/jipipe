package org.hkijena.jipipe.extensions.ijfilaments.util;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class FilamentLocation {
    private int x;
    private int y;
    private int z;
    private int c = -1;

    private int t = -1;

    public FilamentLocation() {
    }

    public FilamentLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public FilamentLocation(int x, int y, int z, int c, int t) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.c = c;
        this.t = t;
    }

    public FilamentLocation(FilamentLocation other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
        this.c = other.c;
        this.t = other.t;
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

    @JsonGetter("z")
    public int getZ() {
        return z;
    }

    @JsonSetter("z")
    public void setZ(int z) {
        this.z = z;
    }

    @JsonGetter("c")
    public int getC() {
        return c;
    }

    @JsonSetter("c")
    public void setC(int c) {
        this.c = c;
    }

    @JsonGetter("t")
    public int getT() {
        return t;
    }

    @JsonSetter("t")
    public void setT(int t) {
        this.t = t;
    }
}
