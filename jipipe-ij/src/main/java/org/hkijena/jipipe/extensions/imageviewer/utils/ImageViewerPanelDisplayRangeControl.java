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

package org.hkijena.jipipe.extensions.imageviewer.utils;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.imageviewer.plugins2d.CalibrationPlugin2D;
import org.hkijena.jipipe.ui.theme.ModernMetalTheme;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;
import org.hkijena.jipipe.utils.UIUtils;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.DefaultMultiThumbModel;
import org.jdesktop.swingx.multislider.Thumb;
import org.jdesktop.swingx.multislider.ThumbListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.util.Optional;

public class ImageViewerPanelDisplayRangeControl extends JPanel implements ThumbListener {

    public static final DecimalFormat DECIMAL_FORMAT_FLOAT = new DecimalFormat("0.###");
    public static final DecimalFormat DECIMAL_FORMAT_INT = new DecimalFormat("0");

    private final CalibrationPlugin2D calibrationPlugin;
    private JXMultiThumbSlider<DisplayRangeStop> slider;
    private TrackRenderer trackRenderer;
    private boolean isUpdating = false;
    private double customMin;
    private double customMax;
    private double minSelectableValue;
    private double maxSelectableValue;

    private WeakReference<ImagePlus> lastSelectableValueCalculationBasis;

    public ImageViewerPanelDisplayRangeControl(CalibrationPlugin2D calibrationPlugin) {
        this.calibrationPlugin = calibrationPlugin;
        initialize();
    }

    private void initialize() {
        setLayout(new BorderLayout());
        trackRenderer = new TrackRenderer(this);
        initializeToolbar();
        slider = new JXMultiThumbSlider<>();
        slider.setPreferredSize(new Dimension(100, 72));
        slider.setMinimumSize(new Dimension(100, 72));
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
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        toolbar.setLayout(new BoxLayout(toolbar, BoxLayout.X_AXIS));

        JButton setMinButton = new JButton("set min");
        setMinButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        setMinButton.addActionListener(e -> {
            if (calibrationPlugin.getCurrentImage().getImage() != null) {
                double value = (minSelectableValue + slider.getModel().getThumbAt(0).getPosition() * (maxSelectableValue - minSelectableValue));
                Optional<Double> newValue = UIUtils.getDoubleByDialog(getCalibrationPlugin().getViewerPanel(),
                        "Set min display value",
                        "Please enter the new value:",
                        value,
                        minSelectableValue,
                        maxSelectableValue);
                if (newValue.isPresent()) {
                    float position = (float) ((newValue.get() - minSelectableValue) / (maxSelectableValue - minSelectableValue));
                    slider.getModel().getThumbAt(0).setPosition(Math.max(0, Math.min(position, 1)));
                    applyCustomCalibration();
                }
            }
        });
        toolbar.add(Box.createHorizontalStrut(2));
        toolbar.add(setMinButton);

        JButton setMaxButton = new JButton("set max");
        setMaxButton.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        setMaxButton.addActionListener(e -> {
            if (calibrationPlugin.getCurrentImage().getImage() != null) {
                double value = (minSelectableValue + slider.getModel().getThumbAt(1).getPosition() * (maxSelectableValue - minSelectableValue));
                Optional<Double> newValue = UIUtils.getDoubleByDialog(getCalibrationPlugin().getViewerPanel(),
                        "Set max display value",
                        "Please enter the new value:",
                        value,
                        minSelectableValue,
                        maxSelectableValue);
                if (newValue.isPresent()) {
                    float position = (float) ((newValue.get() - minSelectableValue) / (maxSelectableValue - minSelectableValue));
                    slider.getModel().getThumbAt(1).setPosition(Math.max(0, Math.min(position, 1)));
                    applyCustomCalibration();
                }
            }
        });
        toolbar.add(Box.createHorizontalStrut(2));
        toolbar.add(setMaxButton);

        toolbar.add(Box.createHorizontalGlue());

        JToggleButton logarithmicHistogramToggle = new JToggleButton("log");
        logarithmicHistogramToggle.setBorder(BorderFactory.createLineBorder(Color.GRAY, 1));
        logarithmicHistogramToggle.setSelected(trackRenderer.isLogarithmic());
        logarithmicHistogramToggle.addActionListener(e -> {
            trackRenderer.setLogarithmic(logarithmicHistogramToggle.isSelected());
            repaint();
        });
        toolbar.add(logarithmicHistogramToggle);

        add(toolbar, BorderLayout.NORTH);
    }

    public void updateFromCurrentSlice(boolean clearCustom) {
        ImageJCalibrationMode selectedCalibration = getCalibrationPlugin().getSelectedCalibration();
        if (clearCustom || selectedCalibration != ImageJCalibrationMode.Custom) {
            isUpdating = true;
            ImagePlus currentImage = getCalibrationPlugin().getCurrentImage();
            ImageProcessor currentSlice = getCalibrationPlugin().getCurrentSlice();
            if (currentImage != null && currentSlice != null) {
                if(lastSelectableValueCalculationBasis == null || lastSelectableValueCalculationBasis.get() != currentImage) {
                    double min;
                    double max;
                    if (currentImage.getBitDepth() == 32) {
                        // We need to find the min and max
                        if (currentImage.getStackSize() == 1) {
                            ImageStatistics statistics = currentImage.getProcessor().getStats();
                            if (statistics == null)
                                return;
                            min = statistics.min;
                            max = statistics.max;
                        } else {
                            // Initial value
                            {
                                ImageProcessor processor = currentImage.getStack().getProcessor(1);
                                ImageStatistics statistics = processor.getStats();
                                if (statistics == null)
                                    return;
                                min = statistics.min;
                                max = statistics.max;
                            }
                            for (int i = 2; i <= currentImage.getStackSize(); i++) {
                                ImageProcessor processor = currentImage.getStack().getProcessor(i);
                                ImageStatistics statistics = processor.getStats();
                                if (statistics == null)
                                    continue;
                                min = Math.min(statistics.min, min);
                                max = Math.max(statistics.max, max);
                            }
                        }
                    } else {
                        min = currentSlice.minValue();
                        max = currentSlice.maxValue();
                    }
                    minSelectableValue = min;
                    maxSelectableValue = max;
                    lastSelectableValueCalculationBasis = new WeakReference<>(currentImage);
                }
                if(selectedCalibration != ImageJCalibrationMode.Custom) {
                    double[] calibration = ImageJUtils.calculateCalibration(currentSlice,
                            selectedCalibration,
                            minSelectableValue,
                            maxSelectableValue,
                            getCalibrationPlugin().getViewerPanel2D().getCurrentSliceStats());
                    customMin = calibration[0];
                    customMax = calibration[1];
                }
                double displayRangeMin = customMin;
                double displayRangeMax = customMax;
                double positionMin = Math.min(1, Math.max(0, displayRangeMin - minSelectableValue) / (maxSelectableValue - minSelectableValue));
                double positionMax = Math.min(1, Math.max(0, displayRangeMax - minSelectableValue) / (maxSelectableValue - minSelectableValue));
                slider.getModel().getThumbAt(0).setPosition((float) positionMin);
                slider.getModel().getThumbAt(1).setPosition((float) positionMax);
            }
            isUpdating = false;
        }
        slider.repaint();
    }

    public double getCustomMin() {
        return customMin;
    }

    public double getCustomMax() {
        return customMax;
    }

    @Override
    public void thumbMoved(int thumb, float pos) {
        applyCustomCalibration();
    }

    private void applyCustomCalibration() {
        if (!isUpdating) {
            double min = minSelectableValue;
            double max = maxSelectableValue;
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
//            calibrationPlugin.disableAutoCalibration();
            calibrationPlugin.setSelectedCalibration(ImageJCalibrationMode.Custom);
        }
    }

    @Override
    public void thumbSelected(int thumb) {

    }

    @Override
    public void mousePressed(MouseEvent evt) {

    }

    public CalibrationPlugin2D getCalibrationPlugin() {
        return calibrationPlugin;
    }

    public void setCustomMinMax(double displayRangeMin, double displayRangeMax) {
        this.customMin = displayRangeMin;
        this.customMax = displayRangeMax;
        double positionMin = Math.min(1, Math.max(0, displayRangeMin - minSelectableValue) / (maxSelectableValue - minSelectableValue));
        double positionMax = Math.min(1, Math.max(0, displayRangeMax - minSelectableValue) / (maxSelectableValue - minSelectableValue));
        slider.getModel().getThumbAt(0).setPosition((float) positionMin);
        slider.getModel().getThumbAt(1).setPosition((float) positionMax);
        slider.repaint();
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
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = slider.getWidth() - 2 * ThumbRenderer.SIZE;
            int h = slider.getHeight() - 22;
            g.setColor(getBackground());
            g.fillRect(0, 0, slider.getWidth(), slider.getHeight());
            g.setColor(UIManager.getColor("Button.borderColor"));
            g.drawLine(0, h, w + 2 * ThumbRenderer.SIZE, h);
            g.setColor(COLOR_SELECTED);
            ImageProcessor slice = imageViewerPanelDisplayRangeControl.getCalibrationPlugin().getViewerPanel2D().getCurrentSlice();
            ImageStatistics statistics = imageViewerPanelDisplayRangeControl.getCalibrationPlugin().getViewerPanel2D().getCurrentSliceStats();
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

            DecimalFormat format;
            if (imageViewerPanelDisplayRangeControl.calibrationPlugin.getCurrentImage().getType() == ImagePlus.GRAY32) {
                format = DECIMAL_FORMAT_FLOAT;
            } else {
                format = DECIMAL_FORMAT_INT;
            }

            // Draw the position bar
            for (int i = 0; i < 2; i++) {
                Thumb<DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
                float position = Math.max(0, Math.min(thumb.getPosition(), 1));
                int x = ThumbRenderer.SIZE - 1 + (int) (w * position);
                g.fillRect(x, 4, 2, h + 4);
            }

            // Draw the label text (requires statistics)
            if (statistics != null) {
                String valueMin = format.format(imageViewerPanelDisplayRangeControl.minSelectableValue + slider.getModel().getThumbAt(0).getPosition() *
                        (imageViewerPanelDisplayRangeControl.maxSelectableValue - imageViewerPanelDisplayRangeControl.minSelectableValue));
                String valueMax = format.format(imageViewerPanelDisplayRangeControl.minSelectableValue + slider.getModel().getThumbAt(1).getPosition() *
                        (imageViewerPanelDisplayRangeControl.maxSelectableValue - imageViewerPanelDisplayRangeControl.minSelectableValue));
                int valueMinWidth = g.getFontMetrics().stringWidth(valueMin);
                int valueMaxWidth = g.getFontMetrics().stringWidth(valueMax);
                float positionMin = Math.max(0, Math.min(slider.getModel().getThumbAt(0).getPosition(), 1));
                float positionMax = Math.max(0, Math.min(slider.getModel().getThumbAt(1).getPosition(), 1));

                if (positionMin < positionMax) {
                    // First
                    int valuePositionMinX = ThumbRenderer.SIZE - 1 + (int) (w * positionMin);
                    valuePositionMinX = Math.max(0, Math.min(w - valueMinWidth, valuePositionMinX - (valueMinWidth / 2)));
                    g.drawString(valueMin, valuePositionMinX, h + 18);

                    // Second
                    int valuePositionMaxX = ThumbRenderer.SIZE - 1 + (int) (w * positionMax);
                    valuePositionMaxX = Math.max(0, Math.min(w - valueMaxWidth, valuePositionMaxX - (valueMaxWidth / 2)));
                    valuePositionMaxX = Math.max(valuePositionMaxX, valuePositionMinX + valueMinWidth + 8);
                    g.drawString(valueMax, valuePositionMaxX, h + 18);

                } else {
                    // First
                    int valuePositionMaxX = ThumbRenderer.SIZE - 1 + (int) (w * positionMax);
                    valuePositionMaxX = Math.max(0, Math.min(w - valueMaxWidth, valuePositionMaxX - (valueMaxWidth / 2)));
                    g.drawString(valueMax, valuePositionMaxX, h + 18);

                    // Second
                    int valuePositionMinX = ThumbRenderer.SIZE - 1 + (int) (w * positionMin);
                    valuePositionMinX = Math.max(0, Math.min(w - valueMinWidth, valuePositionMinX - (valueMinWidth / 2)));
                    valuePositionMinX = Math.max(valuePositionMinX, valuePositionMaxX + valueMaxWidth + 8);
                    g.drawString(valueMin, valuePositionMinX, h + 18);
                }
            }

//            for (int i = 0; i < 2; i++) {
//                Thumb<DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
//                float position = Math.max(0, Math.min(thumb.getPosition(), 1));
//                int x = ThumbRenderer.SIZE - 1 + (int) (w * position);
//                g.fillRect(x, 4, 2, h + 4);
//                if (statistics != null) {
//
//                    String value =
//                    int stringWidth = g.getFontMetrics().stringWidth(value + "");
//                    g.drawString("" + value, Math.max(0, Math.min(w - stringWidth, x - (stringWidth / 2))), h + 18);
//                }
//            }
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
