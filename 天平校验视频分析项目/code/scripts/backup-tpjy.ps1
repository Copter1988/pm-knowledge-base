param(
    [string]$DbHost = "127.0.0.1",
    [string]$DbName = "tpjy",
    [string]$OutDir = (Join-Path $PSScriptRoot "..\backups"),
    [string]$MySqlDump = "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe",
    [int]$KeepDays = 30
)

$ErrorActionPreference = "Stop"
$dbUser = $env:TPJY_DB_USER
$dbPassword = $env:TPJY_DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($dbUser) -or [string]::IsNullOrWhiteSpace($dbPassword)) {
    throw "请先设置环境变量 TPJY_DB_USER 和 TPJY_DB_PASSWORD"
}
if (-not (Test-Path -LiteralPath $MySqlDump)) {
    throw "找不到 mysqldump：$MySqlDump"
}

$backupRoot = [System.IO.Path]::GetFullPath($OutDir)
New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$backupFile = Join-Path $backupRoot "tpjy-$stamp.sql"
$optionFile = [System.IO.Path]::GetTempFileName()

try {
    $client = "[client]`r`nuser=$dbUser`r`npassword=$dbPassword`r`nhost=$DbHost`r`n"
    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($optionFile, $client, $utf8NoBom)

    & $MySqlDump "--defaults-extra-file=$optionFile" "--single-transaction" `
        "--routines" "--triggers" "--result-file=$backupFile" $DbName
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $backupFile)) {
        throw "mysqldump 执行失败，退出码 $LASTEXITCODE"
    }

    $cutoff = (Get-Date).AddDays(-$KeepDays)
    Get-ChildItem -LiteralPath $backupRoot -Filter "tpjy-*.sql" -File |
        Where-Object { $_.LastWriteTime -lt $cutoff } |
        Remove-Item -Force

    Write-Output "备份完成：$backupFile"
}
finally {
    if (Test-Path -LiteralPath $optionFile) {
        Remove-Item -LiteralPath $optionFile -Force
    }
}
