package com.devcampus.create_meme.ui.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.toSize
import com.devcampus.create_meme.R
import com.devcampus.create_meme.ui.common.MemeFontFamily
import com.devcampus.create_meme.ui.model.UiDecorType
import com.devcampus.create_meme.ui.model.UiDecor
import com.devcampus.create_meme.ui.model.textDecor
import java.util.UUID

/**
 * Holds UI state for decorations.
 * Keeps reference to list of all decorations.
 * Holds selected item and handles selected item properties change.
 *
 * @param decorItems decorations list
 * @param textMeasurer measurer for sizing and placing text
 * @param defaultMemeText default text for new text decoration
 * @property [EditorProperties] size and color values
 * @param onDecorAdded called when new decoration item was added
 * @param onDeleteClick called when decor delete button is clicked
 * @param onDecorMoved called when decor position is changed
 * @param onDecorUpdated called when decor properties updated
 */
class MemeEditorState(
    val decorItems: List<UiDecor>,
    val textMeasurer: TextMeasurer,
    val defaultMemeText: String,
    val properties: EditorProperties,
    val onDecorAdded: (UiDecor) -> Unit,
    val onDeleteClick: (String) -> Unit,
    val onDecorMoved: (String, Offset) -> Unit,
    val onDecorUpdated: (UiDecor) -> Unit,
) {

    /**
     * Item selected by user. All decoration property changes (font, size, color) are done for this item and can be cancelled
     * or confirmed by the user.
     */
    var selectedItem by mutableStateOf<UiDecor?>(null)

    var isInTextEditMode by mutableStateOf<Boolean>(false)

    var canvasSize: Size = Size.Unspecified

    /**
     * Add new text decor
     * Measure size and set position for new decor before adding.
     */
    fun addTextDecor() {
        val size = textMeasurer.measure(
            text = defaultMemeText,
            style = TextStyle.Default.copy(
                fontFamily = UiDecorType.TextUiDecor.DefaultFontFamily.fontFamily,
                fontSize = UiDecorType.TextUiDecor.DefaultFontFamily.baseFontSize,
            )).size

        val decor = UiDecor(
            id = UUID.randomUUID().toString(),
            type = UiDecorType.TextUiDecor(defaultMemeText),
            topLeft = Offset(
                x = canvasSize.center.x - size.center.x,
                y = canvasSize.center.y - size.center.y,
            ),
            size = size.toSize(),
        )

        selectedItem = decor

        onDecorAdded(decor)
    }

    /**
     * Confirm all changes for [selectedItem]
     */
    fun confirmChanges(clearSelection: Boolean = true) {
        withSelection { selectedItem ->

            decorItems.find { it.id == selectedItem.id }?.let { currentItem ->
                if (currentItem.type != selectedItem.type) {
                    onDecorUpdated(selectedItem)
                }
            }

            if (clearSelection) {
                this@MemeEditorState.selectedItem = null
                isInTextEditMode = false
            }
        }
    }

    fun cancelChanges() {
        selectedItem = null
    }

    /**
     * Set new font.
     * Update decor size and position for new font selection
     */
    fun setFont(font: MemeFontFamily) {
        withTextSelection { decor, textDecor ->

            val oldSize = decor.size
            val newSize = decor.measure(withFont = font)

            val topLeft = Offset(
                x = decor.topLeft.x - (newSize.width - oldSize.width) / 2f,
                y = decor.topLeft.y - (newSize.height - oldSize.height) / 2f,
            )

            selectedItem = decor.copy(
                type = textDecor.copy(fontFamily = font),
                topLeft = topLeft,
                size = newSize,
            )
        }
    }

    /**
     * Adjust font scale.
     * @param scale required scale.
     * - If decoration size/position exceed screen borders the scale update is declined
     */
    fun setFontScale(scale: Float) {
        withTextSelection { decor, textDecor ->

            val oldSize = decor.size
            val newSize = decor.measure(withFontScale = scale)

            val topLeft = Offset(
                x = decor.topLeft.x - (newSize.width - oldSize.width) / 2f,
                y = decor.topLeft.y - (newSize.height - oldSize.height) / 2f,
            )

            if (topLeft.x < properties.borderMargin || topLeft.y < properties.borderMargin) return
            if (topLeft.x + newSize.width > canvasSize.width - properties.borderMargin) return

            selectedItem = decor.copy(
                type = textDecor.copy(fontScale = scale),
                topLeft = topLeft,
                size = newSize,
            )
        }
    }

    fun setFontColor(color: Color) {
        withTextSelection { decor, textDecor ->
            selectedItem = decor.copy(
                type = textDecor.copy(fontColor = color),
            )
        }
    }

    /**
     * Request text update for the selected decoration.
     * @return
     * - true if update is allowed
     * - false if update is forbidden (due to size / position restrictions)
     */
    fun onTextChange(newText: String): Boolean {

        if (isInTextEditMode.not()) return false

        withTextSelection { decor, textDecor ->

            val oldSize = decor.size
            val newSize = decor.measure(withText = newText)

            val topLeft = Offset(
                x = decor.topLeft.x - (newSize.width - oldSize.width) / 2f,
                y = decor.topLeft.y - (newSize.height - oldSize.height) / 2f,
            )

            if (topLeft.x < properties.borderMargin || topLeft.y < properties.borderMargin) return false
            if (topLeft.x + newSize.width > canvasSize.width - properties.borderMargin) return false

            selectedItem = decor.copy(
                type = textDecor.copy(text = newText),
                topLeft = topLeft,
                size = newSize,
            )

            return true
        }

        return false
    }

    /**
     * Finish text edit mode.
     * Save changes and clear [isInTextEditMode] flag
     */
    fun finishEditMode() {
        withTextSelection { decor, textDecor ->

            // delete decoration if user accepts
            if (textDecor.text.isEmpty()) {
                onDeleteClick(decor.id)
                selectedItem = null
                isInTextEditMode = false
                return
            }

            confirmChanges(clearSelection = false)
            isInTextEditMode = false
        }
    }

    private fun UiDecor.measure(
        withText: String? = null,
        withFont: MemeFontFamily? = null,
        withFontScale: Float? = null,
    ): Size {

        val textDecor = textDecor() ?: error("Can only measure text")

        return textMeasurer.measure(
            text = withText ?: textDecor.text,
            style = TextStyle.Default.copy(
                fontFamily = withFont?.fontFamily ?: textDecor.fontFamily.fontFamily,
                fontSize = (withFont?.baseFontSize ?: textDecor.fontFamily.baseFontSize) * (withFontScale ?: textDecor.fontScale),
            )).size.toSize()
    }

    private inline fun withTextSelection(block: (UiDecor, UiDecorType.TextUiDecor) -> Unit) {
        selectedItem?.let { decor ->
            decor.textDecor()?.let { textDecor->
                block(decor, textDecor)
            }
        }
    }

    private inline fun withSelection(block: (UiDecor) -> Unit) { selectedItem?.let { block(it) } }
}

@Composable
fun rememberMemeEditorState(
    decorItems: List<UiDecor>,
    properties: EditorProperties,
    onDecorAdded: (UiDecor) -> Unit,
    onDeleteClick: (String) -> Unit,
    onDecorMoved: (String, Offset) -> Unit,
    onDecorUpdated: (UiDecor) -> Unit,
): MemeEditorState {

    val textMeasurer = rememberTextMeasurer()
    val defaultMemeText = stringResource(R.string.tap_twice_to_edit)

    return remember {
        MemeEditorState(
            decorItems = decorItems,
            textMeasurer = textMeasurer,
            defaultMemeText = defaultMemeText,
            properties = properties,
            onDecorAdded = onDecorAdded,
            onDeleteClick = onDeleteClick,
            onDecorMoved = onDecorMoved,
            onDecorUpdated = onDecorUpdated,
        )
    }
}
