package org.hkijena.jipipe.extensions.imageviewer.utils.viewer3d;

import ij3d.Image3DUniverse;

public class CustomImage3DUniverse extends Image3DUniverse {
    private final CustomInteractiveViewPlatformTransformer customInteractiveViewPlatformTransformer;

    public CustomImage3DUniverse() {
        customInteractiveViewPlatformTransformer = new CustomInteractiveViewPlatformTransformer(this, this);
    }

    public CustomInteractiveViewPlatformTransformer getCustomInteractiveViewPlatformTransformer() {
        return customInteractiveViewPlatformTransformer;
    }
}
