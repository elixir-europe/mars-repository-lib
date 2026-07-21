from mars_repository import (
    build_mars_receipt,
    mars_receipt_to_json,
    ReceiptAccessionsMap,
    IsaJson,
    Investigation,
    Study,
    Sample,
    Assay,
    Materials,
)

# 1. Build a receipt with errors and info
receipt = build_mars_receipt(
    "ENA",
    errors=["Missing study title", "Invalid characteristic"],
    info=["This is a test receipt"],
)
print(mars_receipt_to_json(receipt))
# {
#   "targetRepository": "ENA",
#   "errors": [
#     {"type": "INVALID_METADATA", "message": "Missing study title"},
#     {"type": "INVALID_METADATA", "message": "Invalid characteristic"}
#   ],
#   "info": [{"message": "This is a test receipt"}]
# }

# 2. Build a receipt with ISA-JSON and accession mapping
isa_json = IsaJson(
    investigation=Investigation(
        identifier="i1",
        studies=[
            Study(
                identifier="s1",
                title="My Study",
                materials=Materials(
                    samples=[Sample(name="sample-1")],
                ),
                assays=[Assay(id="a1", filename="a1.txt")],
            )
        ],
    )
)

# Map ISA item field values to accessions
studies = ReceiptAccessionsMap(item_name="identifier", key="s1")
studies.accession_map["s1"] = "ACC-STUDY-001"

samples = ReceiptAccessionsMap(item_name="name", key="sample-1")
samples.accession_map["sample-1"] = "ACC-SAMPLE-001"

receipt = build_mars_receipt(
    "ENA",
    isa_json=isa_json,
    studies_accessions=studies,
    samples_accessions=samples,
)
print(mars_receipt_to_json(receipt))
# {
#   "targetRepository": "ENA",
#   "accessions": [
#     {"value": "ACC-STUDY-001", "path": [{"key": "investigation"}, {"key": "studies", "where": {"key": "identifier", "value": "s1"}}]},
#     {"value": "ACC-SAMPLE-001", "path": [{"key": "investigation"}, {"key": "studies", "where": {"key": "identifier", "value": "s1"}}, {"key": "materials"}, {"key": "samples", "where": {"key": "name", "value": "sample-1"}}]}
#   ]
# }

# 3. Raise on errors
from mars_repository import MarsReceiptException
try:
    raise MarsReceiptException("Something went wrong")
except MarsReceiptException as e:
    print(e.error.message)  # "Something went wrong"