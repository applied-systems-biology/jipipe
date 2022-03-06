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

import json
from pathlib import Path

from jipipe.data_slot import DataSlot


def get_path(data_slot: DataSlot, row: int = 0) -> str:
    """
    Gets a path data from a data slot
    :param data_slot: the data slot
    :param row: the row
    :return: path data as string
    """
    row_storage_folder = data_slot.get_row_storage_path(row)
    files = list(row_storage_folder.glob("*.json"))

    with open(files[0], "r") as f:
        json_data = json.load(f)

    return json_data["path"]


# Aliases
get_file = get_path
get_folder = get_path


def add_path(path, data_slot: DataSlot, text_annotations: dict = None, data_annotations: dict = None, data_type_id="path"):
    """
    Adds a new path into a new row of the specified slot
    :param data_type_id: The ID of the generated path data type
    :param path: a path-like object
    :param data_slot: the data slot
    :param text_annotations: optional annotations (a dict of string keys and string values)
    :param data_annotations: optional annotations (a dict of string keys and dict values that contain true-data-type and row-storage-folder)
    :return: index of the newly added row
    """
    row = data_slot.add_row(text_annotations=text_annotations, data_annotations=data_annotations)
    row_storage_path = data_slot.get_row_storage_path(row)
    file_name = row_storage_path / Path("data.json")
    print("Writing path data to " + str(file_name))

    with open(file_name, "w") as f:
        json.dump({
            "path": str(path),
            "jipipe:data_type": str(data_type_id)
        }, f)

    return row


def add_folder(path, data_slot: DataSlot, text_annotations: dict = None, data_annotations: dict = None):
    """
   Adds a new path into a new row of the specified slot
   :param path: a path-like object
   :param data_slot: the data slot
   :param text_annotations: optional annotations (a dict of string keys and string values)
   :param data_annotations: optional annotations (a dict of string keys and dict values that contain true-data-type and row-storage-folder)
   :return: index of the newly added row
   """
    add_path(path, data_slot=data_slot, text_annotations=text_annotations, data_annotations=data_annotations, data_type_id="folder")


def add_file(path, data_slot: DataSlot, text_annotations: dict = None, data_annotations: dict = None):
    """
   Adds a new path into a new row of the specified slot
   :param path: a path-like object
   :param data_slot: the data slot
   :param text_annotations: optional annotations (a dict of string keys and string values)
   :param data_annotations: optional annotations (a dict of string keys and dict values that contain true-data-type and row-storage-folder)
   :return: index of the newly added row
   """
    add_path(path, data_slot=data_slot, text_annotations=text_annotations, data_annotations=data_annotations, data_type_id="file")
