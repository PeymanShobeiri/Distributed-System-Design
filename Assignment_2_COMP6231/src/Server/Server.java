package Server;

public class Server {
    public static void main(String[] args) throws Exception {
        new Thread(() -> {
            try {
                new ServerInstance("NYK", args);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                new ServerInstance("LON", args);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();

        new Thread(() -> {
            try {
                new ServerInstance("TOK", args);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
