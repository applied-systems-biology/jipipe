package org.hkijena.jipipe.extensions.imageviewer;

import ij.ImagePlus;
import ij3d.*;
import org.hkijena.jipipe.ui.JIPipeWorkbench;
import org.hkijena.jipipe.ui.JIPipeWorkbenchAccess;
import org.scijava.Disposable;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.RenderingErrorListener;
import org.scijava.java3d.View;
import org.scijava.java3d.utils.universe.SimpleUniverse;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Method;

public class ImageViewerPanel3D extends JPanel implements JIPipeWorkbenchAccess, Disposable, UniverseListener {
    private final ImageViewerPanel imageViewerPanel;
    private ImagePlus image;
    private final JPanel contentPanel = new JPanel(new BorderLayout());

    private Image3DUniverse universe;

    private Canvas3D canvas3D;

    private boolean noOffScreen = true;

    private boolean active = false;

    public ImageViewerPanel3D(ImageViewerPanel imageViewerPanel) {
        this.imageViewerPanel = imageViewerPanel;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        add(contentPanel, BorderLayout.CENTER);
    }

    @Override
    public JIPipeWorkbench getWorkbench() {
        return imageViewerPanel.getWorkbench();
    }

    public ImageViewerPanel getImageViewerPanel() {
        return imageViewerPanel;
    }

    public ImagePlus getImage() {
        return image;
    }

    public void setImage(ImagePlus image) {
        this.image = image;
        updateImage();
    }

    public void activate() {
        if(!active) {
            active = true;
            updateImage();
        }
    }

    private void updateImage() {
        if(!active)
            return;
        if(image == null && universe == null)
            return;
        if(universe == null) {
            initializeUniverse();
        }
        universe.removeAllContents();
        if(image != null) {
            universe.getExecuter().addContent(image, null);
        }
    }

    private void initializeUniverse() {
        universe = new Image3DUniverse();
        universe.addUniverseListener(this);
        final String j3dNoOffScreen = System.getProperty("j3d.noOffScreen");
        if (j3dNoOffScreen != null && j3dNoOffScreen.equals("true"))
            noOffScreen = true;
        canvas3D = universe.getCanvas();

        contentPanel.add(canvas3D, BorderLayout.CENTER);

        universe.ui.setHandTool();
    }

    @Override
    public void dispose() {
        setImage(null);
        if(universe != null) {
            universe.removeUniverseListener(this);

//        // Must remove the listener so this instance can be garbage
//        // collected and removed from the Canvas3D, overcomming the limit
//        // of 32 total Canvas3D instances.
//        try {
//            final Method m =
//                    SimpleUniverse.class.getMethod("removeRenderingErrorListener",
//                            RenderingErrorListener.class);
//            m.invoke(universe, new Object[] { error_listener });
//        }
//        catch (final Exception ex) {
//            System.out.println("Could NOT remove the RenderingErrorListener!");
//            ex.printStackTrace();
//        }
//        imp_updater.quit();

            universe.cleanup();
//        canvas3D.flush();
            universe = null;
        }
    }

    @Override
    public void transformationStarted(View view) {

    }

    @Override
    public void transformationUpdated(View view) {

    }

    @Override
    public void transformationFinished(View view) {

    }

    @Override
    public void contentAdded(Content c) {

    }

    @Override
    public void contentRemoved(Content c) {

    }

    @Override
    public void contentChanged(Content c) {

    }

    @Override
    public void contentSelected(Content c) {

    }

    @Override
    public void canvasResized() {

    }

    @Override
    public void universeClosed() {

    }
}
