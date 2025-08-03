package at.florianschuster.store

import kotlinx.coroutines.Job
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ExecutionJobsTest {

    @Test
    fun `ExecutionJobs can be added and cancelled`() = runTest {
        val events = mutableListOf<StoreEvent>()
        val sut = EffectHandler.ExecutionJobList(events = events::add)

        assertTrue(sut.items.isEmpty())

        sut.cancel(listOf(1))
        assertTrue(sut.items.isEmpty())

        val itemId = 1
        val item = EffectHandler.ExecutionJobList.Item(effectId = itemId, job = Job())
        sut.add(item)
        assertTrue(itemId in sut)
        assertEquals(item, sut.items.single())
        assertTrue(item.job.isActive)

        sut.cancel(listOf(itemId))
        assertTrue(itemId !in sut)
        assertTrue(sut.items.isEmpty())
        assertFalse(item.job.isActive)
        with(events.single()) {
            assertIs<StoreEvent.Effect.Cancel>(this)
            assertEquals(itemId, this.effectId)
        }
    }
}
