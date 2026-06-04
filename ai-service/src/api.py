# src/api.py

import os
from typing import Optional, List, Dict, Any
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel, Field
from langchain_core.messages import HumanMessage
from dotenv import load_dotenv

# Tải biến môi trường
load_dotenv()

# Import graph và checkpointer memory từ src.agent.graph
from src.agent.graph import graph, memory

app = FastAPI(
    title="MinLish AI Smart Tutor Service",
    description="FastAPI service serving the LangGraph AI Smart Tutor agent",
    version="1.0.0"
)

# Cấu hình CORS
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# ====================================================================
# ĐỊNH NGHĨA PAYLOAD ĐẦU VÀO / ĐẦU RA (SCHEMAS)
# ====================================================================

class ChatRequest(BaseModel):
    user_id: str = Field(description="ID người dùng trên Supabase (UUID)")
    message: str = Field(description="Tin nhắn của học viên")
    target_goal: str = Field(default="IELTS", description="Mục tiêu học tập hiện tại")

class TutorState(BaseModel):
    current_word: Optional[str] = None
    is_in_database: Optional[bool] = None
    awaiting_sentence: Optional[bool] = None
    intent: Optional[str] = None
    extracted_words: Optional[List[Dict[str, Any]]] = None
    grammar_feedback: Optional[Dict[str, Any]] = None
    user_sentence: Optional[str] = None
    pending_deck_name: Optional[str] = None
    rag_context: Optional[Dict[str, Any]] = None

class ChatResponse(BaseModel):
    success: bool
    response: str

class ResetRequest(BaseModel):
    user_id: str = Field(description="ID người dùng trên Supabase (UUID)")

# ====================================================================
# CÁC ROUTE API
# ====================================================================

@app.get("/health")
async def health_check():
    """
    Endpoint kiểm tra trạng thái hoạt động của server.
    """
    return {
        "status": "healthy",
        "service": "MinLish AI Tutor API",
        "version": "1.0.0"
    }

@app.post("/api/tutor/chat", response_model=ChatResponse)
async def chat_with_tutor(payload: ChatRequest):
    """
    Nhận tin nhắn từ học viên, chạy đồ thị LangGraph và trả về phản hồi kèm state.
    """
    try:
        user_id_cleaned = payload.user_id.strip()
        message_cleaned = payload.message.strip()
        target_goal_cleaned = payload.target_goal.strip()

        if not user_id_cleaned or not message_cleaned:
            raise HTTPException(status_code=400, detail="user_id và message không được để trống.")

        # Định cấu hình LangGraph checkpointer thread_id
        config = {"configurable": {"thread_id": user_id_cleaned}}

        # Lấy trạng thái hiện tại từ checkpointer
        state_snapshot = graph.get_state(config)

        # Nếu chưa có trạng thái lưu (tin nhắn đầu tiên), khởi tạo trạng thái đầy đủ giống hệt main.py
        if not state_snapshot.values:
            input_state = {
                "user_id": user_id_cleaned,
                "target_goal": target_goal_cleaned,
                "messages": [HumanMessage(content=message_cleaned)],
                "current_word": None,
                "is_in_database": None,
                "user_sentence": None,
                "extracted_words": [],
                "awaiting_sentence": False,
                "rag_context": {"found": False}
            }
        else:
            # Nếu đã có trạng thái, chỉ cần gửi tin nhắn mới và cập nhật target_goal
            input_state = {
                "user_id": user_id_cleaned,
                "target_goal": target_goal_cleaned,
                "messages": [HumanMessage(content=message_cleaned)]
            }

        # Gọi đồ thị LangGraph thực thi
        # ainvoke hoặc invoke đều được, LangGraph hỗ trợ cả hai
        result = graph.invoke(input_state, config)

        # Trích xuất tin nhắn phản hồi cuối cùng của AI
        messages = result.get("messages", [])
        if not messages:
            raise HTTPException(status_code=500, detail="Không nhận được phản hồi nào từ Gia sư AI.")

        last_message = messages[-1]
        response_text = last_message.content

        # Trích xuất các trạng thái cần thiết để gửi về Client
        state_response = TutorState(
            current_word=result.get("current_word"),
            is_in_database=result.get("is_in_database"),
            awaiting_sentence=result.get("awaiting_sentence", False),
            intent=result.get("intent"),
            extracted_words=result.get("extracted_words"),
            grammar_feedback=result.get("grammar_feedback"),
            user_sentence=result.get("user_sentence"),
            pending_deck_name=result.get("pending_deck_name"),
            rag_context=result.get("rag_context")
        )

        return ChatResponse(
            success=True,
            response=response_text
        )

    except Exception as e:
        print(f"Lỗi khi xử lý chat với Tutor: {e}")
        raise HTTPException(status_code=500, detail=f"Sự cố hệ thống AI: {str(e)}")

@app.post("/api/tutor/reset")
async def reset_tutor_state(payload: ResetRequest):
    """
    Xóa sạch lịch sử hội thoại và đặt lại trạng thái chat về mặc định cho một người dùng.
    """
    try:
        user_id_cleaned = payload.user_id.strip()
        if not user_id_cleaned:
            raise HTTPException(status_code=400, detail="user_id không được để trống.")

        # 1. Xóa checkpoints trong MemorySaver
        cleared_count = 0
        if hasattr(memory, "storage") and isinstance(memory.storage, dict):
            # Duyệt và xóa mọi key khớp với user_id
            keys_to_delete = []
            for k in memory.storage.keys():
                # Key có thể là chuỗi hoặc tuple chứa thread_id
                if k == user_id_cleaned or (isinstance(k, tuple) and user_id_cleaned in k) or str(k).count(user_id_cleaned) > 0:
                    keys_to_delete.append(k)
            
            for k in keys_to_delete:
                del memory.storage[k]
                cleared_count += 1
        
        # 2. Ghi đè trạng thái trống để đề phòng checkpointer vẫn còn cache
        config = {"configurable": {"thread_id": user_id_cleaned}}
        graph.update_state(
            config,
            values={
                "messages": [],
                "current_word": None,
                "is_in_database": None,
                "user_sentence": None,
                "grammar_feedback": None,
                "extracted_words": [],
                "pending_deck_name": None,
                "rag_context": {"found": False},
                "awaiting_sentence": False
            }
        )

        return {
            "success": True,
            "message": f"Đã reset thành công trạng thái gia sư cho học viên '{user_id_cleaned}'.",
            "checkpoints_cleared": cleared_count
        }

    except Exception as e:
        print(f"Lỗi khi reset trạng thái Tutor: {e}")
        raise HTTPException(status_code=500, detail=f"Không thể reset trạng thái: {str(e)}")

if __name__ == "__main__":
    import uvicorn
    # Mặc định chạy ở port 8000 khi chạy trực tiếp file này
    uvicorn.run("src.api:app", host="0.0.0.0", port=8000, reload=True)
