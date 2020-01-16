package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
/**
Greyscale reconstruction, by G.Landini. 30/Oct/2003 for ImageJ

Reconstructs a greyscale image (the "mask" image) based on a "seed"
image.
This is an implementation of the parallel algorithm from:

Vincent L. Morphological greyscale reconstuction in Image Analysis:
Applications and efficient algorithms. IEEE Trans Image Proc 2(2)
176-201, 1993.

It is imperative to read Vincent's paper to understand greyscale
reconstruction and its applications.

The reconstruction algorithm is: iterated geodesic dilations of the
seed UNDER the mask image until stability is reached (the idempotent limit).

Send any improvements or bugs to G.Landini@bham.ac.uk
This plugin is based on Calculator_Plus by Wayne Rasband (wayne@codon.nih.gov)

Tested on 1.39q

v1.1 20/Jan/2008 
Supports stacks reconstructed by stacks (i.e. a stack of masks and a stack of seeds

v2.0 22/12/2008 Rewrite following the guidelines at http://pacific.mpi-cbg.de/wiki/index.php/PlugIn_Design_Guidelines
     This version computes the result differently. It binary reconstructs the thresholded mask by the thresholded seed
     and keeps the maximum greylevel at which the reconstruction was done.

  Apart from being inmensely faster, now it can be called from another plugin without having to show the images. 
  It cannot process stacks anymore, but its use was very limited anyway.


v2.1 29/9/2010 The basins were returned inverted
*/

public class Domes_ implements PlugIn {
        /** Ask for parameters and then execute.*/
        public void run(String arg) {

		// 1 - Obtain the currently active image if necessary:
		ImagePlus imp = IJ.getImage();
                // 2 - Ask for parameters:
		
		boolean createWindow = true;

		GenericDialog gd = new GenericDialog("Domes");
		gd.addMessage("Morphological domes");
		gd.addMessage("Domes or h-convex transform are bright regions\nof certain height from the top of the greyscale function.");
		gd.addNumericField ("Height", 40, 0);
		gd.addCheckbox("Basins (dark regions, or h-concave transform)",false);
		gd.showDialog();
		if (gd.wasCanceled()) return
;
		// 3 - Retrieve parameters from the dialog

		int domeHeight = (int) gd.getNextNumber();
		boolean darkDomes = gd.getNextBoolean();

		if (imp.getStackSize()>1) {
			IJ.showMessage("Error", "Stacks not supported");
			return;
		}
		if (imp.getBitDepth()!=8) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}
 
		String name = "Domes";

		 // 4 - Execute!
		long start = System.currentTimeMillis();
		Object[] result = exec(imp, name, domeHeight, darkDomes);

		// 5 - If all went well, show the image:
		if (null != result) {
			ImagePlus resultImage = (ImagePlus) result[1];
			resultImage.show();
		}IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");
	}
	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp, String new_name, int domeHeight, boolean darkDomes) {


		// 0 - Check validity of parameters
		if (null == imp) return null;
		if (null == new_name) new_name = imp.getTitle();

		if(darkDomes)
			imp.getProcessor().invert();

		ImageProcessor ip=imp.getProcessor().duplicate();

		ImagePlus imp2 = new ImagePlus("_seed",ip);
		ImageProcessor ip2=imp2.getProcessor();

		ImagePlus imp3=null;

		ip2.add(-domeHeight);
		imp2.updateAndDraw();

		GreyscaleReconstruct_ gr = new GreyscaleReconstruct_();
		Object[] result = gr.exec(imp, imp2, null, false, false); // the result is returned in imp2 if createWindow is false
		//ImagePlus imp1, ImagePlus imp2, String new_name, boolean createWindow, boolean connect4)
		if (null != result) 
			imp3 = (ImagePlus) result[1];

		ImageCalculator ic = new ImageCalculator();
		ic.calculate("Subtract", imp, imp3); // the result is returned in imp2 if createWindow is false

		imp.updateAndDraw();
		return new Object[]{new_name, imp};
	}
}




