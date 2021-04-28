package org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.maskdrawer;

import org.hkijena.jipipe.extensions.imagejdatatypes.viewer.plugins.ImageViewerPanelPlugin;

import javax.swing.*;

/**
 * Super class for any tool that can be used in {@link MaskDrawerPlugin}
 */
public abstract class MaskDrawerTool extends ImageViewerPanelPlugin {
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

    public String getDescription() {
        return description;
    }

    public Icon getIcon() {
        return icon;
    }

    public boolean isActive() {
        return maskDrawerPlugin.getCurrentTool() == this;
    }

    public abstract void activate();

    public abstract void deactivate();

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
