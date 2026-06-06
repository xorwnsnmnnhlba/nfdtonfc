param (
    [Parameter(Mandatory)][string]$MsiPath
)

$installer = New-Object -ComObject WindowsInstaller.Installer
$db = $installer.GetType().InvokeMember("OpenDatabase", "InvokeMethod", $null, $installer, @($MsiPath, 1))

function ExecSql($sql) {
    $v = $db.GetType().InvokeMember("OpenView", "InvokeMethod", $null, $db, @($sql))
    $v.GetType().InvokeMember("Execute", "InvokeMethod", $null, $v, $null)
    $v.GetType().InvokeMember("Close", "InvokeMethod", $null, $v, $null)
}

function FetchOne($sql) {
    $v = $db.GetType().InvokeMember("OpenView", "InvokeMethod", $null, $db, @($sql))
    $v.GetType().InvokeMember("Execute", "InvokeMethod", $null, $v, $null)
    $r = $v.GetType().InvokeMember("Fetch", "InvokeMethod", $null, $v, $null)
    $val = $r.GetType().InvokeMember("StringData", "GetProperty", $null, $r, @(1))
    $v.GetType().InvokeMember("Close", "InvokeMethod", $null, $v, $null)
    return $val
}

# 첫 번째 컴포넌트 조회
$component = FetchOne "SELECT Component FROM Component"
Write-Host "Component: $component"

# Environment 테이블 생성
try {
    ExecSql "CREATE TABLE Environment (Environment CHAR(72) NOT NULL, Name CHAR(255) NOT NULL LOCALIZABLE, Value CHAR(255) LOCALIZABLE, Component_ CHAR(72) NOT NULL PRIMARY KEY Environment)"
    Write-Host "Environment table created."
} catch {
    Write-Host "Environment table already exists."
}

# PATH 등록
ExecSql "INSERT INTO Environment (Environment, Name, Value, Component_) VALUES ('PATH_NFDTONFC', 'PATH', '[INSTALLDIR]', '$component')"
Write-Host "PATH entry inserted."

# WriteEnvironmentStrings 액션을 InstallExecuteSequence에 추가
try {
    ExecSql "INSERT INTO InstallExecuteSequence (Action, Condition, Sequence) VALUES ('WriteEnvironmentStrings', 'NOT Installed', 5200)"
    Write-Host "WriteEnvironmentStrings added to sequence."
} catch {
    Write-Host "WriteEnvironmentStrings already in sequence."
}

# RemoveEnvironmentStrings 액션을 InstallExecuteSequence에 추가 (제거 시 PATH 정리)
try {
    ExecSql "INSERT INTO InstallExecuteSequence (Action, Condition, Sequence) VALUES ('RemoveEnvironmentStrings', 'Installed', 1600)"
    Write-Host "RemoveEnvironmentStrings added to sequence."
} catch {
    Write-Host "RemoveEnvironmentStrings already in sequence."
}

$db.GetType().InvokeMember("Commit", "InvokeMethod", $null, $db, $null)
Write-Host "MSI patched successfully."
