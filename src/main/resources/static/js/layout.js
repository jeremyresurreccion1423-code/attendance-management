(() => {
    const toggleButton = document.querySelector("[data-sidebar-toggle]");
    const sidebar = document.querySelector(".sidebar");

    function ensureForgotPasswordInProfileMenus() {
        document.querySelectorAll(".profile-menu").forEach((menu) => {
            if (menu.querySelector('a[href="/forgot-password"], a[href*="forgot-password"], [data-forgot-password]')) {
                return;
            }
            const logout = menu.querySelector(".profile-logout");
            const link = document.createElement("a");
            link.href = "/forgot-password";
            link.className = "profile-menu-action";
            link.setAttribute("data-forgot-password", "true");
            link.textContent = "Forgot Password";
            if (logout) {
                menu.insertBefore(link, logout);
            } else {
                menu.appendChild(link);
            }
        });
    }

    ensureForgotPasswordInProfileMenus();

    document.querySelectorAll("[data-ams-greeting]").forEach((el) => {
        const name = el.getAttribute("data-name") || "Admin";
        const hour = new Date().getHours();
        const greeting = hour < 12 ? "Good morning" : hour < 18 ? "Good afternoon" : "Good evening";
        el.textContent = `${greeting}, ${name}!`;
    });

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

        document.querySelectorAll(".profile-menu-action").forEach((action) => {
            if (action.textContent && action.textContent.trim().toLowerCase() === "my profile") {
                action.addEventListener("click", (event) => {
                    event.preventDefault();
                    window.location.href = "/profile";
                });
            }
        });

        toggleButton.addEventListener("click", () => {
            const collapsed = !document.body.classList.contains("sidebar-collapsed");
            applyState(collapsed);
            localStorage.setItem(storageKey, collapsed ? "1" : "0");
        });
    }

    window.toggleSidebarGroup = function (button) {
        const group = button.closest(".sidebar-group");
        if (!group) return;
        const willOpen = !group.classList.contains("open");
        group.classList.toggle("open");
        button.setAttribute("aria-expanded", String(willOpen));
    };

    document.querySelectorAll(".sidebar-group.open .sidebar-group-btn").forEach((button) => {
        button.setAttribute("aria-expanded", "true");
    });

    if (!document.querySelector('script[src*="forms.js"]')) {
        const formsScript = document.createElement("script");
        const layoutSrc = document.querySelector('script[src*="layout.js"]')?.getAttribute("src") || "/js/layout.js";
        formsScript.src = layoutSrc.replace("layout.js", "forms.js");
        formsScript.async = false;
        document.head.appendChild(formsScript);
    }
})();
