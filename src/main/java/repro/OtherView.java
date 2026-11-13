package repro;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.router.Route;

@Route(value = "other", layout = MainLayout.class)
public class OtherView extends Div {

    public OtherView() {
        setText("Plain Flow view 'other'");
    }
}
