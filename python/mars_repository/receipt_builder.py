from typing import Any, Optional

from .models.isa.isa_json import IsaJson
from .models.receipt.mars_accession import MarsAccession
from .models.receipt.mars_error import MarsError
from .models.receipt.mars_error_type import MarsErrorType
from .models.receipt.mars_info import MarsInfo
from .models.receipt.mars_path import MarsPath
from .models.receipt.mars_receipt import MarsReceipt
from .models.receipt.mars_where import MarsWhere
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
    receipt_errors: list[MarsError] = []
    receipt_info: list[MarsInfo] = []

    if errors:
        for msg in errors:
            if msg is not None:
                receipt_errors.append(
                    MarsError(message=str(msg), type=MarsErrorType.INVALID_METADATA)
                )

    if info:
        for msg in info:
            if msg is not None:
                receipt_info.append(MarsInfo(message=msg))

    accessions, resolve_errors = _collect_accessions(
        studies_accessions,
        samples_accessions,
        sources_accessions,
        other_materials_accessions,
        data_files_accessions,
        isa_json,
    )
    receipt_errors.extend(resolve_errors)

    return MarsReceipt(
        target_repository=target_repository,
        errors=receipt_errors or None,
        info=receipt_info or None,
        accessions=accessions or None,
    )


def _collect_accessions(
    studies_acc: Optional[ReceiptAccessionsMap],
    samples_acc: Optional[ReceiptAccessionsMap],
    sources_acc: Optional[ReceiptAccessionsMap],
    other_materials_acc: Optional[ReceiptAccessionsMap],
    data_files_acc: Optional[ReceiptAccessionsMap],
    isa_json: Optional[IsaJson],
) -> tuple[list[MarsAccession], list[MarsError]]:
    if not (isa_json and isa_json.investigation and isa_json.investigation.studies):
        return [], []

    accessions: list[MarsAccession] = []
    errors: list[MarsError] = []

    for study in isa_json.investigation.studies:
        if studies_acc is None:
            continue

        study_key, study_value, study_acc, errs = _resolve(studies_acc, study)
        errors.extend(errs)
        if study_acc is not None:
            accessions.append(
                MarsAccession(
                    value=study_acc,
                    path=make_study_path(study_key, study_value),
                )
            )

        if samples_acc is not None and study.materials is not None:
            for sample in (study.materials.samples or []):
                _, sample_value, sample_acc, errs = _resolve(samples_acc, sample)
                errors.extend(errs)
                if sample_acc is not None:
                    accessions.append(
                        MarsAccession(
                            value=sample_acc,
                            path=make_sample_path(
                                study_key, study_value,
                                samples_acc.isa_item_name, sample_value,
                            ),
                        )
                    )

        if sources_acc is not None and study.materials is not None:
            for source in (study.materials.sources or []):
                _, source_value, source_acc, errs = _resolve(sources_acc, source)
                errors.extend(errs)
                if source_acc is not None:
                    accessions.append(
                        MarsAccession(
                            value=source_acc,
                            path=make_source_path(
                                study_key, study_value,
                                sources_acc.isa_item_name, source_value,
                            ),
                        )
                    )

        if other_materials_acc is not None or data_files_acc is not None:
            for assay in (study.assays or []):
                if other_materials_acc is not None and assay.materials is not None:
                    for om in (assay.materials.other_materials or []):
                        _, om_value, om_acc, errs = _resolve(other_materials_acc, om)
                        errors.extend(errs)
                        if om_acc is not None:
                            accessions.append(
                                MarsAccession(
                                    value=om_acc,
                                    path=make_other_material_path(
                                        study_key, study_value, assay.id,
                                        other_materials_acc.isa_item_name, om_value,
                                    ),
                                )
                            )

                if data_files_acc is not None:
                    for df in (assay.data_files or []):
                        _, df_value, df_acc, errs = _resolve(data_files_acc, df)
                        errors.extend(errs)
                        if df_acc is not None:
                            accessions.append(
                                MarsAccession(
                                    value=df_acc,
                                    path=make_data_file_path(
                                        study_key, study_value, assay.id,
                                        data_files_acc.isa_item_name, df_value,
                                    ),
                                )
                            )

    return accessions, errors


def _resolve(
    acc_map: ReceiptAccessionsMap,
    item: Any,
) -> tuple[str, str, Optional[str], list[MarsError]]:
    field_value = getattr(item, acc_map.isa_item_name, None)
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
                MarsError(
                    message=(
                        f"Cannot find an item of {type(item).__name__} "
                        f"with the key {acc_map.isa_item_name} in the ISA-JSON input"
                    ),
                    type=MarsErrorType.INVALID_METADATA,
                )
            ],
        )


def make_study_path(key: str, value: str) -> list[MarsPath]:
    return [
        MarsPath(key="investigation"),
        MarsPath(key="studies", where=MarsWhere(key=key, value=value)),
    ]


def make_sample_path(
    study_key: str, study_value: str,
    sample_key: str, sample_value: str,
) -> list[MarsPath]:
    return [
        MarsPath(key="investigation"),
        MarsPath(key="studies", where=MarsWhere(key=study_key, value=study_value)),
        MarsPath(key="materials"),
        MarsPath(key="samples", where=MarsWhere(key=sample_key, value=sample_value)),
    ]


def make_source_path(
    study_key: str, study_value: str,
    source_key: str, source_value: str,
) -> list[MarsPath]:
    return [
        MarsPath(key="investigation"),
        MarsPath(key="studies", where=MarsWhere(key=study_key, value=study_value)),
        MarsPath(key="materials"),
        MarsPath(key="sources", where=MarsWhere(key=source_key, value=source_value)),
    ]


def make_other_material_path(
    study_key: str, study_value: str,
    assay_id: Optional[str],
    om_key: str, om_value: str,
) -> list[MarsPath]:
    return [
        MarsPath(key="investigation"),
        MarsPath(key="studies", where=MarsWhere(key=study_key, value=study_value)),
        MarsPath(key="assays", where=MarsWhere(key="@id", value=assay_id)),
        MarsPath(key="materials"),
        MarsPath(key="otherMaterials", where=MarsWhere(key=om_key, value=om_value)),
    ]


def make_data_file_path(
    study_key: str, study_value: str,
    assay_id: Optional[str],
    df_key: str, df_value: str,
) -> list[MarsPath]:
    return [
        MarsPath(key="investigation"),
        MarsPath(key="studies", where=MarsWhere(key=study_key, value=study_value)),
        MarsPath(key="assays", where=MarsWhere(key="@id", value=assay_id)),
        MarsPath(key="dataFiles", where=MarsWhere(key=df_key, value=df_value)),
    ]


def mars_receipt_to_dict(receipt: MarsReceipt) -> dict:
    def _path_dict(p: MarsPath) -> dict:
        d: dict = {"key": p.key}
        if p.where is not None:
            d["where"] = {"key": p.where.key, "value": p.where.value}
        return d

    d: dict = {"targetRepository": receipt.target_repository}

    if receipt.errors:
        d["errors"] = [
            {"type": e.type.value, "message": e.message,
             "path": [_path_dict(p) for p in e.path]} if e.path
            else {"type": e.type.value, "message": e.message}
            for e in receipt.errors
        ]

    if receipt.info:
        d["info"] = [
            {"message": i.message} if i.name is None
            else {"name": i.name, "message": i.message}
            for i in receipt.info
        ]

    if receipt.accessions:
        d["accessions"] = [
            {"value": a.value, "path": [_path_dict(p) for p in a.path]}
            for a in receipt.accessions
        ]

    return d


def mars_receipt_to_json(receipt: MarsReceipt, indent: int = 2, **kwargs) -> str:
    import json
    return json.dumps(mars_receipt_to_dict(receipt), indent=indent, **kwargs)
