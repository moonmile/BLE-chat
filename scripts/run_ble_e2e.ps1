param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$SerialA,

    [Parameter(Mandatory = $true, Position = 1)]
    [string]$SerialB,

    [Parameter(Position = 2)]
    [string]$TestName = "sendHelloOnce"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$Package = "net.moonmile.ble5_chat.claude"
$Runner = "androidx.test.runner.AndroidJUnitRunner"
$SendClass = "$Package.BleChatSendTest#$TestName"
$RecvClass = "$Package.BleChatReceiveTest#dumpReceivedMessages"

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$RepoRoot = Split-Path -Parent $ScriptDir
$SrcRoot = Join-Path $RepoRoot "src"
$BuildDir = Join-Path $SrcRoot "build"

$ApkApp = Join-Path $SrcRoot "app/build/outputs/apk/debug/app-debug.apk"
$ApkTest = Join-Path $SrcRoot "app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"
$LogFile = Join-Path $BuildDir ("recv_{0}.log" -f (Get-Date -Format "yyyyMMdd_HHmmss"))

function Write-Step([string]$Message) {
    Write-Host ""
    Write-Host $Message
}

function Invoke-Adb {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,

        [switch]$IgnoreExitCode,

        [switch]$ReturnOutput
    )

    $output = & adb @Arguments 2>&1
    $exitCode = $LASTEXITCODE

    if (-not $IgnoreExitCode -and $exitCode -ne 0) {
        $joined = $Arguments -join ' '
        throw "adb $joined failed with exit code $exitCode`n$output"
    }

    if ($ReturnOutput) {
        return ($output | Out-String).TrimEnd()
    }
}

function Assert-FileExists([string]$Path) {
    if (-not (Test-Path $Path)) {
        throw "ファイルが見つかりません: $Path"
    }
}

Write-Host "======================================================"
Write-Host " BLE チャット E2E テスト"
Write-Host "  送信端末 (A): $SerialA"
Write-Host "  受信端末 (B): $SerialB"
Write-Host "  送信テスト  : $TestName"
Write-Host "======================================================"

Write-Step "[Step 0] adb 接続端末を確認..."
$devices = Invoke-Adb -Arguments @("devices") -ReturnOutput
if ($devices -notmatch [regex]::Escape($SerialA)) {
    throw "送信端末が adb devices に見つかりません: $SerialA"
}
if ($devices -notmatch [regex]::Escape($SerialB)) {
    throw "受信端末が adb devices に見つかりません: $SerialB"
}
Write-Host "  adb 接続確認 OK"

Write-Step "[Step 1] APK ビルド..."
Push-Location $SrcRoot
try {
    & .\gradlew.bat assembleDebug assembleDebugAndroidTest -q
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle ビルドに失敗しました。"
    }
}
finally {
    Pop-Location
}

Assert-FileExists $ApkApp
Assert-FileExists $ApkTest
Write-Host "  完了: $ApkApp"
Write-Host "  完了: $ApkTest"

Write-Step "[Step 2] APK インストール..."
foreach ($serial in @($SerialA, $SerialB)) {
    Write-Host "  端末: $serial"
    Invoke-Adb -Arguments @("-s", $serial, "install", "-r", "-t", $ApkApp)
    Invoke-Adb -Arguments @("-s", $serial, "install", "-r", "-t", $ApkTest)
    Write-Host "    → インストール完了"
}

Write-Step "[Step 3] 受信端末 ($SerialB) のログ収集を開始..."
New-Item -ItemType Directory -Path $BuildDir -Force | Out-Null
Invoke-Adb -Arguments @("-s", $SerialB, "logcat", "-c")

$logJob = Start-Job -Name "ble-recv-logcat" -ArgumentList $SerialB, $LogFile -ScriptBlock {
    param($JobSerial, $JobLogFile)
    & adb -s $JobSerial logcat -s "BleChatTest:I" "*:S" 2>&1 |
        Tee-Object -FilePath $JobLogFile
}

Write-Host "  ログファイル: $LogFile  (Job Id: $($logJob.Id))"

Write-Step "[Step 4] 受信端末でアプリを起動..."
Invoke-Adb -Arguments @("-s", $SerialB, "shell", "am", "start", "-n", "$Package/.MainActivity")
Start-Sleep -Seconds 3

Write-Step "[Step 5] 受信テストを受信端末で開始..."
$recvJob = Start-Job -Name "ble-recv-test" -ArgumentList $SerialB, $RecvClass, $Package, $Runner -ScriptBlock {
    param($JobSerial, $JobRecvClass, $JobPackage, $JobRunner)
    & adb -s $JobSerial shell am instrument -w -e class $JobRecvClass "$JobPackage.test/$JobRunner"
}
Write-Host "  受信テスト Job Id: $($recvJob.Id)"

Start-Sleep -Seconds 2

Write-Step "[Step 6] 送信テストを送信端末で実行: $TestName"
Write-Host "------------------------------------------------------"
Invoke-Adb -Arguments @("-s", $SerialA, "shell", "am", "instrument", "-w", "-e", "class", $SendClass, "$Package.test/$Runner")
Write-Host "------------------------------------------------------"
Write-Host "  送信テスト完了"

Write-Step "[Step 7] 受信テスト終了を待機..."
Wait-Job -Id $recvJob.Id | Out-Null
Receive-Job -Id $recvJob.Id -Keep | Out-Host

Write-Step "[Step 8] ログ収集を停止して結果表示..."
Start-Sleep -Seconds 2
Stop-Job -Id $logJob.Id | Out-Null
$null = Receive-Job -Id $logJob.Id -Keep -ErrorAction SilentlyContinue
Remove-Job -Id $logJob.Id -Force -ErrorAction SilentlyContinue
Remove-Job -Id $recvJob.Id -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "======================================================"
Write-Host " 受信ログ: $LogFile"
Write-Host "======================================================"
if (Test-Path $LogFile) {
    $recvLines = Select-String -Path $LogFile -Pattern "\[RECV\]" -SimpleMatch:$false
    if ($recvLines) {
        $recvLines | ForEach-Object { Write-Host $_.Line }
    }
    else {
        Write-Host "  (受信メッセージなし)"
    }
}
else {
    Write-Host "  (ログファイル未作成)"
}
Write-Host "======================================================"
