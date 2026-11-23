param(
    [string]$VenvDir = (Join-Path $PSScriptRoot "..\.ocr-venv"),
    [string]$IndexUrl = "https://pypi.tuna.tsinghua.edu.cn/simple"
)

$ErrorActionPreference = "Stop"
$python = (Get-Command python -ErrorAction Stop).Source
if (-not (Test-Path -LiteralPath $VenvDir)) {
    & $python -m venv $VenvDir --system-site-packages
}
$venvPython = Join-Path $VenvDir "Scripts\python.exe"
& $venvPython -m pip install -r (Join-Path $PSScriptRoot "requirements-ocr.txt") -i $IndexUrl
Write-Output "OCR environment ready: $venvPython"
