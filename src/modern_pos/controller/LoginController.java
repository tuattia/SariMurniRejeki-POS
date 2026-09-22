package modern_pos.controller;
import modern_pos.dao.UserDAO;
import modern_pos.utils.Async;
import modern_pos.utils.Session;
import modern_pos.view.DashboardView;
import modern_pos.view.LoginView;

public class LoginController {
    private LoginView view;
    private final UserDAO userDAO = new UserDAO();

    public void setView(LoginView view) { this.view = view; }

    public void handleLogin() {
        if (view == null) return;
        String username = view.getUsernameInput();
        String password = view.getPasswordInput();
        if (username.trim().isEmpty() || password.trim().isEmpty()) {
            view.showError("Username dan Password tidak boleh kosong!");
            return;
        }
        view.setLoading(true);
        Async.ambil(() -> userDAO.authenticate(username, password), user -> {
            view.setLoading(false);
            Session.currentUser = user;
            view.dispose();
            new DashboardView(new DashboardController(user)).setVisible(true);
        }, msg -> {
            view.setLoading(false);
            view.showError(msg);
        });
    }
}
