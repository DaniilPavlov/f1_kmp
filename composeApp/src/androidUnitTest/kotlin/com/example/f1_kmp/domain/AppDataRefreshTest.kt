package com.example.f1_kmp.domain

import com.example.f1_kmp.data.local.CacheDao
import com.example.f1_kmp.data.repository.IEspnRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class AppDataRefreshTest {
    private lateinit var espn: IEspnRepository
    private lateinit var cacheDao: CacheDao
    private lateinit var refresh: AppDataRefresh

    @Before
    fun setUp() {
        espn = mockk(relaxed = true)
        cacheDao = mockk(relaxed = true)
        refresh = AppDataRefresh(espn, cacheDao)
    }

    @Test
    fun clearAll_clearsEspnThenFileCache() = runTest {
        coEvery { espn.clearCaches() } returns Unit
        coEvery { cacheDao.clearAll() } returns Unit

        refresh.clearAll()

        coVerify(exactly = 1) { espn.clearCaches() }
        coVerify(exactly = 1) { cacheDao.clearAll() }
    }
}
