# src/agent/graph.py

from langgraph.graph import StateGraph, START, END
from langgraph.prebuilt import ToolNode
from src.agent.state import AgentState
from src.agent.intent_router import intent_router_node
from src.agent.tutor_agent import tutor_agent_node
from src.agent.text_extractor import text_extractor_node
from src.agent.grammar_examiner import grammar_examiner_node
from src.agent.word_saver import word_saver_node
from src.agent.general_qa import general_qa_node
from src.agent.off_topic import off_topic_node
from src.tools.database_lookup import lookup_word_in_database
from src.tools.create_flashcards import get_user_decks, create_flashcards_in_database


def route_intent(state: AgentState) -> str:
    """
    Rẽ nhánh từ Intent Router sang Node xử lý chuyên sâu:
    - intent = 'explain_word' -> Đi tới 'tutor_agent' (Luồng 1)
    - intent = 'extract_text' -> Đi tới 'text_extractor' (Luồng 2)
    - intent = 'grammar_check' -> Đi tới 'grammar_examiner' (Luồng 3)
    - intent = 'save_word' -> Đi tới 'word_saver' (Luồng 5)
    - intent = 'general_qa' -> Đi tới 'general_qa' (Luồng 4)
    - intent = 'off_topic' -> Đi tới 'off_topic' (Luồng 4)
    - intent = 'general_chat' -> Mặc định đi tới 'tutor_agent' để xử lý chat tự do/lạc đề
    """
    intent = state.get("intent", "explain_word")
    if intent == "extract_text":
        return "text_extractor"
    elif intent == "grammar_check":
        return "grammar_examiner"
    elif intent == "save_word":
        return "word_saver"
    elif intent == "general_qa":
        return "general_qa"
    elif intent == "off_topic":
        return "off_topic"
    return "tutor_agent"


def should_continue(state: AgentState) -> str:
    """
    Hàm rẽ nhánh điều kiện sau khi Node chính (tutor_agent, text_extractor, grammar_examiner, word_saver) phản hồi:
    - Nếu LLM sinh ra yêu cầu gọi công cụ ('tool_calls') -> Chuyển sang node 'tools' để thực thi.
    - Nếu không có yêu cầu gọi công cụ -> Kết thúc luồng hội thoại và trả về kết quả cho học viên.
    """
    messages = state.get("messages", [])
    if not messages:
        return END
        
    last_message = messages[-1]
    
    # Nếu LLM yêu cầu gọi tool
    if hasattr(last_message, "tool_calls") and last_message.tool_calls:
        print(f"🤖 LLM quyết định gọi công cụ: {last_message.tool_calls[0]['name']}")
        return "tools"
        
    return END

def route_after_tools(state: AgentState) -> str:
    """
    Điều hướng luồng chạy sau khi thực thi xong công cụ ngầm (ToolNode):
    Quay trở lại đúng Node đã gọi công cụ đó ban đầu để suy luận lần 2:
    - Nếu đang ở luồng trích xuất văn bản (intent = 'extract_text') -> Quay lại 'text_extractor'
    - Nếu đang ở luồng sửa ngữ pháp (intent = 'grammar_check') -> Quay lại 'grammar_examiner'
    - Nếu đang ở luồng lưu từ (intent = 'save_word') -> Quay lại 'word_saver'
    - Nếu đang ở luồng hỏi đáp chung (intent = 'general_qa') -> Quay lại 'general_qa'
    - Nếu đang ở luồng lạc đề (intent = 'off_topic') -> Quay lại 'off_topic'
    - Nếu ở các luồng khác -> Quay lại 'tutor_agent'
    """
    intent = state.get("intent", "explain_word")
    if intent == "extract_text":
        return "text_extractor"
    elif intent == "grammar_check":
        return "grammar_examiner"
    elif intent == "save_word":
        return "word_saver"
    elif intent == "general_qa":
        return "general_qa"
    elif intent == "off_topic":
        return "off_topic"
    return "tutor_agent"


# 1. Khởi tạo StateGraph
workflow = StateGraph(AgentState)

# 2. Đăng ký các node xử lý vào đồ thị
workflow.add_node("intent_router", intent_router_node)
workflow.add_node("tutor_agent", tutor_agent_node)
workflow.add_node("text_extractor", text_extractor_node)
workflow.add_node("grammar_examiner", grammar_examiner_node)
workflow.add_node("word_saver", word_saver_node)
workflow.add_node("general_qa", general_qa_node)
workflow.add_node("off_topic", off_topic_node)


# Đăng ký ToolNode chứa đầy đủ cả 3 công cụ Python dùng chung
tool_node = ToolNode([lookup_word_in_database, get_user_decks, create_flashcards_in_database])
workflow.add_node("tools", tool_node)

# 3. Thiết lập các cạnh nối (Edges & Conditional Edges)
# Điểm vào đầu tiên luôn là Intent Router
workflow.add_edge(START, "intent_router")

# Rẽ nhánh động từ Intent Router sang Node chuyên trách
workflow.add_conditional_edges(
    "intent_router",
    route_intent,
    {
        "tutor_agent": "tutor_agent",
        "text_extractor": "text_extractor",
        "grammar_examiner": "grammar_examiner",
        "word_saver": "word_saver",
        "general_qa": "general_qa",
        "off_topic": "off_topic"
    }
)


# Rẽ nhánh điều kiện từ tutor_agent (Luồng 1) sang node tools hoặc END
workflow.add_conditional_edges(
    "tutor_agent",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)

# Rẽ nhánh điều kiện từ text_extractor (Luồng 2) sang node tools hoặc END
workflow.add_conditional_edges(
    "text_extractor",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)

# Rẽ nhánh điều kiện từ grammar_examiner (Luồng 3) sang node tools hoặc END
workflow.add_conditional_edges(
    "grammar_examiner",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)

# Rẽ nhánh điều kiện từ word_saver (Luồng 5) sang node tools hoặc END
workflow.add_conditional_edges(
    "word_saver",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)

# Rẽ nhánh điều kiện từ general_qa (Luồng 4) sang node tools hoặc END
workflow.add_conditional_edges(
    "general_qa",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)

# Rẽ nhánh điều kiện từ off_topic (Luồng 4) sang node tools hoặc END
workflow.add_conditional_edges(
    "off_topic",
    should_continue,
    {
        "tools": "tools",
        END: END
    }
)


# Sau khi thực thi xong công cụ, quay trở về đúng Node LLM cha
workflow.add_conditional_edges(
    "tools",
    route_after_tools,
    {
        "tutor_agent": "tutor_agent",
        "text_extractor": "text_extractor",
        "grammar_examiner": "grammar_examiner",
        "word_saver": "word_saver",
        "general_qa": "general_qa",
        "off_topic": "off_topic"
    }
)


# 4. Biên dịch đồ thị
from langgraph.checkpoint.memory import MemorySaver

memory = MemorySaver()
graph = workflow.compile(checkpointer=memory)
