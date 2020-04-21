package org.hkijena.acaq5.ui.grapheditor;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.hkijena.acaq5.api.algorithm.ACAQAlgorithm;
import org.hkijena.acaq5.api.compartments.algorithms.ACAQProjectCompartment;
import org.hkijena.acaq5.api.data.ACAQDataSlot;
import org.hkijena.acaq5.api.events.AlgorithmSlotsChangedEvent;
import org.hkijena.acaq5.api.events.ParameterChangedEvent;
import org.hkijena.acaq5.api.events.TraitConfigurationChangedEvent;
import org.hkijena.acaq5.ui.components.AddAlgorithmSlotPanel;
import org.hkijena.acaq5.ui.events.AlgorithmSelectedEvent;
import org.hkijena.acaq5.ui.extensionbuilder.traiteditor.api.ACAQTraitNode;
import org.hkijena.acaq5.utils.PointRange;
import org.hkijena.acaq5.utils.UIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.Collections;

/**
 * UI around an {@link ACAQAlgorithm} instance
 */
public abstract class ACAQAlgorithmUI extends JPanel {

    /**
     * Height assigned for one slot
     */
    public static final int SLOT_UI_HEIGHT = 50;

    /**
     * Grid width for horizontal direction
     */
    public static final int SLOT_UI_WIDTH = 25;

    private ACAQAlgorithmGraphCanvasUI graphUI;
    private ACAQAlgorithm algorithm;
    private ACAQAlgorithmGraphCanvasUI.ViewMode viewMode;
    private EventBus eventBus = new EventBus();

    private Color fillColor;
    private Color borderColor;

    private boolean selected;
    private JPopupMenu contextMenu = new JPopupMenu();

    /**
     * Creates a new UI
     *
     * @param graphUI   The graph UI that contains this UI
     * @param algorithm The algorithm
     * @param viewMode  Directionality of the canvas UI
     */
    public ACAQAlgorithmUI(ACAQAlgorithmGraphCanvasUI graphUI, ACAQAlgorithm algorithm, ACAQAlgorithmGraphCanvasUI.ViewMode viewMode) {
        this.graphUI = graphUI;
        this.algorithm = algorithm;
        this.viewMode = viewMode;
        this.algorithm.getEventBus().register(this);
        this.algorithm.getTraitConfiguration().getEventBus().register(this);
        this.fillColor = UIUtils.getFillColorFor(algorithm.getDeclaration());
        this.borderColor = UIUtils.getBorderColorFor(algorithm.getDeclaration());
    }

    public JPopupMenu getContextMenu() {
        return contextMenu;
    }

    /**
     * Initializes the context menu
     */
    protected void initializeContextMenu() {
        JMenuItem selectOnlyButton = new JMenuItem("Open settings", UIUtils.getIconFromResources("cog.png"));
        selectOnlyButton.addActionListener(e -> eventBus.post(new AlgorithmSelectedEvent(this, false)));
        contextMenu.add(selectOnlyButton);

        JMenuItem addToSelectionButton = new JMenuItem("Add to selection", UIUtils.getIconFromResources("select.png"));
        addToSelectionButton.addActionListener(e -> eventBus.post(new AlgorithmSelectedEvent(this, true)));
        contextMenu.add(addToSelectionButton);

        if (graphUI.getCopyPasteBehavior() != null) {
            contextMenu.addSeparator();

            JMenuItem cutButton = new JMenuItem("Cut", UIUtils.getIconFromResources("cut.png"));
            cutButton.addActionListener(e -> graphUI.getCopyPasteBehavior().cut(Collections.singleton(getAlgorithm())));
            contextMenu.add(cutButton);

            JMenuItem copyButton = new JMenuItem("Copy", UIUtils.getIconFromResources("copy.png"));
            copyButton.addActionListener(e -> graphUI.getCopyPasteBehavior().copy(Collections.singleton(getAlgorithm())));
            contextMenu.add(copyButton);
        }

        contextMenu.addSeparator();

        if (algorithm instanceof ACAQProjectCompartment) {
            JMenuItem deleteButton = new JMenuItem("Delete compartment", UIUtils.getIconFromResources("delete.png"));
            deleteButton.addActionListener(e -> removeCompartment());
            contextMenu.add(deleteButton);
        } else if (algorithm instanceof ACAQTraitNode) {
            JMenuItem deleteButton = new JMenuItem("Delete annotation", UIUtils.getIconFromResources("delete.png"));
            deleteButton.addActionListener(e -> removeTrait());
            contextMenu.add(deleteButton);
        } else {
            JMenuItem deleteButton = new JMenuItem("Delete algorithm", UIUtils.getIconFromResources("delete.png"));
            deleteButton.setEnabled(graphUI.getAlgorithmGraph().canUserDelete(algorithm));
            deleteButton.addActionListener(e -> removeAlgorithm());
            contextMenu.add(deleteButton);
        }

    }

    private void removeCompartment() {
        ACAQProjectCompartment compartment = (ACAQProjectCompartment) algorithm;
        if (JOptionPane.showConfirmDialog(this, "Do you really want to delete the compartment '" + compartment.getName() + "'?\n" +
                "You will lose all nodes stored in this compartment.", "Delete compartment", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            compartment.getProject().removeCompartment(compartment);
        }
    }

    private void removeTrait() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the annotation '" + algorithm.getName() + "'?", "Delete annotation",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graphUI.getAlgorithmGraph().removeNode(algorithm);
        }
    }

    private void removeAlgorithm() {
        if (JOptionPane.showConfirmDialog(this,
                "Do you really want to remove the algorithm '" + algorithm.getName() + "'?", "Delete algorithm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            graphUI.getAlgorithmGraph().removeNode(algorithm);
        }
    }

    /**
     * @return The displayed algorithm
     */
    public ACAQAlgorithm getAlgorithm() {
        return algorithm;
    }

    protected JButton createAddSlotButton(ACAQDataSlot.SlotType slotType) {
        JButton button = new JButton(UIUtils.getIconFromResources("add.png"));
        button.setPreferredSize(new Dimension(25, SLOT_UI_HEIGHT));
        UIUtils.makeFlat(button);
        button.addActionListener(e -> AddAlgorithmSlotPanel.showDialog(this, algorithm, slotType));

        return button;
    }

    /**
     * Updates the slots
     */
    public abstract void updateAlgorithmSlotUIs();

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return True if setting the location was successful
     */
    public boolean trySetLocationInGrid(int x, int y) {
        y = (int) Math.rint(y * 1.0 / ACAQAlgorithmUI.SLOT_UI_HEIGHT) * ACAQAlgorithmUI.SLOT_UI_HEIGHT;
        x = (int) Math.rint(x * 1.0 / ACAQAlgorithmUI.SLOT_UI_WIDTH) * ACAQAlgorithmUI.SLOT_UI_WIDTH;
        return trySetLocationNoGrid(x, y);
    }

    /**
     * Returns true if this component overlaps with another component
     *
     * @return True if an overlap was found
     */
    public boolean isOverlapping() {
        for (int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if (component instanceof ACAQAlgorithmUI) {
                ACAQAlgorithmUI ui = (ACAQAlgorithmUI) component;
                if (ui != this) {
                    if (ui.getBounds().intersects(getBounds())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Tries to move the node to the provided location
     * A grid is applied to the input coordinates
     *
     * @param x x coordinate
     * @param y y coordinate
     * @return True if setting the location was successful
     */
    public boolean trySetLocationNoGrid(int x, int y) {
        // Check for collisions
        Rectangle futureBounds = new Rectangle(x, y, getWidth(), getHeight());
        for (int i = 0; i < graphUI.getComponentCount(); ++i) {
            Component component = graphUI.getComponent(i);
            if (component instanceof ACAQAlgorithmUI) {
                ACAQAlgorithmUI ui = (ACAQAlgorithmUI) component;
                if (ui != this) {
                    if (ui.getBounds().intersects(futureBounds)) {
                        return false;
                    }
                }
            }
        }

        setLocation(x, y);
        return true;
    }

    /**
     * Get the Y location of the bottom part
     *
     * @return y coordinate
     */
    public int getBottomY() {
        return getY() + getHeight();
    }

    /**
     * Should be triggered when the algorithm's slots are changed.
     * Triggers slot UI updates
     *
     * @param event Generated event
     */
    @Subscribe
    public void onAlgorithmSlotsChanged(AlgorithmSlotsChangedEvent event) {
        updateAlgorithmSlotUIs();
    }

    /**
     * Should be triggered when the algorithm's slot traits are changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onTraitsChanged(TraitConfigurationChangedEvent event) {
        updateSize();
        revalidate();
        repaint();
    }

    /**
     * Should be triggered when an algorithm's name parameter is changed
     *
     * @param event The generated event
     */
    @Subscribe
    public void onAlgorithmParametersChanged(ParameterChangedEvent event) {
        if (event.getParameterHolder() == algorithm && "name".equals(event.getKey())) {
            updateSize();
            updateName();
            revalidate();
            repaint();
        }
    }

    /**
     * Called when the algorithm name was updated
     */
    protected abstract void updateName();

    /**
     * Recalculates the UI size
     */
    public abstract void updateSize();

    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        algorithm.setLocationWithin(graphUI.getCompartment(), new Point(x, y), viewMode.toString());
    }

    @Override
    public void setLocation(Point p) {
        super.setLocation(p);
        algorithm.setLocationWithin(graphUI.getCompartment(), p, viewMode.toString());
    }

    /**
     * @return The event bus
     */
    public EventBus getEventBus() {
        return eventBus;
    }

    /**
     * Returns if the selected-flag was set. This does not mean that the algorithm is actually selected in the graph UI
     *
     * @return True if this algorithm is selected.
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the selected-flag and updates the UI
     * This does not select the algorithm the graph UI
     *
     * @param selected if the algorithm is selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
        updateBorder();
    }

    protected void updateBorder() {
        if (selected) {
            setBorder(BorderFactory.createLineBorder(borderColor, 2));
        } else {
            setBorder(BorderFactory.createLineBorder(borderColor));
        }
    }

    public ACAQAlgorithmGraphCanvasUI.ViewMode getViewMode() {
        return viewMode;
    }

    /**
     * Returns the location of a slot in relative coordinates
     *
     * @param slot the slot
     * @return coordinates relative to this algorithm UI
     */
    public abstract PointRange getSlotLocation(ACAQDataSlot slot);

    public Color getFillColor() {
        return fillColor;
    }

    public Color getBorderColor() {
        return borderColor;
    }

    public ACAQAlgorithmGraphCanvasUI getGraphUI() {
        return graphUI;
    }

    public int getRightX() {
        return getX() + getWidth();
    }
}
