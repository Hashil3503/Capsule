# 캡슐팀 대구가톨릭대학교 캡스톤디자인 과제
[ Capsule ]

OCR과 데이터베이스를 활용하여 처방전을 사진 한장으로 간편하게 등록하고 관리할 수 있는 어플리케이션입니다.

![image](https://github.com/user-attachments/assets/3e14fd52-1469-4fef-95c2-12a2ac47b6d8)

[프로젝트 소개]

프로젝트 이름 : Capsule

프로젝트 주제 : 처방전 인식 기능을 활용한 처방전 관리 앱 개발

개발 기간 : 2024.09.02 ~ 2025.05.20

개발 인원: 김명환, 손준영, 이동훈, 황석양

[역할 분담]

김명환(팀장) : 데이터베이스 설계 및 구축, 프로젝트 병합, 트러블슈팅
이동훈 : OCR, LLM, 서버 구축
손준영 : 복약 알람 기능 구현, UI/UX
황석양 : 혈당 및 혈압 그래프 기능 구현

[수행 과정]
- 프로젝트 목표 설정 및 기능 구조도와 UI 설계도 작성
- 데이터베이스 설계 및 구축 (처방전, 의약품 정보, 혈당/혈압 데이터 등 저장)
- 의약품 정보 수집 및 정제
- 처방전 인식 기능 구현
- 혈당/혈압 관리 기능 구현
- QnA 챗봇 기능 구현
- 반복적인 테스트를 통한 버그 수정

[상세 내용]

![image](https://github.com/user-attachments/assets/20f02c38-d094-4a61-9af1-ae7f59ea17fd)

주요 기능 구현
- OCR 기술을 활용한 처방전 인식 및 약품명 추출
- 의약품 정보 조회 및 복약 알람 기능
- 사용자가 측정한 혈당/혈압 데이터 입력 및 목록/그래프 형태로 조회
- QnA 챗봇을 통한 의약품 관련 간단한 질의응답

[결과 및 기대 효과]

![image](https://github.com/user-attachments/assets/73a12870-3f60-487e-861b-0ff219acd5b2)


OCR 기반의 편리한 처방전 등록으로 사용자 접근성 향상
복약 관리와 건강 데이터 관리 기능으로 사용자 건강 관리 능력 향상
QnA 챗봇을 통한 빠른 의약품 정보 확인
혈당/혈압 추이 그래프를 통한 건강 상태 모니터링 가능

---------------------------------------------------------------------
[개발 환경]

개발 도구 : Android Studio

언어 : JAVA, Python

DBMS : SQLite

라이브러리 : Room, Caemera X, OKHttp, OpenCSV

API : ML Kit, Fast API

LLM : MLP-KTLim/llama-3-Korean-Bllossom-8B

(https://huggingface.co/MLP-KTLim/llama-3-Korean-Bllossom-8B)

[기능 설명]
1. 메인화면

![image](https://github.com/user-attachments/assets/5e60de29-d1e6-4eef-a847-5e5fe80d89c7)

- 앱 실행 시 데이터베이스 파일 유무를 검사하여 데이터베이스 초기화. 
(asset 폴더의 csv 파일과 txt파일을 읽어 의약품 정보가 담긴 MedicineTable 테이블과 MedicineName 테이블에 레코드를 저장)
- 가장 오래 복용 중인 처방전의 의약품 정보를 슬라이드 형태로 제공

2. 처방전 관리

![image](https://github.com/user-attachments/assets/30c5c319-9037-46de-9f9b-869962c02a56)

- 직접 촬영 또는 갤러리에서 처방전 사전을 선택 (사진이 없는 경우 수동 입력으로 처방전 등록 가능)
- OCR 기술과 문자열 후처리를 통해 처방전 사진에서 의약품 이름을 추출
- 처방전 등록 화면에서 전체 복용 일수와 투약 정보를 입력

![image](https://github.com/user-attachments/assets/d26a6773-f1e3-4e17-b379-1ca327906918)

- 처방전 조회 화면에서 처방전 목록 확인 가능. (처방전 id, 등록 일자, 의약품 목록, 복용 일수)
- "자세히" 버튼을 터치하여 처방전에 포함된 의약품의 상세 정보를 확인 가능
- "알람" 버튼을 터치하여 복약 알람 설정가능

3. 복약 알람

![image](https://github.com/user-attachments/assets/2798f765-b82e-4e7e-85c5-52e1fc1a7711)

- 처방전 별 복약 알람 설정 가능
- 복용 시작일과 복용 종료일을 설정하고 복용 시간대를 선택하여 일괄적으로 복약 알람을 등록할 수 있음.
- 알람이 울릴 경우 "알람 끄기" 버튼을 터치하여 알람을 종료할 수 있음.
- 직접 알람을 종료한 경우, 복약을 완료한 것으로 간주하여 알람 목록 화면에서 자동으로 복약 체크가 가능.
- 처방전 목록에서 알람 목록을 확인할 경우, 해당 처방전의 알람 목록만 출력.
- 메인 화면에서 알람 목록을 확인하는 경우, 설정된 모든 알람 목록이 출력.

4. 혈당 / 혈압 관리

![image](https://github.com/user-attachments/assets/3b10e0c3-fbd0-4d4c-920f-a02dde485d61)

- "혈당 기록", "혈압 기록" 버튼을 터치하여 사용자가 자가 측정한 혈당 및 혈압 값을 입력할 수 있음.
- "기록 조회" 버튼을 터치하여 입력한 정보를 목록 형태로 확인할 수 있음.
- 기록 조회 화면에서 혈당/혈압 카테고리를 선택하여 원하는 항목만 확인할 수 있으며,
혈당과 혈압치가 정상 범주 밖에 있는 경우, 붉은 색 글씨로 표시됨.

![image](https://github.com/user-attachments/assets/adb24c85-7bdf-43ba-a31e-84c0eb8cf21b)

- "그래프" 버튼을 터치하여 사용자가 입력한 건강 정보를 그래프 형태로 조회할 수 있음.
- 그래프에 표시되는 각 좌표의 Y값은 '그날 측정한 가장 마지막 기록'을 기준으로 표시됨.
- 비정상 값 발견 시 그래프에 마젠타 색상의 점으로 나타나며, 화면 하단의 "비정상 일자"에 해당 값이 입력된 날짜가 나열됨. 

5. 챗봇

![image](https://github.com/user-attachments/assets/63cc4e5c-541e-4101-8a26-9305db03e00b)

- LLM 서버 실행 파일은 Python 언어로 작성되었음
- fast api를 통해 어플리케이션이 서버와 연결하게 되며, 컴파일 이전에 지정된 ip주소와 포트로 자동으로 접속하게 됨
- LLM 모델은 사용자가 전송한 메시지를 전달받아 사전에 작성된 instruction에 의거하여 답변을 형성하게됨.
- 의약품에 대한 질문만 가능하며, 서버측에 가지고 있지 않은 정보와 질문 형식에 대해서는 "정보가 없습니다"라는 문구를 출력.
- 질문 형식은 [의약품이름], [성분 or 제형 or 효과 or 주의사항] 키워드가 포함된 모든 형식을 지원.
- 현재 응답 토큰의 길이는 약 160, 답변 속도는 약 3~4초 정도

---------------------------------------------------------------------
[LLM 서버 사용 시 주의사항]


server 3.7.py를 터미널에서 실행해 서버 시작.
서버 실행전 pip install fastapi pydantic transformers torch uvicorn로 패키지 설치

Llama2ApiClient.java 에서
private static final String API_URL의 값을 서버를 실행하는 컴퓨터의 공인 IP주소 값으로 수정해야함.


server3.7.py를 편집하여 30행의 약품 정보.txt경로를 최신화해주어야함.

----------------------------------------------------------------------
[설명 영상]

https://www.canva.com/design/DAGonziarxI/rrppWT2pw8fN2FtMmcs29A/watch?utm_content=DAGonziarxI&utm_campaign=designshare&utm_medium=link2&utm_source=uniquelinks&utlId=hdece8e6a04
