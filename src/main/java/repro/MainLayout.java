package repro;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends Div implements RouterLayout {

    public MainLayout() {
        Span state = new Span("mode: " + ReproMode.current());
        Button manual = new Button("Arm manual (10 s sleep when leaving start route)", e -> {
            ReproMode.set(ReproMode.MANUAL);
            state.setText("mode: " + ReproMode.current());
        });
        manual.setId("arm-manual");
        Button auto = new Button("Arm auto (invalidate session from other thread)", e -> {
            ReproMode.set(ReproMode.AUTO);
            state.setText("mode: " + ReproMode.current());
        });
        auto.setId("arm-auto");
        RouterLink toOther = new RouterLink("Go to other", OtherView.class);
        toOther.setId("to-other");
        RouterLink toStart = new RouterLink("Go to start", StartRoute.class);
        toStart.setId("to-start");
        add(new HorizontalLayout(toStart, toOther, manual, auto, state));
    }
}
