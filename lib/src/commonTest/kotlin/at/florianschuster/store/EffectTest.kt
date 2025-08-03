package at.florianschuster.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

internal class EffectTest {

    @Test
    fun `effect factory function`() {
        val effects = mutableListOf<Effect<Int, Int>>()
        effects.add(effect {})
        effects.add(effect(1) {})

        assertEquals(2, effects.count())
        with(effects[0]) {
            assertIs<EffectExecution<Int, Int>>(this)
            assertNull(id)
        }
        with(effects[1]) {
            assertIs<EffectExecution<Int, Int>>(this)
            assertEquals(1, id)
        }
    }

    @Test
    fun `effect on Reducer Context`() {
        val effects = mutableListOf<Effect<Int, Int>>()
        val context = object : Reducer.Context<Int, Int> {
            override val environment: Int = 42
            override fun add(effect: Effect<Int, Int>) {
                effects.add(effect)
            }
        }

        context.effect {}
        context.effect(1) {}

        assertEquals(2, effects.count())
        with(effects[0]) {
            assertIs<EffectExecution<Int, Int>>(this)
            assertNull(id)
        }
        with(effects[1]) {
            assertIs<EffectExecution<Int, Int>>(this)
            assertEquals(1, id)
        }
    }

    @Test
    fun `cancelEffect on Reducer Context`() {
        val effects = mutableListOf<Effect<Int, Int>>()
        val context = object : Reducer.Context<Int, Int> {
            override val environment: Int = 42
            override fun add(effect: Effect<Int, Int>) {
                effects.add(effect)
            }
        }

        context.cancelEffect(id = 1)
        context.cancelEffects(ids = listOf(2, 3))

        assertEquals(2, effects.count())
        with(effects[0]) {
            assertIs<EffectCancellation<Int, Int>>(this)
            assertEquals(listOf(1), ids)
        }
        with(effects[1]) {
            assertIs<EffectCancellation<Int, Int>>(this)
            assertEquals(listOf(2, 3), ids)
        }
    }

    @Test
    fun `effect on ExpandState Context`() {
        val effects = mutableListOf<Effect<Int, Int>>()
        val context = object : DelegateStore.ExpandStateContext<Int, Int> {
            override val environment: Int = 42
            override fun add(effect: Effect<Int, Int>) {
                effects.add(effect)
            }
        }

        context.effect {}
        context.effect(1) {}

        assertEquals(2, effects.count())
        with(effects[0]) {
            assertIs<EffectExecution<Int, Int>>(this)
            assertNull(id)
        }
        with(effects[1]) {
            assertIs<EffectExecution<Int, Int>>(this)
            assertEquals(1, id)
        }
    }

    @Test
    fun `cancelEffect on ExpandState Context`() {
        val effects = mutableListOf<Effect<Int, Int>>()
        val context = object : DelegateStore.ExpandStateContext<Int, Int> {
            override val environment: Int = 42
            override fun add(effect: Effect<Int, Int>) {
                effects.add(effect)
            }
        }

        context.cancelEffect(id = 1)
        context.cancelEffects(ids = listOf(2, 3))

        assertEquals(2, effects.count())
        with(effects[0]) {
            assertIs<EffectCancellation<Int, Int>>(this)
            assertEquals(listOf(1), ids)
        }
        with(effects[1]) {
            assertIs<EffectCancellation<Int, Int>>(this)
            assertEquals(listOf(2, 3), ids)
        }
    }
}
