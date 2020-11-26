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
# score_threshold is set externally
# tolerance is set externally
# rm is set externally and contains a RoiManager
# Table is set externally and contains the ResultsTable
# Bool_SearchRoi is set externally
# searchRoi is set externally
# algorithmProgress is set externally
# subProgress is set externally

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
        IJ.log(
            "The template " + ImpTemplate.getTitle() + " is larger in width and/or height than the search region -> skipped")
        continue  # go directly to the next for iteration
    elif ImpTemplate.width > ImpImage.width or ImpTemplate.height > ImpImage.height:
        IJ.log(
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
progress.log("Hits before NMS: " + str(len(Hits_BeforeNMS)))
progress.log("Non-Maxima-Suppression")

if Method in [0, 1]:
    Hits_AfterNMS = NMS(Hits_BeforeNMS, N=n_hit, maxOverlap=max_overlap,
                        sortDescending=False)  # only difference is the sorting

else:
    Hits_AfterNMS = NMS(Hits_BeforeNMS, N=n_hit, maxOverlap=max_overlap, sortDescending=True)

# print "\n-- Hits after NMS --\n"
# for hit in Hits_AfterNMS : print hit
progress.log("Hits after NMS: " + str(len(Hits_AfterNMS)))

for hit in Hits_AfterNMS:

    # print hit

    if Bool_SearchRoi:  # Add offset of search ROI
        hit['BBox'] = (hit['BBox'][0] + dX, hit['BBox'][1] + dY, hit['BBox'][2], hit['BBox'][3])

        # Create detected ROI
    roi = Roi(*hit['BBox'])
    roi.setName(hit['TemplateName'])

    rm.addRoi(roi)

    Xcorner, Ycorner = hit['BBox'][0], hit['BBox'][1]
    Xcenter, Ycenter = CornerToCenter(Xcorner, Ycorner, hit['BBox'][2], hit['BBox'][3])

    Dico = {'Image': ImName, 'Template': hit['TemplateName'], 'Xcorner': Xcorner, 'Ycorner': Ycorner,
            'Xcenter': Xcenter, 'Ycenter': Ycenter, 'Score': hit['Score']}
    Dico['Roi Index'] = rm.getCount()
    AddToTable(Table, Dico,
               Order=("Image", "Template", "Score", "Roi Index", "Xcorner", "Ycorner", "Xcenter", "Ycenter"))
