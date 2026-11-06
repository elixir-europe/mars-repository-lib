import json
from pathlib import Path

from mars_repository import ReceiptAccessionsMap, build_mars_receipt


def _isa_json():
    path = Path(__file__).parents[2] / "MARS/test-data/biosamples-input-isa-multi.json"
    return json.loads(path.read_text())


def test_reset_mars_receipt():
    initial = build_mars_receipt("test-repository")
    assert initial["target_repository"] == "test-repository"
    assert initial.get("accessions", []) == []
    assert initial.get("errors", []) == []
    assert initial.get("info", []) == []

    populated = build_mars_receipt("test-repository", info=["info"], errors=["error"])
    reset = build_mars_receipt("test-repository")
    assert populated.get("errors") != reset.get("errors")
    assert populated.get("info") != reset.get("info")
    assert reset.get("accessions", []) == []


def test_set_mars_receipt_accessions():
    isa_json = _isa_json()
    assay = isa_json["investigation"]["studies"][0]["assays"][0]
    studies = ReceiptAccessionsMap("id")
    studies.accession_map[assay["@id"]] = "PRJ123456"
    data_files = ReceiptAccessionsMap("id")
    for idx, data_file in enumerate(assay["dataFiles"]):
        data_files.accession_map[data_file["@id"]] = f"ERZ{idx}"

    receipt = build_mars_receipt(
        "test-repository",
        studies_accessions=studies,
        data_files_accessions=data_files,
        isa_json=isa_json,
    )
    assert receipt["target_repository"] == "test-repository"
    assert len(receipt["accessions"]) == 1 + len(assay["dataFiles"])
    assert receipt.get("errors", []) == []
    assert receipt.get("info", []) == []


def test_get_accession_map_entry_field_does_not_exist():
    receipt = build_mars_receipt(
        "test-repository",
        studies_accessions=ReceiptAccessionsMap("missingField", "value"),
        isa_json=_isa_json(),
    )
    assert len(receipt["errors"]) == 1
    assert receipt["errors"][0]["message"] == (
        "Cannot find an item of dict with the key missingField in the ISA-JSON input"
    )
    assert receipt.get("accessions", []) == []
