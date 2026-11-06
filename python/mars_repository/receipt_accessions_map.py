from typing import Dict, Optional


class ReceiptAccessionsMap:
    accession_map: Dict[str, Optional[str]]
    isa_item_name: str

    def __init__(self, item_name: str = "", key: Optional[str] = None):
        self.isa_item_name = item_name
        self.accession_map = {}
        if key is not None:
            self.accession_map[key] = None

    def __str__(self) -> str:
        result = f"ReceiptAccessionsMap:{self.isa_item_name}\n"
        for k, v in self.accession_map.items():
            result += f"{k}:{v}\n"
        return result
