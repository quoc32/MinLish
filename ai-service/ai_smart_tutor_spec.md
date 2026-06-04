# TÀI LIỆU ĐẶC TẢ KỸ THUẬT: AI SMART TUTOR AGENT
**Dự án**: MinLish English Learning App
**Tác giả**: MinLish Development Team & AI Solutions Architect
**Phiên bản**: v1.0.0 (Bản đặc tả thiết kế kỹ thuật)

---

## 1. TỔNG QUAN TÍNH NĂNG (PRODUCT OVERVIEW)
**AI Smart Tutor Agent** là một hệ sinh thái gia sư ảo tiếng Anh thông minh, hoạt động theo mô hình hội thoại có trạng thái (**Stateful Conversational Agent**). Khác biệt hoàn toàn với các chatbot tra cứu tĩnh thông thường, Gia sư AI của MinLish thấu hiểu sâu sắc tiến trình học tập cá nhân của từng học viên, chủ động đưa ra thử thách kiểm tra, sửa lỗi ngữ pháp thời gian thực, đồng thời tự động hóa hoàn toàn quy trình biên soạn và nạp thẻ từ vựng mới (Flashcards) từ tài liệu thực tế của người dùng.

### Các giá trị cốt lõi mang lại:
* **Cá nhân hóa sâu sắc (Supabase RAG)**: Nhận biết rõ từ vựng người dùng hỏi đã có trong kho dữ liệu của họ chưa, tiến trình học của từ đó thế nào để đưa ra phản hồi phù hợp nhất.
* **Học tập chủ động (Active Recall)**: AI đóng vai trò dẫn dắt, luôn thách đố học viên đặt câu thực hành và chấm điểm câu viết thử.
* **Tự động hóa nạp thẻ (Zero-effort Ingestion)**: Trích xuất và soạn thảo từ vựng học thuật đỉnh cao từ bất kỳ đoạn văn bản thô nào người dùng dán vào, lưu trữ trực tiếp chỉ qua một nút xác nhận.

---

## 2. KIẾN TRÚC HỆ THỐNG MICROSERVICES
Hệ thống được tổ chức theo mô hình **3 lớp Microservices** độc lập nhằm phân tách tải trọng (tách biệt tải giao dịch CRUD di động và tải tính toán suy luận LLM):

```mermaid
graph TD
    subgraph Mobile Client [Lớp 1: Di động]
        A[Android App - Kotlin/Compose]
    end

    subgraph Core Backend [Lớp 2: Express Gateway]
        B[Node.js / Express Server]
    end

    subgraph AI Service [Lớp 3: Trí tuệ Nhân tạo]
        C[FastAPI / Python Server]
        D[LangGraph State Engine]
        E[LangChain LLM Interface]
    end

    subgraph Storage [Lớp 4: Cơ sở Dữ liệu]
        F[(Supabase Database)]
    end

    A <=>|HTTP / WebSockets| B
    B <=>|HTTP Client + JWT Auth| C
    C <=>|State Management| D
    D <=>|LangChain LCEL| E
    C <=>|Supabase Python SDK| F
    B <=>|PostgreSQL REST API| F
```

### Chi tiết các lớp:
1. **Lớp di động (Android Client)**: Cung cấp giao diện chat mượt mà, gửi tin nhắn, hiển thị bong bóng chat dạng Markdown, và hiển thị các nút phản hồi nhanh (Quick Reply Chips) như `[Lưu ngay! 👍]`, `[Để sau 👎]` để tăng trải nghiệm.
2. **Lớp API Gateway (Node.js/Express)**: Đóng vai trò là cổng xác thực. Nhận tin nhắn chat từ App, giải mã JWT token của người dùng, lấy ra `userId` và `targetGoal` (mục tiêu học hiện tại như IELTS, TOEIC...), sau đó đóng gói và proxy yêu cầu sang AI Server.
3. **Lớp dịch vụ AI (FastAPI/Python)**: Tiếp nhận yêu cầu từ Gateway, khởi tạo trạng thái máy LangGraph tương ứng với lịch sử chat của `userId` đó. Server này trực tiếp tương tác với cơ sở dữ liệu Supabase thông qua thư viện Supabase Python SDK để thực hiện các tác vụ tra cứu chéo (RAG) hoặc ghi dữ liệu (Tool Calling).

---

## 3. ĐẶC TẢ 4 LUỒNG HỘI THOẠI CỐT LÕI

Hệ thống sử dụng một **Intent Router Node** (được tối ưu hóa bằng mô hình ngôn ngữ lớn) ở đầu vào để phân tích tin nhắn của người dùng và rẽ nhánh thông minh vào 1 trong 4 luồng nghiệp vụ sau:

### Luồng 1: Hỏi đáp từ vựng cá nhân hóa (Personalized Word RAG)
* **Mục tiêu**: Giải nghĩa từ vựng tiếng Anh theo yêu cầu của học viên một cách sinh động, cá nhân hóa theo tiến trình thực tế của họ.
* **Sơ đồ luồng xử lý**:
  1. Học viên gửi câu hỏi: *"Từ 'Ephemeral' nghĩa là gì?"*.
  2. Agent kích hoạt Tool `check_word_in_user_database` trên Supabase:
     * **Nếu từ ĐÃ CÓ trong kho từ của học viên**: AI phản hồi kèm theo ngữ cảnh đầy đủ: *"A! Từ 'Ephemeral' này chính là từ nằm trong bộ bài học 'Academic Essentials' của bạn đấy! Tiến độ học từ này của bạn đang đạt 40%. Nghĩa của nó là..."*
     * **Nếu từ CHƯA CÓ trong kho từ**: AI tra cứu kiến thức của LLM, giải nghĩa chi tiết (phiên âm, định nghĩa Anh - Việt, ví dụ trực quan).
  3. AI lưu trạng thái từ vựng vào `current_word` và chuyển sang **Luồng 3** (Thử thách viết câu).

### Luồng 2: Trích xuất đoạn văn tạo thẻ tự động (Text Flashcard Extractor)
* **Mục tiêu**: Người dùng đọc báo, tài liệu, sách ngoại văn... có thể dán đoạn văn bản thô vào chat để AI tự động chọn lọc từ vựng chất lượng và lưu trữ làm Flashcard chỉ trong 1 giây.
* **Sơ đồ luồng xử lý**:
  1. Học viên gửi yêu cầu kèm văn bản: *"Tạo thẻ từ đoạn văn này giúp mình: [Dán 1 đoạn văn tiếng Anh]"*.
  2. Agent chuyển đổi yêu cầu vào **AI Vocabulary Extractor Node**. Sử dụng mô hình trích xuất định dạng có cấu trúc (**Structured Output**) để chọn ra **3 - 5 từ khóa đắt giá nhất** phù hợp nhất với mục tiêu học của học viên (IELTS/TOEIC/...).
  3. Với mỗi từ, AI tự động biên soạn: Từ vựng, Phiên âm, Nghĩa tiếng Việt theo ngữ cảnh đoạn văn, Giải nghĩa tiếng Anh, Câu ví dụ thực tế.
  4. AI hiển thị danh sách từ đã trích xuất dưới dạng xem trước và hiển thị gợi ý: *"Bạn có muốn lưu các từ này thành thẻ Flashcards cá nhân không?"* kèm các nút lựa chọn nhanh.
  5. Học viên bấm chọn *"Có"*, AI Agent ngầm thực thi vòng lặp gọi Tool `create_new_card` để ghi thẳng dữ liệu vào Supabase.

### Luồng 3: Sửa lỗi ngữ pháp & Chấm điểm đặt câu (Real-time Grammar Examiner)
* **Mục tiêu**: Rèn luyện kỹ năng viết câu sử dụng từ mới và sửa lỗi chi tiết thời gian thực.
* **Sơ đồ luồng xử lý**:
  1. Sau khi giải nghĩa từ ở **Luồng 1**, AI Tutor thách đố học viên: *"Bây giờ, bạn hãy thử viết một câu tiếng Anh với từ này nhé!"*.
  2. Học viên nhập câu đặt thử.
  3. Agent chuyển tin nhắn vào **Grammar Examiner Node**:
     * Phân tích cú pháp câu viết của học viên.
     * Chỉ ra lỗi sai chi tiết (ví dụ: chia sai động từ, dùng sai giới từ) và sửa lại thành một câu chuẩn tự nhiên nhất.
     * Chấm điểm câu viết trên thang điểm 10.
  4. Nếu từ vựng đang thảo luận là từ mới hoàn toàn (chưa có trong database của học viên ở Luồng 1), AI sẽ kích hoạt gợi ý thông minh chuyển tiếp sang **Luồng 2**: *"Bạn đặt câu rất tốt! Bạn có muốn mình lưu từ này làm Flashcard cá nhân không?"*.

### Luồng 4: Chat tự do & Lọc nội dung lạc đề (General & Off-topic Fallback)
* **Mục tiêu**: Đáp ứng các câu hỏi kiến thức tiếng Anh chung và bảo vệ phạm vi hoạt động chuyên nghiệp của ứng dụng.
* **Sơ đồ luồng xử lý**:
  * **Trường hợp hỏi tiếng Anh/Ngữ pháp chung**: (Ví dụ: *"Làm sao phân biệt say, tell, speak, talk?"*). AI rẽ nhánh vào **General Vocab Q&A Node**, dùng tri thức hệ thống để phân tích, kẻ bảng so sánh trực quan dưới dạng Markdown và đưa ra bài tập nhỏ thực hành.
  * **Trường hợp lạc đề hoàn toàn (Off-topic)**: (Ví dụ: *"Code giúp tôi trang web bằng HTML"*, *"Đá bóng tối nay ai thắng?"*). AI rẽ nhánh vào **Off-topic Fallback Node**, phản hồi lịch sự để từ chối và hướng học viên về mục tiêu học tập: *"Mình là Gia sư tiếng Anh MinLish, mình chỉ có thể giúp bạn học từ vựng, ngữ pháp và luyện đặt câu thôi. Bạn có từ vựng nào cần hỏi không nè?"*.

---

## 4. THIẾT KẾ MÁY TRẠNG THÁI HỘI THOẠI (LANGGRAPH STATE MACHINE)

Kiến trúc máy trạng thái của LangGraph được định nghĩa bằng một tập hợp các Node (Hành động) và Edge (Đường chuyển dịch) dựa trên một bộ lưu trữ trạng thái thống nhất (`AgentState`):

```python
from typing import TypedDict, List, Optional
from pydantic import BaseModel, Field

# 1. Định nghĩa cấu trúc dữ liệu trích xuất từ vựng (cho Luồng 2)
class ExtractedWord(BaseModel):
    word: str = Field(description="Từ tiếng Anh gốc (dạng nguyên thể)")
    pronunciation: str = Field(description="Phiên âm UK chuẩn")
    meaning: str = Field(description="Nghĩa tiếng Việt chuẩn theo ngữ cảnh đoạn văn")
    description_en: str = Field(description="Định nghĩa tiếng Anh đơn giản, dễ hiểu")
    example: str = Field(description="Câu ví dụ thực tế có sử dụng từ này")

class ExtractionResult(BaseModel):
    words: List[ExtractedWord] = Field(description="Danh sách từ vựng trích xuất (tối đa 3-5 từ)")

# 2. Định nghĩa AgentState quản lý luồng hội thoại
class AgentState(TypedDict):
    user_id: str                          # ID người dùng để định danh Supabase
    target_goal: str                      # Mục tiêu học hiện tại (IELTS/TOEIC/Giao tiếp/THPT)
    chat_history: List[dict]              # Lịch sử hội thoại (giữ ngữ cảnh chat)
    current_word: Optional[str]            # Từ vựng đang thảo luận trong luồng 1 & 3
    is_in_database: Optional[bool]         # Trạng thái từ đã tồn tại trong DB của user chưa
    user_sentence: Optional[str]          # Câu do học viên tự đặt thử để AI chấm điểm
    extracted_words: List[ExtractedWord]  # Lưu danh sách từ trích xuất từ đoạn văn ở luồng 2
```

---

## 5. THIẾT KẾ KẾT NỐI CƠ SỞ DỮ LIỆU (SUPABASE INTEGRATION)

AI Agent sử dụng các câu lệnh Supabase Python SDK để trực tiếp truy vấn và tương tác với cơ sở dữ liệu MinLish:

### A. Kiểm tra từ vựng tồn tại (RAG Tool - Luồng 1)
Truy vấn bảng `cards` để tìm thẻ từ trùng khớp, đồng thời kiểm tra sự liên kết với bảng `decks` để biết từ đó thuộc bài học nào:
```python
# Gọi từ AI Server để kiểm tra xem từ vựng đã có trong decks của hệ thống hoặc cá nhân chưa
response = supabase.table("cards") \
    .select("id, word, meaning, deck_id, decks(name, target_goal, user_id)") \
    .eq("word", word.lower()) \
    .execute()
```

### B. Tạo thẻ từ vựng mới tự động (Tool Call - Luồng 2 & 3)
Chèn trực tiếp từ mới đã được AI định dạng chuẩn vào bảng `cards` của người dùng:
```python
# Chèn thẻ từ vựng cá nhân mới
new_card = {
    "deck_id": target_deck_id,
    "word": word.strip(),
    "pronunciation": pronunciation.strip(),
    "meaning": meaning.strip(),
    "description_en": description_en.strip(),
    "example": example.strip()
}
response = supabase.table("cards").insert(new_card).execute()
```

---

## 6. GỢI Ý GIAO DIỆN DI ĐỘNG (ANDROID KOTLIN / JETPACK COMPOSE)

Để tối ưu hóa trải nghiệm tương tác với Gia sư AI, màn hình **AI Tutor Screen** trên Android nên được thiết kế như sau:

1. **Khung Chat Bong Bóng (Message Bubble List)**:
   * Render nội dung tin nhắn của AI dưới dạng **Markdown** (nhờ thư viện Jetpack Compose Markdown) để hiển thị bảng biểu so sánh ngữ pháp, bôi đậm từ vựng và phiên âm rõ nét.
2. **Action Chips nổi thông minh (Contextual Quick Replies)**:
   * Khi AI Agent hỏi xác nhận lưu từ, sử dụng trạng thái API trả về để hiển thị các nút bấm tròn bo góc sinh động ngay trên khu vực nhập liệu:
     ```kotlin
     @Composable
     fun QuickReplySection(
         showOptions: Boolean,
         onConfirm: () -> Unit,
         onDismiss: () -> Unit
     ) {
         if (showOptions) {
             Row(
                 modifier = Modifier.fillMaxWidth().padding(8.dp),
                 horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
             ) {
                 Button(
                     onClick = onConfirm,
                     colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)) // Xanh lá
                 ) {
                     Text("Có, lưu ngay! 👍", fontWeight = FontWeight.Bold)
                 }
                 OutlinedButton(
                     onClick = onDismiss
                 ) {
                     Text("Để sau 👎")
                 }
             }
         }
     }
     ```

---

Bản tài liệu đặc tả kỹ thuật này đã phác thảo hoàn hảo từ cấu trúc hệ thống, logic trạng thái LangGraph, luồng cơ sở dữ liệu đến thiết kế giao diện di động. Đây sẽ là cẩm nang hướng dẫn tuyệt đối chính xác cho bạn khi bắt tay vào xây dựng tính năng AI đột phá này cho MinLish App!
