# src/tools/create_flashcards.py

import json
import uuid
from langchain_core.tools import tool
from src.config.database import supabase

@tool
def get_user_decks(user_id: str) -> str:
    """
    Truy vấn danh sách các bộ từ vựng cá nhân hiện có của học viên trên Supabase.
    Thông tin này giúp hiển thị danh sách các Deck để học viên lựa chọn lưu thẻ mới vào.
    
    Args:
        user_id: ID người dùng của học viên (định dạng UUID).
    """
    try:
        res = supabase.table("decks") \
            .select("id, name, target_goal") \
            .eq("user_id", user_id) \
            .execute()
            
        if not res.data:
            return json.dumps({"found": False, "decks": []}, ensure_ascii=False)
            
        return json.dumps({"found": True, "decks": res.data}, ensure_ascii=False)
    except Exception as e:
        print(f"Lỗi khi lấy danh sách decks của user '{user_id}': {e}")
        return json.dumps({"found": False, "error": str(e)}, ensure_ascii=False)

@tool
def create_flashcards_in_database(words: list, user_id: str, deck_name: str) -> str:
    """
    Chèn danh sách các từ vựng mới đã trích xuất (đầy đủ 9 trường thông tin thẻ khớp hoàn toàn với Supabase)
    vào bộ bài học cá nhân do học viên lựa chọn (nếu bộ từ chưa tồn tại, tự động tạo mới bộ từ cá nhân).
    
    Args:
        words: Danh sách (list) các từ vựng đã được biên soạn. Mỗi từ là dict có 9 trường thông tin.
        user_id: ID người dùng của học viên (định dạng UUID).
        deck_name: Tên bộ bài học cá nhân học viên muốn lưu vào (ví dụ: 'Từ vựng Công nghệ').
    """
    try:
        # 1. Nhận dạng danh sách từ vựng (chấp nhận cả list trực tiếp hoặc string JSON nếu LLM gửi nhầm)
        words_list = []
        if isinstance(words, str):
            try:
                words_list = json.loads(words)
            except Exception:
                cleaned_str = words.strip()
                if cleaned_str.startswith("```"):
                    lines = cleaned_str.split("\n")
                    if lines[0].startswith("```json") or lines[0].startswith("```"):
                        cleaned_str = "\n".join(lines[1:-1]).strip()
                words_list = json.loads(cleaned_str)
        elif isinstance(words, list):
            words_list = words
        else:
            words_list = [words]

        if not isinstance(words_list, list):
            words_list = [words_list]
            
        if not words_list:
            return "LỖI: Không tìm thấy từ vựng nào hợp lệ để chèn."

        deck_name_cleaned = deck_name.strip()
        deck_id = None
        
        # 2. Truy vấn xem người dùng đã có deck cá nhân nào tên trùng khớp chưa (so khớp không phân biệt hoa thường)
        decks_res = supabase.table("decks") \
            .select("id, name") \
            .eq("user_id", user_id) \
            .execute()
            
        existing_deck = None
        if decks_res.data:
            for d in decks_res.data:
                if d["name"].lower() == deck_name_cleaned.lower():
                    existing_deck = d
                    break
                    
        # 3. Nếu ĐÃ CÓ deck trùng tên -> lấy deck_id
        if existing_deck:
            deck_id = existing_deck["id"]
            print(f"📁 Tìm thấy bộ từ cá nhân có sẵn: '{deck_name_cleaned}' (ID: {deck_id})")
        else:
            # Nếu CHƯA CÓ -> INSERT tạo mới 1 deck cá nhân cho người này
            new_deck_id = str(uuid.uuid4())
            new_deck = {
                "id": new_deck_id,
                "name": deck_name_cleaned,
                "icon": "📁",
                "tag": "Cá nhân",
                "total_words": len(words_list),
                "order_index": 1,
                "target_goal": "IELTS",  # Mặc định IELTS, backend sẽ tự đồng bộ
                "user_id": user_id
            }
            print(f"🆕 Tiến hành tạo mới bộ từ cá nhân: '{deck_name_cleaned}'...")
            supabase.table("decks").insert(new_deck).execute()
            deck_id = new_deck_id
            
        # 4. INSERT các thẻ từ vựng với đầy đủ 9 trường thông tin khớp hoàn toàn với Supabase
        cards_to_insert = []
        def clean_val(val) -> str:
            if val is None:
                return ""
            if isinstance(val, list):
                return ", ".join(str(item).strip() for item in val).strip()
            return str(val).strip()

        cards_to_insert = []
        for word_data in words_list:
            card_id = str(uuid.uuid4())
            new_card = {
                "id": card_id,
                "deck_id": deck_id,
                "word": clean_val(word_data.get("word")),
                "pronunciation": clean_val(word_data.get("pronunciation")),
                "meaning": clean_val(word_data.get("meaning")),
                "description_en": clean_val(word_data.get("description_en")),
                "example": clean_val(word_data.get("example")),
                "word_type": clean_val(word_data.get("word_type")),
                "collocation": clean_val(word_data.get("collocation")),
                "related_words": clean_val(word_data.get("related_words")),
                "note": clean_val(word_data.get("note"))
            }
            cards_to_insert.append(new_card)
            
        print(f"📥 Đang chèn ngầm {len(cards_to_insert)} thẻ từ vựng vào bảng cards trên Supabase...")
        supabase.table("cards").insert(cards_to_insert).execute()
        
        # 5. Cập nhật lại tổng số từ vựng (total_words) trong deck cá nhân
        # Lấy số từ hiện tại
        deck_cards_res = supabase.table("cards").select("id", count="exact").eq("deck_id", deck_id).execute()
        actual_count = deck_cards_res.count if hasattr(deck_cards_res, "count") else len(cards_to_insert)
        if actual_count is None:
            actual_count = len(cards_to_insert)
            
        supabase.table("decks").update({"total_words": actual_count}).eq("id", deck_id).execute()
        
        return f"THÀNH CÔNG: Đã lưu {len(cards_to_insert)} từ vựng thành công vào bộ bài học '{deck_name_cleaned}' của bạn trên Supabase! 🎉"
        
    except Exception as e:
        print(f"Lỗi khi ghi ngầm thẻ từ vựng vào Supabase: {e}")
        return f"LỖI: Gặp sự cố khi ghi dữ liệu vào Supabase: {str(e)}"
