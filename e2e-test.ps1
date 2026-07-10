<#
  e2e-test.ps1 - end-to-end smoke test for day 10 (core -> Kafka -> notification).
  Flow: register Alice/Bob -> login -> userId from JWT -> mutual like -> check notification log.

  Prereqs:
    - stack is up: docker compose up (core = healthy, notification = up with partitions assigned);
    - run FROM THE PROJECT DIR (where docker-compose.yaml lives) so `docker compose logs` finds the service.

  Run:
    powershell -ExecutionPolicy Bypass -File .\e2e-test.ps1
    .\e2e-test.ps1 -Gateway http://localhost:8088

  NOTE: kept ASCII-only on purpose (Windows PowerShell 5.1 mangles non-ASCII in .ps1 without a BOM).
#>

param(
    [string]$Gateway = "http://localhost:8088",
    [string]$NotificationService = "notification"
)

$ErrorActionPreference = "Stop"

# --- helpers ---------------------------------------------------------------

# HTTP with retry on 503 (cold-start fallback: first request is cold, TimeLimiter may cut it off).
function Invoke-Json {
    param(
        [string]$Method,
        [string]$Uri,
        [hashtable]$Body,
        [string]$Token,
        [int]$Retries = 6
    )
    $headers = @{}
    if ($Token) { $headers["Authorization"] = "Bearer $Token" }
    $json = if ($Body) { $Body | ConvertTo-Json -Compress } else { $null }

    for ($i = 1; $i -le $Retries; $i++) {
        try {
            if ($json) {
                return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers `
                    -ContentType "application/json" -Body $json
            } else {
                return Invoke-RestMethod -Method $Method -Uri $Uri -Headers $headers
            }
        } catch {
            $resp = $_.Exception.Response
            $code = if ($resp) { [int]$resp.StatusCode } else { 0 }
            if ($code -eq 503 -and $i -lt $Retries) {
                Write-Host "     ...503 (cold-start fallback), retry $i/$Retries in 2s" -ForegroundColor DarkYellow
                Start-Sleep -Seconds 2
                continue
            }
            throw
        }
    }
}

# userId = claim "sub" from JWT (no DB access). Decode base64url of the middle segment.
function Get-JwtSub {
    param([string]$Jwt)
    $p = $Jwt.Split('.')[1].Replace('-', '+').Replace('_', '/')
    switch ($p.Length % 4) { 2 { $p += '==' } 3 { $p += '=' } }
    $payload = [System.Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($p))
    return ($payload | ConvertFrom-Json).sub
}

# --- scenario --------------------------------------------------------------

$stamp = Get-Date -Format "yyyyMMdd-HHmmss"
$pass  = "password123"
$alice = "alice_$stamp@x.ru"
$bob   = "bob_$stamp@x.ru"

Write-Host "=== E2E day 10 - gateway=$Gateway ===" -ForegroundColor White

# 0. Warmup: first register is cold (lazy Kafka producer + JIT). Result ignored.
Write-Host "0. Warmup (absorb cold-start 503)..." -ForegroundColor Cyan
try { Invoke-Json -Method POST -Uri "$Gateway/api/auth/register" `
        -Body @{ email = "warmup_$stamp@x.ru"; password = $pass; displayName = "Warm" } | Out-Null } catch {}

# 1. Register two users (unique emails -> no 409, no 'already matched').
Write-Host "1. Register Alice / Bob" -ForegroundColor Cyan
Invoke-Json -Method POST -Uri "$Gateway/api/auth/register" -Body @{ email = $alice; password = $pass; displayName = "Alice" } | Out-Null
Invoke-Json -Method POST -Uri "$Gateway/api/auth/register" -Body @{ email = $bob;   password = $pass; displayName = "Bob"   } | Out-Null

# 2. Login -> accessToken.
Write-Host "2. Login -> tokens" -ForegroundColor Cyan
$aliceTok = (Invoke-Json -Method POST -Uri "$Gateway/api/auth/login" -Body @{ email = $alice; password = $pass }).accessToken
$bobTok   = (Invoke-Json -Method POST -Uri "$Gateway/api/auth/login" -Body @{ email = $bob;   password = $pass }).accessToken

# 3. userId from JWT.
$aliceId = Get-JwtSub $aliceTok
$bobId   = Get-JwtSub $bobTok
Write-Host "   Alice = $aliceId" -ForegroundColor DarkGray
Write-Host "   Bob   = $bobId"   -ForegroundColor DarkGray

# 4. Mutual like.
Write-Host "3. Alice likes Bob" -ForegroundColor Cyan
$r1 = Invoke-Json -Method POST -Uri "$Gateway/api/matching/setLike" -Body @{ toUserId = $bobId } -Token $aliceTok
Write-Host "   match=$($r1.match)  (expected false)"
if ($r1.match -ne $false) { Write-Host "   WARN: expected false" -ForegroundColor Yellow }

Write-Host "4. Bob likes Alice -> should create a match" -ForegroundColor Cyan
$r2 = Invoke-Json -Method POST -Uri "$Gateway/api/matching/setLike" -Body @{ toUserId = $aliceId } -Token $bobTok
$matchColor = if ($r2.match) { "Green" } else { "Red" }
Write-Host "   match=$($r2.match)  (expected true)" -ForegroundColor $matchColor

# 5. End of chain: notification must log the delivery (Kafka async).
#    Find a log line containing BOTH userIds (that is the match delivery line;
#    register lines carry only one id - so we skip them without depending on non-ASCII text).
Write-Host "5. Waiting for notification delivery (up to 25s)..." -ForegroundColor Cyan
$hit = $null
for ($i = 0; $i -lt 25; $i++) {
    $hit = docker compose logs $NotificationService --since 300s 2>$null |
           Select-String -SimpleMatch $aliceId | Select-String -SimpleMatch $bobId
    if ($hit) { break }
    Start-Sleep -Seconds 1
}

Write-Host ""
if ($hit) {
    Write-Host "PASS - end-to-end flow works. notification delivery line:" -ForegroundColor Green
    $hit | ForEach-Object { Write-Host "   $($_.Line.Trim())" -ForegroundColor Green }
    exit 0
} else {
    Write-Host "FAIL - no notification line with both ids found within 25s." -ForegroundColor Red
    Write-Host "Diagnostics:" -ForegroundColor DarkGray
    Write-Host "   docker compose ps" -ForegroundColor DarkGray
    Write-Host "   docker compose logs $NotificationService --tail 40" -ForegroundColor DarkGray
    Write-Host "   docker compose logs core --tail 40   # MatchCreated publish" -ForegroundColor DarkGray
    exit 1
}
