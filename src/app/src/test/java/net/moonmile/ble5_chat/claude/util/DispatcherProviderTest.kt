package net.moonmile.ble5_chat.claude.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DispatcherProviderTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var provider: DefaultDispatcherProvider

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        provider = DefaultDispatcherProvider()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ── io() は Dispatchers.IO を返す ─────────────────────────────
    @Test
    fun `io returns Dispatchers IO`() {
        assertEquals(Dispatchers.IO, provider.io())
    }

    // ── default() は Dispatchers.Default を返す ───────────────────
    @Test
    fun `default returns Dispatchers Default`() {
        assertEquals(Dispatchers.Default, provider.default())
    }

    // ── main() は Dispatchers.Main を返す（setMain で差し替え済み）
    @Test
    fun `main returns Dispatchers Main`() {
        assertNotNull(provider.main())
        assertEquals(Dispatchers.Main, provider.main())
    }

    // ── DispatcherProvider インターフェースを実装している ─────────
    @Test
    fun `DefaultDispatcherProvider implements DispatcherProvider`() {
        val p: DispatcherProvider = DefaultDispatcherProvider()
        assertNotNull(p.io())
        assertNotNull(p.default())
        assertNotNull(p.main())
    }

    // ── 各ディスパッチャは互いに異なるインスタンス ─────────────────
    @Test
    fun `io and default are different dispatchers`() {
        assert(provider.io() !== provider.default())
    }
}
