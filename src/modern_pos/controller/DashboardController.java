package modern_pos.controller;
import java.util.List;
import javax.swing.SwingWorker;
import modern_pos.dao.DashboardDAO;
import modern_pos.dao.UtangDAO;
import modern_pos.model.DashboardSummary;
import modern_pos.model.User;
import modern_pos.model.Utang;
import modern_pos.view.DashboardView;

public class DashboardController {
    private DashboardView view;
    private final UtangDAO utangDAO;
    private final DashboardDAO dashboardDAO;
    private final User currentUser;

    public DashboardController(User user) {
        this.utangDAO = new UtangDAO();
        this.dashboardDAO = new DashboardDAO();
        this.currentUser = user;
    }

    public void setView(DashboardView view) {
        this.view = view;
        view.setUserInfo(currentUser != null ? currentUser.getNama() : "-");
        loadData("");
    }

    public void loadData(String keyword) {
        SwingWorker<DashboardDataBundle, Void> worker = new SwingWorker<DashboardDataBundle, Void>() {
            @Override
            protected DashboardDataBundle doInBackground() throws Exception {
                DashboardDataBundle bundle = new DashboardDataBundle();
                bundle.summary = dashboardDAO.getSummary();
                bundle.utangList = utangDAO.getUtangList(keyword);
                return bundle;
            }
            @Override
            protected void done() {
                try {
                    DashboardDataBundle result = get();
                    view.updateSummaryCards(result.summary);
                    view.populateTable(result.utangList);
                } catch (Exception ex) {
                    view.showError("Gagal memuat data: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }
    
    public void logout() {
        int conf = view.showConfirm("Apakah Anda yakin ingin logout?");
        if (conf == javax.swing.JOptionPane.YES_OPTION) {
            view.dispose();
            new modern_pos.view.LoginView().setVisible(true);
        }
    }
    
    // Class internal untuk menggabungkan hasil background task
    private class DashboardDataBundle {
        DashboardSummary summary;
        List<Utang> utangList;
    }
}