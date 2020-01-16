package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*; 

/** BinaryLabel, by G.Landini. 22/Oct/2003 for ImageJ

Labels binary objects.
Send any improvements or bugs to G.Landini@bham.ac.uk

v1.0 release

 To call this plugin from another without having to display the images use for example:
    (not tested!)

    BinaryLabel_ bl = new BinaryLabel_();
    Object[] result = br.exec(img1, "Labelled",  true, false );
    //parameters above are: mask ImagePlus, name, white particles, connect4 
    if (null != result) {
      String name = (String) result[0];
      ImagePlus recons = (ImagePlus) result[1];
      int particles= result[2];
    }

*/
public class BinaryLabel_ implements PlugIn {
        /** Ask for parameters and then execute.*/
        public void run(String arg) {
		
		if (IJ.versionLessThan("1.37f")) return;
		int[] wList = WindowManager.getIDList();

		// 1 - Obtain the currently active image if necessary:

                // 2 - Ask for parameters:
		boolean  whiteParticles =Prefs.blackBackground, connect4=false;
		GenericDialog gd = new GenericDialog("Binary Label");
		gd.addMessage("Binary Label v 1.0");
		gd.addCheckbox("White particles on black background", whiteParticles);
		gd.addCheckbox("4 connected", false);
		gd.showDialog();
		if (gd.wasCanceled()) return
;
		// 3 - Retrieve parameters from the dialog
		whiteParticles = gd.getNextBoolean();
		connect4 = gd.getNextBoolean();
		ImagePlus imp1 = WindowManager.getCurrentImage();

		//if (imp1.getStackSize()>1 || imp2.getStackSize()>1) {
		//	IJ.showMessage("Error", "Stacks not supported");
		//	return;
		//}
		if (imp1.getBitDepth()!=8 ) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}

		ImageStatistics stats;

		// Check images are binary. You must check this before calling the exec method.
		// 
		stats = imp1.getStatistics();
		if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount){
			IJ.error("8-bit binary mask image (0 and 255) required.");
			return;
		}

		String name = "Labelled";

		 // 4 - Execute!
		Object[] result = exec(imp1,  name,  whiteParticles, connect4);
 
		// 5 - If all went well, show the image:
		if (null != result) {
			ImagePlus resultImage = (ImagePlus) result[1];

		resultImage.show();
		//IJ.log(""+result[2]);
		}
	}
 
	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Warning!: This method does NOT check whether the two input images are binary, this is checked in the setup,
	*  so careful when calling this method from another plugin. Make sure both images are binary!!
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp1, String new_name, boolean whiteParticles, boolean connect4) {

		// 0 - Check validity of parameters
		if (null == imp1) return null;
 
		int width  = imp1.getWidth();
		int height = imp1.getHeight();
		int size = width * height;
		ImageProcessor  ip3;
		ImagePlus imp3;

		int x, y, offset, pointer, labelColour=1;
		int foreground = 255;//, background = 0;

		IJ.showStatus("Labelling...");

		// 1 - Perform the magic
		imp3 = new ImagePlus(new_name,imp1.getProcessor().duplicate());
		ip3 = imp3.getProcessor();

		if (!whiteParticles) 
			ip3.invert();

		ImageConverter ic = new ImageConverter(imp3);
		ic.convertToGray32();
		ip3 = imp3.getProcessor();
		ip3.multiply(-1);
		float[] pixel =(float []) ip3.getPixels();

		FloodFiller ff = new FloodFiller(ip3);

		if (connect4){
			for(y=0; y<height; y++) {
				offset=y*width;
				for(x=0; x<width; x++){
					pointer = offset + x;
					if ( pixel[pointer]  == -foreground) {
						ip3.setColor(labelColour++);
						ff.fill(x, y);
					}
				}
			}
		}
		else { //8
			for(y=0; y<height; y++) {
				offset=y*width;
				for(x=0; x<width; x++){
					pointer = offset + x;
					if ( pixel[pointer]  == -foreground) {
						ip3.setColor(labelColour++);
						ff.fill8(x, y);
					}
				}
			}
		}

		imp3.updateAndDraw();
		labelColour --;
		// 2 - Return the new name and the image
		return new Object[]{new_name, imp3, labelColour};
        }
}