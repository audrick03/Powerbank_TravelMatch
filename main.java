import View.LoginView;

public class main {
    public static void main(String[] args) {
        LoginView loginView = new LoginView();
        loginView.showView();

        AdminView adminView = new AdminView();
        adminView.showView();
    }
}
