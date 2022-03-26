package org.hkijena.jipipe;


import net.imagej.ImageJ;

public class SingleAlgorithmDebug {
    public static void main(String[] args) {
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();
        ij.command().run(JIPipeRunAlgorithmCommand.class, true);
    }
}
