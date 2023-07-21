package org.hkijena.jipipe.extensions.graphannotation.tools;

import ij.IJ;
import org.hkijena.jipipe.api.nodes.JIPipeAnnotationGraphNodeTool;
import org.hkijena.jipipe.extensions.graphannotation.nodes.ImageBoxAnnotationGraphNode;
import org.hkijena.jipipe.extensions.parameters.library.images.ImageParameter;
import org.hkijena.jipipe.extensions.settings.FileChooserSettings;
import org.hkijena.jipipe.ui.grapheditor.JIPipeGraphViewMode;
import org.hkijena.jipipe.utils.BufferedImageUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class ImageBoxAnnotationGraphNodeTool extends JIPipeAnnotationGraphNodeTool<ImageBoxAnnotationGraphNode> {
    public ImageBoxAnnotationGraphNodeTool() {
        super(ImageBoxAnnotationGraphNode.class);
    }

    @Override
    protected ImageBoxAnnotationGraphNode createAndConfigureNode(Point firstPoint, Point secondPoint) {
        ImageBoxAnnotationGraphNode node = super.createAndConfigureNode(firstPoint, secondPoint);
        Path path = FileChooserSettings.openFile(getWorkbench().getWindow(),
                FileChooserSettings.LastDirectoryKey.External,
                "Open image",
                UIUtils.EXTENSION_FILTER_IMAGEIO_IMAGES);
        if (path != null) {
            try {
                BufferedImage image = ImageIO.read(path.toFile());
                int w = Math.abs(firstPoint.x - secondPoint.x);
                int h = Math.abs(firstPoint.y - secondPoint.y);
                double maxNodeWidth = JIPipeGraphViewMode.VerticalCompact.getGridWidth() * 2 * w;
                double maxNodeHeight = JIPipeGraphViewMode.VerticalCompact.getGridHeight() * 2 * h;
                double scaleFactor = Math.min(1, Math.max(maxNodeWidth / image.getWidth(), maxNodeHeight / image.getHeight()));
                if (scaleFactor != 1) {
                    image = BufferedImageUtils.toBufferedImage(image.getScaledInstance((int) (scaleFactor * image.getWidth()), (int) (scaleFactor * image.getHeight()), Image.SCALE_SMOOTH), BufferedImage.TYPE_INT_RGB);
                }
                node.getImageParameters().setImage(new ImageParameter(image));
            } catch (IOException e) {
                IJ.handleException(e);
            }
        }
        return node;
    }

    @Override
    public KeyStroke getKeyBinding() {
        return KeyStroke.getKeyStroke(KeyEvent.VK_F7, 0);
    }

    @Override
    public int getPriority() {
        return -4700;
    }
}
