package org.hkijena.jipipe.extensions.ijfilaments.util;

import ij.process.ColorProcessor;
import mcib3d.geom.Vector3D;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.parameters.AbstractJIPipeParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.ijfilaments.datatypes.Filaments3DData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageBlendMode;
import org.hkijena.jipipe.extensions.parameters.library.colors.OptionalColorParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.optional.OptionalIntegerParameter;
import org.hkijena.jipipe.utils.ColorUtils;

import java.awt.*;

public class FilamentsDrawer extends AbstractJIPipeParameterCollection {
    private OptionalColorParameter overrideVertexColor = new OptionalColorParameter(Color.RED,false);
    private OptionalColorParameter overrideEdgeColor = new OptionalColorParameter(Color.BLUE,false);
    private OptionalIntegerParameter overrideVertexRadius = new OptionalIntegerParameter(false, 1);
    private OptionalIntegerParameter overrideEdgeThickness = new OptionalIntegerParameter(false, 1);
    private boolean drawVertices = true;
    private boolean drawEdges = true;
    private double opacity = 1.0;
    private boolean ignoreZ = false;
    private boolean ignoreC = false;
    private boolean ignoreT = false;
    private ImageBlendMode blendMode = ImageBlendMode.Normal;

    private boolean hollowVertices = false;

    public FilamentsDrawer() {

    }

    public FilamentsDrawer(FilamentsDrawer other) {
        copyFrom(other);
    }

    public void copyFrom(FilamentsDrawer other) {
        this.overrideVertexColor = new OptionalColorParameter(other.overrideVertexColor);
        this.overrideEdgeColor = new OptionalColorParameter(other.overrideEdgeColor);
        this.overrideVertexRadius = new OptionalIntegerParameter(other.overrideVertexRadius);
        this.overrideEdgeThickness = new OptionalIntegerParameter(other.overrideEdgeThickness);
        this.drawVertices = other.drawVertices;
        this.drawEdges = other.drawEdges;
        this.opacity = other.opacity;
        this.ignoreZ = other.ignoreZ;
        this.ignoreC = other.ignoreC;
        this.ignoreT = other.ignoreT;
        this.blendMode = other.blendMode;
        this.hollowVertices = other.hollowVertices;
    }

    @JIPipeDocumentation(name = "Hollow vertices", description = "If enabled, draw vertices as hollow spheres")
    @JIPipeParameter("hollow-vertices")
    public boolean isHollowVertices() {
        return hollowVertices;
    }

    @JIPipeParameter("hollow-vertices")
    public void setHollowVertices(boolean hollowVertices) {
        this.hollowVertices = hollowVertices;
    }

    @JIPipeDocumentation(name = "Blend mode", description = "Determines how the rendered filaments are blended with the reference image")
    @JIPipeParameter("blend-mode")
    public ImageBlendMode getBlendMode() {
        return blendMode;
    }

    @JIPipeParameter("blend-mode")
    public void setBlendMode(ImageBlendMode blendMode) {
        this.blendMode = blendMode;
    }

    @JIPipeDocumentation(name = "Override vertex color", description = "Allows to override the color of vertices")
    @JIPipeParameter("override-vertex-color")
    public OptionalColorParameter getOverrideVertexColor() {
        return overrideVertexColor;
    }

    @JIPipeParameter("override-vertex-color")
    public void setOverrideVertexColor(OptionalColorParameter overrideVertexColor) {
        this.overrideVertexColor = overrideVertexColor;
    }

    @JIPipeDocumentation(name = "Override edge color", description = "Allows to override the color of edges")
    @JIPipeParameter("override-edge-color")
    public OptionalColorParameter getOverrideEdgeColor() {
        return overrideEdgeColor;
    }

    @JIPipeParameter("override-edge-color")
    public void setOverrideEdgeColor(OptionalColorParameter overrideEdgeColor) {
        this.overrideEdgeColor = overrideEdgeColor;
    }

    @JIPipeDocumentation(name = "Override vertex radius", description = "Allows to override the radius of each vertex")
    @JIPipeParameter("override-vertex-radius")
    public OptionalIntegerParameter getOverrideVertexRadius() {
        return overrideVertexRadius;
    }

    @JIPipeParameter("override-vertex-radius")
    public void setOverrideVertexRadius(OptionalIntegerParameter overrideVertexRadius) {
        this.overrideVertexRadius = overrideVertexRadius;
    }

    @JIPipeDocumentation(name = "Draw vertices", description = "If enabled, draw vertices")
    @JIPipeParameter("draw-vertices")
    public boolean isDrawVertices() {
        return drawVertices;
    }

    @JIPipeParameter("draw-vertices")
    public void setDrawVertices(boolean drawVertices) {
        this.drawVertices = drawVertices;
    }

    @JIPipeDocumentation(name = "Draw edges", description = "If enabled, draw edges")
    @JIPipeParameter("draw-edges")
    public boolean isDrawEdges() {
        return drawEdges;
    }

    @JIPipeParameter("draw-edges")
    public void setDrawEdges(boolean drawEdges) {
        this.drawEdges = drawEdges;
    }

    @JIPipeDocumentation(name = "Override edge thickness", description = "If enabled, override the edge thickness with the specified value. Otherwise, the edge thickness interpolates between the two vertex radii.")
    @JIPipeParameter("override-edge-thickness")
    public OptionalIntegerParameter getOverrideEdgeThickness() {
        return overrideEdgeThickness;
    }

    @JIPipeParameter("override-edge-thickness")
    public void setOverrideEdgeThickness(OptionalIntegerParameter overrideEdgeThickness) {
        this.overrideEdgeThickness = overrideEdgeThickness;
    }

    @JIPipeDocumentation(name = "Opacity", description = "The opacity of the rendering")
    @JIPipeParameter("opacity")
    public double getOpacity() {
        return opacity;
    }

    @JIPipeParameter("opacity")
    public void setOpacity(double opacity) {
        this.opacity = opacity;
    }

    @JIPipeDocumentation(name = "Ignore Z", description = "If enabled, the Z location of each each vertex will be ignored")
    @JIPipeParameter("ignore-z")
    public boolean isIgnoreZ() {
        return ignoreZ;
    }

    @JIPipeParameter("ignore-z")
    public void setIgnoreZ(boolean ignoreZ) {
        this.ignoreZ = ignoreZ;
    }

    @JIPipeDocumentation(name = "Ignore channel", description = "If enabled, ignore the channel (C) location of each vertex")
    @JIPipeParameter("ignore-c")
    public boolean isIgnoreC() {
        return ignoreC;
    }

    @JIPipeParameter("ignore-c")
    public void setIgnoreC(boolean ignoreC) {
        this.ignoreC = ignoreC;
    }

    @JIPipeDocumentation(name = "Ignore frame", description = "If enabled, ignore the frame (T) location of each vertex")
    @JIPipeParameter("ignore-t")
    public boolean isIgnoreT() {
        return ignoreT;
    }

    @JIPipeParameter("ignore-t")
    public void setIgnoreT(boolean ignoreT) {
        this.ignoreT = ignoreT;
    }

    public void drawFilamentsOnProcessor(Filaments3DData graph, ColorProcessor processor, int z, int c, int t) {
        if(drawEdges) {
            for (FilamentEdge edge : graph.edgeSet()) {
                FilamentVertex source = graph.getEdgeSource(edge);
                FilamentVertex target = graph.getEdgeTarget(edge);

                if (source.getNonSpatialLocation().getChannel() != c && !ignoreC)
                    continue;
                if (source.getNonSpatialLocation().getFrame() != t && !ignoreT)
                    continue;
                if (target.getNonSpatialLocation().getChannel() != c && !ignoreC)
                    continue;
                if (target.getNonSpatialLocation().getFrame() != t && !ignoreT)
                    continue;

                int sourceRadius = (int) source.getRadius();
                int targetRadius = (int) target.getRadius();
                if(overrideVertexRadius.isEnabled()) {
                    sourceRadius = overrideVertexRadius.getContent();
                    targetRadius = overrideVertexRadius.getContent();
                }
                if(overrideEdgeThickness.isEnabled()) {
                    sourceRadius = overrideEdgeThickness.getContent();
                    targetRadius = overrideEdgeThickness.getContent();
                }

                drawLineOnProcessor(source.getSpatialLocation().getX(), source.getSpatialLocation().getY(), source.getSpatialLocation().getZ(),
                        target.getSpatialLocation().getX(), target.getSpatialLocation().getY(), target.getSpatialLocation().getZ(),
                        edge.getColor(),
                        sourceRadius,
                        targetRadius,
                        processor,
                        z);
            }
        }
        if(drawVertices) {
            for (FilamentVertex vertex : graph.vertexSet()) {
//            if(vertex.getSpatialLocation().getZ() != z && !ignoreZ)
//                continue;
                if (vertex.getNonSpatialLocation().getChannel() != c && !ignoreC)
                    continue;
                if (vertex.getNonSpatialLocation().getFrame() != t && !ignoreT)
                    continue;
                Color color = vertex.getColor();
                int radius = (int) vertex.getRadius();

                if (overrideVertexColor.isEnabled())
                    color = overrideVertexColor.getContent();
                if (overrideVertexRadius.isEnabled())
                    radius = overrideVertexRadius.getContent();

                drawBallOnProcessor(vertex.getSpatialLocation().getX(), vertex.getSpatialLocation().getY(), vertex.getSpatialLocation().getZ(), color, radius, hollowVertices, processor, z);
            }
        }
    }

    private void drawLineOnProcessor(int x0, int y0, int z0, int x1, int y1, int z1, Color color, int rad0, int rad1, ColorProcessor processor, int imageZ) {
        Vector3D V = new Vector3D(x1 - x0, y1 - y0, z1 - z0);
        double len = V.getLength();
        V.normalize();
        double vx = V.getX();
        double vy = V.getY();
        double vz = V.getZ();
        for (int i = 0; i < (int) len; i++) {
            double perc = i / len;
            int rad = (int) (rad0 + perc * (rad1 - rad0));
            drawBallOnProcessor((int) (x0 + i * vx), (int) (y0 + i * vy), (int) (z0 + i * vz), color, rad, false, processor, imageZ);
        }
    }

    private void drawBallOnProcessor(int targetX, int targetY, int targetZ, Color color, int radius, boolean hollow, ColorProcessor processor, int imageZ) {
        int rgb = color.getRGB();
        int imageWidth = processor.getWidth();
        int imageHeight = processor.getHeight();
        if(radius <= 0) {
            if(targetZ == imageZ) {
                if(opacity >= 1 && blendMode == ImageBlendMode.Normal) {
                    processor.set(targetX, targetY, blendMode.blend(processor.get(targetX, targetY), rgb, opacity));
                }
                else {
                    processor.set(targetX, targetY, rgb);
                }
            }
        }
        else if(Math.abs(imageZ - targetZ) <= radius) {
            int[] pixels = (int[]) processor.getPixels();
            for (int y = targetY - radius; y < targetY + radius; y++) {
                if(y < 0 || y >= imageHeight)
                    continue;
                for (int x = targetX - radius; x < targetX + radius; x++) {
                    if(x < 0 || x >= imageWidth)
                        continue;
                    double k = Math.pow(x - targetX, 2) + Math.pow(y - targetY, 2) + Math.pow(imageZ - targetZ, 2);
                    if(k > radius * radius) {
                        continue;
                    }
                    if(hollow && k < Math.pow(radius - 1, 2)) {
                        continue;
                    }
                    if(opacity >= 1 && blendMode == ImageBlendMode.Normal) {
                        pixels[x + y * imageWidth] = rgb;
                    }
                    else {
                        pixels[x + y * imageWidth] = blendMode.blend(pixels[x + y * imageWidth], rgb, opacity);
                    }
                }
            }
        }
    }

    public void drawFilamentsOnGraphics(Filaments3DData graph, Graphics2D graphics2D, Rectangle renderArea, double magnification, int z, int c, int t, boolean drawMuted) {
        if(drawEdges) {
            for (FilamentEdge edge : graph.edgeSet()) {
                FilamentVertex source = graph.getEdgeSource(edge);
                FilamentVertex target = graph.getEdgeTarget(edge);

                if (source.getNonSpatialLocation().getChannel() != c && !ignoreC)
                    continue;
                if (source.getNonSpatialLocation().getFrame() != t && !ignoreT)
                    continue;
                if (target.getNonSpatialLocation().getChannel() != c && !ignoreC)
                    continue;
                if (target.getNonSpatialLocation().getFrame() != t && !ignoreT)
                    continue;

                int sourceRadius = (int) source.getRadius();
                int targetRadius = (int) target.getRadius();
                if(overrideVertexRadius.isEnabled()) {
                    sourceRadius = overrideVertexRadius.getContent();
                    targetRadius = overrideVertexRadius.getContent();
                }
                if(overrideEdgeThickness.isEnabled()) {
                    sourceRadius = overrideEdgeThickness.getContent();
                    targetRadius = overrideEdgeThickness.getContent();
                }

                drawLineOnGraphics(source.getSpatialLocation().getX(), source.getSpatialLocation().getY(), source.getSpatialLocation().getZ(),
                        target.getSpatialLocation().getX(), target.getSpatialLocation().getY(), target.getSpatialLocation().getZ(),
                        edge.getColor(),
                        sourceRadius,
                        targetRadius,
                        graphics2D,
                        z,
                        renderArea,
                        magnification,
                        drawMuted);
            }
        }
        if(drawVertices) {
            for (FilamentVertex vertex : graph.vertexSet()) {
//            if(vertex.getSpatialLocation().getZ() != z && !ignoreZ)
//                continue;
                if (vertex.getNonSpatialLocation().getChannel() != c && !ignoreC)
                    continue;
                if (vertex.getNonSpatialLocation().getFrame() != t && !ignoreT)
                    continue;
                Color color = vertex.getColor();
                int radius = (int) vertex.getRadius();

                if (overrideVertexColor.isEnabled())
                    color = overrideVertexColor.getContent();
                if (overrideVertexRadius.isEnabled())
                    radius = overrideVertexRadius.getContent();

                drawBallOnGraphics(vertex.getSpatialLocation().getX(), vertex.getSpatialLocation().getY(), vertex.getSpatialLocation().getZ(), color, radius,
                        hollowVertices, graphics2D, z, renderArea, magnification, drawMuted);
            }
        }
    }

    private void drawLineOnGraphics(int x0, int y0, int z0, int x1, int y1, int z1, Color color, int rad0, int rad1, Graphics2D graphics2D, int imageZ, Rectangle renderArea, double magnification, boolean drawMuted) {
        Vector3D V = new Vector3D(x1 - x0, y1 - y0, z1 - z0);
        double len = V.getLength();
        V.normalize();
        double vx = V.getX();
        double vy = V.getY();
        double vz = V.getZ();
        for (int i = 0; i < (int) len; i++) {
            double perc = i / len;
            int rad = (int) (rad0 + perc * (rad1 - rad0));
            drawBallOnGraphics((int) (x0 + i * vx), (int) (y0 + i * vy), (int) (z0 + i * vz), color, rad, false, graphics2D, imageZ, renderArea, magnification, drawMuted);
        }
    }

    private void drawBallOnGraphics(int targetX, int targetY, int targetZ, Color color, int radius, boolean hollow, Graphics2D graphics2D, int imageZ, Rectangle renderArea, double magnification, boolean drawMuted) {
        if(drawMuted) {
            color = ColorUtils.scaleHSV(color, 0.8f, 1, 0.5f);
        }
        if(opacity < 1) {
            color = new Color(color.getRed(), color.getGreen(), color.getBlue(), (int) (color.getAlpha() * opacity));
        }
        graphics2D.setColor(color);
        int renderWidth = renderArea.width;
        int renderHeight = renderArea.height;
        int magTargetX = (int)(targetX * magnification);
        int magTargetY = (int)(targetY * magnification);
        int magTargetZ = (int)(targetZ * magnification);
        double magRadius = radius * magnification;
        if(radius <= 0) {
            if(targetZ == imageZ) {
                graphics2D.drawRect(magTargetX + renderArea.x, magTargetY + renderArea.y, Math.max(1, (int)magnification), Math.max(1, (int)magnification));
            }
        }
        else if(Math.abs(imageZ - targetZ) <= radius) {
            double radiusAtZ = Math.cos(Math.abs(imageZ - targetZ)) * magRadius;
            if(hollow) {
                graphics2D.drawOval((int) (magTargetX - radiusAtZ + renderArea.x), (int) (magTargetY - radiusAtZ + renderArea.y), (int) (radiusAtZ * 2), (int) (radiusAtZ * 2));
            }
            else {
                graphics2D.fillOval((int) (magTargetX - radiusAtZ + renderArea.x), (int) (magTargetY - radiusAtZ + renderArea.y), (int) (radiusAtZ * 2), (int) (radiusAtZ * 2));
            }
        }
    }


}
