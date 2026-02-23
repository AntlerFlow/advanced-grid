package org.vaadin.addons.antlerflow.grid.layout;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.server.menu.MenuConfiguration;
import com.vaadin.flow.server.menu.MenuEntry;

public class MainLayout extends AppLayout {

    public MainLayout() {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("Advanced Grid Examples");
        title.getStyle().set("font-size", "1.125rem").set("margin", "0");

        SideNav nav = createSideNav();
        nav.getStyle().set("margin", "var(--vaadin-gap-s)");

        Scroller scroller = new Scroller(nav);

        addToDrawer(scroller);
        addToNavbar(toggle, title);
    }

    private SideNav createSideNav() {
        SideNav sideNav = new SideNav();
        sideNav.setHeightFull();
        MenuConfiguration.getMenuEntries()
                .forEach(entry -> sideNav.addItem(createSideNavItem(entry)));
        return sideNav;
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        String title = menuEntry.title();
        SideNavItem item;
        if (menuEntry.icon() != null) {
            item = new SideNavItem(title, menuEntry.path(), new Icon(menuEntry.icon()));
        } else {
            item = new SideNavItem(title, menuEntry.path());
        }
        item.getElement().setAttribute("title", menuEntry.title());
        return item;
    }
}
