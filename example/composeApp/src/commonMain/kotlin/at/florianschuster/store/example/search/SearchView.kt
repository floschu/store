package at.florianschuster.store.example.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Abc
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import at.florianschuster.store.example.AppTheme
import at.florianschuster.store.example.service.MockSearchRepository
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
internal fun SearchView(
    state: SearchState,
    dispatch: (SearchAction) -> Unit = {},
    onGoToDetail: (id: String) -> Unit = {},
    paddingValues: PaddingValues = PaddingValues(),
) {
    val layoutDirection = LocalLayoutDirection.current
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateStartPadding(layoutDirection),
                    end = paddingValues.calculateEndPadding(layoutDirection),
                )
                .padding(horizontal = AppTheme.dimensions.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.small),
        ) {
            OutlinedTextField(
                modifier = Modifier.weight(1f),
                value = state.query,
                onValueChange = { dispatch(SearchAction.QueryChanged(it)) },
                label = { Text("Search") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (state.loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                        if (state.query.isNotEmpty()) {
                            IconButton(
                                onClick = { dispatch(SearchAction.ResetQuery) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear"
                                )
                            }
                        }
                    }
                }
            )
            IconButton(
                onClick = { dispatch(SearchAction.Logout) }
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Logout,
                    contentDescription = "logout",
                )
            }
        }
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
                bottom = paddingValues.calculateBottomPadding()
            ),
        ) {
            item {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.large))
            }
            item(key = "id_empty") {
                if (state.items.isEmpty()) {
                    Text(
                        modifier = Modifier.fillMaxSize(),
                        text = if (state.query.isEmpty()) "Please enter a Query" else "No items found.",
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(state.items, key = { "id_$it" }) { item ->
                ListItem(
                    modifier = Modifier.clickable { onGoToDetail(item) },
                    leadingContent = { Icon(Icons.Default.Abc, null) },
                    headlineContent = { Text(text = item) },
                    supportingContent = { Text(text = "This is a description for $item") }
                )
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    AppTheme {
        SearchView(state = SearchState())
    }
}

@Composable
@Preview
private fun PreviewError() {
    AppTheme {
        SearchView(
            state = SearchState(
                query = "query",
            )
        )
    }
}

@Composable
@Preview
private fun PreviewItems() {
    AppTheme {
        SearchView(
            state = SearchState(
                query = "Item",
                items = MockSearchRepository.DefaultItems
            )
        )
    }
}
