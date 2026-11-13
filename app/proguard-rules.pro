# 매니페스트에 선언된 서비스·리시버·액티비티는 R8이 자동으로 보존한다.
# 검증 기록에서 볼 수 있도록 TestKind 이름(Intent extra로 전달)을 유지한다.
-keepclassmembers enum app.sureureuk.service.TestKind { *; }
