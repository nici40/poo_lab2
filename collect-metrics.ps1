# OOP Lab 2 - Track 2: metrics collection
# Asks the agent the same 8 questions at memory tier 1 and tier 3,
# then writes metrics\metrics.csv plus the full TRACE logs.
#
# Before running:
#   1. LM Studio -> Developer -> start the server on port 1234
#   2. the model below must be loaded (check with: lms ps)
#
# Run:  powershell -ExecutionPolicy Bypass -File .\collect-metrics.ps1

param(
    [string]$model = "qwen/qwen3-4b-2507",
    [string]$personality = "a grumpy pirate who answers in short sentences",
    [string]$name = "Barbossa",
    [string]$tag  = ""      # suffix for the output files, e.g. -tag "8b" -> metrics-8b.csv
)

if ($tag) { $tag = "-$tag" }

Set-Location $PSScriptRoot

# ---- find a JDK -------------------------------------------------------------
# javac is often not on PATH when the JDK came with IntelliJ, so look for it.
function Find-Jdk {
    if (Get-Command javac -ErrorAction SilentlyContinue) {
        return @{ javac = "javac"; java = "java"; where = "PATH" }
    }
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\javac.exe")) {
        return @{ javac = "$env:JAVA_HOME\bin\javac.exe"; java = "$env:JAVA_HOME\bin\java.exe"; where = "JAVA_HOME" }
    }
    $patterns = @(
        "$env:USERPROFILE\.jdks\*\bin\javac.exe",
        "C:\Program Files\Java\*\bin\javac.exe",
        "C:\Program Files\Eclipse Adoptium\*\bin\javac.exe",
        "C:\Program Files\Microsoft\jdk-*\bin\javac.exe",
        "C:\Program Files\Amazon Corretto\*\bin\javac.exe",
        "C:\Program Files\JetBrains\*\jbr\bin\javac.exe",
        "$env:LOCALAPPDATA\Programs\*\jbr\bin\javac.exe"
    )
    foreach ($p in $patterns) {
        $hit = Get-ChildItem $p -ErrorAction SilentlyContinue |
               Sort-Object FullName -Descending | Select-Object -First 1
        if ($hit) {
            return @{ javac = $hit.FullName
                      java  = (Join-Path $hit.Directory.FullName "java.exe")
                      where = $hit.FullName }
        }
    }
    return $null
}

$jdk = Find-Jdk
if (-not $jdk) {
    Write-Host "No JDK found. Install one, or set JAVA_HOME to your IntelliJ JDK." -ForegroundColor Red
    Write-Host 'Find it with:  Get-ChildItem -Path C:\ -Filter javac.exe -Recurse -ErrorAction SilentlyContinue | Select-Object -First 5 FullName' -ForegroundColor DarkGray
    exit 1
}
$javac = $jdk.javac
$java  = $jdk.java
Write-Host "Using JDK: $($jdk.where)" -ForegroundColor DarkGray
& $javac -version

# ---- compile ----------------------------------------------------------------
Write-Host "Compiling..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force metrics | Out-Null
& $javac -d out src/main/java/org/example/*.java src/test/java/org/example/*.java
if ($LASTEXITCODE -ne 0) { Write-Host "Compile failed." -ForegroundColor Red; exit 1 }

# ---- check the model server is actually up ----------------------------------
try {
    Invoke-RestMethod -Uri "http://localhost:1234/v1/models" -TimeoutSec 5 | Out-Null
} catch {
    Write-Host "LM Studio is not answering on port 1234. Start the local server first." -ForegroundColor Red
    exit 1
}

# ---- run both tiers ---------------------------------------------------------
$questions = @(
    "My name is Nekit.",
    "What is 12 * 7?",
    "Roll a 6-sided dice.",
    "What time is it?",
    "What is today's date?",
    "What is 144 / 12?",
    "Tell me a pirate joke.",
    "What is my name?",
    "quit"
)

$rows = @()
foreach ($tier in 1, 3) {
    Write-Host "Running tier $tier..." -ForegroundColor Cyan
    $log = $questions | & $java -cp out org.example.AgentMain --model $model --tier $tier `
               --level TRACE --name $name --personality $personality 2>&1
    $log | Set-Content "metrics\tier$tier$tag-log.txt"

    $n = 0
    $msgs = 0
    foreach ($line in $log) {
        if ("$line" -match 'Request sent \((\d+) messages\)') { $msgs = [int]$matches[1] }
        if ("$line" -match 'Answer: (\d+) prompt tokens, (\d+) completion tokens, ([\d.,]+) tokens/s') {
            $n++
            $rows += [pscustomobject]@{
                Tier             = $tier
                Request          = $n
                MessagesSent     = $msgs
                PromptTokens     = [int]$matches[1]
                CompletionTokens = [int]$matches[2]
                TokensPerSec     = $matches[3]
            }
        }
    }
    $dropped = ($log | Select-String 'Memory is full').Count
    Write-Host "  tier $tier : $n requests, $dropped memory-drop warnings" -ForegroundColor DarkGray
}

$rows | Format-Table -AutoSize
$rows | Export-Csv "metrics\metrics$tag.csv" -NoTypeInformation
Write-Host ""
Write-Host "Done. metrics\metrics$tag.csv, metrics\tier1$tag-log.txt, metrics\tier3$tag-log.txt" -ForegroundColor Green
Write-Host "Model: $model  |  Personality: $personality" -ForegroundColor DarkGray
