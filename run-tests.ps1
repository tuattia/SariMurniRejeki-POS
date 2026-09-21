# Compile src + test lalu jalankan semua test JUnit (DB: sarimurnirejeki_test).
# Dipakai karena Ant tidak ada di PATH; di NetBeans cukup "Test Project".
$ErrorActionPreference = 'Stop'
$out = 'build/test-run'
if (Test-Path $out) { Remove-Item -Recurse -Force $out }
New-Item -ItemType Directory -Force $out | Out-Null
$sources = Get-ChildItem -Recurse src, test -Filter *.java | ForEach-Object { $_.FullName }
& javac -encoding UTF-8 -nowarn -d $out -cp 'lib/*' $sources
if ($LASTEXITCODE -ne 0) { exit 1 }
$root = (Resolve-Path test).Path
$tests = Get-ChildItem -Recurse test -Filter *Test.java | ForEach-Object {
    $_.FullName.Substring($root.Length + 1).Replace('.java', '').Replace('\', '.')
}
& java "-Ddb.url=jdbc:mysql://localhost:3306/sarimurnirejeki_test" -cp "$out;lib/*" org.junit.runner.JUnitCore $tests
exit $LASTEXITCODE
