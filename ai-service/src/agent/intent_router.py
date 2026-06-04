# src/agent/intent_router.py

import os
import json
from langchain_core.messages import SystemMessage, HumanMessage

from src.agent.state import AgentState

from src.config.llm import llm_router

def intent_router_node(state: AgentState) -> dict:
    """
    Node phân loại ý định (Intent Router).
    Phân tích tin nhắn cuối cùng của người học để quyết định rẽ nhánh sang luồng nghiệp vụ phù hợp.
    Sử dụng kết hợp Heuristic Rule (Python) và LLM Classifier để tối ưu hóa tốc độ và tránh Rate Limit.
    """
    messages = state.get("messages", [])
    if not messages:
        return {"intent": "general_chat"}
        
    last_user_message = messages[-1].content.strip()
    current_word = state.get("current_word")
    awaiting_sentence = state.get("awaiting_sentence", False)
    
    last_msg_lower = last_user_message.lower().strip()
    
    # ====================================================================
    # 1. BỘ PHÂN LOẠI NHANH BẰNG CODE PYTHON (HEURISTIC ROUTER - CHẠY MẤT 0ms)
    # ====================================================================
    
    # A. Lệnh lưu từ vựng rõ ràng
    if any(kw in last_msg_lower for kw in ["lưu từ", "lưu các từ", "thêm từ", "thêm các từ", "save word", "save words", "lưu giúp mình từ"]):
        print("🎯 [Heuristic Router] Nhận diện ý định học viên: 'save_word' (Lệnh lưu từ rõ ràng)")
        return {"intent": "save_word"}
        
    # G. Hỏi nghĩa từ vựng nhanh (Heuristic explain_word)
    # Ví dụ: "database có nghĩa là gì ?", "smartphones nghĩa là gì", "what is query", "meaning of index"
    explain_patterns = ["nghĩa là gì", "nghĩa gì", "là gì", "what is", "what does", "meaning of", "nghia la gi", "la gi", "dịch giúp", "dich giup"]
    if len(last_user_message) < 60 and any(pat in last_msg_lower for pat in explain_patterns):
        # Đảm bảo không trùng với các ý định khác như grammar_check hay save_word thực tế
        if not any(kw in last_msg_lower for kw in ["sửa", "sửa lỗi", "ngữ pháp", "check", "lưu"]):
            print("🎯 [Heuristic Router] Nhận diện ý định học viên: 'explain_word' (Hỏi nghĩa từ vựng nhanh)")
            return {"intent": "explain_word"}

    # B. Xác nhận đồng ý lưu từ đơn đang học (Thường là câu ngắn phản hồi xác nhận)
    if current_word and awaiting_sentence:
        # Loại trừ trường hợp người dùng đang hỏi nghĩa từ vựng khác
        is_asking_meaning = any(q in last_msg_lower for q in ["nghĩa là gì", "nghĩa gì", "là gì", "what does", "what is", "meaning of", "nghia la gi", "la gi"])
        if not is_asking_meaning:
            confirm_keywords = ["lưu", "lưu thẻ", "lưu từ này", "đồng ý", "lưu vào", "lưu đi", "ok", "yes", "có", "lưu giúp mình", "lưu giúp"]
            # Nếu tin nhắn ngắn và chứa từ khóa xác nhận
            if len(last_user_message) < 50 and any(kw in last_msg_lower for kw in confirm_keywords):
                # Loại trừ "có" trong các trợ động từ/cấu trúc không phải xác nhận
                is_invalid_co = "có" in last_msg_lower and any(x in last_msg_lower for x in ["có đúng", "có từ", "có câu", "có thể", "có không"])
                if not is_invalid_co:
                    print(f"🎯 [Heuristic Router] Nhận diện ý định học viên: 'save_word' (Xác nhận lưu từ '{current_word}')")
                    return {"intent": "save_word"}
            
    # C. Chào hỏi xã giao ngắn
    chat_keywords = ["hello", "hi", "chào bạn", "chào gia sư", "tạm biệt", "bye", "chào ad"]
    if len(last_user_message) < 15 and any(last_msg_lower == kw or last_msg_lower.startswith(kw) for kw in chat_keywords):
        print("🎯 [Heuristic Router] Nhận diện ý định học viên: 'general_chat' (Chào hỏi xã giao)")
        return {"intent": "general_chat"}

    # D. Yêu cầu sửa ngữ pháp rõ ràng (kể cả khi awaiting_sentence = False/True)
    grammar_keywords = ["sửa lỗi", "sửa ngữ pháp", "check ngữ pháp", "check câu", "câu này đúng chưa", "câu này viết đúng chưa", "sửa giúp", "sửa hộ", "grammar check", "check grammar", "correct this sentence"]
    if any(kw in last_msg_lower for kw in grammar_keywords):
        print("🎯 [Heuristic Router] Nhận diện ý định học viên: 'grammar_check' (Yêu cầu check ngữ pháp rõ ràng)")
        return {"intent": "grammar_check"}

    # E. Hỏi tiếng Anh/Ngữ pháp chung
    qa_keywords = ["phân biệt", "so sánh giữa", "khác nhau thế nào giữa", "khác gì nhau", "khi nào dùng", "cấu trúc", "cách dùng của", "phân biệt giữa"]
    if any(kw in last_msg_lower for kw in qa_keywords):
        print("🎯 [Heuristic Router] Nhận diện ý định học viên: 'general_qa' (Hỏi tiếng Anh/Ngữ pháp chung)")
        return {"intent": "general_qa"}

    # F. Hỏi lạc đề rõ ràng
    off_topic_keywords = ["code giúp", "viết code", "html", "javascript", "python code", "lập trình", "đá bóng", "đá banh", "thời tiết hôm nay", "giải toán", "bài toán"]
    if any(kw in last_msg_lower for kw in off_topic_keywords):
        print("🎯 [Heuristic Router] Nhận diện ý định học viên: 'off_topic' (Lạc đề hoàn toàn)")
        return {"intent": "off_topic"}





    # ====================================================================
    # 2. FALLBACK GỌI LLM PHÂN LOẠI (Chỉ khi quy tắc heuristic không nhận diện được)
    # ====================================================================
    
    # Định dạng 3 tin nhắn hội thoại gần nhất để cung cấp ngữ cảnh cho LLM phân loại
    recent_msgs = messages[-3:]
    conversation_lines = []
    for msg in recent_msgs:
        role = "Học viên" if msg.type == "human" else "Gia sư"
        if msg.type in ["human", "ai"]:
            # Rút gọn tin nhắn nếu quá dài để tiết kiệm tối đa token
            content = msg.content
            if len(content) > 300:
                content = content[:150] + "\n... [Nội dung dài đã lược bớt] ...\n" + content[-100:]
            conversation_lines.append(f"{role}: {content}")
    formatted_conversation = "\n".join(conversation_lines)
    
    # Prompt phân loại chi tiết và rõ ràng để LLM luôn rẽ nhánh chính xác
    router_prompt = f"""Bạn là Bộ phân loại ý định (Intent Classifier) siêu tốc của ứng dụng MinLish.
Nhiệm vụ của bạn là đọc tin nhắn cuối cùng của người học và phân loại chính xác ý định của họ vào 1 trong 7 nhóm sau:

1. "explain_word":
   - Học viên chỉ hỏi nghĩa, cách phát âm, ví dụ hoặc cách dùng của MỘT từ vựng / cụm từ / thành ngữ tiếng Anh đơn lẻ (Ví dụ: "scrutinize là gì?", "phân biệt scrutinize và look at", "discrepancy nghĩa là gì").
   - Chú ý: Chỉ dành cho việc hỏi định nghĩa từ mới, KHÔNG bao gồm việc đặt câu tiếng Anh để luyện tập, yêu cầu sửa ngữ pháp hay yêu cầu lưu từ.

2. "grammar_check":
   - Học viên đang trả lời thử thách đặt câu tiếng Anh có chứa từ vựng đang học (đặc biệt khi đang ở trạng thái chờ đặt câu với từ vựng hiện tại).
   - Học viên chủ động yêu cầu sửa lỗi ngữ pháp cho một câu tiếng Anh cụ thể hoặc dán câu vào hỏi xem đúng hay chưa (Ví dụ: "sửa ngữ pháp giúp mình câu này: I has a pen", "câu này đúng chưa: she don't know").
   - Chú ý: KHÔNG bao gồm yêu cầu lưu từ vựng hay chào hỏi.

3. "extract_text":
   - Học viên dán một đoạn văn tiếng Anh học thuật dài (hoặc một câu ghép dài có nhiều từ vựng khó) và yêu cầu trích xuất từ vựng, tạo thẻ Flashcard từ đoạn văn đó.
   - Học viên phản hồi đồng ý lưu danh sách từ vựng trích xuất từ đoạn văn của lượt trước (Luồng 2) vào bộ từ (Ví dụ: "Lưu vào TRY TO LEARN", "Lưu vào Từ vựng Công nghệ").

4. "save_word":
   - Học viên chủ động yêu cầu lưu 1 hoặc nhiều từ vựng đơn lẻ cụ thể vào kho từ (Ví dụ: "lưu từ mitigate", "lưu các từ meticulous, scrupulous vào bộ từ của tôi", "thêm từ scrupulous vào bộ từ cá nhân", "save words: Apple, Banana").
   - Học viên phản hồi đồng ý lưu từ đơn sau khi AI vừa gợi ý giải nghĩa ở Word Saver Node (Ví dụ: "Có", "lưu thẻ", "lưu từ này", "đồng ý", "Lưu vào TRY TO LEARN").

5. "general_qa":
   - Học viên hỏi đáp kiến thức tiếng Anh hoặc ngữ pháp chung, phân biệt cách sử dụng các từ đồng nghĩa, hoặc hỏi về các cấu trúc ngữ pháp tiếng Anh nói chung (Ví dụ: "làm sao phân biệt say, tell, speak, talk?", "thì hiện tại hoàn thành dùng khi nào?", "cấu trúc wish dùng ra sao?").
   - Chú ý: Đây là những câu hỏi kiến thức tiếng Anh tổng hợp, KHÔNG phải tra nghĩa một từ đơn lẻ (explain_word) và cũng KHÔNG phải là học viên tự viết câu tiếng Anh rồi nhờ sửa ngữ pháp (grammar_check).

6. "off_topic":
   - Học viên hỏi các chủ đề hoàn toàn không liên quan đến tiếng Anh hay học tập (Ví dụ: "code giúp tôi trang web bằng HTML", "giải bài toán này", "tối nay đá bóng ai thắng?", "kể chuyện cười đi", "bạn có người yêu chưa?").

7. "general_chat":
   - Học viên chào hỏi xã giao (hello, hi, chào bạn, chào gia sư, tạm biệt).
   - Học viên hỏi thăm sức khỏe hoặc chuyện trò ngắn gũi, thân thiện.


=== NGỮ CẢNH TRẠNG THÁI (STATE CONTEXT) ===
- Từ vựng đang thảo luận (current_word): {current_word if current_word else 'Trống (Không thảo luận từ nào)'}
- Đang chờ đặt câu thử thách (awaiting_sentence): {awaiting_sentence}

=== ĐOẠN HỘI THOẠI GẦN NHẤT (CONVERSATION HISTORY) ===
{formatted_conversation}

=== QUY TẮC ƯU TIÊN PHÂN LOẠI CỨNG (MANDATORY RULES) ===
1. Khi `awaiting_sentence` đang là True:
   - Nếu học viên viết một câu tiếng Anh luyện tập (declarative statement) hoặc dán một câu/đoạn ngắn để check lỗi mà không hỏi nghĩa, không chào hỏi, không yêu cầu trích xuất -> Bạn phân loại là "grammar_check".
   - Nếu học viên hỏi định nghĩa từ, hỏi phát âm, so sánh các từ -> Vẫn BẮT BUỘC phân loại là "explain_word".
   - Nếu học viên yêu cầu trích xuất từ vựng từ một đoạn văn/câu dài -> Vẫn BẮT BUỘC phân loại là "extract_text".
   - Nếu học viên chào hỏi, hỏi chuyện phiếm ngoài lề -> Vẫn phân loại là "general_chat".
2. Nếu học viên gõ xác nhận đồng ý lưu từ (ví dụ: "Có", "lưu thẻ", "lưu từ này", "lưu giúp mình", "đồng ý", hoặc chọn lưu vào bộ cụ thể "Lưu vào TRY TO LEARN") sau khi AI gợi ý lưu từ vựng (ở bất kỳ luồng nào như Luồng 2 hay Luồng 3) -> Bạn BẮT BUỘC phải phân loại ý định là "save_word".
   - Ví dụ: Học viên gõ "Có, lưu giúp mình" hoặc "Lưu vào TRY TO LEARN" -> Ý định phải là "save_word".

Tin nhắn cuối cùng của người học: "{last_user_message}"

QUY TẮC TRẢ VỀ:
- Chỉ trả về duy nhất một chuỗi JSON hợp lệ theo định dạng sau, không kèm bất kỳ lời giải thích nào khác ngoài JSON:
{{
  "intent": "explain_word" hoặc "grammar_check" hoặc "extract_text" hoặc "save_word" hoặc "general_qa" hoặc "off_topic" hoặc "general_chat"
}}
"""
    try:
        # Gọi mô hình LLM Singleton dùng chung (Chỉ gửi prompt ngắn, không gửi chat history lê thê để tránh Rate Limit)
        response = llm_router.invoke([HumanMessage(content=router_prompt)])
        content = response.content.strip()
        
        # Sử dụng Regex trích xuất JSON nằm giữa { và } một cách cực kỳ robust
        import re
        json_match = re.search(r'\{.*\}', content, re.DOTALL)
        if json_match:
            content = json_match.group(0)
            
        result = json.loads(content)
        intent = result.get("intent", "general_chat").strip().lower()
        
        if intent not in ["explain_word", "grammar_check", "extract_text", "save_word", "general_chat", "general_qa", "off_topic"]:
            intent = "general_chat"
            
        print(f"🎯 [LLM Router] Nhận diện ý định học viên: '{intent}'")
        return {"intent": intent}

        
    except Exception as e:
        print(f"Lỗi phân loại ý định bằng LLM: {e}")
        # Mặc định dự phòng nếu lỗi là giải nghĩa từ
        return {"intent": "explain_word"}

