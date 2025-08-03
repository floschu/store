package at.florianschuster.store.example.service

internal interface InputValidator {
    fun validateEmail(email: String): Boolean
    fun validatePassword(password: String): Boolean
}

internal class MockInputValidator : InputValidator {
    override fun validateEmail(email: String): Boolean = emailRegex.matches(email)
    override fun validatePassword(password: String): Boolean = password.isNotEmpty()

    companion object Companion {
        private val emailRegex = Regex(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
        )
    }
}