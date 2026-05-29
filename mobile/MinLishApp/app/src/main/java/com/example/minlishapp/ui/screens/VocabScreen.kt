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
import com.example.minlishapp.data.Deck
import com.example.minlishapp.data.UserProgress

@Composable
fun VocabScreen(
    decks: List<Deck>,
    onAddDeck: (Deck) -> Unit,
    onNavigate: (Screen) -> Unit,
    userProgress: UserProgress,
    activeDeck: Deck?,
    onActiveDeckSelect: (Deck) -> Unit,
    onAddWordToDeck: (String, com.example.minlishapp.data.Word) -> Unit,
    onStartStudy: (Deck) -> Unit = {}
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var newDeckName by remember { mutableStateOf("") }
    var newDeckDesc by remember { mutableStateOf("") }
    var newDeckTags by remember { mutableStateOf("Cá nhân") }
    
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, In Progress, Mastered

    // States for adding vocabulary word
    var showAddWordDialog by remember { mutableStateOf(false) }
    var selectedDeckForAddingWord by remember { mutableStateOf<Deck?>(null) }

    // States for Import/Export
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedImportFile by remember { mutableStateOf<String?>(null) }
    
    var newWordText by remember { mutableStateOf("") }
    var newWordPron by remember { mutableStateOf("") }
    var newWordMeaning by remember { mutableStateOf("") }
    var newWordDesc by remember { mutableStateOf("") }
    var newWordExample by remember { mutableStateOf("") }
    var newWordExampleTrans by remember { mutableStateOf("") }
    var newWordCollocations by remember { mutableStateOf("") }
    var newWordSynonyms by remember { mutableStateOf("") }
    var newWordType by remember { mutableStateOf("noun") }

    val filteredDecks = decks.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true)
    }.filter {
        when (selectedFilter) {
            "In Progress" -> it.progress > 0f && it.progress < 1f
            "Mastered" -> it.progress >= 1f
            else -> true
        }
    }

    Scaffold(
        bottomBar = { AppBottomBar(currentScreen = Screen.VocabDecks, onNavigate = onNavigate) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm bộ từ")
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
                    text = "Bộ Từ Vựng theo Lộ trình",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { showImportDialog = true }) {
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
                placeholder = { Text("Tìm kiếm chủ đề từ vựng...") },
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
                            text = label,
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
            if (filteredDecks.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không tìm thấy bộ từ vựng nào.",
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

                                    // Title
                                    Text(
                                        text = deck.name,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Subtitle
                                    Text(
                                        text = "${deck.wordCount} từ vựng",
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
                                                text = "+ Từ mới",
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
                                                    text = "Vào học",
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
                            text = "Tạo Bộ Từ Vựng Mới",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = newDeckName,
                            onValueChange = { newDeckName = it },
                            label = { Text("Tên bộ từ") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = newDeckDesc,
                            onValueChange = { newDeckDesc = it },
                            label = { Text("Mô tả") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )

                        OutlinedTextField(
                            value = newDeckTags,
                            onValueChange = { newDeckTags = it },
                            label = { Text("Nhãn / Tags (Phân cách bằng dấu phẩy)") },
                            placeholder = { Text("Ví dụ: IELTS, Giao tiếp") },
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
                                Text("Hủy")
                            }

                            Button(
                                onClick = {
                                    if (newDeckName.isNotBlank()) {
                                        onAddDeck(
                                            Deck(
                                                id = System.currentTimeMillis().toString(),
                                                name = newDeckName,
                                                description = newDeckDesc,
                                                tags = newDeckTags.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                                words = emptyList()
                                            )
                                        )
                                        newDeckName = ""
                                        newDeckDesc = ""
                                        newDeckTags = "Cá nhân"
                                        showDialog = false
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Lưu")
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
                                text = "Thêm Từ Vựng Vào Bộ",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Bộ từ: ${selectedDeckForAddingWord?.name}",
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
                                label = { Text("Từ tiếng Anh (*)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 2. Loại từ (adjective, noun, verb...)
                            Text(text = "Loại từ:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                                            text = label,
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
                                label = { Text("Phiên âm UK (*)") },
                                placeholder = { Text("Ví dụ: /ə'kɒmədeɪt/") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 4. Nghĩa tiếng Việt
                            OutlinedTextField(
                                value = newWordMeaning,
                                onValueChange = { newWordMeaning = it },
                                label = { Text("Nghĩa tiếng Việt (*)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 5. Câu ví dụ tiếng Anh
                            OutlinedTextField(
                                value = newWordExample,
                                onValueChange = { newWordExample = it },
                                label = { Text("Câu ví dụ tiếng Anh") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 6. Dịch nghĩa câu ví dụ
                            OutlinedTextField(
                                value = newWordExampleTrans,
                                onValueChange = { newWordExampleTrans = it },
                                label = { Text("Dịch nghĩa câu ví dụ") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 7. Collocations
                            OutlinedTextField(
                                value = newWordCollocations,
                                onValueChange = { newWordCollocations = it },
                                label = { Text("Collocations (Phân cách bằng dấu phẩy)") },
                                placeholder = { Text("Ví dụ: accommodate guests, accommodate needs") },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // 8. Từ đồng nghĩa
                            OutlinedTextField(
                                value = newWordSynonyms,
                                onValueChange = { newWordSynonyms = it },
                                label = { Text("Từ đồng nghĩa (Phân cách bằng dấu phẩy)") },
                                placeholder = { Text("Ví dụ: hold, contain") },
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
                                Text("Hủy")
                            }

                            Button(
                                onClick = {
                                    if (newWordText.isNotBlank() && newWordMeaning.isNotBlank() && newWordPron.isNotBlank()) {
                                        val newWord = com.example.minlishapp.data.Word(
                                            word = newWordText.trim(),
                                            pronunciation = newWordPron.trim(),
                                            meaning = newWordMeaning.trim(),
                                            example = newWordExample.trim(),
                                            exampleTranslation = newWordExampleTrans.trim(),
                                            wordType = newWordType,
                                            collocations = newWordCollocations.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                                            synonyms = newWordSynonyms.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                                        )
                                        
                                        onAddWordToDeck(selectedDeckForAddingWord!!.id, newWord)
                                        
                                        // Reset fields
                                        newWordText = ""
                                        newWordPron = ""
                                        newWordMeaning = ""
                                        newWordExample = ""
                                        newWordExampleTrans = ""
                                        newWordCollocations = ""
                                        newWordSynonyms = ""
                                        newWordType = "noun"
                                        
                                        showAddWordDialog = false
                                    } else {
                                        Toast.makeText(context, "Vui lòng điền đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Lưu")
                            }
                        }
                    }
                }
            }
        }

        // Import Dialog
        if (showImportDialog) {
            Dialog(onDismissRequest = { 
                showImportDialog = false
                selectedImportFile = null
            }) {
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
                            text = "Nhập Từ Vựng (Giả Lập)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (selectedImportFile == null) {
                            Text(
                                text = "Chọn file dữ liệu để bắt đầu nhập từ vựng:",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            val files = listOf(
                                "toeic_conferences_vocab.csv" to "Hội nghị & Sự kiện (3 từ)",
                                "ielts_academic_essentials.csv" to "Học thuật Cốt lõi (2 từ)",
                                "travel_survival_kit.xlsx" to "Du lịch Cơ bản (2 từ)"
                            )

                            files.forEach { (filename, desc) ->
                                Card(
                                    onClick = { selectedImportFile = filename },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "📄", fontSize = 20.sp)
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = filename,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = desc,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Chọn bộ từ muốn nạp từ từ file:\n\"${selectedImportFile}\"",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )

                            decks.forEach { deck ->
                                Card(
                                    onClick = {
                                        val wordsToImport = when (selectedImportFile) {
                                            "toeic_conferences_vocab.csv" -> listOf(
                                                com.example.minlishapp.data.Word(
                                                    word = "reception",
                                                    wordType = "noun",
                                                    pronunciation = "/rɪˈsep.ʃən/",
                                                    meaning = "tiệc chiêu đãi, sự đón nhận",
                                                    example = "\"A welcome reception will be held on the first evening.\"",
                                                    exampleTranslation = "(Một bữa tiệc chiêu đãi chào mừng sẽ được tổ chức vào tối đầu tiên.)",
                                                    collocations = listOf("wedding reception: tiệc cưới", "warm reception: sự đón tiếp nồng hậu"),
                                                    synonyms = listOf("welcome", "gathering")
                                                ),
                                                com.example.minlishapp.data.Word(
                                                    word = "negotiate",
                                                    wordType = "verb",
                                                    pronunciation = "/nəˈɡəʊ.ʃi.eɪt/",
                                                    meaning = "đàm phán, thương lượng",
                                                    example = "\"They managed to negotiate a friendly settlement.\"",
                                                    exampleTranslation = "(Họ đã cố gắng thương lượng một thỏa thuận thân thiện.)",
                                                    collocations = listOf("negotiate a deal: đàm phán hợp đồng", "negotiate terms: đàm phán điều khoản"),
                                                    synonyms = listOf("discuss", "bargain")
                                                 ),
                                                 com.example.minlishapp.data.Word(
                                                     word = "executive",
                                                     wordType = "noun",
                                                     pronunciation = "/ɪɡˈzek.jə.tɪv/",
                                                     meaning = "giám đốc điều hành",
                                                     example = "\"The executive committee will meet tomorrow morning.\"",
                                                     exampleTranslation = "(Ban điều hành sẽ họp vào sáng mai.)",
                                                     collocations = listOf("chief executive: giám đốc điều hành"),
                                                     synonyms = listOf("manager", "director")
                                                 )
                                             )
                                             "ielts_academic_essentials.csv" -> listOf(
                                                 com.example.minlishapp.data.Word(
                                                     word = "advocate",
                                                     wordType = "verb",
                                                     pronunciation = "/ˈæd.və.keɪt/",
                                                     meaning = "tán thành, ủng hộ, biện hộ",
                                                     example = "\"Some politicians advocate reforming the educational system.\"",
                                                     exampleTranslation = "(Một số chính trị gia ủng hộ việc cải cách hệ thống giáo dục.)",
                                                     collocations = listOf("strongly advocate: mạnh mẽ ủng hộ"),
                                                     synonyms = listOf("support", "champion")
                                                 ),
                                                 com.example.minlishapp.data.Word(
                                                     word = "mitigate",
                                                     wordType = "verb",
                                                     pronunciation = "/ˈmɪt.ɪ.ɡeɪt/",
                                                     meaning = "giảm thiểu, làm dịu bớt",
                                                     example = "\"Soil erosion was mitigated by the planting of trees.\"",
                                                     exampleTranslation = "(Sự xói mòn đất đã được giảm thiểu bằng việc trồng cây.)",
                                                     collocations = listOf("mitigate risk: giảm thiểu rủi ro"),
                                                     synonyms = listOf("alleviate", "reduce")
                                                 )
                                             )
                                             else -> listOf(
                                                 com.example.minlishapp.data.Word(
                                                     word = "souvenir",
                                                     wordType = "noun",
                                                     pronunciation = "/ˌsuː.vənˈɪər/",
                                                     meaning = "quà lưu niệm",
                                                     example = "\"I bought a model of the Eiffel Tower as a souvenir.\"",
                                                     exampleTranslation = "(Tôi mua một mô hình tháp Eiffel làm quà lưu niệm.)",
                                                     collocations = listOf("souvenir shop: cửa hàng quà lưu niệm"),
                                                     synonyms = listOf("keepsake", "memento")
                                                 ),
                                                 com.example.minlishapp.data.Word(
                                                     word = "embark",
                                                     wordType = "verb",
                                                     pronunciation = "/ɪmˈbɑːk/",
                                                     meaning = "lên tàu, bắt đầu hành trình",
                                                     example = "\"The passengers were waiting to embark on the cruise ship.\"",
                                                     exampleTranslation = "(Các hành khách đang chờ để lên tàu du lịch.)",
                                                     collocations = listOf("embark on a journey: bắt đầu hành trình"),
                                                     synonyms = listOf("start", "board")
                                                 )
                                             )
                                         }

                                         wordsToImport.forEach { word ->
                                             onAddWordToDeck(deck.id, word)
                                         }

                                         Toast.makeText(
                                             context,
                                             "Đã nạp ${wordsToImport.size} từ vào bộ \"${deck.name}\" thành công!",
                                             Toast.LENGTH_SHORT
                                         ).show()

                                         showImportDialog = false
                                         selectedImportFile = null
                                     },
                                     colors = CardDefaults.cardColors(
                                         containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                     ),
                                     modifier = Modifier.fillMaxWidth()
                                 ) {
                                     Text(
                                         text = deck.name,
                                         fontWeight = FontWeight.Bold,
                                         fontSize = 14.sp,
                                         modifier = Modifier.padding(14.dp)
                                     )
                                 }
                             }
                         }

                         Row(
                             modifier = Modifier.fillMaxWidth(),
                             horizontalArrangement = Arrangement.spacedBy(12.dp)
                         ) {
                             if (selectedImportFile != null) {
                                 OutlinedButton(
                                     onClick = { selectedImportFile = null },
                                     modifier = Modifier.weight(1f),
                                     shape = RoundedCornerShape(10.dp)
                                 ) {
                                     Text("Quay lại")
                                 }
                             }

                             Button(
                                 onClick = { 
                                     showImportDialog = false 
                                     selectedImportFile = null
                                 },
                                 modifier = Modifier.weight(1f),
                                 shape = RoundedCornerShape(10.dp)
                             ) {
                                 Text("Hủy")
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
                             text = "Xuất Bộ Từ Vựng",
                             fontWeight = FontWeight.Bold,
                             fontSize = 18.sp,
                             textAlign = TextAlign.Center,
                             modifier = Modifier.fillMaxWidth()
                         )

                         Text(
                             text = "Chọn bộ từ bạn muốn xuất ra file CSV:",
                             fontSize = 13.sp,
                             color = MaterialTheme.colorScheme.onSurfaceVariant
                         )

                         decks.forEach { deck ->
                             Card(
                                 onClick = {
                                     Toast.makeText(
                                         context,
                                         "Đã xuất bộ từ \"${deck.name}\" ra file CSV thành công!",
                                         Toast.LENGTH_LONG
                                     ).show()
                                     showExportDialog = false
                                 },
                                 colors = CardDefaults.cardColors(
                                     containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                 ),
                                 modifier = Modifier.fillMaxWidth()
                             ) {
                                 Row(
                                     modifier = Modifier
                                         .fillMaxWidth()
                                         .padding(14.dp),
                                     horizontalArrangement = Arrangement.SpaceBetween,
                                     verticalAlignment = Alignment.CenterVertically
                                 ) {
                                     Column {
                                         Text(
                                             text = deck.name,
                                             fontWeight = FontWeight.Bold,
                                             fontSize = 14.sp
                                         )
                                         Text(
                                             text = "${deck.words.size} từ vựng",
                                             fontSize = 12.sp,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant
                                         )
                                     }
                                     Text(text = "📥", fontSize = 18.sp)
                                 }
                             }
                         }

                         Button(
                             onClick = { showExportDialog = false },
                             modifier = Modifier.fillMaxWidth(),
                             shape = RoundedCornerShape(10.dp)
                         ) {
                             Text("Hủy")
                         }
                     }
                 }
             }
         }
     }
 }
