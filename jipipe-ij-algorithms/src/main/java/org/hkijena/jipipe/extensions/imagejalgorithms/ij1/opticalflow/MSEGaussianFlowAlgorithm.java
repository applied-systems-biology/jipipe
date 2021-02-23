package org.hkijena.jipipe.extensions.imagejalgorithms.ij1.opticalflow;

import ij.CompositeImage;
import ij.ImagePlus;
import ij.ImageStack;
import ij.plugin.filter.GaussianBlur;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeOrganization;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.parameters.JIPipeParameter;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.d3.greyscale.ImagePlus3DGreyscale32FData;
import org.hkijena.jipipe.extensions.imagejdatatypes.util.ImageJUtils;
import org.hkijena.jipipe.utils.ImageJCalibrationMode;

/**
 * Adapted from {@link mpicbg.ij.plugin.MSEGaussianFlow}, as the methods there are all protected/private
 */
@JIPipeDocumentation(name = "Optical flow (Gaussian Window MSE)", description = "Transfers image sequences into an optic flow field.\n" +
        "Flow fields are calculated for each pair (t,t+1) of the sequence with length |T| independently. " +
        "The motion vector for each pixel in image t is estimated by searching the most similar looking pixel in image t+1. " +
        "The similarity measure is the sum of differences of all pixels in a local vicinity. The local vicinity is defined by a Gaussian. " +
        "Both the standard deviation of the Gaussian (the size of the local vicinity) and the search radius are parameters of the method.\n\n" +
        "The output is a two-channel image with (T-1) items. The pixels in each channel describe the relative location" +
        " of the next similar pixel in polar coordinates (default) or cartesian coordinates.")
@JIPipeOrganization(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Optical flow")
@JIPipeInputSlot(value = ImagePlus3DGreyscale32FData.class, slotName = "Input", autoCreate = true)
@JIPipeOutputSlot(value = ImagePlus3DGreyscale32FData.class, slotName = "Vector field", autoCreate = true)
public class MSEGaussianFlowAlgorithm extends JIPipeSimpleIteratingAlgorithm {

    private static final GaussianBlur GAUSSIAN_BLUR = new GaussianBlur();
    private int sigma = 4;
    private int maxDistance = 7;
    private boolean outputPolarCoordinates = true;
    private boolean relativeDistances = true;

    public MSEGaussianFlowAlgorithm(JIPipeNodeInfo info) {
        super(info);
    }

    public MSEGaussianFlowAlgorithm(MSEGaussianFlowAlgorithm other) {
        super(other);
        this.sigma = other.sigma;
        this.maxDistance = other.maxDistance;
        this.outputPolarCoordinates = other.outputPolarCoordinates;
        this.relativeDistances = other.relativeDistances;
    }

    @JIPipeDocumentation(name = "Relative distances", description = "If enabled, the output radius or x/y are relative to the max distance.")
    @JIPipeParameter("relative-distances")
    public boolean isRelativeDistances() {
        return relativeDistances;
    }

    @JIPipeParameter("relative-distances")
    public void setRelativeDistances(boolean relativeDistances) {
        this.relativeDistances = relativeDistances;
    }

    @JIPipeDocumentation(name = "Sigma", description = "Determines the local vicinity that is used to calculate the similarity of two pixels.")
    @JIPipeParameter("sigma")
    public int getSigma() {
        return sigma;
    }

    @JIPipeParameter("sigma")
    public boolean setBlockRadius(int blockRadius) {
        if(blockRadius <= 0)
            return false;
        this.sigma = blockRadius;
        return true;
    }

    @JIPipeDocumentation(name = "Max distance", description = "Maximum search distance for a most similar pixel. The maximum value is 127 due to performance reasons.")
    @JIPipeParameter("max-distance")
    public int getMaxDistance() {
        return maxDistance;
    }

    @JIPipeParameter("max-distance")
    public boolean setMaxDistance(int maxDistance) {
        if(maxDistance <= 0 || maxDistance >= 127)
            return false;
        this.maxDistance = maxDistance;
        return true;
    }

    @JIPipeDocumentation(name = "Output polar coordinates", description = "If enabled, the output contains polar coordinates (channel 0 being the radius and channel 1 being phi). " +
            "If disabled, the output contains cartesian coordinates.")
    @JIPipeParameter("output-polar-coordinates")
    public boolean isOutputPolarCoordinates() {
        return outputPolarCoordinates;
    }

    @JIPipeParameter("output-polar-coordinates")
    public void setOutputPolarCoordinates(boolean outputPolarCoordinates) {
        this.outputPolarCoordinates = outputPolarCoordinates;
    }

    @Override
    protected void runIteration(JIPipeDataBatch dataBatch, JIPipeProgressInfo progressInfo) {
        ImagePlus imp = dataBatch.getInputData(getFirstInputSlot(), ImagePlus3DGreyscale32FData.class, progressInfo).getImage();
        ImageStack seq = imp.getStack();
        ImageStack seqFlowVectors = new ImageStack( imp.getWidth(), imp.getHeight(), 2 * seq.getSize() - 2 );

        FloatProcessor ip1;
        FloatProcessor ip2 = ( FloatProcessor )seq.getProcessor( 1 ).convertToFloat();

        CompositeImage impFlowVectors = null;

        for ( int i = 1; i < seq.getSize(); ++i )
        {
            progressInfo.resolveAndLog("Slice", i, seq.getSize());
            ip1 = ip2;
            ip2 = ( FloatProcessor )seq.getProcessor( i + 1 ).convertToFloat();

            final FloatProcessor seqFlowVectorRSlice = new FloatProcessor( imp.getWidth(), imp.getHeight() );
            final FloatProcessor seqFlowVectorPhiSlice = new FloatProcessor( imp.getWidth(), imp.getHeight() );

            opticFlow( ip1, ip2, seqFlowVectorRSlice, seqFlowVectorPhiSlice );

            seqFlowVectors.setPixels( seqFlowVectorRSlice.getPixels(), 2 * i - 1 );
            seqFlowVectors.setSliceLabel( "r " + i, 2 * i - 1 );
            seqFlowVectors.setPixels( seqFlowVectorPhiSlice.getPixels(), 2 * i );
            seqFlowVectors.setSliceLabel( "phi " + i, 2 * i );

            if ( i == 1 )
            {
                final ImagePlus notYetComposite = new ImagePlus( imp.getTitle() + " flow vectors", seqFlowVectors );
                notYetComposite.setOpenAsHyperStack( true );
                notYetComposite.setCalibration( imp.getCalibration() );
                notYetComposite.setDimensions( 2, 1, seq.getSize() - 1 );

                impFlowVectors = new CompositeImage( notYetComposite, CompositeImage.GRAYSCALE );
                impFlowVectors.setOpenAsHyperStack( true );
                impFlowVectors.setDimensions( 2, 1, seq.getSize() - 1 );

                if(outputPolarCoordinates) {
                    impFlowVectors.setPosition(1, 1, 1);
                    impFlowVectors.setDisplayRange(0, 1);
                    impFlowVectors.setPosition(2, 1, 1);
                    impFlowVectors.setDisplayRange(-Math.PI, Math.PI);
                }
            }
            impFlowVectors.setPosition( 1, 1, i );
            imp.setSlice( i + 1 );
        }

        if(!outputPolarCoordinates) {
            ImageJUtils.calibrate(impFlowVectors, ImageJCalibrationMode.AutomaticImageJ, 0, 0);
        }

        dataBatch.addOutputData(getFirstOutputSlot(), new ImagePlus3DGreyscale32FData(impFlowVectors), progressInfo);
    }

    public void opticFlow(
            final FloatProcessor ip1,
            final FloatProcessor ip2,
            final FloatProcessor r,
            final FloatProcessor phi)
    {
        final ByteProcessor ipX = new ByteProcessor( ip1.getWidth(), ip1.getHeight() );
        final ByteProcessor ipY = new ByteProcessor( ip1.getWidth(), ip1.getHeight() );
        final FloatProcessor ipD = new FloatProcessor( ip1.getWidth(), ip1.getHeight() );
        final FloatProcessor ipDMin = new FloatProcessor( ip1.getWidth(), ip1.getHeight() );

        final float[] ipDMinInitPixels = ( float[] )ipDMin.getPixels();
        for ( int i = 0; i < ipDMinInitPixels.length; ++i )
            ipDMinInitPixels[ i ] = Float.MAX_VALUE;

        for ( byte yo = ( byte )-maxDistance; yo <= maxDistance; ++yo )
        {
            for ( byte xo = ( byte )-maxDistance; xo <= maxDistance; ++xo )
            {
                // continue if radius is larger than maxDistance
                if ( yo * yo + xo * xo > maxDistance * maxDistance ) continue;

                subtractShifted( ip1, ip2, ipD, xo, yo );

                // blur in order to compare small regions instead of single pixels
                GAUSSIAN_BLUR.blurFloat( ipD, sigma, sigma, 0.002 );

                final float[] ipDPixels = ( float[] )ipD.getPixels();
                final float[] ipDMinPixels = ( float[] )ipDMin.getPixels();
                final byte[] ipXPixels = ( byte[] )ipX.getPixels();
                final byte[] ipYPixels = ( byte[] )ipY.getPixels();

                // update the translation fields
                for ( int i = 0; i < ipDPixels.length; ++i )
                {
                    if ( ipDPixels[ i ] < ipDMinPixels[ i ] )
                    {
                        ipDMinPixels[ i ] = ipDPixels[ i ];
                        ipXPixels[ i ] = xo;
                        ipYPixels[ i ] = yo;
                    }
                }
            }
        }

        if(outputPolarCoordinates) {
            algebraicToPolar(
                    (byte[]) ipX.getPixels(),
                    (byte[]) ipY.getPixels(),
                    (float[]) r.getPixels(),
                    (float[]) phi.getPixels(),
                    relativeDistances ? maxDistance : 1.0);
        }
        else {
            algebraicToCartesian(
                    (byte[]) ipX.getPixels(),
                    (byte[]) ipY.getPixels(),
                    (float[]) r.getPixels(),
                    (float[]) phi.getPixels(),
                    relativeDistances ? maxDistance : 1.0);
        }
    }

    private void algebraicToPolar(
            final byte[] ipXPixels,
            final byte[] ipYPixels,
            final float[] ipRPixels,
            final float[] ipPhiPixels,
            final double max )
    {
        final int n = ipXPixels.length;
        for ( int i = 0; i < n; ++i )
        {
            final double x = ipXPixels[ i ] / max;
            final double y = ipYPixels[ i ] / max;

            final double r = Math.sqrt( x * x + y * y );
            final double phi = Math.atan2( x / r, y / r );

            ipRPixels[ i ] = ( float )r;
            ipPhiPixels[ i ] = ( float )phi;
        }
    }

    private void algebraicToCartesian(
            final byte[] ipXPixels,
            final byte[] ipYPixels,
            final float[] ipCXPixels,
            final float[] ipCYhiPixels,
            final double max )
    {
        final int n = ipXPixels.length;
        for ( int i = 0; i < n; ++i )
        {
            final double x = ipXPixels[ i ] / max;
            final double y = ipYPixels[ i ] / max;

            ipCXPixels[i] = (float)x;
            ipCYhiPixels[i] = (float)y;
        }
    }

    private void subtractShifted(
            final FloatProcessor a,
            final FloatProcessor b,
            final FloatProcessor c,
            final int xo,
            final int yo )
    {
        final float[] af = ( float[] )a.getPixels();
        final float[] bf = ( float[] )b.getPixels();
        final float[] cf = ( float[] )c.getPixels();

        final int w = a.getWidth();
        final int h = a.getHeight();

        for ( int y = 0; y < h; ++y )
        {
            int yb = y + yo;
            if ( yb < 0 || yb >= h )
                yb = pingPong( yb, h - 1 );
            final int yAdd = y * w;
            final int ybAdd = yb * w;

            for ( int x = 0; x < a.getWidth(); ++x )
            {
                int xb = x + xo;
                if ( xb < 0 || xb >= w )
                    xb = pingPong( xb, w - 1 );

                final int i = yAdd + x;
                final float d = bf[ ybAdd + xb ] - af[ i ];
                cf[ i ] = d * d;
            }
        }
    }

    private int pingPong( final int a, final int mod )
    {
        int x = a;
        final int p = 2 * mod;
        if ( x < 0 ) x = -x;
        if ( x >= mod )
        {
            if ( x <= p )
                x = p - x;
            else
            {
                /* catches mod == 1 to no additional cost */
                try
                {
                    x %= p;
                    if ( x >= mod )
                        x = p - x;
                }
                catch ( ArithmeticException e ){ x = 0; }
            }
        }
        return x;
    }
}
