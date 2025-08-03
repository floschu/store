package at.florianschuster.store

import kotlin.test.Test
import kotlin.test.assertEquals

internal class ReducerTest {

    @Test
    fun `Reducer factory function`() {
        val context = object : Reducer.Context<Int, Int> {
            override val environment: Int = 42
            override fun add(effect: Effect<Int, Int>) = error("not implemented")
        }
        val reducer: Reducer<Int, Int, Int> = Reducer { previousState, action ->
            previousState + 420
        }
        with(context) {
            with(reducer) {
                assertEquals(420, reduce(0, 2))
            }
        }
    }

    @Test
    fun `EmptyReducer factory function`() {
        val context = object : Reducer.Context<Int, Int> {
            override val environment: Int = 42
            override fun add(effect: Effect<Int, Int>) = error("not implemented")
        }
        val reducer: Reducer<Int, Int, Int> = EmptyReducer()
        with(context) {
            with(reducer) {
                assertEquals(1, reduce(1, 2))
            }
        }
    }
}
