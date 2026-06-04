# src/agent/text_extractor.py

from langchain_core.messages import SystemMessage, AIMessage
from src.agent.state import AgentState
from src.config.llm import llm
from src.tools.database_lookup import lookup_word_in_database
from src.tools.create_flashcards import get_user_decks, create_flashcards_in_database
from src.prompts.text_extractor_prompt import EXTRACTOR_PROMPT

def text_extractor_node(state: AgentState) -> dict:
    """
    Node xử lý trích xuất từ vựng từ đoạn văn và điều phối lưu thẻ (Luồng 2).
    - Liên kết (binding) các công cụ: get_user_decks, create_flashcards_in_database, và lookup_word_in_database.
    - Tiêm các thông tin trạng thái cần thiết vào Prompt chỉ dẫn của Extractor.
    """
    user_id = state.get("user_id", "00000000-0000-0000-0000-000000000000")
    target_goal = state.get("target_goal", "IELTS")
    messages = state.get("messages", [])
    
    # Xây dựng System Instruction động cho Extractor Node
    system_instruction = f"""{EXTRACTOR_PROMPT}

=== THÔNG TIN NGƯỜI HỌC HIỆN TẠI (CONTEXT) ===
- ID tài khoản của bạn (user_id): {user_id}
- Mục tiêu học tập (target_goal): {target_goal}

=== HƯỚNG DẪN KỸ THUẬT GỌI CÔNG CỤ ===
1. Khi học viên dán đoạn văn dài tiếng Anh và yêu cầu trích xuất từ:
   - Hãy suy nghĩ chọn ra 3-5 từ học thuật đắt giá nhất.
   - Gọi công cụ `lookup_word_in_database` cho từng từ khóa được chọn để kiểm tra xem chúng đã có trong database của học viên chưa.
     * Tham số `word`: từ vựng tiếng Anh nguyên thể viết thường.
     * Tham số `user_id`: Sử dụng chính xác chuỗi ID tài khoản '{user_id}'.
   - Đồng thời, gọi công cụ `get_user_decks` với tham số `user_id='{user_id}'` để lấy ra danh sách các bộ từ cá nhân hiện có của học viên.
"""

    # Chuẩn bị tin nhắn gửi cho LLM
    llm_messages = [SystemMessage(content=system_instruction)]
    
    # Tối ưu hóa tin nhắn hội thoại để tránh lỗi vượt quá giới hạn Token (TPM Limit 6000 của Groq)
    # 1. Chỉ lấy tối đa 6 tin nhắn gần nhất trong lịch sử để tránh bị mất ngữ cảnh từ vựng
    recent_messages = messages[-6:]
    
    from langchain_core.messages import HumanMessage, AIMessage, ToolMessage
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
        # Bind danh sách công cụ phục vụ Luồng 2 vào LLM Singleton (Không còn tool create_flashcards do đã chuyển về word_saver)
        tools = [lookup_word_in_database, get_user_decks]
        llm_with_tools = llm.bind_tools(tools)
        
        # Gọi LLM (Sử dụng HTTP Keep-Alive connection pooling)
        response = llm_with_tools.invoke(llm_messages)
        
        return {"messages": [response]}
        
    except Exception as e:
        error_response = AIMessage(
            content=f"Rất xin lỗi bạn, bộ phận Trích xuất từ vựng của MinLish đang gặp sự cố kỹ thuật: {e}. Bạn thử dán lại đoạn văn khác nhé!"
        )
        return {"messages": [error_response]}
