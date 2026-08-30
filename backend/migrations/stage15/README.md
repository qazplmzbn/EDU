# Stage 15 — DIMKT rollout state

DIMKT is disabled by default. Enable only after the Python health endpoint verifies a versioned manifest, knowledge index, and matching weights hash, and offline evaluation passes. Java always retains `weighted_bkt_elo_v1` as fallback. State references are incremental and can be rebuilt by recalibration.
