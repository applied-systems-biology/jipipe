package org.hkijena.acaq5.ui.grapheditor.algorithmfinder;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.ACAQRegistryService;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmDeclaration;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithmGraph;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQCompartmentOutput;
import org.hkijena.acaq5.api.data.ACAQData;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.data.ACAQMutableSlotConfiguration;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.registries.ACAQDatatypeRegistry;
import org.hkijena.acaq5.ui.events.AlgorithmFinderSuccessEvent;
import org.hkijena.acaq5.ui.registries.ACAQUIDatatypeRegistry;
import org.hkijena.acaq5.utils.StringUtils;
import org.hkijena.acaq5.utils.TooltipUtils;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Set;

import static org.hkijena.acaq5.ui.grapheditor.ACAQAlgorithmUI.SLOT_UI_HEIGHT;

public class ACAQAlgorithmFinderAlgorithmUI extends JPanel {
    private ACAQDataSlot outputSlot;
    private ACAQAlgorithmGraph graph;
    private ACAQAlgorithm algorithm;
    private int score;
    private int maxScore;
    private boolean isExistingInstance;
    private JPanel slotPanel;
    private EventBus eventBus = new EventBus();
    private String compartment;

    public ACAQAlgorithmFinderAlgorithmUI(ACAQDataSlot outputSlot, ACAQAlgorithmGraph graph, String compartment, ACAQAlgorithmDeclaration declaration, int score, int maxScore) {
        this.outputSlot = outputSlot;
        this.graph = graph;
        this.compartment = compartment;
        this.score = score;
        this.maxScore = maxScore;
        this.algorithm = declaration.newInstance();
        this.isExistingInstance = false;

        initialize();
    }

    public ACAQAlgorithmFinderAlgorithmUI(ACAQDataSlot outputSlot, ACAQAlgorithmGraph graph, String compartment, ACAQAlgorithm algorithm, int score, int maxScore) {
        this.outputSlot = outputSlot;
        this.graph = graph;
        this.compartment = compartment;
        this.score = score;
        this.maxScore = maxScore;
        this.algorithm = algorithm;
        this.isExistingInstance = true;

        initialize();
    }

    private void initialize() {
        initializeUI();
        reloadSlotUI();
        this.algorithm.getEventBus().register(this);
    }

    private void initializeUI() {
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(4,4,4,4),
                BorderFactory.createLineBorder(Color.DARK_GRAY, 1, true)));
        setLayout(new BorderLayout());

        JPanel centerPanel = new JPanel(new BorderLayout());

        StringBuilder title = new StringBuilder();
        if(isExistingInstance)
            title.append("<span style=\"color: red;\">Existing </span>");
        else
            title.append("<span style=\"color: grey;\">Create </span>");
        title.append("<span style=\"font-size: 16pt;\">").append(algorithm.getName()).append("</font>");

        double stars = (maxScore > 0 ? (Math.max(0, score) * 1.0 / maxScore) : 1.0) * 5.0;
        JLabel starsLabel = UIUtils.createStarRatingLabel(title.toString(), stars, 5);
        centerPanel.add(starsLabel, BorderLayout.NORTH);

        JLabel label = new JLabel();
        label.setBorder(BorderFactory.createEmptyBorder(8,8,8,8));
        label.setText(TooltipUtils.getAlgorithmTooltip(algorithm.getDeclaration(), false));
        centerPanel.add(label, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        JPanel colorPanel = new JPanel();
        colorPanel.setBackground(UIUtils.getFillColorFor(algorithm.getDeclaration()));
        colorPanel.setPreferredSize(new Dimension(16, 1));
        add(colorPanel, BorderLayout.WEST);

        slotPanel = new JPanel();
        slotPanel.setLayout(new BoxLayout(slotPanel, BoxLayout.Y_AXIS));
        add(slotPanel, BorderLayout.EAST);
    }

    public void reloadSlotUI() {
        slotPanel.removeAll();

        boolean createAddInputSlotButton = false;

        if(algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();
            createAddInputSlotButton = slotConfiguration.allowsInputSlots() && !slotConfiguration.isInputSlotsSealed();
        }

        for(ACAQDataSlot slot : algorithm.getInputSlots()) {
            ACAQAlgorithmFinderSlotUI ui = new ACAQAlgorithmFinderSlotUI(outputSlot, graph, compartment, slot, isExistingInstance);
            ui.getEventBus().register(this);
            slotPanel.add(ui);
        }

        if(createAddInputSlotButton) {
            JButton addInputSlotButton = createAddSlotButton(ACAQDataSlot.SlotType.Input);
            addInputSlotButton.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
            JPanel panel = new JPanel(new BorderLayout());
//            panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,0,1, getAlgorithmBorderColor()),
//                    BorderFactory.createEmptyBorder(0,0,0,4)));
            panel.add(addInputSlotButton, BorderLayout.WEST);
            slotPanel.add(panel);
        }

        slotPanel.revalidate();
        slotPanel.repaint();
    }

    private JButton createAddSlotButton(ACAQDataSlot.SlotType slotType) {
        JButton button = new JButton(UIUtils.getIconFromResources("add.png"));
        UIUtils.makeFlat(button);

        JPopupMenu menu = UIUtils.addPopupMenuToComponent(button);
        ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration)algorithm.getSlotConfiguration();

        Set<Class<? extends ACAQData>> allowedSlotTypes;
        switch(slotType) {
            case Input:
                allowedSlotTypes = slotConfiguration.getAllowedInputSlotTypes();
                break;
            case Output:
                allowedSlotTypes = slotConfiguration.getAllowedOutputSlotTypes();
                break;
            default:
                throw new RuntimeException();
        }

        for(Class<? extends ACAQData> dataClass : allowedSlotTypes) {
            JMenuItem item = new JMenuItem(ACAQData.getNameOf(dataClass), ACAQUIDatatypeRegistry.getInstance().getIconFor(dataClass));
            item.addActionListener(e -> addNewSlot(slotType, dataClass));
            menu.add(item);
        }

        return button;
    }

    private void addNewSlot(ACAQDataSlot.SlotType slotType, Class<? extends ACAQData> klass) {
        if(algorithm.getSlotConfiguration() instanceof ACAQMutableSlotConfiguration) {
            ACAQMutableSlotConfiguration slotConfiguration = (ACAQMutableSlotConfiguration) algorithm.getSlotConfiguration();

            int existingSlots = slotType == ACAQDataSlot.SlotType.Input ? algorithm.getInputSlots().size() : algorithm.getOutputSlots().size();
            String initialValue = slotType + " data ";

            // This is general
            if(algorithm instanceof ACAQCompartmentOutput) {
                initialValue = "Data ";
            }

            String name = null;
            while(name == null) {
                String newName = JOptionPane.showInputDialog(this,"Please a data slot name", initialValue + (existingSlots + 1));
                if(newName == null || newName.trim().isEmpty())
                    return;
                newName = StringUtils.makeFilesystemCompatible(newName);
                if(slotConfiguration.hasSlot(newName))
                    continue;
                name = newName;
            }
            switch (slotType) {
                case Input:
                    slotConfiguration.addInputSlot(name, klass);
                    break;
                case Output:
                    slotConfiguration.addOutputSlot(name, klass);
                    break;
            }
        }
    }

    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        reloadSlotUI();
    }

    @Subscribe
    public void onAlgorithmFinderSuccess(AlgorithmFinderSuccessEvent event) {
        eventBus.post(event);
    }

    public EventBus getEventBus() {
        return eventBus;
    }
}
