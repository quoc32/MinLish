import os
from supabase import create_client, Client
from dotenv import load_dotenv

# Tải biến môi trường từ file .env
load_dotenv()

supabase_url = os.getenv("SUPABASE_URL")
supabase_key = os.getenv("SUPABASE_ANON_KEY")

if not supabase_url or not supabase_key:
    raise ValueError("LỖI: SUPABASE_URL hoặc SUPABASE_ANON_KEY bị thiếu trong tệp .env")

# Khởi tạo Supabase client dùng chung
supabase: Client = create_client(supabase_url, supabase_key)

print("Đã kết nối thành công tới Supabase Database! 🚀")
