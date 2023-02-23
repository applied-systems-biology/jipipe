package org.hkijena.jipipe.extensions.imageviewer.plugins.maskdrawer;

import org.hkijena.jipipe.extensions.imageviewer.ImageViewerPanelCanvasTool;
import org.hkijena.jipipe.extensions.imageviewer.plugins.ImageViewerPanelPlugin;

import javax.swing.*;

/**
 * Super class for any tool that can be used in {@link MaskDrawerPlugin}
 */
public abstract class MaskDrawerTool extends ImageViewerPanelPlugin implements ImageViewerPanelCanvasTool {
    private final MaskDrawerPlugin maskDrawerPlugin;
    private final String name;
    private final String description;
    private final Icon icon;

    public MaskDrawerTool(MaskDrawerPlugin maskDrawerPlugin, String name, String description, Icon icon) {
        super(maskDrawerPlugin.getViewerPanel());
        this.maskDrawerPlugin = maskDrawerPlugin;
        this.name = name;
        this.description = description;
        this.icon = icon;
    }

    public MaskDrawerPlugin getMaskDrawerPlugin() {
        return maskDrawerPlugin;
    }

    public String getName() {
        return name;
    }

    public boolean toolIsActive() {
        return toolIsActive(getMaskDrawerPlugin().getViewerPanel().getCanvas());
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
        getViewerPanel().getCanvas().getEventBus().post(new MaskDrawerPlugin.MaskChangedEvent(getMaskDrawerPlugin()));
    }

    /**
     * Triggered when the highlight color was changed
     */
    public void onHighlightColorChanged() {
    }
}
