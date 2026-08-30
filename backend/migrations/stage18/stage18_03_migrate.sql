UPDATE knowledge_graph_version SET review_status=COALESCE(review_status,'NOT_REQUIRED') WHERE review_status IS NULL;
UPDATE source_document SET review_status=COALESCE(review_status,'NOT_REQUIRED') WHERE review_status IS NULL;
