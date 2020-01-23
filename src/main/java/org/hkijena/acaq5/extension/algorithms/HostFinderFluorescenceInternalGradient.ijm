// Offer choice between Internal Gradient (to find objects with a hole inside) and simple bright spot analysis:
if(internalGradientRadiusRed >= 1){
    if(fastModeCLAHE){
        CLAHEmessage =  "blocksize=" + CLAHEblocks + " histogram=" + CLAHEbins + " maximum=" + CLAHEslope2FlscMacrophages + " mask=*None*  fast_(less_accurate)";
    } else {
        CLAHEmessage =  "blocksize=" + CLAHEblocks + " histogram=" + CLAHEbins + " maximum=" + CLAHEslope2FlscMacrophages + " mask=*None*";
    }
    run("Enhance Local Contrast (CLAHE)",CLAHEmessage);
    run("Gaussian Blur...", "sigma=" + gaussianSmoothingForRedMacrophages);
    run("8-bit");
    run("Morphological Filters", "operation=[Internal Gradient] element=Octagon radius=" + internalGradientRadiusRed);
    run("Enhance Local Contrast (CLAHE)",CLAHEmessage);
    setAutoThreshold(thresholdMethodRedFluorescence + " dark");
    setOption("BlackBackground", true);
    run("Convert to Mask");
    run("Dilate");
    run("Fill Holes");
    run("Dilate");
    run("Dilate");
    run("Dilate");
    run("Fill Holes");
    run("Erode");
    run("Erode");
    if(watershedOnMacrophages==true){
        run("Watershed");
    }
    for(i=0;i<additionalErodeStepsForLabelledMacrophages;i++){
        run("Erode");
    }
}