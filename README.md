[ Capsule ]
OCR과 데이터베이스를 활용하여 처방전을 사진 한장으로 간편하게 등록하고 관리할 수 있는 어플리케이션입니다.

![image](https://github.com/user-attachments/assets/3e14fd52-1469-4fef-95c2-12a2ac47b6d8)

[프로젝트 소개]

프로젝트 이름 : Capsule

프로젝트 주제 : 처방전 인식 기능을 활용한 처방전 관리 앱 개발

개발 기간 : 2024.09.02 ~ 2025.05.20

개발 인원: 김명환, 손준영, 이동훈, 황석양


server 3.7.py를 터미널에서 실행해 서버 시작.
서버 실행전 pip install fastapi pydantic transformers torch uvicorn로 패키지 설치

Llama2ApiClient.java 에서
private static final String API_URL의 값을 서버를 실행하는 컴퓨터의 공인 IP주소 값으로 수정해야함.

[수행 과정 ]
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

