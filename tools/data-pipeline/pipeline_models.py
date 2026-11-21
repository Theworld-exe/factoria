from dataclasses import dataclass, field
from typing import List, Optional, Dict, Any

@dataclass
class NormalizedCategory:
    id: str
    name: str
    icon: Optional[str] = None

@dataclass
class NormalizedItem:
    id: str
    name: str
    category_id: Optional[str]
    stack_size: int = 1
    icon: Optional[str] = None
    type: str = "item"  # "item" or "fluid"

@dataclass
class NormalizedMachine:
    id: str
    name: str
    category: Optional[str]
    crafting_speed: float
    energy_usage_kw: float
    pollution_per_minute: float = 0.0
    module_slots: int = 0
    icon: Optional[str] = None

@dataclass
class NormalizedIngredient:
    item_id: str
    amount: float

@dataclass
class NormalizedProduct:
    item_id: str
    amount: float
    probability: float = 1.0

@dataclass
class NormalizedRecipe:
    id: str
    name: str
    category_id: Optional[str]
    crafting_time: float
    is_catalyst: bool = False
    icon: Optional[str] = None
    ingredients: List[NormalizedIngredient] = field(default_factory=list)
    products: List[NormalizedProduct] = field(default_factory=list)
    producers: List[str] = field(default_factory=list)  # machine IDs that can craft this

@dataclass
class NormalizedMachineRecipe:
    machine_id: str
    recipe_category: str

@dataclass
class ParsedDataset:
    categories: List[NormalizedCategory]
    items: List[NormalizedItem]
    machines: List[NormalizedMachine]
    recipes: List[NormalizedRecipe]
    machine_recipes: List[NormalizedMachineRecipe]
    metadata: Dict[str, str]
