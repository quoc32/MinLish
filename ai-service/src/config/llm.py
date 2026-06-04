import os
from langchain_openai import ChatOpenAI
from dotenv import load_dotenv

# Tải biến môi trường
load_dotenv()

# Khởi tạo mô hình LLM phân loại ý định siêu tốc (Lightweight Router Model)
llm_router = ChatOpenAI(
    openai_api_base=os.getenv("LLM_BASE_URL", "https://api.groq.com/openai/v1"),
    openai_api_key=os.getenv("LLM_API_KEY"),
    model_name="llama-3.1-8b-instant",
    temperature=0.0
) 

# Khởi tạo mô hình LLM giảng dạy & sửa lỗi chuyên sâu (Heavyweight Reasoning Model)
llm = ChatOpenAI(
    openai_api_base=os.getenv("LLM_BASE_URL", "https://api.groq.com/openai/v1"),
    openai_api_key=os.getenv("LLM_API_KEY"),
    model_name="llama-3.3-70b-versatile",
    temperature=0.3
)
 