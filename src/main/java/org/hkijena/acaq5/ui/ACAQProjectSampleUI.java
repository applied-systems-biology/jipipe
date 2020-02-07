package org.hkijena.acaq5.ui;

import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.ACAQProjectSample;
import org.hkijena.acaq5.api.events.ACAQSampleRenamedEvent;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;

public class ACAQProjectSampleUI extends ACAQUIPanel {
    private ACAQProjectSample sample;
    private JLabel sampleTitle;
    private ACAQAlgorithmGraphUI algorithmGraphUI;

    public ACAQProjectSampleUI(ACAQWorkbenchUI workbenchUI, ACAQProjectSample sample) {
        super(workbenchUI);
        this.sample = sample;
        initialize();
        getProject().getEventBus().register(this);
    }

    private void initialize() {
        setLayout(new BorderLayout());
        initializeTitlePanel();

        algorithmGraphUI = new ACAQAlgorithmGraphUI(getWorkbenchUI(), sample.getPreprocessingGraph());
        add(algorithmGraphUI, BorderLayout.CENTER);
    }

    private void initializeTitlePanel() {
        JToolBar panel = new JToolBar();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        sampleTitle = new JLabel(sample.getName(), UIUtils.getIconFromResources("sample.png"), SwingConstants.LEFT);
        panel.add(sampleTitle);

        panel.add(Box.createHorizontalGlue());

        JButton renameButton = new JButton("Rename", UIUtils.getIconFromResources("edit.png"));
        renameButton.addActionListener(e -> renameSample());
        panel.add(renameButton);

        JButton removeButton = new JButton(UIUtils.getIconFromResources("delete.png"));
        removeButton.addActionListener(e -> removeSample());
        panel.add(removeButton);

        add(panel, BorderLayout.NORTH);
    }

    private void renameSample() {
        String newName = JOptionPane.showInputDialog(this,"Please input a new name", sample.getName());
        if(newName != null && !newName.isEmpty() && !newName.equals(sample.getName())) {
            getProject().renameSample(sample, newName);
        }
    }

    private void removeSample() {
        getProject().removeSample(getSample());
    }

    @Subscribe
    public void onSampleRenamed(ACAQSampleRenamedEvent event) {
        if(event.getSample() == sample) {
            sampleTitle.setText(sample.getName());
        }
    }

    public ACAQProjectSample getSample() {
        return sample;
    }
}
