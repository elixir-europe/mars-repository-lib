from typing import Dict, List, Optional

from .models.isa.data_file import DataFile
from .models.isa.materials import Materials
from .models.isa.other_material import OtherMaterial
from .models.isa.process_sequence import ProcessSequence

DATA_FILE_ID_PREFIX = "#data_file/"
DATA_ID_PREFIX = "#data/"


def _value(item: dict, field: str):
    """Read either a Python model field or its ISA-JSON property name."""
    json_fields = {
        "id": "@id",
        "process_sequence": "processSequence",
        "data_files": "dataFiles",
        "other_materials": "otherMaterials",
    }
    json_field = json_fields.get(field, field)
    return item.get(json_field, item.get(field))


def normalize_data_file_id(id: Optional[str]) -> Optional[str]:
    if id is None:
        return None
    return id.replace(DATA_FILE_ID_PREFIX, DATA_ID_PREFIX)


def build_other_materials_by_id(
    materials: Optional[Materials],
) -> Dict[str, OtherMaterial]:
    other_materials_by_id: Dict[str, OtherMaterial] = {}
    other_materials = _value(materials, "other_materials") if materials else None
    if other_materials is None:
        return other_materials_by_id
    for om in other_materials:
        om_id = _value(om, "id") if om is not None else None
        if om_id is not None:
            other_materials_by_id[om_id] = om
    return other_materials_by_id


def find_process_by_output_id(
    process_sequence: Optional[List[ProcessSequence]],
    output_id: Optional[str],
) -> Optional[ProcessSequence]:
    if process_sequence is None or output_id is None:
        return None

    normalized_output_id = normalize_data_file_id(output_id)

    for process in process_sequence:
        outputs = _value(process, "outputs") if process is not None else None
        if outputs is None:
            continue

        for output in outputs:
            output_id = _value(output, "id") if output is not None else None
            if output_id is None:
                continue

            if normalize_data_file_id(output_id) == normalized_output_id:
                return process

    return None


def find_other_material_from_process_input(
    process: Optional[ProcessSequence],
    materials: Optional[Materials],
) -> Optional[OtherMaterial]:
    inputs = _value(process, "inputs") if process is not None else None
    if inputs is None:
        return None

    other_materials_by_id = build_other_materials_by_id(materials)
    for input_item in inputs:
        input_id = _value(input_item, "id") if input_item is not None else None
        if input_id is None:
            continue

        other_material = other_materials_by_id.get(input_id)
        if other_material is not None:
            return other_material

    return None


def find_data_files_from_process_outputs(
    process: Optional[ProcessSequence],
    assay_data_files: Optional[List[DataFile]],
) -> List[DataFile]:
    data_files: List[DataFile] = []
    outputs = _value(process, "outputs") if process is not None else None
    if outputs is None or assay_data_files is None:
        return data_files

    for output in outputs:
        output_id = _value(output, "id") if output is not None else None
        if output_id is None:
            continue

        normalized_output_id = normalize_data_file_id(output_id)
        for data_file in assay_data_files:
            data_file_id = _value(data_file, "id") if data_file is not None else None
            if data_file_id is None:
                continue

            if normalize_data_file_id(data_file_id) == normalized_output_id:
                data_files.append(data_file)

    return data_files
