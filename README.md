# nfdtonfc

파일명의 유니코드 정규화 형식을 NFD에서 NFC로 변환하는 커맨드라인 툴입니다.

macOS(HFS+)에서 생성된 파일은 파일명이 NFD 형식으로 저장되어, NFC를 기대하는 Linux 및 Windows 환경에서 호환성 문제가 발생할 수 있습니다. 이 툴은 해당 문제를 해결합니다.

## 설치

[Releases](https://github.com/xorwnsnmnnhlba/nfdtonfc/releases) 페이지에서 플랫폼에 맞는 설치 파일을 다운로드하세요.

### macOS

`.dmg` 파일을 열어 설치합니다.

### Windows

`.msi` 파일을 실행하여 설치합니다.

설치 후 환경변수 PATH에 설치 경로를 수동으로 추가해야 합니다.

**PowerShell (관리자 권한):**
```powershell
[System.Environment]::SetEnvironmentVariable("PATH", $env:PATH + ";C:\Program Files\nfdtonfc", "Machine")
```

설정 후 터미널을 새로 열면 어디서든 `nfdtonfc` 명령어로 실행 가능합니다.

### Ubuntu / WSL

```bash
sudo apt install ./nfdtonfc-<버전>-ubuntu.deb
```

## 사용법

```
nfdtonfc <파일-또는-디렉터리> [파일-또는-디렉터리 ...]
```

### 예시

```bash
# 파일 하나 변환
nfdtonfc file.txt

# 디렉터리 내 파일 전체 재귀 변환
nfdtonfc ~/Downloads

# 여러 대상 한 번에 변환
nfdtonfc dir1 dir2 file.txt
```

### 옵션

| 옵션 | 설명 |
|------|------|
| `-f`, `--force` | 동일한 파일이 존재할 경우 확인 없이 덮어씌움 |
| `--help` | 도움말 출력 후 종료 |
| `--version` | 버전 출력 후 종료 |

## 빌드

JDK 25 (Zulu) 가 필요합니다.

```bash
# 현재 OS용 설치 파일 빌드
./gradlew jpackageInstaller
```

| OS | 결과물 |
|----|--------|
| macOS | `build/jpackage/nfdtonfc-<버전>.dmg` |
| Windows | `build/jpackage/nfdtonfc-<버전>.msi` |
| Ubuntu | `build/jpackage/nfdtonfc_<버전>_amd64.deb` |

## 제거

### macOS

`/Applications`에서 `nfdtonfc`를 휴지통으로 이동합니다.

### Windows

**설정 > 앱** 에서 제거합니다.

### Ubuntu / WSL

```bash
sudo apt remove nfdtonfc
```
