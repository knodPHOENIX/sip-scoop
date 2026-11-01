package com.sipscoop.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.Entry
import com.sipscoop.app.data.Store

private data class Draft(val id: Long, val time: String, val text: String)

/** Lets you fix or delete any of today's entries, or type the correct total. */
@Composable
fun EditReadingDialog(
    isWater: Boolean,
    entries: List<Entry>,
    onDismiss: () -> Unit,
    onSave: (List<Entry>) -> Unit
) {
    val accent = if (isWater) WaterAccent else ProteinAccent
    val unit = if (isWater) "ml" else "g"
    val tooBig = if (isWater) 2000 else 100
    val drafts = remember { mutableStateListOf<Draft>().apply { addAll(entries.map { Draft(it.id, it.time, it.amount.toString()) }) } }
    var totalText by remember { mutableStateOf("") }
    val draftSum = drafts.sumOf { it.text.toIntOrNull() ?: 0 }

    fun save() {
        val total = totalText.toIntOrNull()
        if (total != null && total != draftSum) {
            onSave(if (total > 0) listOf(Store.newEntry(total)) else emptyList())
        } else {
            onSave(drafts.mapNotNull { d -> d.text.toIntOrNull()?.takeIf { it > 0 }?.let { Entry(d.id, d.time, it) } })
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = C.Surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column {
                Text(if (isWater) "Edit today's water" else "Edit today's protein", style = T.display(22))
                Text("Fix a wrong entry or set the total yourself.", style = T.body(14, C.Muted))
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(accent.card, RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Today's total", style = T.bold(14, accent.text), modifier = Modifier.weight(1f))
                    Text("${fmt(draftSum)} $unit", style = T.display(22, accent.text))
                }

                if (drafts.isEmpty()) {
                    Text("Nothing logged yet today.", style = T.body(14, C.Muted))
                }

                drafts.forEachIndexed { i, d ->
                    val amount = d.text.toIntOrNull() ?: 0
                    val suspicious = amount >= tooBig && amount % 10 == 0
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(if (suspicious) C.RedSoft else Color.Transparent, RoundedCornerShape(14.dp))
                            .padding(if (suspicious) 6.dp else 0.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(d.time, style = T.bold(14, if (suspicious) C.Red else C.Muted), modifier = Modifier.width(48.dp))
                            Box(
                                Modifier
                                    .weight(1f)
                                    .background(C.Surface, RoundedCornerShape(12.dp))
                                    .border(2.dp, if (suspicious) C.Red else C.Line, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 9.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    BasicTextField(
                                        value = d.text,
                                        onValueChange = { v -> drafts[i] = d.copy(text = v.filter { it.isDigit() }.take(5)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        textStyle = T.display(18),
                                        cursorBrush = SolidColor(accent.face),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(unit, style = T.bold(13, C.Muted))
                                }
                            }
                            SquishButton(
                                onClick = { drafts.removeAt(i) },
                                background = C.Surface,
                                shape = RoundedCornerShape(12.dp),
                                borderColor = C.RedLine,
                                contentPadding = PaddingValues(9.dp)
                            ) { LineIcon(Ic.TRASH, C.Red, 18.dp, 2f) }
                        }
                        if (suspicious) {
                            Row(
                                Modifier.fillMaxWidth().padding(start = 58.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("That looks high. Did you mean ${amount / 10}?", style = T.bold(13, C.Red), modifier = Modifier.weight(1f))
                                Btn3D(
                                    onClick = { drafts[i] = d.copy(text = (amount / 10).toString()) },
                                    face = accent.face, edge = accent.edge, fill = false, radius = 10.dp, depth = 4.dp,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) { Text("Use ${amount / 10}", style = T.bold(13, Color.White)) }
                            }
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth().padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Or set today's total", style = T.bold(14, C.Muted), modifier = Modifier.weight(1f))
                    Box(
                        Modifier
                            .width(120.dp)
                            .background(C.Surface, RoundedCornerShape(12.dp))
                            .border(2.dp, C.Line, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                if (totalText.isEmpty()) Text(fmt(draftSum), style = T.display(18, C.Muted.copy(alpha = 0.6f)))
                                BasicTextField(
                                    value = totalText,
                                    onValueChange = { v -> totalText = v.filter { it.isDigit() }.take(5) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = T.display(18),
                                    cursorBrush = SolidColor(accent.face),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Text(unit, style = T.bold(13, C.Muted))
                        }
                    }
                }
                if (totalText.isNotEmpty()) {
                    Text("Today's entries will be replaced by this one total.", style = T.body(13, C.Muted))
                }
            }
        },
        confirmButton = {
            Btn3D(
                onClick = { save() },
                face = accent.face, edge = accent.edge, fill = false, depth = 4.dp,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
            ) { Text("Save changes", style = T.bold(15, Color.White)) }
        },
        dismissButton = {
            Btn3D(
                onClick = onDismiss, face = C.Surface, edge = C.Line, fill = false, depth = 4.dp,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) { Text("Cancel", style = T.bold(15, C.Muted)) }
        }
    )
}
