# This code is based on MultiTemplateMatching-Fiji/Fiji.app/scripts/Plugins/Multi-Template-Matching/Template_Matching_Folder.py in https://github.com/multi-template-matching/MultiTemplateMatching-Fiji.git
# It will be run by JIPipe's Python interpreter

from Template_Matching.MatchTemplate_Module import getHit_Template, CornerToCenter
from Template_Matching.NonMaximaSupression_Py2 import NMS
from ij import IJ
from ij.gui import Roi
from utils import AddToTable

# List_Template is set externally and contains the templates
# ImpImage is set externally and contains the image
# Method is set externally and contains the numeric method id
# flipv is set externally
# fliph is set externally
# angles is set externally
# n_hit is set externally
# max_overlap is set externally
# score_threshold is set externally
# tolerance is set externally
# rm is set externally and contains a ROIListData
# Table is set externally and contains the ResultsTable
# Bool_SearchRoi is set externally
# searchRoi is set externally
# algorithmProgress is set externally
# subProgress is set externally

progress.log("Templates to process: " + str(len(List_Template)))
progress.log("Input image: " + str(ImpImage))
progress.log("Parameter flipv: " + str(flipv))
progress.log("Parameter fliph: " + str(fliph))
progress.log("Angles to test: " + str(angles))
progress.log("Expected number of objects: " + str(n_hit))
progress.log("Score threshold: " + str(score_threshold))
progress.log("tolerance: " + str(tolerance))
progress.log("Scoring method: " + str(Method))

# Define offset
if searchRoi:
    Bool_SearchRoi = True
    dX = int(searchRoi.getXBase())
    dY = int(searchRoi.getYBase())
else:
    Bool_SearchRoi = False
    dX = dY = 0

ImName = ImpImage.getTitle()

# Crop Image if searchRoi
if Bool_SearchRoi:
    ImpImage.setRoi(searchRoi)
    ImpImage = ImpImage.crop()

# We will not apply cropping. Users can do this via the cropping node.

Hits_BeforeNMS = []

for index, ImpTemplate in enumerate(List_Template):

    progress.log("Template " + str(index + 1) + " / " + str(len(List_Template)))

    # Check that template is smaller than the searched image or ROI
    if Bool_SearchRoi and (
            ImpTemplate.height > searchRoi.getFloatHeight() or ImpTemplate.width > searchRoi.getFloatWidth()):
        progress.log(
            "The template " + ImpTemplate.getTitle() + " is larger in width and/or height than the search region -> skipped")
        continue  # go directly to the next for iteration
    elif ImpTemplate.width > ImpImage.width or ImpTemplate.height > ImpImage.height:
        progress.log(
            "The template " + ImpTemplate.getTitle() + " is larger in width and/or height than the searched image-> skipped")
        continue  # go directly to the next for iteration

    # Get hits for the current template (and his flipped and/or rotated versions)
    List_Hit = getHit_Template(ImpTemplate, ImpImage, flipv, fliph, angles, Method, n_hit, score_threshold,
                               tolerance)  # raher use ImagePlus as input to get the name of the template used

    # Store the hits
    Hits_BeforeNMS.extend(List_Hit)

### NMS ###
# print "\n-- Hits before NMS --\n",
# for hit in Hits_BeforeNMS: print hit

# InterHit NMS if more than one hit
if with_nms:
    progress.log("Hits before NMS: " + str(len(Hits_BeforeNMS)))
    progress.log("Non-Maxima-Suppression")

    if Method in [0, 1]:
        Hits_AfterNMS = NMS(Hits_BeforeNMS, N=n_hit, maxOverlap=max_overlap,
                            sortDescending=False)  # only difference is the sorting

    else:
        Hits_AfterNMS = NMS(Hits_BeforeNMS, N=n_hit, maxOverlap=max_overlap, sortDescending=True)
else:
    progress.log("Non-Maxima-Suppression was skipped via parameter")
    Hits_AfterNMS = Hits_BeforeNMS

# print "\n-- Hits after NMS --\n"
# for hit in Hits_AfterNMS : print hit
progress.log("Hits after NMS: " + str(len(Hits_AfterNMS)))

for hit in Hits_AfterNMS:

    # progress.log(str(hit))
    # print hit

    if Bool_SearchRoi:  # Add offset of search ROI
        hit['BBox'] = (hit['BBox'][0] + dX, hit['BBox'][1] + dY, hit['BBox'][2], hit['BBox'][3])

        # Create detected ROI
    roi = Roi(*hit['BBox'])
    roi.setName(hit['TemplateName'])

    rm.add(roi)

    Xcorner, Ycorner = hit['BBox'][0], hit['BBox'][1]
    Xcenter, Ycenter = CornerToCenter(Xcorner, Ycorner, hit['BBox'][2], hit['BBox'][3])

    Dico = {'Image': ImName, 'Template': hit['TemplateName'], 'Xcorner': Xcorner, 'Ycorner': Ycorner,
            'Xcenter': Xcenter, 'Ycenter': Ycenter, 'Score': hit['Score']}
    Dico['Roi Index'] = rm.size()
    AddToTable(Table, Dico,
               Order=("Image", "Template", "Score", "Roi Index", "Xcorner", "Ycorner", "Xcenter", "Ycenter"))
