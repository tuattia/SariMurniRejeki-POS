# Sub-proyek 4: Hapus stack lama + audit — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: superpowers:executing-plans. Steps use checkbox (`- [ ]`) syntax.

**Goal:** Satu codebase: hanya `modern_pos` (+ `config.koneksi`) yang tersisa; build dan test tetap hijau; lalu audit over-engineering dan keamanan.

**Konteks (hasil pemetaan):** `modern_pos` hanya bergantung ke stack lama lewat `login.enkripsi.sha256` (di `UserDAO`). `koneksi.getConnection()` hanya dipakai stack lama. `AbsoluteLayout.jar`/`DateChooser.jar` dan `src/icon/` hanya dipakai `src/gui/`. Entry point sudah `modern_pos.view.LoginView` (sub-proyek 3). Tidak ada spec terpisah: pekerjaan mekanis tanpa pilihan desain; keputusan user = hapus stack lama (roadmap sub-proyek 1).

## Global Constraints

- Java 1.8 source. Test: `powershell -ExecutionPolicy Bypass -File run-tests.ps1`.
- Tidak ada perubahan perilaku `modern_pos`; hash password tetap SHA-256 hex (login lama tetap valid).
- DB tidak disentuh.
- Commit: `git -c user.name="attia" -c user.email="attiaitem@gmail.com" commit -m "<pesan>" -m "Co-Authored-By: Claude Opus 5 <noreply@anthropic.com>"`.

---

### Task 1: Pindahkan SHA-256 ke `modern_pos.utils.Hash`

- [ ] **Step 1: Test gagal `test/modern_pos/utils/HashTest.java`**
```java
package modern_pos.utils;

import org.junit.Test;
import static org.junit.Assert.*;

public class HashTest {
    @Test
    public void sha256HexSamaDenganFormatLama() {
        // Nilai yang tersimpan di tabel user untuk password "admin".
        assertEquals("8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918", Hash.sha256("admin"));
    }
}
```
- [ ] **Step 2: Jalankan → gagal** (`cannot find symbol Hash`).
- [ ] **Step 3: `src/modern_pos/utils/Hash.java`**
```java
package modern_pos.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class Hash {
    private Hash() {}

    // ponytail: SHA-256 tanpa salt (format lama, keputusan user). Ganti ke PBKDF2 + upgrade saat login bila perlu.
    public static String sha256(String teks) {
        try {
            byte[] h = MessageDigest.getInstance("SHA-256").digest(teks.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : h) hex.append(String.format("%02x", b));
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 wajib ada di setiap JVM
        }
    }
}
```
- [ ] **Step 4:** `UserDAO`: ganti `import login.enkripsi;` dengan `import modern_pos.utils.Hash;` dan semua `enkripsi.sha256(` → `Hash.sha256(`.
- [ ] **Step 5: Jalankan → `OK (90 tests)`.** Commit `refactor(modern_pos): own SHA-256 helper instead of legacy login.enkripsi`.

### Task 2: Hapus stack lama

- [ ] **Step 1:** `git rm -r -q src/gui src/controller src/model src/login src/sarimurnirejeki src/icon stock_snapshot_ddl.sql lib/AbsoluteLayout.jar lib/DateChooser.jar`
- [ ] **Step 2:** `config/koneksi.java`: hapus method `getConnection()` beserta komentarnya. `test/config/KoneksiTest.java`: hapus test `getConnectionLamaTetapReturnNullBilaGagal`.
- [ ] **Step 3:** `nbproject/project.properties`: hapus baris `file.reference.AbsoluteLayout.jar=...` dan `file.reference.DateChooser.jar=...`; `javac.classpath` hanya `${file.reference.mysql-connector-j-9.5.0.jar}`; `javac.processorpath` hanya `${javac.classpath}`.
- [ ] **Step 4: Verifikasi**
```powershell
powershell -ExecutionPolicy Bypass -File run-tests.ps1
Select-String -Path nbproject\*.properties,nbproject\project.xml -Pattern 'AbsoluteLayout|DateChooser|gui\.login|absolutelayout'
Get-ChildItem -Recurse src -Filter *.java | Select-String -Pattern '^import (login|gui|controller|model)\.|getConnection\(\)'
```
Expected: `OK (89 tests)`; kedua grep tanpa output.
- [ ] **Step 5: Smoke test** `java -cp "build/test-run;lib/*" modern_pos.view.LoginView` hidup ≥ 6 detik tanpa stderr, lalu dihentikan.
- [ ] **Step 6:** Commit `chore: remove legacy gui/controller/model/login stack and its libraries`.

### Task 3: Audit

- [ ] **Step 1:** skill `ponytail:ponytail-audit` atas seluruh repo (laporan, tanpa fix).
- [ ] **Step 2:** skill `security-review` atas perubahan branch.
- [ ] **Step 3:** Laporkan temuan ke user dan minta keputusan mana yang dikerjakan (fix tidak otomatis).
