package net.moonmile.ble5_chat.copilot.util

/**
 * 参加端末の最終受信時刻管理
 */
class PeerRegistry {
    private val peers = mutableMapOf<String, Long>()
    private val peerTimeoutMs = 30_000L  // 30 秒でタイムアウト

    /**
     * ピアを登録 / 更新
     *
     * @param peerId ピア ID（端末 ID など）
     */
    fun registerPeer(peerId: String) {
        peers[peerId] = System.currentTimeMillis()
    }

    /**
     * アクティブなピア数を取得（タイムアウトしたピアは除外）
     *
     * @return アクティブなピア数
     */
    fun getActivePeerCount(): Int {
        val now = System.currentTimeMillis()
        return peers.count { (_, lastSeen) ->
            now - lastSeen < peerTimeoutMs
        }
    }

    /**
     * すべてのピアを取得
     *
     * @return ピア ID のリスト
     */
    fun getAllPeers(): List<String> {
        return peers.keys.toList()
    }

    /**
     * レジストリをリセット
     */
    fun reset() {
        peers.clear()
    }
}
