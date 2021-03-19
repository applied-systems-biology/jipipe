from jipipe import data_slot
from jipipe.imagej import *
from pathlib import Path
import jipipe.data_slot

if __name__ == '__main__':
    # ds = data_slot.import_from_folder("E:\\Projects\\tmp\\Neuer Ordner\\Image")
    ds = data_slot.import_from_folder("E:\\Projects\\tmp\\Find_Particles\\Measurements")
    target = data_slot.DataSlot(data_type=ds.data_type, storage_path=Path("E:\\Projects\\tmp\\Output"))
    for row in range(ds.rows):
        target.copy_row(ds, row)
    target.save(with_csv=True)
    # print(load_table_file(ds, 0))
    # print(ds.to_dict())
