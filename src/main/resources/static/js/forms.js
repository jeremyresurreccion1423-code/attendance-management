(() => {
    const EMAIL_RE = /^[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}$/;

    let confirmResolve = null;
    let confirmModal = null;
    let statusModal = null;

    function ensureConfirmModal() {
        if (confirmModal) return confirmModal;

        const backdrop = document.createElement("div");
        backdrop.id = "ams-confirm-modal";
        backdrop.className = "modal-backdrop confirm-modal-backdrop";
        backdrop.setAttribute("role", "dialog");
        backdrop.setAttribute("aria-modal", "true");
        backdrop.setAttribute("aria-hidden", "true");
        backdrop.innerHTML = `
            <div class="confirm-modal modal-card">
                <div class="confirm-modal-header">
                    <div class="confirm-modal-icon" aria-hidden="true">?</div>
                    <h3 class="confirm-modal-title">Confirm Action</h3>
                </div>
                <p class="confirm-modal-message"></p>
                <div class="confirm-modal-actions">
                    <button type="button" class="btn btn-secondary confirm-modal-cancel">Cancel</button>
                    <button type="button" class="btn btn-primary confirm-modal-ok">Confirm</button>
                </div>
            </div>
        `;
        document.body.appendChild(backdrop);

        const card = backdrop.querySelector(".confirm-modal");
        const titleEl = backdrop.querySelector(".confirm-modal-title");
        const messageEl = backdrop.querySelector(".confirm-modal-message");
        const iconEl = backdrop.querySelector(".confirm-modal-icon");
        const okBtn = backdrop.querySelector(".confirm-modal-ok");
        const cancelBtn = backdrop.querySelector(".confirm-modal-cancel");

        function closeConfirm(result) {
            backdrop.classList.remove("show");
            backdrop.setAttribute("aria-hidden", "true");
            document.body.style.overflow = "";
            const resolve = confirmResolve;
            confirmResolve = null;
            if (resolve) resolve(result);
        }

        okBtn.addEventListener("click", () => closeConfirm(true));
        cancelBtn.addEventListener("click", () => closeConfirm(false));
        backdrop.addEventListener("click", (event) => {
            if (event.target === backdrop) closeConfirm(false);
        });
        document.addEventListener("keydown", (event) => {
            if (!backdrop.classList.contains("show")) return;
            if (event.key === "Escape") closeConfirm(false);
        });

        confirmModal = {
            backdrop,
            card,
            titleEl,
            messageEl,
            iconEl,
            okBtn,
            cancelBtn,
            closeConfirm,
        };
        return confirmModal;
    }

    function confirmAction(message, options = {}) {
        const modal = ensureConfirmModal();
        const type = options.type || "default";
        const title = options.title || "Confirm Action";
        const okText = options.okText || "Confirm";
        const cancelText = options.cancelText || "Cancel";

        modal.card.classList.remove("confirm-modal--danger", "confirm-modal--warning");
        modal.okBtn.classList.remove("btn-confirm-danger");
        modal.okBtn.classList.add("btn-primary");

        if (type === "danger") {
            modal.card.classList.add("confirm-modal--danger");
            modal.okBtn.classList.remove("btn-primary");
            modal.okBtn.classList.add("btn-confirm-danger");
            modal.iconEl.textContent = "!";
        } else if (type === "warning") {
            modal.card.classList.add("confirm-modal--warning");
            modal.iconEl.textContent = "↪";
        } else {
            modal.iconEl.textContent = "?";
        }

        modal.titleEl.textContent = title;
        modal.messageEl.textContent = message || "Are you sure you want to continue?";
        modal.okBtn.textContent = okText;
        modal.cancelBtn.textContent = cancelText;

        modal.backdrop.classList.add("show");
        modal.backdrop.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
        modal.okBtn.focus();

        return new Promise((resolve) => {
            confirmResolve = resolve;
        });
    }

    function ensureStatusModal() {
        if (statusModal) return statusModal;

        const backdrop = document.createElement("div");
        backdrop.id = "ams-status-modal";
        backdrop.className = "modal-backdrop status-modal-backdrop";
        backdrop.setAttribute("role", "dialog");
        backdrop.setAttribute("aria-modal", "true");
        backdrop.setAttribute("aria-hidden", "true");
        backdrop.innerHTML = `
            <div class="status-modal modal-card">
                <div class="status-modal-header">
                    <div class="status-modal-icon" aria-hidden="true">✓</div>
                    <h3 class="status-modal-title">Success</h3>
                </div>
                <p class="status-modal-message"></p>
                <div class="status-modal-actions">
                    <button type="button" class="btn btn-primary status-modal-ok">OK</button>
                </div>
            </div>
        `;
        document.body.appendChild(backdrop);

        const closeBtn = backdrop.querySelector(".status-modal-ok");
        const close = () => {
            backdrop.classList.remove("show");
            backdrop.setAttribute("aria-hidden", "true");
            document.body.style.overflow = "";
        };

        closeBtn.addEventListener("click", close);
        backdrop.addEventListener("click", (event) => {
            if (event.target === backdrop) close();
        });
        document.addEventListener("keydown", (event) => {
            if (!backdrop.classList.contains("show")) return;
            if (event.key === "Escape") close();
        });

        statusModal = {
            backdrop,
            titleEl: backdrop.querySelector(".status-modal-title"),
            messageEl: backdrop.querySelector(".status-modal-message"),
            iconEl: backdrop.querySelector(".status-modal-icon"),
            closeBtn,
        };

        return statusModal;
    }

    function showStatusModal(type, message) {
        const modal = ensureStatusModal();
        const normalizedType = type === "error" ? "error" : "success";

        modal.backdrop.classList.remove("status-modal--success", "status-modal--error");
        modal.backdrop.classList.add(normalizedType === "error" ? "status-modal--error" : "status-modal--success");

        modal.titleEl.textContent = normalizedType === "error" ? "Action Failed" : "Success";
        modal.iconEl.textContent = normalizedType === "error" ? "!" : "✓";
        modal.messageEl.textContent = message || (normalizedType === "error" ? "Something went wrong." : "Saved successfully.");

        modal.backdrop.classList.add("show");
        modal.backdrop.setAttribute("aria-hidden", "false");
        document.body.style.overflow = "hidden";
        modal.closeBtn.focus();
    }

    function showFieldError(input, message) {
        if (!input) return;
        input.classList.add("input-error");
        let hint = input.parentElement.querySelector(".field-error");
        if (!hint) {
            hint = document.createElement("small");
            hint.className = "field-error";
            input.parentElement.appendChild(hint);
        }
        hint.textContent = message;
    }

    function clearFieldError(input) {
        if (!input) return;
        input.classList.remove("input-error");
        const hint = input.parentElement.querySelector(".field-error");
        if (hint) hint.remove();
    }

    function validatePasswordForm(form) {
        const newPass = form.querySelector('input[name="newPassword"]');
        const confirm = form.querySelector('input[name="confirmPassword"]');
        if (!newPass || !confirm) return true;

        clearFieldError(newPass);
        clearFieldError(confirm);

        if (newPass.value.length < 6) {
            showFieldError(newPass, "Password must be at least 6 characters.");
            return false;
        }
        if (newPass.value !== confirm.value) {
            showFieldError(confirm, "Passwords do not match.");
            return false;
        }
        return true;
    }

    function validateEmailInputs(form) {
        let valid = true;
        form.querySelectorAll('input[type="email"]').forEach((input) => {
            clearFieldError(input);
            if (input.value && !EMAIL_RE.test(input.value.trim())) {
                showFieldError(input, "Please enter a valid email.");
                valid = false;
            }
        });
        return valid;
    }

    function validateMarkForm(form) {
        let valid = true;
        ["quizScore", "examScore", "assignmentScore"].forEach((name) => {
            const input = form.querySelector(`input[name="${name}"]`);
            if (!input || input.value === "") return;
            const val = Number(input.value);
            clearFieldError(input);
            if (Number.isNaN(val) || val < 0 || val > 100) {
                showFieldError(input, "Score must be between 0 and 100.");
                valid = false;
            }
        });
        return valid;
    }

    function validatePhotoForm(form) {
        const file = form.querySelector('input[type="file"][name="photo"]');
        if (!file || !file.files.length) return true;
        clearFieldError(file);
        const selected = file.files[0];
        if (!selected.type.startsWith("image/")) {
            showFieldError(file, "Please select an image file.");
            return false;
        }
        if (selected.size > 5 * 1024 * 1024) {
            showFieldError(file, "Image must be 5MB or smaller.");
            return false;
        }
        return true;
    }

    function validateRequiredForm(form) {
        let valid = true;
        form.querySelectorAll("[required]").forEach((input) => {
            clearFieldError(input);
            if (!input.value || !String(input.value).trim()) {
                showFieldError(input, "This field is required.");
                valid = false;
            }
        });
        return valid;
    }

    document.addEventListener("click", (event) => {
        const logoutLink = event.target.closest('a[href="/logout"], a.profile-logout');
        if (logoutLink) {
            event.preventDefault();
            confirmAction("Are you sure you want to log out?", {
                type: "warning",
                title: "Log Out",
                okText: "Yes, Log Out",
            }).then((confirmed) => {
                if (confirmed) window.location.href = "/logout";
            });
        }
    });

    document.addEventListener("submit", (event) => {
        const form = event.target;
        if (!(form instanceof HTMLFormElement)) return;

        if (form.classList.contains("confirm-delete") || form.hasAttribute("data-confirm-delete")) {
            event.preventDefault();
            const msg = form.getAttribute("data-confirm-message")
                || "Are you sure you want to delete this record? This action cannot be undone.";
            confirmAction(msg, {
                type: "danger",
                title: "Confirm Delete",
                okText: "Yes, Delete",
            }).then((confirmed) => {
                if (confirmed) {
                    applyFormLoading(form);
                    form.submit();
                }
            });
            return;
        }

        if (form.classList.contains("confirm-action") || form.hasAttribute("data-confirm")) {
            event.preventDefault();
            const msg = form.getAttribute("data-confirm-message")
                || "Are you sure you want to proceed?";
            confirmAction(msg, {
                type: "default",
                title: "Confirm Action",
                okText: "Yes, Proceed",
            }).then((confirmed) => {
                if (confirmed) {
                    applyFormLoading(form);
                    form.submit();
                }
            });
            return;
        }

        if (form.classList.contains("ams-validate") || form.hasAttribute("data-validate")) {
            if (!validateRequiredForm(form)) {
                event.preventDefault();
                return;
            }
            if (!validateEmailInputs(form)) {
                event.preventDefault();
                return;
            }
            if (form.querySelector('input[name="newPassword"]') && !validatePasswordForm(form)) {
                event.preventDefault();
                return;
            }
            if (form.querySelector('input[name="quizScore"]') && !validateMarkForm(form)) {
                event.preventDefault();
                return;
            }
            if (form.querySelector('input[type="file"][name="photo"]') && !validatePhotoForm(form)) {
                event.preventDefault();
                return;
            }
            const loginPassword = form.querySelector('input[name="password"]');
            if (loginPassword && loginPassword.value && loginPassword.value.length < 6) {
                showFieldError(loginPassword, "Password must be at least 6 characters.");
                event.preventDefault();
                return;
            }
        }

        if (!event.defaultPrevented && form.dataset.noLoading !== "true") {
            applyFormLoading(form);
        }
    });

    function isAuthPage() {
        return Boolean(document.querySelector(".auth-page, .login-page, [data-auth-page]"));
    }

    function applyFormLoading(form) {
        const btn = form.querySelector('button[type="submit"]:not([disabled])');
        if (btn) setButtonLoading(btn, true);
        form.classList.add("is-submitting");
    }

    function setButtonLoading(button, loading) {
        if (!button) return;
        if (loading) {
            if (!button.dataset.originalText) {
                button.dataset.originalText = button.textContent.trim();
            }
            button.disabled = true;
            button.classList.add("btn-loading");
            button.innerHTML = '<span class="loading-spinner" aria-hidden="true"></span><span>Please wait...</span>';
        }
    }

    function initLoginUsernameRestore() {
        const loginForm = document.querySelector('form[action*="/login"]');
        if (!loginForm) return;
        const usernameInput = loginForm.querySelector('input[name="username"]');
        if (!usernameInput) return;
        const storageKey = "ams_login_username";
        const hasLoginError = Boolean(document.querySelector(".alert.alert-error"));
        if (hasLoginError) {
            const saved = sessionStorage.getItem(storageKey);
            if (saved) usernameInput.value = saved;
        } else {
            sessionStorage.removeItem(storageKey);
        }
        loginForm.addEventListener("submit", () => {
            const value = usernameInput.value.trim();
            if (value) sessionStorage.setItem(storageKey, value);
        });
    }

    function initEmptyStates() {
        document.querySelectorAll("table tbody tr").forEach((row) => {
            const cell = row.querySelector("td[colspan]");
            if (!cell || row.children.length !== 1) return;
            const text = cell.textContent.trim().toLowerCase();
            if (text.includes("no ") || text.includes("walang") || text.includes("yet") || text.includes("empty")) {
                row.classList.add("empty-state-row");
                cell.classList.add("empty-state-cell");
            }
        });
    }

    let sessionToastEl = null;

    function hideSessionToast() {
        if (sessionToastEl) {
            sessionToastEl.classList.remove("show");
        }
    }

    function showSessionToast(message, isExpired) {
        if (!sessionToastEl) {
            sessionToastEl = document.createElement("div");
            sessionToastEl.className = "session-timeout-toast";
            sessionToastEl.setAttribute("role", "alert");
            sessionToastEl.innerHTML = '<span class="session-timeout-icon" aria-hidden="true">⏱</span><span class="session-timeout-text"></span>';
            document.body.appendChild(sessionToastEl);
        }
        sessionToastEl.querySelector(".session-timeout-text").textContent = message;
        sessionToastEl.classList.toggle("session-timeout-toast--expired", Boolean(isExpired));
        sessionToastEl.classList.add("show");
    }

    function initSessionTimeout() {
        if (isAuthPage()) return;
        const isLoggedIn = document.querySelector('a[href="/logout"], a.profile-logout, .sidebar');
        if (!isLoggedIn) return;

        const timeoutMs = (Number(document.body.dataset.sessionMinutes) || 30) * 60 * 1000;
        const warningMs = 2 * 60 * 1000;
        let expiry = Date.now() + timeoutMs;
        let warned = false;

        const resetTimer = () => {
            expiry = Date.now() + timeoutMs;
            warned = false;
            hideSessionToast();
        };

        ["click", "keydown", "mousemove", "scroll", "touchstart"].forEach((evt) => {
            document.addEventListener(evt, resetTimer, { passive: true });
        });

        setInterval(() => {
            const remaining = expiry - Date.now();
            if (remaining <= 0) {
                showSessionToast("Your session has expired. Redirecting to login...", true);
                setTimeout(() => { window.location.href = "/login?error=session"; }, 2500);
            } else if (remaining <= warningMs && !warned) {
                warned = true;
                showSessionToast("Your session will expire in 2 minutes due to inactivity.");
            }
        }, 10000);
    }

    document.querySelectorAll('form[action*="/forgot-password"]:not([action*="request-otp"])').forEach((form) => {
        if (form.querySelector('input[name="newPassword"]')) {
            form.classList.add("confirm-action");
            form.setAttribute("data-confirm-message", "Reset your password with this OTP?");
        }
    });

    document.querySelectorAll('form[action*="/profile/password"]').forEach((form) => {
        form.classList.add("ams-validate", "confirm-action");
        form.setAttribute("data-confirm-message", "Change your password?");
    });

    document.querySelectorAll('form[action*="/forgot-password"]').forEach((form) => {
        form.classList.add("ams-validate");
    });

    document.querySelectorAll('form[action*="/profile/photo"]').forEach((form) => {
        form.classList.add("ams-validate");
    });

    document.querySelectorAll('form[action*="/delete"]').forEach((form) => {
        form.classList.add("confirm-delete");
    });

    document.querySelectorAll('form[action*="/publish"]').forEach((form) => {
        form.classList.add("confirm-action");
        form.setAttribute("data-confirm-message", "Publish this schedule to students?");
    });

    document.querySelectorAll('form[action*="/attendance/manual"]').forEach((form) => {
        form.classList.add("confirm-action");
        form.setAttribute("data-confirm-message", "Mark attendance for this student?");
    });

    document.querySelectorAll('form[action*="/marks/compute"]').forEach((form) => {
        form.classList.add("confirm-action");
        form.setAttribute("data-confirm-message", "Compute final grades for all students in this subject?");
    });

    document.querySelectorAll('form[action*="/marks"]:not([action*="/marks/compute"])').forEach((form) => {
        if (form.method && form.method.toLowerCase() === "post" && form.querySelector('input[name="quizScore"]')) {
            form.classList.add("ams-validate");
        }
    });

    // Show flash messages as centered popups on app pages; keep inline on auth pages.
    const authPage = isAuthPage();
    const successAlert = document.querySelector(".alert.alert-success");
    const errorAlert = document.querySelector(".alert.alert-error");
    if (successAlert && successAlert.textContent.trim()) {
        if (!authPage) {
            showStatusModal("success", successAlert.textContent.trim());
            successAlert.style.display = "none";
        }
    } else if (errorAlert && errorAlert.textContent.trim()) {
        if (!authPage) {
            showStatusModal("error", errorAlert.textContent.trim());
            errorAlert.style.display = "none";
        }
    }

    initLoginUsernameRestore();
    initEmptyStates();
    initSessionTimeout();

    window.amsConfirm = confirmAction;
    window.amsShowStatus = showStatusModal;
})();
