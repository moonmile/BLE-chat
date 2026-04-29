#!/usr/bin/env bash
# =============================================================================
# run_ble_e2e.sh  –  BLE チャット E2E テスト実行スクリプト
#
# 使い方:
#   bash scripts/run_ble_e2e.sh <送信端末のSerial> <受信端末のSerial> [テスト名]
#
# テスト名（省略時: sendHelloOnce）:
#   sendHelloOnce                     1回送信
#   sendHelloTenTimesEverySecond      1秒おきに10回
#   sendHelloFiveTimesEveryFiveSeconds 5秒おきに5回
#
# 例:
#   bash scripts/run_ble_e2e.sh R3CN10XXXXX R5CT20YYYYY sendHelloTenTimesEverySecond
#
# 事前確認:
#   adb devices          → 2台とも表示されること
#   両端末の Bluetooth が ON になっていること
# =============================================================================
set -euo pipefail

# ── 引数チェック ──────────────────────────────────────────────────
SERIAL_A="${1:?[ERROR] 引数1: 送信端末のSerial が必要です。'adb devices' で確認してください。}"
SERIAL_B="${2:?[ERROR] 引数2: 受信端末のSerial が必要です。'adb devices' で確認してください。}"
TEST_NAME="${3:-sendHelloOnce}"

PACKAGE="net.moonmile.ble5_chat.claude"
RUNNER="androidx.test.runner.AndroidJUnitRunner"
SEND_CLASS="${PACKAGE}.BleChatSendTest#${TEST_NAME}"
RECV_CLASS="${PACKAGE}.BleChatReceiveTest#dumpReceivedMessages"

APK_APP="app/build/outputs/apk/debug/app-debug.apk"
APK_TEST="app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk"

LOG_FILE="build/recv_$(date +%Y%m%d_%H%M%S).log"

echo "======================================================"
echo " BLE チャット E2E テスト"
echo "  送信端末 (A): $SERIAL_A"
echo "  受信端末 (B): $SERIAL_B"
echo "  送信テスト  : $TEST_NAME"
echo "======================================================"

# ── Step 1: APK ビルド ────────────────────────────────────────────
echo ""
echo "[Step 1] APK ビルド..."
cd "$(dirname "$0")/.."
./gradlew assembleDebug assembleDebugAndroidTest -q
echo "  完了: $APK_APP"
echo "  完了: $APK_TEST"

# ── Step 2: 両端末にインストール ──────────────────────────────────
echo ""
echo "[Step 2] APK インストール..."
for SERIAL in "$SERIAL_A" "$SERIAL_B"; do
    echo "  端末: $SERIAL"
    adb -s "$SERIAL" install -r -t "$APK_APP"  > /dev/null
    adb -s "$SERIAL" install -r -t "$APK_TEST" > /dev/null
    echo "    → インストール完了"
done

# ── Step 3: 受信端末のログ収集を開始（バックグラウンド） ──────────
echo ""
echo "[Step 3] 受信端末 ($SERIAL_B) のログ収集を開始..."
mkdir -p build
adb -s "$SERIAL_B" logcat -c                          # ログバッファをクリア
adb -s "$SERIAL_B" logcat -s "BleChatTest:I" "*:S" \
    | tee "$LOG_FILE" &
LOG_PID=$!
echo "  ログファイル: $LOG_FILE  (PID: $LOG_PID)"

# ── Step 4: 受信端末でアプリを起動 ───────────────────────────────
echo ""
echo "[Step 4] 受信端末でアプリを起動..."
adb -s "$SERIAL_B" shell am start -n "${PACKAGE}/.MainActivity" > /dev/null
sleep 3   # BLE 初期化待機

# ── Step 5: 受信テストを受信端末でバックグラウンド実行 ────────────
echo ""
echo "[Step 5] 受信テストを受信端末で開始..."
adb -s "$SERIAL_B" shell am instrument -w \
    -e class "$RECV_CLASS" \
    "${PACKAGE}.test/${RUNNER}" &
RECV_PID=$!
echo "  受信テスト PID: $RECV_PID"

sleep 2   # 受信テストが先に起動するよう少し待つ

# ── Step 6: 送信テストを送信端末で実行（フォアグラウンド） ──────
echo ""
echo "[Step 6] 送信テストを送信端末で実行: $TEST_NAME"
echo "------------------------------------------------------"
adb -s "$SERIAL_A" shell am instrument -w \
    -e class "$SEND_CLASS" \
    "${PACKAGE}.test/${RUNNER}"
echo "------------------------------------------------------"
echo "  送信テスト完了"

# ── Step 7: 受信テスト終了まで待機 ───────────────────────────────
echo ""
echo "[Step 7] 受信テスト終了を待機..."
wait "$RECV_PID" 2>/dev/null || true

# ── Step 8: ログ収集を停止して結果表示 ───────────────────────────
sleep 2
kill "$LOG_PID" 2>/dev/null || true
wait "$LOG_PID" 2>/dev/null || true

echo ""
echo "======================================================"
echo " 受信ログ: $LOG_FILE"
echo "======================================================"
grep "\[RECV\]" "$LOG_FILE" || echo "  (受信メッセージなし)"
echo "======================================================"
