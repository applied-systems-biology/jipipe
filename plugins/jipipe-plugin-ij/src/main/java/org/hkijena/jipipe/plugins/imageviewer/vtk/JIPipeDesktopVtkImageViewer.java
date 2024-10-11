package org.hkijena.jipipe.plugins.imageviewer.vtk;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopDummyWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbench;
import org.hkijena.jipipe.desktop.app.JIPipeDesktopWorkbenchPanel;
import org.hkijena.jipipe.desktop.commons.components.ribbon.*;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopUITheme;
import org.hkijena.jipipe.plugins.imageviewer.vtk.tools.ViewDefaultTool;
import org.hkijena.jipipe.plugins.imageviewer.vtk.tools.ViewRotateTool;
import org.hkijena.jipipe.plugins.imageviewer.vtk.tools.ViewTranslateTool;
import org.hkijena.jipipe.plugins.imageviewer.vtk.tools.ViewZoomTool;
import org.hkijena.jipipe.utils.UIUtils;
import org.hkijena.jipipe.utils.ui.JIPipeDesktopDockPanel;
import vtk.*;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class JIPipeDesktopVtkImageViewer extends JIPipeDesktopWorkbenchPanel {

    private VtkPanel renderer;
    private final BiMap<Class<? extends JIPipeDesktopVtkImageViewerTool>, JIPipeDesktopVtkImageViewerTool> viewInteractionTools
            = HashBiMap.create();
    private final BiMap<Class<? extends JIPipeDesktopVtkImageViewerTool>, JIPipeDesktopToggleButtonRibbonAction> viewInteractionToolButtons
            = HashBiMap.create();
    private JIPipeDesktopVtkImageViewerTool currentTool;

    public JIPipeDesktopVtkImageViewer(JIPipeDesktopWorkbench desktopWorkbench) {
        super(desktopWorkbench);
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
    }

    public void buildRibbon(JIPipeDesktopRibbon ribbon) {
        JIPipeDesktopRibbon.Task generalTask = ribbon.getOrCreateTask("General");
        JIPipeDesktopRibbon.Band toolsBand = generalTask.getOrCreateBand("Tools");
        JIPipeDesktopRibbon.Band cameraBand = generalTask.getOrCreateBand("View");

        addToolToRibbon(new ViewDefaultTool(), toolsBand, true);
        addToolToRibbon(new ViewRotateTool(), toolsBand, false);
        addToolToRibbon(new ViewTranslateTool(), toolsBand, false);
        addToolToRibbon(new ViewZoomTool(), toolsBand, false);

        // Reset camera button
        cameraBand.add(new JIPipeDesktopLargeButtonRibbonAction("Reset",
                "Resets the view",
                UIUtils.getIcon32FromResources("actions/edit-reset.png"),
                this::resetView));
    }

    public void switchTool(Class<? extends JIPipeDesktopVtkImageViewerTool> klass) {
       if(viewInteractionToolButtons.containsKey(klass)) {
           if(currentTool != null && currentTool.getClass() == klass) {
               currentTool.onDeactivate(this);
           }
           for (Map.Entry<Class<? extends JIPipeDesktopVtkImageViewerTool>,
                   JIPipeDesktopToggleButtonRibbonAction> entry : viewInteractionToolButtons.entrySet()) {
               entry.getValue().setSelected(entry.getKey() == klass);
           }
           currentTool = viewInteractionTools.get(klass);
           currentTool.onActivate(this);
           if(renderer != null) {
               renderer.setInteractionTool(currentTool.getInteractionTool());
           }
       }
    }

    public void addToolToRibbon(JIPipeDesktopVtkImageViewerTool tool, JIPipeDesktopRibbon.Band band, boolean large) {
        JIPipeDesktopToggleButtonRibbonAction action;
        if(large) {
            action = new JIPipeDesktopLargeToggleButtonRibbonAction(tool.getLabel(),
                    tool.getDescription(),
                    tool.getIcon32(),
                    false,
                    button -> {
                        if(button.isSelected()) {
                            switchTool(tool.getClass());
                        }
                        else {
                            switchTool(ViewDefaultTool.class);
                        }
                    });
        }
        else {
            action = new JIPipeDesktopSmallToggleButtonRibbonAction(tool.getLabel(),
                    tool.getDescription(),
                    tool.getIcon16(),
                    false,
                    button -> {
                        if(button.isSelected()) {
                            switchTool(tool.getClass());
                        }
                        else {
                            switchTool(ViewDefaultTool.class);
                        }
                    });
        }
        band.add(action);
        viewInteractionTools.put(tool.getClass(), tool);
        viewInteractionToolButtons.put(tool.getClass(), action);
    }

    private void resetView() {
        renderer.getNativeVtkPanelWrapper().resetCamera();
        renderer.getNativeVtkPanelWrapper().Render();
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

        renderer = new VtkPanel();
        renderer.getRenderer().AddActor(coneActor);
        renderer.getNativeVtkPanelWrapper().Report();

        add(renderer, BorderLayout.CENTER);
        switchTool(ViewDefaultTool.class);
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
        ribbon.rebuildRibbon();

        panel.add(ribbon, BorderLayout.NORTH);
        panel.add(dockPanel, BorderLayout.CENTER);
        dockPanel.setBackgroundComponent(imageViewer);

        frame.getContentPane().add(panel, BorderLayout.CENTER);
        frame.pack();
        frame.setSize(1920,1080);
        frame.setVisible(true);

        imageViewer.startRenderer();
    }

    public JIPipeDesktopVtkImageViewerTool getCurrentTool() {
        return currentTool;
    }
}
