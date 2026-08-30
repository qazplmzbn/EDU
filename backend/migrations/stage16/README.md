# Stage 16 — Release verification and guarded cutover

Run all Stage 08–15 verify scripts first. Stage 16 adds trace indexes and a release checkpoint, then performs cross-domain invariants. Cleanup never drops columns automatically; a separate reviewed release operation must confirm backups, old-code shutdown, API regression, and restoration tests.
