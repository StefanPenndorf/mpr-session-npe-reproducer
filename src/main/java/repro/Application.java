package repro;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Long polling simulates a proxy without WebSocket support (or a dropped WebSocket).
// With WebSocket, Tomcat closes the socket on session invalidation and the push
// handler waits for the Vaadin session lock, which serializes the race away.
@Push(value = PushMode.AUTOMATIC, transport = Transport.LONG_POLLING)
@SpringBootApplication
public class Application implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
