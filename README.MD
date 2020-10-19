# Where I am
### 개요
스마트폰의 여러 센서를 이용하여 초기 위치로부터의 위치 변화를 감지해 자신의 위치를 확인할 수 있고 더 나아가 다른 사람과 자신의 위치를 공유할 수 있는 서비스를 개발하기 위한 프로젝트

### 개발 동기
대다수 위치 기반 서비스는 GPS를 사용한다. 하지만 실내 환경의 GPS 사용은 제한이 있다. 이에 우리팀은 `PDR` 기법을 이용하여 실내에서 사용자의 위치를 확인할 수 있는 서비스를 개발하고자 했다.
> PDR
`Pedestrian Dead Reckoning`의 약자로 각종 센서를 통해 사용자의 이동 속도, 방향 등 이동 정보를 파악하여 이전의 위치로부터 현재의 위치를 계산하는 기술을 말한다.

### 애플리케이션 화면
- Login

  ![Login](/MarkDownImages/login.PNG)
  
- Main

  ![Main](/MarkDownImages/main.PNG)
  
- Demo

  ![Demo](/MarkDownImages/demo.gif)

### 역할
- 임태현: Server, PDR
- 지휘찬: Android Client, PDR
- 류동현: PDR

### 사용 라이브러리 & API
- Kakao Login
- Kakao Link
- Retrofit
- Gson
- [Glide](https://github.com/bumptech/glide)
- [zxing](https://github.com/zxing/zxing)
- [Socket.IO-client Java](https://github.com/socketio/socket.io-client-java)
- Firebase Cloud Messaging
