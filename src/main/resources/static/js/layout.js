(() => {
    const toggleButton = document.querySelector("[data-sidebar-toggle]");
    const sidebar = document.querySelector(".sidebar");

    if (toggleButton && sidebar) {
        const storageKey = "ams-sidebar-collapsed";
        const applyState = (collapsed) => {
            document.body.classList.toggle("sidebar-collapsed", collapsed);
            toggleButton.setAttribute("aria-expanded", String(!collapsed));
            toggleButton.setAttribute("title", collapsed ? "Show menu" : "Hide menu");
            toggleButton.textContent = collapsed ? "⋮" : "☰";
            toggleButton.setAttribute("aria-label", collapsed ? "Show sidebar menu" : "Hide sidebar menu");
        };

        const stored = localStorage.getItem(storageKey) === "1";
        applyState(stored);

        toggleButton.addEventListener("click", () => {
            const collapsed = !document.body.classList.contains("sidebar-collapsed");
            applyState(collapsed);
            localStorage.setItem(storageKey, collapsed ? "1" : "0");
        });
    }

    if (!document.querySelector('script[src*="forms.js"]')) {
        const formsScript = document.createElement("script");
        const layoutSrc = document.querySelector('script[src*="layout.js"]')?.getAttribute("src") || "/js/layout.js";
        formsScript.src = layoutSrc.replace("layout.js", "forms.js");
        formsScript.async = false;
        document.head.appendChild(formsScript);
    }
})();

function closeAllProfileMenus() {
    document.querySelectorAll(".profile-menu").forEach((menu) => menu.classList.remove("show"));
    document.querySelectorAll(".profile-panel").forEach((panel) => panel.classList.remove("show"));
}

function toggleProfileMenu(button) {
    const widget = button.closest(".page-header-profile-widget") || button.closest(".sidebar-profile-widget");
    const menu = widget ? widget.querySelector(".profile-menu") : null;
    if (!menu) return;
    const shouldOpen = !menu.classList.contains("show");
    closeAllProfileMenus();
    if (shouldOpen) menu.classList.add("show");
}

function toggleProfilePanel(panelId) {
    closeAllProfileMenus();
    const panel = document.getElementById(panelId);
    if (panel) panel.classList.add("show");
}

document.addEventListener("click", function (event) {
    const target = event.target;

    const profileAction = target.closest(".profile-menu-action[data-open-profile]");
    if (profileAction) {
        event.preventDefault();
        const widget = profileAction.closest(".page-header-profile-widget") || profileAction.closest(".sidebar-profile-widget");
        const panel = widget ? widget.querySelector(".profile-panel") : document.getElementById("profile-panel");
        if (panel) {
            toggleProfilePanel(panel.id);
        } else {
            window.location.href = "/profile";
        }
        return;
    }

    if (!target.closest(".page-header-profile-widget") && !target.closest(".sidebar-profile-widget")) {
        closeAllProfileMenus();
    }
});

document.querySelectorAll(".profile-trigger").forEach((trigger) => {
    if (trigger.getAttribute("onclick")) {
        return;
    }
    trigger.addEventListener("click", (event) => {
        event.preventDefault();
        toggleProfileMenu(trigger);
    });
});
