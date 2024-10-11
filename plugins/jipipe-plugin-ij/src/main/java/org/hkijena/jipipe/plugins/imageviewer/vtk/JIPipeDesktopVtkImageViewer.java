package org.hkijena.jipipe.plugins.imageviewer.vtk;

import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.JIPipeDesktopRibbon;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import vtk.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

public class JIPipeDesktopVtkImageViewer extends JIPipeDesktopWorkbenchPanel {

    private vtkPanel renderer;
    private final Timer resizeRendererTimer;

    public JIPipeDesktopVtkImageViewer(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        initialize();
        this.resizeRendererTimer = new Timer(250, e -> updateRendererSize());
        this.resizeRendererTimer.setRepeats(false);
    }



    private void initialize() {
        setLayout(new BorderLayout());
    }

    public void buildRibbon(JIPipeDesktopRibbon ribbon) {

    }

    public void buildDock(JIPipeDesktopDockPanel dockPanel) {

    }

    public void startRenderer() {
        vtkConeSource cone = new vtkConeSource();
        cone.SetResolution(8);

        vtkPolyDataMapper coneMapper = new vtkPolyDataMapper();
        coneMapper.SetInputConnection(cone.GetOutputPort());

        vtkActor coneActor = new vtkActor();
        coneActor.SetMapper(coneMapper);

        renderer = new vtkPanel();
        renderer.GetRenderer().AddActor(coneActor);
        renderer.Report();
        renderer.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizeRendererTimer.restart();
            }
        });

        add(renderer, BorderLayout.CENTER);
    }

    private void updateRendererSize() {
        if (renderer != null) {
            renderer.lock();
            renderer.GetRenderWindow().SetSize(renderer.getWidth(), renderer.getHeight());
            renderer.unlock();
            UIUtils.repaintLater(renderer);
        }
    }

    public static void main(String[] args) {
        JIPipeDesktopUITheme.ModernLight.install();
        VTKUtils.loadVtkNativeLibraries();

        JFrame frame = new JFrame();
        JIPipeDesktopVtkImageViewer imageViewer = new JIPipeDesktopVtkImageViewer(new JIPipeDesktopDummyWorkbench());

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setTitle("JIPipe Image Viewer Test");
        JPanel panel = new JPanel(new BorderLayout());
        JIPipeDesktopRibbon ribbon = new JIPipeDesktopRibbon();
        JIPipeDesktopDockPanel dockPanel = new JIPipeDesktopDockPanel();

        imageViewer.buildRibbon(ribbon);
        imageViewer.buildDock(dockPanel);

        panel.add(ribbon, BorderLayout.NORTH);
        panel.add(dockPanel, BorderLayout.CENTER);
        dockPanel.setBackgroundComponent(imageViewer);

        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setSize(1920,1080);
        frame.setVisible(true);

        imageViewer.startRenderer();
    }

}
