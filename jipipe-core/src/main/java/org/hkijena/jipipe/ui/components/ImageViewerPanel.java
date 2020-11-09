/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under BSD 2-Clause.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.ui.components;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.ImageWindow;
import ij.gui.StackWindow;
import ij.measure.Calibration;
import ij.util.Tools;
import org.hkijena.jipipe.utils.StringUtils;
import org.hkijena.jipipe.utils.UIUtils;

import javax.swing.*;
import javax.xml.transform.Source;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;

public class ImageViewerPanel extends JPanel {
    private ImagePlus image;
    private Canvas canvas = new Canvas();
    private JLabel stackSliderLabel = new JLabel("Slice (Z)");
    private JLabel channelSliderLabel = new JLabel("Channel (C)");
    private JLabel frameSliderLabel = new JLabel("Frame (T)");
    private JScrollBar stackSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private JScrollBar channelSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private JScrollBar frameSlider = new JScrollBar(Adjustable.HORIZONTAL, 1, 1, 1, 100);
    private FormPanel bottomPanel;
    private final JButton zoomStatusButton = new JButton();
    private long lastTimeZoomed;
    private JLabel imageInfoLabel = new JLabel();
    private JScrollPane scrollPane;

    public ImageViewerPanel() {
        initialize();
        updateZoomStatus();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        scrollPane = new JScrollPane(canvas);

        initializeToolbar();
        canvas.addMouseWheelListener(e -> {
            if (e.isControlDown()) {
                final double zoom = canvas.getZoom();
                // We move the graph cursor to the mouse
                // The zoom will "focus" on this cursor and modify the scroll bars accordingly
                int x = e.getX();
                int y = e.getY();
                double beforeZoomX = x * zoom;
                double beforeZoomY = y * zoom;

                if (e.getWheelRotation() < 0) {
                    increaseZoom();
                } else {
                    decreaseZoom();
                }

                double afterZoomX = x * zoom;
                double afterZoomY = y * zoom;

                double dX = afterZoomX - beforeZoomX;
                double dY = afterZoomY - beforeZoomY;

//                if (scrollPane != null) {
//                    scrollPane.getHorizontalScrollBar().setValue(scrollPane.getHorizontalScrollBar().getValue() + (int) dX);
//                    scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getValue() + (int) dY);
//                }

            } else {
                getParent().dispatchEvent(e);
            }
        });

        add(scrollPane, BorderLayout.CENTER);

        bottomPanel = new FormPanel(null, FormPanel.NONE);
        bottomPanel.addToForm(channelSlider, channelSliderLabel, null);
        bottomPanel.addToForm(stackSlider, stackSliderLabel, null);
        bottomPanel.addToForm(frameSlider, frameSliderLabel, null);
        add(bottomPanel, BorderLayout.SOUTH);

        // Register slider events
        stackSlider.addAdjustmentListener(e -> refreshSlice());
        channelSlider.addAdjustmentListener(e -> refreshSlice());
        frameSlider.addAdjustmentListener(e -> refreshSlice());
    }

    private void initializeToolbar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton openInImageJButton = new JButton("Open in ImageJ", UIUtils.getIconFromResources("apps/imagej.png"));
        openInImageJButton.addActionListener(e -> openInImageJ());
        toolBar.add(openInImageJButton);
        toolBar.add(Box.createHorizontalStrut(8));
        toolBar.add(imageInfoLabel);

        toolBar.add(Box.createHorizontalGlue());
        JButton zoomOutButton = new JButton(UIUtils.getIconFromResources("actions/zoom-out.png"));
        UIUtils.makeFlat25x25(zoomOutButton);
        zoomOutButton.addActionListener(e -> decreaseZoom());
        toolBar.add(zoomOutButton);

        UIUtils.makeBorderlessWithoutMargin(zoomStatusButton);
        JPopupMenu zoomMenu = UIUtils.addPopupMenuToComponent(zoomStatusButton);
        for (double zoom = 0.5; zoom <= 2; zoom += 0.25) {
            JMenuItem changeZoomItem = new JMenuItem((int) (zoom * 100) + "%", UIUtils.getIconFromResources("actions/zoom.png"));
            double finalZoom = zoom;
            changeZoomItem.addActionListener(e -> {
                canvas.setZoom(finalZoom);
                updateZoomStatus();
            });
            zoomMenu.add(changeZoomItem);
        }
        zoomMenu.addSeparator();
        JMenuItem fitImageItem = new JMenuItem("Fit image to window");
        fitImageItem.addActionListener(e -> {
            if(image != null) {
                double zoomx = canvas.getWidth() / (1.0 * image.getWidth());
                double zoomy = canvas.getHeight() / (1.0 * image.getHeight());
                canvas.setZoom(Math.min(zoomx, zoomy));
                updateZoomStatus();
            }
        });
        zoomMenu.add(fitImageItem);
        JMenuItem changeZoomToItem = new JMenuItem("Set zoom value ...");
        changeZoomToItem.addActionListener(e -> {
            String zoomInput = JOptionPane.showInputDialog(this, "Please enter a new zoom value (in %)", (int) (canvas.getZoom() * 100) + "%");
            if (!StringUtils.isNullOrEmpty(zoomInput)) {
                zoomInput = zoomInput.replace("%", "");
                try {
                    int percentage = Integer.parseInt(zoomInput);
                    canvas.setZoom(percentage / 100.0);
                    updateZoomStatus();
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
            }
        });
        zoomMenu.add(changeZoomToItem);
        toolBar.add(zoomStatusButton);

        JButton zoomInButton = new JButton(UIUtils.getIconFromResources("actions/zoom-in.png"));
        UIUtils.makeFlat25x25(zoomInButton);
        zoomInButton.addActionListener(e -> increaseZoom());
        toolBar.add(zoomInButton);

        add(toolBar, BorderLayout.NORTH);
    }

    private void updateZoomStatus() {
        zoomStatusButton.setText((int)Math.round(canvas.getZoom() * 100) + "%");
    }

    private void increaseZoom() {
        long diff = System.currentTimeMillis() - lastTimeZoomed;
        double x = Math.min(250, diff);
        double fac = 0.2 - 0.15 * (x / 250);
        lastTimeZoomed = System.currentTimeMillis();
        canvas.setZoom(canvas.getZoom() + fac);
        updateZoomStatus();
    }

    private void decreaseZoom() {
        long diff = System.currentTimeMillis() - lastTimeZoomed;
        double x = Math.min(250, diff);
        double fac = 0.2 - 0.15 * (x / 250);
        lastTimeZoomed = System.currentTimeMillis();
        canvas.setZoom(canvas.getZoom() - fac);
        updateZoomStatus();
    }

    private void openInImageJ() {
        if(image != null) {
            String title = image.getTitle();
            ImagePlus duplicate = image.duplicate();
            duplicate.setTitle(title);
            duplicate.show();
        }
    }

    private void refreshSliders() {
        if(image != null) {
            bottomPanel.setVisible(true);
            stackSlider.setVisible(image.getNSlices() > 1);
            stackSliderLabel.setVisible(image.getNSlices() > 1);
            channelSlider.setVisible(image.getNChannels() > 1);
            channelSliderLabel.setVisible(image.getNChannels() > 1);
            frameSlider.setVisible(image.getNFrames() > 1);
            frameSliderLabel.setVisible(image.getNFrames() > 1);
            stackSlider.setMinimum(1);
            stackSlider.setMaximum(image.getNSlices() + 1);
            channelSlider.setMinimum(1);
            channelSlider.setMinimum(image.getNChannels() + 1);
            frameSlider.setMinimum(1);
            frameSlider.setMaximum(image.getNFrames() + 1);
        }
        else {
            bottomPanel.setVisible(false);
        }
    }

    public ImagePlus getImage() {
        return image;
    }

    public void setImage(ImagePlus image) {
        this.image = image;
        refreshSliders();
        refreshSlice();
        refreshImageInfo();
        SwingUtilities.invokeLater(() -> canvas.centerImage());
    }

    public void refreshImageInfo() {
        String s="";
        if (image==null) {
            imageInfoLabel.setText("");
            return;
        }
        int type = image.getType();
        Calibration cal = image.getCalibration();
        if (cal.scaled()) {
            boolean unitsMatch = cal.getXUnit().equals(cal.getYUnit());
            double cwidth = image.getWidth()*cal.pixelWidth;
            double cheight = image.getHeight()*cal.pixelHeight;
            int digits = Tools.getDecimalPlaces(cwidth, cheight);
            if (digits>2) digits=2;
            if (unitsMatch) {
                s += IJ.d2s(cwidth,digits) + "x" + IJ.d2s(cheight,digits)
                        + " " + cal.getUnits() + " (" + image.getWidth() + "x" + image.getHeight() + "); ";
            } else {
                s += (cwidth) + " " + cal.getXUnit() + " x "
                        + (cheight) + " " + cal.getYUnit()
                        + " (" + image.getWidth() + "x" + image.getHeight() + "); ";
            }
        } else
            s += image.getWidth() + "x" + image.getHeight() + " pixels; ";
        switch (type) {
            case ImagePlus.GRAY8:
            case ImagePlus.COLOR_256:
                s += "8-bit";
                break;
            case ImagePlus.GRAY16:
                s += "16-bit";
                break;
            case ImagePlus.GRAY32:
                s += "32-bit";
                break;
            case ImagePlus.COLOR_RGB:
                s += "RGB";
                break;
        }
        if (image.isInvertedLut())
            s += " (inverting LUT)";
        s += "; " + ImageWindow.getImageSize(image);
        imageInfoLabel.setText(s);
    }

    public void refreshSlice() {
        if(image != null) {
            int stack = Math.max(1, Math.min(image.getNSlices(), stackSlider.getValue()));
            int frame = Math.max(1, Math.min(image.getNFrames(), frameSlider.getValue()));
            int channel = Math.max(1, Math.min(image.getNChannels(), channelSlider.getValue()));
            stackSliderLabel.setText(String.format("Slice (Z) %d/%d", stack, image.getNSlices()));
            frameSliderLabel.setText(String.format("Frame (T) %d/%d", frame, image.getNFrames()));
            channelSliderLabel.setText(String.format("Channel (C) %d/%d", channel, image.getNChannels()));
            image.setPosition(channel, stack, frame);
            canvas.setImage(image.getBufferedImage());
        }
    }

    public static void main(String[] args) {
        ImagePlus image = IJ.openImage("/data/Mitochondria/data/Mic13 SNAP Deconv.lif - WT_Hela_Mic13_SNAP_Series011_10_cmle_converted.tif");
        JFrame frame = new JFrame();
        ImageViewerPanel panel = new ImageViewerPanel();
        panel.setImage(image);
        frame.setContentPane(panel);
        frame.pack();
        frame.setSize(1280,1024);
        frame.setVisible(true);
    }

    public static class Canvas extends JPanel implements MouseListener, MouseMotionListener {
        private BufferedImage image;
        private double zoom = 1.0;
        private int contentX = 0;
        private int contentY = 0;
        private Point currentDragOffset = null;

        public Canvas() {
            addMouseListener(this);
            addMouseMotionListener(this);
        }

        public BufferedImage getImage() {
            return image;
        }

        public void setImage(BufferedImage image) {
            this.image = image;
            revalidate();
            repaint();
        }

        public void centerImage() {
            if(image.getWidth() < getWidth()) {
                contentX = getWidth() / 2 - image.getWidth() / 2;
            }
            else {
                contentX = 0;
            }
            if(image.getHeight() < getHeight()) {
                contentY = getHeight() / 2 - image.getHeight() / 2;
            }
            else {
                contentY = 0;
            }
            revalidate();
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            int width = 0;
            int height = 0;
            if(image != null) {
                width = (int)(image.getWidth() * zoom) + contentX;
                height = (int)(image.getHeight() * zoom) + contentY;
            }
            return new Dimension(width, height);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D graphics2D = (Graphics2D) g;

            if(image != null) {
                AffineTransform transform = new AffineTransform();
                transform.scale(zoom, zoom);
                BufferedImageOp op = new AffineTransformOp(transform, zoom < 1 ? AffineTransformOp.TYPE_BILINEAR : AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
                graphics2D.drawImage(image, op,contentX, contentY);
            }
        }

        public double getZoom() {
            return zoom;
        }

        public void setZoom(double zoom) {
            zoom = Math.max(zoom, 10e-4);
            double oldZoom = this.zoom;
            {
                Point mousePosition = getMousePosition();
                int correctedMouseX = (int)((mousePosition.x / oldZoom) * zoom);
                int correctedMouseY = (int)((mousePosition.y / oldZoom) * zoom);
                int dx = correctedMouseX - mousePosition.x;
                int dy = correctedMouseY - mousePosition.y;
                contentX -= dx;
                contentY -= dy;
            }
            this.zoom = zoom;
            revalidate();
            repaint();
        }

        @Override
        public void mouseClicked(MouseEvent e) {

        }

        @Override
        public void mousePressed(MouseEvent e) {
           if(SwingUtilities.isLeftMouseButton(e)) {
               currentDragOffset = new Point(e.getPoint().x - contentX, e.getPoint().y - contentY);
               setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
           }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            currentDragOffset = null;
            setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }

        @Override
        public void mouseEntered(MouseEvent e) {

        }

        @Override
        public void mouseExited(MouseEvent e) {
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if(currentDragOffset != null) {
                Point newLocation = e.getPoint();
                int withOffsetX = Math.max(0, newLocation.x - currentDragOffset.x);
                int withOffsetY = Math.max(0, newLocation.y - currentDragOffset.y);
                this.contentX = withOffsetX;
                this.contentY = withOffsetY;
                revalidate();
                repaint();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {

        }
    }
}
