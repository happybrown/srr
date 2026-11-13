# 스르륵 (Sureureuk)

넷플릭스·유튜브를 보다가 잠들어도 괜찮도록, 정해진 시간이 되면 **소리를 서서히 줄이고 → 재생을 멈추고 → 화면을 꺼 주는** 안드로이드 슬립 타이머입니다.

> 현재 검증 단계(0.x)입니다. 실제 기기에서 앱별로 어떤 방법이 통하는지 확인하는 검증 화면이 들어 있습니다. 검증 방법은 [VERIFY.md](VERIFY.md)를 보세요.

## 설치

[Releases](https://github.com/happybrown/srr/releases)에서 최신 `sureureuk-x.y.z-github.apk`를 받아 설치하세요. 설치한 뒤에는 앱이 새 버전을 알려 주고, 앱 안에서 바로 업데이트할 수 있습니다.

- 지원: Android 8.0 이상 (targetSdk 36)

## 주요 기능

- 화면 맨 앞에 다른 앱이 있어도 뒤에서 동작 (포그라운드 서비스 + 정확한 알람 백업)
- 끝나기 전에 미디어 볼륨을 dB 기준으로 서서히 줄이기
- 재생 정지: 미디어 세션 → 미디어 키 → 오디오 포커스 순서로 시도하고 결과 확인
- 화면 끄기: 자연스럽게 끄기(생체 인증 유지) 또는 즉시 잠금
- 영상 위 오버레이와 알림에서 +5/+10/+15분 연장, 빠른 설정 타일, 흔들어서 연장
- 바꾼 볼륨·화면 설정은 앱이 종료되거나 재부팅돼도 원래대로 복원

## 배포판

| 배포판 | 배포처 | 인터넷 권한 | 업데이트 |
|---|---|---|---|
| `github` | GitHub Releases | 있음 (새 버전 확인 전용) | 앱 안에서 확인·설치 |
| `play` | 플레이 스토어 | 없음 | 플레이 스토어 |

두 배포판 모두 개인정보를 수집하거나 외부로 보내지 않습니다.

## 개발

```bash
./gradlew assembleGithubDebug     # 개발용 APK
./gradlew lintGithubDebug         # 정적 분석
```

## 배포 방법

`v1.2.3` 형식의 태그를 push하면 GitHub Actions가 서명된 결과물을 빌드해 Release에 올립니다.

```bash
git tag v0.2.0
git push origin v0.2.0
```

| 결과물 | 용도 |
|---|---|
| `sureureuk-0.2.0-github.apk` | 직접 설치, 앱 내 업데이트 |
| `sureureuk-0.2.0-play.aab` | 플레이 콘솔 업로드 |
| `mapping-0.2.0.zip` | 오류 보고서 해석용 |

- **버전 규칙:** versionName = 태그(`0.2.0`), versionCode = `주×10000 + 부×100 + 수` (`0.2.0` → `200`)
- **처음 한 번만 할 일:** 서명 키를 저장소 시크릿에 등록합니다. 먼저 `gh auth login`으로 로그인한 뒤 `scripts/upload-signing-secrets.sh`를 실행하세요.
- 서명 키(`keystore/`, `keystore.properties`)는 저장소에 올리지 않습니다. 따로 안전하게 백업해 두세요.
