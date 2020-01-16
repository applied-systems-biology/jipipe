package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.plugin.filter.GaussianBlur;

import ij.gui.*;
import ij.process.*; 
import ij.measure.*;
import ij.text.*;
import java.awt.*;

/** Threshold Regional Gradient for ImageJ,  by G.Landini, 16 Dec 2015 

Send any improvements or bugs to G.Landini@bham.ac.uk
Reference: Landini G, Randell DA, Fouad S, Galton A. Automatic thresholding from the gradients of region boundaries. Journal of Microscopy, 2016.

v1.0  14 May 2016 First release
*/


public class Threshold_Regional_Gradient implements PlugIn {
        /** Ask for parameters and then execute.*/
        public void run(String arg) {
		
		if (IJ.versionLessThan("1.50f")) return;

		// 1 - Obtain the currently active image if necessary:
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) {
			IJ.showMessage("Error", "No image!");
			return;
		}

                // 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Threshold Regional Gradient");
		gd.addMessage("Threshold Regional Gradient v1.0");
		gd.addMessage("See reference:\nLandini G, Randell DA, Fouad S, Galton A.\nAutomatic thresholding from the gradients of\nregion boundaries. Journal of Microscopy, 2016.");
;
		
		gd.addMessage("This procedure searches for dark objects (from black to white)");
		gd.addNumericField ("Circularity", 0.60, 2);
		gd.addNumericField ("Minimum area", 400, 0);
		gd.addNumericField ("Maximum area", 3600, 0);
		gd.addCheckbox("Fill_phase holes", true);
		gd.addCheckbox("Fill_detected boundaries",true);
		gd.addCheckbox("Display boundary gradients",false);
		
		String [] DisplayOption={"Fast", "Slow"};
		gd.addMessage("The fast method is about 3.5 times quicker, but requires more\nmemory to build a temporary stack.");
		gd.addChoice("Method", DisplayOption, DisplayOption[0]);
		
		gd.showDialog();
		if (gd.wasCanceled()) return
;
		// 3 - Retrieve parameters from the dialog
		double minCirc = (double) gd.getNextNumber();
		int minArea = (int) gd.getNextNumber();
		int maxArea = (int) gd.getNextNumber();
		boolean phasefill = gd.getNextBoolean();
		boolean fill = gd.getNextBoolean();
		boolean details = gd.getNextBoolean();
		String mode = gd.getNextChoice ();

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
		Object[] result = exec(imp, minCirc, minArea, maxArea, phasefill, fill, details, mode);

		// 5 - If all went well, show the image, not needed in this case
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
	 public Object[] exec(ImagePlus imp, double minCirc, int minArea, int maxArea, boolean phasefill, boolean fill, boolean details, String mode) {

		//long startTime = System.currentTimeMillis();
		ImageStatistics stats;
		stats = imp.getStatistics();

		int i, g, minGrey=0, maxGrey=255, nX, xs, ys, gravg;
		double ar, per, circ, fourPi = 4.0 * Math.PI;	
		
		while (stats.histogram[minGrey]==0) 
		        minGrey++;

		while (stats.histogram[maxGrey]==0) 
		        maxGrey--;

		//IJ.log("min: "+minGrey+" max: "+maxGrey);
		ImageProcessor ip3, ip4, ip5;

		//imp2 gradient
		ImagePlus imp2= new Duplicator().run(imp);
		IJ.run(imp2, "Find Edges", "");
		IJ.run(imp2, "Add...", "value=1");
		imp2.setTitle("_Edges");
		//imp2.show(); //gradients

		ImageCalculator ic = new ImageCalculator();
		
		ImagePlus imp3=null;// imp3 to be segmented
		ImagePlus imp4=null;
		
		Particles8_ p8 = new Particles8_();
		if (mode.equals("Fast")) {
			ImagePlus imp5 = IJ.createImage("regions", "8-bit white", imp.getWidth(), imp.getHeight(), 1);// imp5 to store the edges of regions
			imp3 = IJ.createImage("work", "8-bit white", imp.getWidth(), imp.getHeight(), 1);
			// faster but requires a stack holding all thresholds
			for (i =minGrey; i<=maxGrey; i++){
				IJ.showProgress((double)i/maxGrey);
				if (i>minGrey) IJ.run(imp5, "Add Slice", "");
				ic.run("Copy", imp3, imp);
				IJ.setRawThreshold(imp3, 0, i, null);
				IJ.run(imp3, "Convert to Mask", "");
				ic.run("Copy", imp5, imp3);
			}
			imp3.close();

			if (phasefill) IJ.run(imp5, "Fill Holes", "stack");
			IJ.run(imp5, "Convolve...", "text1=[0 -1 0\n-1 4 -1\n0 -1 0\n] stack");

			//imp2.show(); //gradients
			Object[] result = p8.exec(imp5, imp2, true, false, false, false, "Particles", false, 0,0, false, true);
			//the line above is faster than IJ.run(imp5, "Particles8 ", "white show=Particles overwrite redirect=_Edges");

			//imp2.hide();
			imp2.close();

			ResultsTable rt = ResultsTable.getResultsTable();
			nX=rt.getCounter();
		
			if (nX==0) IJ.log("Error: No regions found in the thresholded stack.");

			int slice, thisSlice=0;
			ip5 = imp5.getProcessor();
			FloodFiller ff = new FloodFiller(ip5);

			for (i=0; i<nX; i++) {    
				IJ.showProgress((double)i/nX);
				slice= (int) rt.getValue("Slice", i);
				if (slice!= thisSlice) {
					imp5.setSlice(slice);
					ip5 = imp5.getProcessor();
				}
		
				xs = (int) rt.getValue("XStart", i);
				ys =(int) rt.getValue("YStart", i);
				ar = rt.getValue("Area", i);
				per = rt.getValue("Perim", i);
				gravg =(int) rt.getValue("GrAverage", i);
				circ=0; // compute the circularity
				if (ar>0 && per>0)
					circ = fourPi*ar/(per*per);

				if (circ<minCirc || ar>maxArea || ar<minArea)
					ip5.setColor(0); // to be deleted
				else
					ip5.setColor(gravg); // to be labelled with the average gradient

				ff.fill8(xs, ys);
			}

			IJ.run(imp5, "Z Project...", "projection=[Max Intensity]");
			imp4 = WindowManager.getImage("MAX_regions"); // this image has the maximum of all region borders average gradients

			if (details)
				imp5.show();
		}

		else {
			// 3.5 times slower, but avoids creating the stack. so uses less RAM
			imp4= new Duplicator().run(imp); // imp4 to store the edges of regions
			imp4.setTitle("regions");

			if(details) imp4.show();

			ip4 = imp4.getProcessor();
			ip4.set(0);

			imp3=new Duplicator().run(imp);
			ip3=imp3.getProcessor();
			FloodFiller ff = new FloodFiller(ip3);
			for (g=minGrey; g<=maxGrey; g++){
				IJ.showProgress((double)-g/maxGrey);
				IJ.setRawThreshold(imp3, 0, g, null);
				IJ.run(imp3, "Convert to Mask", "");
				if (phasefill) IJ.run(imp3, "Fill Holes", "");
				IJ.run(imp3, "Convolve...", "text1=[0 -1 0\n-1 4 -1\n0 -1 0\n]");
				Object[] result = p8.exec(imp3, imp2, true, false, false, false, "Particles", false, 0,0, false, true);
				//the line above is faster than IJ.run(imp3, "Particles8 ", "white show=Particles overwrite redirect=_Edges");
				ResultsTable rt = ResultsTable.getResultsTable();
				nX=rt.getCounter();

				if (nX>0) {
					for (i=0; i<nX; i++) {    
						//IJ.showProgress((double)-i/nX);
						xs = (int) rt.getValue("XStart", i);
						ys =(int) rt.getValue("YStart", i);
						ar = rt.getValue("Area", i);
						per = rt.getValue("Perim", i);
						gravg =(int) rt.getValue("GrAverage", i);
			
						circ=0; // compute circularity
						if (ar>0 && per>0)
							circ = fourPi*ar/(per*per);

						if (circ<minCirc || ar>maxArea || ar<minArea)
							ip3.setColor(0); // to be deleted
						else
							ip3.setColor(gravg); // to be labelled with the average gradient
			
						ff.fill8(xs, ys);
			
					}
					ic.run("Max", imp4, imp3); //accumulate
				}
				ic.run("Copy", imp3, imp);
			}
			if (details)
				imp4.show();
			imp2.hide(); // edges image
			imp2.close();
			imp3.changes = false;
			imp3.close();
		}
	
		// get the 'regional maxima'
		IJ.run(imp4, "Domes ", "height=1");
		IJ.setRawThreshold(imp4, 1, 255, null);
		IJ.run(imp4, "Convert to Mask", "");

		if (fill)
			IJ.run(imp4, "Fill Holes", "");

		imp4.setTitle("Result");
		imp4.show();

		//startTime=System.currentTimeMillis()-startTime;
		//IJ.log("Processing time (sec.): " + startTime / 1000.0);
		return null;
	}
}
