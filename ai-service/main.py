# main.py

import os
from dotenv import load_dotenv
from langchain_core.messages import HumanMessage, AIMessage
from src.agent.graph import graph

# Tải biến môi trường
load_dotenv()

def print_tutor_response(text: str):
    """
    In phản hồi của gia sư ra màn hình với định dạng đẹp mắt.
    """
    print("\n=================== GIA SƯ AI MINLISH ===================")
    print(text)
    print("=========================================================\n")

def main():
    # Kiểm tra cấu hình LLM
    base_url = os.getenv("LLM_BASE_URL")
    api_key = os.getenv("LLM_API_KEY")
    model_name = os.getenv("LLM_MODEL")
    
    print("=========================================================")
    print("🚀 KHỞI ĐỘNG HỆ THỐNG GIA SƯ AI MINLISH - LUỒNG 1 CLI")
    print(f"📍 API Endpoint: {base_url}")
    print(f"🤖 LLM Model: {model_name}")
    print("=========================================================\n")
    
    if not api_key or "your_groq_api_key" in api_key:
        print("⚠️ CẢNH BÁO: Chưa cấu hình LLM_API_KEY hợp lệ trong file .env!")
        print("Vui lòng mở file ai-service/.env và điền Groq API Key để bắt đầu trò chuyện.")
        print("---------------------------------------------------------\n")
    
    # Khởi tạo State ban đầu
    state = {
        "user_id": "2552f5d5-9e74-4313-9969-13666637c179",  # Dummy UUID cho hệ thống
        "target_goal": "IELTS",
        "messages": [],
        "current_word": None,
        "is_in_database": None,
        "user_sentence": None,
        "extracted_words": [],
        "awaiting_sentence": False,
        "rag_context": {"found": False}
    }
    
    print("Gia sư AI MinLish đã sẵn sàng! Gõ 'exit' hoặc 'quit' để dừng trò chuyện.")
    print_tutor_response("Chào bạn! Mình là Gia sư Tiếng Anh MinLish đây. Bạn có từ vựng nào cần hỏi nghĩa hôm nay không nè? 😉")
    
    while True:
        try:
            user_input = input("Bạn: ").strip()
            if not user_input:
                continue
                
            if user_input.lower() in ["exit", "quit"]:
                print("\nTạm biệt bạn nhé! Hẹn gặp lại bạn trong các buổi học từ vựng tiếp theo cùng MinLish! 👋")
                break
                
            # 1. Thêm tin nhắn của người dùng vào state (gửi tin nhắn mới qua checkpointer)
            config = {"configurable": {"thread_id": state["user_id"]}}
            input_state = {
                "user_id": state["user_id"],
                "target_goal": state["target_goal"],
                "messages": [HumanMessage(content=user_input)]
            }
            
            # 2. Gửi state vào LangGraph workflow và stream các bước xử lý
            print("\n=================== CHU TRÌNH SUY LUẬN CỦA AGENT ===================")
            
            for event in graph.stream(input_state, config, stream_mode="updates"):
                for node_name, update in event.items():
                    print(f"\n⚙️ [Thực thi xong Node: '{node_name}']")
                    
                    # Cập nhật các trường dữ liệu từ bản update vào state hiện tại
                    for key, value in update.items():
                        if key == "messages":
                            state["messages"].extend(value)
                        else:
                            state[key] = value
                            
                    # Tracing chi tiết hành động của từng Node
                    
                    # Node Intent Router (Phân loại ý định) chạy xong
                    if node_name == "intent_router":
                        print(f"🎯 AI Router: Phân tích thành công ý định học viên -> '{state.get('intent')}'")
                        
                    elif "messages" in update:
                        last_msg = update["messages"][-1]
                        
                        # Node Tutor Agent (LLM Luồng 1) chạy xong
                        if node_name == "tutor_agent":
                            # Trường hợp LLM quyết định gọi tool (Lượt 1)
                            if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
                                tool_call = last_msg.tool_calls[0]
                                print(f"🤖 AI suy luận (Luồng 1): Cần gọi công cụ tra cứu dữ liệu RAG.")
                                print(f"   👉 Quyết định gọi tool: '{tool_call['name']}'")
                                print(f"      ├─ Tham số 'word': '{tool_call['args'].get('word')}'")
                                print(f"      └─ Tham số 'user_id': '{tool_call['args'].get('user_id')}'")
                            # Trường hợp LLM trả lời trực tiếp (Lượt 2 hoặc khi không cần gọi tool)
                            else:
                                print(f"🤖 AI suy luận (Luồng 1): Đã có đủ thông tin, sinh phản hồi giảng bài cuối cùng.")
                                print_tutor_response(last_msg.content)
                                
                        # Node Text Extractor (LLM Luồng 2) chạy xong
                        elif node_name == "text_extractor":
                            # Trường hợp LLM quyết định gọi tool
                            if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
                                print(f"🤖 AI suy luận (Luồng 2): Cần thực thi công cụ hỗ trợ trích xuất hoặc ghi dữ liệu.")
                                for tc in last_msg.tool_calls:
                                    print(f"   👉 Quyết định gọi tool: '{tc['name']}'")
                                    print(f"      └─ Tham số: {tc['args']}")
                            # Trường hợp LLM trả lời trực tiếp
                            else:
                                print(f"🤖 AI suy luận (Luồng 2): Sinh phản hồi hiển thị báo cáo hoặc chúc mừng.")
                                print_tutor_response(last_msg.content)
                                
                        # Node Grammar Examiner (LLM Luồng 3) chạy xong
                        elif node_name == "grammar_examiner":
                            # Trường hợp LLM quyết định gọi tool
                            if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
                                print(f"🤖 AI suy luận (Luồng 3): Cần thực thi công cụ hỗ trợ sửa lỗi hoặc lưu thẻ.")
                                for tc in last_msg.tool_calls:
                                    print(f"   👉 Quyết định gọi tool: '{tc['name']}'")
                                    print(f"      └─ Tham số: {tc['args']}")
                            # Trường hợp LLM trả lời trực tiếp
                            else:
                                print(f"🤖 AI suy luận (Luồng 3): Sinh phản hồi sửa lỗi ngữ pháp chi tiết.")
                                print_tutor_response(last_msg.content)
                                
                        # Node Word Saver (LLM Luồng 5) chạy xong
                        elif node_name == "word_saver":
                            # Trường hợp LLM quyết định gọi tool
                            if hasattr(last_msg, "tool_calls") and last_msg.tool_calls:
                                print(f"🤖 AI suy luận (Luồng 5): Cần thực thi công cụ hỗ trợ lưu trữ từ vựng.")
                                for tc in last_msg.tool_calls:
                                    print(f"   👉 Quyết định gọi tool: '{tc['name']}'")
                                    print(f"      └─ Tham số: {tc['args']}")
                            # Trường hợp LLM trả lời trực tiếp
                            else:
                                print(f"🤖 AI suy luận (Luồng 5): Sinh phản hồi giải nghĩa hoặc thông báo kết quả lưu thẻ.")
                                print_tutor_response(last_msg.content)
                                
                        # Node General English QA (LLM Luồng 4) chạy xong
                        elif node_name == "general_qa":
                            print(f"🤖 AI suy luận (Luồng 4): Sinh câu trả lời giải đáp tiếng Anh chung.")
                            print_tutor_response(last_msg.content)
                            
                        # Node Off-topic Fallback (LLM Luồng 4) chạy xong
                        elif node_name == "off_topic":
                            print(f"🤖 AI suy luận (Luồng 4): Nhận diện lạc đề, sinh phản hồi hướng học viên về mục tiêu học tập.")
                            print_tutor_response(last_msg.content)

                                
                        # Node Tools (Python) chạy xong
                        elif node_name == "tools":
                            print(f"📥 Kết quả thực thi công cụ:")
                            print(f"   📄 Trả về: {last_msg.content}")
                            print("⏳ [Hệ thống: Tự động chuyển tiếp kết quả về LLM để tiếp tục suy luận...]")
            
            # Đồng bộ lại state đầy đủ từ checkpointer sau khi kết thúc chu trình
            state = graph.get_state(config).values
            
            print("====================================================================\n")
            
        except KeyboardInterrupt:
            print("\nTạm biệt bạn nhé! 👋")
            break
        except Exception as e:
            print(f"\n❌ Đã xảy ra lỗi hệ thống: {e}\n")

if __name__ == "__main__":
    main()
