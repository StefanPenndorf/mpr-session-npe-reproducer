# MPR: NPE in `AbstractMprServlet.getCurrentUI` when the HTTP session is invalidated during navigation

Navigating away from a `MprRouteAdapter` route throws a `NullPointerException` when the HTTP
session is invalidated by another thread (e.g. a Spring Security logout) while the UIDL request
holds the Vaadin session lock.

## Versions

- Java 21, Spring Boot 4.1.1 (embedded Tomcat)
- Vaadin Platform 25.2.11 (Flow 25.2.13)
- `com.vaadin:mpr-v8` 8.0.1 (`mpr-core` 8.0.1)
- Vaadin 8 Framework 8.32.2 (`vaadin-server-mpr-jakarta`, `vaadin-themes`)

## Setup

```
mvn package -DskipTests        # also compiles the AppWidgetset (~40 s)
mvn spring-boot:run            # dev mode, http://localhost:8090/
```

(Dev mode must be started with `spring-boot:run`; running the repackaged jar from a different
working directory fails in dev mode, unrelated to this bug.)

## What the app contains

- `StartRoute` (`""`): `MprRouteAdapter<LegacyStartView>` wrapping a Vaadin 8 `View` with a `Label`.
- `OtherView` (`/other`): plain Flow view. `MainLayout` has `RouterLink`s and the arm buttons.
- `@Push(transport = LONG_POLLING)` on the application shell (see below).
- `/logout`: servlet filter that calls `request.getSession(false).invalidate()` and redirects to `/`
  (no Vaadin lock, like a Spring Security logout).
- `RaceListener`: UI-wide `BeforeLeaveListener` that forces the race when leaving `StartRoute`.
  UI listeners run before `MprRouteAdapter.beforeLeave`.

## Steps to reproduce

Automatic:

1. Open http://localhost:8090/.
2. Click "Arm auto".
3. Click "Go to other".
4. The listener starts a thread that invalidates the HTTP session, waits until
   `ui.getSession().getSession() == null`, and returns. Then `MprRouteAdapter.beforeLeave` runs.

Manual:

1. Open http://localhost:8090/, click "Arm manual".
2. Click "Go to other"; the listener sleeps 10 s while holding the session lock.
3. Meanwhile open http://localhost:8090/logout in a second tab (same browser session).

## Expected

Navigation completes (or fails gracefully); no NPE from MPR.

## Actual

```
REPRO auto: invalidating HTTP session from another thread while holding the session lock
REPRO auto: wrapped session is now null
ERROR com.vaadin.flow.router.Router : Exception occurred while rendering error view 'class com.vaadin.flow.router.InternalServerError' for 'other'. Falling back to InternalServerError.

java.lang.NullPointerException: Cannot invoke "com.vaadin.flow.server.WrappedSession.getAttribute(String)" because "session" is null
	at com.vaadin.mpr.core.AbstractMprServlet.getCurrentUI(AbstractMprServlet.java:95)
	at com.vaadin.util.CurrentInstance.get(CurrentInstance.java:83)
	at com.vaadin.ui.UI.getCurrent(UI.java:979)
	at com.vaadin.mpr.MprRouteAdapter.beforeLeave(MprRouteAdapter.java:48)
	at com.vaadin.flow.router.internal.AbstractNavigationStateRenderer.executeBeforeLeaveNavigation(AbstractNavigationStateRenderer.java:608)
	...
	at com.vaadin.flow.router.Router.handleExceptionNavigation(Router.java:514)
	at com.vaadin.flow.router.Router.executeNavigation(Router.java:578)
```

The router then tries the error view and the `InternalServerError` fallback; each calls
`MprRouteAdapter.beforeLeave` again and fails with the same NPE, followed by
`NullPointerException: Cannot invoke "VaadinSession.getState()" because "UI.getSession()" is null`.

## Analysis

`AbstractMprServlet.getCurrentUI()` (fallback resolver for the Vaadin 8 `UI.getCurrent()`) does
`flowUi.getSession().getSession().getAttribute("ui." + id)` without a null check. When Tomcat
invalidates the HTTP session, Flow's `VaadinSession.valueUnbound` sets the wrapped session to
`null` without taking the session lock, so `VaadinSession.getSession()` returns `null` while a
request is still being handled. The neighbouring method `getCurrentSession` already checks for
`null`; `getCurrentUI` should do the same (return `null`).

## WebSocket vs. long polling

With WebSocket push the problem does not occur: Tomcat's `WsSessionListener` closes the WebSocket
on session invalidation and `PushHandler.connectionLost` waits for the Vaadin session lock, which
serializes the invalidation behind the running request. With long polling (a proxy without
WebSocket support, or a dropped WebSocket connection) nothing waits for the lock. Hence the
reproducer uses `Transport.LONG_POLLING`.

## Note on the attribute order

On invalidation Tomcat unbinds attributes in map order; the V8 `VaadinSession` attribute waits for
the lock in `valueUnbound`. The race only shows if the Flow `VaadinSession` attribute is unbound
before the V8 one. In this reproducer that order occurred in our verification run (Tomcat's attribute map order depends on the attribute
names, so it should be stable for this setup, but this was not checked across multiple environments).
