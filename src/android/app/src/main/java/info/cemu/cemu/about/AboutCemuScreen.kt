package info.cemu.cemu.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import info.cemu.cemu.BuildConfig
import info.cemu.cemu.R
import info.cemu.cemu.core.components.ScreenContent
import info.cemu.cemu.core.translation.tr

// TODO
@Composable
fun AboutCemuScreen(navigateBack: () -> Unit) {
    ScreenContent(
        appBarText = tr("About Cemu"),
        navigateBack = navigateBack,
        contentModifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        contentVerticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        AboutSection {
            Text(
                text = stringResource(R.string.app_name),
                fontSize = 32.sp,
            )
            Text(
                text = tr("Version: {0}", BuildConfig.VERSION_NAME),
                fontSize = 18.sp,
            )
            Text(
                text = tr(
                    "Original authors: {0}",
                    stringResource(R.string.cemu_original_authors)
                ),
                fontSize = 18.sp,
            )
            CemuWebsite()
        }
        AboutSection {
            Text(
                text = tr("Cemu is a Wii U emulator.\n\nWii and Wii U are trademarks of Nintendo.\nCemu is not affiliated with Nintendo."),
                fontSize = 18.sp
            )
        }
        AboutSection {
            Text(
                text = tr("Used libraries:"),
                fontSize = 18.sp,
            )
            UsedLibraries.forEach {
                Library(it)
            }
            Text(tr("Modified ih264 from Android project"))
        }
    }
}

@Composable
fun CemuWebsite() {
    Text(
        buildAnnotatedString {
            append(tr("Website:"))
            append(" ")
            withLink(
                LinkAnnotation.Url(
                    CEMU_WEBSITE,
                    TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                )
            ) {
                append(CEMU_WEBSITE)
            }
        }
    )
}

@Composable
fun AboutSection(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        SelectionContainer {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(16.dp),
                content = content,
            )
        }
    }
}

@Composable
fun Library(nameLinkPair: Pair<String, String>) {
    val (name, link) = nameLinkPair
    Text(
        buildAnnotatedString {
            append(name)
            append(" (")
            withLink(
                LinkAnnotation.Url(
                    link,
                    TextLinkStyles(
                        style = SpanStyle(
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textDecoration = TextDecoration.Underline,
                        ),
                    )
                )
            ) {
                append(link)
            }
            append(")")
        }
    )
}

private val UsedLibraries: List<Pair<String, String>> = listOf(
    "zlib" to "https://www.zlib.net",
    "OpenSSL" to "https://www.openssl.org",
    "libcurl" to "https://curl.haxx.se/libcurl",
    "imgui" to "https://github.com/ocornut/imgui",
    "fontawesome" to "https://github.com/FortAwesome/Font-Awesome",
    "boost" to "https://www.boost.org",
    "libusb" to "https://libusb.info",
)

private const val CEMU_WEBSITE = "https://cemu.info"
