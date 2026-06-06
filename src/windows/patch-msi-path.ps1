param (
    [Parameter(Mandatory)][string]$MsiPath
)

$installer = New-Object -ComObject WindowsInstaller.Installer
$db = $installer.GetType().InvokeMember("OpenDatabase", "InvokeMethod", $null, $installer, @($MsiPath, 1))

# 첫 번째 컴포넌트 조회
$view = $db.GetType().InvokeMember("OpenView", "InvokeMethod", $null, $db, @("SELECT Component FROM Component"))
$view.GetType().InvokeMember("Execute", "InvokeMethod", $null, $view, $null)
$record = $view.GetType().InvokeMember("Fetch", "InvokeMethod", $null, $view, $null)
$component = $record.GetType().InvokeMember("StringData", "GetProperty", $null, $record, @(1))
$view.GetType().InvokeMember("Close", "InvokeMethod", $null, $view, $null)

# Environment 테이블 생성
try {
    $createSql = "CREATE TABLE Environment (Environment CHAR(72) NOT NULL, Name CHAR(255) NOT NULL LOCALIZABLE, Value CHAR(255) LOCALIZABLE, Component_ CHAR(72) NOT NULL PRIMARY KEY Environment)"
    $view = $db.GetType().InvokeMember("OpenView", "InvokeMethod", $null, $db, @($createSql))
    $view.GetType().InvokeMember("Execute", "InvokeMethod", $null, $view, $null)
    $view.GetType().InvokeMember("Close", "InvokeMethod", $null, $view, $null)
} catch {
    Write-Host "Environment table already exists, skipping creation."
}

# PATH 등록
$insertSql = "INSERT INTO Environment (Environment, Name, Value, Component_) VALUES ('PATH_NFDTONFC', 'PATH', '[INSTALLDIR]', '$component')"
$view = $db.GetType().InvokeMember("OpenView", "InvokeMethod", $null, $db, @($insertSql))
$view.GetType().InvokeMember("Execute", "InvokeMethod", $null, $view, $null)
$view.GetType().InvokeMember("Close", "InvokeMethod", $null, $view, $null)

$db.GetType().InvokeMember("Commit", "InvokeMethod", $null, $db, $null)

Write-Host "PATH registration patched successfully."
