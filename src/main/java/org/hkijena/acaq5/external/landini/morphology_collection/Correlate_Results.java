package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.*;
import ij.gui.*;
import ij.plugin.PlugIn;
import ij.measure.*;
import java.util.*;
import ij.util.*;
import java.awt.*;
//import java.io.*;

// Correlate_Results.txt
// Copyright by G. Landini 17/Dec/2005
// 19/Sep/2006 added incremental filling if empty

// Note that this plugin runs only if:
//   1. there is a Results Table currently on memory with particle parameters
//
// Send feedback & comments to:
// G. Landini at bham. ac. uk


public class Correlate_Results implements PlugIn {

	public void run(String arg) {

		ResultsTable rt=ResultsTable.getResultsTable();
		int count = rt.getCounter();
		if (count==0) {
			IJ.error("Error", "The \"Results\" table is empty");
			return;
		}
		

		String head= rt.getColumnHeadings();
		StringTokenizer t = new StringTokenizer(head, "\t");
		int tokens = t.countTokens(); //one more for "empty"
		String[] strings = new String[tokens];
		//String[] criterium = {"none","1","2","3","4","5","6","7","8","9"};
        String[] fitList = {"Straight Line", "2nd Degree Polynomial", "3rd Degree Polynomial", "4th Degree Polynomial", "Exponential", "Power", "log", "Rodbard", "Gamma Variate", "y = a+b*ln(x-c)", "Rodbard (NIH Image)"};

		String[] fList = {"y = a+bx","y = a+bx+cx^2", "y = a+bx+cx^2+dx^3", "y = a+bx+cx^2+dx^3+ex^4","y = a*exp(bx)","y = ax^b","y = a*ln(bx)", "y = d+(a-d)/(1+(x/c)^b)", "y = a*(x-b)^c*exp(-(x-b)/d)", "y = a+b*ln(x-c)", "y = d+(a-d)/(1+(x/c)^b)",};

		String[] grph = {"Circles","Circles & Line","Line"};

		String[] set = new String[2];

		int i;

		//strings[0] = t.nextToken(); // first token is empty?
	   	for(i=0; i<tokens; i++){
			strings[i] = t.nextToken();
		}

		strings[0] = "-empty-";
		GenericDialog gd = new GenericDialog("Correlate Results", IJ.getInstance());
		gd.addMessage("If -empty-, the array is filled form 1..n");
		gd.addChoice("X axis", strings, strings[0]);
		gd.addChoice("Y axis", strings, strings[0]);
		//gd.addChoice("Order", criterium, criterium[0]);
		gd.addChoice("Equation", fitList, fitList[0]);
		gd.addCheckbox("Show fitted line", true);
		gd.addChoice("Graph", grph, grph[0]);



		gd.showDialog();
		if (gd.wasCanceled())
			return;

		set[0] = gd.getNextChoice();
		set[1] = gd.getNextChoice();

		//String order = gd.getNextChoice();
		int fitterCurve = gd.getNextChoiceIndex();
		boolean showfit = gd.getNextBoolean();
		String gtype = gd.getNextChoice();

		if (set[0].equals("-empty-") && set[1].equals("-empty-"))
			return;

		int nc=rt.getCounter();
		double[] datax = new double[nc];
		double[] datay = new double[nc];

		for (i=0;i<nc;i++){ 
			if (set[0].equals("-empty-"))
				datax[i]=i+1;
			else
				datax[i] = rt.getValue(set[0],i);

			if (set[1].equals("-empty-"))
				datay[i]=i+1;	
			else
				datay[i] = rt.getValue(set[1],i);
			//IJ.log(""+ datax[i]+"  "+datay[i]);
		}

		CurveFitter cf = new CurveFitter(datax, datay);
		cf.setMaxIterations(20000);
		cf.setRestarts(50);
		cf.doFit(fitterCurve);
		double[] p = cf.getParams();
		int nP = cf.getNumParams();
		if (showfit){
			IJ.log(fitList[fitterCurve]+" Fitting.");
			IJ.log("x axis: "+set[0]+",  y axis: "+set[1]);
			IJ.log(fList[fitterCurve]);
			IJ.log(cf.getResultString());
			IJ.log("------");
			IJ.log(" ");
		}

		float[] px = new float[100];
		float[] py = new float[100];
		double[] a = Tools.getMinMax(datax);
		double xmin=a[0], xmax=a[1]; 
		a = Tools.getMinMax(datay);
		double ymin=a[0], ymax=a[1]; 
		double inc = (xmax-xmin)/99.0;
		double tmp = xmin;
		for (i=0; i<100; i++) {
			px[i]=(float)tmp;
			tmp += inc;
		}
		for (i=0; i<100; i++)
			py[i] = (float)CurveFitter.f(fitterCurve, p, px[i]);
		a = Tools.getMinMax(py);
		ymin = Math.min(ymin, a[0]);
		ymax = Math.max(ymax, a[1]);

		//PlotWindow plot = new PlotWindow("Scatter Plot",set[0],set[1], px, py);
		PlotWindow plot = new PlotWindow("Scatter Plot", set[0], set[1], new double[1], new double[1]);

		plot.setLimits(xmin,xmax,ymin,ymax);

		if (gtype.equals("Circles & Line") || gtype.equals("Line")){
			plot.setColor(Color.gray);
			plot.addPoints(datax,datay,PlotWindow.LINE);
		}
		if (gtype.equals("Circles & Line") || gtype.equals("Circles")){
			plot.setColor(Color.red);
			plot.addPoints(datax,datay,PlotWindow.CIRCLE);
		}
		
		if (showfit){
			plot.setColor(Color.black);
			plot.addLabel(0.3, 0, fitList[fitterCurve]+" Fitting");
			plot.setColor(Color.blue);
			plot.addPoints(px,py,PlotWindow.LINE);
		}
		plot.draw();
	}

}
