import json
from pathlib import Path

from mars_repository.isa_json_graph_lookup import (
    build_other_materials_by_id,
    find_data_files_from_process_outputs,
    find_linked_upstream_processes,
    find_other_material_from_process_input,
    find_process_by_output_id,
    find_process_input_ids,
    find_process_output_ids,
    find_submission_id,
    normalize_data_file_id,
)


def _isa_json():
    path = Path(__file__).parents[2] / "MARS/test-data/ena-complete.json"
    isa_json = json.loads(path.read_text())
    # Add submission ID as a comment in the assay
    comments = isa_json["studies"][0]["assays"][0].get("comments", [])
    comments.append({"name": "ena_submission_id", "value": "submission-123"})
    return isa_json


def _assay():
    return _isa_json()["studies"][0]["assays"][0]


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


def test_find_submission_id_for_repository():
    isa_json = _isa_json()
    assert find_submission_id(isa_json, "ena") == "submission-123"
    assert find_submission_id(isa_json, "eva") is None


# --- previousProcess/nextProcess fallbacks -------------------------------------------------------
# The PTX study chains "sample growth culture" and "sample collection" without an intermediate
# material. Its assays declare every input and output, so the assay cases clear them to simulate
# protocol chains without a material in between.

PTX_ISA_JSON = (
    Path(__file__).parents[2]
    / "MARS/test-data/ISA-PTX-TEST/isajson/i_investigation_PTX_Pilot_Daphnia_magna_valid.json"
)


def _ptx_study():
    return json.loads(PTX_ISA_JSON.read_text())["studies"][0]


def _protocol_name(study, process):
    names = {protocol["@id"]: protocol["name"] for protocol in study["protocols"]}
    return names.get(process.get("executesProtocol", {}).get("@id"))


def _executing(study, processes, protocol_name):
    return [p for p in processes if _protocol_name(study, p) == protocol_name]


def _rnaseq_assay(study):
    return next(
        assay
        for assay in study["assays"]
        if _executing(study, assay["processSequence"], "nucleic acid sequencing")
    )


def test_sample_collection_without_inputs_resolves_source_through_previous_process():
    study = _ptx_study()
    processes = study["processSequence"]
    for sample in study["materials"]["samples"]:
        collection = find_process_by_output_id(processes, sample["@id"])
        assert _protocol_name(study, collection) == "sample collection"
        assert not collection.get("inputs")
        assert find_process_input_ids(processes, collection) == [sample["derivesFrom"][0]["@id"]]
        upstream = find_linked_upstream_processes(processes, collection)
        assert [_protocol_name(study, p) for p in upstream] == ["sample growth culture"]


def test_growth_culture_without_outputs_resolves_sample_through_next_process():
    study = _ptx_study()
    processes = study["processSequence"]
    samples_by_id = {sample["@id"]: sample for sample in study["materials"]["samples"]}
    growth_processes = _executing(study, processes, "sample growth culture")
    assert growth_processes
    for growth in growth_processes:
        assert not growth.get("outputs")
        (sample_id,) = find_process_output_ids(processes, growth)
        assert samples_by_id[sample_id]["derivesFrom"][0]["@id"] == growth["inputs"][0]["@id"]


def test_sequencing_without_inputs_resolves_library_through_previous_process():
    study = _ptx_study()
    assay = _rnaseq_assay(study)
    processes = assay["processSequence"]
    sequencing = _executing(study, processes, "nucleic acid sequencing")
    library_by_sequencing = {p["@id"]: p["inputs"][0]["@id"] for p in sequencing}
    for p in sequencing:
        p["inputs"] = []

    for p in sequencing:
        library = find_other_material_from_process_input(p, assay["materials"], processes)
        assert library["@id"] == library_by_sequencing[p["@id"]]
        assert find_other_material_from_process_input(p, assay["materials"]) is None
        assert find_linked_upstream_processes(processes, p) == []


def test_library_preparation_without_outputs_is_found_through_next_process():
    study = _ptx_study()
    processes = _rnaseq_assay(study)["processSequence"]
    preparations = _executing(study, processes, "library preparation")
    preparation_by_library = {p["outputs"][0]["@id"]: p["@id"] for p in preparations}
    for p in preparations:
        p["outputs"] = []

    assert len(preparation_by_library) == len(preparations)
    for library_id, preparation_id in preparation_by_library.items():
        assert find_process_by_output_id(processes, library_id)["@id"] == preparation_id


def test_library_preparation_chained_to_sequencing_without_library_material_resolves_extract():
    study = _ptx_study()
    assay = _rnaseq_assay(study)
    processes = assay["processSequence"]
    by_id = {p["@id"]: p for p in processes}
    sequencing = _executing(study, processes, "nucleic acid sequencing")
    preparation_by_sequencing = {p["@id"]: by_id[p["previousProcess"]["@id"]] for p in sequencing}
    extract_by_sequencing = {
        sequencing_id: preparation["inputs"][0]["@id"]
        for sequencing_id, preparation in preparation_by_sequencing.items()
    }

    # Library preparation -> sequencing without a library node in between.
    for p in _executing(study, processes, "library preparation"):
        p["outputs"] = []
    for p in sequencing:
        p["inputs"] = []

    for p in sequencing:
        extract = find_other_material_from_process_input(p, assay["materials"], processes)
        assert extract["@id"] == extract_by_sequencing[p["@id"]]
        assert find_linked_upstream_processes(processes, p) == [preparation_by_sequencing[p["@id"]]]


def test_sequencing_without_outputs_resolves_data_files_through_next_process():
    study = _ptx_study()
    assay = _rnaseq_assay(study)
    processes = assay["processSequence"]
    sequencing = _executing(study, processes, "nucleic acid sequencing")[0]
    data_file_ids = [output["@id"] for output in sequencing["outputs"]]

    # A downstream process that only declares the raw files as its inputs.
    processes.append(
        {
            "@id": "#process/quality_control_test",
            "previousProcess": {"@id": sequencing["@id"]},
            "inputs": [{"@id": data_file_id} for data_file_id in data_file_ids],
        }
    )
    sequencing["outputs"] = []

    for data_file_id in data_file_ids:
        assert find_process_by_output_id(processes, data_file_id)["@id"] == sequencing["@id"]
    found = find_data_files_from_process_outputs(sequencing, assay["dataFiles"], processes)
    assert [data_file["@id"] for data_file in found] == data_file_ids
    assert find_data_files_from_process_outputs(sequencing, assay["dataFiles"]) == []


def test_declared_inputs_and_outputs_take_precedence_over_links():
    study = _ptx_study()
    processes = _rnaseq_assay(study)["processSequence"]
    for p in processes:
        assert find_process_input_ids(processes, p) == [i["@id"] for i in p["inputs"]]
        assert find_process_output_ids(processes, p) == [o["@id"] for o in p["outputs"]]
        assert find_linked_upstream_processes(processes, p) == []


def test_cyclic_links_without_materials_terminate():
    first = {"@id": "#process/first", "nextProcess": {"@id": "#process/second"}}
    second = {"@id": "#process/second", "nextProcess": {"@id": "#process/first"}}
    processes = [first, second]

    assert find_process_input_ids(processes, first) == []
    assert find_process_output_ids(processes, first) == []
    assert find_linked_upstream_processes(processes, first) == [second]
    assert find_process_by_output_id(processes, "#sample/none") is None
