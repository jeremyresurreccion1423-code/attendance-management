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

    if (target.closest(".profile-trigger")) {
        return;
    }

    if (target.closest(".profile-menu-action[data-open-profile]")) {
        event.preventDefault();
        const action = target.closest(".profile-menu-action[data-open-profile]");
        const widget = action.closest(".page-header-profile-widget") || action.closest(".sidebar-profile-widget");
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

function initProfileTriggers() {
    document.querySelectorAll(".profile-trigger").forEach((trigger) => {
        if (trigger.dataset.profileBound === "true" || trigger.getAttribute("onclick")) return;
        trigger.dataset.profileBound = "true";
        trigger.addEventListener("click", (event) => {
            event.preventDefault();
            event.stopPropagation();
            toggleProfileMenu(trigger);
        });
    });

    document.querySelectorAll(".profile-menu").forEach((menu) => {
        if (menu.querySelector("[data-forgot-password]")) return;
        const logout = menu.querySelector(".profile-logout");
        const link = document.createElement("a");
        link.href = "/forgot-password";
        link.className = "profile-menu-action";
        link.setAttribute("data-forgot-password", "true");
        link.textContent = "Forgot Password";
        if (logout) menu.insertBefore(link, logout);
        else menu.appendChild(link);
    });
}

if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initProfileTriggers);
} else {
    initProfileTriggers();
}
