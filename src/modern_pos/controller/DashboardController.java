package modern_pos.controller;
import java.util.List;
import modern_pos.dao.DashboardDAO;
import modern_pos.dao.UtangDAO;
import modern_pos.model.DashboardSummary;
import modern_pos.model.User;
import modern_pos.model.Utang;
import modern_pos.utils.Async;
import modern_pos.view.DashboardView;

public class DashboardController {
    private DashboardView view;
    private final UtangDAO utangDAO = new UtangDAO();
    private final DashboardDAO dashboardDAO = new DashboardDAO();
    private final User currentUser;

    public DashboardController(User user) {
        this.currentUser = user;
    }

    public void setView(DashboardView view) {
        this.view = view;
        view.setUserInfo(currentUser != null ? currentUser.getNama() : "-");
        loadData("");
    }

    public void loadData(String keyword) {
        final DashboardSummary[] summary = new DashboardSummary[1];
        Async.ambil(() -> {
            summary[0] = dashboardDAO.getSummary();
            return utangDAO.getUtangList(keyword);
        }, (List<Utang> utang) -> {
            view.updateSummaryCards(summary[0]);
            view.populateTable(utang);
        }, msg -> view.showError("Gagal memuat data: " + msg));
    }

    public void logout() {
        int conf = view.showConfirm("Apakah Anda yakin ingin logout?");
        if (conf == javax.swing.JOptionPane.YES_OPTION) {
            view.dispose();
            new modern_pos.view.LoginView().setVisible(true);
        }
    }
}
