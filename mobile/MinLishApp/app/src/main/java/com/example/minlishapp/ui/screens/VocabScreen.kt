package com.example.minlishapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.minlishapp.data.Deck
import com.example.minlishapp.data.UserProgress
import com.example.minlishapp.data.Word
import com.example.minlishapp.data.DeckExportJson
import com.example.minlishapp.core.utils.LanguageHelper
import com.example.minlishapp.core.utils.translated
import com.google.gson.Gson
import androidx.compose.ui.tooling.preview.Preview
import com.example.minlishapp.ui.theme.MinLishAppTheme

@Composable
fun VocabScreen(
    vocabViewModel: com.example.minlishapp.ui.viewmodel.VocabViewModel,
    onNavigate: (Screen) -> Unit,
    userProgress: UserProgress,
    activeDeck: Deck?,
    onActiveDeckSelect: (Deck) -> Unit,
    onStartStudy: (Deck) -> Unit = {}
) {
    val decks by vocabViewModel.decks.collectAsState()
    val isLoadingDecks by vocabViewModel.isLoading.collectAsState()

    LaunchedEffect(Unit) {
        vocabViewModel.fetchDecks()
    }

    VocabScreenContent(
        decks = decks,
        isLoadingDecks = isLoadingDecks,
        userProgress = userProgress,
        activeDeck = activeDeck,
        onNavigate = onNavigate,
        onActiveDeckSelect = onActiveDeckSelect,
        onStartStudy = onStartStudy,
        onCreateDeck = { name, tag, onResult -> vocabViewModel.createDeck(name, tag, onResult) },
        onUpdateDeck = { deckId, name, tag, onResult -> vocabViewModel.updateDeck(deckId, name, tag, onResult) },
        onDeleteDeck = { deckId, onResult -> vocabViewModel.deleteDeck(deckId, onResult) },
        onFetchDeckCards = { deckId, onComplete -> vocabViewModel.fetchDeckCards(deckId, onComplete) },
        onCreateCard = { deckId, word, onResult -> vocabViewModel.createCard(deckId, word, onResult) },
        onUpdateCard = { cardId, deckId, word, onResult -> vocabViewModel.updateCard(cardId, deckId, word, onResult) },
        onDeleteCard = { cardId, onResult -> vocabViewModel.deleteCard(cardId, onResult) },
        onExportDeck = { deckId, onResult -> vocabViewModel.exportDeck(deckId, onResult) },
        onExportDeckCsv = { deckId, onResult -> vocabViewModel.exportDeckCsv(deckId, onResult) },
        onImportDeck = { uri, onResult -> vocabViewModel.importDeck(uri, onResult) }
    )
}

@Composable
fun VocabScreenContent(
    decks: List<Deck>,
    isLoadingDecks: Boolean,
    userProgress: UserProgress,
    activeDeck: Deck?,
    onNavigate: (Screen) -> Unit,
    onActiveDeckSelect: (Deck) -> Unit,
    onStartStudy: (Deck) -> Unit = {},
    onCreateDeck: (name: String, tag: String?, onResult: (Boolean, String) -> Unit) -> Unit,
    onUpdateDeck: (deckId: String, name: String, tag: String?, onResult: (Boolean, String) -> Unit) -> Unit,
    onDeleteDeck: (deckId: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onFetchDeckCards: (deckId: String, onComplete: (List<Word>) -> Unit) -> Unit,
    onCreateCard: (deckId: String, word: Word, onResult: (Boolean, String) -> Unit) -> Unit,
    onUpdateCard: (cardId: String, deckId: String, word: Word, onResult: (Boolean, String) -> Unit) -> Unit,
    onDeleteCard: (cardId: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onExportDeck: (deckId: String, onResult: (Boolean, DeckExportJson?) -> Unit) -> Unit,
    onExportDeckCsv: (deckId: String, onResult: (Boolean, String?) -> Unit) -> Unit,
    onImportDeck: (uri: android.net.Uri, onResult: (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current

    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, In Progress, Mastered

    // States for adding vocabulary word
    var showAddWordDialog by remember { mutableStateOf(false) }
    var selectedDeckForAddingWord by remember { mutableStateOf<Deck?>(null) }
    
    // States for managing cards
    var showManageCardsDialog by remember { mutableStateOf(false) }
    var selectedDeckForManaging by remember { mutableStateOf<Deck?>(null) }
    var managingCardsList by remember { mutableStateOf<List<Word>>(emptyList()) }
    var isLoadingCards by remember { mutableStateOf(false) }

    // States for Import/Export
    var showExportDialog by remember { mutableStateOf(false) }
    
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { sourceUri ->
            onImportDeck(sourceUri) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Nhập thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi: $msg", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val filteredDecks = remember(decks, searchQuery, selectedFilter) {
        decks.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
        }.filter {
            when (selectedFilter) {
                "In Progress" -> it.progress > 0f && it.progress < 1f
                "Mastered" -> it.progress >= 1f
                else -> true
            }
        }
    }

    Scaffold(
        bottomBar = { AppBottomBar(currentScreen = Screen.VocabDecks, onNavigate = onNavigate, appLanguage = userProgress.appLanguage) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm bộ từ".translated(userProgress.appLanguage))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Bộ từ vựng".translated(userProgress.appLanguage),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/csv", "text/comma-separated-values", "*/*")) }) {
                        Icon(
                            imageVector = Icons.Default.Upload,
                            contentDescription = "Import",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Tìm kiếm bộ từ...".translated(userProgress.appLanguage)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs
            FilterTabs(
                selectedFilter = selectedFilter,
                onFilterSelect = { selectedFilter = it },
                appLanguage = userProgress.appLanguage
            )

            Spacer(modifier = Modifier.height(16.dp))

            // List of Decks
            if (isLoadingDecks && decks.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredDecks.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Không tìm thấy bộ từ vựng nào.".translated(userProgress.appLanguage) else "Không tìm thấy kết quả cho".translated(userProgress.appLanguage) + " \"$searchQuery\"",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    itemsIndexed(filteredDecks) { index, deck ->
                        val currentOffsetPercent = when (index % 3) {
                            0 -> -0.1f
                            1 -> 0.1f
                            else -> 0.0f
                        }
                        val nextOffsetPercent = if (index < filteredDecks.size - 1) {
                            when ((index + 1) % 3) {
                                0 -> -0.1f
                                1 -> 0.1f
                                else -> 0.0f
                            }
                        } else 0f

                        DeckItem(
                            deck = deck,
                            index = index,
                            isLastItem = index == filteredDecks.size - 1,
                            currentOffsetPercent = currentOffsetPercent,
                            nextOffsetPercent = nextOffsetPercent,
                            userProgress = userProgress,
                            onNavigate = onNavigate,
                            onActiveDeckSelect = onActiveDeckSelect,
                            onStartStudy = onStartStudy,
                            onUpdateDeck = onUpdateDeck,
                            onDeleteDeck = onDeleteDeck,
                            onAddWordClick = {
                                selectedDeckForAddingWord = it
                                showAddWordDialog = true
                            },
                            onManageCardsClick = {
                                selectedDeckForManaging = it
                                isLoadingCards = true
                                showManageCardsDialog = true
                                onFetchDeckCards(it.id) { words ->
                                    managingCardsList = words
                                    isLoadingCards = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Add Deck Dialog
    if (showDialog) {
        AddDeckDialog(
            userProgress = userProgress,
            onDismiss = { showDialog = false },
            onCreateDeck = onCreateDeck
        )
    }

    // Add Word Dialog
    if (showAddWordDialog && selectedDeckForAddingWord != null) {
        AddWordDialog(
            deck = selectedDeckForAddingWord!!,
            userProgress = userProgress,
            onDismiss = { showAddWordDialog = false },
            onCreateCard = onCreateCard
        )
    }

    // Manage Cards Dialog
    if (showManageCardsDialog && selectedDeckForManaging != null) {
        ManageCardsDialog(
            deck = selectedDeckForManaging!!,
            userProgress = userProgress,
            isLoadingCards = isLoadingCards,
            cardsList = managingCardsList,
            onDismiss = { showManageCardsDialog = false },
            onUpdateCard = onUpdateCard,
            onDeleteCard = onDeleteCard,
            onRefreshCards = {
                isLoadingCards = true
                onFetchDeckCards(selectedDeckForManaging!!.id) { words ->
                    managingCardsList = words
                    isLoadingCards = false
                }
            }
        )
    }

    // Export Dialog
    if (showExportDialog) {
        ExportDeckDialog(
            decks = decks,
            userProgress = userProgress,
            onDismiss = { showExportDialog = false },
            onExportDeck = onExportDeck,
            onExportDeckCsv = onExportDeckCsv
        )
    }
}

@Composable
fun FilterTabs(
    selectedFilter: String,
    onFilterSelect: (String) -> Unit,
    appLanguage: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf(
            "All" to "Tất cả",
            "In Progress" to "Đang học",
            "Mastered" to "Đã thuộc"
        ).forEach { (id, label) ->
            val isSel = selectedFilter == id
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isSel) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surface
                    )
                    .border(
                        1.dp,
                        if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { onFilterSelect(id) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = label.translated(appLanguage),
                    color = if (isSel) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun DeckItem(
    deck: Deck,
    index: Int,
    isLastItem: Boolean,
    currentOffsetPercent: Float,
    nextOffsetPercent: Float,
    userProgress: UserProgress,
    onNavigate: (Screen) -> Unit,
    onActiveDeckSelect: (Deck) -> Unit,
    onStartStudy: (Deck) -> Unit,
    onUpdateDeck: (deckId: String, name: String, tag: String?, onResult: (Boolean, String) -> Unit) -> Unit,
    onDeleteDeck: (deckId: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onAddWordClick: (Deck) -> Unit,
    onManageCardsClick: (Deck) -> Unit
) {
    val context = LocalContext.current
    val offsetPercent = when (index % 3) {
        0 -> -0.1f
        1 -> 0.1f
        else -> 0.0f
    }
    val alignmentBias = offsetPercent * 100

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = alignmentBias.dp)
    ) {
        val isCompleted = deck.progress >= 1.0f
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isCompleted) Color(0xFFDCFCE7) else MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(
                1.dp,
                if (isCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            modifier = Modifier
                .width(280.dp)
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                var expandedMenu by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                var showDeleteConfirmDialog by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val tagText = deck.tags.firstOrNull() ?: "CHỦ ĐỀ"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = tagText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box {
                        IconButton(onClick = { expandedMenu = true }) {
                            Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Tùy chọn".translated(userProgress.appLanguage))
                        }
                        DropdownMenu(
                            expanded = expandedMenu,
                            onDismissRequest = { expandedMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Quản lý từ vựng".translated(userProgress.appLanguage)) },
                                onClick = {
                                    expandedMenu = false
                                    onManageCardsClick(deck)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Sửa thông tin".translated(userProgress.appLanguage)) },
                                onClick = { 
                                    expandedMenu = false
                                    showEditDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bộ từ".translated(userProgress.appLanguage)) },
                                onClick = { 
                                    expandedMenu = false
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }

                if (showEditDialog) {
                    EditDeckDialog(
                        deck = deck,
                        userProgress = userProgress,
                        onDismiss = { showEditDialog = false },
                        onUpdateDeck = onUpdateDeck
                    )
                }

                if (showDeleteConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirmDialog = false },
                        title = { Text("Xác nhận xóa".translated(userProgress.appLanguage)) },
                        text = { Text("Bạn có chắc chắn muốn xóa bộ từ này không?".translated(userProgress.appLanguage)) },
                        confirmButton = {
                            Button(
                                onClick = {
                                    showDeleteConfirmDialog = false
                                    onDeleteDeck(deck.id) { success, msg ->
                                        if (success) {
                                            Toast.makeText(context, "Đã xóa".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Xóa".translated(userProgress.appLanguage))
                            }
                        },
                        dismissButton = {
                            OutlinedButton(onClick = { showDeleteConfirmDialog = false }) {
                                Text("Hủy".translated(userProgress.appLanguage))
                            }
                        }
                    )
                }

                Text(
                    text = deck.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "${deck.wordCount} " + "từ".translated(userProgress.appLanguage),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddWordClick(deck) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text(
                            text = "+ Từ mới".translated(userProgress.appLanguage),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    val buttonColor = Color(0xFF10B981)
                    OutlinedButton(
                        onClick = { onStartStudy(deck) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, buttonColor),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = buttonColor
                        ),
                        modifier = Modifier.weight(1.2f).height(44.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Vào học".translated(userProgress.appLanguage),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }

    if (!isLastItem) {
        val connectorOffset = (currentOffsetPercent + nextOffsetPercent) / 2
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp)
        ) {
            val startX = size.width / 2 + (connectorOffset * 100.dp.toPx())
            drawLine(
                color = Color.LightGray.copy(alpha = 0.6f),
                start = Offset(startX, 0f),
                end = Offset(startX, size.height),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }
    }
}

@Composable
fun AddDeckDialog(
    userProgress: UserProgress,
    onDismiss: () -> Unit,
    onCreateDeck: (name: String, tag: String?, onResult: (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var newDeckName by remember { mutableStateOf("") }
    var newDeckDesc by remember { mutableStateOf("") }
    var newDeckTags by remember { mutableStateOf("Cá nhân") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Tạo Bộ Từ Vựng Mới".translated(userProgress.appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = newDeckName,
                    onValueChange = { newDeckName = it },
                    label = { Text("Tên bộ từ".translated(userProgress.appLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = newDeckDesc,
                    onValueChange = { newDeckDesc = it },
                    label = { Text("Mô tả".translated(userProgress.appLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )

                OutlinedTextField(
                    value = newDeckTags,
                    onValueChange = { newDeckTags = it },
                    label = { Text("Nhãn / Tags (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) },
                    placeholder = { Text("Ví dụ: IELTS, Giao tiếp".translated(userProgress.appLanguage)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Hủy".translated(userProgress.appLanguage))
                    }

                    Button(
                        onClick = {
                            if (newDeckName.isNotBlank()) {
                                onCreateDeck(newDeckName, newDeckTags.split(",").firstOrNull()?.trim()) { success, msg ->
                                    if (success) {
                                        Toast.makeText(context, "Thêm bộ từ thành công".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Lưu".translated(userProgress.appLanguage))
                    }
                }
            }
        }
    }
}

@Composable
fun EditDeckDialog(
    deck: Deck,
    userProgress: UserProgress,
    onDismiss: () -> Unit,
    onUpdateDeck: (deckId: String, name: String, tag: String?, onResult: (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var editDeckName by remember { mutableStateOf(deck.name) }
    var editDeckDesc by remember { mutableStateOf(deck.description) }
    var editDeckTags by remember { mutableStateOf(deck.tags.joinToString(", ")) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Sửa Bộ Từ Vựng".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold, fontSize = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(
                    value = editDeckName,
                    onValueChange = { editDeckName = it },
                    label = { Text("Tên bộ từ".translated(userProgress.appLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editDeckDesc,
                    onValueChange = { editDeckDesc = it },
                    label = { Text("Mô tả".translated(userProgress.appLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = editDeckTags,
                    onValueChange = { editDeckTags = it },
                    label = { Text("Nhãn / Tags".translated(userProgress.appLanguage)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    var showConfirmSaveDeck by remember { mutableStateOf(false) }

                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Hủy".translated(userProgress.appLanguage))
                    }
                    Button(
                        onClick = { showConfirmSaveDeck = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lưu".translated(userProgress.appLanguage))
                    }

                    if (showConfirmSaveDeck) {
                        AlertDialog(
                            onDismissRequest = { showConfirmSaveDeck = false },
                            title = { Text("Xác nhận lưu".translated(userProgress.appLanguage)) },
                            text = { Text("Bạn có chắc chắn muốn lưu các thay đổi này không?".translated(userProgress.appLanguage)) },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showConfirmSaveDeck = false
                                        onUpdateDeck(deck.id, editDeckName, editDeckTags.split(",").firstOrNull()?.trim()) { success, msg ->
                                            if (success) {
                                                onDismiss()
                                                Toast.makeText(context, "Đã sửa".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                ) {
                                    Text("Lưu".translated(userProgress.appLanguage))
                                }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = { showConfirmSaveDeck = false }) {
                                    Text("Hủy".translated(userProgress.appLanguage))
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddWordDialog(
    deck: Deck,
    userProgress: UserProgress,
    onDismiss: () -> Unit,
    onCreateCard: (deckId: String, word: Word, onResult: (Boolean, String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var newWordText by remember { mutableStateOf("") }
    var newWordPron by remember { mutableStateOf("") }
    var newWordMeaning by remember { mutableStateOf("") }
    var newWordDesc by remember { mutableStateOf("") }
    var newWordExample by remember { mutableStateOf("") }
    var newWordExampleTrans by remember { mutableStateOf("") }
    var newWordCollocations by remember { mutableStateOf("") }
    var newWordSynonyms by remember { mutableStateOf("") }
    var newWordRelated by remember { mutableStateOf("") }
    var newWordNote by remember { mutableStateOf("") }
    var newWordType by remember { mutableStateOf("noun") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Thêm Từ Vựng Vào Bộ".translated(userProgress.appLanguage),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Bộ từ:".translated(userProgress.appLanguage) + " ${deck.name}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(weight = 1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = newWordText,
                        onValueChange = { newWordText = it },
                        label = { Text("Từ tiếng Anh (*)".translated(userProgress.appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(text = "Loại từ:".translated(userProgress.appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("verb" to "Động từ", "noun" to "Danh từ", "adjective" to "Tính từ", "adverb" to "Trạng từ").forEach { (typeKey, label) ->
                            val isSel = newWordType == typeKey
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable { newWordType = typeKey }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                    Text(
                                        text = label.translated(userProgress.appLanguage),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newWordPron,
                        onValueChange = { newWordPron = it },
                        label = { Text("Phiên âm UK (*)".translated(userProgress.appLanguage)) },
                        placeholder = { Text("Ví dụ: /ə'kɒmədeɪt/".translated(userProgress.appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordMeaning,
                        onValueChange = { newWordMeaning = it },
                        label = { Text("Nghĩa tiếng Việt (*)".translated(userProgress.appLanguage)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordDesc,
                        onValueChange = { newWordDesc = it },
                        label = { Text("Định nghĩa tiếng Anh (Description)".translated(userProgress.appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordExample,
                        onValueChange = { newWordExample = it },
                        label = { Text("Câu ví dụ tiếng Anh".translated(userProgress.appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordExampleTrans,
                        onValueChange = { newWordExampleTrans = it },
                        label = { Text("Dịch nghĩa câu ví dụ".translated(userProgress.appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordCollocations,
                        onValueChange = { newWordCollocations = it },
                        label = { Text("Collocations (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) },
                        placeholder = { Text("Ví dụ: accommodate guests, accommodate needs".translated(userProgress.appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordSynonyms,
                        onValueChange = { newWordSynonyms = it },
                        label = { Text("Từ đồng nghĩa (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) },
                        placeholder = { Text("Ví dụ: hold, contain".translated(userProgress.appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordRelated,
                        onValueChange = { newWordRelated = it },
                        label = { Text("Từ liên quan (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newWordNote,
                        onValueChange = { newWordNote = it },
                        label = { Text("Ghi chú (Note)".translated(userProgress.appLanguage)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Hủy".translated(userProgress.appLanguage))
                    }

                    Button(
                        onClick = {
                            if (newWordText.isNotBlank() && newWordMeaning.isNotBlank() && newWordPron.isNotBlank()) {
                                val newWord = Word(
                                    word = newWordText.trim(),
                                    pronunciation = newWordPron.trim(),
                                    meaning = newWordMeaning.trim(),
                                    description = newWordDesc.trim(),
                                    example = newWordExample.trim(),
                                    exampleTranslation = newWordExampleTrans.trim(),
                                    wordType = newWordType,
                                    collocations = newWordCollocations.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    synonyms = newWordSynonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    relatedWords = newWordRelated.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                    note = newWordNote.trim()
                                )
                                
                                onCreateCard(deck.id, newWord) { success, msg ->
                                    if (success) {
                                        Toast.makeText(context, "Thêm từ thành công".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                        onDismiss()
                                    } else {
                                        Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Vui lòng điền đủ các trường bắt buộc (*)".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Lưu".translated(userProgress.appLanguage))
                    }
                }
            }
        }
    }
}

@Composable
fun ManageCardsDialog(
    deck: Deck,
    userProgress: UserProgress,
    isLoadingCards: Boolean,
    cardsList: List<Word>,
    onDismiss: () -> Unit,
    onUpdateCard: (cardId: String, deckId: String, word: Word, onResult: (Boolean, String) -> Unit) -> Unit,
    onDeleteCard: (cardId: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onRefreshCards: () -> Unit
) {
    var editingCard by remember { mutableStateOf<Word?>(null) }
    var cardToDelete by remember { mutableStateOf<Word?>(null) }
    val context = LocalContext.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng".translated(userProgress.appLanguage))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Quản lý từ vựng".translated(userProgress.appLanguage) + " - ${deck.name}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                }

                if (isLoadingCards) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (cardsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Bộ từ này chưa có từ vựng nào.".translated(userProgress.appLanguage))
                    }
                } else {
                    if (editingCard != null) {
                        EditCardForm(
                            card = editingCard!!,
                            deckId = deck.id,
                            userProgress = userProgress,
                            onCancel = { editingCard = null },
                            onSave = { updatedWord ->
                                onUpdateCard(updatedWord.id, deck.id, updatedWord) { success, msg ->
                                    if (success) {
                                        Toast.makeText(context, "Cập nhật thành công".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                        editingCard = null
                                        onRefreshCards()
                                    } else {
                                        Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize().weight(1f),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(cardsList) { card ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(card.word, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                            if (card.pronunciation.isNotBlank()) {
                                                Text(card.pronunciation, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Text(card.meaning, fontSize = 14.sp)
                                        }
                                        IconButton(onClick = { editingCard = card }) {
                                            Icon(Icons.Default.Edit, contentDescription = "Sửa".translated(userProgress.appLanguage), tint = MaterialTheme.colorScheme.primary)
                                        }
                                        IconButton(onClick = { cardToDelete = card }) {
                                            Icon(Icons.Default.Delete, contentDescription = "Xóa".translated(userProgress.appLanguage), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }

                        if (cardToDelete != null) {
                            AlertDialog(
                                onDismissRequest = { cardToDelete = null },
                                title = { Text("Xác nhận xóa".translated(userProgress.appLanguage)) },
                                text = { Text("Bạn có chắc chắn muốn xóa từ vựng '${cardToDelete?.word}' không?".translated(userProgress.appLanguage)) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val targetId = cardToDelete!!.id
                                            cardToDelete = null
                                            onDeleteCard(targetId) { success, msg ->
                                                if (success) {
                                                    Toast.makeText(context, "Đã xóa".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                    onRefreshCards()
                                                } else {
                                                    Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Text("Xóa".translated(userProgress.appLanguage), color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    OutlinedButton(onClick = { cardToDelete = null }) {
                                        Text("Hủy".translated(userProgress.appLanguage))
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditCardForm(
    card: Word,
    deckId: String,
    userProgress: UserProgress,
    onCancel: () -> Unit,
    onSave: (Word) -> Unit
) {
    var editWord by remember { mutableStateOf(card.word) }
    var editPronunciation by remember { mutableStateOf(card.pronunciation) }
    var editMeaning by remember { mutableStateOf(card.meaning) }
    var editDescription by remember { mutableStateOf(card.description) }
    var editExample by remember { mutableStateOf(card.example) }
    var editExampleTrans by remember { mutableStateOf(card.exampleTranslation) }
    var editWordType by remember { mutableStateOf(card.wordType) }
    var editCollocations by remember { mutableStateOf(card.collocations.joinToString(", ")) }
    var editSynonyms by remember { mutableStateOf(card.synonyms.joinToString(", ")) }
    var editRelatedWords by remember { mutableStateOf(card.relatedWords.joinToString(", ")) }
    var editNote by remember { mutableStateOf(card.note) }

    Column(
        modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Sửa từ vựng".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold, fontSize = 18.sp)

        OutlinedTextField(
            value = editWord, onValueChange = { editWord = it },
            label = { Text("Từ vựng *".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )

        Text(text = "Loại từ:".translated(userProgress.appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("verb" to "Động từ", "noun" to "Danh từ", "adobjective" to "Tính từ", "adverb" to "Trạng từ").map { it.first to it.second }.forEach { (typeKey, label) ->
                val isSel = editWordType == typeKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSel) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .border(
                            1.dp,
                            if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent,
                            RoundedCornerShape(8.dp)
                        )
                        .clickable { editWordType = typeKey }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label.translated(userProgress.appLanguage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) MaterialTheme.colorScheme.primary else Color.Gray
                    )
                }
            }
        }

        OutlinedTextField(
            value = editPronunciation, onValueChange = { editPronunciation = it },
            label = { Text("Phiên âm".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editMeaning, onValueChange = { editMeaning = it },
            label = { Text("Nghĩa tiếng Việt *".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editDescription, onValueChange = { editDescription = it },
            label = { Text("Nghĩa tiếng Anh (Description)".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editExample, onValueChange = { editExample = it },
            label = { Text("Ví dụ".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editExampleTrans, onValueChange = { editExampleTrans = it },
            label = { Text("Dịch nghĩa câu ví dụ".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editCollocations, onValueChange = { editCollocations = it },
            label = { Text("Collocations (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editSynonyms, onValueChange = { editSynonyms = it },
            label = { Text("Từ đồng nghĩa (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editRelatedWords, onValueChange = { editRelatedWords = it },
            label = { Text("Từ liên quan (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editNote, onValueChange = { editNote = it },
            label = { Text("Ghi chú (Note)".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
        )

        var showConfirmSaveWord by remember { mutableStateOf(false) }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Hủy".translated(userProgress.appLanguage))
            }
            Button(
                onClick = { showConfirmSaveWord = true },
                modifier = Modifier.weight(1f)
            ) {
                Text("Lưu".translated(userProgress.appLanguage))
            }
        }

        if (showConfirmSaveWord) {
            AlertDialog(
                onDismissRequest = { showConfirmSaveWord = false },
                title = { Text("Xác nhận lưu".translated(userProgress.appLanguage)) },
                text = { Text("Bạn có chắc chắn muốn lưu các thay đổi của từ vựng này không?".translated(userProgress.appLanguage)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmSaveWord = false
                            if (editWord.isNotBlank() && editMeaning.isNotBlank()) {
                                onSave(
                                    card.copy(
                                        word = editWord.trim(),
                                        pronunciation = editPronunciation.trim(),
                                        meaning = editMeaning.trim(),
                                        description = editDescription.trim(),
                                        example = editExample.trim(),
                                        exampleTranslation = editExampleTrans.trim(),
                                        wordType = editWordType,
                                        collocations = editCollocations.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                        synonyms = editSynonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                        relatedWords = editRelatedWords.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                        note = editNote.trim()
                                    )
                                )
                            }
                        }
                    ) {
                        Text("Lưu".translated(userProgress.appLanguage))
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showConfirmSaveWord = false }) {
                        Text("Hủy".translated(userProgress.appLanguage))
                    }
                }
            )
        }
    }
}

@Composable
fun ExportDeckDialog(
    decks: List<Deck>,
    userProgress: UserProgress,
    onDismiss: () -> Unit,
    onExportDeck: (deckId: String, onResult: (Boolean, DeckExportJson?) -> Unit) -> Unit,
    onExportDeckCsv: (deckId: String, onResult: (Boolean, String?) -> Unit) -> Unit
) {
    val context = LocalContext.current
    var deckToExport by remember { mutableStateOf<Deck?>(null) }
    var exportFormat by remember { mutableStateOf("json") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        onDismiss()
        uri?.let { destUri ->
            deckToExport?.let { deck ->
                onExportDeck(deck.id) { success, exportJson ->
                    if (success && exportJson != null) {
                        try {
                            context.contentResolver.openOutputStream(destUri)?.use { out ->
                                out.write(Gson().toJson(exportJson).toByteArray(Charsets.UTF_8))
                            }
                            Toast.makeText(context, "Xuất JSON thành công", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi ghi file", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Lỗi xuất dữ liệu", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    val csvExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        onDismiss()
        uri?.let { destUri ->
            deckToExport?.let { deck ->
                onExportDeckCsv(deck.id) { success, csvString ->
                    if (success && csvString != null) {
                        try {
                            context.contentResolver.openOutputStream(destUri)?.use { out ->
                                val bom = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
                                out.write(bom)
                                out.write(csvString.toByteArray(Charsets.UTF_8))
                            }
                            Toast.makeText(context, "Xuất CSV thành công", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Lỗi ghi file", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Lỗi xuất dữ liệu", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Chọn Bộ Từ Xuất Ra".translated(userProgress.appLanguage),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = exportFormat == "json",
                        onClick = { exportFormat = "json" },
                        label = { Text("JSON") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = exportFormat == "csv",
                        onClick = { exportFormat = "csv" },
                        label = { Text("CSV") },
                        modifier = Modifier.weight(1f)
                    )
                }

                decks.forEach { deck ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            deckToExport = deck
                            if (exportFormat == "csv") {
                                csvExportLauncher.launch("${deck.name}.csv")
                            } else {
                                exportLauncher.launch("${deck.name}.json")
                            }
                        },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "📚", fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = deck.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "${deck.wordCount} " + "từ".translated(userProgress.appLanguage),
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Hủy".translated(userProgress.appLanguage))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VocabScreenPreview() {
    val mockDecks = listOf(
        Deck(
            id = "1",
            name = "IELTS Academic Vocabulary",
            description = "Chủ đề học thuật nâng cao",
            tags = listOf("IELTS"),
            apiProgress = 0.4f,
            apiWordCount = 35
        ),
        Deck(
            id = "2",
            name = "Giao tiếp cơ bản",
            description = "Từ vựng thông dụng hàng ngày",
            tags = listOf("Giao tiếp"),
            apiProgress = 1.0f,
            apiWordCount = 15
        )
    )

    val mockUserProgress = UserProgress(
        appLanguage = "Vietnamese"
    )

    MinLishAppTheme {
        VocabScreenContent(
            decks = mockDecks,
            isLoadingDecks = false,
            userProgress = mockUserProgress,
            activeDeck = null,
            onNavigate = {},
            onActiveDeckSelect = {},
            onStartStudy = {},
            onCreateDeck = { _, _, cb -> cb(true, "") },
            onUpdateDeck = { _, _, _, cb -> cb(true, "") },
            onDeleteDeck = { _, cb -> cb(true, "") },
            onFetchDeckCards = { _, cb -> cb(emptyList()) },
            onCreateCard = { _, _, cb -> cb(true, "") },
            onUpdateCard = { _, _, _, cb -> cb(true, "") },
            onDeleteCard = { _, cb -> cb(true, "") },
            onExportDeck = { _, cb -> cb(true, null) },
            onExportDeckCsv = { _, cb -> cb(true, "") },
            onImportDeck = { _, cb -> cb(true, "") }
        )
    }
}