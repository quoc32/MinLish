from typing import TypedDict, List, Optional, Annotated
from langchain_core.messages import BaseMessage
from langgraph.graph.message import add_messages
from pydantic import BaseModel, Field

class ExtractedWordSchema(BaseModel):
    word: str = Field(description="Từ tiếng Anh gốc ở dạng nguyên thể, viết thường, ví dụ: 'catalyst'")
    pronunciation: str = Field(description="Phiên âm chuẩn UK/US, ví dụ: '/ˈkæt.əl.ɪst/'")
    meaning: str = Field(description="Nghĩa tiếng Việt chuẩn và ngắn gọn nhất theo ngữ cảnh đoạn văn, ví dụ: 'chất xúc tác'")
    description_en: str = Field(description="Định nghĩa tiếng Anh đơn giản, dễ hiểu")
    example: str = Field(description="Biên soạn một câu ví dụ tiếng Anh mới tinh thật đơn giản, ngắn gọn và có ý nghĩa thực tế để học viên dễ học thuộc lòng có chứa từ vựng này (bôi đậm từ vựng)")
    word_type: str = Field(description="Loại từ: Noun, Verb, Adjective, Adverb, v.v., ví dụ: 'Noun'")
    collocation: str = Field(description="Cụm từ hay đi kèm thông dụng thực tế, ví dụ: 'chemical catalyst, catalyst for reform'")
    related_words: str = Field(description="Các từ đồng nghĩa, liên quan gần nhất, ví dụ: 'stimulus, instigator'")
    note: str = Field(description="Gợi ý ghi chú học tập hoặc bối cảnh từ vựng học thuật")

class ExtractionResultSchema(BaseModel):
    words: List[ExtractedWordSchema] = Field(description="Danh sách các từ vựng học thuật quan trọng được chọn lọc trích xuất (tối đa từ 3 đến 5 từ)")

class AgentState(TypedDict):
    user_id: str
    """ID của người dùng trên Supabase, dùng để truy vấn RAG và lưu thẻ."""
    
    target_goal: str
    """Mục tiêu học tập của người dùng (IELTS, TOEIC, Giao tiếp, THPT Quốc gia)."""
    
    messages: Annotated[List[BaseMessage], add_messages]
    """Lịch sử hội thoại đầy đủ. LangGraph sẽ tự động append tin nhắn mới nhờ add_messages."""
    
    intent: Optional[str]
    """Ý định thực tế của tin nhắn học viên (explain_word, extract_text, general_chat)."""
    
    current_word: Optional[str]
    """Từ vựng đơn lẻ đang thảo luận (Luồng 1 và Luồng 3)."""
    
    is_in_database: Optional[bool]
    """Từ vựng đang thảo luận đã có trong database của user chưa."""
    
    user_sentence: Optional[str]
    """Câu tiếng Anh do người học đặt thử để luyện tập."""
    
    grammar_feedback: Optional[dict]
    """Kết quả phân tích ngữ pháp, câu sửa lại và điểm số từ AI (Luồng 3)."""
    
    extracted_words: Optional[List[dict]]
    """Danh sách từ vựng trích xuất được từ đoạn văn bản dán vào (Luồng 2)."""
    
    pending_deck_name: Optional[str]
    """Tên bộ từ cá nhân học viên mong muốn lựa chọn để lưu trữ thẻ từ vựng trích xuất (Luồng 2)."""
    
    rag_context: Optional[dict]
    """Dữ liệu RAG chi tiết từ Supabase dùng để định hình bài giảng (Luồng 1)."""
    
    awaiting_sentence: Optional[bool]
    """Trạng thái chờ người học đặt câu tiếng Anh để kiểm tra ngữ pháp."""

