package org.hkijena.jipipe.ui.components.ribbon;

import org.hkijena.jipipe.ui.components.tabs.DocumentTabPane;
import org.hkijena.jipipe.ui.theme.CustomTabbedPaneUI;
import org.hkijena.jipipe.ui.theme.JIPipeUITheme;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * A simple Ribbon-style panel
 */
public class Ribbon extends JPanel {

    public static final Border DEFAULT_BORDER = BorderFactory.createEmptyBorder(2, 2, 2, 2);

    private int numRows;
    private final DocumentTabPane tabPane = new DocumentTabPane();

    private List<Task> tasks;

    public Ribbon(Task... tasks) {
        this(3, tasks);
    }

    public Ribbon(int numRows, Task... tasks) {
        this.numRows = numRows;
        this.tasks = new ArrayList<>(Arrays.asList(tasks));
        initialize();
        rebuildRibbon();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        tabPane.setTabPanelBorder(BorderFactory.createEmptyBorder(2,2,2,2));
//        tabPane.getTabbedPane().setTabPlacement(SwingConstants.TOP);
        tabPane.setEnableTabContextMenu(false);
        add(tabPane, BorderLayout.CENTER);
    }

    public void rebuildRibbon() {
        tabPane.closeAllTabs(true);
        for (Task task : tasks) {
            JPanel taskPanel = new JPanel(new GridBagLayout());
            buildTaskPanel(taskPanel, task);
            tabPane.addTab(task.label, null, taskPanel, DocumentTabPane.CloseMode.withoutCloseButton);
        }
    }

    private void buildTaskPanel(JPanel taskPanel, Task task) {
        List<Band> bands = task.bands;
        final Insets separatorInsets = new Insets(2, 4, 2, 4);
        final Insets bandLabelInsets = new Insets(12,4,4,4);
        int col = 0;
        int row = 0;
        for (Band band : bands) {
            // Add actions
            final int startCol = col;

            for (Action action : band.getActions()) {
                int available = numRows - row;
                int requested = Math.max(1, Math.min(numRows, action.getHeight()));
                if (requested > available) {
                    ++col;
                    row = 0;
                }
                taskPanel.add(action.getComponent(), new GridBagConstraints(col, row, 1, requested, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, action.insets, 0, 0));
                row += requested;
            }

            final int endCol = col;

            // Add label
            JLabel bandLabel = new JLabel(band.getLabel());
            bandLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
            bandLabel.setHorizontalAlignment(SwingConstants.CENTER);
            taskPanel.add(bandLabel, new GridBagConstraints(startCol, numRows, endCol - startCol + 1, 1, 0, 0, GridBagConstraints.SOUTH, GridBagConstraints.HORIZONTAL, bandLabelInsets, 0, 0));

            // Increment col
            ++col;

            // Add separator
            {
                taskPanel.add(new JSeparator(SwingConstants.VERTICAL), new GridBagConstraints(col, 0, 1, numRows + 1, 0, 1, GridBagConstraints.NORTHWEST, GridBagConstraints.VERTICAL, separatorInsets, 0, 0));

                // Go to next column and set row to 0
                ++col;
                row = 0;
            }
        }

        // Add horizontal glue
        taskPanel.add(new JPanel(), new GridBagConstraints(col, 0, 1,1,1,1,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL, new Insets(0,0,0,0), 0,0));
    }

    public static void main(String[] args) {

        JIPipeUITheme.ModernLight.install();

        JFrame frame = new JFrame();
        frame.setContentPane(new JPanel(new BorderLayout()));

        Ribbon panel = new Ribbon(new Task("Task 1",
                    new Band("Band 1",
                            new LargeButtonAction("Action", "", UIUtils.getIcon32FromResources("module-imagej.png"), () -> {}),
                            new SmallButtonAction("Button 1", "", UIUtils.getIconFromResources("actions/configure.png"), () -> {}),
                            new SmallButtonAction("Button 2", "", UIUtils.getIconFromResources("actions/configure.png"), () -> {}),
                            new SmallButtonAction("Button 3", "", UIUtils.getIconFromResources("actions/configure.png"), () -> {})),
                    new Band("Band 2",
                            new SmallButtonAction("Button 1", "", UIUtils.getIconFromResources("actions/configure.png"), () -> {}),
                            new SmallButtonAction("Button 2", "", UIUtils.getIconFromResources("actions/configure.png"), () -> {}),
                            new SmallButtonAction("Button 3", "", UIUtils.getIconFromResources("actions/configure.png"), () -> {}))),
                new Task("Task 2",
                        new Band("Band 3"),
                        new Band("Band 4"),
                        new Band("Band 5")));
        panel.tabPane.getTabbedPane().setUI(new CustomTabbedPaneUI());

        frame.getContentPane().add(panel, BorderLayout.NORTH);
        frame.pack();
        frame.setSize(1024,768);
        frame.setVisible(true);
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
        rebuildRibbon();
    }

    public int getNumRows() {
        return numRows;
    }

    /**
     * Sets the number of rows
     * Will not rebuild the ribbon!
     * @param numRows the number of rows
     */
    public void setNumRows(int numRows) {
        this.numRows = numRows;
    }

    /**
     * Clears the ribbon.
     * Will not rebuild the ribbon!
     */
    public void clear() {
        tasks.clear();
    }

    /**
     * Adds a new task.
     * Will not rebuild the ribbon.
     * @param label the label
     * @return the task
     */
    public Task addTask(String label) {
        Task task = new Task(label);
        tasks.add(task);
        return task;
    }

    public Task getOrCreateTask(String label) {
        Optional<Task> optional = tasks.stream().filter(t -> Objects.equals(t.label, label)).findFirst();
        return optional.orElseGet(() -> addTask(label));
    }

    /**
     * The highest-order organization of a ribbon.
     * Corresponds to the tabs in the tab bar
     */
    public static class Task {

        private String label;
        private List<Band> bands;

        public Task(String label, Band... bands) {
            this.label = label;
            this.bands = new ArrayList<>(Arrays.asList(bands));
        }

        public Task(String label, List<Band> bands) {
            this.label = label;
            this.bands = bands;
        }

        public List<Band> getBands() {
            return bands;
        }

        public void setBands(List<Band> bands) {
            this.bands = bands;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Task add(Band band) {
            bands.add(band);
            return this;
        }

        public Task addAll(Band... bands) {
            Collections.addAll(this.bands, bands);
            return this;
        }

        public Band addBand(String label) {
            Band band = new Band(label);
            add(band);
            return band;
        }

        public Band getOrCreateBand(String label) {
            Optional<Band> optional = bands.stream().filter(t -> Objects.equals(t.label, label)).findFirst();
            return optional.orElseGet(() -> addBand(label));
        }
    }

    /**
     * Secondary organization within one task
     * Creates a label at the bottom of the current tak tab
     * Separated from other bands via a separator line
     */
    public static class Band {

        private String label;
        private List<Action> actions;

        public Band(String label, Action... actions) {
            this.label = label;
            this.actions = new ArrayList<>(Arrays.asList(actions));
        }

        public Band(String label, List<Action> actions) {
            this.label = label;
            this.actions = actions;
        }

        public List<Action> getActions() {
            return actions;
        }

        public void setActions(List<Action> actions) {
            this.actions = actions;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public Band add(Action action) {
            actions.add(action);
            return this;
        }

        public Band addAll(Action... actions) {
            Collections.addAll(this.actions, actions);
            return this;
        }
    }

    /**
     * An action within the ribbon
     */
    public static class Action {
        private Component component;
        private int height;

        private Insets insets;

        public Action(Component component, int height, Insets insets) {
            this.component = component;
            this.height = height;
            this.insets = insets;
        }

        public Component getComponent() {
            return component;
        }

        public void setComponent(Component component) {
            this.component = component;
        }

        public int getHeight() {
            return height;
        }

        public void setHeight(int height) {
            this.height = height;
        }

        public Insets getInsets() {
            return insets;
        }

        public void setInsets(Insets insets) {
            this.insets = insets;
        }
    }
}