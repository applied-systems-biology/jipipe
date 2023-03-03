package org.hkijena.jipipe.extensions.imageviewer.plugins2d.maskdrawer;

import org.hkijena.jipipe.extensions.imageviewer.utils.ImageViewerPanelCanvas2DTool;
import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelPlugin2D;

import javax.swing.*;

/**
 * Super class for any tool that can be used in {@link MaskDrawerPlugin2D}
 */
public abstract class MaskDrawer2DTool extends ImageViewerPanelPlugin2D implements ImageViewerPanelCanvas2DTool {
    private final MaskDrawerPlugin2D maskDrawerPlugin;
    private final String name;
    private final String description;
    private final Icon icon;

    public MaskDrawer2DTool(MaskDrawerPlugin2D maskDrawerPlugin, String name, String description, Icon icon) {
        super(maskDrawerPlugin.getViewerPanel());
        this.maskDrawerPlugin = maskDrawerPlugin;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public MaskDrawerPlugin2D getMaskDrawerPlugin() {
        return maskDrawerPlugin;
    }

    public String getName() {
        return name;
    }

    public boolean toolIsActive() {
        return toolIsActive(getMaskDrawerPlugin().getViewerPanel2D().getCanvas());
    }

    @Override
    public String getToolName() {
        return getName();
    }

    public String getDescription() {
        return description;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean showGuides() {
        return true;
    }

    @Override
    public String getCategory() {
        return null;
    }

    @Override
    public Icon getCategoryIcon() {
        return null;
    }

    /**
     * Posts a mask changed event to the viewer' canvas event bus
     */
    public void postMaskChangedEvent() {
        getViewerPanel2D().getCanvas().getEventBus().post(new MaskDrawerPlugin2D.MaskChangedEvent(getMaskDrawerPlugin()));
    }

    /**
     * Triggered when the highlight color was changed
     */
    public void onHighlightColorChanged() {
    }
}
