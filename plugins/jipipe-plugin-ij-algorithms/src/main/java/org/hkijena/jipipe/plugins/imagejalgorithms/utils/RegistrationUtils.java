/*
 * Copyright by Zoltán Cseresnyés, Ruman Gerst
 *
 * Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
 * https://www.leibniz-hki.de/en/applied-systems-biology.html
 * HKI-Center for Systems Biology of Infection
 * Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
 * Adolf-Reichwein-Straße 23, 07745 Jena, Germany
 *
 * The project code is licensed under MIT.
 * See the LICENSE file provided with the code for the full license.
 */

package org.hkijena.jipipe.plugins.imagejalgorithms.utils;

import bunwarpj.Transformation;
import bunwarpj.bUnwarpJ_;
import bunwarpj.trakem2.transform.CubicBSplineTransform;
import ij.IJ;
import ij.ImagePlus;
import ij.io.FileSaver;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import mpicbg.ij.FeatureTransform;
import mpicbg.ij.SIFT;
import mpicbg.ij.util.Util;
import mpicbg.imagefeatures.Feature;
import mpicbg.imagefeatures.FloatArray2DSIFT;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;
import mpicbg.trakem2.transform.*;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.SIFTParameters;
import org.hkijena.jipipe.plugins.imagejalgorithms.nodes.registration.SimpleBUnwarpJParameters;
import register_virtual_stack.Register_Virtual_Stack_MT;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Port of {@link Register_Virtual_Stack_MT}
 */
public class RegistrationUtils {
    private RegistrationUtils() {

    }

    /**
     * Register two images with corresponding masks and
     * following the features and registration models.
     *
     * @param imp1         target image
     * @param imp2         source image (input and output)
     * @param imp1mask     target mask
     * @param imp2mask     source mask (input and output)
     * @param t            coordinate transform
     * @param commonBounds current common bounds of the registration space
     * @param bounds       list of bounds for the already registered images
     */
    public static void register(
            final ImagePlus imp1,
            final ImagePlus imp2,
            final ImagePlus imp1mask,
            final ImagePlus imp2mask,
            final CoordinateTransform t,
            final Rectangle commonBounds,
            final List<Rectangle> bounds,
            final SIFTParameters siftParameters,
            final bunwarpj.Param elasticParams,
            JIPipeProgressInfo progressInfo) throws Exception {

        // Extract SIFT features
        List<Feature> fs1 = extractFeatures(siftParameters, imp1.getProcessor());
        List<Feature> fs2 = extractFeatures(siftParameters, imp2.getProcessor());

        final java.util.List<PointMatch> candidates = new ArrayList<>();
        FeatureTransform.matchFeatures(fs2, fs1, candidates, Register_Virtual_Stack_MT.Param.rod);

        final java.util.List<PointMatch> inliers = new ArrayList<>();

        // Select features model
        Model<?> featuresModel;
        switch (Register_Virtual_Stack_MT.Param.featuresModelIndex) {
            case Register_Virtual_Stack_MT.TRANSLATION:
                featuresModel = new TranslationModel2D();
                break;
            case Register_Virtual_Stack_MT.RIGID:
                featuresModel = new RigidModel2D();
                break;
            case Register_Virtual_Stack_MT.SIMILARITY:
                featuresModel = new SimilarityModel2D();
                break;
            case Register_Virtual_Stack_MT.AFFINE:
                featuresModel = new AffineModel2D();
                break;
            default:
                throw new RuntimeException("ERROR: unknown featuresModelIndex = " + Register_Virtual_Stack_MT.Param.featuresModelIndex);
        }

        // Filter candidates into inliers
        try {
            featuresModel.filterRansac(
                    candidates,
                    inliers,
                    1000,
                    Register_Virtual_Stack_MT.Param.maxEpsilon,
                    Register_Virtual_Stack_MT.Param.minInlierRatio);
        } catch (NotEnoughDataPointsException e) {
            throw new RuntimeException("Unable to find model features!");
        }

        // Generate registered image, put it into imp2 and save it
        switch (Register_Virtual_Stack_MT.Param.registrationModelIndex) {
            case Register_Virtual_Stack_MT.TRANSLATION:
            case Register_Virtual_Stack_MT.SIMILARITY:
            case Register_Virtual_Stack_MT.RIGID:
            case Register_Virtual_Stack_MT.AFFINE:
                ((Model<?>) t).fit(inliers);
                break;
            case Register_Virtual_Stack_MT.ELASTIC:
                // set inliers as a PointRoi, and set masks
                //call_bUnwarpJ(...);
                //imp1.show();
                //imp2.show();
                final List<mpicbg.models.Point> sourcePoints = new ArrayList<>();
                final List<mpicbg.models.Point> targetPoints = new ArrayList<>();
                if (!inliers.isEmpty()) {
                    PointMatch.sourcePoints(inliers, sourcePoints);
                    PointMatch.targetPoints(inliers, targetPoints);

                    imp2.setRoi(Util.pointsToPointRoi(sourcePoints));
                    imp1.setRoi(Util.pointsToPointRoi(targetPoints));
                }

                //imp1.show();
                //ImagePlus aux = new ImagePlus("source", imp2.getProcessor().duplicate());
                //aux.setRoi( Util.pointsToPointRoi(targetPoints) );
                //aux.show();
                //if(imp1mask != null) new ImagePlus("mask", imp1mask).show();

                // Tweak initial affine transform based on the chosen model
                if (Register_Virtual_Stack_MT.Param.featuresModelIndex < Register_Virtual_Stack_MT.AFFINE) {
                    // Remove shearing
                    elasticParams.setShearCorrection(1.0);
                    // Remove anisotropy
                    elasticParams.setAnisotropyCorrection(1.0);
                    if (Register_Virtual_Stack_MT.Param.featuresModelIndex < Register_Virtual_Stack_MT.SIMILARITY) {
                        // Remove scaling
                        elasticParams.setScaleCorrection(1.0);
                    }
                }

                // Perform registration
                ImageProcessor mask1 = imp1mask.getProcessor() == null ? null : imp1mask.getProcessor();
                ImageProcessor mask2 = imp2mask.getProcessor() == null ? null : imp2mask.getProcessor();

                Transformation warp = bUnwarpJ_.computeTransformationBatch(imp2, imp1, mask2, mask1, elasticParams);

                // take the mask from the results
                //final ImagePlus output_ip = warp.getDirectResults();
                //output_ip.setTitle("result " + i);

                //imp2mask.setProcessor(imp2mask.getTitle(), output_ip.getStack().getProcessor(3));
                //output_ip.show();


                // Store result in a Cubic B-Spline transform
                ((CubicBSplineTransform) t).set(warp.getIntervals(), warp.getDirectDeformationCoefficientsX(), warp.getDirectDeformationCoefficientsY(),
                        imp2.getWidth(), imp2.getHeight());
                break;
            case Register_Virtual_Stack_MT.MOVING_LEAST_SQUARES:
                ((MovingLeastSquaresTransform) t).setModel(AffineModel2D.class);
                ((MovingLeastSquaresTransform) t).setAlpha(1); // smoothness
                ((MovingLeastSquaresTransform) t).setMatches(inliers);
                break;
        }

        // Calculate transform mesh
        TransformMesh mesh = new TransformMesh(t, 32, imp2.getWidth(), imp2.getHeight());
        TransformMeshMapping mapping = new TransformMeshMapping(mesh);

        // Create interpolated mask (only for elastic registration)
        if (Register_Virtual_Stack_MT.Param.registrationModelIndex == Register_Virtual_Stack_MT.ELASTIC) {
            imp2mask.setProcessor(imp2mask.getTitle(), new ByteProcessor(imp2.getWidth(), imp2.getHeight()));
            imp2mask.getProcessor().setValue(255);
            imp2mask.getProcessor().fill();
            final ImageProcessor ip2mask = Register_Virtual_Stack_MT.Param.interpolate ? mapping.createMappedImageInterpolated(imp2mask.getProcessor()) : mapping.createMappedImage(imp2mask.getProcessor());
            imp2mask.setProcessor(imp2mask.getTitle(), ip2mask);
        }
        // Create interpolated deformed image with black background
        imp2.getProcessor().setValue(0);
        final ImageProcessor ip2 = Register_Virtual_Stack_MT.Param.interpolate ? mapping.createMappedImageInterpolated(imp2.getProcessor()) : mapping.createMappedImage(imp2.getProcessor());
        imp2.setProcessor(imp2.getTitle(), ip2);

        // Accumulate bounding boxes, so in the end they can be reopened and re-saved with an enlarged canvas.
        final Rectangle currentBounds = mesh.getBoundingBox();
        final Rectangle previousBounds = bounds.get(bounds.size() - 1);
        currentBounds.x += previousBounds.x;
        currentBounds.y += previousBounds.y;
        bounds.add(currentBounds);

        //IJ.log(i + ": current bounding box = [" + currentBounds.x + " " + currentBounds.y + " " + currentBounds.width + " " + currentBounds.height + "]");

        // Update common bounds
        int min_x = commonBounds.x;
        int min_y = commonBounds.y;
        int max_x = commonBounds.x + commonBounds.width;
        int max_y = commonBounds.y + commonBounds.height;

        if (currentBounds.x < commonBounds.x)
            min_x = currentBounds.x;
        if (currentBounds.y < commonBounds.y)
            min_y = currentBounds.y;
        if (currentBounds.x + currentBounds.width > max_x)
            max_x = currentBounds.x + currentBounds.width;
        if (currentBounds.y + currentBounds.height > max_y)
            max_y = currentBounds.y + currentBounds.height;

        commonBounds.x = min_x;
        commonBounds.y = min_y;
        commonBounds.width = max_x - min_x;
        commonBounds.height = max_y - min_y;
    }

    private static ArrayList<Feature> extractFeatures(final SIFTParameters siftParameters, final ImageProcessor ip) {
        final ArrayList<Feature> fs = new ArrayList<>();
        new SIFT(new FloatArray2DSIFT(siftParameters.toParam())).extractFeatures(ip, fs);
        return fs;
    }

}
