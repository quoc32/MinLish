from langchain_core.messages import SystemMessage, AIMessage
from src.agent.state import AgentState
from src.config.llm import llm
from src.tools.database_lookup import lookup_word_in_database
from src.prompts.tutor_agent_prompt import SYSTEM_PROMPT

def tutor_agent_node(state: AgentState) -> dict:
    """
    Node cốt lõi điều phối hội thoại (Tutor Agent).
    - Liên kết (binding) công cụ lookup_word_in_database vào LLM Singleton.
    - Tiêm ID người học (user_id) và mục tiêu vào System Prompt để LLM tự quyết định gọi tool khi cần thiết.
    """
    user_id = state.get("user_id", "00000000-0000-0000-0000-000000000000")
    target_goal = state.get("target_goal", "IELTS")
    messages = state.get("messages", [])
    
    # Định dạng lại System Prompt để tiêm thông tin user_id và target_goal
    # Điều này giúp LLM tự động biết ID để điền vào đối số 'user_id' khi gọi công cụ lookup_word_in_database
    system_instruction = f"""{SYSTEM_PROMPT}

=== THÔNG TIN NGƯỜI HỌC HIỆN TẠI (CONTEXT) ===
- ID tài khoản của bạn (user_id): {user_id}
- Mục tiêu học tập (target_goal): {target_goal}

=== HƯỚNG DẪN GỌI CÔNG CỤ TRUY VẤN (TOOL CALLING & LEMMATIZATION) ===
1. CHỈ GỌI CÔNG CỤ khi học viên thực sự hỏi giải nghĩa của một từ vựng hoặc cụm từ cụ thể (ví dụ: "ubiquitous nghĩa là gì?", "giải thích từ scrutinized"). KHÔNG gọi công cụ khi chào hỏi xã giao hoặc hỏi ngữ pháp chung chung.
2. CHUẨN HÓA NGUYÊN THỂ (MANDATORY LEMMATIZATION): Trước khi truyền từ vào tham số `word` của công cụ `lookup_word_in_database`, bạn BẮT BUỘC phải chuyển từ vựng đó về dạng NGUYÊN THỂ (lemma / base form, viết thường). 
   * Ví dụ:
     - Số nhiều: "smartphones" -> gọi tool với "smartphone"
     - Động từ chia thì: "scrutinized" / "scrutinizing" / "scrutinizes" -> gọi tool với "scrutinize"
     - So sánh hơn/nhất: "easier" / "easiest" -> gọi tool với "easy"
     - Danh động từ: "running" -> gọi tool với "run"
   Điều này giúp đối khớp RAG chính xác 100% với dữ liệu được lưu trong cơ sở dữ liệu.
3. THAM SỐ GỌI CÔNG CỤ:
   - `word`: Từ vựng tiếng Anh đã được đưa về dạng nguyên thể viết thường.
   - `user_id`: Sử dụng chính xác chuỗi ID tài khoản '{user_id}' được cung cấp ở trên.

=== HƯỚNG DẪN CẤU TRÚC HIỂN THỊ PREMIUM MARDOWN ===
Để giao diện bong bóng chat của người học đạt độ thẩm mỹ cao nhất, hãy trình bày bài giảng theo đúng cấu trúc sau:
1. Câu dẫn vào tự nhiên và sinh động.
2. Thẻ từ vựng trực quan:
   ### 📖 **[Từ vựng]** ` [Phiên âm] ` • *[Loại từ]*
   > **Ý nghĩa:** [Nghĩa tiếng Việt]
   > *([Định nghĩa tiếng Anh])*
3. **Ví dụ thực tế:** [Ví dụ mẫu]
   👉 *Dịch nghĩa: [Dịch nghĩa ví dụ]*
4. Đan xen tự nhiên Collocation đi kèm, Từ vựng liên quan, Lưu ý nhỏ (nếu có).
5. Phần mở rộng **"💡 Gia sư phân tích sâu & mở rộng:"** (gồm: Điểm khác biệt, Mẹo ghi nhớ, Giao tiếp thực tế).
6. Khung thách đố đặt câu (`> `) nổi bật cuối bài giảng.
"""
    
    # Chuẩn bị tin nhắn cho LLM
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
        # Bind công cụ trực tiếp vào instance LLM Singleton dùng chung
        llm_with_tools = llm.bind_tools([lookup_word_in_database])
        
        # Gọi LLM (Sử dụng HTTP Keep-Alive connection pooling)
        response = llm_with_tools.invoke(llm_messages)
        
        # Nếu LLM trả về tin nhắn thông thường (kết thúc suy luận ở Lượt 2 hoặc khi không cần gọi tool)
        if not (hasattr(response, "tool_calls") and response.tool_calls):
            found_word = None
            is_found_in_db = False
            
            # Quét ngược các tin nhắn trong state và tin nhắn mới để tìm kết quả gọi tool
            all_messages = messages + [response]
            for msg in reversed(all_messages):
                # Kiểm tra ToolMessage
                if msg.type == "tool" and msg.name == "lookup_word_in_database":
                    try:
                        import json
                        tool_res = json.loads(msg.content)
                        if tool_res.get("found"):
                            found_word = tool_res.get("word")
                            is_found_in_db = True
                        break
                    except Exception:
                        pass
            
            # Nếu không tìm thấy qua ToolMessage, tìm qua đối số của AIMessage chứa tool_calls
            if not found_word:
                for msg in reversed(all_messages):
                    if msg.type == "ai" and hasattr(msg, "tool_calls") and msg.tool_calls:
                        for tc in msg.tool_calls:
                            if tc["name"] == "lookup_word_in_database":
                                found_word = tc["args"].get("word")
                                break
                        if found_word:
                            break
            
            updates = {"messages": [response]}
            if found_word:
                word_clean = found_word.lower().strip()
                updates["current_word"] = word_clean
                updates["is_in_database"] = is_found_in_db
                print(f"💾 [Tutor Agent State Update] current_word='{word_clean}', is_in_database={is_found_in_db}")
            
            # Kiểm tra xem AI có thách đố đặt câu ở phản hồi này không
            content_lower = response.content.lower()
            if ">" in response.content or "đặt câu" in content_lower or "viết một câu" in content_lower or "đặt một câu" in content_lower:
                updates["awaiting_sentence"] = True
                print(f"📝 [Tutor Agent] Đã thách đố đặt câu. Set awaiting_sentence = True")
            else:
                updates["awaiting_sentence"] = False
                
            return updates
            
        return {"messages": [response]}
        
    except Exception as e:
        error_response = AIMessage(
            content=f"Rất xin lỗi bạn, mình đang gặp một chút sự cố kỹ thuật kết nối LLM: {e}. Bạn thử hỏi lại từ vựng khác nhé!"
        )
        return {"messages": [error_response]}
