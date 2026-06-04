# src/agent/off_topic.py

from langchain_core.messages import SystemMessage, AIMessage, HumanMessage, ToolMessage
from src.agent.state import AgentState
from src.config.llm import llm
from src.prompts.off_topic_prompt import OFF_TOPIC_PROMPT

def off_topic_node(state: AgentState) -> dict:
    """
    Node xử lý các câu hỏi ngoài phạm vi học tập (Luồng 4 - Off-topic Fallback).
    """
    messages = state.get("messages", [])
    
    system_instruction = OFF_TOPIC_PROMPT
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
        return {
            "messages": [response],
            "awaiting_sentence": False
        }
    except Exception as e:
        # Fallback cứng nếu LLM gặp sự cố
        fallback_msg = AIMessage(
            content="Mình là Gia sư tiếng Anh MinLish, mình chỉ có thể giúp bạn học từ vựng, ngữ pháp và luyện đặt câu thôi. Bạn có từ vựng nào cần hỏi không nè?"
        )
        return {"messages": [fallback_msg]}
