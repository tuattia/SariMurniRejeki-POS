-- ==========================================================
-- DUMMY DATA UNTUK TOKO SARI MURNI REJEKI
-- Eksekusi file ini di phpMyAdmin atau MySQL CLI Anda
-- ==========================================================

-- 1. DATA BARANG (Katalog Produk Sembako & Sehari-hari)
INSERT INTO barang (kode_barang, nama_barang, harga_jual, stok) VALUES
('BRG-001', 'Beras Sania Premium 5Kg', 65000, 25),
('BRG-002', 'Beras Rojo Lele 10Kg', 125000, 15),
('BRG-003', 'Minyak Goreng Bimoli 2L', 34000, 40),
('BRG-004', 'Minyak Goreng SunCo 2L', 33500, 30),
('BRG-005', 'Gula Pasir Gulaku 1Kg', 15000, 50),
('BRG-006', 'Indomie Goreng (1 Dus)', 112000, 20),
('BRG-007', 'Indomie Kuah Ayam Bawang (1 Dus)', 105000, 18),
('BRG-008', 'Telur Ayam Negeri 1Kg', 28000, 12),
('BRG-009', 'Tepung Terigu Segitiga Biru 1Kg', 12000, 35),
('BRG-010', 'Kopi Kapal Api Mix (1 Renceng)', 13000, 45);


-- 2. DATA TRANSAKSI (Transaksi Master Induk)
INSERT INTO transaksi (kode_transaksi, tanggal, nama_pelanggan, total, jenis_transaksi, created_at) VALUES
('TRX-20231001001', '2023-10-01', 'Budi', 99000, 'TUNAI', '2023-10-01 10:15:00'),
('TRX-20231001002', '2023-10-01', 'Umum', 34000, 'TUNAI', '2023-10-01 12:30:00'),
('TRX-20231002001', '2023-10-02', 'Ibu Siti', 252000, 'TUNAI', '2023-10-02 08:45:00'),
('TRX-20231002002', '2023-10-02', 'Umum', 15000, 'TUNAI', '2023-10-02 16:20:00'),
('TRX-20231003001', '2023-10-03', 'Pak RT', 112000, 'TUNAI', '2023-10-03 19:10:00');


-- 3. DATA TRANSAKSI DETAIL (Item yang dibeli di dalam satu transaksi)
INSERT INTO transaksi_detail (kode_transaksi, kode_barang, harga, qty, subtotal) VALUES
('TRX-20231001001', 'BRG-001', 65000, 1, 65000),
('TRX-20231001001', 'BRG-003', 34000, 1, 34000),
('TRX-20231001002', 'BRG-003', 34000, 1, 34000),
('TRX-20231002001', 'BRG-002', 125000, 1, 125000),
('TRX-20231002001', 'BRG-006', 112000, 1, 112000),
('TRX-20231002001', 'BRG-005', 15000, 1, 15000),
('TRX-20231002002', 'BRG-005', 15000, 1, 15000),
('TRX-20231003001', 'BRG-006', 112000, 1, 112000);


-- 4. DATA LOG TRANSAKSI (Riwayat transaksi komplit dengan bayar dan kembali)
INSERT INTO log_transaksi (kode_transaksi, tanggal, nama_pelanggan, total, bayar, kembali, tipe_transaksi, keterangan) VALUES
('TRX-20231001001', '2023-10-01 10:15:00', 'Budi', 99000, 100000, 1000, 'TUNAI', 'Selesai'),
('TRX-20231001002', '2023-10-01 12:30:00', 'Umum', 34000, 35000, 1000, 'TUNAI', 'Selesai'),
('TRX-20231002001', '2023-10-02 08:45:00', 'Ibu Siti', 252000, 300000, 48000, 'TUNAI', 'Selesai'),
('TRX-20231002002', '2023-10-02 16:20:00', 'Umum', 15000, 20000, 5000, 'TUNAI', 'Selesai'),
('TRX-20231003001', '2023-10-03 19:10:00', 'Pak RT', 112000, 112000, 0, 'TUNAI', 'Selesai');


-- 5. DATA UTANG / PIUTANG
INSERT INTO utang (kode_utang, nama, alamat, telepon, harga_brng, dp, jumlah_cicilan, jatuh_tempo, status, kode_barang, kode_transaksi) VALUES
('UTG-001', 'Warung Bu Tejo', 'Jl. Kenangan 12', '081234567890', 1500000, 500000, 5, '2026-12-15', 'belum', 'BRG-001', NULL),
('UTG-002', 'Pak Slamet', 'Perumahan Indah A1', '085678901234', 450000, 100000, 3, '2026-11-20', 'belum', 'BRG-002', NULL),
('UTG-003', 'Ibu Rahma', 'Jl. Melati 4', '089876543210', 300000, 100000, 2, '2025-10-10', 'lunas', 'BRG-003', NULL),
('UTG-004', 'Toko Berkah', 'Pasar Induk C3', '081122334455', 2500000, 1000000, 10, '2027-02-01', 'belum', 'BRG-004', NULL),
('UTG-005', 'Kang Agus', 'Jl. Anggrek 22', '082233445566', 150000, 50000, 2, '2025-10-25', 'lunas', 'BRG-005', NULL);