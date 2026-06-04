# Hướng dẫn Kỹ thuật: Quá trình Triển khai AI Smart Tutor Agent - Luồng 1 (Personalized Word RAG)

Tài liệu này ghi nhận chi tiết toàn bộ quá trình nghiên cứu, thiết kế, triển khai và tối ưu hóa **Luồng 1: Tra cứu từ vựng cá nhân hóa (Personalized Word RAG)** trong dự án MinLish English Learning App. Đây là cẩm nang hướng dẫn kỹ thuật cốt lõi giúp bạn dễ dàng nắm bắt dòng chạy của code và sẵn sàng mở rộng các luồng 2, 3, 4 tiếp theo.

---

## 1. Mục tiêu Nghiệp vụ của Luồng 1
Khi học viên hỏi nghĩa của một từ vựng tiếng Anh bất kỳ (ví dụ: *"từ smartphone nghĩa là gì?"*):
* **Trường hợp từ ĐÃ CÓ trong kho từ vựng của học viên (RAG):** AI Tutor nhận diện chính xác từ này nằm trong bộ bài học (Deck) nào của học viên, **bắt buộc sử dụng 100% dữ liệu gốc trong Supabase** (loại từ, phiên âm, định nghĩa tiếng Anh, nghĩa tiếng Việt, cụm collocation, và **nguyên văn câu ví dụ tiếng Anh gốc**) để giảng bài. Đồng thời, LLM sẽ tự động phân tích sâu ví dụ và mở rộng kiến thức bổ trợ (Word Comparison, Memory Hack, Idioms thực tế) để bài học sinh động.
* **Trường hợp từ CHƯA CÓ trong kho từ vựng:** AI nhẹ nhàng thông báo từ chưa có và tự dùng trí tuệ của mình để biên soạn một bài giảng chuẩn mực và khích lệ học viên lưu thành thẻ Flashcard mới.
* **Quy tắc Active Recall:** Cuối mỗi câu trả lời, AI luôn thách đố học viên tự đặt một câu tiếng Anh để thực hành.

---

## 2. Kiến trúc Hệ thống: Máy Trạng thái ReAct Agent
Chúng ta đã chuyển dịch từ mô hình đồ thị tuyến tính cứng (Deterministic Flow) sang **Mô hình Agentic Tool Calling (ReAct Agent) chuẩn của LangGraph**. 

Trong mô hình này, **LLM đóng vai trò là bộ não điều phối duy nhất**, tự động phân tích câu hỏi của người học để quyết định khi nào cần gọi công cụ (tool) và tổng hợp thông tin suy luận đa bước.

### Sơ đồ luồng chạy (Execution Flow):
```text
  [User: "từ smartphone nghĩa là gì?"]
                  │
                  ▼
         ┌──────────────────┐
         │   tutor_agent    │ (Hàm tutor_agent_node chạy Lượt 1)
         └──────────────────┘
                  │
                  ├─► LLM nhận tin nhắn của User.
                  ├─► Tự động chuyển từ khóa về nguyên thể (smartphone).
                  └─► Sinh yêu cầu gọi tool: lookup_word_in_database(word="smartphone", user_id="...")
                  │
                  ▼ (Conditional Edge: should_continue)
         ┌──────────────────┐
         │ should_continue  │ ──► Phát hiện tool_calls
         └──────────────────┘     ==► Chuyển sang Node: "tools"
                  │
                  ▼
         ┌──────────────────┐
         │      tools       │ ──► ToolNode thực thi hàm Python kết nối Supabase.
         └──────────────────┘     ──► Trả về chuỗi JSON kết quả từ bảng cards và decks.
                  │               ──► ToolMessage được append tự động vào State.
                  │
                  ▼ (LangGraph Edge tự động quay đầu)
         ┌──────────────────┐
         │   tutor_agent    │ (Hàm tutor_agent_node chạy Lượt 2)
         └──────────────────┘
                  │
                  ├─► LLM đọc kết quả RAG từ database (nhất quán 100% nghĩa/ví dụ).
                  ├─► LLM suy luận mở rộng: Word Comparison, Memory Hack, Idioms thực tế.
                  └─► Sinh câu trả lời giảng bài chất lượng cao gửi cho người học.
                  │
                  ▼ (Conditional Edge: should_continue Lượt 2)
         ┌──────────────────┐
         │ should_continue  │ ──► Phát hiện tin nhắn text thường, không còn tool_calls.
         └──────────────────┘     ==► Kết thúc END!
```

---

## 3. Cấu trúc Thư mục Modular Clean Architecture
Dự án được tổ chức mô-đun hóa sạch sẽ nhằm phục vụ đắc lực cho việc mở rộng các luồng 2, 3, 4 tiếp theo:

```text
ai-service/
├── .env                  # Chứa biến môi trường kết nối Supabase & LLM API Key
├── pyproject.toml        # File cấu hình quản lý thư viện của uv
├── flow1_implementation.md # File tài liệu hướng dẫn kỹ thuật này
├── main.py               # File chạy CLI chính (Entry Point) với Logging Tracer
└── src/
    ├── __init__.py
    ├── config/           # Cấu hình dịch vụ hạ tầng
    │   ├── __init__.py
    │   ├── database.py   # Kết nối Supabase SDK
    │   └── llm.py        # Khởi tạo OpenAI-compatible LLM Singleton Instance
    │
    ├── tools/            # Các công cụ Python mà Agent có thể gọi tự động
    │   ├── __init__.py
    │   └── database_lookup.py  # Công cụ lookup_word_in_database (Luồng 1)
    │
    ├── prompts/          # Quản lý các prompt chỉ dẫn LLM
    │   ├── __init__.py
    │   └── system.py     # Persona Gia sư MinLish & Quy tắc giảng bài RAG
    │
    └── agent/            # Nhân đồ thị LangGraph
        ├── __init__.py
        ├── state.py      # Định nghĩa Schema trạng thái AgentState
        ├── tutor_agent.py # Node điều phối chính (Tutor Agent Node)
        └── graph.py      # Xây dựng đồ thị StateGraph & Biên dịch đồ thị
```

---

## 4. Chi tiết các Bước Triển khai & Tối ưu hóa

### Bước 1: Kết nối Supabase hạ tầng (`src/config/database.py`)
Khởi tạo Supabase client dùng chung duy nhất dựa trên `SUPABASE_URL` và `SUPABASE_ANON_KEY` được load từ `.env`.

### Bước 2: Thiết lập Singleton LLM Client (`src/config/llm.py`)
Khởi tạo đối tượng `ChatOpenAI` tương thích OpenAI làm **Singleton dùng chung toàn cục**. 
* **Tối ưu hóa hiệu năng:** Object chỉ được khởi tạo đúng 1 lần duy nhất trong bộ nhớ.
* **HTTP Keep-Alive:** HTTP Client bên dưới tự động kích hoạt tính năng **Connection Pooling**, giữ kết nối socket HTTPS luôn mở tới máy chủ API (như Groq). Cả 2 lần gọi LLM trong vòng lặp đều dùng chung một socket, giúp triệt tiêu hoàn toàn độ trễ thiết lập kết nối (handshake) mạng!

### Bước 3: Lập trình Tool RAG có bảo mật quyền riêng tư (`src/tools/database_lookup.py`)
Bọc logic bằng decorator `@tool` của LangChain để định nghĩa rõ ràng mô tả công cụ cho LLM học.
* **Lọc sở hữu chặt chẽ (Ownership Filter):** Truy vấn toàn bộ các thẻ từ khớp với từ khóa (`.ilike` không phân biệt hoa thường). Lặp qua từng thẻ từ và đối chiếu quyền sở hữu của bộ từ chứa nó: Chỉ chấp nhận thẻ từ đó nếu `user_id` của bộ từ là `NULL` (bộ từ mẫu hệ thống dành cho tất cả mọi người) HOẶC `user_id` trùng khớp với `user_id` của người học hiện tại. Nếu thuộc về người dùng khác, bỏ qua để bảo mật riêng tư tuyệt đối.
* **Phòng thủ tham số mặc định:** Gán giá trị mặc định cho `user_id` (`user_id: str = "00000000-0000-0000-0000-000000000000"`) đề phòng LLM nhỏ bị lỗi truyền khuyết thiếu đối số, tránh gây sập (crash) ứng dụng.

### Bước 4: Viết Prompt Hệ thống "Sắt đá" (`src/prompts/system.py`)
Định hình Persona Gia sư MinLish thân thiện, kiêu nhã nhưng cực kỳ nghiêm khắc về cấu trúc phản hồi:
* **Ép buộc 100% ví dụ từ DB:** Buộc AI phải sao chép nguyên văn câu ví dụ tiếng Anh trong database, dịch nghĩa mượt mà sang tiếng Việt và cấm tự chế ví dụ khác.
* **Loại bỏ sự lặp lại:** Bắt AI đi thẳng vào định dạng bài giảng có cấu trúc thay vì viết một đoạn giải nghĩa dài ở đầu rồi lại liệt kê gạch đầu dòng ở dưới.
* **Mở rộng tri thức LLM:** Thiết lập riêng mục **"💡 Gia sư phân tích sâu & mở rộng:"** để LLM tự do phân tích sắc thái, so sánh phân biệt từ vựng, chia sẻ mẹo nhớ siêu tốc và cụm từ giao tiếp thực tế.

### Bước 5: Viết Node điều phối Agent (`src/agent/tutor_agent.py`)
Lập trình hàm `tutor_agent_node`:
* Tiêm trực tiếp `user_id` và `target_goal` của người học hiện tại vào System Prompt để LLM tự động biết ID điền vào đối số khi gọi công cụ.
* **Lemmatization tại mức LLM:** Chỉ đạo nghiêm ngặt bắt LLM **phải tự động đưa từ vựng về dạng NGUYÊN THỂ (lemma) viết thường** trước khi gọi tool (ví dụ: `smartphones` -> `smartphone`, `scrutinized` -> `scrutinize`), giúp giải quyết triệt để lỗi so khớp RAG do số nhiều/chia thì trong database mà không tốn tài nguyên code Python.
* Thực hiện bind công cụ và invoke LLM.

### Bước 6: Liên kết Đồ thị & Streaming Tracer (`src/agent/graph.py` & `main.py`)
* Trong `graph.py`, chúng ta đăng ký node `"tutor_agent"` và node `"tools"` (`ToolNode`), kết nối chúng bằng conditional edge `should_continue` kiểm tra thuộc tính `tool_calls` và cho phép quay đầu `"tools" -> "tutor_agent"` để hoàn tất vòng lặp suy luận.
* Trong `main.py`, chúng ta nâng cấp cơ chế `.invoke()` cố định thành **`graph.stream(state, stream_mode="updates")`** thời gian thực. Mỗi khi một node thực thi xong, chương trình sẽ in ra logs trace chi tiết (AI quyết định gọi tool nào, tham số ra sao, kết quả RAG thô trả về từ Supabase thế nào và LLM nhận dữ liệu tổng hợp bài giảng cuối cùng ra sao) giúp gỡ lỗi và kiểm nghiệm cực kỳ trực quan.

---

## 5. Các Mẹo & Kinh nghiệm Kỹ thuật tích lũy
1. **Lỗi Groq Deprecated Model (400):** Khi kết nối Groq, hãy luôn sử dụng các mô hình đang hoạt động tích cực (active) như `llama-3.3-70b-versatile` hoặc `qwen/qwen3-32b` (qua API Provider), tránh dùng các mô hình đã bị khai tử như `llama-3.1-70b-versatile`.
2. **Tại sao nên giữ ChatOpenAI:** Giúp mã nguồn có tính linh hoạt tối đa. Khi chuyển đổi sang chạy mô hình local (như Ollama), bạn chỉ cần sửa duy nhất tệp cấu hình `.env` mà không phải code lại bất kỳ hàm nào.
3. **Cơ chế Lemmatization bằng Prompt:** Tận dụng triệt để trí tuệ của LLM để chuẩn hóa từ vựng về nguyên thể thay vì viết code Python phức tạp, vừa nhẹ nhàng vừa đạt độ chính xác cực cao.
