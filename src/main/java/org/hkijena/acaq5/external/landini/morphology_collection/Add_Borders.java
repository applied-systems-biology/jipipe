package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*; 

/** Add Borders, by G.Landini. 18/Apr/2010 for ImageJ
Adds borders to an the image.
Send any improvements or bugs to G.Landini@bham.ac.uk
v1.0 release 18/Apr/2010
v1.1 release 24/Apr/2010

Adds borders in 255 for 8, 16 and 32 bits, and in colour (255,255,255) for RGB images) 
If "white" is not checked it draws the frame with value 0 (or (0,0,0) for RGB)

*/
public class Add_Borders implements PlugIn {
	 /** Ask for parameters and then execute.*/
	public void run(String arg) {
		
		// 1 - Obtain the currently active image if necessary:
		ImagePlus imp = IJ.getImage();
		if (null == imp) return;

		int stackSize = imp.getStackSize();

                // 2 - Ask for parameters:
		boolean whiteParticles =Prefs.blackBackground, addTop = true, addRight = true, addBottom, addLeft = true, doIstack = false;
		GenericDialog gd = new GenericDialog("Add Borders");
		gd.addMessage("Add Borders v 1.1");
		gd.addCheckbox("Top border", true);
		gd.addCheckbox("Right border", true);
		gd.addCheckbox("Bottom border", true);
		gd.addCheckbox("Left border", true);
		gd.addCheckbox("White borders", whiteParticles);
		if (stackSize>1) 
			gd.addCheckbox("Stack",false);

		gd.showDialog();
		if (gd.wasCanceled()) return
;
		// 3 - Retrieve parameters from the dialog
		addTop = gd.getNextBoolean();
		addRight = gd.getNextBoolean();
		addBottom = gd.getNextBoolean();
		addLeft = gd.getNextBoolean();
		whiteParticles = gd.getNextBoolean();
		if (stackSize>1) 
			doIstack = gd.getNextBoolean ();

		 // 4 - Execute!
		if (stackSize>1 && doIstack){
			for (int j=1; j<=stackSize; j++){
				imp.setSlice(j);
				Object[] result = exec(imp, addTop, addRight, addBottom, addLeft, whiteParticles);
			}
			imp.setSlice(1);
		}
		else {
			Object[] result = exec(imp, addTop, addRight, addBottom, addLeft, whiteParticles);
			}
		// 5 - If all went well, show the image:
		imp.updateAndDraw();
	}
 
	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp, boolean  addTop, boolean addRight, boolean addBottom, boolean addLeft, boolean whiteParticles) {

		// 0 - Check validity of parameters
		if (null == imp) return null;

		int width  = imp.getWidth();
		int height = imp.getHeight();
		int xem1 =width - 1;
		int yem1= height - 1;
		int i, offset;
		ImageProcessor ip;
		IJ.showStatus("Add Borders...");
		ip = imp.getProcessor();

		ip.snapshot(); //undo
		Undo.setup(Undo.FILTER, imp);

		// 1 - Perform the magic
		if (imp.getBitDepth()==8) {
			byte[] pixels =(byte []) ip.getPixels();
			byte foreground = (byte) 0xff;
			if (!whiteParticles)  
				foreground =  (byte) 0;

			 if (addTop) {
				 for (i=0;i<width;i++) 
					pixels[i] =  foreground;
			}
			if (addRight) {
				for (i=0;i<height;i++)
					pixels[xem1+ i * width] = foreground;
			}
			if (addBottom) {
				offset=yem1 * width;
				for (i=0;i<width;i++) 
					pixels[offset+i] = foreground;
			}
			if (addLeft) {
				for (i=0;i<height;i++)
					pixels[i * width] = foreground;
			}
		}
		else if (imp.getBitDepth()==16) {
			short [] pixels =(short []) ip.getPixels();
			short  foreground =   255;
			if (!whiteParticles) 
				foreground =  0;

			 if (addTop) {
				 for (i=0;i<width;i++) 
					pixels[i] =  foreground;
			}
			if (addRight) {
				for (i=0;i<height;i++)
					pixels[xem1+ i * width] = foreground;
			}
			if (addBottom) {
				offset=yem1 * width;
				for (i=0;i<width;i++) 
					pixels[offset+i] = foreground;
			}
			if (addLeft) {
				for (i=0;i<height;i++)
					pixels[i * width] = foreground;
			}
		}
		else if (imp.getBitDepth()==24) {
			int[] pixels =(int []) ip.getPixels();
			int  foreground =   (255 << 16) + (255 << 8) + 255;
			if (!whiteParticles) 
				foreground =  0;

			 if (addTop) {
				 for (i=0;i<width;i++) 
					pixels[i] = foreground;
			}
			if (addRight) {
				for (i=0;i<height;i++)
					pixels[xem1+ i * width] = foreground;
			}
			if (addBottom) {
				offset=yem1 * width;
				for (i=0;i<width;i++) 
					pixels[offset+i] = foreground;
			}
			if (addLeft) {
				for (i=0;i<height;i++)
					pixels[i * width] = foreground;
			}
		}
		else if (imp.getBitDepth()==32) {
			float[] pixels =(float []) ip.getPixels();
			float  foreground =   255;
			if (!whiteParticles) 
				foreground =  0;

			 if (addTop) {
				 for (i=0;i<width;i++) 
					pixels[i] = foreground;
			}
			if (addRight) {
				for (i=0;i<height;i++)
					pixels[xem1+ i * width] = foreground;
			}
			if (addBottom) {
				offset=yem1 * width;
				for (i=0;i<width;i++) 
					pixels[offset+i] = foreground;
			}
			if (addLeft) {
				for (i=0;i<height;i++)
					pixels[i * width] = foreground;
			}
		}
		return new Object[]{imp};
	}
}