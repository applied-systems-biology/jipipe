setAutoThreshold(thresholdMethodRedFluorescence + " dark");
setOption("BlackBackground", true);
run("Convert to Mask");
for(i=0;i<dilateErodeStepsForMacrophages;i++){
    run("Dilate");
}
run("Fill Holes");
for(i=0;i<dilateErodeStepsForMacrophages;i++){
    run("Erode");
}
if(gaussianSmoothingForMacrophages>=1){
    run("Gaussian Blur...", "sigma=" + gaussianSmoothingForMacrophages);
}
setAutoThreshold(thresholdMethodRedFluorescence + " dark");
setOption("BlackBackground", true);
run("Convert to Mask");
if(watershedOnMacrophages==true){
    run("Watershed");
}