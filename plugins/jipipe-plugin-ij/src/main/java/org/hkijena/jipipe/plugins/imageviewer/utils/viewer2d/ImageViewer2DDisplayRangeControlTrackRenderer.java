package org.hkijena.jipipe.plugins.imageviewer.utils.viewer2d;

import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import org.hkijena.jipipe.desktop.commons.theme.JIPipeDesktopModernMetalTheme;
import org.hkijena.jipipe.plugins.imagejdatatypes.util.ImageJHistogram;
import org.jdesktop.swingx.JXMultiThumbSlider;
import org.jdesktop.swingx.multislider.Thumb;

import javax.swing.*;
import java.awt.*;
import java.text.DecimalFormat;

public class ImageViewer2DDisplayRangeControlTrackRenderer extends JComponent implements org.jdesktop.swingx.multislider.TrackRenderer {

    public static final Color COLOR_SELECTED = JIPipeDesktopModernMetalTheme.PRIMARY5;
    public static final Color COLOR_UNSELECTED = UIManager.getColor("Button.borderColor");
    private final ImageViewer2DDisplayRangeControl displayRangeControl;
    private JXMultiThumbSlider<ImageViewer2DDisplayRangeControl.DisplayRangeStop> slider;
    private boolean logarithmic = true;

    public ImageViewer2DDisplayRangeControlTrackRenderer(ImageViewer2DDisplayRangeControl displayRangeControl) {
        this.displayRangeControl = displayRangeControl;
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
        final int sliderWidth = slider.getWidth() - 2 * ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE;
        final int sliderHeight = slider.getHeight() - 22;
        g.setColor(getBackground());
        g.fillRect(0, 0, slider.getWidth(), slider.getHeight());
        g.setColor(UIManager.getColor("Button.borderColor"));
        g.drawLine(0, sliderHeight, sliderWidth + 2 * ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE, sliderHeight);
        g.setColor(COLOR_SELECTED);
        ImageProcessor slice = displayRangeControl.getCalibrationPlugin().getViewerPanel2D().getCurrentSlice();
        ImageViewer2DSliceStatistics currentSliceStats = displayRangeControl.getCalibrationPlugin().getViewerPanel2D().getCurrentSliceStats();
        if(currentSliceStats != null) {


            ImageJHistogram histogram = currentSliceStats.getHistogram();
            if (histogram != null && slice != null) {
                if (!histogram.isEmpty()) {
                    int selectedXMin = Integer.MAX_VALUE;
                    int selectedXMax = Integer.MIN_VALUE;
                    for (int i = 0; i < 2; i++) {
                        Thumb<ImageViewer2DDisplayRangeControl.DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
                        float position = Math.max(0, Math.min(thumb.getPosition(), 1));
                        int x = ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE - 1 + (int) (sliderWidth * position);
                        selectedXMin = Math.min(x, selectedXMin);
                        selectedXMax = Math.max(x, selectedXMax);
                    }

                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
                    final int numRenderedBins = Math.min(sliderWidth, histogram.size());
                    final int binMaxHeight = sliderHeight - 8;
                    final double binSize = 1.0 * sliderWidth / numRenderedBins;
                    final double lmax = Math.log(Math.max(1, histogram.getMaxCount()));
                    for (int k = 0; k < numRenderedBins; k++) {
                        final long countInRange = histogram.maxInPercentageRange(1.0 * k / numRenderedBins, (1.0 + k) / numRenderedBins);
                        int binHeight;
                        if (logarithmic)
                            binHeight = (int) Math.round((Math.log(countInRange) / lmax) * binMaxHeight);
                        else
                            binHeight = (int) (Math.round(1.0 * countInRange / histogram.getMaxCount()) * binMaxHeight);
                        int x = (int) (k * binSize) + ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE;
                        if (x >= selectedXMin && x <= selectedXMax)
                            g.setColor(COLOR_SELECTED);
                        else
                            g.setColor(COLOR_UNSELECTED);
                        g.fillRect(x, binMaxHeight - binHeight + 8, (int) binSize + 1, binHeight);
                    }
                }
            }
        }
        g.setColor(UIManager.getColor("Label.foreground"));

        DecimalFormat format;
        if (displayRangeControl.getCalibrationPlugin().getCurrentImagePlus() == null) {
            return;
        }
        if (displayRangeControl.getCalibrationPlugin().getCurrentImagePlus().getType() == ImagePlus.GRAY32) {
            format = ImageViewer2DDisplayRangeControl.DECIMAL_FORMAT_FLOAT;
        } else {
            format = ImageViewer2DDisplayRangeControl.DECIMAL_FORMAT_INT;
        }

        // Draw the position bar
        for (int i = 0; i < 2; i++) {
            Thumb<ImageViewer2DDisplayRangeControl.DisplayRangeStop> thumb = slider.getModel().getThumbAt(i);
            float position = Math.max(0, Math.min(thumb.getPosition(), 1));
            int x = ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE - 1 + (int) (sliderWidth * position);
            g.fillRect(x, 4, 2, sliderHeight + 4);
        }

        // Draw the label text (requires statistics)
        if(currentSliceStats != null) {

            ImageStatistics statistics = currentSliceStats.getFastImageStatistics();
            if (statistics != null) {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                String valueMin = format.format(displayRangeControl.getMinSelectableValue() + slider.getModel().getThumbAt(0).getPosition() *
                        (displayRangeControl.getMaxSelectableValue() - displayRangeControl.getMinSelectableValue()));
                String valueMax = format.format(displayRangeControl.getMinSelectableValue() + slider.getModel().getThumbAt(1).getPosition() *
                        (displayRangeControl.getMaxSelectableValue() - displayRangeControl.getMinSelectableValue()));
                int valueMinWidth = g.getFontMetrics().stringWidth(valueMin);
                int valueMaxWidth = g.getFontMetrics().stringWidth(valueMax);
                float positionMin = Math.max(0, Math.min(slider.getModel().getThumbAt(0).getPosition(), 1));
                float positionMax = Math.max(0, Math.min(slider.getModel().getThumbAt(1).getPosition(), 1));

                if (positionMin < positionMax) {
                    // First
                    int valuePositionMinX = ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE - 1 + (int) (sliderWidth * positionMin);
                    valuePositionMinX = Math.max(0, Math.min(sliderWidth - valueMinWidth, valuePositionMinX - (valueMinWidth / 2)));
                    g.drawString(valueMin, valuePositionMinX, sliderHeight + 18);

                    // Second
                    int valuePositionMaxX = ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE - 1 + (int) (sliderWidth * positionMax);
                    valuePositionMaxX = Math.max(0, Math.min(sliderWidth - valueMaxWidth, valuePositionMaxX - (valueMaxWidth / 2)));
                    valuePositionMaxX = Math.max(valuePositionMaxX, valuePositionMinX + valueMinWidth + 8);
                    g.drawString(valueMax, valuePositionMaxX, sliderHeight + 18);

                } else {
                    // First
                    int valuePositionMaxX = ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE - 1 + (int) (sliderWidth * positionMax);
                    valuePositionMaxX = Math.max(0, Math.min(sliderWidth - valueMaxWidth, valuePositionMaxX - (valueMaxWidth / 2)));
                    g.drawString(valueMax, valuePositionMaxX, sliderHeight + 18);

                    // Second
                    int valuePositionMinX = ImageViewer2DDisplayRangeControl.ThumbRenderer.SIZE - 1 + (int) (sliderWidth * positionMin);
                    valuePositionMinX = Math.max(0, Math.min(sliderWidth - valueMinWidth, valuePositionMinX - (valueMinWidth / 2)));
                    valuePositionMinX = Math.max(valuePositionMinX, valuePositionMaxX + valueMaxWidth + 8);
                    g.drawString(valueMin, valuePositionMinX, sliderHeight + 18);
                }
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
