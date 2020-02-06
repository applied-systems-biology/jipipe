roiManager("reset");
if(excludeEdges==true){
    if(gatherResultsForEntireImageSet==true){
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display exclude summarize add");
    }
    else{
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display exclude clear summarize add");
    }
}
else{
    if(gatherResultsForEntireImageSet==true){
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display summarize add");
    }
    else{
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display clear summarize add");
    }
}

//Now rerun the particle finder on enlarged areas:
run("Invert");
run("Fill Holes");
for(i=0;i<enlargeRedMacrophages;i++){
    run("Dilate");
}
if(watershedOnMacrophages==true){
    run("Watershed");
}
if(excludeEdges==true){
    if(gatherResultsForEntireImageSet==true){
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display exclude summarize add");
    }
    else{
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display exclude clear summarize add");
    }
}
else{
    if(gatherResultsForEntireImageSet==true){
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display summarize add");
    }
    else{
        run("Analyze Particles...", "size=" + minMacrophageSize + "-" + maxMacrophageSize + " circularity=" + minMacrophageCircularity + "-" + maxMacrophageCircularity + " show=[Bare Outlines] display clear summarize add");
    }
}