package net.moonmile.ble5_chat.claude.repository

import android.content.SharedPreferences
import net.moonmile.ble5_chat.claude.model.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeast
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * FavoriteRepository のユニットテスト
 *
 * SharedPreferences / SharedPreferences.Editor を mock に差し替え、
 * - toggle / remove / isFavorite のロジック
 * - StateFlow へのリアルタイム反映
 * - SharedPreferences への save（putString / apply 呼び出し）
 * - load による初期値の復元
 * を検証する。
 *
 * NOTE: JVM ユニットテスト環境では org.json.JSONArray がスタブ化されるため、
 * JSON の中身を直接検証するテスト（シリアライズ内容の確認等）は行わない。
 * save の呼び出し自体と State の変化を中心に検証する。
 * また、anyOrNull() を使用するのは save() 内で arr.toString() が null を返す
 * スタブ挙動に対応するためである。
 */
class FavoriteRepositoryTest {

    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor
    private lateinit var repository: FavoriteRepository

    // ── テスト用サンプルメッセージ ────────────────────────────────
    private fun msg(
        id: String     = "msg-001",
        sender: String = "Alice",
        ts: Long       = 1_700_000_000_000L,
        text: String   = "Hello"
    ) = ChatMessage(id, sender, ts, text)

    @Before
    fun setUp() {
        // NOTE: save() 内で JSONArray.toString() がスタブで null を返すため
        //       putString の第2引数が null になる。anyOrNull() で null も受け取れるようにする。
        mockEditor = mock<SharedPreferences.Editor>().also { editor ->
            whenever(editor.putString(anyOrNull(), anyOrNull())).thenReturn(editor)
        }
        mockPrefs = mock<SharedPreferences>().also { prefs ->
            whenever(prefs.getString(eq("favorites"), eq("[]"))).thenReturn("[]")
            whenever(prefs.edit()).thenReturn(mockEditor)
        }
        repository = FavoriteRepository(mockPrefs)
    }

    // ── 初期状態 ──────────────────────────────────────────────────
    @Test
    fun `initial favorites is empty`() {
        assertTrue(repository.favorites.value.isEmpty())
    }

    @Test
    fun `isFavorite returns false when empty`() {
        assertFalse(repository.isFavorite("msg-001"))
    }

    @Test
    fun `getString favorites key is called during init`() {
        verify(mockPrefs).getString(eq("favorites"), eq("[]"))
    }

    // ── toggle: 追加 ──────────────────────────────────────────────
    @Test
    fun `toggle adds message to favorites`() {
        val m = msg()
        repository.toggle(m)

        assertEquals(1, repository.favorites.value.size)
        assertEquals(m, repository.favorites.value.first())
    }

    @Test
    fun `isFavorite returns true after toggle add`() {
        repository.toggle(msg())
        assertTrue(repository.isFavorite("msg-001"))
    }

    @Test
    fun `StateFlow updates after toggle add`() {
        val m = msg()
        repository.toggle(m)
        assertEquals(listOf(m), repository.favorites.value)
    }

    // ── toggle: 解除（2回目） ──────────────────────────────────────
    @Test
    fun `toggle twice removes message`() {
        val m = msg()
        repository.toggle(m)
        repository.toggle(m)

        assertTrue(repository.favorites.value.isEmpty())
    }

    @Test
    fun `isFavorite returns false after toggle remove`() {
        val m = msg()
        repository.toggle(m)
        repository.toggle(m)

        assertFalse(repository.isFavorite("msg-001"))
    }

    // ── toggle: 複数メッセージ ────────────────────────────────────
    @Test
    fun `toggle multiple messages`() {
        repository.toggle(msg("msg-001"))
        repository.toggle(msg("msg-002"))
        repository.toggle(msg("msg-003"))

        assertEquals(3, repository.favorites.value.size)
        assertTrue(repository.isFavorite("msg-001"))
        assertTrue(repository.isFavorite("msg-002"))
        assertTrue(repository.isFavorite("msg-003"))
    }

    @Test
    fun `toggle removes specific message leaving others intact`() {
        repository.toggle(msg("msg-001"))
        repository.toggle(msg("msg-002"))
        repository.toggle(msg("msg-001"))   // msg-001 を解除

        assertEquals(1, repository.favorites.value.size)
        assertFalse(repository.isFavorite("msg-001"))
        assertTrue(repository.isFavorite("msg-002"))
    }

    // ── remove() ─────────────────────────────────────────────────
    @Test
    fun `remove deletes message by id`() {
        repository.toggle(msg("msg-001"))
        repository.remove("msg-001")

        assertTrue(repository.favorites.value.isEmpty())
        assertFalse(repository.isFavorite("msg-001"))
    }

    @Test
    fun `remove leaves other messages intact`() {
        repository.toggle(msg("msg-001"))
        repository.toggle(msg("msg-002"))
        repository.remove("msg-001")

        assertEquals(1, repository.favorites.value.size)
        assertEquals("msg-002", repository.favorites.value.first().messageId)
    }

    @Test
    fun `remove on non-existing id does nothing`() {
        repository.toggle(msg("msg-001"))
        repository.remove("not-exist")

        assertEquals(1, repository.favorites.value.size)
    }

    // ── SharedPreferences への保存検証 ───────────────────────────
    @Test
    fun `toggle calls editor putString and apply`() {
        repository.toggle(msg())

        verify(mockEditor).putString(eq("favorites"), anyOrNull())
        verify(mockEditor).apply()
    }

    @Test
    fun `remove calls editor apply`() {
        repository.toggle(msg("msg-001"))
        repository.remove("msg-001")

        // toggle 1回 + remove 1回 = 合計 2回 apply されるはず
        verify(mockEditor, atLeast(2)).apply()
    }

    @Test
    fun `putString is called on every state change`() {
        repository.toggle(msg("msg-001"))  // 1回目
        repository.toggle(msg("msg-002"))  // 2回目
        repository.remove("msg-001")       // 3回目

        verify(mockEditor, times(3)).putString(eq("favorites"), anyOrNull())
    }

    @Test
    fun `edit is called when saving`() {
        repository.toggle(msg())
        // toggle 1回で prefs.edit() が呼ばれる（load 時は呼ばれない）
        verify(mockPrefs, times(1)).edit()
    }

    // ── load による初期値復元 ─────────────────────────────────────
    // NOTE: JVM テスト環境では org.json.JSONArray がスタブ化されるため
    //       JSON のデシリアライズは行われず、load() は常に空リストを返す。
    //       ここでは getString の呼び出しと graceful fallback を検証する。

    @Test
    fun `load falls back to empty list when prefs returns empty json`() {
        // setUp で prefs.getString("favorites", "[]") → "[]" を返すよう設定済み
        // JSONArray スタブにより length=0 → emptyList
        assertTrue(repository.favorites.value.isEmpty())
        verify(mockPrefs).getString(eq("favorites"), eq("[]"))
    }

    @Test
    fun `load falls back to empty list when prefs returns invalid json`() {
        val brokenPrefs  = mock<SharedPreferences>()
        val brokenEditor = mock<SharedPreferences.Editor>().also {
            whenever(it.putString(anyOrNull(), anyOrNull())).thenReturn(it)
        }
        whenever(brokenPrefs.getString(eq("favorites"), eq("[]"))).thenReturn("NOT_JSON")
        whenever(brokenPrefs.edit()).thenReturn(brokenEditor)

        val repo = FavoriteRepository(brokenPrefs)
        assertTrue(repo.favorites.value.isEmpty())
    }

    @Test
    fun `load falls back to empty list when prefs returns null`() {
        val nullPrefs  = mock<SharedPreferences>()
        val nullEditor = mock<SharedPreferences.Editor>().also {
            whenever(it.putString(anyOrNull(), anyOrNull())).thenReturn(it)
        }
        // getString が null を返す場合
        whenever(nullPrefs.getString(eq("favorites"), eq("[]"))).thenReturn(null)
        whenever(nullPrefs.edit()).thenReturn(nullEditor)

        val repo = FavoriteRepository(nullPrefs)
        assertTrue(repo.favorites.value.isEmpty())
    }
}
