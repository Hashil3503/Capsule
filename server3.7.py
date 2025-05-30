from fastapi import FastAPI
from pydantic import BaseModel
from transformers import AutoTokenizer, AutoModelForCausalLM, BitsAndBytesConfig
import difflib
import torch
import re
import os

app = FastAPI()

MODEL_NAME = "MLP-KTLim/llama-3-Korean-Bllossom-8B"
print("모델 및 토크나이저 로딩 중...")
tokenizer = AutoTokenizer.from_pretrained(MODEL_NAME)
bnb_config = BitsAndBytesConfig(
    load_in_4bit=True,
    bnb_4bit_use_double_quant=True,
    bnb_4bit_quant_type="nf4",
    bnb_4bit_compute_dtype=torch.float16
)
model = AutoModelForCausalLM.from_pretrained(
    MODEL_NAME,
    torch_dtype=torch.float16,
    device_map={"": 0},
    quantization_config=bnb_config
)
model.eval()

FIELDS = ["성분", "효과", "제형", "주의사항"]

with open(r'C:\Users\kimmh\AndroidStudioProjects\Capsule 2.4\app\src\main\assets\medicine_info\약품 정보.txt', 'r', encoding='utf-8') as f:
    raw_drug_data = f.read().split('\n\n\n')

drug_info_dict = {}
for entry in raw_drug_data:
    match = re.search(r'#(.+)', entry)
    if match:
        name = match.group(1).strip()
        drug_info_dict[name] = entry.strip()

class QueryRequest(BaseModel):
    prompt: str
    max_new_tokens: int = 160
    temperature: float = 0.3
    top_p: float = 0.9

def parse_query(query: str):
    fields = [f for f in FIELDS if f in query]
    name_candidates = [name for name in drug_info_dict.keys() if name in query]
    name = name_candidates[0] if name_candidates else None

    print(f"\n[Query Parsing] 약품명: {name}, 필드들: {fields}")
    return name, fields

def extract_field_text(drug_name, field):
    entry = drug_info_dict.get(drug_name)
    if not entry:
        return f"{drug_name}에 대한 정보가 없습니다."
    pattern = f"@{field}:(.*?)(@|$)"
    match = re.search(pattern, entry, re.DOTALL)
    if match:
        return match.group(1).strip()
    else:
        return f"{drug_name}에 대한 {field} 정보가 없습니다."

def retrieve_context(query: str) -> str:
    name, fields = parse_query(query)
    if not name:
        return "관련 약품 정보를 찾을 수 없습니다."
    if not fields:
        return drug_info_dict.get(name, "관련 약품 정보를 찾을 수 없습니다.")

    context_blocks = [f"{name}의 {field}:\n{extract_field_text(name, field)}" for field in fields]
    return "\n\n".join(context_blocks)

def clean_special_characters(text: str) -> str:
    # 1. 문장 끝에 반복되는 특수문자 제거 (예: ...., !!!, ???, ~~~ 등)
    text = re.sub(r'[\.\!\?\~]{2,}$', '', text).strip()
    # 2. 문장 중간에 과도한 특수문자 반복은 "..."로 정리
    text = re.sub(r'[\.\!\?\~]{3,}', '...', text)
    return text.strip()

def correct_korean_ingredient_names(response: str, context: str) -> str: #모델이 한글 성분명을 자꾸 오타내서 추가한 함수
    # Context에서 한글 성분명 추출 (-으로 시작해서 ) 또는 \n 또는 문자열 끝)
    korean_ingredients = re.findall(r'-([가-힣]+)(?:\(|\n|$)', context)

    # 응답에서 한글 단어 추출 (2글자 이상 단어만 교정 대상으로)
    korean_words = re.findall(r'\b[가-힣]{2,}\b', response)

    for word in korean_words:
        best_match = None
        best_ratio = 0
        for ingredient in korean_ingredients:
            ratio = difflib.SequenceMatcher(None, word, ingredient).ratio()
            if ratio > best_ratio:
                best_ratio = ratio
                best_match = ingredient
        # 80% 이상 유사할 때만 교정
        if best_ratio >= 0.7 and best_match and word != best_match:
            response = re.sub(rf'\b{word}\b', best_match, response)
    return response


def generate_response(prompt: str, max_new_tokens: int = 160, temperature: float = 0.3, top_p: float = 0.9) -> str:
    print("\n==================== [PROMPT RECEIVED] ====================")
    print(f"Prompt: {prompt}")

    context = retrieve_context(prompt)
    
    # 약품 정보 없으면 LLM 호출 없이 고정 응답 바로 반환
    if context == "관련 약품 정보를 찾을 수 없습니다.":
        return "죄송합니다. 요청하신 약품에 대한 정보를 찾을 수 없습니다."

    print("------------------------------------------------------------")
    print(f"Context:\n{context}")
    print("============================================================\n")

    formatted_prompt = (
        f"Context:\n{context}\n\n"
        f"Instruction:\n{prompt.strip()}\n\n"
        "질문에 대하여 위 Context에 포함된 정보를 자연스러운 문장으로 요약하여 전달하십시오. Context에 없는 정보는 절대 추가하지 마십시오.\n"
        "Context의 단어와 문장은 절대 변경하지 말고 그대로 사용하십시오. 새로운 문장이나 표현을 만들지 마십시오. 예: '기침 중추에 작용하여 기침 반사를 억제'는 반드시 그대로 사용하십시오. \n"
        "약품 이름과 성분명은 반드시 Context의 내용을 그대로 사용하십시오. 철자를 변경하거나 유사하게 작성하지 마십시오.\n"
        "특수 문자를 사용하지 마세요.\n"
        "답변 자체의 내용과 관련 없는 부가적인 설명은 하지마시오\n"
        "정보의 시점, 작성자 정보 등을 절대 명시하지 마시오. \n"
        "더 쓸 내용이 없으면 억지로 내용을 채우지 마시오.\n"
        "반드시 한글로만 간결하게 답변하시오.\n\n"
        f"질문: {prompt.strip()}\n"
        "Answer:"
    )

    inputs = tokenizer(formatted_prompt, return_tensors="pt").to(model.device)
    outputs = model.generate(
        **inputs,
        max_new_tokens=max_new_tokens,
        do_sample=True,
        temperature=temperature,
        top_p=top_p,
        pad_token_id=tokenizer.eos_token_id,
        eos_token_id=tokenizer.eos_token_id,
        repetition_penalty=1.2  # 반복 방지
    )

    raw_response = tokenizer.decode(outputs[0], skip_special_tokens=True, errors="ignore")
    final_response = raw_response.strip()

    print("\n==================== [RESPONSE GENERATED] =================")
    print(f"Response: {final_response}")
    print("============================================================\n")

    if "Answer:" in final_response:
        cleaned_response = final_response.split("Answer:")[-1].strip()
    else:
        cleaned_response = final_response.strip()

    # 문장 중복 제거
    sentences = re.split(r'(?<=[.!?])\s+', cleaned_response)
    unique_sentences = []
    seen = set()
    for sentence in sentences:
        sentence_clean = sentence.strip()
        if sentence_clean and sentence_clean not in seen and len(sentence_clean) > 3:
            unique_sentences.append(sentence_clean)
            seen.add(sentence_clean)

    if unique_sentences:
        cleaned_response = ' '.join(unique_sentences).strip()
        if not re.search(r'[.!?]$', cleaned_response):
            sentences = re.split(r'(?<=[.!?])\s+', cleaned_response)
            cleaned_response = ' '.join(sentences[:-1]).strip() if len(sentences) > 1 else sentences[0].strip()
    else:
        cleaned_response = cleaned_response or "죄송합니다. 답변을 생성하지 못했습니다."

    # 특수문자 반복 제거
    cleaned_response = clean_special_characters(cleaned_response)
    
    # -END OF ANSWER- 이후 내용 제거. 이상하게 시킨적도 없는데 자꾸 이 문구로 응답을 끝맺어서 추가함.
    end_marker = "-END OF ANSWER-"
    if end_marker in cleaned_response:
        cleaned_response = cleaned_response.split(end_marker)[0].strip()

    # 한글 성분명 오타 교정
    cleaned_response = correct_korean_ingredient_names(cleaned_response, context)

    return cleaned_response

@app.post("/chat")
async def chat_endpoint(request: QueryRequest):
    response = generate_response(
        request.prompt,
        max_new_tokens=request.max_new_tokens,
        temperature=request.temperature,
        top_p=request.top_p
    )
    return {"generated_text": response}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
