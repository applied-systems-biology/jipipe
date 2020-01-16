package org.hkijena.acaq5.external.landini.morphology_collection;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.process.ImageStatistics;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.StringTokenizer;

// Classify_Particles.txt
// Copyright by G. Landini 4/Dec/2005
//
// Requires BinaryReconstruct plugin.
// This plugin classifies particles according to parameters
// extracted by other plugins and which are stored in the Results Table
// (including the built-in Particle_analyzer, Particles4, Particles8
// and Particles8_Plus).
// The plugin generates and runs automatically a macro stored in 
// the 'macros' folder and produces an image or stack called "Subset" 
// with the results of the classification.
// The classification test is added as metadata to the Subset image
// for future reference.
// The macro will be overwritten each time the plugin is run,
// so save it with a differnt name if you wish to keep it.
//
// Note that this plugin runs only if:
//   1. the analysed image is binary (0 & 255)
//   2. there is a Results Table currently on memory with particle parameters
//   3. the Slice number has been recorded (even for single images).
//   4. the column names in the Results Table do NOT have clashing names
//      after non-word characters have been removed (eg: "Circ." and "C-ir*c"
//      have clashing names).
//   5. the particle starts have been recorded.
//
// Send feedback & comments to:
// G. Landini at bham. ac. uk

// v1.1 bug in grey memmbers
// v1.2 7/June/2007 Jarek Sacha kindly provided 2 fixes:
//   1. Early error message when the Classify_Particles.txt cannot be saved,
//   showing full path of attempted save.
//   2. When current image has spaces in the name the macro was failing in 
//   the call to Binary_Reconstruct (added [ ] around the file name).
// v1.3  30/June/2014 File separator instead of "/"
// v1.4  fixed stack processing (binary reconstruction does not process stacks directly)

public class Classify_Particles implements PlugIn {
    private File outputFile;
    private FileOutputStream out = null;

    public void run(String arg) {

        ImagePlus imp = WindowManager.getCurrentImage();
        if (imp == null) {
            IJ.error("Error", "No image!.\nPlease open a binary (8-bit) image.");
            return;
        }


        ResultsTable rt = ResultsTable.getResultsTable();
        int count = rt.getCounter();
        if (count == 0) {
            IJ.error("Error", "The \"Results\" table is empty.\nRun the \"Particle Analyzer\" or the \"Particles8_Plus\" plugin\n(or any other) to generate the classifier data.");
            return;
        }

        ImageStatistics stats;
        stats = imp.getStatistics();
        if (stats.histogram[0] + stats.histogram[255] != stats.pixelCount) {
            IJ.error("Error", "8-bit binary image (0 & 255) required.\nPlease threshold the image.");
            return;
        }

        String head = rt.getColumnHeadings();
        StringTokenizer t = new StringTokenizer(head, "\t");
        int tokens = t.countTokens(); //one more for "empty"

        String[] strings = new String[tokens];
        String[] operator = {"-empty-", ">", ">=", "==", "<=", "<", "!="};
        String[] criterium = {"AND (match all)", "OR (match any)"};
        String[] process = {"Keep members", "Grey non-members", "Grey members", "Delete members"};
        String[] set = new String[4];
        String[] op = new String[4];
        double[] val = new double[4];
        String[] macrostring = new String[16];
        String finalstring = "";
        boolean isslice = false, isstart = false, isname = false;
        int i, j;

        strings[0] = t.nextToken(); // first token is empty?
        for (i = 1; i < tokens; i++) {
            strings[i] = t.nextToken();
            //IJ.log(strings[i]);
            if (strings[i].equals("Slice")) isslice = true;
            if (strings[i].equals("XStart")) isstart = true;
        }

        if (!isslice) {
            IJ.error("Error", "The \'Slice\' column is missing from the Results Table.\n \nIf you used the Particle Analyzer, please run it again\nmaking sure that the Slice option has been checked\nin the Set Measurements dialog.");
            return;
        }

        if (!isstart) {
            IJ.error("Error", "The \'XStart\' & \'YStart\' columns are missing from the Results Table.\n \nIf you used the Particle Analyzer, please run it again making \nsure that the \'Record Starts\' option has been checked in the\nAnalyze Particles dialog.");
            return;
        }

        for (i = 0; i < tokens - 1; i++) {
            for (j = i + 1; j < tokens; j++) {
                if (strings[i].replaceAll("\\W", "").equals(strings[j].replaceAll("\\W", ""))) {
                    isname = true;
                }
            }
        }

        if (isname) {
            IJ.error("Error", "The column names clash after removing non-word characters.\n \nContact the plugin author. See the source code for details.");
            return;
        }

        strings[0] = "-empty-";
        GenericDialog gd = new GenericDialog("Classify", IJ.getInstance());
        gd.addMessage("----- Classify particles -----");
        gd.addChoice("Class[1]", strings, strings[0]);
        gd.addChoice("Operator[1]", operator, operator[0]);
        gd.addNumericField("Value[1]", 0, 4);
        gd.addMessage("-----");
        gd.addChoice("Class[2]", strings, strings[0]);
        gd.addChoice("Operator[2]", operator, operator[0]);
        gd.addNumericField("Value[2]", 0, 4);
        gd.addMessage("-----");
        gd.addChoice("Class[3]", strings, strings[0]);
        gd.addChoice("Operator[3]", operator, operator[0]);
        gd.addNumericField("Value[3]", 0, 4);
        gd.addMessage("-----");
        gd.addChoice("Class[4]", strings, strings[0]);
        gd.addChoice("Operator[4]", operator, operator[0]);
        gd.addNumericField("Value[4]", 0, 4);
        //gd.addMessage("-----");

        gd.addCheckbox("White particles", true);
        gd.addChoice("Combine", criterium, criterium[0]);
        gd.addChoice("Output", process, process[0]);

        gd.showDialog();
        if (gd.wasCanceled())
            return;

        set[0] = gd.getNextChoice();
        op[0] = gd.getNextChoice();
        val[0] = gd.getNextNumber();

        set[1] = gd.getNextChoice();
        op[1] = gd.getNextChoice();
        val[1] = gd.getNextNumber();

        set[2] = gd.getNextChoice();
        op[2] = gd.getNextChoice();
        val[2] = gd.getNextNumber();

        set[3] = gd.getNextChoice();
        op[3] = gd.getNextChoice();
        val[3] = gd.getNextNumber();

        String combine = gd.getNextChoice();
        String result = gd.getNextChoice();

        boolean white = gd.getNextBoolean();

        j = 0;
        if (combine.equals("AND (match all)")) {
            for (i = 0; i < 4; i++) {
                if (!set[i].equals("-empty-") && !op[i].equals("-empty-")) {
                    macrostring[j] = "(" + (set[i].replaceAll("\\W", "")) + " ";
                    macrostring[j + 1] = op[i] + " ";
                    macrostring[j + 2] = Double.toString(val[i]) + ")";
                    macrostring[j + 3] = " && ";
                    j += 4;
                }
            }
        } else {
            for (i = 0; i < 4; i++) {
                if (!set[i].equals("-empty-") && !op[i].equals("-empty-")) {
                    macrostring[j] = "(" + (set[i].replaceAll("\\W", "")) + " ";
                    macrostring[j + 1] = op[i] + " ";
                    macrostring[j + 2] = Double.toString(val[i]) + ")";
                    macrostring[j + 3] = " || ";
                    j += 4;
                }
            }
        }

        for (i = 0; i < j - 1; i++)
            finalstring = finalstring + macrostring[i];
        //IJ.log(finalstring);

        if (finalstring.equals("")) {
            IJ.error("Error", "No classes set!");// avoid no-test macro
        } else {
            String sname = "Particle_Classifier.ijm";
            // open file
            outputFile = new File(IJ.getDirectory("macros")+ File.separator + sname).getAbsoluteFile();

            try {
                out = new FileOutputStream(outputFile);
            } catch (FileNotFoundException e) {
                IJ.error("Cannot create macro file. " + e.getMessage());
                return;
            }

            try {
                //-------------
                writeString("// Particle_Classifier.txt\n");
                writeString("// This macro is automatically generated and run by the\n");
                writeString("// Classify_Particles plugin and will be overwritten each\n");
                writeString("// time the plugin is run.\n");
                writeString("// G. Landini 8/Dec/2005, 2014.\n");
                writeString("// Note that this macro runs succesfully only if:\n");
                writeString("//   1. the analysed image is binary (0 & 255),\n");
                writeString("//   2. there is a Results Table currently on memory with,\n");
                writeString("//      with particle parameters,\n");
                writeString("//   3. the Slice number is recorded in the Results Table\n");
                writeString("//      regardless of whether it is a stack or a single image,\n");
                writeString("//   4. the column names in the Results Table do NOT have clashing\n");
                writeString("//      names after removal of non-alphanumeric characters (for\n");
                writeString("//      example: \"Circ.\" and \"C-ir*c\", have clashing names), and\n");
                writeString("//   5. the particle Start coordinates (i.e. XStart, YStart) have\n");
                writeString("//      been recorded.\n");
                writeString("//------\n");
                writeString("t=getTitle();\n");
                writeString("slices=nSlices;\n");
                writeString("run(\"Duplicate...\", \"title=Subset duplicate\");\n");
                writeString("setBatchMode(true);\n");
                writeString("selectWindow(\"Subset\");\n");
                writeString("thisSlice=0;\n");
                writeString("setPasteMode(\"Copy\");\n");
                writeString("for (i=0; i<nResults; i++) {\n");
                writeString("  s = getResult(\'Slice\', i);\n");
                writeString("  if (s > thisSlice){\n");
                writeString("    setSlice(s);\n");
                writeString("    setMetadata(\"" + finalstring + " " + result + "\");\n");
                if (white)
                    writeString("run(\"Set...\", \"value=0\");\n");
                else
                    writeString("run(\"Set...\", \"value=255\");\n");
                writeString("    thisSlice=s;\n");
                writeString("  }\n");
                writeString("  x = getResult(\'XStart\', i);\n");
                writeString("  y = getResult(\'YStart\', i);\n");
                for (i = 0; i < 4; i++) {
                    if (!set[i].equals("-empty-") && !op[i].equals("-empty-")) {
                        writeString("  " + (set[i].replaceAll("\\W", "")) + " = getResult(\'" + set[i] + "\', i);\n");
                    }
                }
                writeString("  if");
                writeString("  (" + finalstring + "){\n");
                if (white)
                    writeString("  setPixel (x,y,255); // set start coordinates\n");
                else
                    writeString("  setPixel (x,y,0); // set start coordinates\n");
                writeString("  }\n");
                writeString("}\n");
                writeString("for (i=1; i<=slices; i++) {\n");
                writeString("  selectWindow(t);\n");
                writeString("  setSlice(i);\n");
                writeString("  run(\"Duplicate...\", \"title=_MASK_\");\n");
                writeString("  selectWindow(\"Subset\");\n");
                writeString("  setSlice(i);\n");
                writeString("  run(\"Duplicate...\", \"title=_SEED_\");\n");
                writeString("  // Reconstruct based on start coordinates of particles passing the test\n");
                if (white)
                   writeString("   run(\"BinaryReconstruct \", \"mask=[_MASK_] seed=[_SEED_] white\");\n");
                else
                   writeString("   run(\"BinaryReconstruct \", \"mask=[_MASK_] seed=[_SEED_]\");\n");
                writeString("   selectWindow(\"_SEED_\");\n");
                writeString("   run(\"Copy\");\n");
                writeString("   close();\n");
                writeString("   selectWindow(\"Subset\");\n");
                writeString("   run(\"Paste\");\n");
                writeString("   run(\"Select None\");\n");
                writeString("   selectWindow(\"_MASK_\");\n");
                writeString("   close();\n");
                writeString("}\n");
                writeString("selectWindow(\"Subset\");\n");
                writeString("setSlice(1);\n");
                writeString("selectWindow(t);\n");
                writeString("setSlice(1);\n");
                writeString("setBatchMode(false);\n");

                // Higlight ("Keep members"?, do nothing)
                if (result.equals("Delete members")) {
                    writeString("imageCalculator(\"XOR stack\", \"Subset\", t);\n");
                }
                if (result.equals("Grey members")) {
                    writeString("imageCalculator(\"XOR stack\", \"Subset\", t);\n");
                    writeString("imageCalculator(\"Average stack\", \"Subset\", t);\n");
                }
                if (result.equals("Grey non-members")) {
                    writeString("imageCalculator(\"Average stack\", \"Subset\", t);\n");
                }
                //--------------
            }
            catch (IOException e) {
                IJ.error("Cannot write macro file. " + e.getMessage());
                return;
            } finally {
                try {
                    out.close();
                } catch (IOException e) {
                    IJ.log("Error closing macro file.");
                }
            }
            IJ.runMacroFile(outputFile.getAbsolutePath());
        }
    }

    void writeString(String s) throws IOException {
        byte[] bytes = s.getBytes("UTF-8");
        out.write(bytes);
    }
}

//(1 && 2 && 3 && 4)
//(1 && 2 && 3) || 4
//(1 && 2) && (3 || 4)
//(1 && 2) || (3 && 4)
//(1 || 2) && (3 || 4)
//(1 || 2) || (3 && 4)
//(1 || 2 || 3) && 4
//(1 || 2 || 3 || 4)
