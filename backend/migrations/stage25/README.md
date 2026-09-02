# stage25 — 图关系唯一键按图版本隔离

## 背景

`knowledge_relation.uk_knowledge_relation` 建于 stage01 时期，键为
`(source_id, target_id, relation_type)`，没有包含 `graph_version_id`。

在二阶段的版本化图谱模型下这是一个阻断缺陷：

- 同一课程的新 DRAFT 版本只要复用任何一条既有边，`batchInsert` 就会命中
  `Duplicate entry` 并以 `SYSTEM_ERROR` 500 失败，新版本无法建立；
- `deleteByVersion` 是按 `graph_version_id` 的硬删除，因此唯一键按版本隔离后
  同一版本内仍然保持"同源同宿同类型只有一条边"的语义，不会放松约束。

验收依据：`后端功能重实现与接口验收文档.md` TABLE-003
「边唯一 `(graph_version_id,source,target,relation_type)`」。

## 复现

```
POST /api/v1/admin/courses/{id}/graph-versions        -> DRAFT v2
PUT  /api/v1/admin/graph-versions/{v2}/relations      -> 500 SYSTEM_ERROR
     （请求体中包含任意一条已被 v1 使用的边）
```

## 执行顺序

```
stage25_01_precheck.sql   # 确认无 NULL graph_version_id、按新键无重复
stage25_02_schema.sql     # 替换唯一键
stage25_03_migrate.sql    # 无数据迁移（仅索引变更），保留显式说明
stage25_04_verify.sql     # 校验新键存在、旧键已移除、无重复
stage25_05_cleanup.sql    # 默认不执行；仅在需要回滚时使用
```

## 影响面

只影响 `knowledge_relation` 的索引定义，不改列、不改数据。
`fk_knowledge_relation_source/target` 外键依赖 `source_id/target_id` 前缀索引，
因此 `stage25_02_schema.sql` 先补建 `idx_knowledge_relation_source_fk`
再删除旧唯一键，避免 MySQL 报 errno 150。
