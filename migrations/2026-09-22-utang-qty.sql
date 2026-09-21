-- Sub-proyek 2a: utang punya qty. Utang lama otomatis qty 1.
ALTER TABLE utang ADD COLUMN qty INT NOT NULL DEFAULT 1 AFTER kode_barang;
