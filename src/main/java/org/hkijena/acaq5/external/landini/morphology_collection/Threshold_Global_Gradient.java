package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.plugin.filter.GaussianBlur;

import ij.gui.*;
import ij.process.*; 
import ij.measure.*;
import ij.text.*;
import java.awt.*;

/** Threshold_Gradients_Global for ImageJ,  by G.Landini, 16 Dec 2015

Defines the threshold value as the grey value at which thresholded regions
exhibit the gratest total boundary gradient or average boundary gradient
Send any improvements or bugs to G.Landini@bham.ac.uk

v1.0  14 May 2016 First release
*/


public class Threshold_Global_Gradient implements PlugIn {
        /** Ask for parameters and then execute.*/
        public void run(String arg) {
		
		if (IJ.versionLessThan("1.50f")) return;

		// 1 - Obtain the currently active image if necessary:
		ImagePlus imp = WindowManager.getCurrentImage();

                // 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Threshold Global Gradients");
		gd.addMessage("Threshold Global Gradient v1.0");
		gd.addMessage("See reference:\nLandini G, Randell DA, Fouad S, Galton A.\nAutomatic thresholding from the gradients of\nregion boundaries. Journal of Microscopy, 2016.");
;		
		String [] DisplayOption={"Mean", "Total"};
		gd.addChoice("Gradient", DisplayOption, DisplayOption[0]);

		gd.addCheckbox("Binarise",true);
		gd.addCheckbox("White objects",false);
		gd.addCheckbox("Display values",false);
//		gd.addCheckbox("Show plot", false);

		
		gd.showDialog();
		if (gd.wasCanceled()) return;
		// 3 - Retrieve parameters from the dialog
		String mode = gd.getNextChoice ();
		boolean binarise = gd.getNextBoolean();
		boolean white = gd.getNextBoolean();
		
		boolean details = gd.getNextBoolean();
		boolean show=false;// = gd.getNextBoolean();
		
		if (imp.getStackSize()>1) {
			IJ.showMessage("Error", "Stacks not supported");
			return;
		}
		if (imp.getBitDepth()!=8 ) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}

		if (imp.isInvertedLut() ) {
			IJ.showMessage("Error", "No inverted LUTs, please");
			return;
		}


		 // 4 - Execute!
		Object[] result = exec(imp, mode, binarise, white, details, show);

		// 5 - If all went well, show the image:
		//if (null != result) {
		//	 IJ.log("Result : "+result[0]);
		//	ImagePlus resultImage = (ImagePlus) result[1];
		//	resultImage.show();
		//}
	}
 

	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Warning!: This method does NOT check whether the two input images are binary, this is checked in the setup,
	*  so careful when calling this method from another plugin. Make sure both images are binary!!
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp, String mode, boolean binarise, boolean white, boolean details,boolean show  ) {

		long startTime = System.currentTimeMillis();

		ImageProcessor ip, ip3;
		ip=imp.getProcessor();
		
		int i, g, minGrey=0, maxGrey=255;
		int nBins=256, nPixels, m, j;

		int[] threshold = new int[nBins];
		int[] wGrad = new int[nBins];
		double[] wMGrad= new double[nBins];      
		int[] histogram= new int[nBins];  

		histogram=ip.getHistogram();
		
		while (histogram[minGrey]==0) 
			minGrey++;

		while (histogram[maxGrey]==0) 
		        maxGrey--;
       
		//IJ.log("min: "+minGrey+" max: "+maxGrey);
	

		//imp2 gradient
		ImagePlus imp2= new Duplicator().run(imp);
		IJ.run(imp2, "Find Edges", "");

		ImageCalculator ic = new ImageCalculator();

		ImagePlus imp3=IJ.createImage("__1", "8-bit white", imp.getWidth(), imp.getHeight(), 1);// imp5 to store the edges of regions
		ImagePlus imp6=IJ.createImage("__2", "8-bit white", imp.getWidth(), imp.getHeight(), 1);// imp5 to store the edges of regions
		ip3=imp3.getProcessor();
		
		for (i =minGrey; i<maxGrey; i++){
			ic.run("Copy ", imp3, imp); 
			IJ.setRawThreshold(imp3, 0, i, null);
			IJ.run(imp3, "Convert to Mask", "");
			ic.run("Copy ", imp6, imp3);
			IJ.run(imp6, "Minimum...", "radius=0.5");
			ic.run("Subtract ", imp3, imp6); //beucher gradient
			histogram=ip3.getHistogram();
			nPixels=histogram[255];
			//IJ.log("Edgepixels["+i+"]:"+nPixels);
			ic.run("And", imp3, imp2); 
			histogram=ip3.getHistogram();
			wGrad[i]=0;
			wMGrad[i]=0.0;
			m=0;
			if (nPixels>0){
				for(j=1;j<nBins;j++) 
					m=m+histogram[j]*j;
				wGrad[i] = m;
				wMGrad[i] = (double) m /(double) nPixels;
			}
			//IJ.log("WGrad["+i+"]:"+wGrad[i]+" wMGrad["+i+"]:"+wMGrad[i]);
		}
		imp3.close();
		imp6.close();

		// compute threshold value
		int myThreshold=-1;
		int myMThreshold=-1;

		maxGrey=-1;
		double maxMGrey=-1;

		// find maximum
		for (i=0;i<nBins;i++) {
			threshold[i]=i;
			if (wGrad[i]>maxGrey) {
				maxGrey = wGrad[i];
				myThreshold = i;
			}
			if (wMGrad[i]>maxMGrey) {
				maxMGrey = wMGrad[i];
				myMThreshold = i;
			}
		}

		
		if (mode.equals("Mean")){
			if (white) 
				IJ.setRawThreshold(imp, myMThreshold+1, 255, null);
			else
				IJ.setRawThreshold(imp, 0, myMThreshold, null);

			if (binarise) {
				IJ.run(imp, "Convert to Mask", "");
				IJ.resetThreshold(imp);
			}
			if (details) IJ.log("Mean gradient threshold: "+myMThreshold+" time: " + (System.currentTimeMillis()-startTime) / 1000.0);

		}
		else {
			if (white) 
				IJ.setRawThreshold(imp, myThreshold+1, 255, null);
			else
				IJ.setRawThreshold(imp, 0, myThreshold, null);
			if (binarise) {
				IJ.run(imp, "Convert to Mask", "");
				IJ.resetThreshold(imp);
			}
			if (details) IJ.log("Total gradient threshold: "+myThreshold+" time: " + (System.currentTimeMillis()-startTime) / 1000.0);
		}
	
		return null;
	}
}
