//*** Here we apply only the Bright Spot method, where we use the center of each pathogen as marker for watershed: ***
if(segmentationMethodForUnlabeledPathogens=="Guided Watershed"){
    selectWindow("Pathogens_Hessian_GuidedWatershed");
    run("Duplicate...", "title=Pathogens_Hessian_GuidedWatershed_copy");
    selectWindow("Pathogens_Hessian_GuidedWatershed");
    /*run("Duplicate...", "title=Mask");
    selectWindow("Mask");
    run("Gaussian Blur...", "sigma=3");
    run("Invert");
    setAutoThreshold("Otsu");
    setOption("BlackBackground", true);
    run("Convert to Mask");
    run("Dilate");
    run("Dilate");
    run("Fill Holes");
    run("Divide...", "value=255");
    imageCalculator("Multiply create", "Pathogens_Hessian_GuidedWatershed","Mask");
    run("Erode");
    run("Remove Outliers...", "radius=3 threshold=50 which=Bright");
    run("Analyze Particles...", "size=3-300 circularity=0.10-1.00 show=[Bare Outlines] display exclude clear summarize add");
    selectWindow("Drawing of Result of Pathogens_Hessian_GuidedWatershed");
    run("Invert");
    run("Fill Holes");
    rename("Seeds");*/

    selectWindow("Pathogens_Hessian_GuidedWatershed_copy");
    run("Invert");
        //run("Analyze Particles...", "size=20-3000 circularity=0.10-1.00 show=[Bare Outlines] display exclude clear summarize add");
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
    /*selectWindow("Drawing of Pathogens_Hessian_GuidedWatershed_copy");
    run("Invert");
    run("Fill Holes");
    run("Gaussian Blur...", "sigma=3");
    setAutoThreshold("Otsu");
    //run("Threshold...");
    run("Convert to Mask");

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
    //selectWindow("Drawing of Drawing of Pathogens_Hessian_GuidedWatershed_copy");*/
    run("Invert");
    run("Dilate");
    rename("Pathogens_Hessian_GuidedWatershed_ROIs");
    run("Duplicate...", "title=Pathogens_Hessian_GuidedWatershed_ROIs_copy");
    selectWindow("Pathogens_Hessian_GuidedWatershed_ROIs");

    nROI_GuidedWatershed_spores = roiManager("count");
    print("Number of Hessian-based Guided Watershed pathogens found= " + nROI_GuidedWatershed_spores);
    nROI_spores = roiManager("count");	//refresh and save the total ROI number, now the Guided Watershed pathogens
    if(saveResults==true && nROI_GuidedWatershed_spores>0){
        if(useImageFilename==true){
            roiManager("save", dir + list[m] + "__Pathogens_GuidedWatershed_ROIs.zip");
        }
        else{
            roiManager("save",  dir + "/Image_" + testImageNumber + "__Pathogens_GuidedWatershed_ROIs.zip");
        }
    }
}