# src/agent/grammar_examiner.py

from langchain_core.messages import SystemMessage, AIMessage, HumanMessage, ToolMessage
from src.agent.state import AgentState
from src.config.llm import llm
from src.tools.create_flashcards import get_user_decks
from src.prompts.grammar_examiner_prompt import GRAMMAR_EXAMINER_PROMPT

def grammar_examiner_node(state: AgentState) -> dict:
    """
    Node chuyên trách chấm điểm đặt câu, sửa lỗi ngữ pháp (Luồng 3).
    - Cung cấp tính năng sửa ngữ pháp chi tiết cho câu đặt thử của từ vựng đang học.
    - Cung cấp tính năng check ngữ pháp cho các câu độc lập dán vào.
    - Gợi ý lưu từ mới (nếu có) và hiển thị danh sách Decks cá nhân của học viên.
    - Không thực hiện bất kỳ lệnh lưu thẻ trực tiếp nào tại đây (được chuyển giao cho word_saver_node).
    """
    user_id = state.get("user_id", "00000000-0000-0000-0000-000000000000")
    target_goal = state.get("target_goal", "IELTS")
    current_word = state.get("current_word")
    is_in_db = state.get("is_in_database", False)
    messages = state.get("messages", [])
    
    # Xây dựng System Instruction động cho Grammar Examiner Node
    system_instruction = f"""{GRAMMAR_EXAMINER_PROMPT}

=== THÔNG TIN NGƯỜI HỌC & NGỮ CẢNH HIỆN TẠI ===
- ID tài khoản của bạn (user_id): {user_id}
- Mục tiêu học tập (target_goal): {target_goal}
- Từ vựng đang học (current_word): {current_word if current_word else 'Trống (Người học tự dán câu độc lập, không theo từ vựng nào)'}
- Trạng thái từ vựng trong DB của người học (is_in_database): {is_in_db}

=== HƯỚNG DẪN GỌI CÔNG CỤ ===
1. Nếu học viên đặt câu chứa từ '{current_word}' và `is_in_database` đang là False:
   - Gọi công cụ `get_user_decks` với tham số `user_id='{user_id}'` để liệt kê các bộ từ cá nhân cho học viên chọn lưu.
2. TUYỆT ĐỐI KHÔNG thực hiện bất kỳ cuộc gọi tool lưu database nào tại Node này. Việc lưu thẻ khi học viên đồng ý sẽ do Word Saver Node đảm nhiệm.
"""

    llm_messages = [SystemMessage(content=system_instruction)]
    
    # Lấy tối đa 6 tin nhắn gần nhất để tránh tràn Token và không bị mất ngữ cảnh từ vựng
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
        # Chỉ bind get_user_decks để lấy danh sách decks hiển thị khi gợi ý lưu
        tools = [get_user_decks]
        llm_with_tools = llm.bind_tools(tools)
        
        response = llm_with_tools.invoke(llm_messages)
        
        # Nếu LLM trả về tin nhắn thông thường (kết thúc suy luận ở Lượt 2)
        if not (hasattr(response, "tool_calls") and response.tool_calls):
            updates = {"messages": [response]}
            
            # Gợi ý lưu từ mới và set awaiting_sentence = True để chờ phản hồi xác nhận lưu
            if current_word and not is_in_db:
                content_lower = response.content.lower()
                if "lưu" in content_lower or "flashcard" in content_lower or "bộ từ" in content_lower:
                    updates["awaiting_sentence"] = True
                    print("📝 [Grammar Examiner] Gợi ý lưu từ mới. Chờ học viên xác nhận lưu...")
                else:
                    updates["awaiting_sentence"] = False
            else:
                updates["awaiting_sentence"] = False
            
            return updates
            
        return {"messages": [response]}
        
    except Exception as e:
        error_response = AIMessage(
            content=f"Rất xin lỗi bạn, bộ phận Kiểm tra ngữ pháp đang gặp sự cố kết nối LLM: {e}. Bạn thử đặt lại câu khác nhé!"
        )
        return {"messages": [error_response]}
