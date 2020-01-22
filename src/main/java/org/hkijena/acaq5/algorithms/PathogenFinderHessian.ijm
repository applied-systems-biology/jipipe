//***************************************************************************************************************

//*** Pathogen  finder with Hessian starts: ***

if(labelledSpores == false) {
    selectWindow("TL");//not using illum correction
    run("Duplicate...", "title=Spores_TL");
    selectWindow("Spores_TL");
    run("FeatureJ Hessian", "largest absolute smoothing=" + smoothingHessianSpores);
    run("Morphological Filters", "operation=[Internal Gradient] element=Octagon radius=" + gradientRadiusHessianSpores);
    setAutoThreshold(thresholdMethodHessianSpores + " dark");
    run("Convert to Mask");
    run("Despeckle");
    run("Despeckle");
    rename("Pathogens_Hessian_Hough");
    run("Duplicate...", "title=Pathogens_Hessian_BrightSpot");
    run("Duplicate...", "title=Pathogens_Hessian_GuidedWatershed");

    //*** Here we use the Hough-filter to find circles in the thresholded image: ***
    if(segmentationMethodForUnlabeledPathogens=="Hough-filter"|| segmentationMethodForUnlabeledPathogens=="Hough and Watershed"){
        selectWindow("Pathogens_Hessian_Hough");
        run("Hough Circle Transform","minRadius=" + minimumRadiusHoughHessianSpores + ", maxRadius=" + maximumRadiusHoughHessianSpores + ", inc=" + incrementRadiusHoughHessianSpores + ", minCircles=" + minimumNumberHoughHessianSpores + ", maxCircles=" + maximumNumberHoughHessianSpores + ", threshold=" + thresholdHoughHessianSpores + ", resolution=" + resolutionHoughHessianSpores + ", ratio=" + ratioHoughHessianSpores + ", bandwidth=" + bandwidthRadiusHoughHessianSpores + ", local_radius=" + localradiusHoughHessianSpores + ",  reduce show_mask show_centroids show_scores results_table");
        while(!isOpen("Centroid map")){};
        wait(200);
        selectWindow("Centroid map");
        colorThresholdingHSB();
        selectWindow("Centroid map_colorThreshold");
        //run("Invert");//NOT needed when using colorThresholdingHSB()
        run("Open");
        run("Watershed");
        roiManager("reset");
        if(excludeEdges==true){
            if(gatherResultsForEntireImageSet==true){
                run("Analyze Particles...", "size=" + minSporeSize + "-" + maxSporeSize + " circularity=" + minSporeCircularity + "-" + maxSporeCircularity + " show=[Bare Outlines] display exclude summarize add");
            }
            else{
                run("Analyze Particles...", "size=" + minSporeSize + "-" + maxSporeSize + " circularity=" + minSporeCircularity + "-" + maxSporeCircularity + " show=[Bare Outlines] display exclude clear summarize add");
            }
        }
        else{
            if(gatherResultsForEntireImageSet==true){
                run("Analyze Particles...", "size=" + minSporeSize + "-" + maxSporeSize + " circularity=" + minSporeCircularity + "-" + maxSporeCircularity + " show=[Bare Outlines] display summarize add");
            }
            else{
                run("Analyze Particles...", "size=" + minSporeSize + "-" + maxSporeSize + " circularity=" + minSporeCircularity + "-" + maxSporeCircularity + " show=[Bare Outlines] display clear summarize add");
            }
        }

        run("Invert");
        run("Dilate");
        rename("Pathogens_Hessian_Hough_ROIs");
        run("Duplicate...", "title=Pathogens_Hessian_Hough_ROIs_copy");
        selectWindow("Pathogens_Hessian_Hough_ROIs");

        nROI_spores = roiManager("count");
        nROI_Hough_spores =  = roiManager("count");//save a copy for the ROI merging
        print("Number of Hessian-based pathogens found= " + nROI_spores);
        if(saveResults==true && nROI_spores>0){
                if(useImageFilename==true){
                    roiManager("save", dir + list[m] + "__Pathogens_ROIs.zip");
                }
                else{
                    roiManager("save",  dir + "/Image_" + testImageNumber + "__Pathogens_ROIs.zip");
                }
        }
    }