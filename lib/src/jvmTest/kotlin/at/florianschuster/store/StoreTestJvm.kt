package at.florianschuster.store

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class StoreTestJvm {

    @Test
    fun `effect throws`() {
        assertFailsWith<IllegalStateException> {
            runTest {
                val sut: Store<Unit, Int, Int> = Store(
                    initialState = 0,
                    effectScope = backgroundScope,
                    environment = Unit,
                    reducer = Reducer { previousState, _ ->
                        effect { error("error") }
                        previousState
                    },
                )
                sut.dispatch(1)
                runCurrent()
            }
        }
    }

    @Test
    fun `initialEffect throws`() {
        assertFailsWith<IllegalStateException> {
            runTest {
                Store<Unit, Int, Int>(
                    initialState = 0,
                    effectScope = backgroundScope,
                    environment = Unit,
                    reducer = Reducer(
                        initialEffect = effect { error("error") },
                    ) { previousState, _ -> previousState },
                )
                runCurrent()
            }
        }
    }
}
