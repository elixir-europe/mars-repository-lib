from typing import Any, Optional

from .models.isa.isa_json import IsaJson
from .models.receipt.mars_error_type import MarsErrorType
from .models.receipt.mars_receipt import MarsReceipt
from .receipt_accessions_map import ReceiptAccessionsMap


def build_mars_receipt(
    target_repository: str,
    *,
    studies_accessions: Optional[ReceiptAccessionsMap] = None,
    samples_accessions: Optional[ReceiptAccessionsMap] = None,
    sources_accessions: Optional[ReceiptAccessionsMap] = None,
    other_materials_accessions: Optional[ReceiptAccessionsMap] = None,
    data_files_accessions: Optional[ReceiptAccessionsMap] = None,
    info: Optional[list[str]] = None,
    errors: Optional[list[str]] = None,
    isa_json: Optional[IsaJson] = None,
) -> MarsReceipt:
    receipt_errors: list[dict] = []
    receipt_info: list[dict] = []

    if errors:
        for msg in errors:
            if msg is not None:
                receipt_errors.append(
                    {"message": str(msg), "type": MarsErrorType.INVALID_METADATA}
                )

    if info:
        for msg in info:
            if msg is not None:
                receipt_info.append({"message": msg})

    accessions, resolve_errors = _collect_accessions(
        studies_accessions,
        samples_accessions,
        sources_accessions,
        other_materials_accessions,
        data_files_accessions,
        isa_json,
    )
    receipt_errors.extend(resolve_errors)

    result: MarsReceipt = {"target_repository": target_repository}
    if receipt_errors:
        result["errors"] = receipt_errors
    if receipt_info:
        result["info"] = receipt_info
    if accessions:
        result["accessions"] = accessions
    return result


def _collect_accessions(
    studies_acc: Optional[ReceiptAccessionsMap],
    samples_acc: Optional[ReceiptAccessionsMap],
    sources_acc: Optional[ReceiptAccessionsMap],
    other_materials_acc: Optional[ReceiptAccessionsMap],
    data_files_acc: Optional[ReceiptAccessionsMap],
    isa_json: Optional[IsaJson],
) -> tuple[list[dict], list[dict]]:
    if not (isa_json and isa_json.get("investigation") and isa_json["investigation"].get("studies")):
        return [], []

    accessions: list[dict] = []
    errors: list[dict] = []
    for study in isa_json["investigation"]["studies"]:
        if studies_acc is None:
            continue

        if studies_acc.isa_item_name == "id":
            for assay in (study.get("assays") or []):
                _, assay_value, assay_acc, errs = _resolve(studies_acc, assay)
                errors.extend(errs)
                if assay_acc is not None:
                    accessions.append(
                        {
                            "value": assay_acc,
                            "path": make_assay_path(
                                "title", study.get("title") or "",
                                "@id", assay_value,
                            ),
                        }
                    )
        else:
            study_key, study_value, study_acc, errs = _resolve(studies_acc, study)
            errors.extend(errs)
            if study_acc is not None:
                accessions.append(
                    {
                        "value": study_acc,
                        "path": make_study_path(study_key, study_value),
                    }
                )

        study_key = "title"
        study_value = study.get("title") or ""

        materials = study.get("materials")
        if samples_acc is not None and materials is not None:
            for sample in (materials.get("samples") or []):
                _, sample_value, sample_acc, errs = _resolve(samples_acc, sample)
                errors.extend(errs)
                if sample_acc is not None:
                    accessions.append(
                        {
                            "value": sample_acc,
                            "path": make_sample_path(
                                study_key, study_value,
                                samples_acc.isa_item_name, sample_value,
                            ),
                        }
                    )

        if sources_acc is not None and materials is not None:
            for source in (materials.get("sources") or []):
                _, source_value, source_acc, errs = _resolve(sources_acc, source)
                errors.extend(errs)
                if source_acc is not None:
                    accessions.append(
                        {
                            "value": source_acc,
                            "path": make_source_path(
                                study_key, study_value,
                                sources_acc.isa_item_name, source_value,
                            ),
                        }
                    )

        if other_materials_acc is not None or data_files_acc is not None:
            for assay in (study.get("assays") or []):
                assay_materials = assay.get("materials")
                if other_materials_acc is not None and assay_materials is not None:
                    for om in (assay_materials.get("other_materials") or []):
                        _, om_value, om_acc, errs = _resolve(other_materials_acc, om)
                        errors.extend(errs)
                        if om_acc is not None:
                            accessions.append(
                                {
                                    "value": om_acc,
                                    "path": make_other_material_path(
                                        study_key, study_value, assay.get("id"),
                                        other_materials_acc.isa_item_name, om_value,
                                    ),
                                }
                            )

                if data_files_acc is not None:
                    for df in (assay.get("data_files") or []):
                        _, df_value, df_acc, errs = _resolve(data_files_acc, df)
                        errors.extend(errs)
                        if df_acc is not None:
                            accessions.append(
                                {
                                    "value": df_acc,
                                    "path": make_data_file_path(
                                        study_key, study_value, assay.get("id"),
                                        data_files_acc.isa_item_name, df_value,
                                    ),
                                }
                            )

    return accessions, errors


def _resolve(
    acc_map: ReceiptAccessionsMap,
    item: Any,
) -> tuple[str, str, Optional[str], list[dict]]:
    field_value = item.get(acc_map.isa_item_name)
    if field_value is not None:
        field_str = str(field_value)
        accession = acc_map.accession_map.get(field_str)
        return (acc_map.isa_item_name, field_str, accession, [])
    else:
        return (
            acc_map.isa_item_name,
            "",
            None,
            [
                {
                    "message": (
                        f"Cannot find an item of {type(item).__name__} "
                        f"with the key {acc_map.isa_item_name} in the ISA-JSON input"
                    ),
                    "type": MarsErrorType.INVALID_METADATA,
                }
            ],
        )


def make_study_path(key: str, value: str) -> list[dict]:
    return [
        {"key": "investigation"},
        {"key": "studies", "where": {"key": key, "value": value}},
    ]


def make_sample_path(
    study_key: str, study_value: str,
    sample_key: str, sample_value: str,
) -> list[dict]:
    return [
        {"key": "investigation"},
        {"key": "studies", "where": {"key": study_key, "value": study_value}},
        {"key": "materials"},
        {"key": "samples", "where": {"key": sample_key, "value": sample_value}},
    ]


def make_assay_path(
    study_key: str, study_value: str,
    assay_key: str, assay_value: str,
) -> list[dict]:
    return [
        {"key": "investigation"},
        {"key": "studies", "where": {"key": study_key, "value": study_value}},
        {"key": "assays", "where": {"key": assay_key, "value": assay_value}},
    ]


def make_source_path(
    study_key: str, study_value: str,
    source_key: str, source_value: str,
) -> list[dict]:
    return [
        {"key": "investigation"},
        {"key": "studies", "where": {"key": study_key, "value": study_value}},
        {"key": "materials"},
        {"key": "sources", "where": {"key": source_key, "value": source_value}},
    ]


def make_other_material_path(
    study_key: str, study_value: str,
    assay_id: Optional[str],
    om_key: str, om_value: str,
) -> list[dict]:
    return [
        {"key": "investigation"},
        {"key": "studies", "where": {"key": study_key, "value": study_value}},
        {"key": "assays", "where": {"key": "@id", "value": assay_id}},
        {"key": "materials"},
        {"key": "otherMaterials", "where": {"key": om_key, "value": om_value}},
    ]


def make_data_file_path(
    study_key: str, study_value: str,
    assay_id: Optional[str],
    df_key: str, df_value: str,
) -> list[dict]:
    return [
        {"key": "investigation"},
        {"key": "studies", "where": {"key": study_key, "value": study_value}},
        {"key": "assays", "where": {"key": "@id", "value": assay_id}},
        {"key": "dataFiles", "where": {"key": df_key, "value": df_value}},
    ]


def mars_receipt_to_dict(receipt: MarsReceipt) -> dict:
    d: dict = {"targetRepository": receipt.get("target_repository", "")}

    errors = receipt.get("errors")
    if errors:
        d["errors"] = [
            {"type": e["type"].value, "message": e["message"],
             "path": [_path_dict(p) for p in e.get("path", [])]} if e.get("path")
            else {"type": e["type"].value, "message": e["message"]}
            for e in errors
        ]

    info = receipt.get("info")
    if info:
        d["info"] = [
            {"message": i["message"]} if i.get("name") is None
            else {"name": i["name"], "message": i["message"]}
            for i in info
        ]

    accessions = receipt.get("accessions")
    if accessions:
        d["accessions"] = [
            {"value": a["value"], "path": [_path_dict(p) for p in a["path"]]}
            for a in accessions
        ]

    return d


def _path_dict(p: dict) -> dict:
    d: dict = {"key": p["key"]}
    where = p.get("where")
    if where is not None:
        d["where"] = {"key": where["key"], "value": where["value"]}
    return d


def mars_receipt_to_json(receipt: MarsReceipt, indent: int = 2, **kwargs) -> str:
    import json
    return json.dumps(mars_receipt_to_dict(receipt), indent=indent, **kwargs)
