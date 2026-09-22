package modern_pos.model;

public class KartuAngsuran {
    private final Utang utang;
    private final String namaBarang;

    public KartuAngsuran(Utang utang, String namaBarang) {
        this.utang = utang;
        this.namaBarang = namaBarang;
    }
    public Utang getUtang() { return utang; }
    public String getNamaBarang() { return namaBarang; }
}
