# src/agent/word_saver.py

from langchain_core.messages import SystemMessage, AIMessage, HumanMessage, ToolMessage
from src.agent.state import AgentState
from src.config.llm import llm
from src.tools.database_lookup import lookup_word_in_database
from src.tools.create_flashcards import get_user_decks, create_flashcards_in_database
from src.prompts.word_saver_prompt import WORD_SAVER_PROMPT

def word_saver_node(state: AgentState) -> dict:
    """
    Node chuyên trách lưu từ vựng (Luồng 5).
    - Hỗ trợ lưu 1 hoặc nhiều từ vựng độc lập cùng lúc.
    - Gọi tool check trùng lặp RAG song song và lấy danh sách decks.
    - Gợi ý lưu từ mới và gọi bulk insert khi học viên đồng ý.
    """
    user_id = state.get("user_id", "00000000-0000-0000-0000-000000000000")
    target_goal = state.get("target_goal", "IELTS")
    current_word = state.get("current_word")
    messages = state.get("messages", [])
    
    # Dựng System Instruction động cho Word Saver Node
    system_instruction = f"""{WORD_SAVER_PROMPT}

=== THÔNG TIN NGƯỜI HỌC & NGỮ CẢNH HIỆN TẠI ===
- ID tài khoản của bạn (user_id): {user_id}
- Mục tiêu học tập (target_goal): {target_goal}
- Từ vựng đang học hiện tại (current_word): {current_word if current_word else 'Trống'}

=== HƯỚNG DẪN GỌI CÔNG CỤ CHI TIẾT ===
1. Khi học viên yêu cầu lưu từ vựng:
   - Hãy trích xuất danh sách các từ vựng họ muốn lưu (ví dụ: ['mitigate', 'meticulous']).
   - BẮT BUỘC gọi công cụ `lookup_word_in_database` song song cho TỪNG từ đó (đối số `word`: từ nguyên thể viết thường, `user_id`: '{user_id}').
   - BẮT BUỘC gọi công cụ `get_user_decks` với `user_id='{user_id}'`.
2. Khi học viên đồng ý xác nhận lưu các từ mới:
   - BẮT BUỘC gọi công cụ `create_flashcards_in_database` truyền vào:
     * `user_id`: '{user_id}'
     * `deck_name`: Tên bộ từ học viên chọn (nếu không rõ, mặc định là 'Từ vựng cá nhân').
     * `words`: Mảng/Danh sách các đối tượng từ vựng mới được biên soạn đầy đủ 9 trường thông tin.
"""

    llm_messages = [SystemMessage(content=system_instruction)]
    
    # Cắt giảm tin nhắn lịch sử để tránh tràn Token
    recent_messages = messages[-6:]
    
    for idx, msg in enumerate(recent_messages):
        content = msg.content
        is_last = (idx == len(recent_messages) - 1)
        # Chỉ rút gọn nếu KHÔNG PHẢI tin nhắn cuối cùng và có độ dài quá lớn (> 250 ký tự)
        # Hoặc nếu là tin nhắn cuối cùng nhưng cực kỳ khổng lồ (> 3000 ký tự)
        threshold = 3000 if is_last else 250
        
        if isinstance(content, str) and len(content) > threshold:
            if is_last:
                msg_content = content[:2000] + "\n... [Nội dung cuối quá dài đã được lược bớt] ...\n"
            else:
                msg_content = content[:150] + "\n... [Nội dung cũ đã rút gọn] ...\n"
                
            if isinstance(msg, HumanMessage):
                llm_messages.append(HumanMessage(content=msg_content))
            elif isinstance(msg, AIMessage):
                llm_messages.append(AIMessage(content=msg_content, tool_calls=msg.tool_calls))
            elif isinstance(msg, ToolMessage):
                llm_messages.append(ToolMessage(content=msg_content, tool_call_id=msg.tool_call_id))
            else:
                llm_messages.append(msg)
        else:
            llm_messages.append(msg)
            
    try:
        # Bind cả 3 công cụ cần thiết vào LLM Singleton
        tools = [lookup_word_in_database, get_user_decks, create_flashcards_in_database]
        llm_with_tools = llm.bind_tools(tools)
        
        response = llm_with_tools.invoke(llm_messages)
        
        # Nếu LLM trả về tin nhắn thông thường (kết thúc suy luận ở Lượt 2)
        if not (hasattr(response, "tool_calls") and response.tool_calls):
            updates = {"messages": [response]}
            
            # Kiểm tra xem có vừa chạy tool create_flashcards_in_database thành công không
            flashcard_saved = False
            for msg in reversed(messages + [response]):
                if msg.type == "tool" and msg.name == "create_flashcards_in_database":
                    if "THÀNH CÔNG" in msg.content:
                        flashcard_saved = True
                    break
            
            if flashcard_saved:
                # Nếu đã lưu thẻ thành công, reset trạng thái
                updates["awaiting_sentence"] = False
                updates["current_word"] = None
                updates["is_in_database"] = None
                updates["extracted_words"] = []
                print("♻️ [Word Saver State Update] Đã lưu thẻ thành công. Reset state học từ đơn.")
            else:
                # Nếu vừa giải nghĩa từ mới và gợi ý lưu, set awaiting_sentence = True để chờ xác nhận
                content_lower = response.content.lower()
                if "lưu" in content_lower or "flashcard" in content_lower or "bộ từ" in content_lower:
                    updates["awaiting_sentence"] = True
                    print("📝 [Word Saver] Đã gợi ý lưu từ mới. Chờ học viên xác nhận...")
                else:
                    updates["awaiting_sentence"] = False
                    
            return updates
            
        return {"messages": [response]}
        
    except Exception as e:
        error_response = AIMessage(
            content=f"Rất xin lỗi bạn, bộ phận Lưu từ vựng đang gặp sự cố kết nối LLM: {e}. Bạn thử lưu lại nhé!"
        )
        return {"messages": [error_response]}
