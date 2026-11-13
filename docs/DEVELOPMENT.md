# 개발·배포 안내

## 구조

| 경로 | 내용 |
|---|---|
| `service/SleepTimerService.kt` | 타이머 본체 (포그라운드 서비스, 정확한 알람 백업, 마무리 순서) |
| `media/` | 볼륨 줄이기(`VolumeFader`), 재생 정지(`MediaStopper`), 흔들기 감지 |
| `screen/ScreenController.kt` | 화면 꺼짐 시간을 잠시 줄였다가 복원 |
| `overlay/FadeOverlay.kt` | 마지막 구간에 영상 위에 뜨는 막대 |
| `tile/SleepTileService.kt` | 빠른 설정 타일 |
| `update/Updates.kt` | GitHub Releases 최신 버전 확인 (github 배포판만) |
| `ui/` | Compose 화면 (홈, 설정, 권한 안내, 검증) |

기기 검증 방법은 [VERIFY.md](VERIFY.md)를 보세요.

## 배포판

| 배포판 | 배포처 | 인터넷 | 업데이트 |
|---|---|---|---|
| `github` | GitHub Releases | 있음 (새 버전 확인 전용) | 앱이 알리고 릴리스 페이지를 열어줌 |
| `play` | 플레이 스토어 | 없음 | 플레이 스토어 |

Play 프로텍트 차단을 피하려고 다음은 넣지 않습니다.

- **기기 관리자:** 화면 잠금과 오버레이를 함께 쓰면 랜섬웨어 패턴으로 보입니다.
- **`REQUEST_INSTALL_PACKAGES`와 앱 내 APK 다운로드:** 악성 앱을 몰래 설치하는 드로퍼 패턴으로 보입니다.

## 빌드

```bash
./gradlew assembleGithubDebug     # 개발용 APK
./gradlew lintGithubDebug         # 정적 분석
```

## 배포

1. `RELEASE_NOTES.md`를 새 버전 내용으로 고칩니다. 사용자가 읽는 문장으로 쓰고, 첫 줄에 버전 번호를 넣습니다.
2. 커밋하고 push합니다.
3. 태그를 push합니다.
   ```bash
   git tag v0.2.0 && git push origin v0.2.0
   ```

GitHub Actions(`release.yml`)가 다음을 처리합니다.

- `sureureuk-0.2.0.apk`(github 배포판)를 서명해 Release에 올리고, `RELEASE_NOTES.md`를 Release 설명으로 씁니다.
- 플레이 콘솔용 AAB와 mapping 파일은 Actions 실행 화면의 Artifacts에 90일 동안 보관합니다. 사용자에게 보이는 Release에는 올리지 않습니다.
- `RELEASE_NOTES.md`에 해당 버전 번호가 없으면 배포를 멈춥니다.

**버전 규칙**
- versionName은 태그 그대로입니다(`0.2.0`).
- versionCode는 `주×10000 + 부×100 + 수`입니다(`0.2.0` → `200`).

**서명**
- 처음 한 번 `gh auth login` 후 `scripts/upload-signing-secrets.sh`로 서명 키를 저장소 시크릿에 등록합니다.
- 키 파일(`keystore/`, `keystore.properties`)은 저장소에 올리지 않습니다. 따로 백업해 두세요.
