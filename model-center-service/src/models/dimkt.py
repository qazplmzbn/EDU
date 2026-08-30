"""DIMKT model definition with an explicit mastery head.

The production endpoint refuses to advertise readiness until a versioned
manifest and matching weight artifact are present. PyTorch is intentionally an
optional dependency of the base model-center service; install
requirements-dimkt.txt on inference/training hosts.
"""
from __future__ import annotations
import hashlib
import json
from pathlib import Path

try:
    import torch
    from torch import nn
except ImportError:  # base model center can still serve non-DIMKT routes
    torch = None
    nn = object
    DIMKTModel = None

if torch is not None:
    class DIMKTModel(nn.Module):
        def __init__(self, knowledge_count: int, hidden_size: int = 128):
            super().__init__()
            self.knowledge_embedding = nn.Embedding(knowledge_count, hidden_size)
            self.difficulty_projection = nn.Linear(1, hidden_size)
            self.response_projection = nn.Linear(1, hidden_size)
            self.state_cell = nn.GRUCell(hidden_size * 3, hidden_size)
            self.mastery_head = nn.Linear(hidden_size, knowledge_count)

        def forward(self, knowledge_index, difficulty, score, hidden):
            features = torch.cat((self.knowledge_embedding(knowledge_index),
                                  self.difficulty_projection(difficulty),
                                  self.response_projection(score)), dim=-1)
            hidden = self.state_cell(features, hidden)
            return torch.sigmoid(self.mastery_head(hidden)), hidden

def load_manifest(model_dir: Path, version: str) -> dict:
    manifest_path = model_dir / version / "manifest.json"
    if not manifest_path.exists():
        raise FileNotFoundError(f"DIMKT manifest missing: {manifest_path}")
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    weights = model_dir / version / manifest["weightsFile"]
    actual = hashlib.sha256(weights.read_bytes()).hexdigest()
    if actual != manifest["weightsSha256"]:
        raise ValueError("DIMKT weights hash mismatch")
    return manifest
