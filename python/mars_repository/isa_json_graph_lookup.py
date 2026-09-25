from typing import Dict, List, Optional

from .models.isa.data_file import DataFile
from .models.isa.materials import Materials
from .models.isa.other_material import OtherMaterial
from .models.isa.output import Output
from .models.isa.process_sequence import ProcessSequence

DATA_FILE_ID_PREFIX = "#data_file/"
DATA_ID_PREFIX = "#data/"


def normalize_data_file_id(id: Optional[str]) -> Optional[str]:
    if id is None:
        return None
    return id.replace(DATA_FILE_ID_PREFIX, DATA_ID_PREFIX)


def build_other_materials_by_id(
    materials: Optional[Materials],
) -> Dict[str, OtherMaterial]:
    other_materials_by_id: Dict[str, OtherMaterial] = {}
    if materials is None or materials.get("other_materials") is None:
        return other_materials_by_id
    for om in materials["other_materials"]:
        if om is not None and om.get("id") is not None:
            other_materials_by_id[om["id"]] = om
    return other_materials_by_id


def find_process_by_output_id(
    process_sequence: Optional[List[ProcessSequence]],
    output_id: Optional[str],
) -> Optional[ProcessSequence]:
    if process_sequence is None or output_id is None:
        return None

    normalized_output_id = normalize_data_file_id(output_id)

    for process in process_sequence:
        if process is None or process.get("outputs") is None:
            continue

        for output in process["outputs"]:
            if output is None or output.get("id") is None:
                continue

            if normalize_data_file_id(output["id"]) == normalized_output_id:
                return process

    return None


def find_other_material_from_process_input(
    process: Optional[ProcessSequence],
    materials: Optional[Materials],
) -> Optional[OtherMaterial]:
    if process is None or process.get("inputs") is None:
        return None

    other_materials_by_id = build_other_materials_by_id(materials)
    for input_item in process["inputs"]:
        if input_item is None or input_item.get("id") is None:
            continue

        other_material = other_materials_by_id.get(input_item["id"])
        if other_material is not None:
            return other_material

    return None


def find_data_files_from_process_outputs(
    process: Optional[ProcessSequence],
    assay_data_files: Optional[List[DataFile]],
) -> List[DataFile]:
    data_files: List[DataFile] = []
    if process is None or process.get("outputs") is None or assay_data_files is None:
        return data_files

    for output in process["outputs"]:
        if output is None or output.get("id") is None:
            continue

        normalized_output_id = normalize_data_file_id(output["id"])
        for data_file in assay_data_files:
            if data_file is None or data_file.get("id") is None:
                continue

            if normalize_data_file_id(data_file["id"]) == normalized_output_id:
                data_files.append(data_file)

    return data_files
