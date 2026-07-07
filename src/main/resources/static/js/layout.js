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

    const layoutScript = document.querySelector('script[src*="layout.js"]');
    const layoutSrc = layoutScript?.getAttribute("src") || "/js/layout.js";
    const baseSrc = layoutSrc.replace("layout.js", "");

    function appendScript(name) {
        if (document.querySelector(`script[src*="${name}"]`)) return;
        const script = document.createElement("script");
        script.src = baseSrc + name;
        script.async = false;
        document.body.appendChild(script);
    }

    appendScript("profile-menu.js");
    appendScript("forms.js");
})();
