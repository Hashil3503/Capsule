server 3.7.py를 터미널에서 실행해 서버 시작.
서버 실행전 pip install fastapi pydantic transformers torch uvicorn로 패키지 설치

Llama2ApiClient.java 에서
private static final String API_URL의 값을 서버를 실행하는 컴퓨터의 공인 IP주소 값으로 수정해야함.
