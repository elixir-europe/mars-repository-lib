import json
from pathlib import Path

from mars_repository.isa_json_graph_lookup import (
    build_other_materials_by_id,
    find_data_files_from_process_outputs,
    find_other_material_from_process_input,
    find_process_by_output_id,
    normalize_data_file_id,
)


def _assay():
    path = Path(__file__).parents[2] / "MARS/test-data/ena-complete.json"
    isa_json = json.loads(path.read_text())
    return isa_json["studies"][0]["assays"][0]


def test_find_process_by_output_id():
    assay = _assay()
    process = find_process_by_output_id(assay["processSequence"], "#data/334")
    assert process["@id"] == "#process/nucleic_acid_sequencing/334"


def test_find_process_by_output_id_no_process_found():
    assert find_process_by_output_id(_assay()["processSequence"], "#data/does-not-exist") is None


def test_find_other_material_from_process_input():
    assay = _assay()
    material = find_other_material_from_process_input(assay["processSequence"][2], assay["materials"])
    assert material["@id"] == "#other_material/333"


def test_find_data_files_from_process_outputs():
    assay = _assay()
    files = find_data_files_from_process_outputs(assay["processSequence"][2], assay["dataFiles"])
    assert [item["@id"] for item in files] == ["#data_file/334", "#data_file/335"]


def test_build_other_materials_by_id():
    materials = build_other_materials_by_id(_assay()["materials"])
    assert materials["#other_material/332"]["@id"] == "#other_material/332"


def test_normalize_data_file_id():
    assert normalize_data_file_id("#data_file/334") == "#data/334"
    assert normalize_data_file_id("#data/334") == "#data/334"
    assert normalize_data_file_id("#sample/331") == "#sample/331"
    assert normalize_data_file_id(None) is None
