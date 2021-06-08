"""
Copyright by Jan-Philipp_Praetorius

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo
Figge
https://www.leibniz-hki.de/en/applied-systems-biology.html
HKI-Center for Systems Biology of Infection
Leibniz Institute for Natural Product Research and Infection Biology -
Hans Knöll Insitute (HKI)
Adolf-Reichwein-Straße 23, 07745 Jena, Germany
"""

from __future__ import print_function

#############################################################
#             		    utils functions                     #
#############################################################

"""
slide over the specified input image
"""


def sliding_window(img, stepSize, windowSize=(256, 256)):
    # slide a window across the image
    for y in range(0, img.shape[0], stepSize):
        for x in range(0, img.shape[1], stepSize):
            # yield the current window, cause of reduce memory costs
            yield (x, y, img[y:y + windowSize[1], x:x + windowSize[0]])
