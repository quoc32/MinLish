# src/agent/general_qa.py

from langchain_core.messages import SystemMessage, AIMessage, HumanMessage, ToolMessage
from src.agent.state import AgentState
from src.config.llm import llm
from src.prompts.general_qa_prompt import GENERAL_QA_PROMPT

def general_qa_node(state: AgentState) -> dict:
    """
    Node giải đáp thắc mắc tiếng Anh / Ngữ pháp chung (Luồng 4 - General QA).
    """
    messages = state.get("messages", [])
    
    system_instruction = GENERAL_QA_PROMPT
    llm_messages = [SystemMessage(content=system_instruction)]
    
    # Lấy tối đa 6 tin nhắn gần nhất trong lịch sử
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
        response = llm.invoke(llm_messages)
        # Tự động kết thúc trạng thái đặt câu nếu học viên chuyển sang hỏi đáp chung
        return {
            "messages": [response],
            "awaiting_sentence": False
        }
    except Exception as e:
        error_response = AIMessage(
            content=f"Rất xin lỗi bạn, mình gặp sự cố kết nối LLM khi giải đáp thắc mắc: {e}. Bạn hỏi lại nhé!"
        )
        return {"messages": [error_response]}
