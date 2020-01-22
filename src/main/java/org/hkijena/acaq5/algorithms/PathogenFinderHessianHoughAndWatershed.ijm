//*** Here we apply the Bright Spot method to the Hessian pathogens image, then we merge the two ROIs: ***
if(segmentationMethodForUnlabeledPathogens=="Hough and Watershed"){
    selectWindow("Pathogens_Hessian_BrightSpot");
    run("Invert");
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

    rename("Pathogens_Hessian_BrightSpot_ROIs");
    run("Duplicate...", "title=Pathogens_Hessian_BrightSpot_ROIs_copy");
    selectWindow("Pathogens_Hessian_BrightSpot_ROIs");

    nROI_BrightSpots_spores = roiManager("count");
    print("Number of Hessian-based Bright Spots pathogens found= " + nROI_BrightSpots_spores);
    if(saveResults==true && nROI_BrightSpots_spores>0){
            if(useImageFilename==true){
                roiManager("save", dir + list[m] + "__Pathogens_BrightSpots_ROIs.zip");
            }
            else{
                roiManager("save",  dir + "/Image_" + testImageNumber + "__Pathogens_BrightSpots_ROIs.zip");
            }
    }
    //*** End of Bright Spot pathogen finder ***
    //*** Merging Hough and Bright Spot ROIs if such option selected: ***
    roiManager("reset");
    if(nROI_Hough_spores>0){
        if(useImageFilename==true){
            roiManager("open", dir + list[m] + "__Pathogens_ROIs.zip");
        }
        else{
            roiManager("open",  dir + "/Image_" + testImageNumber + "__Pathogens_ROIs.zip");
        }

        run("Select All");
        if(nROI_Hough_spores>=2){
            roiManager("Combine");
        }
        roiManager("Add");	//now the merged ROI is in position "nROI_spores" of the ROI array
    }

    if(nROI_BrightSpots_spores>0){
        if(useImageFilename==true){
            roiManager("open", dir + list[m] + "__Pathogens_ROIs_BrightSpots.zip");
        }
        else{
            roiManager("open",  dir + "/Image_" + testImageNumber + "__Pathogens_ROIs_BrightSpots.zip");
        }
        GreenSporesAlreadyCounted = newArray(nROI_BrightSpots_spores);
        GreenSporesAlreadyCountedCounter = 0;
        for (ibrightspot=nROI_Hough_spores+1; ibrightspot<nROI_BrightSpots_spores+nROI_Hough_spores+1; ibrightspot++) {
            roiManager("Select", newArray(ibrightspot,nROI_Hough_spores));
            roiManager("AND");
            if(selectionType != (-1)){ //already counted
                GreenSporesAlreadyCounted[GreenSporesAlreadyCountedCounter]=ibrightspot;
                GreenSporesAlreadyCountedCounter++;
            }
        }

        if(GreenSporesAlreadyCountedCounter>0){
            roiManager("Select", GreenSporesAlreadyCounted);
            if(GreenSporesAlreadyCountedCounter >= 2){ //Combine only works with >=2 ROIs
                roiManager("Combine");
            }
            roiManager("Delete");
        }
        roiManager("Select", nROI_Hough_spores);
        roiManager("Delete");	//delete the merged original Hough ROIs
        nROI_spores = roiManager("count");	//refresh and save the total ROI number, now including Bright Spot pathogens
        print("New total pathogen count after Bright Spots added = " + nROI_spores);
        if(saveResults==true && nROI_spores>0){
            if(useImageFilename==true){
                roiManager("save", dir + list[m] + "__AllPathogens_ROIs.zip");
            }
            else{
                roiManager("save",  dir + "/Image_" + testImageNumber + "__AllPathogens_ROIs.zip");
            }
        }
    }
    //*** End of merging Hough-filter and Bright Spot ROIs ***
}