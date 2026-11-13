package repro;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.BeforeLeaveEvent;
import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.WrappedHttpSession;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Forces the race: while the UIDL request that leaves the MprRouteAdapter route holds
 * the Vaadin session lock, the HTTP session is invalidated by a different thread.
 */
@Component
public class RaceListener implements VaadinServiceInitListener {

    private static final Logger LOG = LoggerFactory.getLogger(RaceListener.class);

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(
                uiEvent -> uiEvent.getUI().addBeforeLeaveListener(this::beforeLeave));
    }

    private void beforeLeave(BeforeLeaveEvent event) {
        UI ui = event.getUI();
        boolean leavingStart = ui.getInternals().getActiveRouterTargetsChain().stream()
                .anyMatch(StartRoute.class::isInstance);
        if (!leavingStart) {
            return;
        }
        // fire once per arming; error-view navigations re-enter this listener
        ReproMode mode = ReproMode.current();
        ReproMode.set(ReproMode.OFF);
        switch (mode) {
            case MANUAL -> sleepForTenSeconds();
            case AUTO -> invalidateFromOtherThread(ui);
            case OFF -> { }
        }
    }

    private void sleepForTenSeconds() {
        LOG.info("REPRO manual: sleeping 10 s while holding the session lock; open /logout in another tab now");
        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void invalidateFromOtherThread(UI ui) {
        HttpSession httpSession = ((WrappedHttpSession) ui.getSession().getSession()).getHttpSession();
        LOG.info("REPRO auto: invalidating HTTP session from another thread while holding the session lock");
        Thread invalidator = new Thread(httpSession::invalidate, "repro-invalidator");
        invalidator.start();
        long deadline = System.currentTimeMillis() + 10_000;
        while (ui.getSession().getSession() != null && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        LOG.info("REPRO auto: wrapped session is now {}", ui.getSession().getSession());
    }
}
