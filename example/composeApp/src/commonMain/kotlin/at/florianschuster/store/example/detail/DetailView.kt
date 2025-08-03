package at.florianschuster.store.example.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import at.florianschuster.store.example.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Immutable
internal data class DetailState(
    val id: String,
)

@Composable
internal fun DetailView(
    state: DetailState,
    paddingValues: PaddingValues = PaddingValues(),
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppTheme.dimensions.medium),
            text = "This is just a mock detail view for Item '${state.id}'.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@Preview
private fun Preview() {
    AppTheme {
        DetailView(state = DetailState("id"))
    }
}

