package org.hkijena.jipipe.utils;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class CopyImageToClipboard implements ClipboardOwner {

    public void copyImage(BufferedImage bi) {

        // We must convert to RGB, otherwise Java runs into issues due to some internal JPEG conversion
        BufferedImage newRGB = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_RGB);
        newRGB.createGraphics().drawImage(bi, 0, 0, bi.getWidth(), bi.getHeight(), null);

        TransferableImage trans = new TransferableImage(newRGB);
        Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
        c.setContents(trans, this);
    }

    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {

    }

    private static class TransferableImage implements Transferable {
        Image i;

        public TransferableImage(Image i) {
            this.i = i;
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if (flavor.equals(DataFlavor.imageFlavor) && i != null) {
                return i;
            } else {
                throw new UnsupportedFlavorException(flavor);
            }
        }

        public DataFlavor[] getTransferDataFlavors() {
            DataFlavor[] flavors = new DataFlavor[1];
            flavors[0] = DataFlavor.imageFlavor;
            return flavors;
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            DataFlavor[] flavors = getTransferDataFlavors();
            for (int i = 0; i < flavors.length; i++) {
                if (flavor.equals(flavors[i])) {
                    return true;
                }
            }

            return false;
        }
    }
}
