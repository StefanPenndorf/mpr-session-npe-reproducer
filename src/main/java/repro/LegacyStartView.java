package repro;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener.ViewChangeEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

public class LegacyStartView extends VerticalLayout implements View {

    public LegacyStartView() {
        addComponent(new Label("Legacy Vaadin 8 view (wrapped in MprRouteAdapter)"));
    }

    @Override
    public void enter(ViewChangeEvent event) {
        // nothing to do
    }
}
