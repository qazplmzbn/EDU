param(
    [Parameter(Mandatory)]
    [ValidatePattern('^[A-Za-z0-9_]+$')]
    [string]$Database,

    [Parameter(Mandatory)]
    [ValidateScript({ Test-Path -LiteralPath $_ })]
    [string]$BackupFile
)

$ErrorActionPreference = 'Stop'

$composeFile = Join-Path $PSScriptRoot '..\compose.yml'
$composeFile = (Resolve-Path $composeFile).Path
$BackupFile = (Resolve-Path $BackupFile).Path

$securePassword = Read-Host 'MySQL root password' -AsSecureString
$passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
try {
    $rootPassword = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    $compose = @('compose', '-f', $composeFile)
    $exists = & docker @compose exec -T -e "MYSQL_PWD=$rootPassword" mysql mysql -uroot --batch --skip-column-names --execute "SHOW DATABASES LIKE '$Database';"
    if ($LASTEXITCODE -ne 0) {
        throw 'Unable to connect to the Docker MySQL service. Start it and wait for its health check first.'
    }
    if ($exists) {
        throw "Database '$Database' already exists. Refusing to overwrite it."
    }

    & docker @compose exec -T -e "MYSQL_PWD=$rootPassword" mysql mysql -uroot --execute "CREATE DATABASE `$Database` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    if ($LASTEXITCODE -ne 0) {
        throw "Could not create database '$Database'."
    }

    # Do not pipe SQL text through PowerShell: it can change the dump encoding.
    $containerBackup = "/tmp/edu-import-$([guid]::NewGuid().ToString('N')).sql"
    try {
        & docker cp $BackupFile "edu-mysql:$containerBackup"
        if ($LASTEXITCODE -ne 0) {
            throw 'Could not copy the backup into the Docker MySQL container.'
        }

        & docker @compose exec -T -e "MYSQL_PWD=$rootPassword" mysql mysql -uroot --database=$Database --execute "SOURCE $containerBackup"
        if ($LASTEXITCODE -ne 0) {
            throw "Restore into '$Database' failed. The database was created but may be incomplete."
        }
    }
    finally {
        & docker exec edu-mysql rm -f $containerBackup 2>$null
    }

    Write-Host "Restored $BackupFile into $Database."
}
finally {
    if ($passwordPointer -ne [IntPtr]::Zero) {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
    $rootPassword = $null
}
