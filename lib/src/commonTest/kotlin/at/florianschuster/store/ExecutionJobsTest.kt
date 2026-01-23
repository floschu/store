package at.florianschuster.store

import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExecutionJobsTest {

    @Test
    fun `ExecutionJobs can be added via launchIfNotActive and cancelled`() = runTest {
        val sut = EffectHandler.ExecutionJobList(backgroundScope)

        assertTrue(sut.items.isEmpty())

        // Cancel on empty list should be safe
        sut.cancel(listOf(1))
        assertTrue(sut.items.isEmpty())

        val itemId = 1
        var createdJob: CompletableJob? = null
        
        // Launch a new job via launchIfNotActive
        val job = sut.launchIfNotActive(itemId) {
            Job().also { createdJob = it }
        }
        
        assertNotNull(job)
        assertEquals<Job?>(createdJob, job)
        assertEquals(1, sut.items.size)
        assertTrue(job.isActive)

        // Trying to launch again with same ID should return null (already active)
        val duplicateJob = sut.launchIfNotActive(itemId) {
            Job() // This should not be called
        }
        assertNull(duplicateJob)
        assertEquals(1, sut.items.size) // Still only 1 item

        // Cancel should remove and cancel the job
        sut.cancel(listOf(itemId))
        assertTrue(sut.items.isEmpty())
        assertFalse(job.isActive)

        // After cancellation, a new job can be launched with same ID
        val newJob = sut.launchIfNotActive(itemId) {
            Job()
        }
        assertNotNull(newJob)
        assertEquals(1, sut.items.size)
    }

    @Test
    fun `ExecutionJobs allows launching with different IDs`() = runTest {
        val sut = EffectHandler.ExecutionJobList(backgroundScope)

        val job1 = sut.launchIfNotActive(1) { Job() }
        val job2 = sut.launchIfNotActive(2) { Job() }
        val job3 = sut.launchIfNotActive(3) { Job() }

        assertNotNull(job1)
        assertNotNull(job2)
        assertNotNull(job3)
        assertEquals(3, sut.items.size)

        // Cancel just one
        sut.cancel(listOf(2))
        assertEquals(2, sut.items.size)
        assertTrue(job1.isActive)
        assertFalse(job2.isActive)
        assertTrue(job3.isActive)

        // Cancel remaining
        sut.cancel(listOf(1, 3))
        assertTrue(sut.items.isEmpty())
    }

    @Test
    fun `concurrent launchIfNotActive with same ID only creates one job`() = runTest {
        val sut = EffectHandler.ExecutionJobList(backgroundScope)
        val results = mutableListOf<Job?>()
        val resultsMutex = Mutex()
        val effectId = "concurrent-test-id"
        
        // Launch multiple concurrent attempts with the same ID
        // This tests that the mutex properly serializes access and prevents duplicates
        repeat(50) {
            launch {
                val job = sut.launchIfNotActive(effectId) { Job() }
                resultsMutex.withLock {
                    results.add(job)
                }
            }
        }
        
        advanceUntilIdle()
        
        // Only one should have succeeded (non-null), rest should be null
        val successfulLaunches = results.filterNotNull()
        assertEquals(1, successfulLaunches.size, "Expected exactly one successful launch, got ${successfulLaunches.size}")
        assertEquals(1, sut.items.size, "Expected exactly one item tracked")
        
        // The successful job should be active
        assertTrue(successfulLaunches.first().isActive)
    }

    @Test
    fun `concurrent launchIfNotActive with different IDs all succeed`() = runTest {
        val sut = EffectHandler.ExecutionJobList(backgroundScope)
        val results = mutableListOf<Job?>()
        val resultsMutex = Mutex()
        
        // Launch concurrent attempts with different IDs
        repeat(20) { id ->
            launch {
                val job = sut.launchIfNotActive("id-$id") { Job() }
                resultsMutex.withLock {
                    results.add(job)
                }
            }
        }
        
        advanceUntilIdle()
        
        // All should succeed since they have different IDs
        val successfulLaunches = results.filterNotNull()
        assertEquals(20, successfulLaunches.size, "All launches should succeed with unique IDs")
        assertEquals(20, sut.items.size, "All items should be tracked")
    }
}
