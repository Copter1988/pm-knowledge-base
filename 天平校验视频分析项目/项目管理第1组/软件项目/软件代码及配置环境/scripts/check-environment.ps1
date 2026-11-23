param(
    [string]$MavenCommand = "mvn",
    [string]$FfmpegPath = "E:\ffmpeg\bin\ffmpeg.exe",
    [string]$OcrPython = ".\.ocr-venv\Scripts\python.exe",
    [int]$Port = 8080
)

$ErrorActionPreference = "Continue"
$results = @()

function Add-Result {
    param(
        [string]$Check,
        [string]$Status,
        [string]$Detail
    )
    $script:results += [PSCustomObject]@{
        Check = $Check
        Status = $Status
        Detail = $Detail
    }
}

function Test-CommandPath {
    param([string]$Command)
    $resolved = Get-Command $Command -ErrorAction SilentlyContinue
    if ($resolved) {
        return $resolved.Source
    }
    if (Test-Path -LiteralPath $Command) {
        return (Resolve-Path -LiteralPath $Command).Path
    }
    return $null
}

$mavenPath = Test-CommandPath $MavenCommand
if ($mavenPath) {
    $mavenOutput = & $mavenPath -version 2>&1 | Out-String
    $javaLine = ($mavenOutput -split "`r?`n" |
        Where-Object { $_ -match "Java version" } |
        Select-Object -First 1)
    Add-Result "Maven/JDK" "PASS" "$mavenPath; $javaLine"
} else {
    Add-Result "Maven/JDK" "FAIL" "Maven not found: $MavenCommand"
}

if (-not [string]::IsNullOrWhiteSpace($env:TPJY_DB_USER) -and
    -not [string]::IsNullOrWhiteSpace($env:TPJY_DB_PASSWORD)) {
    Add-Result "MySQL credentials" "PASS" "TPJY_DB_USER and TPJY_DB_PASSWORD are set"
} else {
    Add-Result "MySQL credentials" "FAIL" "Set TPJY_DB_USER and TPJY_DB_PASSWORD"
}

if (Test-Path -LiteralPath $FfmpegPath) {
    $ffmpegVersion = (& $FfmpegPath -version 2>&1 | Select-Object -First 1)
    Add-Result "ffmpeg" "PASS" "$FfmpegPath; $ffmpegVersion"
} else {
    Add-Result "ffmpeg" "FAIL" "Path not found: $FfmpegPath"
}

if (Test-Path -LiteralPath $OcrPython) {
    $ocrOutput = (& $OcrPython -c "from importlib.metadata import version; import sys; print(sys.version.split()[0]); print(version('rapidocr_onnxruntime'))" 2>&1 | Out-String).Trim()
    if ($LASTEXITCODE -eq 0) {
        $ocrFlat = $ocrOutput -replace "`r?`n", " / "
        Add-Result "Local OCR" "PASS" "$OcrPython; Python/RapidOCR=$ocrFlat"
    } else {
        Add-Result "Local OCR" "FAIL" "RapidOCR import failed: $ocrOutput"
    }
} else {
    Add-Result "Local OCR" "FAIL" "Missing $OcrPython; run scripts/setup-local-ocr.ps1"
}

$requiredFiles = @(
    "pom.xml",
    "src\main\resources\application.yml",
    "src\main\resources\sql\schema.sql",
    "scripts\local_ocr_worker.py",
    "scripts\test_local_ocr_worker.py"
)
$missingFiles = @($requiredFiles |
    Where-Object { -not (Test-Path -LiteralPath $_) })
if ($missingFiles.Count -eq 0) {
    Add-Result "Project files" "PASS" "Build, config, SQL and OCR files found"
} else {
    Add-Result "Project files" "FAIL" "Missing: $($missingFiles -join ', ')"
}

$writableDirs = @("upload\video", "upload\frame", "backups")
$dirErrors = @()
foreach ($dir in $writableDirs) {
    try {
        New-Item -ItemType Directory -Path $dir -Force | Out-Null
        $probe = Join-Path $dir ".write-check-$PID"
        Set-Content -LiteralPath $probe -Value "ok" -Encoding ASCII
        Remove-Item -LiteralPath $probe -Force
    } catch {
        $dirErrors += "$dir ($($_.Exception.Message))"
    }
}
if ($dirErrors.Count -eq 0) {
    Add-Result "Runtime dirs" "PASS" "upload/video, upload/frame and backups are writable"
} else {
    Add-Result "Runtime dirs" "FAIL" ($dirErrors -join "; ")
}

$listener = Get-NetTCPConnection -State Listen -LocalPort $Port -ErrorAction SilentlyContinue |
    Select-Object -First 1
if ($listener) {
    Add-Result "Application port" "WARN" "Port $Port is in use by PID $($listener.OwningProcess)"
} else {
    Add-Result "Application port" "PASS" "Port $Port is available"
}

$drive = Get-PSDrive -Name ((Get-Item -LiteralPath ".").PSDrive.Name) -ErrorAction SilentlyContinue
if ($drive -and $drive.Free -ge 5GB) {
    $freeGb = [math]::Round($drive.Free / 1GB, 1)
    Add-Result "Disk space" "PASS" "$freeGb GB free"
} elseif ($drive) {
    $freeGb = [math]::Round($drive.Free / 1GB, 1)
    Add-Result "Disk space" "WARN" "$freeGb GB free; at least 5 GB recommended"
} else {
    Add-Result "Disk space" "WARN" "Unable to read free disk space"
}

$results | Format-Table -AutoSize
$failures = @($results | Where-Object { $_.Status -eq "FAIL" })
$warnings = @($results | Where-Object { $_.Status -eq "WARN" })

if ($failures.Count -gt 0) {
    Write-Output "Environment check failed: $($failures.Count) FAIL, $($warnings.Count) WARN"
    exit 1
}

Write-Output "Environment check passed: 0 FAIL, $($warnings.Count) WARN"
exit 0
