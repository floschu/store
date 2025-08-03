package at.florianschuster.store.example

internal object Log {
    fun e(throwable: Throwable) {
        println("Error: $throwable")
    }
}