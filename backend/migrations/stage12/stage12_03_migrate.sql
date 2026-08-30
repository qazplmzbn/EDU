-- Existing teacher resources remain compatibility content. New student bundles start empty and are created only by the reviewed workflow.
UPDATE resource_source SET evidence_type=COALESCE(evidence_type,support_type),support_score=COALESCE(support_score,relevance_score) WHERE resource_id IS NOT NULL;
