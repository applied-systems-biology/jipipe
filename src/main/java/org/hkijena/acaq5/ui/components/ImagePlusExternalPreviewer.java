package org.hkijena.acaq5.ui.components;

import ij.IJ;
import ij.ImagePlus;

import java.awt.*;

public class ImagePlusExternalPreviewer {
    private ImagePlus currentImage;
    private Point lastWindowLocation;
    private Dimension lastWindowSize;

    public ImagePlusExternalPreviewer() {

    }

    public ImagePlus getCurrentImage() {
        return currentImage;
    }

    public void setCurrentImage(ImagePlus newImage) {
        backupLocationAndSize();
        hide();
        currentImage = newImage.duplicate();
        if(currentImage != null) {
            currentImage.setTitle("Preview");
        }
        show();
    }

    public void show() {
        if(currentImage != null) {
            currentImage.show();
            if(currentImage.getWindow() == null) {
                IJ.wait(100);
            }
            if(currentImage.getWindow() != null && lastWindowLocation != null && lastWindowSize != null) {
                currentImage.getWindow().setBounds(lastWindowLocation.x,
                        lastWindowLocation.y,
                        lastWindowSize.width,
                        lastWindowSize.height);
                currentImage.getWindow().getCanvas().fitToWindow();
            }
        }
    }

    public void hide() {
        if(currentImage != null) {
            currentImage.hide();
        }
    }

    private void backupLocationAndSize() {
        if(currentImage != null && currentImage.getWindow() != null) {
            lastWindowLocation = currentImage.getWindow().getLocation();
            lastWindowSize = currentImage.getWindow().getSize();
        }
    }
}
