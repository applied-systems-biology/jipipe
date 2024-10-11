package org.hkijena.jipipe.plugins.imageviewer.vtk;

import org.hkijena.jipipe.utils.UIUtils;
import org.scijava.Disposable;
import vtk.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Wrapper around the VTK panel that sanitizes some of its behaviors
 */
public class VtkPanel extends JPanel implements Disposable {

    private final Timer resizeRendererTimer;
    private NativeVtkPanelWrapper nativeVtkPanelWrapper;
    private InteractionMode interactionMode = InteractionMode.Rotate;

    public VtkPanel() {
        this.resizeRendererTimer = new Timer(250, e -> updateRendererSize());
        this.resizeRendererTimer.setRepeats(false);
        this.nativeVtkPanelWrapper = new NativeVtkPanelWrapper();
        setLayout(new BorderLayout());
        add(nativeVtkPanelWrapper, BorderLayout.CENTER);
        nativeVtkPanelWrapper.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeRendererTimer.restart();
            }
        });
        nativeVtkPanelWrapper.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
               onNativePanelMouseDragged(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                onNativePanelMousePressed(e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                onNativePanelMouseEntered(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                onNativePanelMouseReleased(e);
            }
        });
        nativeVtkPanelWrapper.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                onNativePanelMouseMoved(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                onNativePanelMouseDragged(e);
            }
        });
        nativeVtkPanelWrapper.addMouseWheelListener(this::onNativePanelMouseWheelMoved);
    }

    private void onNativePanelMouseWheelMoved(MouseWheelEvent e) {
        vtkRenderer ren = nativeVtkPanelWrapper.GetRenderer();
        vtkCamera cam = nativeVtkPanelWrapper.getCamera();
        vtkRenderWindow rw = nativeVtkPanelWrapper.getRenderWindow();
        if (ren.VisibleActorCount() != 0 && e.getWheelRotation() != 0) {
            int exponent = -10 * e.getWheelRotation() / Math.abs(e.getWheelRotation());
            double zoomFactor = Math.pow(1.02, (double)exponent);
            if (cam.GetParallelProjection() == 1) {
                cam.SetParallelScale(cam.GetParallelScale() / zoomFactor);
            } else {
                cam.Dolly(zoomFactor);
                nativeVtkPanelWrapper.resetCameraClippingRange();
            }

            nativeVtkPanelWrapper.Render();
        }
    }

    private void onNativePanelMouseReleased(MouseEvent e) {
        nativeVtkPanelWrapper.getRenderWindow().SetDesiredUpdateRate(0.01);
    }

    private void onNativePanelMouseEntered(MouseEvent e) {
        nativeVtkPanelWrapper.requestFocus();
    }

    private void onNativePanelMousePressed(MouseEvent e) {
        vtkRenderer ren = nativeVtkPanelWrapper.GetRenderer();
        vtkRenderWindow rw = nativeVtkPanelWrapper.getRenderWindow();
        if (ren.VisibleActorCount() != 0) {
            rw.SetDesiredUpdateRate(5.0);
            nativeVtkPanelWrapper.setLastX(e.getX());
            nativeVtkPanelWrapper.setLastY(e.getY());
            int mods = e.getModifiersEx();
            int ShiftMouseButton1 = 1088;
            int MouseButton2 = 2048;
            int MouseButton3 = 4096;
            if ((mods & MouseButton2) != MouseButton2 && (mods & ShiftMouseButton1) != ShiftMouseButton1) {
                if ((mods & MouseButton3) == MouseButton3) {
                    setInteractionMode(InteractionMode.Zoom);
                } else {
                    setInteractionMode(InteractionMode.Rotate);
                }
            } else {
                setInteractionMode(InteractionMode.Translate);
            }

        }
    }

    private void onNativePanelMouseMoved(MouseEvent e) {
        nativeVtkPanelWrapper.setLastX(e.getX());
        nativeVtkPanelWrapper.setLastY(e.getY());
    }

    private void onNativePanelMouseDragged(MouseEvent e) {
        vtkRenderer ren = nativeVtkPanelWrapper.GetRenderer();
        vtkCamera cam = nativeVtkPanelWrapper.getCamera();
        vtkRenderWindow rw = nativeVtkPanelWrapper.getRenderWindow();

        if (ren.VisibleActorCount() != 0) {
            int x = e.getX();
            int y = e.getY();
            if (interactionMode == InteractionMode.Rotate) {
                cam.Azimuth(nativeVtkPanelWrapper.getLastX() - x);
                cam.Elevation(y - nativeVtkPanelWrapper.getLastY());
                cam.OrthogonalizeViewUp();
                nativeVtkPanelWrapper.resetCameraClippingRange();
                if (nativeVtkPanelWrapper.isLightFollowCamera()) {
                    nativeVtkPanelWrapper.getLight().SetPosition(cam.GetPosition());
                    nativeVtkPanelWrapper.getLight().SetFocalPoint(cam.GetFocalPoint());
                }
            }

            if (interactionMode == InteractionMode.Translate) {
                double[] APoint = new double[3];
                double[] FPoint = cam.GetFocalPoint();
                double[] PPoint = cam.GetPosition();
                ren.SetWorldPoint(FPoint[0], FPoint[1], FPoint[2], 1.0);
                ren.WorldToDisplay();
                double focalDepth = ren.GetDisplayPoint()[2];
                APoint[0] = (double) rw.GetSize()[0] / 2.0 + (double)(x - nativeVtkPanelWrapper.getLastX());
                APoint[1] = (double) rw.GetSize()[1] / 2.0 - (double)(y - nativeVtkPanelWrapper.getLastY());
                APoint[2] = focalDepth;
                ren.SetDisplayPoint(APoint);
                ren.DisplayToWorld();
                double[] RPoint = ren.GetWorldPoint();
                if (RPoint[3] != 0.0) {
                    RPoint[0] /= RPoint[3];
                    RPoint[1] /= RPoint[3];
                    RPoint[2] /= RPoint[3];
                }

                cam.SetFocalPoint((FPoint[0] - RPoint[0]) / 2.0 + FPoint[0], (FPoint[1] - RPoint[1]) / 2.0 + FPoint[1], (FPoint[2] - RPoint[2]) / 2.0 + FPoint[2]);
                cam.SetPosition((FPoint[0] - RPoint[0]) / 2.0 + PPoint[0], (FPoint[1] - RPoint[1]) / 2.0 + PPoint[1], (FPoint[2] - RPoint[2]) / 2.0 + PPoint[2]);
                nativeVtkPanelWrapper.resetCameraClippingRange();
            }

            if (interactionMode == InteractionMode.Zoom) {
                double zoomFactor = Math.pow(1.02, y - nativeVtkPanelWrapper.getLastY());
                if (cam.GetParallelProjection() == 1) {
                    cam.SetParallelScale(cam.GetParallelScale() / zoomFactor);
                } else {
                    cam.Dolly(zoomFactor);
                    nativeVtkPanelWrapper.resetCameraClippingRange();
                }
            }

            nativeVtkPanelWrapper.setLastX(x);
            nativeVtkPanelWrapper.setLastY(y);
            nativeVtkPanelWrapper.Render();
        }
    }

    public void setInteractionMode(InteractionMode interactionMode) {
        this.interactionMode = interactionMode;
    }

    public InteractionMode getInteractionMode() {
        return interactionMode;
    }

    public vtkRenderer getRenderer() {
        return nativeVtkPanelWrapper.GetRenderer();
    }

    public NativeVtkPanelWrapper getNativeVtkPanelWrapper() {
        return nativeVtkPanelWrapper;
    }

    private void updateRendererSize() {
        if (nativeVtkPanelWrapper != null) {
            nativeVtkPanelWrapper.lock();
            nativeVtkPanelWrapper.GetRenderWindow().SetSize(nativeVtkPanelWrapper.getWidth(), nativeVtkPanelWrapper.getHeight());
            nativeVtkPanelWrapper.unlock();
            UIUtils.repaintLater(nativeVtkPanelWrapper);
        }
    }

    @Override
    public void dispose() {
        nativeVtkPanelWrapper.Delete();
        nativeVtkPanelWrapper = null;
    }

    public enum InteractionMode {
        Rotate,
        Translate,
        Zoom
    }

    /**
     * Internal panel.
     */
    public static class NativeVtkPanelWrapper extends vtkPanel {
        public NativeVtkPanelWrapper() {
            removeNativeEventListeners();
        }

        public NativeVtkPanelWrapper(vtkRenderWindow renwin) {
            super(renwin);
            removeNativeEventListeners();
        }

        /**
         * We will implement our own listeners
         */
        private void removeNativeEventListeners() {
            removeMouseListener(this);
            removeMouseMotionListener(this);
            removeMouseWheelListener(this);
            removeKeyListener(this);
        }

        public boolean isLightFollowCamera() {
            return LightFollowCamera == 1;
        }

        public void setLightFollowCamera(boolean lightFollowCamera) {
            this.LightFollowCamera = lightFollowCamera ? 1 : 0;
        }

        public int getLastX() {
            return lastX;
        }

        public void setLastX(int lastX) {
            this.lastX = lastX;
        }

        public int getLastY() {
            return lastY;
        }

        public void setLastY(int lastY) {
            this.lastY = lastY;
        }

        public vtkCamera getCamera() {
            return this.cam;
        }

        public vtkLight getLight() {
            return this.lgt;
        }

        public vtkRenderWindow getRenderWindow() {
            return GetRenderWindow();
        }
    }
}
