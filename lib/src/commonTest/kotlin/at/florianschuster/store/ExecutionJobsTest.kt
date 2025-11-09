package at.florianschuster.store

import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionJobsTest {

    @Test
    fun `ExecutionJobs can be added and cancelled`() = runTest {
        val sut = EffectHandler.ExecutionJobList(backgroundScope)

        assertTrue(sut.items.isEmpty())

        sut.cancel(listOf(1))
        assertTrue(sut.items.isEmpty())

        val itemId = 1
        val item = EffectHandler.ExecutionJobList.JobItem(effectId = 1, job = Job())
        sut.add(item)
        assertEquals(item, sut.items.single())
        assertTrue(item.job.isActive)
        assertTrue(sut.isActive(itemId))

        sut.cancel(listOf(itemId))
        assertTrue(sut.items.isEmpty())
        assertFalse(item.job.isActive)
        assertFalse(sut.isActive(itemId))
    }
}
