package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*; 

/** Binary Kill Borders, by G.Landini. 25/May/2009 for ImageJ
Deletes particles touching the border of the image.
Send any improvements or bugs to G.Landini@bham.ac.uk
v1.0 release 25/May/2009
v1.1 wrong dialog title.
*/
public class BinaryKillBorders_ implements PlugIn {
	 /** Ask for parameters and then execute.*/
	public void run(String arg) {
		
		// 1 - Obtain the currently active image if necessary:
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;

		int stackSize = imp.getStackSize();
		if (imp.getBitDepth()!=8) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}
		ImageStatistics stats = imp.getStatistics();
		if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return;
		}

                // 2 - Ask for parameters:
		boolean whiteParticles =Prefs.blackBackground, killTop = true, killRight = true, killBottom, killLeft = true, connect4 = false, doIstack = false;
		GenericDialog gd = new GenericDialog("Binary Kill Borders");
		gd.addMessage("Binary Kill Borders v 1.1");
		gd.addMessage("Delete particles touching:");
		gd.addCheckbox("Top border", true);
		gd.addCheckbox("Right border", true);
		gd.addCheckbox("Bottom border", true);
		gd.addCheckbox("Left border", true);
		gd.addCheckbox("White particles on black background", whiteParticles);
		gd.addCheckbox("4 connected", connect4);
		if (stackSize>1) 
			gd.addCheckbox("Stack",false);

		gd.showDialog();
		if (gd.wasCanceled()) return
;
		// 3 - Retrieve parameters from the dialog
		killTop = gd.getNextBoolean();
		killRight = gd.getNextBoolean();
		killBottom = gd.getNextBoolean();
		killLeft = gd.getNextBoolean();
		whiteParticles = gd.getNextBoolean();
		connect4 = gd.getNextBoolean();
		if (stackSize>1) 
			doIstack = gd.getNextBoolean ();

		 // 4 - Execute!
		if (stackSize>1 && doIstack){
			for (int j=1; j<=stackSize; j++){
				imp.setSlice(j);
				Object[] result = exec(imp, killTop, killRight, killBottom, killLeft, whiteParticles, connect4);
			}
			imp.setSlice(1);
		}
		else {
			Object[] result = exec(imp, killTop, killRight, killBottom, killLeft, whiteParticles, connect4);
 		}
		// 5 - If all went well, show the image:
		imp.updateAndDraw();
	}
 
	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp, boolean  killTop, boolean killRight, boolean killBottom, boolean killLeft, boolean whiteParticles, boolean connect4) {

		// 0 - Check validity of parameters
		if (null == imp) return null;

		int width  = imp.getWidth();
		int height = imp.getHeight();
		int xem1 =width - 1;
		int yem1= height - 1;
		int i, offset;
		int foreground = 255, background = 0;
		ImageProcessor ip;

		IJ.showStatus("Kill Borders...");
		ip = imp.getProcessor();

		ip.snapshot(); //undo
		Undo.setup(Undo.FILTER, imp);

		byte[] pixels =(byte []) ip.getPixels();

		// 1 - Perform the magic
		if (!whiteParticles) { 
			foreground = 0;
			background = 255;
		}

		FloodFiller ff = new FloodFiller(ip);
		ip.setColor(background);

		if (connect4){
			 if (killTop) {
				 for (i=0;i<width;i++) 
					if ((int) (pixels[i] & 0xff) == foreground) ff.fill(i, 0);
			}
			if (killRight) {
				for (i=0;i<height;i++)
					if ((int) (pixels[xem1+ i * width] & 0xff) == foreground) ff.fill(xem1, i);
			}
			if (killBottom) {
				offset=yem1 * width;
				for (i=0;i<width;i++) 
					if ((int) (pixels[offset+i] & 0xff) == foreground) ff.fill(i, yem1);
			}
			if (killLeft) {
				for (i=0;i<height;i++)
					if ((int) (pixels[i * width] & 0xff) == foreground) ff.fill(0, i);
			}
		}
		else {
			 if (killTop) {
				 for (i=0;i<width;i++) 
					if ((int) (pixels[i] & 0xff) == foreground) ff.fill8(i, 0);
			}
			if (killRight) {
				for (i=0;i<height;i++)
					if ((int) (pixels[xem1+ i * width] & 0xff) == foreground) ff.fill8(xem1, i);
			}
			if (killBottom) {
				offset=yem1 * width;
				for (i=0;i<width;i++) 
					if ((int) (pixels[offset+i] & 0xff) == foreground) ff.fill8(i, yem1);
			}
			if (killLeft) {
				for (i=0;i<height;i++)
					if ((int) (pixels[i * width] & 0xff) == foreground) ff.fill8(0, i);
			}
		}
		return new Object[]{imp};
	}
}