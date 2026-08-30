from __future__ import annotations
import os
import uuid
from pathlib import Path
from typing import Dict

from src.models.dimkt import load_manifest, torch, DIMKTModel
from src.schemas.dimkt import DimktInferRequest, DimktInferenceResponse

class DimktService:
    def __init__(self):
        self.model_dir = Path(os.getenv("DIMKT_MODEL_DIR", "models/dimkt"))
        self._states: Dict[str, dict] = {}
        self._loaded: Dict[str, tuple] = {}

    def health(self, version: str) -> dict:
        try:
            manifest = load_manifest(self.model_dir, version)
            if torch is None:
                return {"ready": False, "modelVersion": version, "reason": "PYTORCH_NOT_INSTALLED"}
            return {"ready": True, "modelVersion": version,
                    "knowledgeIndexVersion": manifest["knowledgeIndexVersion"],
                    "knowledgeCount": len(manifest["knowledgeIndex"])}
        except Exception as exc:
            return {"ready": False, "modelVersion": version, "reason": str(exc)}

    def infer(self, request: DimktInferRequest, recalibrate: bool = False) -> DimktInferenceResponse:
        health = self.health(request.modelVersion)
        if not health["ready"]:
            raise RuntimeError(health["reason"])
        manifest, model = self._model(request.modelVersion)
        if manifest["knowledgeIndexVersion"] != request.knowledgeIndexVersion:
            raise ValueError("KNOWLEDGE_INDEX_VERSION_MISMATCH")
        index = {str(code): i for i, code in enumerate(manifest["knowledgeIndex"])}
        saved_state = {} if recalibrate else dict(self._states.get(request.previousStateRef or "", {}))
        hidden_size = int(manifest.get("hiddenSize", 128))
        hidden_values = saved_state.get("hidden")
        hidden = torch.tensor(hidden_values, dtype=torch.float32) if hidden_values else torch.zeros((1, hidden_size))
        mastery_values = saved_state.get("mastery", {})
        processed = 0
        with torch.no_grad():
            for event in sorted(request.interactions, key=lambda x: x.interactionSeq):
                processed = max(processed, event.interactionSeq)
                for code, weight in event.knowledgeWeights.items():
                    if code not in index:
                        raise ValueError(f"UNKNOWN_KNOWLEDGE_INDEX:{code}")
                    knowledge = torch.tensor([index[code]], dtype=torch.long)
                    difficulty = torch.tensor([[event.questionDifficulty]], dtype=torch.float32)
                    score = torch.tensor([[event.scoreNormalized * float(weight)]], dtype=torch.float32)
                    mastery_head, hidden = model(knowledge, difficulty, score, hidden)
                    mastery_values = {name: float(mastery_head[0, idx]) for name, idx in index.items()}
        ref = "dimkt_state_" + uuid.uuid4().hex
        self._states[ref] = {"hidden": hidden.tolist(), "mastery": mastery_values}
        mastery = {code: float(mastery_values.get(code, 0.30)) for code in index}
        confidence = {code: min(1.0, 0.35 + 0.08 * len(request.interactions)) for code in index}
        return DimktInferenceResponse(masteryHead=mastery, confidence=confidence,
                                      stateRef=ref, processedThroughSeq=processed,
                                      modelVersion=request.modelVersion,
                                      knowledgeIndexVersion=request.knowledgeIndexVersion)

    def _model(self, version: str):
        if version not in self._loaded:
            manifest = load_manifest(self.model_dir, version)
            model = DIMKTModel(len(manifest["knowledgeIndex"]), int(manifest.get("hiddenSize", 128)))
            weights = self.model_dir / version / manifest["weightsFile"]
            model.load_state_dict(torch.load(weights, map_location="cpu"))
            model.eval()
            self._loaded[version] = (manifest, model)
        return self._loaded[version]
