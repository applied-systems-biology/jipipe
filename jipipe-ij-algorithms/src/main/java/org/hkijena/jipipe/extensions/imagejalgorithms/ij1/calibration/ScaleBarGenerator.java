package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.calibration;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.TextRoi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontFamilyParameter;
import org.hkijena.jipipe.extensions.parameters.library.primitives.FontStyleParameter;

import java.awt.*;

/**
 * Copy of {@link ij.plugin.ScaleBar} where extended access to parameters is given
 */
public class ScaleBarGenerator {
    private final ScaleBarConfiguration config = new ScaleBarConfiguration();

    private final ImagePlus imp;
    private int hBarWidthInPixels;
    private int vBarHeightInPixels;
    private int roiX, roiY, roiWidth, roiHeight;

    private Rectangle hBackground = new Rectangle();
    private Rectangle hBar = new Rectangle();
    private Rectangle hText = new Rectangle();
    private Rectangle vBackground = new Rectangle();
    private Rectangle vBar = new Rectangle();
    private Rectangle vText = new Rectangle();

    public ScaleBarGenerator(ImagePlus imp) {
        this.imp = imp;
    }

    /**
     * There is no hard codded value for the width of the scalebar,
     * when the plugin is called for the first time in an ImageJ
     * instance, a defautl value for the width will be computed by
     * this method.
     */
    public void computeDefaultBarWidth() {
        Calibration cal = imp.getCalibration();
        ImageWindow win = imp.getWindow();
        double mag = (win != null) ? win.getCanvas().getMagnification() : 1.0;
        if (mag > 1.0)
            mag = 1.0;

        double pixelWidth = cal.pixelWidth;
        if (pixelWidth == 0.0)
            pixelWidth = 1.0;
        double pixelHeight = cal.pixelHeight;
        if (pixelHeight == 0.0)
            pixelHeight = 1.0;
        double imageWidth = imp.getWidth() * pixelWidth;
        double imageHeight = imp.getHeight() * pixelHeight;

        if (config.hBarWidth <= 0.0 || config.hBarWidth > 0.67 * imageWidth) {
            // If the bar is of negative width or too wide for the image,
            // set the bar width to 80 pixels.
            config.hBarWidth = (80.0 * pixelWidth) / mag;
            if (config.hBarWidth > 0.67 * imageWidth)
                // If 80 pixels is too much, do 2/3 of the image.
                config.hBarWidth = 0.67 * imageWidth;
            if (config.hBarWidth > 5.0)
                // If the resulting size is larger than 5 units, round the value.
                config.hBarWidth = (int) config.hBarWidth;
        }

        if (config.vBarHeight <= 0.0 || config.vBarHeight > 0.67 * imageHeight) {
            config.vBarHeight = (80.0 * pixelHeight) / mag;
            if (config.vBarHeight > 0.67 * imageHeight)
                // If 80 pixels is too much, do 2/3 of the image.
                config.vBarHeight = 0.67 * imageHeight;
            if (config.vBarHeight > 5.0)
                // If the resulting size is larger than 5 units, round the value.
                config.vBarHeight = (int) config.vBarHeight;
        }
    }

    /**
     * Return the X unit strings defined in the image calibration.
     */
    String getHUnit() {
        String hUnits = imp.getCalibration().getXUnits();
        if (hUnits.equals("microns"))
            hUnits = IJ.micronSymbol + "m";
        return hUnits;
    }

    /**
     * Return the Y unit strings defined in the image calibration.
     */
    String getVUnit() {
        String vUnits = imp.getCalibration().getYUnits();
        if (vUnits.equals("microns"))
            vUnits = IJ.micronSymbol + "m";
        return vUnits;
    }

    /**
     * Create & draw the scalebar using an Overlay.
     */
    public Overlay createScaleBarOverlay() {
        Overlay overlay = new Overlay();

        Color barColor = config.barColor;
        Color textColor = config.textColor;
        Color backgroundColor = config.backgroundColor;

        int fontType = config.fontStyle.getNativeValue();
        String face = config.fontFamily.getValue();
        Font font = new Font(face, fontType, config.fontSize);
        ImageProcessor ip = imp.getProcessor();
        ip.setFont(font);

        setElementsPositions(ip);

        if (backgroundColor != null) {
            if (config.showHorizontal) {
                Roi hBackgroundRoi = new Roi(hBackground.x, hBackground.y, hBackground.width, hBackground.height);
                hBackgroundRoi.setFillColor(backgroundColor);
                overlay.add(hBackgroundRoi, "Scale bar background");
            }
            if (config.showVertical) {
                Roi vBackgroundRoi = new Roi(vBackground.x, vBackground.y, vBackground.width, vBackground.height);
                vBackgroundRoi.setFillColor(backgroundColor);
                overlay.add(vBackgroundRoi, "Scale bar background");
            }
        }

        if (config.showHorizontal) {
            Roi hBarRoi = new Roi(hBar.x, hBar.y, hBar.width, hBar.height);
            hBarRoi.setFillColor(barColor);
            overlay.add(hBarRoi, "Scale bar");
        }
        if (config.showVertical) {
            Roi vBarRoi = new Roi(vBar.x, vBar.y, vBar.width, vBar.height);
            vBarRoi.setFillColor(barColor);
            overlay.add(vBarRoi, "Scale bar");
        }

        if (!config.hideText) {
            if (config.showHorizontal) {
                TextRoi hTextRoi = new TextRoi(hText.x, hText.y, getHLabel(), font);
                hTextRoi.setStrokeColor(textColor);
                overlay.add(hTextRoi, "Scale bar label (" + getHLabel() + ")");
            }
            if (config.showVertical) {
                TextRoi vTextRoi = new TextRoi(vText.x, vText.y + vText.height, getVLabel(), font);
                vTextRoi.setStrokeColor(textColor);
                vTextRoi.setAngle(90.0);
                overlay.add(vTextRoi, "Scale bar label (" + getVLabel() + ")");
            }
        }

        return overlay;
    }

    /**
     * Returns the text to draw near the scalebar
     */
    String getHLabel() {
        return config.hLabel;
    }

    /**
     * Returns the text to draw near the scalebar
     */
    String getVLabel() {
        return config.vLabel;
    }

    /**
     * Returns the width of the box that contains the horizontal scalebar and
     * its label.
     */
    int getHBoxWidthInPixels() {
        updateFont();
        ImageProcessor ip = imp.getProcessor();
        int hLabelWidth = config.hideText ? 0 : ip.getStringWidth(getHLabel());
        int hBoxWidth = Math.max(hBarWidthInPixels, hLabelWidth);
        return (config.showHorizontal ? hBoxWidth : 0);
    }

    /**
     * Returns the height of the box that contains the horizontal scalebar and
     * its label.
     */
    int getHBoxHeightInPixels() {
        int hLabelHeight = config.hideText ? 0 : config.fontSize;
        int hBoxHeight = config.barThicknessInPixels + (int) (hLabelHeight * 1.25);
        return (config.showHorizontal ? hBoxHeight : 0);
    }

    /**
     * Returns the height of the box that contains the vertical scalebar and
     * its label.
     */
    int getVBoxHeightInPixels() {
        updateFont();
        ImageProcessor ip = imp.getProcessor();
        int vLabelHeight = config.hideText ? 0 : ip.getStringWidth(getVLabel());
        int vBoxHeight = Math.max(vBarHeightInPixels, vLabelHeight);
        return (config.showVertical ? vBoxHeight : 0);
    }

    /**
     * Returns the width of the box that contains the vertical scalebar and
     * its label.
     */
    int getVBoxWidthInPixels() {
        int vLabelWidth = config.hideText ? 0 : config.fontSize;
        int vBoxWidth = config.barThicknessInPixels + (int) (vLabelWidth * 1.25);
        return (config.showVertical ? vBoxWidth : 0);
    }

    /**
     * Returns the size of margins that should be displayed between the scalebar
     * elements and the image edge.
     */
    int getOuterMarginSizeInPixels() {
        int imageWidth = imp.getWidth();
        int imageHeight = imp.getHeight();
        return (imageWidth + imageHeight) / 100;
    }

    /**
     * Retruns the size of margins that should be displayed between the scalebar
     * elements and the edge of the element's backround.
     */
    int getInnerMarginSizeInPixels() {
        int maxWidth = Math.max(getHBoxWidthInPixels(), getVBoxHeightInPixels());
        int margin = Math.max(maxWidth / 20, 2);
        return config.backgroundColor == null ? 0 : margin;
    }

    void updateFont() {
        ImageProcessor ip = imp.getProcessor();
        ip.setFont(config.getFontStyle().toFont(config.getFontFamily(), config.fontSize));
        ip.setAntialiasedText(true);
    }

    public ScaleBarConfiguration getConfig() {
        return config;
    }

    /**
     * Sets the positions x y of hBackground and vBackground based on
     * the current configuration.
     */
    void setBackgroundBoxesPositions() {
        Calibration cal = imp.getCalibration();
        hBarWidthInPixels = (int) (config.hBarWidth / cal.pixelWidth);
        vBarHeightInPixels = (int) (config.vBarHeight / cal.pixelHeight);

        boolean hTextTop = config.showVertical && (config.location.equals(ScaleBarPosition.UpperLeft) || config.location.equals(ScaleBarPosition.UpperRight));

        int imageWidth = imp.getWidth();
        int imageHeight = imp.getHeight();
        int hBoxWidth = getHBoxWidthInPixels();
        int hBoxHeight = getHBoxHeightInPixels();
        int vBoxWidth = getVBoxWidthInPixels();
        int vBoxHeight = getVBoxHeightInPixels();
        int outerMargin = getOuterMarginSizeInPixels();
        int innerMargin = getInnerMarginSizeInPixels();

        hBackground.width = innerMargin + hBoxWidth + innerMargin;
        hBackground.height = innerMargin + hBoxHeight + innerMargin;
        vBackground.width = innerMargin + vBoxWidth + innerMargin;
        vBackground.height = innerMargin + vBoxHeight + innerMargin;

        if (config.location.equals(ScaleBarPosition.UpperRight)) {
            hBackground.x = imageWidth - outerMargin - innerMargin - vBoxWidth + (config.showVertical ? config.barThicknessInPixels : 0) - hBoxWidth - innerMargin;
            hBackground.y = outerMargin;
            vBackground.x = imageWidth - outerMargin - innerMargin - vBoxWidth - innerMargin;
            vBackground.y = outerMargin + (hTextTop ? hBoxHeight - config.barThicknessInPixels : 0);
            hBackground.width += (config.showVertical ? vBoxWidth - config.barThicknessInPixels : 0);

        } else if (config.location.equals(ScaleBarPosition.LowerRight)) {
            hBackground.x = imageWidth - outerMargin - innerMargin - vBoxWidth - hBoxWidth + (config.showVertical ? config.barThicknessInPixels : 0) - innerMargin;
            hBackground.y = imageHeight - outerMargin - innerMargin - hBoxHeight - innerMargin;
            vBackground.x = imageWidth - outerMargin - innerMargin - vBoxWidth - innerMargin;
            vBackground.y = imageHeight - outerMargin - innerMargin - hBoxHeight + (config.showHorizontal ? config.barThicknessInPixels : 0) - vBoxHeight - innerMargin;
            vBackground.height += (config.showHorizontal ? hBoxHeight - config.barThicknessInPixels : 0);

        } else if (config.location.equals(ScaleBarPosition.UpperLeft)) {
            hBackground.x = outerMargin + (config.showVertical ? vBackground.width - 2 * innerMargin - config.barThicknessInPixels : 0);
            hBackground.y = outerMargin;
            vBackground.x = outerMargin;
            vBackground.y = outerMargin + (hTextTop ? hBoxHeight - config.barThicknessInPixels : 0);
            hBackground.width += (config.showVertical ? vBoxWidth - config.barThicknessInPixels : 0);
            hBackground.x -= (config.showVertical ? vBoxWidth - config.barThicknessInPixels : 0);

        } else if (config.location.equals(ScaleBarPosition.LowerLeft)) {
            hBackground.x = outerMargin + (config.showVertical ? vBackground.width - 2 * innerMargin - config.barThicknessInPixels : 0);
            hBackground.y = imageHeight - outerMargin - innerMargin - hBoxHeight - innerMargin;
            vBackground.x = outerMargin;
            vBackground.y = imageHeight - outerMargin - innerMargin - hBoxHeight + (config.showHorizontal ? config.barThicknessInPixels : 0) - vBoxHeight - innerMargin;
            vBackground.height += (config.showHorizontal ? hBoxHeight - config.barThicknessInPixels : 0);

        } else {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Sets the rectangles x y positions for scalebar elements (hBar, hText, vBar, vText),
     * based on the current configuration. Also sets the width and height of the rectangles.
     *
     * The position of each rectangle is relative to hBackground and vBackground,
     * so setBackgroundBoxesPositions() must run before this method computes positions.
     * This method calls setBackgroundBoxesPositions().
     */
    void setElementsPositions(ImageProcessor ip) {

        setBackgroundBoxesPositions();

        int hBoxWidth = getHBoxWidthInPixels();
        int hBoxHeight = getHBoxHeightInPixels();

        int vBoxWidth = getVBoxWidthInPixels();
        int vBoxHeight = getVBoxHeightInPixels();

        int innerMargin = getInnerMarginSizeInPixels();

        boolean right = config.location.equals(ScaleBarPosition.LowerRight) || config.location.equals(ScaleBarPosition.UpperRight);
        boolean upper = config.location.equals(ScaleBarPosition.UpperRight) || config.location.equals(ScaleBarPosition.UpperLeft);
        boolean hTextTop = config.showVertical && upper;

        hBar.x = hBackground.x + innerMargin + (hBoxWidth - hBarWidthInPixels) / 2 + (config.showVertical && !right && upper ? vBoxWidth - config.barThicknessInPixels : 0);
        hBar.y = hBackground.y + innerMargin + (hTextTop ? hBoxHeight - config.barThicknessInPixels : 0);
        hBar.width = hBarWidthInPixels;
        hBar.height = config.barThicknessInPixels;

        hText.height = config.hideText ? 0 : config.fontSize;
        hText.width = config.hideText ? 0 : ip.getStringWidth(getHLabel());
        hText.x = hBackground.x + innerMargin + (hBoxWidth - hText.width) / 2 + (config.showVertical && !right && upper ? vBoxWidth - config.barThicknessInPixels : 0);
        hText.y = hTextTop ? (hBackground.y + innerMargin - (int) (config.fontSize * 0.25)) : (hBar.y + hBar.height);

        vBar.width = config.barThicknessInPixels;
        vBar.height = vBarHeightInPixels;
        vBar.x = vBackground.x + (right ? innerMargin : vBackground.width - config.barThicknessInPixels - innerMargin);
        vBar.y = vBackground.y + innerMargin + (vBoxHeight - vBar.height) / 2;

        vText.height = config.hideText ? 0 : ip.getStringWidth(getVLabel());
        vText.width = config.hideText ? 0 : config.fontSize;
        vText.x = right ? (vBar.x + vBar.width) : (vBar.x - vBoxWidth + config.barThicknessInPixels - (int) (config.fontSize * 0.25));
        vText.y = vBackground.y + innerMargin + (vBoxHeight - vText.height) / 2;
    }

    public enum ScaleBarPosition {
        UpperRight, LowerRight, LowerLeft, UpperLeft;


        @Override
        public String toString() {
            switch (this) {
                case UpperRight:
                    return "Upper right";
                case LowerRight:
                    return "Lower right";
                case LowerLeft:
                    return "Lower left";
                case UpperLeft:
                    return "Upper left";
                default:
                    return super.toString();
            }
        }
    }

    public static class ScaleBarConfiguration {
        public Color textColor = Color.WHITE;
        FontFamilyParameter fontFamily = new FontFamilyParameter();
        FontStyleParameter fontStyle = FontStyleParameter.Bold;
        boolean labelAll;
        private boolean showHorizontal;
        private boolean showVertical;
        private double hBarWidth;
        private double vBarHeight;
        private String vLabel;
        private String hLabel;
        private int barThicknessInPixels;
        private ScaleBarPosition location = ScaleBarPosition.LowerRight;
        private Color barColor = Color.WHITE;
        private Color backgroundColor = null;
        private boolean hideText;
        private boolean useOverlay;
        private int fontSize;

        /**
         * Create ScaleBarConfiguration with default values.
         */
        ScaleBarConfiguration() {
            this.showHorizontal = true;
            this.showVertical = false;
            this.hBarWidth = -1;
            this.vBarHeight = -1;
            this.barThicknessInPixels = 4;
            this.hideText = false;
            this.useOverlay = true;
            this.fontSize = 14;
            this.labelAll = false;
        }

        public boolean isShowHorizontal() {
            return showHorizontal;
        }

        public void setShowHorizontal(boolean showHorizontal) {
            this.showHorizontal = showHorizontal;
        }

        public boolean isShowVertical() {
            return showVertical;
        }

        public void setShowVertical(boolean showVertical) {
            this.showVertical = showVertical;
        }

        public double gethBarWidth() {
            return hBarWidth;
        }

        public void sethBarWidth(double hBarWidth) {
            this.hBarWidth = hBarWidth;
        }

        public double getvBarHeight() {
            return vBarHeight;
        }

        public void setvBarHeight(double vBarHeight) {
            this.vBarHeight = vBarHeight;
        }

        public String getvLabel() {
            return vLabel;
        }

        public void setvLabel(String vLabel) {
            this.vLabel = vLabel;
        }

        public String gethLabel() {
            return hLabel;
        }

        public void sethLabel(String hLabel) {
            this.hLabel = hLabel;
        }

        public int getBarThicknessInPixels() {
            return barThicknessInPixels;
        }

        public void setBarThicknessInPixels(int barThicknessInPixels) {
            this.barThicknessInPixels = barThicknessInPixels;
        }

        public ScaleBarPosition getLocation() {
            return location;
        }

        public void setLocation(ScaleBarPosition location) {
            this.location = location;
        }

        public Color getBarColor() {
            return barColor;
        }

        public void setBarColor(Color barColor) {
            this.barColor = barColor;
        }

        public Color getTextColor() {
            return textColor;
        }

        public void setTextColor(Color textColor) {
            this.textColor = textColor;
        }

        public Color getBackgroundColor() {
            return backgroundColor;
        }

        public void setBackgroundColor(Color backgroundColor) {
            this.backgroundColor = backgroundColor;
        }

        public boolean isHideText() {
            return hideText;
        }

        public void setHideText(boolean hideText) {
            this.hideText = hideText;
        }

        public boolean isUseOverlay() {
            return useOverlay;
        }

        public void setUseOverlay(boolean useOverlay) {
            this.useOverlay = useOverlay;
        }

        public int getFontSize() {
            return fontSize;
        }

        public void setFontSize(int fontSize) {
            this.fontSize = fontSize;
        }

        public FontFamilyParameter getFontFamily() {
            return fontFamily;
        }

        public void setFontFamily(FontFamilyParameter fontFamily) {
            this.fontFamily = fontFamily;
        }

        public FontStyleParameter getFontStyle() {
            return fontStyle;
        }

        public void setFontStyle(FontStyleParameter fontStyle) {
            this.fontStyle = fontStyle;
        }

        public boolean isLabelAll() {
            return labelAll;
        }

        public void setLabelAll(boolean labelAll) {
            this.labelAll = labelAll;
        }
    } //ScaleBarConfiguration inner class
}
