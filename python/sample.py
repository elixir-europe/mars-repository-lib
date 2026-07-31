import json
from typing import Any

from mars_repository import IsaJson, ReceiptAccessionsMap, build_mars_receipt, mars_receipt_to_json

ENA_ISA_JSON = """
{
  "investigation": {
    "identifier": "i1",
    "studies": [{
        "identifier": "comparative-analysis",
        "title": "comparative-analysis",
        "materials": {
          "samples": [
            {"name": "stomach_microbiota"}
        ]},
        "assays": [{
            "id": "a1",
            "filename": "a1.txt",
            "materials": {
              "other_materials": [
                {"name": "illumina-hiSeq"}
            ]},
            "data_files": [
              {"name": "paired-data"}
            ]}
        ]}
    ]}
}
"""

ENA_RECEIPT_JSON = """
{
  "success" : true,
  "experiments" : [{
    "alias" : "illumina-hiSeq",
    "accession" : "ERX9223136",
    "status" : "PRIVATE"
  }],
  "runs" : [{
    "alias" : "paired-data",
    "accession" : "ERR9669128",
    "status" : "PRIVATE"
  }],
  "samples" : [{
    "alias" : "stomach_microbiota",
    "accession" : "ERS27605861",
    "status" : "PRIVATE",
    "externalAccession" : {
      "id" : "SAMEA130793922",
      "db" : "biosample"
    }
}],
  "projects" : [{
    "alias" : "comparative-analysis",
    "accession" : "PRJEB101337",
    "status" : "PRIVATE",
    "externalAccession" : {
      "id" : "ERP201886",
      "db" : "study"
    }
}],
  "messages" : {
    "info" : [ "All objects in this submission are set to private status (HOLD)." ]
  },
  "actions" : [ "ADD", "HOLD" ]
}"""


def interpret_ena_receipt(
    ena_receipt_json: str,
) -> tuple[
    ReceiptAccessionsMap,
    ReceiptAccessionsMap,
    ReceiptAccessionsMap,
    ReceiptAccessionsMap,
    list[str],
    list[str],
]:
    data: dict[str, Any] = json.loads(ena_receipt_json)

    info = data.get("messages", {}).get("info", [])
    errors = data.get("messages", {}).get("error", [])

    projects_map = ReceiptAccessionsMap(item_name="identifier")
    samples_map = ReceiptAccessionsMap(item_name="name")
    experiments_map = ReceiptAccessionsMap(item_name="name")
    runs_map = ReceiptAccessionsMap(item_name="name")

    for p in data.get("projects", []):
        projects_map.accession_map[p["alias"]] = p["accession"]

    for s in data.get("samples", []):
        samples_map.accession_map[s["alias"]] = s["accession"]

    for e in data.get("experiments", []):
        experiments_map.accession_map[e["alias"]] = e["accession"]

    for r in data.get("runs", []):
        runs_map.accession_map[r["alias"]] = r["accession"]

    return projects_map, samples_map, experiments_map, runs_map, info, errors


isa_json: IsaJson = json.loads(ENA_ISA_JSON)
projects, samples, experiments, runs, info, errors = interpret_ena_receipt(ENA_RECEIPT_JSON)
receipt = build_mars_receipt(
    "ena",
    isa_json=isa_json,
    studies_accessions=projects,
    samples_accessions=samples,
    other_materials_accessions=experiments,
    data_files_accessions=runs,
    info=info,
    errors=errors,
)
print(mars_receipt_to_json(receipt))