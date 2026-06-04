# Báo cáo Kết quả Sửa lỗi & Tối ưu hóa Gia sư AI MinLish

Tài liệu này tổng hợp toàn bộ các vấn đề đã được phát hiện, phân tích nguyên nhân và khắc phục thành công trong buổi làm việc hôm nay cho dự án Gia sư AI MinLish.

---

## 🛠️ Danh sách các lỗi đã khắc phục

### 1. Lỗi Crash Supabase: `'list' object has no attribute 'strip'`
*   **Triệu chứng:** Khi học viên đồng ý lưu từ vựng, công cụ `create_flashcards_in_database` gặp lỗi crash hệ thống: `LỖI: Gặp sự cố khi ghi dữ liệu vào Supabase: 'list' object has no attribute 'strip'`.
*   **Nguyên nhân:** Mô hình LLM (như `llama-3.3-70b-versatile`) đôi khi trả về trường `related_words` (các từ liên quan) dưới dạng một mảng (JSON array) thay vì chuỗi trần. Khi code Python thực hiện ép kiểu và gọi `.strip()` trực tiếp trên đối tượng list này đã gây lỗi runtime.
*   **Giải pháp:** 
    *   Tạo thêm hàm tiện ích `clean_val(val) -> str` bên trong file [create_flashcards.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/tools/create_flashcards.py) để chuẩn hóa an toàn mọi giá trị đầu vào.
    *   Nếu giá trị nhận được là một `list` (ví dụ: `['sleep', 'awake']`), hàm sẽ tự động nối các phần tử thành chuỗi phân tách bằng dấu phẩy trước khi gọi `.strip()` và ghi vào database.

---

### 2. Lỗi Lưu sai từ: Lưu từ `wake` thay vì `morning`
*   **Triệu chứng:** Khi người dùng học từ `morning` -> đặt câu tập luyện *"I wake up early every morning"* -> chọn lưu từ vựng, hệ thống lại tiến hành tạo flashcard cho từ `wake` thay vì `morning`.
*   **Nguyên nhân (Mất ngữ cảnh):** 
    *   Các node nghiệp vụ (`word_saver`, `grammar_examiner`, `tutor_agent`, `text_extractor`) ban đầu được cấu hình cắt lát lịch sử chat rất ngắn, chỉ lấy **5 tin nhắn gần nhất** (`messages[-5:]`) để tránh tràn token.
    *   Tuy nhiên, quy trình gọi công cụ ngầm (như `get_user_decks`) phát sinh thêm 2 tin nhắn hội thoại (tin nhắn yêu cầu gọi tool của AI và tin nhắn trả về kết quả của Tool). Việc này đẩy tin nhắn giảng bài giải nghĩa từ mục tiêu (`morning`) của Tutor Agent lùi sâu xuống quá 5 tin nhắn và bị cắt mất.
    *   Khi node `word_saver` chạy, nó hoàn toàn không thấy thông tin giải nghĩa của `morning` đâu trong lịch sử chat để trích xuất 9 trường thông tin, dẫn tới việc tự suy luận sai lệch từ câu đặt thử của người dùng và lưu nhầm từ `wake`.
*   **Giải pháp:** 
    *   Nâng giới hạn tin nhắn lịch sử gửi lên LLM ở cả 4 node nghiệp vụ nói trên lên **15 tin nhắn** (`messages[-15:]`). 
    *   Việc này giúp giữ nguyên vẹn toàn bộ bài giảng định nghĩa từ ban đầu, đảm bảo LLM luôn trích xuất chính xác 1:1 các trường thông tin của từ cần lưu.

---

### 3. Lỗi Nhận diện nhầm từ đã tồn tại: Trường hợp từ `language`
*   **Triệu chứng:** Khi người dùng yêu cầu `"lưu từ language vào flashcard đi"` khi từ này hoàn toàn chưa có trong cơ sở dữ liệu, AI vẫn phản hồi: *"Từ language đã có sẵn ở bộ 'Từ vựng cá nhân' của bạn rồi nè (quá tốt luôn! 🌟)"* nhưng ngay dưới đó lại hiển thị thông tin xem trước và hỏi có muốn lưu không.
*   **Nguyên nhân (Lộn tool output):** 
    *   Tại node `word_saver`, LLM gọi song song 2 công cụ: `lookup_word_in_database` (trả về `{"found": false}`) và `get_user_decks` (trả về danh sách các bộ từ dạng `{"found": true, "decks": [...]}`).
    *   Do cả 2 công cụ đều có trường dữ liệu `"found"` trong kết quả trả về, mô hình LLM đã nhầm lẫn lấy khóa `"found": true` của danh sách decks để áp dụng cho kết quả tra cứu sự tồn tại của từ.
*   **Giải pháp:** 
    *   Bổ sung chỉ dẫn chi tiết và cảnh báo nghiêm ngặt vào file prompt [word_saver_prompt.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/prompts/word_saver_prompt.py).
    *   Yêu cầu LLM bắt buộc phân loại dựa trên kết quả chính xác của công cụ `lookup_word_in_database` để biết từ đó đã có hay chưa; cấm tuyệt đối việc nhầm lẫn với kết quả `"found"` của `get_user_decks`.

---

### 4. Xử lý sự cố Rate Limit của Groq & Chuyển đổi LLM Model
*   **Triệu chứng:** Trong quá trình chạy thử nghiệm, hệ thống trả về lỗi: `Error code: 429 - Rate limit reached for model llama-3.3-70b-versatile... on tokens per day (TPD)`.
*   **Giải pháp:** Tiến hành thay đổi model giảng dạy chính trong file [llm.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/config/llm.py) từ `"llama-3.3-70b-versatile"` sang `"qwen/qwen3-32b"` để khôi phục hoạt động của hệ thống ngay lập tức. Sau khi chuyển đổi, hệ thống hoạt động ổn định và mượt mà.

---

## 🧪 Kết quả Kiểm thử thực tế cuối cùng

Sau khi áp dụng đầy đủ các sửa đổi và khởi chạy lại CLI bằng lệnh:
```bash
uv run main.py
```

### Kịch bản 1: Thử nghiệm lưu từ `mouse` (Từ chưa có trong DB)
1.  **Nhập lệnh:** `lưu từ mouse vào từ vựng`
2.  **Hệ thống xử lý:** Gọi `lookup_word_in_database` (trả về `found: false`) và `get_user_decks`.
3.  **Kết quả hiển thị:** AI nhận diện đúng từ `mouse` chưa có trong kho, hiển thị preview thông tin 9 trường của từ cực kỳ đẹp mắt và hỏi người học lựa chọn bộ từ để lưu.
4.  **Xác nhận lưu:** Học viên nhập `"lưu vào bộ từ vựng "từ vựng cá nhân""`.
5.  **Kết quả lưu:** Gọi `create_flashcards_in_database` thành công tốt đẹp, ghi thẻ vào bảng cards trên Supabase không bị crash, hiển thị thông báo chúc mừng kèm theo thẻ từ trực quan.

### Kịch bản 2: Hỏi từ vựng mới `home` với Model Qwen
1.  **Nhập lệnh:** `từ home có nghĩa là gì ?`
2.  **Kết quả hiển thị:** Phân tích đúng ý định học từ (`explain_word`), tra cứu DB (trả về `found: false`), cập nhật `current_word = 'home'`, chuyển tiếp kết quả và in bài giảng chi tiết, hấp dẫn với cấu trúc premium markdown và thử thách đặt câu thành công.
