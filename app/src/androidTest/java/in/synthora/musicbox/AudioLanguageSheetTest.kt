package `in`.synthora.musicbox

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import `in`.synthora.musicbox.models.Track
import `in`.synthora.musicbox.network.AlexaBackendApi
import `in`.synthora.musicbox.ui.screens.AudioLanguageSheet
import `in`.synthora.musicbox.ui.screens.PlayerHeader
import `in`.synthora.musicbox.ui.theme.JUKETheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AudioLanguageSheetTest {
    @get:Rule val compose = createComposeRule()
    @Test fun menuAlwaysOffersTimerButHidesUnavailableAudioLanguage() {
        compose.setContent { JUKETheme {
            PlayerHeader({}, {}, {}, {}, {}, false, true, false)
        } }
        compose.onNodeWithContentDescription("Song options").performClick()
        compose.onNodeWithText("Sleep timer").assertIsDisplayed()
        compose.onNodeWithText("Audio language").assertDoesNotExist()
    }
    @Test fun confirmedAlternatesEnableLanguageMenu() {
        compose.setContent { JUKETheme {
            PlayerHeader({}, {}, {}, {}, {}, false, true, false, audioLanguageAvailable = true)
        } }
        compose.onNodeWithContentDescription("Song options").performClick()
        compose.onNodeWithText("Audio language").assertIsDisplayed()
    }
    @Test fun sheetSelectsOneLanguageAndIdentifiesOriginalAudio() {
        var selected = ""
        val original = AlexaBackendApi.AudioLanguage("en", "English", original = true)
        val alternate = AlexaBackendApi.AudioLanguage("hi", "Hindi", autoDubbed = true)
        compose.setContent { JUKETheme {
            AudioLanguageSheet(Track("song", "Episode", "Artist", durationSec=180),
                AlexaBackendApi.AudioLanguages(listOf(original, alternate), "en"), {}, { selected = it.id })
        } }
        compose.onNodeWithText("Original audio").assertIsDisplayed()
        compose.onNodeWithText("AI generated").assertIsDisplayed()
        compose.onNodeWithText("Hindi").performClick()
        assertEquals("hi", selected)
    }
}
