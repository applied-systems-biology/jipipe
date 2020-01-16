package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.MeasurementsWriter;
import java.util.*;

//Lines8_Plus by Gabriel Landini, G.Landini@bham.ac.uk
// Copyright (c) G. Landini
// 1.0   18/Mar/2006 Converted from Particle8_Plus (v2.0) to Lines8_Plus
// 2.1   14/May/2006 Remove the "\n" from the slice label, otherwise the Results table is not formatted correctly
// 2.2   27/2/2007 added proprer skeleton length PLenght is the skeleton length based on the perimeter,
//         SkelLen is the skeleton length based on connectivity
// 2.3   16/Jun/2007 added redirection to RGB
// 2.4   25/Aug/2007 If Overwrite is false, the data is appended to the Results table.
//         When run from the IJ menu and Overwrite is true, it will ask to save any data
//         in the Results Table. When running from a macro, no questions are asked.
// 2.6   18/May/2000 Speed up using flood fill instead of scanning up and down the blob bounding box
// 2.7   21/May/2008 Single plugin to do morphology or fast analysis.
// 2.8   28/May/2008 fast erasing if using filtering, removed feret drawing (use a macro instead)
// 2.9   17/Jul/2008 Does the CentreOfMass at the end, otherwise the point might fall in a near particle and that
//         locks the labelling. Coordinates are done at the end too.
// 2.10  19/Feb/2009 Faster results table, thanks to Wayne Rasband
// 2.11  19/Feb/2009 Fixed bug introduced in 2.10 :-/
// 2.12  14/Sep/2010 Allow redirection to 16 bits images
// 2.13  15/Apr/2011 Rename 1Point etc to Point1 so the Particle Classifier does not get confused
// 2.14  20/Sep/2012 Fixed connectivity analysis for stacks
// 2.15  18/Feb/2014 Allow redirection to 32 bit images
// 2.16  5/Mar/2014  Fixed bug when calling from setBatchMode(true) macro
// 2.17  22/Jul/2014 cleaned code & speed up
// 2.18  16/Apr/2016 PerEquivD is computed properly now as Perim/Pi. Thanks to Peter J. Lee for reporting this
// 2.19  28/May/2016 convert to plugin to call directly, several speed ups, fixed single pixel in 32bit images


public class Lines8_ implements PlugIn {

	/** Ask for parameters and then execute.*/
	public void run(String arg) {
		
		if (IJ.versionLessThan("1.49c")) return;
		
		// 1 - Obtain the currently active image if necessary:
		ImagePlus imp2=null, imp = WindowManager.getCurrentImage();

		if (imp==null) {
			IJ.error("No image found.");
			return;
		}

		// Check images are 8-bit, binary and square pixels. Must check this before calling the exec method.
		if (imp.getBitDepth()!=8 || imp.getBitDepth()!=8) {
			IJ.showMessage("Error", "Only 8-bit images are supported");
			return;
		}

		ImageStatistics stats;
		stats = imp.getStatistics();
		if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount){
			IJ.error("8-bit binary image (0 and 255) required.");
			return;
		}
		
		Calibration cal = imp.getCalibration();
		if(cal.pixelWidth!=cal.pixelHeight){
			IJ.error("Image pixels are not squared.");
			return;
		}
		
		// get ther rest of the images to redirection list
		int[] wList = WindowManager.getIDList();

		if (wList==null || wList.length<1) {
			IJ.showMessage("Lines8 ","There must be at least one image open");
			return;
		}

		String[] titles = new String[wList.length+1];
		String none = "None";
		titles[0] = none;
		int  thisimage=imp.getID(), indx=0;

		for (int i=0; i<wList.length; i++) {
			imp2 = WindowManager.getImage(wList[i]);
			titles[i]=none;
			if (null !=imp2) {
				if (wList[i]!=thisimage)
					titles[i] = imp2.getTitle();
				else
					indx=i;
			}
		}

		if (indx!=0){
			none=titles[0];
			titles[indx]=none;
			titles[0]="None"; // first in list
		}
		titles[titles.length-1] = "Connectivity";
		imp2=null;
		
		
		// 2 - Ask for parameters:
		GenericDialog gd = new GenericDialog("Lines8_", IJ.getInstance());
		gd.addMessage("Lines8_ v2.19 by G. Landini");
		gd.addMessage("Only \'square pixel\' images supported!");
		gd.addCheckbox("White lines [255] on black [0] background",true);
		gd.addCheckbox("Skeletonize",true);
		gd.addCheckbox("Exclude edge lines",false);
		gd.addCheckbox("Label lines",false);
		gd.addCheckbox("Morphology Parameters",false);
		String [] DisplayOption={"Lines", "Coordinates", "CentreOfMass"};
		gd.addChoice("Show", DisplayOption, DisplayOption[0]);
		gd.addCheckbox("Filter by size (pixels):",false);
		gd.addNumericField ("Minimum size:",  0, 0);
		gd.addNumericField ("Maximum size:",  9999999, 0);
		gd.addCheckbox("Display results",true);
		gd.addCheckbox("Overwrite results",false);
		gd.addChoice("Redirect to", titles, titles[0]);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		
		// 3 - Retrieve parameters from the dialog
		boolean doIwhite = gd.getNextBoolean ();
		boolean doISkel= gd.getNextBoolean ();
		boolean doIdeleteborders = gd.getNextBoolean ();
		boolean doIlabel = gd.getNextBoolean ();
		boolean doImorpho = gd.getNextBoolean ();
		//boolean doReset = doImorpho && rt.getCounter()>1 && rt.getColumnIndex("Roundness")<0; //wr
		String selectedOption = gd.getNextChoice ();

		boolean doIminmax = gd.getNextBoolean ();
		int mi = (int)gd.getNextNumber();
		int ma = (int)gd.getNextNumber();
		boolean doIshowstats = gd.getNextBoolean ();
		boolean doIoverwrite = gd.getNextBoolean ();
		String redirected = gd.getNextChoice ();
		if (!redirected.equals("None") && !redirected.equals("Connectivity")){
			imp2=WindowManager.getImage(redirected);
		}
		
		ResultsTable rt= ResultsTable.getResultsTable();
		
		if (doIoverwrite){ //wr
			if (!IJ.macroRunning()){
				if (rt.getCounter()>0) {
					SaveChangesDialog di = new SaveChangesDialog(IJ.getInstance(), "Save "+rt.getCounter()+" measurements?");
					if (di.cancelPressed())
						return ;
					else if (di.savePressed())
						new MeasurementsWriter().run("");
				}
			}
			//rt.reset();
		}

		 // 4 - Execute!
		Object[] result = exec(imp, imp2, doIwhite, doISkel, doIdeleteborders, doIlabel, doImorpho, selectedOption, doIminmax, mi, ma, doIshowstats, doIoverwrite, redirected);//, rt);
  
		// 5 - If all went well, show the image:
		if (null != result) {
			ImagePlus resultImage = (ImagePlus) result[1];

			//if(createWindow)
			//	resultImage.show();
			//else
			//	imp2.getProcessor().insert(resultImage.getProcessor(),0,0);
		}
	}
	
	
	/** Execute the plugin functionality: duplicate and scale the given image.
	* @return an Object[] array with the name and the scaled ImagePlus.
	* Warning!: This method does NOT check whether the two input images are binary, this is checked in the setup,
	*  so careful when calling this method from another plugin. Make sure both images are binary!!
	* Does NOT show the new, image; just returns it. */
	 public Object[] exec(ImagePlus imp, ImagePlus imp2, boolean doIwhite, boolean doISkel, boolean doIdeleteborders, boolean doIlabel, boolean doImorpho, String selectedOption, boolean doIminmax, int mi,int ma, boolean doIshowstats, boolean doIoverwrite, String redirected) {
	 

		int xe = imp.getWidth();
		int ye = imp.getHeight();
		ImageStack istk = imp.getStack();
		int lastSlice=istk.getSize();

		String slabel;
		int X = xe-1, Y = ye-1, XY = X*Y;
		int [] xd = {1,1,1,0,-1,-1,-1,0};
		int [] yd = {-1,0,1,1,1,0,-1,-1};
		int [] g = {6,0,0,2,2,4,4,6};
		double [] z = {1.414213538169861, 1.0, 1.414213538169861, 1.0, 1.414213538169861, 1.0, 1.414213538169861, 1.0};
		double [] chresults = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0}; // convex hull, cx, cy, radius of minimal bounding circle, convex area, feret, fx1, fy1, fx2, fy2
		int x=0, y=0, x1, y1, q, uy, dy, rx, lx, pixs, fig, x2, y2, py=0, px=0, newcol, f1, fx1=-1, fx2=-1, fy1=-1, fy2=-1, vl, pb1=-1, pb2=-1;
		long m10, m01;
		int bits=0, pxgrey=0, pxred=0,  pxgreen=0, pxblue=0;
		float fpxgrey=0;
		double nz, narea, mcx, mcy, feret=-1, tferet, rads=180/Math.PI, angle=0.0, breadth1=-1, breadth2=-1, minr=-1, maxr=-1, skellen=-1;

		float [] parsR = new float [20];
		float [] parsG = new float [20];
		float [] parsB = new float [20];

		ResultsTable rt= ResultsTable.getResultsTable();
		if (doIoverwrite) rt.reset();
		
		Vector vx = new Vector();
		Vector vy = new Vector();

		Vector vr = new Vector();
		Vector vg = new Vector();
		Vector vb = new Vector();
	
		ImageProcessor ip=imp.getProcessor(), ip2=null;
		
		FloodFiller ff = new FloodFiller(ip);

		if (doISkel){
			Prefs.blackBackground = true;
			IJ.run(imp, "Skeletonize", "stack");
		}

		if (redirected.equals("Connectivity")){
			//IJ.run("Duplicate...", "title=Connectivity duplicate");
			//IJ.run("BinaryConnectivity ", "white");
			imp2=new Duplicator().run(imp);
			IJ.run(imp2, "BinaryConnectivity ", "white");
			//imp2.show();
		}

		
		IJ.showStatus("Lines8...");
		for(int slice=1;slice<=lastSlice;slice++){
			IJ.showProgress(slice, lastSlice);

			imp.setSlice(slice);
			//imp.setSliceWithoutUpdate(slice);
			slabel= istk.getSliceLabel(slice)+"\n"; //add a \n in case it is a null
			if (slabel.length()>0) slabel=slabel.replaceAll("\\n",""); // remove "\n"
		
			if (imp2!=null) {
				bits=imp2.getBitDepth();
				imp2.setSlice(slice); // advance redirection stack
				//imp.setSliceWithoutUpdate(slice);
				ip2=imp2.getProcessor();
			}


			// if not white particles then invert
			if (doIwhite==false){
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++)
						ip.putPixel(x,y,255-ip.getPixel(x,y));
				}
			}

			if(doIdeleteborders){
				ip.setColor(0);
				for (y=0; y<ye; y++){
					if (ip.getPixel(0,y)==255) ff.fill8(0, y);
					if (ip.getPixel(X,y)==255) ff.fill8(X, y);
				}
				for (x=0; x<xe; x++){
					if (ip.getPixel(x,0)==255) ff.fill8(x, 0);
					if (ip.getPixel(x,Y)==255) ff.fill8(x, Y);
				}
			}

			ip.setColor(254);

			fig=0;
			// find first scanned pixel
			for (y=0; y<ye; y++){
				for (x=0; x<xe; x++){
					if (ip.getPixel(x,y)==255){
				
						vx.clear();
						vy.clear();
						vr.clear();
						vg.clear();
						vb.clear();

						vx.add(new Integer(x));
						vy.add(new Integer(y));

						x1=x;
						y1=y;
						//bounding box
						uy=y;//upper y
						dy=0;//lower y
						rx=0;//right x
						lx=xe;//left x

						nz=0;
						skellen=0;
						narea=0;
						pixs=0;
						//single pixel?
						if(ip.getPixel(x+1,y)+ip.getPixel(x,y+1)+ip.getPixel(x-1,y+1)+ip.getPixel(x+1,y+1)==0){
							if (doIminmax && (mi>0)) {
								ip.putPixel(x,y,0);
							}
							else{
								ip.putPixel(x,y,((fig  % 251) + 1)); //new colour if labelling
								if (ip2!=null){
									if (bits==24){
										pxgrey = ip2.getPixel(x,y);// look at x,y of redirected
										pxred = (pxgrey & 0xff0000)>>16;
										pxgreen = (pxgrey & 0x00ff00)>>8;
										pxblue = (pxgrey & 0x0000ff);
									}
									else if (bits==32){
										fpxgrey=ip2.getPixelValue(x,y);
									}
									else
										pxgrey = ip2.getPixel(x,y);// look at x,y of redirected 8 or 16 bit
								}
								pixs=1;
								fig++;

								rt.incrementCounter();
								rt.addLabel(slabel);
								rt.addValue("Slice", slice);
								rt.addValue("Number", fig);
								rt.addValue("XStart", x);
								rt.addValue("YStart", y);
								rt.addValue("PLength", 0);
								rt.addValue("SkelLength", 0);
								rt.addValue("Area", 0);
								rt.addValue("Pixels", pixs);
								rt.addValue("XM", x);
								rt.addValue("YM", y);
								if (doImorpho){
									rt.addValue("ROIX1", x);
									rt.addValue("ROIY1", y);
									rt.addValue("ROIX2", x);
									rt.addValue("ROIY2", y);
									rt.addValue("MinR", 0);
									rt.addValue("MaxR", 0);
									rt.addValue("Feret", 0);
									rt.addValue("FeretX1", x);
									rt.addValue("FeretY1", y);
									rt.addValue("FeretX2", x);
									rt.addValue("FeretY2", y);
									rt.addValue("FAngle", 0);
									rt.addValue("Breadth", 0);
									rt.addValue("BrdthX1", x);
									rt.addValue("BrdthY1", y);
									rt.addValue("BrdthX2", x);
									rt.addValue("BrdthY2", y);
									rt.addValue("CHull", 0);
									rt.addValue("CArea", 0);
									rt.addValue("MBCX", x);
									rt.addValue("MBCY", y);
									rt.addValue("MBCRadius", 0);
									rt.addValue("CountCorrect", 1);

									rt.addValue("AspRatio",-1);
									rt.addValue("Circ", -1);
									rt.addValue("Roundness", -1);
									rt.addValue("ArEquivD", -1);
									rt.addValue("PerEquivD",-1);
									rt.addValue("EquivEllAr",-1);
									rt.addValue("Compactness", -1);
									rt.addValue("Solidity", -1);
									rt.addValue("Concavity", -1);
									rt.addValue("Convexity", -1);
									rt.addValue("Shape",-1);
									rt.addValue("RFactor",-1);
									rt.addValue("ModRatio",-1);
									rt.addValue("Sphericity",-1);
									rt.addValue("ArBBox", -1);
									rt.addValue("Rectang", -1);
								}
								if (ip2!=null){
									if (bits==24){
										rt.addValue("RedIntDen", pxred);
										rt.addValue("RedMin", pxred);
										rt.addValue("RedMax", pxred);
										rt.addValue("RedMode", pxred);
										rt.addValue("RedMedian", pxred);
										rt.addValue("RedAverage",(float) pxred);
										rt.addValue("RedAvDev", (float) 0);
										rt.addValue("RedStDev", (float) 0);
										rt.addValue("RedVar", (float) 0);
										rt.addValue("RedSkew", (float) 0);
										rt.addValue("RedKurt", (float) 0);
										rt.addValue("RedEntr", (float) 0);

										rt.addValue("GreenIntDen", pxgreen);
										rt.addValue("GreenMin", pxgreen);
										rt.addValue("GreenMax", pxgreen);
										rt.addValue("GreenMode", pxgreen);
										rt.addValue("GreenMedian", pxgreen);
										rt.addValue("GreenAverage",(float) pxgreen);
										rt.addValue("GreenAvDev", (float) 0);
										rt.addValue("GreenStDev", (float) 0);
										rt.addValue("GreenVar", (float) 0);
										rt.addValue("GreenSkew", (float) 0);
										rt.addValue("GreenKurt", (float) 0);
										rt.addValue("GreenEntr", (float) 0);

										rt.addValue("BlueIntDen", pxblue);
										rt.addValue("BlueMin", pxblue);
										rt.addValue("BlueMax", pxblue);
										rt.addValue("BlueMode", pxblue);
										rt.addValue("BlueMedian", pxblue);
										rt.addValue("BlueAverage",(float) pxblue);
										rt.addValue("BlueAvDev", (float) 0);
										rt.addValue("BlueStDev", (float) 0);
										rt.addValue("BlueVar", (float) 0);
										rt.addValue("BlueSkew", (float) 0);
										rt.addValue("BlueKurt", (float) 0);
										rt.addValue("BlueEntr", (float) 0);
									}
									else if (bits==8 || bits==16){
										rt.addValue("GrIntDen", pxgrey);
										rt.addValue("GrMin", pxgrey);
										rt.addValue("GrMax", pxgrey);
										rt.addValue("GrMode", pxgrey);
										rt.addValue("GrMedian", pxgrey);
										rt.addValue("GrAverage",(float) pxgrey);
										rt.addValue("GrAvDev", (float) 0);
										rt.addValue("GrStDev", (float) 0);
										rt.addValue("GrVar", (float) 0);
										rt.addValue("GrSkew", (float) 0);
										rt.addValue("GrKurt", (float) 0);
										rt.addValue("GrEntr", (float) 0);
									}
									else if (bits==32){
										rt.addValue("GrIntDen", fpxgrey);
										rt.addValue("GrMin", fpxgrey);
										rt.addValue("GrMax", fpxgrey);
										rt.addValue("GrAverage",(float) fpxgrey);
										rt.addValue("GrAvDev", (float) 0);
										rt.addValue("GrStDev", (float) 0);
										rt.addValue("GrVar", (float) 0);
										rt.addValue("GrSkew", (float) 0);
										rt.addValue("GrKurt", (float) 0);
									}
									if (redirected.equals("Connectivity")){
										rt.addValue("Point1", (float) 1);
										rt.addValue("Point2", (float) 0);
										rt.addValue("Point3", (float) 0);
										rt.addValue("Point4", (float) 0);
										rt.addValue("Point5", (float) 0);
										rt.addValue("Point6", (float) 0);
										rt.addValue("Point7", (float) 0);
										rt.addValue("Point8", (float) 0);
									}
								}
								//rt.addResults(); //wr gl
							}
						}
						else { //go around the edge
							x1=x;
							y1=y;
							ip.putPixel(x,y,254);
							q=0;
							while(true){
								if (ip.getPixel(x1+xd[q],y1+yd[q])>0){
									x2=x1;
									y2=y1;
									x1+=xd[q];
									y1+=yd[q];
									narea+=(double)((y1+y2)*(x2-x1))*.5;
									//ip.putPixel(x1,y1,254);
									//update coordinates of ROI box
									if(x1 > rx) rx = x1;
									if(x1 < lx) lx = x1;
									if(y1 > dy) dy = y1;
									if(y1 < uy) uy = y1;
									if(ip.getPixel(x1,y1)!=254)
										nz+=z[q]; //Length
									q=g[q];
									ip.putPixel(x1,y1,254);
									
									//check for *that* unique pixel configuration
									if (x1==x){
										if (y1==y){
											if (ip.getPixel(x-1,y+1)!=255){
												break;
											}
										}
									}
									vx.add(new Integer(x1));
									vy.add(new Integer(y1));
								}
								else
									q=(q+1)%8;
							}
							//System.out.println("Debug:"+xe+" "+ye+":   "+ uy+ " "+lx+" "+dy+" "+rx);

							vl=vx.size();

							//reset boundary to 255 and paint in 254
							for (f1=0;f1<vl;f1++){
								ip.putPixel(((Integer)vx.get(f1)).intValue(),((Integer)vy.get(f1)).intValue(),255);
							}
							ip.setColor(254);
							ff.fill8(x, y);


							newcol = (fig  % 251) + 1; //new colour if labelling
							m10 = 0;
							m01 = 0;
							mcx=0.0;
							mcy=0.0;

							//momentum
							for(py=uy;py<=dy;py++){
								for(px=lx;px<=rx;px++){
									if(ip.getPixel(px,py)== 254){
										if (ip.getPixel(px,py-1)!=0) skellen++; //calculate the connected pixels then /2
										if (ip.getPixel(px-1,py)!=0) skellen++;
										if (ip.getPixel(px+1,py)!=0) skellen++;
										if (ip.getPixel(px,py+1)!=0) skellen++;

										if ((ip.getPixel(px-1,py-1)!=0 ) && ((ip.getPixel(px,py-1) + ip.getPixel(px-1,py))==0)) skellen+=z[0];
										if ((ip.getPixel(px+1,py-1)!=0 ) && ((ip.getPixel(px,py-1) + ip.getPixel(px+1,py))==0)) skellen+=z[0];
										if ((ip.getPixel(px-1,py+1)!=0 ) && ((ip.getPixel(px-1,py) + ip.getPixel(px,py+1))==0)) skellen+=z[0];
										if ((ip.getPixel(px+1,py+1)!=0 ) && ((ip.getPixel(px+1,py) + ip.getPixel(px,py+1))==0)) skellen+=z[0];
									
										pixs+=1;
										m10+=px;
										m01+=py;
										ip.putPixel(px,py,newcol); //label particle either newcol or 253 (to be tested for deletion).
										if (ip2!=null){
											if (bits==24){
												pxgrey=ip2.getPixel(px,py);
												vr.add(new Integer(((pxgrey & 0xff0000)>>16)));
												vg.add(new Integer(((pxgrey & 0x00ff00)>>8)));
												vb.add(new Integer((pxgrey & 0x0000ff)));
											}
											else if (bits==8 || bits==16 || redirected.equals("Connectivity")) {
												vr.add(new Integer(ip2.getPixel(px,py)));// look at px,py of original to get greys, put into vector 
											}
	
											else if (bits==32){
												vr.add(new Float(ip2.getPixelValue(px,py)));// look at px,py of original to get greys, put into vector 
											}
											//else 
											//	vr.add(new Integer(ip2.getPixel(px,py)));// look at px,py of original to get greys, put into vector 
											
										}
									}
								}
							}

							skellen/=2; // divide skellen by 2 as each connection was counted twice.

							mcx = ((double)m10 / pixs);
							mcy = ((double)m01 / pixs);

							if (doIminmax && (pixs<mi || pixs>ma)){ // erase (filter) particles
								ip.setColor(0);
								ff.fill8(x, y);
	
							}
							else {  //particle not to be deleted... compute morphology
								fig++;
								rt.incrementCounter();
								rt.addLabel(slabel);
								
								rt.addValue("Slice", slice);
								rt.addValue("Number", fig);
								rt.addValue("XStart", x);
								rt.addValue("YStart", y);
								rt.addValue("PLength", (float)nz);
								rt.addValue("SkelLength", (float)skellen);
								rt.addValue("Area", (float)narea);
								rt.addValue("Pixels", pixs);
								rt.addValue("XM", (float)mcx);
								rt.addValue("YM", (float)mcy);
								if (doImorpho){
									// Convex hull here
									convh( vx, vy, vl, chresults);
									
									// feret diameter here
									feret=chresults[5];
									fx1=(int)chresults[6];
									fy1=(int)chresults[7];
									fx2=(int)chresults[8];
									fy2=(int)chresults[9];

									// feret angle here
									if (fx2==fx1)
										angle=90.0;
									else
										angle=-Math.atan((double)(fy2-fy1)/(double)(fx2-fx1))*rads;
									if (angle<0)
										angle=180+angle;

									// breadth here	
									breadth1=0;
									breadth2=0;
									pb1=0;
									pb2=0;
									for (f1=0;f1<vl;f1++){
										tferet=((fy1-fy2)*((Integer)(vx.get(f1))).intValue() +(fx2-fx1)*((Integer)(vy.get(f1))).intValue()+(fx1*fy2 -fx2*fy1))/feret;
										if (tferet>breadth1){
											breadth1=tferet;
											pb1=f1;
										}
										if (tferet<breadth2){
											breadth2=tferet;
											pb2=f1;
										}
									}
									//IJ.log("Breadth1: "+breadth1+"   "+((Integer)(vx.get(pb1))).intValue()+" "+((Integer)(vy.get(pb1))).intValue()+" breadth2: "+breadth2+"   "+

									//Minimal Maximal radii
									minr=Double.MAX_VALUE;
									maxr=-1;
									for (f1=0;f1<vl;f1++){
										tferet=Math.pow(mcx-((Integer)(vx.get(f1))).intValue(),2) +Math.pow(mcy-((Integer)(vy.get(f1))).intValue(),2);
										if (tferet>maxr){
											maxr=tferet;
										}
										if (tferet<minr){
											minr=tferet;
										}
									}
									minr=Math.sqrt(minr);
									maxr=Math.sqrt(maxr);
									//IJ.log("MinR: "+minr+"   MaxR:"+maxr);
									
									// add to the table
									rt.addValue("ROIX1", lx);
									rt.addValue("ROIY1", uy);
									rt.addValue("ROIX2", rx);
									rt.addValue("ROIY2", dy);
									rt.addValue("MinR", (float)minr);
									rt.addValue("MaxR", (float)maxr);
									rt.addValue("Feret", (float)feret);
									rt.addValue("FeretX1", fx1);
									rt.addValue("FeretY1", fy1);
									rt.addValue("FeretX2", fx2);
									rt.addValue("FeretY2", fy2);
									rt.addValue("FAngle", (float)angle);
									rt.addValue("Breadth",(float)(Math.abs(breadth1)+Math.abs(breadth2)));
									rt.addValue("BrdthX1",((Integer)(vx.get(pb1))).intValue());
									rt.addValue("BrdthY1",((Integer)(vy.get(pb1))).intValue());
									rt.addValue("BrdthX2",((Integer)(vx.get(pb2))).intValue());
									rt.addValue("BrdthY2",((Integer)(vy.get(pb2))).intValue());
									rt.addValue("CHull", (float)chresults[0]);
									rt.addValue("CArea", (float)chresults[4]);
									rt.addValue("MBCX", (float)chresults[1]);
									rt.addValue("MBCY", (float)chresults[2]);
									rt.addValue("MBCRadius", (float)chresults[3]);
									rt.addValue("CountCorrect", (float)((double)XY/((X-(1+rx-lx))*(Y-(1+dy-uy)))));
									rt.addValue("AspRatio", (((Math.abs(breadth1)+Math.abs(breadth2))<0.01)?-1:(float)(feret/(double)(Math.abs(breadth1)	+Math.abs(breadth2)))));
									rt.addValue("Circ", (float) (4.0*Math.PI*narea/Math.pow(nz,2)));
									rt.addValue("Roundness", (float) (4.0*narea/(Math.PI*Math.pow(feret,2))));
									rt.addValue("ArEquivD", (float) Math.sqrt((4.0/Math.PI)*narea));
									rt.addValue("PerEquivD", (float) (nz/Math.PI));
									rt.addValue("EquivEllAr", (float) (Math.PI*feret*(Math.abs(breadth1)+Math.abs(breadth2))/4.0));
									rt.addValue("Compactness", (float) (Math.sqrt((4.0/Math.PI)*narea)/feret));
									rt.addValue("Solidity", ((chresults[4]>0)?(float)(narea/chresults[4]):-1));
									rt.addValue("Concavity", (float) (chresults[4]-narea));
									rt.addValue("Convexity", (float) (chresults[0]/nz));
									rt.addValue("Shape", ((narea>0)?(float) ((nz*nz)/narea):-1));
									rt.addValue("RFactor", (float) (chresults[0]/(feret*Math.PI)));
									rt.addValue("ModRatio", (float) ((2.0*minr)/feret));
									rt.addValue("Sphericity", (float) (minr/maxr));
									rt.addValue("ArBBox", (float) (feret*(Math.abs(breadth1)+Math.abs(breadth2))));
									rt.addValue("Rectang",   (((Math.abs(breadth1)+Math.abs(breadth2))<0.01)?-1:(float)(narea/(feret*(Math.abs(breadth1)+Math.abs(breadth2))))));
								}
								if (ip2!=null){
									if (bits==24){
										stats(pixs, vr, parsR);
										stats(pixs, vg, parsG);
										stats(pixs, vb, parsB);
										
										rt.addValue("RedIntDen", (int) parsR[0]);
										rt.addValue("RedMin",  (int)parsR[1]);
										rt.addValue("RedMax", (int)parsR[2]);
										rt.addValue("RedMode", (int)parsR[3]);
										rt.addValue("RedMedian", (int)parsR[4]);
										rt.addValue("RedAverage", parsR[5]);
										rt.addValue("RedAvDev", parsR[6]);
										rt.addValue("RedStDev", parsR[7]);
										rt.addValue("RedVar", parsR[8]);
										rt.addValue("RedSkew", parsR[9]);
										rt.addValue("RedKurt", parsR[10]);
										rt.addValue("RedEntr", parsR[11]);
										rt.addValue("GreenIntDen", (int)parsG[0]);
										rt.addValue("GreenMin", (int)parsG[1]);
										rt.addValue("GreenMax", (int)parsG[2]);
										rt.addValue("GreenMode", (int)parsG[3]);
										rt.addValue("GreenMedian", (int)parsG[4]);
										rt.addValue("GreenAverage", parsG[5]);
										rt.addValue("GreenAvDev", parsG[6]);
										rt.addValue("GreenStDev", parsG[7]);
										rt.addValue("GreenVar", parsG[8]);
										rt.addValue("GreenSkew", parsG[9]);
										rt.addValue("GreenKurt", parsG[10]);
										rt.addValue("GreenEntr", parsG[11]);
										rt.addValue("BlueIntDen", (int)parsB[0]);
										rt.addValue("BlueMin", (int)parsB[1]);
										rt.addValue("BlueMax", (int)parsB[2]);
										rt.addValue("BlueMode", (int)parsB[3]);
										rt.addValue("BlueMedian", (int)parsB[4]);
										rt.addValue("BlueAverage", parsB[5]);
										rt.addValue("BlueAvDev", parsB[6]);
										rt.addValue("BlueStDev", parsB[7]);
										rt.addValue("BlueVar", parsB[8]);
										rt.addValue("BlueSkew", parsB[9]);
										rt.addValue("BlueKurt", parsB[10]);
										rt.addValue("BlueEntr", parsB[11]);
									}
									else if (bits==8 || bits==16){
										stats(pixs, vr, parsR);
										
										rt.addValue("GrIntDen", (int) parsR[0]);
										rt.addValue("GrMin", (int)parsR[1]);
										rt.addValue("GrMax", (int)parsR[2]);
										rt.addValue("GrMode", (int)parsR[3]);
										rt.addValue("GrMedian", (int)parsR[4]);
										rt.addValue("GrAverage", parsR[5]);
										rt.addValue("GrAvDev", parsR[6]);
										rt.addValue("GrStDev", parsR[7]);
										rt.addValue("GrVar", parsR[8]);
										rt.addValue("GrSkew", parsR[9]);
										rt.addValue("GrKurt", parsR[10]);
										rt.addValue("GrEntr", parsR[11]);
									} 
									else if (bits==32){
										stats32(pixs, vr, parsR);
										
										rt.addValue("GrIntDen", parsR[0]);
										rt.addValue("GrMin", parsR[1]);
										rt.addValue("GrMax", parsR[2]);
										rt.addValue("GrAverage", parsR[5]);
										rt.addValue("GrAvDev", parsR[6]);
										rt.addValue("GrStDev", parsR[7]);
										rt.addValue("GrVar", parsR[8]);
										rt.addValue("GrSkew", parsR[9]);
										rt.addValue("GrKurt", parsR[10]);
									}
									if (redirected.equals("Connectivity")){
										rt.addValue("Point1", parsR[12]);
										rt.addValue("Point2", parsR[13]);
										rt.addValue("Point3", parsR[14]);
										rt.addValue("Point4", parsR[15]);
										rt.addValue("Point5", parsR[16]);
										rt.addValue("Point6", parsR[17]);
										rt.addValue("Point7", parsR[18]);
										rt.addValue("Point8", parsR[19]);
									}//integral, min, max, mode, median, ave, avedev, sdev, var, skew, kurt
								}
								//rt.addResults();
							}
						}
					}
				}
			}
			//rt.addResults();

			if (selectedOption.equals("Coordinates") || selectedOption.equals("CentreOfMass")) {
				// delete particles to put other chosen marker
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++){
						if (ip.getPixel(x,y)>0)
							ip.putPixel(x,y,0);
					}
				}
			}

			//if not labelling, change the colours back to 255
			if (doIlabel==false){
				for(y=0;y<ye;y++) {
					for(x=0;x<xe;x++){
						if (ip.getPixel(x,y)>0)
							ip.putPixel(x,y,255);
					}
				}

				//since the centre of mass is done at the end, coordinates are done here too
				if(selectedOption.equals("Coordinates")){
					for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
						ip.putPixel((int)rt.getValue("XStart",x),(int)rt.getValue("YStart",x),255);//.. put startPixel
				}

				// do the centre of mass at the end because it may fall inside some other particle and that locks the labelling
				if(selectedOption.equals("CentreOfMass")){
					for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
						ip.putPixel((int)Math.floor(rt.getValue("XM",x)+0.5),(int)Math.floor(rt.getValue("YM",x)+0.5),255);//.. put centre of mass
				}

				// if white particles, invert
				if (doIwhite==false){
					for(y=0;y<ye;y++) {
						for(x=0;x<xe;x++)
							ip.putPixel(x,y,255-ip.getPixel(x,y));
					}
				}
			}
			else{
				//labelling, leave colours as they are

				if(selectedOption.equals("Coordinates")){
					y=0;
					for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
						ip.putPixel((int)rt.getValue("XStart",x),(int)rt.getValue("YStart",x),(y++  % 251) + 1);//.. put startPixel
				}

				if(selectedOption.equals("CentreOfMass")){
					y=0;
					for (x=rt.getCounter()-fig;x<rt.getCounter();x++)
						ip.putPixel((int)Math.floor(rt.getValue("XM",x)+0.5),(int)Math.floor(rt.getValue("YM",x)+0.5),(y++  % 251) + 1);//.. put centre of mass
				}

				if (doIwhite==false){
					for(y=0;y<ye;y++) {
						for(x=0;x<xe;x++){
							if (ip.getPixel(x,y)==0)
								ip.putPixel(x,y,255); //make black a white background
						}
					}
				}
			}

			if (doIshowstats ){
				if (slice==lastSlice){ 
					rt.updateResults();
					rt.show("Results");
				}
			}
		}

		IJ.showProgress(1.0);
		imp.updateAndDraw();
		imp.setSlice(1);
		if (imp2!=null) imp2.setSlice(1);

		return null;

	}

	void convh(Vector vx, Vector vy, int vl, double [] results) {
		
		int i, j, k=0, m;
		
		int[] x = new int[vl+1];
		int[] y = new int[vl+1];

		results[0]=0.0;
		results[1]=0.0;
		results[2]=0.0;
		results[3]=0.0;
		results[4]=0.0;
		
		for (j=0;j<vl;j++){
			x[j] = ((Integer)(vx.get(j))).intValue();
			y[j] = ((Integer)(vy.get(j))).intValue();
		}
		//x[vl]=x[0];
		//y[vl]=y[0];
		
		//cnvx hull
		int n=vl, min = 0, ney=0, h, h2, dx, dy, temp,ax, ay;
		double minangle, th, t, v, zxmi=0;

		for (i=1;i<n;i++){
			if (y[i] < y[min]) 
				min = i;
		}

		temp = x[0]; x[0] = x[min]; x[min] = temp;
		temp = y[0]; y[0] = y[min]; y[min] = temp;
		min = 0;

		for (i=1;i<n;i++){
			if (y[i] == y[0]){
				ney ++;
				if (x[i] < x[min]) min = i;
			}
		}
		temp = x[0]; x[0] = x[min]; x[min] = temp;
		temp = y[0]; y[0] = y[min]; y[min] = temp;

		min = 0;
		m = -1; 
		x[n] = x[min];
		y[n] = y[min];
		if (ney > 0) 
			minangle = -1;
		else
			minangle = 0;

		while (min != n ){
			m = m + 1;
			temp = x[m]; x[m] = x[min]; x[min] = temp;
			temp = y[m]; y[m] = y[min]; y[min] = temp;

			min = n ; //+1
			v = minangle;
			minangle = 360.0;
			h2 = 0;

			for (i = m + 1;i<n+1;i++){
				dx = x[i] - x[m];
				ax = Math.abs(dx);
				dy = y[i] - y[m];
				ay = Math.abs(dy);
  
				if (dx == 0 && dy == 0) 
					t = 0.0;
				else 
					t = (double)dy / (double)(ax + ay);
  
				if (dx < 0)
					t = 2.0 - t;
				else {
					if (dy < 0)
						t = 4.0 + t;
				}
				th = t * 90.0;

				if(th > v){
					if(th < minangle){
						min = i;
						minangle = th;
						h2 = dx * dx + dy * dy;
					}
					else{
						if (th == minangle){
							h = dx * dx + dy * dy;
							if (h > h2){
								min = i;
								h2 = h;
							}
						}
					}
				}
			}
			zxmi += Math.sqrt(h2);
		}
		m++;

		int[] hx = new int[m];// ROI polygon array 
		int[] hy = new int[m];

		for (i=0;i<m;i++){
			hx[i] =  x[i]; // copy to new polygon array
			hy[i] =  y[i];
			//IJ.log(" "+hx[i]+" "+hy[i]);
			if (i>0)
				results[4] += (double)((hy[i]+hy[i-1])*(hx[i-1]-hx[i]))*.5;
		}
		results[4] += (double)((hy[i-1]+hy[0])*(hx[i-1]-hx[0]))*.5;
		//IJ.log("Hull_points: "+ m);
		//IJ.log("Hull_length: "+(float)zxmi);
		//IJ.log("Hull_area"+results[4]);
		results[0]=(float)zxmi;
		// get the edges between points
		double [] d = new double [(m *(m-1))/2]; // edge length
		int[] p1 = new int [(m *(m-1))/2]; // point 1
		int[] p2 = new int [(m *(m-1))/2]; // point 2

		double feret=-1;
		int pferet1=-1, pferet2=-1;
		k=0;
		for (i=0;i<m-1;i++){
			for (j=i+1;j<m;j++){
				d[k]= Math.sqrt(Math.pow(hx[i]-hx[j], 2.0) + Math.pow(hy[i]-hy[j], 2.0));
				if (d[k]>feret) {feret = d[k]; pferet1=i; pferet2=j;}
				p1[k]=i;
				p2[k]=j;
				k++;
			}
		}
		k--;

		results[5]=feret;
		results[6]=hx[pferet1];
		results[7]=hy[pferet1];
		results[8]=hx[pferet2];
		results[9]=hy[pferet2];

		//sort distances
		boolean sw = true;
		double tempd;
		int p3, ip1;
		double tt, tttemp, radius, cx, cy;
		while (sw){
			sw=false;
			for(i=0;i<k-1;i++){
				ip1=i+1;
				if (d[i]<d[i+1]){
					tempd = d[i]; d[i]  = d[ip1];  d[ip1]  = tempd;
					temp = p1[i]; p1[i] = p1[ip1]; p1[ip1] = temp;
					temp = p2[i]; p2[i] = p2[ip1]; p2[ip1] = temp;
					sw = true;
				}
			}
		}

		//IJ.log("1:"+d[0]+" "+p1[0]+" "+p2[0]);
		//IJ.log("2:"+d[1]+" "+p1[1]+" "+p2[1]);
		radius=d[0]/2.0;

		cx=(hx[p1[0]]+hx[p2[0]])/2.0;
		cy=(hy[p1[0]]+hy[p2[0]])/2.0;

		// find largest distance from point c
		//sw=false;
		p3=-1;
		tt=radius;
		for (i=0;i<m;i++){
			tttemp=Math.sqrt(Math.pow(hx[i]-cx, 2.0) + Math.pow(hy[i]-cy, 2.0));
			if(tttemp>tt){
				tt=tttemp;
				//IJ.log("Largest from c: "+Math.sqrt(Math.pow(hx[i]-cx, 2.0) + Math.pow(hy[i]-cy, 2.0))+"  "+radius);
				p3=i;
			}
		}

		if (p3>-1){
			// 3 or more- points circle
			//IJ.log("p3:"+p3);
			//IJ.log("osculating circle p1, p2, p3");
			double [] op1 = new double [2];
			double [] op2 = new double [2];
			double [] op3 = new double [2];
			double [] circ = new double [3];
			double tD=Double.MAX_VALUE;
			int tp1=0, tp2=0, tp3=0, z;

			// GL new test: find the smallest osculating circle that contains all convex hull points
			for (i=0; i<m-2; i++){
				for (j=i+1; j<m-1; j++){
					for (k=j+1; k<m; k++){
						op1[0]=hx[i];
						op1[1]=hy[i];
						op2[0]=hx[j];
						op2[1]=hy[j];
						op3[0]=hx[k];
						op3[1]=hy[k];
						osculating(op1, op2, op3, circ);
						// IJ.log(""+i+ " "+j+" "+k+"   "+circ[2]);
						// store a large dummy radius
						if (circ[2]>0){
							sw=true;
							for (z=0;z<m;z++){
								tttemp=(float)Math.sqrt(Math.pow(hx[z]-circ[0], 2.0) + Math.pow(hy[z]-circ[1], 2.0));
								if(tttemp>circ[2]){
									sw=false; // points are outside it
									break; // don't check any more points
								}
							}
							if(sw){ //no CH points outside
								// store radius & coordinates
								// IJ.log(""+i+ " "+j+" "+k+"   "+circ[2]);
								if (circ[2]<tD){
									tp1=i;
									tp2=j;
									tp3=k;
									tD=circ[2];
								}
							}
						}
					}
				}
			}
			op1[0]=hx[tp1];
			op1[1]=hy[tp1];
			op2[0]=hx[tp2];
			op2[1]=hy[tp2];
			op3[0]=hx[tp3];
			op3[1]=hy[tp3];
			// IJ.log("Solved for:"+tp1+ " "+tp2+" "+tp3);
			osculating(op1, op2, op3, circ);
			radius=circ[2];
			// IJ.log("Bounding circle (3 points) x: "+((float)circ[0])+", y: "+((float)circ[1])+", radius: "+ radius);
			results[1]=circ[0];
			results[2]=circ[1];
		}
		else{
			//2-point circle centred at cx, cy radius
			//IJ.log("Bounding circle (2 points) x: "+cx+", y: "+cy+", radius: "+ radius);
			results[1]=cx;
			results[2]=cy;
		}
		results[3]=radius;
	}

	
	void osculating( double [] pa, double [] pb, double [] pc, double [] centrad){
		// returns 3 double values: the centre (x,y) coordinates & radius
		// of the circle passing through 3 points pa, pb and pc
		double a, b, c, d, e, f, g;
		if ((pa[0]==pb[0] && pb[0]==pc[0]) || (pa[1]==pb[1] && pb[1]==pc[1])){ //colinear coordinates
			centrad[0]=-1; //x
			centrad[1]=-1; //y
			centrad[2]=-1; //radius
			return;
		}

		a = pb[0] - pa[0];
		b = pb[1] - pa[1];
		c = pc[0] - pa[0];
		d = pc[1] - pa[1];

		e = a*(pa[0] + pb[0]) + b*(pa[1] + pb[1]);
		f = c*(pa[0] + pc[0]) + d*(pa[1] + pc[1]);

		g = 2.0 * ( a * (pc[1] - pb[1]) - b * (pc[0] - pb[0]));
		// If g is 0 then the three points are colinear and no finite-radius
		// circle through them exists. Return -1 for the radius. Somehow this does not
		// work as it should (representation of double number?), so it is trapped earlier.
		centrad[0] = (d * e - b * f) / g;
		centrad[1] = (a * f - c * e) / g;
		centrad[2] =(float) Math.sqrt(Math.pow((pa[0] - centrad[0]),2) + Math.pow((pa[1] - centrad[1]),2));
	}


	void stats(int nc, Vector vg, float [] pars){
		// integral, min, max, mode, median, average, avg.deviation, std.deviation, variance, skewness, kurtosis, entropy
		int[] data = new int[nc];
		int i;
		float s = 0, min = Float.MAX_VALUE, max = -Float.MAX_VALUE, totl=0, ave=0, adev=0, sdev=0, var=0, skew=0, kurt=0, mode=0, median=0, p, q, msf=0, nco2 = (float)nc/2;
		double log2= Math.log(2), pr;
		for(i=0;i<nc;i++){
			data[i]= ((Integer)(vg.get(i))).intValue();
			totl+= data[i];
			if(data[i]<min) min = data[i];
			if(data[i]>max) max = data[i];
		}

		ave = totl/nc;

		for(i=0;i<nc;i++){
			s = data[i] - ave;
			adev+=Math.abs(s);
			p = s * s;
			var+= p;
			p*=s;
			skew+= p;
			p*= s;
			kurt+= p;
		}

		adev/= nc;
		var/=nc-1;
		sdev = (float) Math.sqrt(var);

		if(var> 0){
			skew = (float)skew / (nc * (float) Math.pow(sdev,3));
			kurt = (float)kurt / (nc * (float) Math.pow(var, 2)) - 3;
		}
		q=0;
		pr=0;

		int[] hist = new int[(max<10?10:(int)max+1)];
		for(i=0;i<nc;i++){
			hist[data[i]]++; // load histogram
		}
		pars[12]=hist[2];// endpoint
		pars[13]=hist[3];// double point
		pars[14]=hist[4];// triple point
		pars[15]=hist[5];// 4-point
		pars[16]=hist[6];// 5-point
		pars[17]=hist[7];// 6-point
		pars[18]=hist[8];// 7-point
		pars[19]=hist[9];// 8-point
		
		

		for (i=0;i<=max;i++){
			if (hist[i]> msf){
				msf=hist[i];
				mode=i;
			}
			if (hist[i]>0)
				pr+=((double)hist[i]/(double)nc)*(Math.log((double)hist[i]/(double)nc)/log2);

			q+=hist[i];
			if (q<=nco2)
				median=i; //simple median
		}
		if (min==max)
			median=max;// median in a single bin case

		if (pr<0)
			pr=-pr;

		pars[0]=totl;
		pars[1]= min;
		pars[2]= max;
		pars[3]= mode;
		pars[4]= median;
		pars[5]=ave;
		pars[6]=adev;
		pars[7]=sdev;
		pars[8]=var;
		pars[9]=skew;
		pars[10]=kurt;
		pars[11]=(float)pr;
	}

	void stats32(int nc, Vector vg, float [] pars){
		// integral, min, max, mode, median, average, avg.deviation, std.deviation, variance, skewness, kurtosis, entropy
		float[] data = new float[nc];
		int i;
		float s = 0, min = Float.MAX_VALUE, max = -Float.MAX_VALUE, totl=0, ave=0, adev=0, sdev=0, var=0, skew=0, kurt=0, p;
		double log2= Math.log(2), pr;
		for(i=0;i<nc;i++){
			data[i]= ((Float)(vg.get(i))).floatValue();
			totl+= data[i];
			if(data[i]<min) min = data[i];
			if(data[i]>max) max = data[i];
		}

		ave = totl/nc;

		for(i=0;i<nc;i++){
			s = data[i] - ave;
			adev+=Math.abs(s);
			p = s * s;
			var+= p;
			p*=s;
			skew+= p;
			p*= s;
			kurt+= p;
		}

		adev/= nc;
		var/=nc-1;
		sdev = (float) Math.sqrt(var);

		if(var> 0){
			skew = (float)skew / (nc * (float) Math.pow(sdev,3));
			kurt = (float)kurt / (nc * (float) Math.pow(var, 2)) - 3;
		}

		pars[0]=totl;
		pars[1]= min;
		pars[2]= max;
		pars[3]= -1;
		pars[4]= -1;
		pars[5]=ave;
		pars[6]=adev;
		pars[7]=sdev;
		pars[8]=var;
		pars[9]=skew;
		pars[10]=kurt;
		pars[11]=-1;
	}

}

	
