package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;

//Greyscale Erode by Gabriel Landini, G.Landini@bham.ac.uk

public class GreyscaleErode_ implements PlugInFilter {
	protected boolean doIwhite;
	protected int iterations;

	public int setup(String arg, ImagePlus imp) {

		if (arg.equals("about"))
			{showAbout(); return DONE;}
		GenericDialog gd = new GenericDialog("GreyscaleErode", IJ.getInstance());
		gd.addMessage("Greyscale Erosion (3x3)");
		gd.addNumericField ("Iterations", 1, 0);
		gd.addCheckbox("White foreground",false);

		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;
		iterations = (int) gd.getNextNumber();
		doIwhite = gd.getNextBoolean ();
		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y, cp, i;
		int [][] pixel = new int [xe][ye];
		int [][] pixel2 = new int [xe][ye];
		//original converted to white particles
		if (doIwhite==false){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y,255-ip.getPixel(x,y));
			}
		}
		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel2[x][y]=ip.getPixel(x,y);//original
			}
		}

		for(i=0;i<iterations;i++){ //image has changed
			IJ.showStatus("Eroding: "+ (i+1));
			for (x=1; x<xe-1; x++) {
				for (y=1; y<ye-1; y++) {
					//white Erode of image except borders
					cp=255;
					if (pixel2[x-1][y-1]<cp) cp=pixel2[x-1][y-1];
					if (pixel2[x  ][y-1]<cp) cp=pixel2[x  ][y-1];
					if (pixel2[x+1][y-1]<cp) cp=pixel2[x+1][y-1];
					if (pixel2[x-1][y  ]<cp) cp=pixel2[x-1][y  ];
					if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
					if (pixel2[x+1][y  ]<cp) cp=pixel2[x+1][y  ];
					if (pixel2[x-1][y+1]<cp) cp=pixel2[x-1][y+1];
					if (pixel2[x  ][y+1]<cp) cp=pixel2[x  ][y+1];
					if (pixel2[x+1][y+1]<cp) cp=pixel2[x+1][y+1];
					pixel[x][y]=cp;
				}
			}

			y=0;
			for (x=1; x<xe-1; x++) {
				//white Erode upper row
				cp=255;
				if (pixel2[x-1][y  ]<cp) cp=pixel2[x-1][y  ];
				if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
				if (pixel2[x+1][y  ]<cp) cp=pixel2[x+1][y  ];
				if (pixel2[x-1][y+1]<cp) cp=pixel2[x-1][y+1];
				if (pixel2[x  ][y+1]<cp) cp=pixel2[x  ][y+1];
				if (pixel2[x+1][y+1]<cp) cp=pixel2[x+1][y+1];
				pixel[x][y]=cp;
			}

			y=ye-1;
			for (x=1; x<xe-1; x++) {
				//white Erode lower row
				cp=255;
				if (pixel2[x-1][y-1]<cp) cp=pixel2[x-1][y-1];
				if (pixel2[x  ][y-1]<cp) cp=pixel2[x  ][y-1];
				if (pixel2[x+1][y-1]<cp) cp=pixel2[x+1][y-1];
				if (pixel2[x-1][y  ]<cp) cp=pixel2[x-1][y  ];
				if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
				if (pixel2[x+1][y  ]<cp) cp=pixel2[x+1][y  ];
				pixel[x][y]=cp;
			}

			x=0;
			for (y=1; y<ye-1; y++) {
				//white Erode left column
				cp=255;
				if (pixel2[x  ][y-1]<cp) cp=pixel2[x  ][y-1];
				if (pixel2[x+1][y-1]<cp) cp=pixel2[x+1][y-1];
				if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
				if (pixel2[x+1][y  ]<cp) cp=pixel2[x+1][y  ];
				if (pixel2[x  ][y+1]<cp) cp=pixel2[x  ][y+1];
				if (pixel2[x+1][y+1]<cp) cp=pixel2[x+1][y+1];
				pixel[x][y]=cp;
			}

			x=xe-1;
			for (y=1; y<ye-1; y++) {
				//white Erode right column
				cp=255;
				if (pixel2[x-1][y-1]<cp) cp=pixel2[x-1][y-1];
				if (pixel2[x  ][y-1]<cp) cp=pixel2[x  ][y-1];
				if (pixel2[x-1][y  ]<cp) cp=pixel2[x-1][y  ];
				if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
				if (pixel2[x-1][y+1]<cp) cp=pixel2[x-1][y+1];
				if (pixel2[x  ][y+1]<cp) cp=pixel2[x  ][y+1];
				pixel[x][y]=cp;
			}

			x=0; //upper left corner
			y=0;
			cp=255;
			if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
			if (pixel2[x+1][y  ]<cp) cp=pixel2[x+1][y  ];
			if (pixel2[x  ][y+1]<cp) cp=pixel2[x  ][y+1];
			if (pixel2[x+1][y+1]<cp) cp=pixel2[x+1][y+1];
			pixel[x][y]=cp;

			x=xe-1; //upper right corner
			//y=0;
			cp=255;
			if (pixel2[x-1][y  ]<cp) cp=pixel2[x-1][y  ];
			if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
			if (pixel2[x-1][y+1]<cp) cp=pixel2[x-1][y+1];
			if (pixel2[x  ][y+1]<cp) cp=pixel2[x  ][y+1];
			pixel[x][y]=cp;

			x=0; //lower left corner
			y=ye-1;
			cp=255;
			if (pixel2[x  ][y-1]<cp) cp=pixel2[x  ][y-1];
			if (pixel2[x+1][y-1]<cp) cp=pixel2[x+1][y-1];
			if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
			if (pixel2[x+1][y  ]<cp) cp=pixel2[x+1][y  ];
			pixel[x][y]=cp;

			x=xe-1; //lower right corner
			y=ye-1;
			cp=255;
			if (pixel2[x-1][y-1]<cp) cp=pixel2[x-1][y-1];
			if (pixel2[x  ][y-1]<cp) cp=pixel2[x  ][y-1];
			if (pixel2[x-1][y  ]<cp) cp=pixel2[x-1][y  ];
			if (pixel2[x  ][y  ]<cp) cp=pixel2[x  ][y  ];
			pixel[x][y]=cp;

			//min of mask and Eroded seed
			for (x=0; x<xe; x++) {
				for (y=0; y<ye; y++) {
						pixel2[x][y]=pixel[x][y];
						ip.putPixel(x,y, pixel[x][y]);
				}
			}
		}

		//return to original state
		if (doIwhite==false){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y,255-ip.getPixel(x,y));
			}
		}

	}


	void showAbout() {
		IJ.showMessage("About GreyscaleErode_...",
		"GreyscaleErode_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for morphological dilation of a Greyscale image."+
		"Erodes the entire image, including borders.\n"+
		"Supports black and white foregrounds.");
	}

}
