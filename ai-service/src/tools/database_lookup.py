import json
from langchain_core.tools import tool
from src.config.database import supabase

@tool
def lookup_word_in_database(word: str, user_id: str = "00000000-0000-0000-0000-000000000000") -> str:
    """
    Tra cứu thông tin chi tiết của một từ vựng tiếng Anh trong cơ sở dữ liệu học tập của học viên (bao gồm phiên âm, nghĩa tiếng Việt, định nghĩa tiếng Anh, câu ví dụ, loại từ, cụm từ collocation, từ liên quan và ghi chú).
    Chỉ trả về kết quả nếu từ vựng thuộc về bộ từ mẫu của hệ thống hoặc bộ từ cá nhân của chính học viên đó.
    
    Args:
        word: Từ vựng tiếng Anh cần tra cứu (ví dụ: 'ubiquitous').
        user_id: ID người dùng của học viên (định dạng UUID).
    """
    word_cleaned = word.lower().strip()
    try:
        # 1. Truy vấn toàn bộ thông tin thẻ từ vựng khớp với từ (không giới hạn bản ghi đầu tiên)
        card_res = supabase.table("cards") \
            .select("id, deck_id, word, pronunciation, meaning, description_en, example, word_type, collocation, related_words, note") \
            .ilike("word", word_cleaned) \
            .execute()
            
        if not card_res.data:
            return json.dumps({"found": False}, ensure_ascii=False)
            
        # 2. Duyệt qua từng thẻ từ để kiểm tra quyền sở hữu của bộ từ chứa nó
        for card in card_res.data:
            card_id = card["id"]
            deck_id = card["deck_id"]
            
            # Truy vấn thông tin bộ từ (deck) chứa thẻ này
            deck_res = supabase.table("decks") \
                .select("name, target_goal, user_id") \
                .eq("id", deck_id) \
                .execute()
                
            if not deck_res.data:
                continue
                
            deck = deck_res.data[0]
            deck_owner = deck.get("user_id")
            
            # ĐIỀU KIỆN SỞ HỮU CHẶT CHẼ:
            # - Hoặc deck_owner là None (Bộ từ vựng mẫu của hệ thống dành cho tất cả mọi người)
            # - Hoặc deck_owner trùng với user_id của người hỏi hiện tại (Bộ từ vựng cá nhân của họ)
            if deck_owner is None or deck_owner == user_id:
                result = {
                    "found": True,
                    "word_id": card_id,
                    "word": card["word"],
                    "pronunciation": card["pronunciation"],
                    "meaning": card["meaning"],
                    "description_en": card["description_en"],
                    "example": card["example"],
                    "word_type": card.get("word_type"),
                    "collocation": card.get("collocation"),
                    "related_words": card.get("related_words"),
                    "note": card.get("note"),
                    "deck_name": deck.get("name"),
                    "target_goal": deck.get("target_goal"),
                    "is_system_deck": deck_owner is None
                }
                return json.dumps(result, ensure_ascii=False)
                
        # Nếu đã duyệt qua tất cả thẻ từ trùng tên mà không có thẻ nào thuộc sở hữu hợp lệ
        return json.dumps({"found": False}, ensure_ascii=False)
        
    except Exception as e:
        print(f"Lỗi khi tra cứu cơ sở dữ liệu cho từ '{word}': {e}")
        return json.dumps({"found": False, "error": str(e)}, ensure_ascii=False)
