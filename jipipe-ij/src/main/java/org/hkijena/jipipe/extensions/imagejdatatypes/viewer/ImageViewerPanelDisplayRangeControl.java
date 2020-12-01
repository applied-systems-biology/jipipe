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

package org.hkijena.jipipe.extensions.imagejdatatypes.viewer;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.DefaultMultiThumbModel;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.ThumbListener;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.MouseEvent;

public class ImageViewerPanelDisplayRangeControl extends JPanel implements ThumbListener {
    private final ImageViewerPanel imageViewerPanel;
    private JXMultiThumbSlider<DisplayRangeStop> slider;
    private TrackRenderer trackRenderer;
    private boolean isUpdating = false;
    private double customMin;
    private double customMax;

    public ImageViewerPanelDisplayRangeControl(ImageViewerPanel imageViewerPanel) {
        this.imageViewerPanel = imageViewerPanel;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        trackRenderer = new TrackRenderer(this);
        initializeToolbar();
        slider = new JXMultiThumbSlider<>();
        slider.setPreferredSize(new Dimension(100, 64));
        slider.setMinimumSize(new Dimension(100, 64));
        slider.setOpaque(true);
        slider.setTrackRenderer(trackRenderer);
        slider.setThumbRenderer(new ThumbRenderer());
        DefaultMultiThumbModel<DisplayRangeStop> model = new DefaultMultiThumbModel<>();
        slider.setModel(model);
        model.addThumb(0, DisplayRangeStop.Start);
        model.addThumb(1, DisplayRangeStop.End);
        add(slider, BorderLayout.CENTER);

        slider.addMultiThumbListener(this);
    }

    private void initializeToolbar() {
        JPanel toolbar = new JPanel();
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));
        toolbar.add(Box.createHorizontalGlue());
        JToggleButton logarithmicHistogramToggle = new JToggleButton(UIUtils.getIconFromResources("actions/logarithm.png"));
        logarithmicHistogramToggle.setSelected(trackRenderer.isLogarithmic());
        logarithmicHistogramToggle.addActionListener(e -> {
            trackRenderer.setLogarithmic(logarithmicHistogramToggle.isSelected());
            repaint();
        });
        toolbar.add(logarithmicHistogramToggle);
        add(toolbar, BorderLayout.NORTH);
    }

    public void updateSliders() {
        isUpdating = true;
        if (imageViewerPanel.getImage() != null) {
            double displayRangeMin = imageViewerPanel.getImage().getDisplayRangeMin();
            double displayRangeMax = imageViewerPanel.getImage().getDisplayRangeMax();
            ImageStatistics statistics = imageViewerPanel.getStatistics();
            double min = statistics.min;
            double max = statistics.max;
            double positionMin = Math.min(1, Math.max(0, displayRangeMin - min) / (max - min));
            double positionMax = Math.min(1, Math.max(0, displayRangeMax - min) / (max - min));
            slider.getModel().getThumbAt(0).setPosition((float) positionMin);
            slider.getModel().getThumbAt(1).setPosition((float) positionMax);
        }
        isUpdating = false;
        slider.repaint();
    }

    public void applyCalibration(ImagePlus foreign) {
        ImageJUtils.calibrate(foreign, imageViewerPanel.getSelectedCalibration(), customMin, customMax);
    }

    public void applyCalibration(boolean upload) {
        if (imageViewerPanel.getImage() != null) {
            ImageJUtils.calibrate(imageViewerPanel.getImage(), imageViewerPanel.getSelectedCalibration(), customMin, customMax);
            updateSliders();
            if (upload)
                imageViewerPanel.uploadSliceToCanvas();
        }
    }

    @Override
    public void thumbMoved(int thumb, float pos) {
        applyCustomCalibration();
    }

    private void applyCustomCalibration() {
        if (!isUpdating) {
            ImageStatistics statistics = imageViewerPanel.getStatistics();
            double min = statistics.min;
            double max = statistics.max;
            double diff = max - min;
            float posMin = Float.POSITIVE_INFINITY;
            float posMax = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < 2; i++) {
                Thumb<DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
                posMin = Math.min(posMin, thumb.getPosition());
                posMax = Math.max(posMax, thumb.getPosition());
            }
            customMin = min + diff * posMin;
            customMax = min + diff * posMax;
            imageViewerPanel.disableAutoCalibration();
            imageViewerPanel.setSelectedCalibration(ImageJCalibrationMode.Custom);
        }
    }

    @Override
    public void thumbSelected(int thumb) {

    }

    @Override
    public void mousePressed(MouseEvent evt) {

    }

    public ImageViewerPanel getImageViewerPanel() {
        return imageViewerPanel;
    }

    public enum DisplayRangeStop {
        Start,
        End
    }

    public static class ThumbRenderer extends JComponent implements org.jdesktop.swingx.multislider.ThumbRenderer {

        public static final Polygon SHAPE = new Polygon(new int[]{5, 0, 10}, new int[]{10, 0, 0}, 3);
        public static int SIZE = 5;
        private JXMultiThumbSlider<DisplayRangeStop> slider;
        private int index;

        public ThumbRenderer() {
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            paintComponent(g);
        }

        @Override
        protected void paintComponent(Graphics gfx) {
            Graphics2D g = (Graphics2D) gfx;
            g.setColor(UIManager.getColor("Label.foreground"));
            g.fill(SHAPE);
        }

        @Override
        public JComponent getThumbRendererComponent(JXMultiThumbSlider slider, int index, boolean selected) {
            this.slider = slider;
            this.index = index;
            return this;
        }
    }

    public static class TrackRenderer extends JComponent implements org.jdesktop.swingx.multislider.TrackRenderer {

        public static final Color COLOR_SELECTED = ModernMetalTheme.PRIMARY5;
        public static final Color COLOR_UNSELECTED = UIManager.getColor("Button.borderColor");
        private final ImageViewerPanelDisplayRangeControl imageViewerPanelDisplayRangeControl;
        private JXMultiThumbSlider<DisplayRangeStop> slider;
        private boolean logarithmic = true;

        public TrackRenderer(ImageViewerPanelDisplayRangeControl imageViewerPanelDisplayRangeControl) {
            this.imageViewerPanelDisplayRangeControl = imageViewerPanelDisplayRangeControl;
        }

        @Override
        public void paint(Graphics g) {
            super.paint(g);
            paintComponent(g);
        }

        @Override
        protected void paintComponent(Graphics gfx) {
            Graphics2D g = (Graphics2D) gfx;
            int w = slider.getWidth() - 2 * ThumbRenderer.SIZE;
            int h = slider.getHeight() - 16;
            g.setColor(getBackground());
            g.fillRect(0, 0, slider.getWidth(), slider.getHeight());
            g.setColor(UIManager.getColor("Button.borderColor"));
            g.drawLine(0, h, w + 2 * ThumbRenderer.SIZE, h);
            g.setColor(COLOR_SELECTED);
            ImageProcessor slice = imageViewerPanelDisplayRangeControl.getImageViewerPanel().getSlice();
            ImageStatistics statistics = imageViewerPanelDisplayRangeControl.getImageViewerPanel().getStatistics();
            if (statistics != null && slice != null) {
                long[] histogram = statistics.getHistogram();
                if (histogram.length > 0) {
                    int selectedXMin = Integer.MAX_VALUE;
                    int selectedXMax = Integer.MIN_VALUE;
                    for (int i = 0; i < 2; i++) {
                        Thumb<DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
                        float position = Math.max(0, Math.min(thumb.getPosition(), 1));
                        int x = ThumbRenderer.SIZE - 1 + (int) (w * position);
                        selectedXMin = Math.min(x, selectedXMin);
                        selectedXMax = Math.max(x, selectedXMax);
                    }

                    double binSize = 1.0 * w / histogram.length;
                    int binMaxHeight = h - 8;
                    long max = 1;
                    for (long l : histogram) {
                        max = Math.max(l, max);
                    }
                    double lmax = Math.log(max);
                    for (int i = 0; i < histogram.length; i++) {
                        int binHeight;
                        if (logarithmic)
                            binHeight = (int) Math.round((Math.log(histogram[i]) / lmax) * binMaxHeight);
                        else
                            binHeight = (int) (Math.round(1.0 * histogram[i] / max) * binMaxHeight);
                        int x = (int) (i * binSize) + ThumbRenderer.SIZE;
                        if (x >= selectedXMin && x <= selectedXMax)
                            g.setColor(COLOR_SELECTED);
                        else
                            g.setColor(COLOR_UNSELECTED);
                        g.fillRect(x, binMaxHeight - binHeight + 8, (int) binSize + 1, binHeight);
                    }
                }
            }
            g.setColor(UIManager.getColor("Label.foreground"));
            for (int i = 0; i < 2; i++) {
                Thumb<DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
                float position = Math.max(0, Math.min(thumb.getPosition(), 1));
                int x = ThumbRenderer.SIZE - 1 + (int) (w * position);
                g.fillRect(x, 4, 2, h + 4);
            }
        }

        @Override
        public JComponent getRendererComponent(JXMultiThumbSlider slider) {
            this.slider = slider;
            return this;
        }

        public boolean isLogarithmic() {
            return logarithmic;
        }

        public void setLogarithmic(boolean logarithmic) {
            this.logarithmic = logarithmic;
        }
    }
}
