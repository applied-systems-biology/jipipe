package org.hkijena.jipipe.utils.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class FloatingDockPanel extends JPanel {

    private final JToolBar leftToolBar = new JToolBar();
    private final JToolBar rightToolBar = new JToolBar();
    private final JLayeredPane layeredPane = new JLayeredPane();
    private final JPanel layeredPaneBackground = new JPanel(new BorderLayout());
    private final JPanel leftFloatingPanel = new JPanel(new BorderLayout());
    private final JPanel rightFloatingPanel = new JPanel(new BorderLayout());
    private final JPanel leftResizerPanel = new JPanel();
    private final JPanel rightResizerPanel = new JPanel();
    private int floatingMargin = 8;
    private int leftFloatingSize = 350;
    private int rightFloatingSize = 350;
    private int minimumFloatingSize = 50;
    private JComponent leftFloatingPanelContent;
    private JComponent rightFloatingPanelContent;

    public FloatingDockPanel() {
        super(new BorderLayout());
        initialize();
        updateContent();
    }

    private void initialize() {
        leftToolBar.setFloatable(false);
        rightToolBar.setFloatable(false);
        add(leftToolBar, BorderLayout.WEST);
        add(rightToolBar, BorderLayout.EAST);

//        layeredPane.setLayout(new OverlayLayout(layeredPane));
        add(layeredPane, BorderLayout.CENTER);

        layeredPane.add(layeredPaneBackground, JLayeredPane.DEFAULT_LAYER);
        layeredPane.add(leftFloatingPanel, JLayeredPane.PALETTE_LAYER);
        layeredPane.add(rightFloatingPanel, JLayeredPane.PALETTE_LAYER);

        initializeLeftFloatingPanel();
        initializeRightFloatingPanel();

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateSizes();
            }
        });
    }

    private void initializeRightFloatingPanel() {
        rightFloatingPanel.setOpaque(false);
        rightFloatingPanel.add(rightResizerPanel, BorderLayout.WEST);
        rightResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        rightResizerPanel.setPreferredSize(new Dimension(8, 64));
        rightResizerPanel.setMinimumSize(new Dimension(8, 32));
        rightResizerPanel.setBackground(Color.RED);

//        rightResizerPanel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseReleased(MouseEvent e) {
//                updateSizes();
//            }
//        });
        rightResizerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point convertedPoint = SwingUtilities.convertPoint(rightResizerPanel, e.getPoint(), FloatingDockPanel.this);
                rightFloatingSize = Math.max(minimumFloatingSize, layeredPane.getWidth() - convertedPoint.x);
                updateSizes();
            }


        });
    }

    private void initializeLeftFloatingPanel() {
        leftFloatingPanel.setOpaque(false);
        leftFloatingPanel.add(leftResizerPanel, BorderLayout.EAST);
        leftResizerPanel.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        leftResizerPanel.setPreferredSize(new Dimension(8, 64));
        leftResizerPanel.setMinimumSize(new Dimension(8, 32));
        leftResizerPanel.setBackground(Color.RED);

//        leftResizerPanel.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseReleased(MouseEvent e) {
//                updateSizes();
//            }
//        });
        leftResizerPanel.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseDragged(MouseEvent e) {
                Point convertedPoint = SwingUtilities.convertPoint(leftResizerPanel, e.getPoint(), layeredPane);
                leftFloatingSize = Math.max(minimumFloatingSize, convertedPoint.x);
                updateSizes();
            }
        });
    }

    public void setBackgroundComponent(JComponent component) {
        layeredPaneBackground.removeAll();
        layeredPaneBackground.add(component, BorderLayout.CENTER);
        revalidate();
        repaint();
        updateSizes();
    }

    private void updateSizes() {

        layeredPaneBackground.setBounds(leftToolBar.getWidth(), 0, getWidth() - leftToolBar.getWidth() - rightToolBar.getWidth() -4, getHeight());

        int availableWidth = leftToolBar.getWidth() + layeredPaneBackground.getWidth();
        Dimension leftSize = new Dimension(leftFloatingSize, getHeight() - 2 * floatingMargin);
        Dimension rightSize = new Dimension(rightFloatingSize, getHeight() - 2 * floatingMargin);


        leftFloatingPanel.setPreferredSize(leftSize);
        rightFloatingPanel.setPreferredSize(rightSize);
        leftFloatingPanel.setMaximumSize(leftSize);
        rightFloatingPanel.setMaximumSize(rightSize);


//        leftFloatingPanel.revalidate();
//        rightFloatingPanel.revalidate();

        leftFloatingPanel.setBounds(floatingMargin, floatingMargin, leftSize.width, leftSize.height);
        rightFloatingPanel.setBounds(availableWidth - floatingMargin - rightFloatingSize - 2, floatingMargin, rightSize.width, rightSize.height);

//        System.out.println(leftResizerPanel.getBounds());
        revalidate();
        repaint();
    }

    private void updateContent() {
        if(leftFloatingPanelContent != null) {
            leftFloatingPanel.remove(leftFloatingPanelContent);
        }
        if(rightFloatingPanelContent != null) {
            rightFloatingPanel.remove(rightFloatingPanelContent);
        }
        leftFloatingPanelContent = new JPanel();
        rightFloatingPanelContent = new JPanel();


        leftFloatingPanelContent.setOpaque(false);
        rightFloatingPanelContent.setOpaque(false);

//        leftFloatingPanelContent.setBackground(Color.YELLOW);

        leftFloatingPanel.add(leftFloatingPanelContent, BorderLayout.CENTER);
        rightFloatingPanel.add(rightFloatingPanelContent, BorderLayout.CENTER);
        leftFloatingPanel.revalidate();
        leftFloatingPanel.repaint();
        rightFloatingPanel.revalidate();
        rightFloatingPanel.repaint();
    }

    public enum PanelLocation {
        TopLeft,
        BottomLeft,
        TopRight,
        BottomRight
    }
}
