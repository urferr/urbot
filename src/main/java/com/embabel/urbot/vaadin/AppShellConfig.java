package com.embabel.urbot.vaadin;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.Theme;

/**
 * Vaadin app shell configuration with Push enabled for async UI updates.
 */
@Push(value = PushMode.AUTOMATIC)
@Theme("urbot")
public class AppShellConfig implements AppShellConfigurator {
}
