package com.sipscoop.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sipscoop.app.data.AppState
import com.sipscoop.app.data.Food
import com.sipscoop.app.data.FoodDb
import com.sipscoop.app.data.FoodItem
import com.sipscoop.app.data.OpenFoodFacts
import com.sipscoop.app.data.addProtein
import com.sipscoop.app.data.proteinRoom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.roundToInt

private sealed interface Online {
    data object Idle : Online
    data object Loading : Online
    data class Done(val items: List<FoodItem>) : Online
    data class Failed(val message: String) : Online
}

private fun fmt1(v: Double): String {
    val r = (v * 10).roundToInt() / 10.0
    return if (r % 1.0 == 0.0) r.toInt().toString() else String.format(Locale.US, "%.1f", r)
}

private fun baseName(name: String) = name.substringBefore(",").trim()

/** The protein screen's "Search food" bar. */
@Composable
fun FoodSearchBar(onClick: () -> Unit) {
    Btn3D(
        onClick = onClick,
        face = C.Surface, edge = C.OrangeLine, border = C.OrangeLine,
        depth = 5.dp, horizontalAlignment = Alignment.Start,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 13.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LineIcon(Ic.SEARCH, C.OrangeText, 20.dp, 2.6f)
            Text("Search food, e.g. paneer", style = T.bold(16, C.Muted), modifier = Modifier.weight(1f))
            Text(
                "NEW", style = T.bold(11, C.OrangeText),
                modifier = Modifier
                    .background(C.OrangeSoft, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FoodSearchScreen(
    state: AppState,
    update: ((AppState) -> AppState) -> Unit,
    onBack: () -> Unit,
    onAdded: (String) -> Unit
) {
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var online by remember { mutableStateOf<Online>(Online.Idle) }
    var selected by remember { mutableStateOf<FoodItem?>(null) }
    var sheetFood by remember { mutableStateOf<FoodItem?>(null) }
    var focused by remember { mutableStateOf(false) }
    val requester = remember { FocusRequester() }

    val results = remember(query) { FoodDb.search(context, query) }
    val recents = remember(state.recentFoods) { state.recentFoods.mapNotNull { FoodDb.find(context, it) }.take(4) }

    LaunchedEffect(query) { online = Online.Idle }
    LaunchedEffect(Unit) {
        delay(350)
        runCatching { requester.requestFocus() }
    }
    BackHandler(enabled = selected != null) { selected = null }

    fun open(f: FoodItem) {
        focus.clearFocus()
        sheetFood = f
        selected = f
    }

    fun searchOnline() {
        val q = query.trim()
        if (q.length < 2) {
            online = Online.Failed("Type at least 2 letters to search packaged products.")
            return
        }
        focus.clearFocus()
        online = Online.Loading
        scope.launch {
            val r = withContext(Dispatchers.IO) { runCatching { OpenFoodFacts.search(q) } }
            online = r.fold(
                onSuccess = { Online.Done(it) },
                onFailure = { e ->
                    if (e is OpenFoodFacts.TooSoon) Online.Failed("Please wait ${e.waitSeconds} seconds before searching online again.")
                    else Online.Failed("Couldn't reach the online search. Check your internet and try again.")
                }
            )
        }
    }

    val border by animateColorAsState(if (focused) C.Orange else C.Line, tween(200), label = "searchBorder")

    Box(Modifier.fillMaxSize().background(C.Bg)) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SquishButton(onClick = onBack, background = C.Faint, shape = RoundedCornerShape(14.dp), contentPadding = PaddingValues(11.dp)) {
                        LineIcon(Ic.BACK, C.Muted, 22.dp, 3f)
                    }
                    Row(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .background(C.Surface, RoundedCornerShape(14.dp))
                            .border(2.dp, border, RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LineIcon(Ic.SEARCH, C.OrangeText, 18.dp, 2.6f)
                        Box(Modifier.weight(1f)) {
                            if (query.isEmpty()) Text("Search food", style = T.bold(17, C.Muted.copy(alpha = 0.7f)))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it.take(40) },
                                singleLine = true,
                                textStyle = T.bold(17),
                                cursorBrush = SolidColor(C.Orange),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = {
                                    focus.clearFocus()
                                    if (results.isEmpty()) searchOnline()
                                }),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .focusRequester(requester)
                                    .onFocusChanged { focused = it.isFocused }
                            )
                        }
                        AnimatedVisibility(visible = query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
                            SquishButton(onClick = { query = "" }, background = C.Line, contentPadding = PaddingValues(5.dp)) {
                                LineIcon(Ic.CLOSE, C.Ink, 12.dp, 3f)
                            }
                        }
                    }
                }
                AnimatedVisibility(visible = query.isEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (recents.isNotEmpty()) {
                            recents.forEach { f ->
                                Chip(baseName(f.name), recent = true) { open(f) }
                            }
                        } else {
                            listOf("Paneer", "Tofu", "Egg", "Dal").forEach { c -> Chip(c, recent = false) { query = c } }
                        }
                    }
                }
            }

            LazyColumn(
                Modifier.weight(1f),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "title") {
                    SectionLabel(if (query.isBlank()) "Popular foods" else "Built-in foods", Modifier.animateItem())
                }
                items(results, key = { "b:" + it.name }) { f ->
                    FoodRow(f, Modifier.animateItem()) { open(f) }
                }
                if (query.isNotBlank() && results.isEmpty()) {
                    item(key = "none") {
                        Text(
                            "\"${query.trim()}\" isn't in the built-in list. Try searching packaged products online.",
                            style = T.body(14, C.Muted),
                            modifier = Modifier.animateItem().padding(vertical = 6.dp)
                        )
                    }
                }
                item(key = "online") {
                    Box(Modifier.animateItem().padding(top = 6.dp)) {
                        AnimatedContent(
                            targetState = online,
                            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                            contentKey = { it::class },
                            label = "online"
                        ) { o ->
                            when (o) {
                                Online.Idle -> Btn3D(
                                    onClick = { searchOnline() },
                                    face = C.OrangeCard, edge = C.OrangeLine, border = C.OrangeLine,
                                    depth = 5.dp, contentPadding = PaddingValues(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LineIcon(Ic.GLOBE, C.OrangeText, 18.dp, 2f)
                                        Text(
                                            if (query.isBlank()) "Search packaged products online"
                                            else "Search packaged products for \"${query.trim()}\"",
                                            style = T.bold(15, C.OrangeText),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Online.Loading -> Row(
                                    Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(Modifier.size(20.dp), color = C.Orange, strokeWidth = 3.dp)
                                    Spacer(Modifier.size(10.dp))
                                    Text("Searching packaged products…", style = T.bold(14, C.Muted))
                                }
                                is Online.Failed -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    NoteBox(o.message, warning = true)
                                    SquishButton(onClick = { searchOnline() }, background = C.OrangeSoft, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)) {
                                        Text("Try again", style = T.bold(14, C.OrangeText))
                                    }
                                }
                                is Online.Done -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    SectionLabel("Packaged products")
                                    if (o.items.isEmpty()) {
                                        Text("No packaged products found with protein info.", style = T.body(14, C.Muted))
                                    }
                                    o.items.forEachIndexed { i, f ->
                                        StaggerIn(i) { FoodRow(f) { open(f) } }
                                    }
                                    Text("Product data from Open Food Facts (ODbL).", style = T.body(12, C.Muted))
                                }
                            }
                        }
                    }
                }
                item(key = "note") {
                    Text(
                        "Values are estimates. Home-cooked dishes vary by recipe.",
                        style = T.body(12, C.Muted),
                        modifier = Modifier.animateItem().padding(top = 4.dp)
                    )
                }
            }
        }

        // Details sheet
        AnimatedVisibility(visible = selected != null, enter = fadeIn(tween(220)), exit = fadeOut(tween(220))) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (ThemeState.dark) 0.6f else 0.45f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { selected = null }
            )
        }
        AnimatedVisibility(
            visible = selected != null,
            enter = slideInVertically(spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)) { it } + fadeIn(),
            exit = slideOutVertically(tween(260, easing = FastOutSlowInEasing)) { it } + fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            sheetFood?.let { f ->
                key(f.name + f.brand) {
                    FoodSheet(
                        food = f,
                        state = state,
                        onClose = { selected = null },
                        onStar = { label, grams ->
                            update { s ->
                                if (s.foods.any { it.name == label }) s.copy(foods = s.foods.filter { it.name != label })
                                else s.copy(foods = s.foods + Food(label, grams))
                            }
                        },
                        onAdd = { grams ->
                            val room = state.proteinRoom
                            if (room <= 0) {
                                selected = null
                                onAdded("You've reached today's limit of 400 g protein.")
                                return@FoodSheet
                            }
                            val actual = minOf(grams, room)
                            update { s ->
                                val recent = if (f.online) s.recentFoods
                                else (listOf(f.name) + s.recentFoods.filter { it != f.name }).take(8)
                                s.addProtein(actual).copy(recentFoods = recent)
                            }
                            onAdded(
                                "Added $actual g protein from ${baseName(f.name).lowercase()}" +
                                    if (actual < grams) " (daily limit reached)" else ""
                            )
                            selected = null
                            onBack()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, recent: Boolean, onClick: () -> Unit) {
    SquishButton(
        onClick = onClick,
        background = C.Surface,
        borderColor = C.Line,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            if (recent) LineIcon(Ic.CLOCK, C.Muted, 13.dp, 2.4f)
            Text(label, style = T.bold(13))
        }
    }
}

/** Fades and lifts each online result in, one after another. */
@Composable
private fun StaggerIn(index: Int, content: @Composable () -> Unit) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index * 45L)
        a.animateTo(1f, tween(260, easing = FastOutSlowInEasing))
    }
    Box(Modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 14.dp.toPx() }) { content() }
}

private val TINTS: List<Pair<Color, Color>>
    get() = listOf(C.OrangeSoft to C.OrangeText, C.BlueSoft to C.BlueDark, C.GreenSoft to C.GreenText, C.PinkSoft to C.PinkText)

@Composable
private fun FoodRow(f: FoodItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium), label = "rowScale")
    val bg by animateColorAsState(if (pressed) C.Faint else C.Surface, tween(120), label = "rowBg")
    val tint = TINTS[(f.name.firstOrNull()?.code ?: 0) % TINTS.size]
    Row(
        modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(bg, RoundedCornerShape(16.dp))
            .border(2.dp, C.Line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(40.dp).background(if (f.online) C.Faint else tint.first, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (f.online) LineIcon(Ic.BOX, C.Muted, 20.dp, 2f)
            else Text(f.name.take(1).uppercase(), style = T.display(18, tint.second))
        }
        Column(Modifier.weight(1f)) {
            Text(f.name, style = T.bold(15), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(f.subtitle, style = T.body(12, C.Muted), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${fmt1(f.protein)} g", style = T.display(17, C.OrangeText))
            Text(if (f.ml) "protein / 100 ml" else "protein / 100 g", style = T.body(11, C.Muted))
        }
    }
}

@Composable
private fun FoodSheet(
    food: FoodItem,
    state: AppState,
    onClose: () -> Unit,
    onStar: (label: String, grams: Int) -> Unit,
    onAdd: (grams: Int) -> Unit
) {
    var unitIndex by remember { mutableIntStateOf(0) }
    val unit = food.portions[unitIndex.coerceIn(0, food.portions.lastIndex)]
    var count by remember { mutableIntStateOf(food.portions.first().let { if (it.isPlainUnit) it.start else 1 }) }
    val weight = count * unit.grams
    val exact = food.proteinFor(weight)
    val rounded = exact.roundToInt().let { if (it == 0 && exact > 0.2) 1 else it }
    val shown by animateFloatAsState(exact.toFloat(), tween(380, easing = FastOutSlowInEasing), label = "protein")
    val unitWord = if (food.ml) "ml" else "g"

    val base = baseName(food.name).take(16)
    val label = if (unit.isPlainUnit) "$base $count $unitWord" else "$base ×$count"
    val starred = state.foods.any { it.name == label }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(starred) {
        pop.snapTo(0.7f)
        pop.animateTo(1f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium))
    }

    val unitText = if (unit.isPlainUnit) unit.label else {
        val b = unit.label.substringBefore(" (").trim()
        val word = if (count == 1 || b.endsWith("s")) b else b + "s"
        if (b.first().isDigit()) "× $b" else word
    }
    val maxCount = if (unit.isPlainUnit) 2000 else 30

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 640.dp)
            .background(C.Surface, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.align(Alignment.CenterHorizontally).size(44.dp, 5.dp).background(C.Line, CircleShape))
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(food.name, style = T.display(24))
                Text(food.subtitle, style = T.body(13, C.Muted))
            }
            SquishButton(
                onClick = { onStar(label, rounded.coerceAtLeast(1)) },
                background = if (starred) C.YellowSoft else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
                borderColor = C.YellowLine,
                contentPadding = PaddingValues(10.dp)
            ) {
                Box(Modifier.graphicsLayer { scaleX = pop.value; scaleY = pop.value }) {
                    if (starred) FillIcon(Ic.STAR, C.Yellow, 22.dp) else LineIcon(Ic.STAR, C.YellowDark, 22.dp, 1.8f)
                }
            }
            SquishButton(
                onClick = onClose,
                background = C.Faint,
                contentPadding = PaddingValues(12.dp)
            ) { LineIcon(Ic.CLOSE, C.Muted, 16.dp, 3f) }
        }
        AnimatedVisibility(visible = starred, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Text("Saved to Quick foods as \"$label\"", style = T.body(13, C.YellowText))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Macro(Modifier.weight(1f), "${fmt1(food.protein)} g", "protein", C.OrangeCard, C.OrangeText)
            Macro(Modifier.weight(1f), food.kcal?.toString() ?: "–", "kcal", C.Faint, C.Ink)
            Macro(Modifier.weight(1f), food.carbs?.let { "${fmt1(it)} g" } ?: "–", "carbs", C.Faint, C.Ink)
            Macro(Modifier.weight(1f), food.fat?.let { "${fmt1(it)} g" } ?: "–", "fat", C.Faint, C.Ink)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionLabel("How much did you have?")
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(C.Faint, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                food.portions.forEachIndexed { i, p ->
                    val sel = i == unitIndex
                    val bg by animateColorAsState(if (sel) (if (ThemeState.dark) C.Line else C.Surface) else Color.Transparent, tween(200), label = "seg")
                    Text(
                        p.label,
                        style = T.bold(13, if (sel) C.Ink else C.Muted),
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        modifier = Modifier
                            .weight(1f)
                            .background(bg, RoundedCornerShape(10.dp))
                            .clickable {
                                unitIndex = i
                                count = if (p.isPlainUnit) p.start else 1
                            }
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Btn3D(
                    onClick = { count = (count - unit.step).coerceAtLeast(unit.step) },
                    face = C.Surface, edge = C.OrangeLine, border = C.OrangeLine,
                    modifier = Modifier.size(52.dp, 58.dp), contentPadding = PaddingValues(0.dp)
                ) { Text("−", style = T.display(26, C.OrangeText)) }
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    AnimatedContent(
                        targetState = count,
                        transitionSpec = {
                            val up = targetState > initialState
                            (slideInVertically(tween(180)) { if (up) it / 2 else -it / 2 } + fadeIn(tween(180))) togetherWith
                                (slideOutVertically(tween(150)) { if (up) -it / 2 else it / 2 } + fadeOut(tween(120)))
                        },
                        label = "count"
                    ) { c -> Text("$c", style = T.display(34)) }
                    Spacer(Modifier.size(6.dp))
                    Text(unitText, style = T.bold(15, C.Muted), modifier = Modifier.padding(bottom = 4.dp))
                }
                Btn3D(
                    onClick = { count = (count + unit.step).coerceAtMost(maxCount) },
                    face = C.Surface, edge = C.OrangeLine, border = C.OrangeLine,
                    modifier = Modifier.size(52.dp, 58.dp), contentPadding = PaddingValues(0.dp)
                ) { Text("+", style = T.display(26, C.OrangeText)) }
            }
            Text(
                if (unit.isPlainUnit) "per 100 $unitWord: ${fmt1(food.protein)} g protein" else "= $weight $unitWord",
                style = T.body(13, C.Muted),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            Modifier
                .fillMaxWidth()
                .background(C.OrangeCard, RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("You get about", style = T.bold(15, C.OrangeText), modifier = Modifier.weight(1f))
            Text("≈ ${fmt1(shown.toDouble())} g protein", style = T.display(26, C.OrangeText))
        }

        Btn3D(
            onClick = { if (rounded > 0) onAdd(rounded) else onClose() },
            face = C.Orange, edge = C.OrangeDark, depth = 6.dp,
            contentPadding = PaddingValues(vertical = 13.dp)
        ) {
            Text(
                if (rounded > 0) "Add $rounded g protein" else "Too little protein to log",
                style = T.display(20, Color.White)
            )
        }
    }
}

@Composable
private fun Macro(modifier: Modifier, value: String, label: String, bg: Color, fg: Color) {
    Column(
        modifier
            .background(bg, RoundedCornerShape(14.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = T.display(17, fg), maxLines = 1)
        Text(label, style = T.body(11, C.Muted))
    }
}
