-- ============================================================
-- DDL: Periodic Stock Snapshot
-- Database: sarimurnirejeki
-- Dibuat  : 2026-09-03
-- ============================================================

-- -----------------------------------------------
-- Tabel 1: stock_snapshot
-- Menyimpan rekaman "foto" stok pada bulan tertentu.
-- periode selalu berisi hari ke-1 dari bulan tsb,
-- misal September 2026 -> '2026-09-01'
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS stock_snapshot (
    id_snapshot     INT AUTO_INCREMENT PRIMARY KEY,
    periode         DATE        NOT NULL COMMENT 'Hari pertama bulan, contoh: 2026-09-01',
    kode_barang     VARCHAR(50) NOT NULL,
    nama_barang     VARCHAR(100) NOT NULL,
    harga_jual      INT         NOT NULL DEFAULT 0,
    satuan          VARCHAR(20),
    stok_snapshot   INT         NOT NULL DEFAULT 0,
    dibuat_pada     DATETIME    DEFAULT CURRENT_TIMESTAMP,
    diperbarui_pada DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    -- Mencegah duplikat: 1 barang hanya 1 snapshot per bulan
    UNIQUE KEY uq_snap_periode_barang (periode, kode_barang)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Rekaman stok bulanan';


-- -----------------------------------------------
-- Tabel 2: stock_movement_log
-- Mencatat setiap gerakan stok (masuk / keluar / koreksi).
-- kode_transaksi boleh NULL jika gerakan bukan dari transaksi penjualan.
-- -----------------------------------------------
CREATE TABLE IF NOT EXISTS stock_movement_log (
    id_log          INT AUTO_INCREMENT PRIMARY KEY,
    kode_barang     VARCHAR(50)  NOT NULL,
    kode_transaksi  VARCHAR(100) DEFAULT NULL COMMENT 'NULL jika bukan dari transaksi penjualan',
    tipe_gerakan    ENUM('KELUAR','MASUK','KOREKSI') NOT NULL,
    qty             INT          NOT NULL,
    stok_sebelum    INT          NOT NULL,
    stok_sesudah    INT          NOT NULL,
    keterangan      VARCHAR(255),
    waktu           DATETIME     DEFAULT CURRENT_TIMESTAMP,

    INDEX idx_movement_kode_barang (kode_barang),
    INDEX idx_movement_waktu       (waktu),
    INDEX idx_movement_kode_trx    (kode_transaksi)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Log gerakan stok masuk dan keluar';


-- -----------------------------------------------
-- Verifikasi: tampilkan struktur tabel yang baru dibuat
-- -----------------------------------------------
DESCRIBE stock_snapshot;
DESCRIBE stock_movement_log;
