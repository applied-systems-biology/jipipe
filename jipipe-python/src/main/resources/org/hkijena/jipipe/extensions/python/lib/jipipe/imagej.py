# -*- coding: utf-8 -*-

"""
This file provides functions to read/write ImageJ data types

Zoltán Cseresnyés, Ruman Gerst

Research Group Applied Systems Biology - Head: Prof. Dr. Marc Thilo Figge
https://www.leibniz-hki.de/en/applied-systems-biology.html
HKI-Center for Systems Biology of Infection
Leibniz Institute for Natural Product Research and Infection Biology - Hans Knöll Institute (HKI)
Adolf-Reichwein-Straße 23, 07745 Jena, Germany

The project code is licensed under BSD 2-Clause.
See the LICENSE file provided with the code for the full license.
"""

from jipipe.data_slot import DataSlot
from pathlib import Path


def get_image_file(data_slot: DataSlot, row: int):
    """
    Finds the image file located in imagej-imgplus-* data slot rows
    :param data_slot: the data slot
    :param row: the row
    :return: path to the image file or None if not found
    """
    row_storage_folder = data_slot.get_row_storage_path(row)
    files = []
    for extension in ("*.tif", "*.tiff", "*.png", "*.jpg", "*.jpeg", "*.bmp"):
        files.extend(row_storage_folder.glob(extension))
    return files[0] if len(files) > 0 else None


def get_table_file(data_slot: DataSlot, row: int = 0):
    """
    Finds the CSV table file located in imagej-results-table (and related) data slow rows
    :param data_slot: the data slot
    :param row: the row
    :return: path to the CSV file or None if not found
    """
    row_storage_folder = data_slot.get_row_storage_path(row)
    files = list(row_storage_folder.glob("*.csv"))
    return files[0] if len(files) > 0 else None


def load_image_file(data_slot: DataSlot, row: int = 0):
    """
    Finds and loads the image file located in imagej-imgplus-* data slot rows and loads it with Skimage.
    Requires that Skimage is installed.
    :param data_slot: the data slot
    :param row: the row
    :return: Image data or None if no image was found
    """
    from skimage.io import imread
    file = get_image_file(data_slot, row)
    print("Loading image from " + str(file))

    # Conversion to string here due to https://github.com/scikit-image/scikit-image/issues/4138
    return imread(fname=str(file)) if file is not None else None


def load_table_file(data_slot: DataSlot, row: int = 0):
    """
    Finds and loads the CSV table file in imagej-results-table (and related) data slow rows as pandas data frame
    Requires that pandas is installed.
    :param data_slot: the data slot
    :param row: the row
    :return: Image data or None if no image was found
    """
    from pandas import read_csv
    file = get_table_file(data_slot, row)
    print("Loading table from " + str(file))
    return read_csv(filepath_or_buffer=file) if file is not None else None


def add_table(table, data_slot: DataSlot, text_annotations: dict = None, data_annotations: dict = None):
    """
    Adds a new table into a new row of the specified slot
    :param table: the table. must be a Pandas table or dictionary that can be converted into a data frame
    :param data_slot: the data slot
    :param text_annotations: optional annotations (a dict of string keys and string values)
    :param data_annotations: optional annotations (a dict of string keys and dict values that contain true-data-type and row-storage-folder)
    :return: index of the newly added row
    """
    row = data_slot.add_row(text_annotations=text_annotations, data_annotations=data_annotations)
    row_storage_path = data_slot.get_row_storage_path(row)
    file_name = row_storage_path / Path("data.csv")
    print("Writing table to " + str(file_name))
    from pandas import DataFrame
    if type(table) is DataFrame:
        table.to_csv(file_name)
    else:
        DataFrame(data=table).to_csv(file_name)
    return row


def add_image(data, data_slot: DataSlot, text_annotations: dict = None, data_annotations: dict = None, convert_64_to_32=True, imagej=True, **kwargs):
    """
    Adds a new image into a new row of the specified slot. The image will be saved as TIFF.
    Requires tifffile. This method accepts all parameters utilized by tifffile.imsave
    :param convert_64_to_32: If enabled, convert 64-bit float to 32-bit float. ImageJ does not support 64 bit
    :param data: an image. must be a numpy array. Can be None. In this case, you must provide shape and dtype (like tifffile's method requires)
    :param data_slot: the data slot
    :param text_annotations: optional annotations (a dict of string keys and string values)
    :param data_annotations: optional annotations (a dict of string keys and dict values that contain true-data-type and row-storage-folder)
    :param imagej: if the generated output should have the necessary metadata for ImageJ loading
    :return: index of the newly added row
    """
    import numpy as np
    if convert_64_to_32 and data.dtype is np.dtype("float64"):
        data = np.float32(data)
    row = data_slot.add_row(text_annotations=text_annotations, data_annotations=data_annotations)
    row_storage_path = data_slot.get_row_storage_path(row)
    file_name = row_storage_path / Path("data.tif")
    from tifffile import imwrite
    print("Writing image to " + str(file_name))
    imwrite(file=str(file_name), data=data, imagej=imagej, **kwargs)
    return row
