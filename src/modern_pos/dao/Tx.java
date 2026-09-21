package modern_pos.dao;

import java.sql.Connection;
import java.sql.SQLException;

final class Tx {
    private Tx() {}

    // Rollback tanpa menutupi error asli: kegagalan rollback ditempel sebagai suppressed.
    static SQLException rollbackQuietly(Connection con, SQLException e) {
        try {
            con.rollback();
        } catch (SQLException r) {
            e.addSuppressed(r);
        }
        return e;
    }
}
