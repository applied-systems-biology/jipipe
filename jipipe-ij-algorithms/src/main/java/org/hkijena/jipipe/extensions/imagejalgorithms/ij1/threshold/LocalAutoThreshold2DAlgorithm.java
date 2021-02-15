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

package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.threshold;

import com.fathzer.soft.javaluator.StaticVariableSet;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.NewImage;
import ij.gui.OvalRoi;
import ij.gui.Roi;
import ij.plugin.ContrastEnhancer;
import ij.plugin.filter.RankFilters;
import ij.process.AutoThresholder;
import ij.process.Blitter;
import ij.process.ImageConverter;
import ij.process.ImageProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.data.JIPipeAnnotation;
import org.hkijena.jipipe.api.data.JIPipeAnnotationMergeStrategy;
import org.hkijena.jipipe.api.data.JIPipeDefaultMutableSlotConfiguration;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeDynamicParameterCollection;
import org.hkijena.jipipe.api.parameters.JIPipeMutableParameterAccess;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.api.parameters.JIPipeParameterVisibility;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscale8UData;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.greyscale.ImagePlusGreyscaleMaskData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.extensions.parameters.expressions.DefaultExpressionParameter;
import org.hkijena.jipipe.extensions.parameters.expressions.ExpressionParameterSettings;
import org.hkijena.jipipe.extensions.parameters.primitives.OptionalAnnotationNameParameter;
import org.scijava.Priority;

import java.util.ArrayList;
import java.util.List;

import static org.hkijena.jipipe.extensions.imagejalgorithms.ImageJAlgorithmsExtension.ADD_MASK_QUALIFIER;

/**
 * Segmenter node that thresholds via an auto threshold
 * Based on code from {@link fiji.threshold.Auto_Local_Threshold}
 */
@JIPipeDocumentation(name = "Local auto threshold 2D", description = "Applies a local auto-thresholding algorithm. " +
        "If higher-dimensional data is provided, the filter is applied to each 2D slice.")
@JIPipeOrganization(menuPath = "Threshold", nodeTypeCategory = ImagesNodeTypeCategory.class)
@JIPipeInputSlot(value = ImagePlusGreyscale8UData.class, slotName = "Input")
@JIPipeOutputSlot(value = ImagePlusGreyscaleMaskData.class, slotName = "Output")
public class LocalAutoThreshold2DAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private Method method = Method.Otsu;
    private boolean darkBackground = true;
    private JIPipeDynamicParameterCollection methodParameters;
    private JIPipeMutableParameterAccess bernsenContrastThresholdParameter;
    private JIPipeMutableParameterAccess meanCValueParameter;
    private JIPipeMutableParameterAccess medianCValueParameter;
    private JIPipeMutableParameterAccess midGreyCValueParameter;
    private JIPipeMutableParameterAccess niblackKValueParameter;
    private JIPipeMutableParameterAccess niblackCValueParameter;
    private JIPipeMutableParameterAccess phansalkarKValueParameter;
    private JIPipeMutableParameterAccess phansalkarRValueParameter;
    private JIPipeMutableParameterAccess phansalkarPValueParameter;
    private JIPipeMutableParameterAccess phansalkarQValueParameter;
    private JIPipeMutableParameterAccess sauvolaKValueParameter;
    private JIPipeMutableParameterAccess sauvolaRValueParameter;
    /**
     * @param info the info
     */
    public LocalAutoThreshold2DAlgorithm(JIPipeNodeInfo info) {
        super(info, JIPipeDefaultMutableSlotConfiguration.builder().addInputSlot("Input", ImagePlusGreyscale8UData.class)
                .addOutputSlot("Output", ImagePlusGreyscaleMaskData.class, "Input", ADD_MASK_QUALIFIER)
                .allowOutputSlotInheritance(true)
                .seal()
                .build());
        this.methodParameters = new JIPipeDynamicParameterCollection(false);
        registerSubParameter(methodParameters);
        initMethodParameters();
        updateMethodParameters();
    }

    private void initMethodParameters() {
        // Bernsen
        bernsenContrastThresholdParameter = methodParameters.addParameter("bernsen-contrast-threshold", double.class);
        bernsenContrastThresholdParameter.setName("Contrast threshold");
        bernsenContrastThresholdParameter.set(15);
        bernsenContrastThresholdParameter.setDescription("Bernsen recommends a value of 15 and a radius of 16.");

        // Mean
        meanCValueParameter = methodParameters.addParameter("mean-c-value", int.class);
        meanCValueParameter.setName("Local threshold");
        meanCValueParameter.set(0);
        meanCValueParameter.setDescription("A pixel is considered foreground if its value is greater than (mean of local values - local threshold).");

        // Median
        medianCValueParameter = methodParameters.addParameter("median-c-value", int.class);
        medianCValueParameter.setName("Local threshold");
        medianCValueParameter.set(0);
        medianCValueParameter.setDescription("A pixel is considered foreground if its value is greater than (median of local values - local threshold).");

        // Mid-grey
        midGreyCValueParameter = methodParameters.addParameter("mid-grey-c-value", int.class);
        midGreyCValueParameter.setName("Local threshold");
        midGreyCValueParameter.set(0);
        midGreyCValueParameter.setDescription("A pixel is considered foreground if its value is greater than (((max of local values) + (min of local values)) / 2 - local threshold).");

        // Niblack
        niblackKValueParameter = methodParameters.addParameter("niblack-k-value", double.class);
        niblackKValueParameter.setName("K");
        niblackKValueParameter.set(0.2);
        niblackKValueParameter.setDescription("Niblack recommends a value of 0.2");

        niblackCValueParameter = methodParameters.addParameter("niblack-c-value", int.class);
        niblackCValueParameter.setName("Modifier");
        niblackCValueParameter.set(0);
        niblackCValueParameter.setDescription("This value is subtracted from the calculated local threshold");

        // Phansalkar
        phansalkarKValueParameter = methodParameters.addParameter("phansalkar-k-value", double.class);
        phansalkarKValueParameter.setName("K");
        phansalkarKValueParameter.set(0.25);
        phansalkarKValueParameter.setDescription("Phansalkar recommends a value of 0.25");

        phansalkarRValueParameter = methodParameters.addParameter("phansalkar-r-value", double.class);
        phansalkarRValueParameter.setName("R");
        phansalkarRValueParameter.set(0.5);
        phansalkarRValueParameter.setDescription("Phansalkar recommends a value of 0.5");

        phansalkarPValueParameter = methodParameters.addParameter("phansalkar-p-value", double.class);
        phansalkarPValueParameter.setName("P");
        phansalkarPValueParameter.set(2.0);
        phansalkarPValueParameter.setDescription("Phansalkar recommends a value of 2");

        phansalkarQValueParameter = methodParameters.addParameter("phansalkar-q-value", double.class);
        phansalkarQValueParameter.setName("Q");
        phansalkarQValueParameter.set(10.0);
        phansalkarQValueParameter.setDescription("Phansalkar recommends a value of 10");

        // Sauvola
        sauvolaKValueParameter = methodParameters.addParameter("sauvola-k-value", double.class);
        sauvolaKValueParameter.setName("K");
        sauvolaKValueParameter.set(0.5);
        sauvolaKValueParameter.setDescription("Sauvola recommends a value of 0.5");

        sauvolaRValueParameter = methodParameters.addParameter("sauvola-r-value", double.class);
        sauvolaRValueParameter.setName("R");
        sauvolaRValueParameter.set(128.0);
        sauvolaRValueParameter.setDescription("Sauvola recommends a value of 128");
    }

    /**
     * Copies the algorithm
     *
     * @param other the original
     */
    public LocalAutoThreshold2DAlgorithm(LocalAutoThreshold2DAlgorithm other) {
        super(other);
        this.method = other.method;
        this.darkBackground = other.darkBackground;
        this.methodParameters = new JIPipeDynamicParameterCollection(other.methodParameters);
        registerSubParameter(methodParameters);
        updateMethodParameters();
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlusData inputData = dataBatch.getInputData(getFirstInputSlot(), ImagePlusGreyscale8UData.class, progressInfo);
        ImagePlus img = inputData.getDuplicateImage();
    }

    private void updateMethodParameters() {
        String methodNameString = method.name().toLowerCase();
        for (String key : methodParameters.getParameters().keySet()) {
            JIPipeMutableParameterAccess parameter = methodParameters.getParameter(key);
            if(key.startsWith(methodNameString)) {
                parameter.setVisibility(JIPipeParameterVisibility.TransitiveVisible);
            }
            else {
                parameter.setVisibility(JIPipeParameterVisibility.Hidden);
            }
        }
        methodParameters.getEventBus().post(new ParameterStructureChangedEvent(methodParameters));
    }

    @JIPipeDocumentation(name = "Method parameters", description = "Following parameters are specific to the currently selected thresholding-method.")
    @JIPipeParameter("method-parameters")
    public JIPipeDynamicParameterCollection getMethodParameters() {
        return methodParameters;
    }

    @JIPipeParameter(value = "method", priority = Priority.HIGH)
    @JIPipeDocumentation(name = "Method")
    public Method getMethod() {
        return method;
    }

    @JIPipeParameter("method")
    public void setMethod(Method method) {
        this.method = method;
        updateMethodParameters();
    }

    @JIPipeDocumentation(name = "Dark background", description = "If the background color is dark. Disable this if your image has a bright background.")
    @JIPipeParameter("dark-background")
    public boolean isDarkBackground() {
        return darkBackground;
    }

    @JIPipeParameter("dark-background")
    public void setDarkBackground(boolean darkBackground) {
        this.darkBackground = darkBackground;
    }

    public static void Bernsen(ImagePlus imp, int radius, double contrast_threshold, boolean doIwhite) {
        // Bernsen recommends WIN_SIZE = 31 and CONTRAST_THRESHOLD = 15.
        //  1) Bernsen J. (1986) "Dynamic Thresholding of Grey-Level Images"
        //    Proc. of the 8th Int. Conf. on Pattern Recognition, pp. 1251-1255
        //  2) Sezgin M. and Sankur B. (2004) "Survey over Image Thresholding
        //   Techniques and Quantitative Performance Evaluation" Journal of
        //   Electronic Imaging, 13(1): 146-165
        //   http://citeseer.ist.psu.edu/sezgin04survey.html
        // Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
        // This version uses a circular local window, instead of a rectagular one
        ImagePlus Maximp, Minimp;
        ImageProcessor ip=imp.getProcessor(), ipMax, ipMin;
        int local_contrast;
        int mid_gray;
        byte object;
        byte backg;
        int temp;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Maximp=duplicateImage(ip);
        ipMax=Maximp.getProcessor();
        RankFilters rf=new RankFilters();
        rf.rank(ipMax, radius, rf.MAX);// Maximum
        //Maximp.show();
        Minimp=duplicateImage(ip);
        ipMin=Minimp.getProcessor();
        rf.rank(ipMin, radius, rf.MIN); //Minimum
        //Minimp.show();
        byte[] pixels = (byte [])ip.getPixels();
        byte[] max = (byte [])ipMax.getPixels();
        byte[] min = (byte [])ipMin.getPixels();

        for (int i=0; i<pixels.length; i++) {
            local_contrast = (max[i]&0xff) -(min[i]&0xff);
            mid_gray = ((min[i]&0xff) + (max[i]&0xff) ) / 2;
            temp= pixels[i] & 0x0000ff;
            if ( local_contrast < contrast_threshold )
                pixels[i] = ( mid_gray >= 128 ) ? object :  backg;  //Low contrast region
            else
                pixels[i] = (temp >= mid_gray ) ? object : backg;
        }
        //imp.updateAndDraw();
    }

    public static void Contrast(ImagePlus imp, int radius, boolean doIwhite) {
        // G. Landini, 2013
        // Based on a simple contrast toggle. This procedure does not have user-provided parameters other than the kernel radius
        // Sets the pixel value to either white or black depending on whether its current value is closest to the local Max or Min respectively
        // The procedure is similar to Toggle Contrast Enhancement (see Soille, Morphological Image Analysis (2004), p. 259

        ImagePlus Maximp, Minimp;
        ImageProcessor ip=imp.getProcessor(), ipMax, ipMin;
        int c_value =0;
        int mid_gray;
        byte object;
        byte backg;


        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Maximp=duplicateImage(ip);
        ipMax=Maximp.getProcessor();
        RankFilters rf=new RankFilters();
        rf.rank(ipMax, radius, rf.MAX);// Maximum
        //Maximp.show();
        Minimp=duplicateImage(ip);
        ipMin=Minimp.getProcessor();
        rf.rank(ipMin, radius, rf.MIN); //Minimum
        //Minimp.show();
        byte[] pixels = (byte [])ip.getPixels();
        byte[] max = (byte [])ipMax.getPixels();
        byte[] min = (byte [])ipMin.getPixels();
        for (int i=0; i<pixels.length; i++) {
            pixels[i] = ((Math.abs((int)(max[i]&0xff- pixels[i]&0xff)) <= Math.abs((int)(pixels[i]&0xff- min[i]&0xff))) && ((int)(pixels[i]&0xff) != 0)) ? object :  backg;
        }
        //imp.updateAndDraw();
    }

    public static void Mean(ImagePlus imp, int radius, int c_value, boolean doIwhite) {
        // See: Image Processing Learning Resourches HIPR2
        // http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
        ImagePlus Meanimp;
        ImageProcessor ip=imp.getProcessor(), ipMean;
        byte object;
        byte backg;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Meanimp=duplicateImage(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();

        ipMean=Meanimp.getProcessor();
        RankFilters rf=new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean
        //Meanimp.show();
        byte[] pixels = (byte []) ip.getPixels();
        float[] mean = (float []) ipMean.getPixels();

        for (int i=0; i<pixels.length; i++)
            pixels[i] = ( (pixels[i] &0xff) > (int)( mean[i]  - c_value)) ? object : backg;
        //imp.updateAndDraw();
    }

    public static void Median(ImagePlus imp, int radius, int c_value, boolean doIwhite) {
        // See: Image Processing Learning Resourches HIPR2
        // http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
        ImagePlus Medianimp;
        ImageProcessor ip=imp.getProcessor(), ipMedian;
        byte object;
        byte backg;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Medianimp=duplicateImage(ip);
        ipMedian=Medianimp.getProcessor();
        RankFilters rf=new RankFilters();
        rf.rank(ipMedian, radius, rf.MEDIAN);
        //Medianimp.show();
        byte[] pixels = (byte []) ip.getPixels();
        byte[] median = (byte []) ipMedian.getPixels();

        for (int i=0; i<pixels.length; i++)
            pixels[i] = ( (pixels[i] &0xff) > ( (median[i]  &0xff) - c_value)) ? object : backg;
        //imp.updateAndDraw();
    }

    public static void MidGrey(ImagePlus imp, int radius, int c_value, boolean doIwhite) {
        // See: Image Processing Learning Resourches HIPR2
        // http://homepages.inf.ed.ac.uk/rbf/HIPR2/adpthrsh.htm
        ImagePlus Maximp, Minimp;
        ImageProcessor ip=imp.getProcessor(), ipMax, ipMin;
        int mid_gray;
        byte object;
        byte backg;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Maximp=duplicateImage(ip);
        ipMax=Maximp.getProcessor();
        RankFilters rf=new RankFilters();
        rf.rank(ipMax, radius, rf.MAX);// Maximum
        //Maximp.show();
        Minimp=duplicateImage(ip);
        ipMin=Minimp.getProcessor();
        rf.rank(ipMin, radius, rf.MIN); //Minimum
        //Minimp.show();
        byte[] pixels = (byte [])ip.getPixels();
        byte[] max = (byte [])ipMax.getPixels();
        byte[] min = (byte [])ipMin.getPixels();

        for (int i=0; i<pixels.length; i++) {
            pixels[i] = ( (pixels[i] &0xff) > (((max[i]&0xff) +(min[i]&0xff))/2) - c_value ) ? object : backg;
        }
        //imp.updateAndDraw();
    }

    public static void Niblack(ImagePlus imp, int radius,  double k_value, int c_value, boolean doIwhite  ) {
        // Niblack recommends K_VALUE = -0.2 for images with black foreground
        // objects, and K_VALUE = +0.2 for images with white foreground objects.
        // Niblack W. (1986) "An introduction to Digital Image Processing" Prentice-Hall.
        // Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
        // This version uses a circular local window, instead of a rectagular one

        ImagePlus Meanimp, Varimp;
        ImageProcessor ip=imp.getProcessor(), ipMean, ipVar;

        byte object;
        byte backg ;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Meanimp=duplicateImage(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();

        ipMean=Meanimp.getProcessor();
        RankFilters rf=new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean
        //Meanimp.show();
        Varimp=duplicateImage(ip);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar=Varimp.getProcessor();
        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        //Varimp.show();
        byte[] pixels = (byte []) ip.getPixels();
        float[] mean = (float []) ipMean.getPixels();
        float[] var = (float []) ipVar.getPixels();

        for (int i=0; i<pixels.length; i++)
            pixels[i] = ( (pixels[i] &0xff) > (int)( mean[i] + k_value * Math.sqrt ( var[i] ) - c_value)) ? object : backg;
        //imp.updateAndDraw();
    }

    public static void Otsu(ImagePlus imp, int radius, boolean doIwhite) {
        // Otsu's threshold algorithm
        // M. Emre Celebi 6.15.2007, Fourier Library https://sourceforge.net/projects/fourier-ipal/
        // ported to ImageJ plugin by G.Landini. Same algorithm as in Auto_Threshold, this time for local circular regions

        int[] data;
        int w=imp.getWidth();
        int h=imp.getHeight();
        int position;
        int radiusx2=radius * 2;
        ImageProcessor ip=imp.getProcessor();
        byte[] pixels = (byte []) ip.getPixels();
        byte[] pixelsOut = new byte[pixels.length]; // need this to avoid changing the image data (and further histograms)
        byte object;
        byte backg;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        int ih, roiy, L=256; //L is for 8bit images.
        int threshold;
        int num_pixels;
        double total_mean;	/* mean gray-level for the whole image */
        double bcv, term;	/* between-class variance, scaling term */
        double max_bcv;		/* max BCV */
        double [] cnh = new  double [L];	/* cumulative normalized histogram */
        double [] mean = new  double [L];	/* mean gray-level */
        double [] histo = new  double [L];	/* normalized histogram */


        Roi roi = new OvalRoi(0, 0, radiusx2, radiusx2);
        //ip.setRoi(roi);
        for (int y =0; y<h; y++){
            IJ.showProgress((double)(y)/(h-1)); // this method is slow, so let's show the progress bar
            roiy = y-radius;
            for (int x = 0; x<w; x++){
                roi.setLocation(x-radius,roiy);
                ip.setRoi(roi);
                //ip.setRoi(new OvalRoi(x-radius, roiy, radiusx2, radiusx2));
                position=x+y*w;
                data = ip.getHistogram();

                //----
                /* Calculate total numbre of pixels */
                num_pixels=0;

                for ( ih = 0; ih < L; ih++ )
                    num_pixels=num_pixels+data[ih];

                term = 1.0 / ( double ) num_pixels;

                /* Calculate the normalized histogram */
                for ( ih = 0; ih < L; ih++ ) {
                    histo[ih] = term * data[ih];
                }

                /* Calculate the cumulative normalized histogram */
                cnh[0] = histo[0];
                for ( ih = 1; ih < L; ih++ ) {
                    cnh[ih] = cnh[ih - 1] + histo[ih];
                }

                mean[0] = 0.0;

                for (ih = 1; ih < L; ih++ ) {
                    mean[ih] = mean[ih - 1] + ih * histo[ih];
                }

                total_mean = mean[L-1];

                //	Calculate the BCV at each gray-level and find the threshold that maximizes it
                threshold = 0; //Integer.MIN_VALUE;
                max_bcv = 0.0;

                for ( ih = 0; ih < L; ih++ ) {
                    bcv = total_mean * cnh[ih] - mean[ih];
                    bcv *= bcv / ( cnh[ih] * ( 1.0 - cnh[ih] ) );

                    if ( max_bcv < bcv ) {
                        max_bcv = bcv;
                        threshold = ih;
                    }
                }
                pixelsOut[position] = ((pixels[position]& 0xff) >threshold || (pixels[position]& 0xff) ==255) ? object : backg;
            }
        }
        for (position=0; position<w*h; position++) pixels[position]=pixelsOut[position]; //update with thresholded pixels
    }

    public static void Phansalkar(ImagePlus imp, int radius,  double k_value, double r_value, double p_value, double q_value, boolean doIwhite) {
        // This is a modification of Sauvola's thresholding method to deal with low contrast images.
        // Phansalskar N. et al. Adaptive local thresholding for detection of nuclei in diversity stained
        // cytology images.International Conference on Communications and Signal Processing (ICCSP), 2011,
        // 218 - 220.
        // In this method, the threshold t = mean*(1+p*exp(-q*mean)+k*((stdev/r)-1))
        // Phansalkar recommends k = 0.25, r = 0.5, p = 2 and q = 10. In this plugin, k and r are the
        // parameters 1 and 2 respectively, but the values of p and q are fixed.
        //
        // Implemented from Phansalkar's paper description by G. Landini
        // This version uses a circular local window, instead of a rectagular one

        ImagePlus Meanimp, Varimp, Orimp;
        ImageProcessor ip=imp.getProcessor(), ipMean, ipVar, ipOri;
        byte object;
        byte backg;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Meanimp=duplicateImage(ip);
        ContrastEnhancer ce = new ContrastEnhancer();
        ce.setNormalize(true); // Needs to be true for correct normalization
        ce.stretchHistogram(Meanimp, 0.0);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();
        ipMean=Meanimp.getProcessor();
        ipMean.multiply(1.0/255);

        Orimp=duplicateImage(ip);
        ce.stretchHistogram(Orimp, 0.0);
        ic = new ImageConverter(Orimp);
        ic.convertToGray32();
        ipOri=Orimp.getProcessor();
        ipOri.multiply(1.0/255); //original to compare
        //Orimp.show();

        RankFilters rf=new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean

        //Meanimp.show();
        Varimp=duplicateImage(ip);
        ce.stretchHistogram(Varimp, 0.0);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar=Varimp.getProcessor();
        ipVar.multiply(1.0/255);

        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        ipVar.sqrt(); //SD

        //Varimp.show();
        byte[] pixels = (byte []) ip.getPixels();
        float[] ori = (float []) ipOri.getPixels();
        float[] mean = (float []) ipMean.getPixels();
        float[] sd = (float []) ipVar.getPixels();

        for (int i=0; i<pixels.length; i++)
            pixels[i] = ( (ori[i]) > ( mean[i] * (1.0 + p_value * Math.exp(-q_value * mean[i]) + k_value * (( sd[i] / r_value)- 1.0)))) ? object : backg;
        //imp.updateAndDraw();
    }

    public static void Sauvola(ImagePlus imp, int radius,  double k_value, double r_value, boolean doIwhite) {
        // Sauvola recommends K_VALUE = 0.5 and R_VALUE = 128.
        // This is a modification of Niblack's thresholding method.
        // Sauvola J. and Pietaksinen M. (2000) "Adaptive Document Image Binarization"
        // Pattern Recognition, 33(2): 225-236
        // http://www.ee.oulu.fi/mvg/publications/show_pdf.php?ID=24
        // Ported to ImageJ plugin from E Celebi's fourier_0.8 routines
        // This version uses a circular local window, instead of a rectagular one

        ImagePlus Meanimp, Varimp;
        ImageProcessor ip=imp.getProcessor(), ipMean, ipVar;
        byte object;
        byte backg;

        if (doIwhite){
            object =  (byte) 0xff;
            backg =   (byte) 0;
        }
        else {
            object =  (byte) 0;
            backg =  (byte) 0xff;
        }

        Meanimp=duplicateImage(ip);
        ImageConverter ic = new ImageConverter(Meanimp);
        ic.convertToGray32();

        ipMean=Meanimp.getProcessor();
        RankFilters rf=new RankFilters();
        rf.rank(ipMean, radius, rf.MEAN);// Mean
        //Meanimp.show();
        Varimp=duplicateImage(ip);
        ic = new ImageConverter(Varimp);
        ic.convertToGray32();
        ipVar=Varimp.getProcessor();
        rf.rank(ipVar, radius, rf.VARIANCE); //Variance
        //Varimp.show();
        byte[] pixels = (byte []) ip.getPixels();
        float[] mean = (float []) ipMean.getPixels();
        float[] var = (float []) ipVar.getPixels();

        for (int i=0; i<pixels.length; i++)
            pixels[i] = ( (pixels[i] &0xff) > (int)( mean[i] * (1.0+ k_value *(( Math.sqrt ( var[i] )/r_value)-1.0)))) ? object : backg;
        //imp.updateAndDraw();
    }


    public static ImagePlus duplicateImage(ImageProcessor iProcessor){
        int w=iProcessor.getWidth();
        int h=iProcessor.getHeight();
        ImagePlus iPlus= NewImage.createByteImage("Image", w, h, 1, NewImage.FILL_BLACK);
        ImageProcessor imageProcessor=iPlus.getProcessor();
        imageProcessor.copyBits(iProcessor, 0,0, Blitter.COPY);
        return iPlus;
    }

    public enum Method {
        Bernsen,
        Contrast,
        Mean,
        Median,
        MidGrey,
        Niblack,
        Otsu,
        Phansalkar,
        Sauvola
    }
}
