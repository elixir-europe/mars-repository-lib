"""Shared ISA-JSON graph helpers for repository service conversions.

Declared process ``input``/``outputs`` always take precedence. A process that declares none is
resolved through its ``previousProcess``/``nextProcess`` links within the process sequence it
belongs to, so protocol chains without an intermediate material (e.g. growth culture -> sample
collection) still resolve. Mirrors the Java ``IsaJsonGraphLookup``.
"""

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
        "previous_process": "previousProcess",
        "next_process": "nextProcess",
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
    """Find the process that produced the given output material or data file.

    Declared outputs are matched first. If no process declares the item, a process without
    declared outputs is matched when its outputs resolved through ``nextProcess`` links (see
    ``find_process_output_ids``) contain it.
    """
    if process_sequence is None or output_id is None:
        return None

    normalized_output_id = normalize_data_file_id(output_id)

    for process in process_sequence:
        if _contains_normalized_id(_declared_output_ids(process), normalized_output_id):
            return process

    # Fallback for processes chained to their successor without an intermediate material.
    graph = _ProcessGraph(process_sequence)
    for process in process_sequence:
        if (
            process is not None
            and not _declared_output_ids(process)
            and _contains_normalized_id(graph.output_ids(process), normalized_output_id)
        ):
            return process

    return None


def find_other_material_from_process_input(
    process: Optional[ProcessSequence],
    materials: Optional[Materials],
    process_sequence: Optional[List[ProcessSequence]] = None,
) -> Optional[OtherMaterial]:
    """Find the first assay other material referenced by a process input.

    With ``process_sequence`` the inputs are resolved through ``previousProcess`` links when the
    process declares none (see ``find_process_input_ids``); without it only declared inputs count.
    """
    if process is None:
        return None

    other_materials_by_id = build_other_materials_by_id(materials)
    for input_id in find_process_input_ids(process_sequence, process):
        other_material = other_materials_by_id.get(input_id)
        if other_material is not None:
            return other_material

    return None


def find_data_files_from_process_outputs(
    process: Optional[ProcessSequence],
    assay_data_files: Optional[List[DataFile]],
    process_sequence: Optional[List[ProcessSequence]] = None,
) -> List[DataFile]:
    """Find assay data files referenced by a process's outputs.

    With ``process_sequence`` the outputs are resolved through ``nextProcess`` links when the
    process declares none (see ``find_process_output_ids``); without it only declared outputs count.
    """
    data_files: List[DataFile] = []
    if process is None or assay_data_files is None:
        return data_files

    for output_id in find_process_output_ids(process_sequence, process):
        normalized_output_id = normalize_data_file_id(output_id)
        for data_file in assay_data_files:
            data_file_id = _value(data_file, "id") if data_file is not None else None
            if data_file_id is None:
                continue

            if normalize_data_file_id(data_file_id) == normalized_output_id:
                data_files.append(data_file)

    return data_files


def find_process_input_ids(
    process_sequence: Optional[List[ProcessSequence]],
    process: Optional[ProcessSequence],
) -> List[str]:
    """Return the IDs of the items that went into a process.

    These are the declared inputs or, when the process declares none, the items resolved by
    walking back through ``previousProcess`` links in ``process_sequence``: the outputs of the
    preceding process, or its inputs when it declares no outputs either. Without a process
    sequence only declared inputs are returned.
    """
    declared = _declared_input_ids(process)
    if declared or process is None or process_sequence is None:
        return declared
    return _ProcessGraph(process_sequence).input_ids(process)


def find_process_output_ids(
    process_sequence: Optional[List[ProcessSequence]],
    process: Optional[ProcessSequence],
) -> List[str]:
    """Return the IDs of the items produced by a process.

    These are the declared outputs or, when the process declares none, the items resolved by
    walking forward through ``nextProcess`` links in ``process_sequence``: the inputs of the
    following process, or its outputs when it declares no inputs either. Without a process
    sequence only declared outputs are returned.
    """
    declared = _declared_output_ids(process)
    if declared or process is None or process_sequence is None:
        return declared
    return _ProcessGraph(process_sequence).output_ids(process)


def find_linked_upstream_processes(
    process_sequence: Optional[List[ProcessSequence]],
    process: Optional[ProcessSequence],
) -> List[ProcessSequence]:
    """Return the processes chained directly before a process without an intermediate material.

    Closest first. Such processes act on the same material as ``process``, so their parameters
    describe the same step. A preceding process that declares outputs produced a separate
    material and ends the chain; a process with declared inputs has no such chain.
    """
    if process is None or process_sequence is None or _declared_input_ids(process):
        return []
    return _ProcessGraph(process_sequence).upstream_linked_processes(process)


def _declared_input_ids(process: Optional[ProcessSequence]) -> List[str]:
    return _item_ids(_value(process, "inputs") if process is not None else None)


def _declared_output_ids(process: Optional[ProcessSequence]) -> List[str]:
    return _item_ids(_value(process, "outputs") if process is not None else None)


def _item_ids(items) -> List[str]:
    ids: List[str] = []
    for item in items or []:
        item_id = _value(item, "id") if item is not None else None
        if item_id is not None:
            ids.append(item_id)
    return ids


def _contains_normalized_id(ids: List[str], normalized_id: Optional[str]) -> bool:
    return any(normalize_data_file_id(item_id) == normalized_id for item_id in ids)


def _add_unique(target: list, items) -> None:
    for item in items:
        if not any(existing is item or existing == item for existing in target):
            target.append(item)


class _ProcessGraph:
    """Index of the ``previousProcess``/``nextProcess`` links within one process sequence.

    Links are honoured in both directions, so a chain where only one side sets the reference
    still resolves. Processes are keyed by identity because they are plain dicts.
    """

    def __init__(self, process_sequence: List[ProcessSequence]):
        self._predecessors: Dict[int, List[ProcessSequence]] = {}
        self._successors: Dict[int, List[ProcessSequence]] = {}

        processes_by_id: Dict[str, ProcessSequence] = {}
        for process in process_sequence:
            process_id = _value(process, "id") if process is not None else None
            if process_id is not None:
                processes_by_id.setdefault(process_id, process)

        for process in process_sequence:
            if process is None:
                continue
            previous = _value(process, "previous_process")
            if previous is not None:
                self._link(processes_by_id.get(_value(previous, "id")), process)
            following = _value(process, "next_process")
            if following is not None:
                self._link(process, processes_by_id.get(_value(following, "id")))

    def input_ids(self, process: ProcessSequence) -> List[str]:
        ids: List[str] = []
        self._collect_input_ids(process, ids, set())
        return ids

    def output_ids(self, process: ProcessSequence) -> List[str]:
        ids: List[str] = []
        self._collect_output_ids(process, ids, set())
        return ids

    def upstream_linked_processes(self, process: ProcessSequence) -> List[ProcessSequence]:
        linked: List[ProcessSequence] = []
        self._collect_upstream_linked_processes(process, linked, set())
        return linked

    def _collect_input_ids(self, process, ids: List[str], visited: set) -> None:
        if process is None or id(process) in visited:
            return
        visited.add(id(process))

        declared = _declared_input_ids(process)
        if declared:
            _add_unique(ids, declared)
            return

        for predecessor in self._predecessors.get(id(process), []):
            predecessor_output_ids = _declared_output_ids(predecessor)
            if predecessor_output_ids:
                _add_unique(ids, predecessor_output_ids)
            else:
                self._collect_input_ids(predecessor, ids, visited)

    def _collect_output_ids(self, process, ids: List[str], visited: set) -> None:
        if process is None or id(process) in visited:
            return
        visited.add(id(process))

        declared = _declared_output_ids(process)
        if declared:
            _add_unique(ids, declared)
            return

        for successor in self._successors.get(id(process), []):
            successor_input_ids = _declared_input_ids(successor)
            if successor_input_ids:
                _add_unique(ids, successor_input_ids)
            else:
                self._collect_output_ids(successor, ids, visited)

    def _collect_upstream_linked_processes(self, process, linked: list, visited: set) -> None:
        if process is None or id(process) in visited or _declared_input_ids(process):
            return
        visited.add(id(process))

        for predecessor in self._predecessors.get(id(process), []):
            if not _declared_output_ids(predecessor) and id(predecessor) not in visited:
                linked.append(predecessor)
                self._collect_upstream_linked_processes(predecessor, linked, visited)

    def _link(self, previous, following) -> None:
        if previous is None or following is None or previous is following:
            return
        _add_unique(self._successors.setdefault(id(previous), []), [following])
        _add_unique(self._predecessors.setdefault(id(following), []), [previous])
