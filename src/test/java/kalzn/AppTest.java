package kalzn;

import io.javalin.Javalin;

public class AppTest {
    public static void main(String[] args) {
        var app = Javalin.create();
        app.wsBefore("/private/*", wsConfig -> {
            wsConfig.onMessage(ctx -> {
                System.out.println("private1");
            });
        });
        app.wsBefore("/private/ws/*", wsConfig -> {
            wsConfig.onMessage(ctx -> {
                System.out.println("private2");
            });
        });
        app.ws("/private/ws/ww", wsConfig -> {
            wsConfig.onMessage(ctx -> {
                System.out.println("private3");
            });
        });
        app.start(8084);
    }


}
