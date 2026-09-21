package modern_pos.controller;
import javax.swing.SwingWorker;
import modern_pos.dao.UserDAO;
import modern_pos.model.User;
import modern_pos.view.LoginView;
import modern_pos.view.DashboardView;

public class LoginController {
    private LoginView view;
    private final UserDAO userDAO;
    public LoginController() { this.userDAO = new UserDAO(); }
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
        SwingWorker<User, Void> worker = new SwingWorker<User, Void>() {
            @Override
            protected User doInBackground() throws Exception {
                return userDAO.authenticate(username, password);
            }
            @Override
            protected void done() {
                view.setLoading(false);
                try {
                    User loggedInUser = get();
                    view.dispose(); 
                    
                    // Buka Dashboard Modern
                    DashboardController dashCtrl = new DashboardController(loggedInUser);
                    new DashboardView(dashCtrl).setVisible(true);
                    
                } catch (Exception ex) {
                    String cause = (ex.getCause() != null) ? ex.getCause().getMessage() : ex.getMessage();
                    view.showError(cause);
                }
            }
        };
        worker.execute();
    }
}