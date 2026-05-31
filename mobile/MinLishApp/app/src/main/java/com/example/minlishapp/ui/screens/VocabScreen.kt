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
import com.example.minlishapp.core.utils.LanguageHelper
import com.example.minlishapp.core.utils.translated
import com.google.gson.Gson


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
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        vocabViewModel.fetchDecks()
    }

    var showDialog by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }
    var newDeckDesc by remember { mutableStateOf("") }
    var newDeckTags by remember { mutableStateOf("Cá nhân") }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, In Progress, Mastered

    // States for adding vocabulary word
    var showAddWordDialog by remember { mutableStateOf(false) }
    var selectedDeckForAddingWord by remember { mutableStateOf<Deck?>(null) }
    
    // States for managing cards
    var showManageCardsDialog by remember { mutableStateOf(false) }
    var selectedDeckForManaging by remember { mutableStateOf<Deck?>(null) }
    var managingCardsList by remember { mutableStateOf<List<com.example.minlishapp.data.Word>>(emptyList()) }
    var isLoadingCards by remember { mutableStateOf(false) }

    // States for Import/Export
    var showExportDialog by remember { mutableStateOf(false) }
    var deckToExport by remember { mutableStateOf<Deck?>(null) }
    
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { destUri ->
            deckToExport?.let { deck ->
                vocabViewModel.exportDeck(deck.id) { success, exportJson ->
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
        uri?.let { destUri ->
            deckToExport?.let { deck ->
                vocabViewModel.exportDeckCsv(deck.id) { success, csvString ->
                    if (success && csvString != null) {
                        try {
                            context.contentResolver.openOutputStream(destUri)?.use { out ->
                                // Write UTF-8 BOM so Excel opens it correctly
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

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { sourceUri ->
            vocabViewModel.importDeck(sourceUri) { success, msg ->
                if (success) {
                    Toast.makeText(context, "Nhập thành công", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi: $msg", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
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

            // Tabs bộ lọc
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
                            .clickable { selectedFilter = id }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = label.translated(userProgress.appLanguage),
                            color = if (isSel) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // List of Decks - Staggered connected roadmap layout
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
                            // Render thẻ chủ đề học từ chuẩn thiết kế (Image 1)
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Tag
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
                                                        selectedDeckForManaging = deck
                                                        isLoadingCards = true
                                                        showManageCardsDialog = true
                                                        vocabViewModel.fetchDeckCards(deck.id) { words ->
                                                            managingCardsList = words
                                                            isLoadingCards = false
                                                        }
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
                                                        vocabViewModel.deleteDeck(deck.id) { success, msg ->
                                                            if (success) {
                                                                Toast.makeText(context, "Đã xóa".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                            } else {
                                                                Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    if (showEditDialog) {
                                        var editDeckName by remember { mutableStateOf(deck.name) }
                                        var editDeckDesc by remember { mutableStateOf(deck.description) }
                                        var editDeckTags by remember { mutableStateOf(deck.tags.joinToString(", ")) }

                                        Dialog(onDismissRequest = { showEditDialog = false }) {
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
                                                        OutlinedButton(onClick = { showEditDialog = false }, modifier = Modifier.weight(1f)) {
                                                            Text("Hủy".translated(userProgress.appLanguage))
                                                        }
                                                        Button(
                                                            onClick = {
                                                                vocabViewModel.updateDeck(deck.id, editDeckName, editDeckTags.split(",").firstOrNull()?.trim()) { success, msg ->
                                                                    if (success) {
                                                                        showEditDialog = false
                                                                        Toast.makeText(context, "Đã sửa".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                }
                                                            },
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Text("Lưu".translated(userProgress.appLanguage))
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Title
                                    Text(
                                        text = deck.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Subtitle
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
                                        // Nút Thêm từ mới (Primary blue)
                                        OutlinedButton(
                                            onClick = {
                                                selectedDeckForAddingWord = deck
                                                showAddWordDialog = true
                                            },
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

                                        // Nút Vào học bo viền xanh lá chuẩn thiết kế
                                        val buttonColor = Color(0xFF10B981)
                                        OutlinedButton(
                                            onClick = {
                                                onStartStudy(deck)
                                            },
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

                        // Vẽ connector nét đứt nối giữa các thẻ lộ trình
                        if (index < filteredDecks.size - 1) {
                            val nextOffsetPercent = when ((index + 1) % 3) {
                                0 -> -0.1f
                                1 -> 0.1f
                                else -> 0.0f
                            }
                            val currentOffsetPercent = when (index % 3) {
                                0 -> -0.1f
                                1 -> 0.1f
                                else -> 0.0f
                            }
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
                }
            }
        }

        // Add Deck Dialog
        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
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
                                onClick = { showDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Hủy".translated(userProgress.appLanguage))
                            }

                            Button(
                                onClick = {
                                    if (newDeckName.isNotBlank()) {
                                        vocabViewModel.createDeck(newDeckName, newDeckTags.split(",").firstOrNull()?.trim()) { success, msg ->
                                            if (success) {
                                                Toast.makeText(context, "Thêm bộ từ thành công".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                newDeckName = ""
                                                newDeckDesc = ""
                                                newDeckTags = "Cá nhân"
                                                showDialog = false
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

        // Add Word Dialog
        if (showAddWordDialog && selectedDeckForAddingWord != null) {
            Dialog(onDismissRequest = { showAddWordDialog = false }) {
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
                        // Sticky Header
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
                                text = "Bộ từ:".translated(userProgress.appLanguage) + " ${selectedDeckForAddingWord?.name}",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Scrollable Body Content
                        Column(
                            modifier = Modifier
                                .weight(weight = 1f, fill = false)
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // 1. Từ vựng
                            OutlinedTextField(
                                value = newWordText,
                                onValueChange = { newWordText = it },
                                label = { Text("Từ tiếng Anh (*)".translated(userProgress.appLanguage)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 2. Loại từ (adjective, noun, verb...)
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

                            // 3. Phiên âm
                            OutlinedTextField(
                                value = newWordPron,
                                onValueChange = { newWordPron = it },
                                label = { Text("Phiên âm UK (*)".translated(userProgress.appLanguage)) },
                                placeholder = { Text("Ví dụ: /ə'kɒmədeɪt/".translated(userProgress.appLanguage)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 4. Nghĩa tiếng Việt
                            OutlinedTextField(
                                value = newWordMeaning,
                                onValueChange = { newWordMeaning = it },
                                label = { Text("Nghĩa tiếng Việt (*)".translated(userProgress.appLanguage)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 4b. Định nghĩa tiếng Anh
                            OutlinedTextField(
                                value = newWordDesc,
                                onValueChange = { newWordDesc = it },
                                label = { Text("Định nghĩa tiếng Anh (Description)".translated(userProgress.appLanguage)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 5. Câu ví dụ tiếng Anh
                            OutlinedTextField(
                                value = newWordExample,
                                onValueChange = { newWordExample = it },
                                label = { Text("Câu ví dụ tiếng Anh".translated(userProgress.appLanguage)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 6. Dịch nghĩa câu ví dụ
                            OutlinedTextField(
                                value = newWordExampleTrans,
                                onValueChange = { newWordExampleTrans = it },
                                label = { Text("Dịch nghĩa câu ví dụ".translated(userProgress.appLanguage)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 7. Collocations
                            OutlinedTextField(
                                value = newWordCollocations,
                                onValueChange = { newWordCollocations = it },
                                label = { Text("Collocations (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) },
                                placeholder = { Text("Ví dụ: accommodate guests, accommodate needs".translated(userProgress.appLanguage)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 8. Từ đồng nghĩa
                            OutlinedTextField(
                                value = newWordSynonyms,
                                onValueChange = { newWordSynonyms = it },
                                label = { Text("Từ đồng nghĩa (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) },
                                placeholder = { Text("Ví dụ: hold, contain".translated(userProgress.appLanguage)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 8b. Từ liên quan (Related words)
                            OutlinedTextField(
                                value = newWordRelated,
                                onValueChange = { newWordRelated = it },
                                label = { Text("Từ liên quan (Phân cách bằng dấu phẩy)".translated(userProgress.appLanguage)) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 8c. Ghi chú (Note)
                            OutlinedTextField(
                                value = newWordNote,
                                onValueChange = { newWordNote = it },
                                label = { Text("Ghi chú (Note)".translated(userProgress.appLanguage)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Sticky Footer Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { showAddWordDialog = false },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Hủy".translated(userProgress.appLanguage))
                            }

                            Button(
                                onClick = {
                                    if (newWordText.isNotBlank() && newWordMeaning.isNotBlank() && newWordPron.isNotBlank()) {
                                        val newWord = com.example.minlishapp.data.Word(
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
                                        
                                        vocabViewModel.createCard(selectedDeckForAddingWord!!.id, newWord) { success, msg ->
                                            if (success) {
                                                Toast.makeText(context, "Thêm từ thành công".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                newWordText = ""
                                                newWordPron = ""
                                                newWordMeaning = ""
                                                newWordDesc = ""
                                                newWordExample = ""
                                                newWordExampleTrans = ""
                                                newWordCollocations = ""
                                                newWordSynonyms = ""
                                                newWordRelated = ""
                                                newWordNote = ""
                                                newWordType = "noun"
                                                showAddWordDialog = false
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

        // Manage Cards Dialog
        if (showManageCardsDialog && selectedDeckForManaging != null) {
            Dialog(
                onDismissRequest = { showManageCardsDialog = false },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { showManageCardsDialog = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Đóng".translated(userProgress.appLanguage))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Quản lý từ vựng".translated(userProgress.appLanguage) + " - ${selectedDeckForManaging?.name}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        if (isLoadingCards) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        } else if (managingCardsList.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("Bộ từ này chưa có từ vựng nào.".translated(userProgress.appLanguage))
                            }
                        } else {
                            var editingCard by remember { mutableStateOf<com.example.minlishapp.data.Word?>(null) }
                            
                            if (editingCard != null) {
                                // Edit Form
                                Column(
                                    modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text("Sửa từ vựng".translated(userProgress.appLanguage), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    var editWord by remember { mutableStateOf(editingCard!!.word) }
                                    var editPronunciation by remember { mutableStateOf(editingCard!!.pronunciation) }
                                    var editMeaning by remember { mutableStateOf(editingCard!!.meaning) }
                                    var editDescription by remember { mutableStateOf(editingCard!!.description) }
                                    var editExample by remember { mutableStateOf(editingCard!!.example) }
                                    var editExampleTrans by remember { mutableStateOf(editingCard!!.exampleTranslation) }
                                    var editWordType by remember { mutableStateOf(editingCard!!.wordType) }
                                    var editCollocations by remember { mutableStateOf(editingCard!!.collocations.joinToString(", ")) }
                                    var editSynonyms by remember { mutableStateOf(editingCard!!.synonyms.joinToString(", ")) }
                                    var editRelatedWords by remember { mutableStateOf(editingCard!!.relatedWords.joinToString(", ")) }
                                    var editNote by remember { mutableStateOf(editingCard!!.note) }

                                    OutlinedTextField(
                                        value = editWord, onValueChange = { editWord = it },
                                        label = { Text("Từ vựng *".translated(userProgress.appLanguage)) }, modifier = Modifier.fillMaxWidth()
                                    )

                                    // Loại từ (adjective, noun, verb...)
                                    Text(text = "Loại từ:".translated(userProgress.appLanguage), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("verb" to "Động từ", "noun" to "Danh từ", "adjective" to "Tính từ", "adverb" to "Trạng từ").forEach { (typeKey, label) ->
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

                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        OutlinedButton(onClick = { editingCard = null }, modifier = Modifier.weight(1f)) {
                                            Text("Hủy".translated(userProgress.appLanguage))
                                        }
                                        Button(
                                            onClick = {
                                                if (editWord.isNotBlank() && editMeaning.isNotBlank()) {
                                                    val updatedWord = editingCard!!.copy(
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
                                                    vocabViewModel.updateCard(
                                                        cardId = updatedWord.id,
                                                        deckId = selectedDeckForManaging!!.id,
                                                        word = updatedWord
                                                    ) { success, msg ->
                                                        if (success) {
                                                            Toast.makeText(context, "Cập nhật thành công".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                            editingCard = null
                                                            isLoadingCards = true
                                                            vocabViewModel.fetchDeckCards(selectedDeckForManaging!!.id) { words ->
                                                                managingCardsList = words
                                                                isLoadingCards = false
                                                            }
                                                        } else {
                                                            Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                } else {
                                                    Toast.makeText(context, "Vui lòng nhập đủ các trường bắt buộc".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Lưu".translated(userProgress.appLanguage))
                                        }
                                    }
                                }
                            } else {
                                // List of Cards
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(managingCardsList) { card ->
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
                                                IconButton(onClick = {
                                                    vocabViewModel.deleteCard(card.id) { success, msg ->
                                                        if (success) {
                                                            Toast.makeText(context, "Đã xóa".translated(userProgress.appLanguage), Toast.LENGTH_SHORT).show()
                                                            managingCardsList = managingCardsList.filter { it.id != card.id }
                                                        } else {
                                                            Toast.makeText(context, "Lỗi".translated(userProgress.appLanguage) + ": $msg", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }) {
                                                    Icon(Icons.Default.Delete, contentDescription = "Xóa".translated(userProgress.appLanguage), tint = MaterialTheme.colorScheme.error)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Export Dialog
        if (showExportDialog) {
            Dialog(onDismissRequest = { showExportDialog = false }) {
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

                        // Format selection
                        var exportFormat by remember { mutableStateOf("json") }
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
                                    showExportDialog = false
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
                            onClick = { showExportDialog = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Hủy".translated(userProgress.appLanguage))
                        }
                    }
                }
            }
        }
    }
}
