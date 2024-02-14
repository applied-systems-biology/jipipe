package org.hkijena.jipipe.extensions.ijtrackmate.nodes.spots;

import fiji.plugin.trackmate.Model;
import fiji.plugin.trackmate.Settings;
import fiji.plugin.trackmate.TrackMate;
import ij.ImagePlus;
import org.hkijena.jipipe.api.JIPipeDocumentation;
import org.hkijena.jipipe.api.JIPipeNode;
import org.hkijena.jipipe.api.JIPipeProgressInfo;
import org.hkijena.jipipe.api.nodes.*;
import org.hkijena.jipipe.api.nodes.categories.ImagesNodeTypeCategory;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeIterationContext;
import org.hkijena.jipipe.api.nodes.iterationstep.JIPipeSingleIterationStep;
import org.hkijena.jipipe.api.nodes.algorithm.JIPipeIteratingAlgorithm;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntry;
import org.hkijena.jipipe.api.validation.JIPipeValidationReportEntryLevel;
import org.hkijena.jipipe.api.validation.JIPipeValidationRuntimeException;
import org.hkijena.jipipe.api.validation.contexts.GraphNodeValidationReportContext;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotDetectorData;
import org.hkijena.jipipe.extensions.ijtrackmate.datatypes.SpotsCollectionData;
import org.hkijena.jipipe.extensions.ijtrackmate.utils.JIPipeLogger;
import org.hkijena.jipipe.extensions.imagejdatatypes.datatypes.ImagePlusData;

@JIPipeDocumentation(name = "Detect spots", description = "Detect spots using TrackMate")
@JIPipeNode(nodeTypeCategory = ImagesNodeTypeCategory.class, menuPath = "Tracking")
@JIPipeInputSlot(value = ImagePlusData.class, slotName = "Image", description = "The image where the spots should be detected", autoCreate = true)
@JIPipeInputSlot(value = SpotDetectorData.class, slotName = "Spot detector", description = "The algorithm that detects the spots", autoCreate = true)
@JIPipeOutputSlot(value = SpotsCollectionData.class, slotName = "Spots", description = "The detected spots", autoCreate = true)
public class SpotDetectorNode extends JIPipeIteratingAlgorithm {

    private int numThreads = 1;

    public SpotDetectorNode(JIPipeNodeInfo info) {
        super(info);
    }

    public SpotDetectorNode(SpotDetectorNode other) {
        super(other);
        this.numThreads = other.numThreads;
    }

    @Override
    protected void runIteration(JIPipeSingleIterationStep iterationStep, JIPipeIterationContext iterationContext, JIPipeGraphNodeRunContext runContext, JIPipeProgressInfo progressInfo) {
        ImagePlus image = iterationStep.getInputData("Image", ImagePlusData.class, progressInfo).getImage();
        SpotDetectorData spotDetectorData = iterationStep.getInputData("Spot detector", SpotDetectorData.class, progressInfo);

        Model model = new Model();
        model.setLogger(new JIPipeLogger(progressInfo.resolve("TrackMate")));

        Settings settings = new Settings(image);
        settings.detectorFactory = spotDetectorData.getSpotDetectorFactory().copy();
        settings.detectorSettings = spotDetectorData.getSettings();
        settings.addAllAnalyzers();

        TrackMate trackMate = new TrackMate(model, settings);

//        if(!trackMate.checkInput()) {
//            progressInfo.log(trackMate.getErrorMessage());
//            throw new UserFriendlyRuntimeException(trackMate.getErrorMessage(),
//                    "TrackMate: Invalid input",
//                    getDisplayName(),
//                    "TrackMate detected an invalid input",
//                    "Please check the parameters");
//        }

        if (!trackMate.process()) {
            progressInfo.log(trackMate.getErrorMessage());
            throw new JIPipeValidationRuntimeException(new JIPipeValidationReportEntry(JIPipeValidationReportEntryLevel.Error,
                    new GraphNodeValidationReportContext(this),
                    "TrackMate: Error while processing",
                    "TrackMate could not successfully process the data",
                    trackMate.getErrorMessage()));
        }

        iterationStep.addOutputData(getFirstOutputSlot(), new SpotsCollectionData(model, settings, image), progressInfo);
    }

    @Override
    public boolean supportsParallelization() {
        return true;
    }

    @Override
    public int getParallelizationBatchSize() {
        return numThreads;
    }

//    public void execDetection(Model model, Settings settings, JIPipeProgressInfo progressInfo) {
//        final Logger logger = model.getLogger();
//
//        final SpotDetectorFactoryBase< ? > factory = settings.detectorFactory;
//        if ( factory instanceof ManualDetectorFactory )
//        {
//          throw new UnsupportedOperationException("Manual detector is not supported");
//        }
//
//        /*
//         * Prepare interval
//         */
//        final ImgPlus img = TMUtils.rawWraps( settings.imp );
//
//        if ( !factory.setTarget( img, settings.detectorSettings ) )
//        {
//            throw new RuntimeException(factory.getErrorMessage());
//        }
//
//        /*
//         * Separate frame-by-frame or global detection depending on the factory
//         * type.
//         */
//
//        if ( factory instanceof SpotGlobalDetectorFactory )
//        {
//            processGlobal( ( SpotGlobalDetectorFactory ) factory, img, logger, settings, model, progressInfo );
//        }
//        else if ( factory instanceof SpotDetectorFactory )
//        {
//            processFrameByFrame( ( SpotDetectorFactory ) factory, img, logger, settings, model, progressInfo );
//        }
//
//        throw new RuntimeException("Don't know how to handle detector factory of type: " + factory.getClass());
//    }

//    private void processGlobal(final SpotGlobalDetectorFactory factory, final ImgPlus img, final Logger logger, Settings settings, Model model, JIPipeProgressInfo progressInfo)
//    {
//        final Interval interval = TMUtils.getIntervalWithTime( img, settings );
//
//        // To translate spots, later
//        final double[] calibration = TMUtils.getSpatialCalibration( settings.imp );
//
//        final SpotGlobalDetector< ? > detector = factory.getDetector( interval );
//        if ( detector instanceof MultiThreaded)
//        {
//            final MultiThreaded md = ( MultiThreaded ) detector;
//            md.setNumThreads( numThreads );
//        }
//
////        if ( detector instanceof Cancelable)
////            cancelables.add( ( Cancelable ) detector );
//
//        // Execute detection
//        logger.setStatus( "Detection..." );
//        if ( detector.checkInput() && detector.process() )
//        {
//            final SpotCollection rawSpots = detector.getResult();
//            rawSpots.setNumThreads( numThreads );
//
//            /*
//             * Filter out spots not in the ROI.
//             */
//            final SpotCollection spots;
//            final Roi roi = settings.getRoi();
//            if ( roi != null )
//            {
//                spots = new SpotCollection();
//                spots.setNumThreads( numThreads );
//                for ( int frame = settings.tstart; frame <= settings.tend; frame++ )
//                {
//                    final List<Spot> spotsThisFrame = new ArrayList<>();
//                    final Iterable< Spot > spotsIt = rawSpots.iterable( frame, false );
//                    if ( spotsIt == null )
//                        continue;
//
//                    for ( final Spot spot : spotsIt )
//                    {
//                        if ( roi.contains(
//                                ( int ) Math.round( spot.getFeature( Spot.POSITION_X ) / calibration[ 0 ] ),
//                                ( int ) Math.round( spot.getFeature( Spot.POSITION_Y ) / calibration[ 1 ] ) ) )
//                        {
//                            spotsThisFrame.add( spot );
//                        }
//                    }
//                    spots.put( frame, spotsThisFrame );
//                }
//            }
//            else
//            {
//                spots = rawSpots;
//            }
//
//            // Add detection feature other than position
//            for ( final Spot spot : spots.iterable( false ) )
//                spot.putFeature( Spot.POSITION_T, spot.getFeature( Spot.FRAME ) * settings.dt );
//
//            model.setSpots( spots, true );
//            logger.setStatus( "" );
//            if ( progressInfo.isCancelled() )
//                logger.log( "Detection canceled. Reason:\n" + progressInfo.getCancelReason() + "\n" );
//            logger.log( "Found " + spots.getNSpots( false ) + " spots.\n" );
//        }
//        else
//        {
//            // Fail: exit and report error.
//            throw new RuntimeException(detector.getErrorMessage());
//        }
//    }
//
//    @SuppressWarnings( "rawtypes" )
//    private boolean processFrameByFrame(final SpotDetectorFactory factory, final ImgPlus img, final Logger logger, Settings settings, Model model, JIPipeProgressInfo progressInfo)
//    {
//        final Interval interval = TMUtils.getInterval( img, settings );
//        final int zindex = img.dimensionIndex( Axes.Z );
//        final int numFrames = settings.tend - settings.tstart + 1;
//        // Final results holder, for all frames
//        final SpotCollection spots = new SpotCollection();
//        spots.setNumThreads( numThreads );
//        // To report progress
//        final AtomicInteger spotFound = new AtomicInteger( 0 );
//        final AtomicInteger progress = new AtomicInteger( 0 );
//        // To translate spots, later
//        final double[] calibration = TMUtils.getSpatialCalibration( settings.imp );
//
//        /*
//         * Fine tune multi-threading: If we have 10 threads and 15 frames to
//         * process, we process 10 frames at once, and allocate 1 thread per
//         * frame. But if we have 10 threads and 2 frames, we process the 2
//         * frames at once, and allocate 5 threads per frame if we can.
//         */
//        final int nSimultaneousFrames = ( factory.forbidMultithreading() )
//                ? 1
//                : Math.min( numThreads, numFrames );
//        final int threadsPerFrame = Math.max( 1, numThreads / nSimultaneousFrames );
//
//        logger.log( "Detection processes "
//                + ( ( nSimultaneousFrames > 1 ) ? ( nSimultaneousFrames + " frames" ) : "1 frame" )
//                + " simultaneously and allocates "
//                + ( ( threadsPerFrame > 1 ) ? ( threadsPerFrame + " threads" ) : "1 thread" )
//                + " per frame.\n" );
//
//        final ExecutorService executorService = Executors.newFixedThreadPool( nSimultaneousFrames );
//        final List<Future< Boolean >> tasks = new ArrayList<>( numFrames );
//        for ( int i = settings.tstart; i <= settings.tend; i++ )
//        {
//            final int frame = i;
//            final Callable< Boolean > callable = new Callable< Boolean >()
//            {
//
//                @Override
//                public Boolean call() throws Exception
//                {
//                    if ( progressInfo.isCanceled() )
//                        return Boolean.TRUE; // ok to be canceled.
//
//                    // Yield detector for target frame
//                    final SpotDetector< ? > detector = factory.getDetector( interval, frame );
//                    if ( detector instanceof MultiThreaded )
//                    {
//                        final MultiThreaded md = ( MultiThreaded ) detector;
//                        md.setNumThreads( threadsPerFrame );
//                    }
//
////                    if ( detector instanceof Cancelable )
////                        progressInfo.cancelables.add( ( Cancelable ) detector );
//
//                    // Execute detection
//                    if ( detector.checkInput() && detector.process() )
//                    {
//                        // On success, get results.
//                        final List< Spot > spotsThisFrame = detector.getResult();
//
//                        /*
//                         * Special case: if we have a single column image, then
//                         * the detectors internally dealt with a single line
//                         * image. We need to permute back the X & Y coordinates
//                         * if it's the case.
//                         */
//                        if ( img.dimension( 0 ) < 2 && zindex < 0 )
//                        {
//                            for ( final Spot spot : spotsThisFrame )
//                            {
//                                spot.putFeature( Spot.POSITION_Y, spot.getDoublePosition( 0 ) );
//                                spot.putFeature( Spot.POSITION_X, 0d );
//                            }
//                        }
//
//                        List< Spot > prunedSpots;
//                        final Roi roi = settings.getRoi();
//                        if ( roi != null )
//                        {
//                            prunedSpots = new ArrayList<>();
//                            for ( final Spot spot : spotsThisFrame )
//                            {
//                                if ( roi.contains(
//                                        ( int ) Math.round( spot.getFeature( Spot.POSITION_X ) / calibration[ 0 ] ),
//                                        ( int ) Math.round( spot.getFeature( Spot.POSITION_Y ) / calibration[ 1 ] ) ) )
//                                    prunedSpots.add( spot );
//                            }
//                        }
//                        else
//                        {
//                            prunedSpots = spotsThisFrame;
//                        }
//                        // Add detection feature other than position
//                        for ( final Spot spot : prunedSpots )
//                        {
//                            // FRAME will be set upon adding to
//                            // SpotCollection.
//                            spot.putFeature( Spot.POSITION_T, frame * settings.dt );
//                        }
//                        // Store final results for this frame
//                        spots.put( frame, prunedSpots );
//                        // Report
//                        spotFound.addAndGet( prunedSpots.size() );
//                        logger.setProgress( progress.incrementAndGet() / ( double ) numFrames );
//
//                    }
//                    else
//                    {
//                        // Fail: exit and report error.
//                        throw new RuntimeException(detector.getErrorMessage());
////                        return Boolean.FALSE;
//                    }
//                    return Boolean.TRUE;
//                }
//            };
//            final Future< Boolean > task = executorService.submit( callable );
//            tasks.add( task );
//        }
//        logger.setStatus( "Detection..." );
//        logger.setProgress( 0 );
//
//        final AtomicBoolean reportOk = new AtomicBoolean( true );
//        try
//        {
//            for ( final Future< Boolean > task : tasks )
//            {
//                final Boolean ok = task.get();
//                if ( !ok )
//                {
//                    reportOk.set( false );
//                    break;
//                }
//            }
//        }
//        catch (InterruptedException | ExecutionException e )
//        {
//            throw new RuntimeException("Problem during detection: " + e.getMessage());
////            reportOk.set( false );
////            e.printStackTrace();
//        }
//
//        model.setSpots( spots, true );
//
//        if ( reportOk.get() )
//        {
//            if ( progressInfo.isCanceled() )
//                logger.log( "Detection canceled after " + ( progress.get() + 1 ) + " frames. Reason:\n" + progressInfo.getCancelReason() + "\n" );
//            logger.log( "Found " + spotFound.get() + " spots.\n" );
//        }
//        else
//        {
//            logger.error( "Detection failed after " + progress.get() + " frames:\n");
//            logger.log( "Found " + spotFound.get() + " spots prior failure.\n" );
//        }
//        logger.setProgress( 1 );
//        logger.setStatus( "" );
//        return reportOk.get();
//    }
}
