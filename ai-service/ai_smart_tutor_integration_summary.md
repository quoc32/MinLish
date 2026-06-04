# Tổng Hợp Quá Trình Tích Hợp AI Smart Tutor (FastAPI & LangGraph)

Tài liệu này tổng hợp toàn bộ các công việc, thay đổi mã nguồn và giải pháp tối ưu đã thực hiện để tích hợp đồ thị LangGraph AI Smart Tutor vào ứng dụng backend dạng FastAPI và kết nối kiểm thử thành công.

---

## 1. Giai Đoạn 1: Xây Dựng API Dịch Vụ (FastAPI)

Mục tiêu là chuyển đổi dịch vụ AI từ dạng CLI chạy cục bộ thành một HTTP REST API để Backend chính (ExpressJS) và Client (Android App) có thể gọi thông qua HTTP.

### Các thay đổi đã thực hiện:
* **[pyproject.toml](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/pyproject.toml)**: Bổ sung các thư viện `fastapi` và `uvicorn` vào danh sách dependencies.
* **[graph.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/agent/graph.py)**:
  * Tích hợp bộ nhớ `MemorySaver` của LangGraph làm checkpointer để quản lý phiên hội thoại.
  * Biên dịch đồ thị LangGraph kèm checkpointer:
    ```python
    from langgraph.checkpoint.memory import MemorySaver
    memory = MemorySaver()
    graph = workflow.compile(checkpointer=memory)
    ```
* **[main.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/main.py)**:
  * Cập nhật CLI để truyền thêm `config={"configurable": {"thread_id": state["user_id"]}}` khi chạy stream đồ thị. Việc này giúp giữ đồng bộ bộ nhớ và tránh lỗi do đồ thị LangGraph yêu cầu checkpointer.
* **[api.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/api.py) [NEW]**:
  * Thiết lập FastAPI server hỗ trợ CORS.
  * Xây dựng 3 endpoint chính:
    1. `GET /health`: Kiểm tra sức khỏe của dịch vụ.
    2. `POST /api/tutor/chat`: Nhận tin nhắn chat từ học viên, tự động khởi tạo trạng thái mặc định cho turn đầu tiên giống CLI, chạy đồ thị LangGraph theo `thread_id` (chính là `user_id` của học viên) và trả về nội dung text phản hồi cuối cùng của AI dưới định dạng gọn nhẹ `{ "success": true, "response": "..." }`.
    3. `POST /api/tutor/reset`: Xóa sạch checkpoints trong `MemorySaver` và cập nhật lại trạng thái trống cho thread của người dùng để reset toàn bộ cuộc trò chuyện.
* **[tutor_api_test.rest](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/tutor_api_test.rest) [NEW]**:
  * Tạo file REST Client chứa các mẫu HTTP Requests (`health`, `general chat`, `explain word`, `grammar check`, `reset`) để test nhanh các API bằng extension REST Client của VS Code.

---

## 2. Giai Đoạn 2: Tinh Chỉnh Prompts

Mục tiêu là tối ưu hiển thị tin nhắn trên di động, tránh định dạng blog dài dòng, làm phản hồi giống một gia sư nhắn tin trò chuyện thân thiện hơn nhưng vẫn giữ đầy đủ thông tin học tập cần thiết.

### Các thay đổi đã thực hiện:
* **[tutor_agent_prompt.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/prompts/tutor_agent_prompt.py)**:
  * Chuyển phong cách phản hồi sang dạng chat tự nhiên, đan xen emoji nhẹ nhàng.
  * Loại bỏ các định dạng Markdown cồng kềnh (như `### 📖`, trích dẫn `> **Ý nghĩa:**`).
  * Giảm tải phần mở rộng chuyên sâu: AI sẽ tự động chọn lọc và giải thích **duy nhất 1 khía cạnh thú vị** (hoặc so sánh từ đồng nghĩa, hoặc mẹo ghi nhớ memory hack, hoặc cụm từ giao tiếp) thay vì liệt kê dồn dập cả 3 như trước.
  * *Đảm bảo nội dung:* Vẫn giữ đầy đủ phiên âm, từ loại, ý nghĩa, câu ví dụ kèm dịch nghĩa tiếng Việt, collocation, từ liên quan, lưu ý nhỏ và thử thách đặt câu.
* **[grammar_examiner_prompt.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/prompts/grammar_examiner_prompt.py)**:
  * Rút gọn cấu trúc nhận xét ngữ pháp.
  * Trình bày lỗi sai bằng các gạch đầu dòng ngắn gọn kèm 1 dòng giải thích nhanh cho từng lỗi thay vì khối văn bản phân tích lớn.
  * Định dạng kết quả trực quan: Câu gốc, Câu sửa chuẩn (bôi đậm phần thay đổi).

---

## 3. Tối Ưu Hóa Token (Token Optimization)

### Vấn đề:
Khi thêm `MemorySaver`, LangGraph tự động lưu trữ và tích lũy tất cả lịch sử trò chuyện trong thread. Khi node xử lý lấy lịch sử này để gửi cho LLM, do các phản hồi giải nghĩa từ vựng của gia sư chứa nhiều thông tin và dài, việc gửi quá nhiều tin nhắn lịch sử (cấu hình cũ lấy `15` tin nhắn gần nhất) lên LLM ở mỗi turn chat khiến lượng token đầu vào (input tokens) tăng lên rất lớn.

### Giải pháp đã áp dụng:
Tôi đã cập nhật cả 6 node xử lý của Agent (`tutor_agent.py`, `grammar_examiner.py`, `word_saver.py`, `text_extractor.py`, `general_qa.py`, `off_topic.py`):
1. **Rút ngắn History Window:** Chỉ lấy tối đa **6 tin nhắn gần nhất** (`messages[-6:]`) thay vì 15. Do một chu kỳ học từ vựng (Tra từ $\rightarrow$ Đặt câu $\rightarrow$ Sửa lỗi & lưu thẻ) chỉ kéo dài tối đa 3 lượt (6 tin nhắn), cấu hình này là tối ưu để giữ trọn vẹn ngữ cảnh.
2. **Cắt giảm tin nhắn cũ cực kỳ mạnh mẽ (Aggressive Truncation):**
   * Với tin nhắn mới nhất của người dùng (tin nhắn cuối): Giữ độ dài tối đa để đảm bảo đầy đủ thông tin đầu vào.
   * Với các tin nhắn cũ trong lịch sử: Nếu độ dài vượt quá **250 ký tự**, hệ thống sẽ tự động cắt bỏ phần đuôi và chỉ gửi **150 ký tự đầu tiên** cho LLM. Nhờ đó, LLM biết được từ vựng/chủ đề nào đang thảo luận ở lượt trước mà không cần đọc lại toàn bộ bài giảng Markdown dài của lượt đó.

---

## 4. Hướng Dẫn Chạy Và Kiểm Thử

### Khởi chạy FastAPI Server local:
Chạy lệnh sau tại thư mục `ai-service`:
```bash
uv run uvicorn src.api:app --host 0.0.0.0 --port 8000 --reload
```

### Chạy CLI local để debug:
```bash
uv run main.py
```

### Cách thức kiểm thử API:
Bạn có thể sử dụng file [tutor_api_test.rest](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/tutor_api_test.rest) để gửi các request lên server, hoặc dùng lệnh curl:
```bash
# Gửi tin nhắn tra từ
curl -s -X POST -H "Content-Type: application/json" \
  -d '{"user_id": "test_user_id", "message": "meticulous nghĩa là gì?"}' \
  http://127.0.0.1:8000/api/tutor/chat
```
