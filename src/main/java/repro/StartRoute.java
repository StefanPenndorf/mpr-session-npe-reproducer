package repro;

import com.vaadin.flow.router.Route;
import com.vaadin.mpr.MprRouteAdapter;

@Route(value = "", layout = MainLayout.class)
public class StartRoute extends MprRouteAdapter<LegacyStartView> {
}
