# Tổng kết Luồng 3: Kiểm tra Ngữ pháp & Đặt câu Sửa lỗi (AI Grammar Examiner)

Tài liệu này tổng hợp thiết kế hệ thống, các quy tắc nghiệp vụ và các cải tiến quan trọng đã thực hiện cho **Luồng 3 (Flow 3: Grammar Check)** của Gia sư AI MinLish.

---

## 🎯 1. Vai trò của Luồng 3 trong Hệ thống
Luồng 3 chịu trách nhiệm tiếp nhận câu tiếng Anh do học viên viết thử hoặc dán vào, chấm điểm/nhận xét ngữ pháp, sửa lỗi chi tiết bằng tiếng Việt, phân tích cách dùng từ mục tiêu, và gợi ý người dùng lưu từ vựng làm Flashcard cá nhân.

Hệ thống chia làm 2 kịch bản chính:
1. **Dạng 1 (Active Recall - Đặt câu theo từ vựng đang học):** Người học đặt câu chứa từ vựng vừa được giải nghĩa ở Luồng 1. Nếu từ vựng đó chưa có trong database (`is_in_database` là `False`), AI sẽ truy vấn danh sách Decks cá nhân của người học và gợi ý lưu thẻ vựng.
2. **Dạng 2 (Independent Grammar Check - Sửa lỗi câu độc lập):** Người học dán một câu bất kỳ để nhờ sửa lỗi, không liên quan đến từ vựng đang học. Kịch bản này không hiển thị gợi ý lưu thẻ.

---

## 📋 2. Quy tắc Nghiệp vụ & Trích xuất câu (Target Sentence Extraction)

Để tối ưu hóa trải nghiệm người dùng, AI được định hình bằng các bộ quy tắc chặt chẽ:
*   **Trích xuất câu mục tiêu (Target Sentence Extraction):** Học viên thường gõ câu tiếng Anh kèm theo yêu cầu tiếng Việt ở cuối (Ví dụ: *"I wake up early every morning . Sửa ngữ pháp"*). AI sẽ tự động tách biệt phần câu tiếng Anh cần sửa ra khỏi câu hỏi tiếng Việt.
*   **Hiển thị so sánh trực quan:**
    *   **Câu gốc của bạn:** Chỉ hiển thị phần câu tiếng Anh mục tiêu (đã loại bỏ phần *"Sửa ngữ pháp"*).
    *   **Câu sửa tự nhiên:** Bôi đậm các phần được chỉnh sửa/thêm/bớt để học viên dễ nhận biết điểm sai.
*   **Bỏ qua lỗi vụn vặt:** Không bắt lỗi thiếu dấu chấm câu hay thừa khoảng trắng trước dấu chấm do thói quen gõ phím của học viên, nhưng câu sửa lại của AI luôn có dấu câu chuẩn mực.

---

## 🛠️ 3. Các Cải tiến Quan trọng Đã Thực hiện

### 💡 Cải tiến 1: Tối ưu hóa Định tuyến (Intent Router)
*   **Trước đây:** Bộ định tuyến cứng nhắc quy định nếu trạng thái `awaiting_sentence = True` thì mọi tin nhắn của học viên đều bị rẽ sang Luồng 3 (Grammar Check), ngay cả khi họ hỏi chuyện phiếm hoặc dán một đoạn văn dài yêu cầu trích xuất từ vựng.
*   **Hiện tại:** Bỏ heuristic cứng nhắc, sử dụng LLM Classifier với các Quy tắc Ưu tiên Phân loại Cứng (Mandatory Rules) để định tuyến linh hoạt:
    *   Nếu tin nhắn là câu khẳng định luyện tập hoặc dán câu sửa lỗi ngắn -> Định tuyến sang `grammar_check`.
    *   Nếu tin nhắn hỏi định nghĩa từ, so sánh từ -> Định tuyến sang `explain_word`.
    *   Nếu tin nhắn là đoạn văn dài -> Định tuyến sang `extract_text`.

### 💡 Cải tiến 2: Nâng giới hạn lịch sử hội thoại lên 15 tin nhắn
*   **Trước đây:** Node `grammar_examiner.py` chỉ lấy 5 tin nhắn gần nhất (`messages[-5:]`). Khi học viên đồng ý lưu từ ở bước tiếp theo, toàn bộ ngữ cảnh bài giảng giải nghĩa ban đầu đã bị trôi đi và bị cắt mất, khiến AI bị mất gốc dữ liệu và lưu sai từ.
*   **Hiện tại:** Đã thay đổi thành `messages[-15:]` ở tất cả các node. Đảm bảo lịch sử chat bao quát cả bài giảng cũ, câu đặt thử, phản hồi sửa lỗi và lệnh xác nhận lưu thẻ của học viên mà không bị tràn token.

### 💡 Cải tiến 3: Đồng bộ giao diện Premium Markdown
*   Chuyển đổi các danh sách gạch đầu dòng khô khan thành các khối block trực quan:
    *   Sử dụng emoji biểu tượng sinh động (`📝`, `💡`, `🎯`, `✨`).
    *   Tách rõ ràng phần **Giải thích chi tiết** và phần **Cách dùng từ mục tiêu** để bài học mạch lạc, dễ học.

---

## 📄 4. Chi tiết các File liên quan trong Codebase

*   **Prompt Instruction:** [grammar_examiner_prompt.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/prompts/grammar_examiner_prompt.py) - Định nghĩa toàn bộ prompt hệ thống, quy tắc trích xuất câu và cấu trúc định dạng đầu ra.
*   **Node Logic:** [grammar_examiner.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/agent/grammar_examiner.py) - Quản lý việc gọi tool `get_user_decks` ngầm để nạp danh sách bộ từ cá nhân của người học và trả về trạng thái cập nhật cho State Graph.
*   **Routing Logic:** [intent_router.py](file:///Users/vovantu/Documents/LAPTRINHDIDONG/MinLish/ai-service/src/agent/intent_router.py) - Thực hiện phân loại ý định học viên nhanh bằng Heuristic (Python) và nâng cao bằng LLM Router.
