# MySQL Docker environments

This setup uses **one MySQL 8.0.46 container and one named Docker volume**. Environments are separate complete databases, not separate table groups or separate containers.

- `question_bank`: Docker source snapshot. Import the verified backup here only when you are ready to cut over from the host MySQL service.
- `question_bank_restore_verify_YYYYMMDD`: restore/rehearsal database.
- `question_bank_stage08_YYYYMMDD`: create on demand from the same backup immediately before the Stage 08 rehearsal. Continue Stage 09–19 on that one stage database; do not create one database per stage.

## Start the container

Copy `.env.example` to `.env`, set `MYSQL_ROOT_PASSWORD`, then run:

```powershell
docker compose -f docker/mysql/compose.yml up -d
docker compose -f docker/mysql/compose.yml ps
```

The default host port is `3307`, so the existing host MySQL on `3306` remains untouched during migration.

## Import a verified backup

The import helper creates a new database and refuses to overwrite an existing one. It prompts for the Docker MySQL root password and does not write it to the repository.

```powershell
.\docker\mysql\scripts\New-DatabaseFromBackup.ps1 `
  -Database question_bank_restore_verify_20260831 `
  -BackupFile .\backups\question_bank_restore_verify_20260831\question_bank_20260831_171700.sql
```

Use the same command with `-Database question_bank` only at the planned source cutover. Before that point, keep the current host `question_bank` as the authoritative source.

## Backend connection examples

Use temporary environment variables or a local ignored profile; do not commit passwords.

```text
jdbc:mysql://127.0.0.1:3307/question_bank_restore_verify_20260831
jdbc:mysql://127.0.0.1:3307/question_bank_stage08_20260831
```

## Data safety

- Do not run Stage 08–19 on `question_bank`.
- Restore into a new verification/stage database first.
- Keep the backup file and its SHA-256 from the recovery drill as the baseline.
- The named volume is persistent. Removing the container does not remove data; removing `edu_mysql_data` does.
