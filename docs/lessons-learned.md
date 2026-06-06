# nfdtonfc 개발 시행착오 및 교훈 정리

> 추후 바이브코딩 시 동일한 시행착오를 반복하지 않기 위한 기록

---

## 1. 프로젝트 구성

### Java 콘솔 앱 (Spring Boot 불필요)
- 단순 파일명 변환 유틸리티에 Spring Boot는 과도한 선택
- `java.text.Normalizer.normalize(str, Normalizer.Form.NFC)` 하나로 NFD→NFC 변환 가능
- Gradle `application` 플러그인 + `jpackage`로 네이티브 설치 파일 생성

### Gradle Wrapper 주의사항
- Spring Boot 기본 `.gitignore`에 `gradlew`, `gradlew.bat`, `gradle/`이 포함되어 있음
- GitHub Actions에서 `gradlew: No such file or directory` 오류 발생
- **해결**: `.gitignore`에서 해당 항목 제거 후 커밋

---

## 2. jpackage 설치 파일 생성

### 플랫폼별 타입 선택
| OS | 타입 | 비고 |
|----|------|------|
| macOS | `pkg` | `dmg`는 postinstall 스크립트 미지원 → PATH 등록 불가 |
| Windows | `msi` | `--win-console` 필수 (없으면 JVM 실행 실패) |
| Linux | `deb` | postinst/postrm 스크립트로 PATH 등록 가능 |

### Windows MSI - `--win-console` 필수
```gradle
// CLI 앱인데 이 옵션 없으면 "failed to launch JVM" 오류 발생
'--win-console'
```

### jpackage 경로 하드코딩 금지
```gradle
// 나쁜 예
def jpackage = '/Library/Java/JavaVirtualMachines/zulu-25.jdk/Contents/Home/bin/jpackage'

// 좋은 예 (환경변수 또는 시스템 프로퍼티에서 동적으로 읽기)
def javaHome = System.getenv('JAVA_HOME') ?: System.getProperty('java.home')
def jpackage = "${javaHome}/bin/jpackage"
```

### JVM 옵션 (jpackage)
```gradle
'--java-options', '-Dfile.encoding=UTF-8',
'--java-options', '-Dstdout.encoding=UTF-8',
'--java-options', '-Dstderr.encoding=UTF-8',
'--java-options', '--enable-native-access=ALL-UNNAMED'
```
- FFM API(Foreign Function & Memory) 사용 시 `--enable-native-access=ALL-UNNAMED` 필수
- 없으면 경고(Java 25) → 향후 버전에서 블록 예정

---

## 3. GitHub Actions 멀티플랫폼 빌드

### 기본 구조
```yaml
strategy:
  matrix:
    include:
      - os: macos-15
        ext: pkg
        os-name: macos
      - os: windows-2025
        ext: msi
        os-name: windows
      - os: ubuntu-24.04
        ext: deb
        os-name: ubuntu
```

### 버전 주입 방법
- `build.gradle`에 버전 하드코딩 금지
- 태그에서 동적으로 추출 후 Gradle `-P` 플래그로 주입

```yaml
- name: Extract version
  id: version
  shell: bash
  run: echo "VERSION=${GITHUB_REF_NAME#v}" >> $GITHUB_OUTPUT

- name: Build installer
  shell: bash
  run: ./gradlew jpackageInstaller -Pversion=${{ steps.version.outputs.VERSION }}
```

### sed -i 크로스플랫폼 주의
- macOS runner에서 `sed -i "s/..."` 는 backup suffix가 없으면 오류 발생
- `sed -i.bak` 또는 Gradle `-P` 플래그 방식으로 우회

### GitHub Actions 권한 설정
```yaml
permissions:
  contents: write  # Release 생성 시 필수
```
- 없으면 `Resource not accessible by integration` 오류 발생

### OS runner 버전 명시 권장
```yaml
# latest 대신 명확한 버전 사용
macos-15        # macos-latest 대신
windows-2025    # windows-latest 대신
ubuntu-24.04    # ubuntu-latest 대신
```

---

## 4. PATH 자동 등록

### Ubuntu (deb) - postinst/postrm 스크립트
`src/jpackage/postinst`:
```bash
#!/bin/bash
ln -sf /opt/nfdtonfc/bin/nfdtonfc /usr/local/bin/nfdtonfc

mkdir -p /usr/local/share/man/man1
cat <<'EOF' | gzip > /usr/local/share/man/man1/nfdtonfc.1.gz
# man 페이지 내용
EOF

mandb -q
```

`src/jpackage/postrm`:
```bash
#!/bin/bash
rm -f /usr/local/bin/nfdtonfc
rm -f /usr/local/share/man/man1/nfdtonfc.1.gz
mandb -q
```

- jpackage `--resource-dir` 옵션으로 스크립트 포함
- `deb` 기본 설치 경로는 `/opt/<appname>/` → PATH에 자동 등록 안 됨

### macOS (pkg) - postinstall 스크립트
`src/jpackage/postinstall`:
```bash
#!/bin/bash
ln -sf /Applications/nfdtonfc.app/Contents/MacOS/nfdtonfc /usr/local/bin/nfdtonfc
```

- `dmg` 타입은 postinstall 스크립트 미지원 → `pkg` 타입 사용 필수
- macOS도 기본 설치 후 PATH 등록 안 됨 → `pkg` + postinstall로 해결

### Windows (msi) - MSI 데이터베이스 패치
- jpackage MSI는 기본적으로 PATH 등록 미지원
- Windows Installer COM 자동화로 MSI 데이터베이스에 직접 패치
- `WriteEnvironmentStrings`, `RemoveEnvironmentStrings` 액션을 `InstallExecuteSequence`에 추가해야 실제 적용됨

`src/windows/patch-msi-path.ps1` 핵심:
```powershell
# Environment 테이블 생성 후 PATH 항목 삽입
# WriteEnvironmentStrings, RemoveEnvironmentStrings를 InstallExecuteSequence에 추가
```

- YAML 내 PowerShell 인라인 작성 시 백틱(`) 이스케이프 충돌 → 별도 `.ps1` 파일로 분리 필수

---

## 5. Windows 콘솔 한글 인코딩

### 문제
- Windows cmd/PowerShell 기본 인코딩이 UTF-8이 아니어서 한글이 `????`로 출력
- `System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8))` 만으로는 불충분

### 해결: FFM API로 SetConsoleOutputCP(65001) 직접 호출
```java
Linker linker = Linker.nativeLinker();
SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32", Arena.global());
MethodHandle setConsoleOutputCP = linker.downcallHandle(
    kernel32.find("SetConsoleOutputCP").get(),
    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
);
setConsoleOutputCP.invoke(65001);
```

- `chcp 65001`을 자식 프로세스로 실행하면 현재 프로세스에 영향 없음 → 효과 없음
- 반드시 `kernel32`에서 직접 호출해야 함

---

## 6. macOS NFD→NFC 변환 (핵심 트러블슈팅)

### 배경
- macOS HFS+: 파일명을 항상 NFD로 강제 저장
- macOS APFS: normalization-insensitive (NFD/NFC를 같은 파일로 인식)

### 문제 1: Java Files.move()가 macOS에서 NFD로 강제 변환
- `Files.move()`가 macOS VFS 레이어를 거치며 경로를 NFD로 변환
- **해결**: FFM API로 `libSystem.B.dylib`의 `rename()` syscall 직접 호출

```java
SymbolLookup libc = SymbolLookup.libraryLookup("libSystem.B.dylib", Arena.global());
MethodHandle renameHandle = linker.downcallHandle(
    libc.find("rename").get(),
    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.ADDRESS)
);
```

### 문제 2: APFS normalization-insensitive → NFD→NFC 직접 rename이 no-op
- APFS가 NFD와 NFC를 같은 파일로 인식하여 직접 rename을 무시
- **해결**: 2단계 rename으로 우회 (NFD → temp → NFC)

```java
invokeRename(source.toString(), tempPath);  // NFD → ASCII temp
invokeRename(tempPath, nfcPath);            // ASCII temp → NFC
```

### 문제 3: target 경로를 Path.toString()으로 조립하면 다시 NFD로 변환
- `path.resolveSibling(nfcName).toString()` → macOS Path 구현이 내부적으로 NFD 변환
- **해결**: Path 객체를 거치지 않고 문자열 직접 조립

```java
// 나쁜 예
byte[] dstBytes = target.toString().getBytes(UTF_8);  // NFD로 변환됨

// 좋은 예
String parentStr = source.getParent().toString() + "/";
byte[] dstBytes = (parentStr + nfcName).getBytes(UTF_8);  // NFC 유지
```

### 문제 4: pkg 번들 JRE가 파일명을 NFC로 정규화하여 반환
- pkg 설치 후 실행 시 JRE가 `Files.walk()`로 읽은 파일명을 자동으로 NFC로 반환
- `originalName.equals(normalizedName)` → true → 변환 불필요로 판단하여 skip
- **해결**: macOS에서는 문자열 비교 대신 NFC 바이트 vs NFD 바이트 비교

```java
private boolean needsNoConversion(String originalName, String normalizedName) {
    if (!IS_MAC) {
        return originalName.equals(normalizedName);
    }
    // macOS JVM은 파일명을 NFC로 정규화하여 반환 → 문자열 비교 불가
    // NFC 바이트와 NFD 바이트가 같으면(ASCII 등) 변환 불필요
    byte[] nfcBytes = normalizedName.getBytes(StandardCharsets.UTF_8);
    byte[] nfdBytes = Normalizer.normalize(originalName, Normalizer.Form.NFD)
                                .getBytes(StandardCharsets.UTF_8);
    return Arrays.equals(nfcBytes, nfdBytes);
}
```

### macOS FFM rename 요약 흐름
```
1. needsNoConversion() → NFC/NFD 바이트 비교로 변환 필요 여부 판단
2. renameDirect() → 2단계 rename
   a. invokeRename(src, temp)  // 실제 파일(NFD) → ASCII 임시 이름
   b. invokeRename(temp, nfc)  // ASCII 임시 이름 → NFC 이름
3. invokeRename() → FFM으로 libSystem.B.dylib::rename() 직접 호출
   - NFC String을 getBytes(UTF_8)로 인코딩 → NFC 바이트 보장
```

---

## 7. man 페이지 등록 (Ubuntu deb)

- `gzip` 압축 후 `/usr/local/share/man/man1/` 에 배치
- `mandb -q` 실행으로 인덱스 갱신
- man 페이지 내용을 `postinst` 내 heredoc으로 직접 임베드 (별도 파일 포함 방식은 jpackage에서 경로 문제 발생)

```bash
cat <<'EOF' | gzip > /usr/local/share/man/man1/nfdtonfc.1.gz
.TH NFDTONFC 1 ...
EOF
```

---

## 8. 사용자 입력 처리

### Scanner NoSuchElementException
- `Ctrl+C` 등 임의 종료 시 `scanner.nextLine()`에서 `NoSuchElementException` 발생
- **해결**: try-catch로 잡아서 `System.exit(0)` 처리

```java
try {
    input = scanner.nextLine().trim().toLowerCase();
} catch (NoSuchElementException e) {
    System.out.println("\nAborted.");
    System.exit(0);
}
```

### 다중 충돌 파일 처리
- 파일마다 매번 묻는 것은 UX 저하
- y/n/a/s 방식으로 구현 (a=all overwrite, s=skip all)
- `forceAll`, `skipAll` 상태를 `ConflictResolver` 인스턴스에서 관리

---

## 9. 코드 아키텍처

### 클래스 분리
| 클래스 | 역할 |
|--------|------|
| `Main` | 진입점 (main() 한 줄) |
| `App` | 애플리케이션 흐름 조율 |
| `ConsoleEncoder` | UTF-8 인코딩 초기화 |
| `HelpPrinter` | 도움말/버전 출력 |
| `Options` | 인자 파싱 |
| `ConflictResolver` | 충돌 시 y/n/a/s 처리 |
| `Converter` | NFD→NFC 변환 및 파일 이동 |

### switch 문 스타일
```java
// yield 사용 금지, return 사용
switch (input) {
    case "y" -> {
        return Action.OVERWRITE;
    }
    case "a" -> {
        forceAll = true;
        return Action.OVERWRITE;
    }
    case "s" -> {
        skipAll = true;
        return Action.SKIP;
    }
}
return Action.SKIP;
```

---

## 10. 기타

### Glob 패턴 지원
- Ubuntu(bash/zsh): 쉘이 glob 확장 → Java 처리 불필요하지만 따옴표 사용 시 Java 처리
- Windows/macOS: 쉘이 glob 미확장 → Java 내부에서 `PathMatcher`로 처리 필요
- 사용 시 따옴표로 감싸야 쉘 확장 방지: `nfdtonfc "*.txt"`

### jar manifest 버전 정보
```gradle
jar {
    manifest {
        attributes 'Main-Class': 'com.example.nfdtonfc.Main',
                   'Implementation-Version': version
    }
}
```
```java
String version = Main.class.getPackage().getImplementationVersion();
```

### Gatekeeper (macOS 미서명 앱)
- Apple 개발자 계정 없이 배포 시 Gatekeeper 경고 발생
- 우회: `sudo xattr -rd com.apple.quarantine /Applications/nfdtonfc.app`
- 근본 해결: Apple Developer Program 가입 후 코드 서명 + 공증(Notarization)
