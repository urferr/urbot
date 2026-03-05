package com.embabel.urbot.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import com.vaadin.flow.theme.Theme;

/**
 * Vaadin app shell configuration with Push enabled for async UI updates.
 * WebSocket keeps a persistent connection that survives tab backgrounding,
 * unlike LONG_POLLING which relies on repeated HTTP requests that browsers
 * throttle in inactive tabs (causing spurious session loss).
 */
@Push(value = PushMode.AUTOMATIC, transport = Transport.WEBSOCKET_XHR)
@Theme("urbot")
public class AppShellConfig implements AppShellConfigurator {
}
