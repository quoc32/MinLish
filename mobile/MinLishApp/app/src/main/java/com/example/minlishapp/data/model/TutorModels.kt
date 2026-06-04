package com.example.minlishapp.data

// Request body gửi đến POST /api/tutor/chat
data class TutorChatRequest(
    val user_id: String,
    val message: String,
    val target_goal: String = "IELTS"
)

// Response body nhận về từ POST /api/tutor/chat
data class TutorChatResponse(
    val success: Boolean,
    val response: String        // Nội dung Markdown phản hồi của AI
)

// Request body gửi đến POST /api/tutor/reset
data class TutorResetRequest(
    val user_id: String
)

// Response body nhận về từ POST /api/tutor/reset
data class TutorResetResponse(
    val success: Boolean,
    val message: String
)

// Data class đại diện cho một tin nhắn trong cuộc hội thoại (dùng cho UI)
data class ChatMessage(
    val id: String,           // UUID unique cho mỗi tin nhắn
    val content: String,      // Nội dung tin nhắn
    val isFromUser: Boolean,  // true = tin nhắn của người dùng, false = tin nhắn của AI
    val timestamp: Long = System.currentTimeMillis()
)
