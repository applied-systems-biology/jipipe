package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import ij.gui.*;

// Binary Thin is a Hit or Miss transform by Gabriel Landini, G.Landini at bham.ac.uk
// With thanks to Wayne Rasband for help with the dialog handling
// v.1.1 added "inverse"

public class BinaryHitOrMiss_ implements PlugInFilter {
	protected boolean doIwhite;
	protected int gridWidth = 3;
    protected int gridHeight = 3;
    protected int gridSize = gridWidth*gridHeight;
    protected TextField[] tf = new TextField[gridSize];
    protected int [] value = new int[gridSize];
	protected String selectedOption;
	protected String kernA="";
	protected int iterations;

	public int setup(String arg, ImagePlus imp) {
		int i;
		ImageStatistics stats;
		stats=imp.getStatistics();
		if (IJ.versionLessThan("1.31l")){
           IJ.error("Needs ImageJ 1.31l or newer.");
			return DONE;
		}
		if (stats.histogram[0]+stats.histogram[255]!=stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return DONE;
		}

		if (arg.equals("about"))
			{showAbout(); return DONE;}

		showDialog("A"); //show 3x3 kernel

		for (i=0; i<9;i++)
			kernA+=Integer.toString(value[i])+" ";

		GenericDialog gd = new GenericDialog("Binary Hit-or-Miss", IJ.getInstance());
		gd.addMessage("Binary Hit-or-Miss transform)");
		gd.addMessage("Kernel configuration");
		gd.addMessage("   1  2  3");
		gd.addMessage("   4  5  6");
		gd.addMessage("   7  8  9");
		gd.addMessage("");
		gd.addMessage("0=empty, 1=set, 2=don't care");
		gd.addStringField ("Kernel_A", kernA,14);

		String [] roption={"none", "rotate 90", "rotate 45", "rotate 180", "mirror", "flip", "inverse" };
		gd.addChoice("Rotations", roption, roption[0]);
		gd.addCheckbox("White foreground",true);

		gd.showDialog();
		if (gd.wasCanceled())
			return DONE;

		kernA=gd.getNextString();

		selectedOption=gd.getNextChoice();
		doIwhite = gd.getNextBoolean ();

		return DOES_8G+DOES_STACKS;
	}

	public void run(ImageProcessor ip) {
		//long start = System.currentTimeMillis();
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y, i, j;
		int [][] pixel = new int [xe][ye];
		int [][] pixel2 = new int [xe][ye];

		int []lut = new int [512];
		int[] kA = new int[gridSize];
		//= transform kernel strings to arrays
		// will accept 0 1 2 separated by spaces or not
		j=-1;
		for (i=0;i<kernA.length();i++){
			if (!(kernA.substring(i,i+1).equals(" "))){
				kA[++j]=(int) Integer.valueOf(kernA.substring(i,i+1)).intValue();
			}
		}

		//original converted to white particles
		if (doIwhite==false){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y,255-ip.getPixel(x,y));
			}
		}
		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				pixel2[x][y]=(ip.getPixel(x,y)==0?0:1);//original
				pixel[x][y]=0;//pixel2[x][y]/255;
			}
		}

		//do it the first time
		createHMlut(kA,lut);
		hitmiss (xe, ye, pixel2, pixel, lut); //pixel2 is the input, pixel is the result

		//do other rotations
		if (selectedOption.equals("rotate 90")){// rotate left
			for(i=0;i<3;i++){
				rotate90(kA);
				createHMlut(kA,lut);
				hitmiss (xe, ye, pixel2, pixel, lut); //pixel2 is the input, pixel is the result
			}
			//put back in order for next iteration
			rotate90(kA);
		}

		if (selectedOption.equals("rotate 45")){// rotate left
			for(i=0;i<7;i++){
				rotate45(kA);
				createHMlut(kA,lut);
				hitmiss (xe, ye, pixel2, pixel, lut); //pixel2 is the input, pixel is the result
			}
			//put back in order for next iteration
			rotate45(kA);
		}

		if (selectedOption.equals("rotate 180")){// rotate left 90 twice
			rotate90(kA);
			rotate90(kA);
			createHMlut(kA,lut);
			hitmiss (xe, ye, pixel2, pixel, lut); //pixel2 is the input, pixel is the result
			//put back in order for next iteration
			rotate90(kA);
			rotate90(kA);
		}

		if (selectedOption.equals("mirror")){// side reflection
			mirror(kA);
			createHMlut(kA,lut);
			hitmiss (xe, ye, pixel2, pixel, lut); //pixel2 is the input, pixel is the result
			mirror(kA);
		}

		if (selectedOption.equals("flip")){// upside down
			flip(kA);
			createHMlut(kA,lut);
			hitmiss (xe, ye, pixel2, pixel, lut); //pixel2 is the input, pixel is the result
			//put back in order for next iteration
			flip(kA);
		}

		if (selectedOption.equals("inverse")){// upside down
			inverse(kA);
			createHMlut(kA,lut);
			hitmiss (xe, ye, pixel2, pixel, lut); //pixel2 is the input, pixel is the result
			//put back in order for next iteration
			inverse(kA);
		}

		//System.out.println("Debug:"+((System.currentTimeMillis()-start)/1000.0)+" seconds");

		//put result
		if (doIwhite==false){
		//return to original state
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x, y, (pixel[x][y]>0?0:255));
			}
		}
		else{
			for (x=0; x<xe; x++) {
				for (y=0; y<ye; y++){
				ip.putPixel(x, y, (pixel[x][y]>0?255:0));
				}
			}
		}
	}


	void rotate90 (int [] kernel){
		int temp;
		temp=kernel[0];
		kernel[0]=kernel[2];
		kernel[2]=kernel[8];
		kernel[8]=kernel[6];
		kernel[6]=temp;
		temp=kernel[3];
		kernel[3]=kernel[1];
		kernel[1]=kernel[5];
		kernel[5]=kernel[7];
		kernel[7]=temp;
	}

	void rotate45 (int [] kernel){
		int temp;
		temp=kernel[0];
		kernel[0]=kernel[1];
		kernel[1]=kernel[2];
		kernel[2]=kernel[5];
		kernel[5]=kernel[8];
		kernel[8]=kernel[7];
		kernel[7]=kernel[6];
		kernel[6]=kernel[3];
		kernel[3]=temp;
	}

	void mirror (int [] kernel){
		int temp;
		temp=kernel[0];
		kernel[0]=kernel[2];
		kernel[2]=temp;
		temp=kernel[3];
		kernel[3]=kernel[5];
		kernel[5]=temp;
		temp=kernel[6];
		kernel[6]=kernel[8];
		kernel[8]=temp;
	}

	void flip (int [] kernel){
		int temp;
		temp=kernel[0];
		kernel[0]=kernel[6];
		kernel[6]=temp;
		temp=kernel[1];
		kernel[1]=kernel[7];
		kernel[7]=temp;
		temp=kernel[2];
		kernel[2]=kernel[8];
		kernel[8]=temp;
	}
	void inverse (int [] kernel){
		// make 1->0 and 0->1
		for (int j=0;j<9;j++){
			if (kernel[j]==0)
				kernel[j]=1;
			else if (kernel[j]==1)
				kernel[j]=0;
		}
	}



	void createHMlut(int [] kernel, int [] lut){
		int i, j, match, toMatch;

		for(i=0;i<512;i++)
			lut[i]=1;

		toMatch=0;
		for(j=0;j<9;j++){
			if (kernel[j]!=2)
				toMatch++;
		}
		//System.out.println("Debug: to match: "+toMatch);
		//make lut
		for(i=0;i<512;i++){
			match=0;
			for(j=0;j<9;j++){
				if (kernel[j]!=2){
					if ((((i & (int)Math.pow(2,j))!=0)?1:0) == kernel[j]) match++;
				}
			}
			if (match!=toMatch){
				lut[i]=0;
			}
		}
	}

	void hitmiss(int xe, int ye, int [][] pixel2, int [][]pixel, int [] lut){
		int x, y;
		// considers the pixels outside image to be 0 (not set)
		for (x=1; x<xe-1; x++) {
			for (y=1; y<ye-1; y++) {
				pixel[x][y]+= lut[(
				pixel2[x-1][y-1] +
				pixel2[x  ][y-1] * 2 +
				pixel2[x+1][y-1] * 4 +
				pixel2[x-1][y  ] * 8 +
				pixel2[x  ][y  ] * 16 +
				pixel2[x+1][y  ] * 32 +
				pixel2[x-1][y+1] * 64 +
				pixel2[x  ][y+1] * 128 +
				pixel2[x+1][y+1] * 256)];
			}
		}

		y=0;
		for (x=1; x<xe-1; x++) {
			//upper row
			pixel[x][y]+= lut[(
			pixel2[x-1][y  ] * 8 +
			pixel2[x  ][y  ] * 16 +
			pixel2[x+1][y  ] * 32 +
			pixel2[x-1][y+1] * 64 +
			pixel2[x  ][y+1] * 128 +
			pixel2[x+1][y+1] * 256)];
		}

		y=ye-1;
		for (x=1; x<xe-1; x++) {
			//lower row
			pixel[x][y]+= lut[(
			pixel2[x-1][y-1] +
			pixel2[x  ][y-1] * 2 +
			pixel2[x+1][y-1] * 4 +
			pixel2[x-1][y  ] * 8 +
			pixel2[x  ][y  ] * 16 +
			pixel2[x+1][y  ] * 32)];
		}

		x=0;
		for (y=1; y<ye-1; y++) {
			//left column
			pixel[x][y]+= lut[(
			pixel2[x  ][y-1] * 2 +
			pixel2[x+1][y-1] * 4 +
			pixel2[x  ][y  ] * 16 +
			pixel2[x+1][y  ] * 32 +
			pixel2[x  ][y+1] * 128 +
			pixel2[x+1][y+1] * 256)];
		}

		x=xe-1;
		for (y=1; y<ye-1; y++) {
			//right column
			pixel[x][y]+= lut[(
			pixel2[x-1][y-1] +
			pixel2[x  ][y-1] * 2 +
			pixel2[x-1][y  ] * 8 +
			pixel2[x  ][y  ] * 16 +
			pixel2[x-1][y+1] * 64 +
			pixel2[x  ][y+1] * 128)];
		}

		x=0; //upper left corner
		y=0;
		pixel[x][y]+= lut[(pixel2[x][y] * 16 + pixel2[x+1][y] * 32 + pixel2[x][y+1] * 128 + pixel2[x+1][y+1] * 256)];

		x=xe-1; //upper right corner
		//y=0;
		pixel[x][y]+= lut[(pixel2[x-1][y] * 8 + pixel2[x][y] * 16 + pixel2[x-1][y+1] * 64 + pixel2[x][y+1] * 128)];

		x=0; //lower left corner
		y=ye-1;
		pixel[x][y]+= lut[(pixel2[x][y-1] * 2 + pixel2[x+1][y-1] * 4 + pixel2[x][y] * 16 + pixel2[x+1][y] * 32)];

		x=xe-1; //lower right corner
		y=ye-1;
		pixel[x][y]+= lut[(pixel2[x-1][y-1] + pixel2[x][y-1] * 2 + pixel2[x-1][y] * 8 + pixel2[x  ][y] * 16)];
	}


	boolean showDialog(String t) {
		GenericDialog gd = new GenericDialog(t);
		gd.addPanel(makePanel(gd));
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		getValues();
		return true;
	}

	Panel makePanel(GenericDialog gd) {
		Panel panel = new Panel();
		panel.setLayout(new GridLayout(gridWidth, gridHeight));
		for (int i=0; i<gridSize; i++) {
			tf[i] = new TextField(""+value[i]);
			panel.add(tf[i]);
		}
		return panel;
	}

	void getValues() {
		for (int i=0; i<gridSize; i++) {
			String s = tf[i].getText();
			value[i] = getValue(s);
		}
	}

	int getValue(String theText) {
		Double d;
		try {d = new Double(theText);}
		catch (NumberFormatException e){
			d = null;
		}
		return d==null?(int)Double.NaN:(int)d.doubleValue();
	}


	void showAbout() {
		IJ.showMessage("About BinaryHitOrMiss_...",
		"BinaryHitOrMiss_ by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"ImageJ plugin for hit-or-miss transform of a binary image.\n"+
		"Returns the locations of the image that match the kernel pattern..\n"+
		"Supports rotation of the kernel template.\n"+
		"Supports black and white foregrounds.");
	}

}
