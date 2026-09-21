package modern_pos.utils;

import modern_pos.model.User;

// Aplikasi desktop: satu user login per proses, jadi cukup field statis.
public class Session {
    public static User currentUser;
}
