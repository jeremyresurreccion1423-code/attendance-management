(function () {
    const green = "#16a34a";
    const greenLight = "#86efac";
    const red = "#dc2626";
    const amber = "#d97706";
    const rainbow = ["#ef4444", "#f59e0b", "#22c55e", "#3b82f6", "#a855f7", "#ec4899"];
    const teacherBlue = "#2563EB";
    const teacherSky = "#3B82F6";
    const teacherAccent = "#60A5FA";
    const teacherBlues = ["#1E3A8A", "#1D4ED8", "#2563EB", "#3B82F6", "#60A5FA", "#93C5FD"];
    const purple = "#A855F7";
    const purpleLight = "#E9D5FF";
    const purpleShades = ["#1E1B4B", "#4C1D95", "#7C3AED", "#A855F7", "#C084FC", "#9333EA"];
    const controlCenter = ["#F59E0B", "#10B981", "#3B82F6", "#111827", "#6B7280", "#EF4444"];
    const isTeacherTheme = function () {
        return document.body && document.body.classList.contains("teacher-theme");
    };
    const isAdminTheme = function () {
        return document.body && document.body.classList.contains("admin-theme");
    };
    const isSuperAdminTheme = function () {
        return document.body && document.body.classList.contains("super-admin-theme");
    };

    function ensureChartJs() {
        if (typeof Chart === "undefined") {
            console.error("Chart.js is not loaded. Charts cannot render.");
            return false;
        }
        return true;
    }

    function renderLineChart(canvasId, labels, datasets) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !ensureChartJs()) return;
        const existing = Chart.getChart(canvas);
        if (existing) existing.destroy();
        new Chart(canvas, {
            type: "line",
            data: { labels, datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: "bottom" } },
                scales: { y: { beginAtZero: true, ticks: { precision: 0 } } }
            }
        });
    }

    function renderBarChart(canvasId, labels, datasets) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || !ensureChartJs()) return;
        const existing = Chart.getChart(canvas);
        if (existing) existing.destroy();
        new Chart(canvas, {
            type: "bar",
            data: { labels, datasets },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { position: "bottom" } },
                scales: { y: { beginAtZero: true, max: 100 } }
            }
        });
    }

    function normalizeTrend(trend) {
        if (!trend || typeof trend !== "object") {
            return { labels: [], present: [], absent: [], late: [], totals: [] };
        }
        return {
            labels: Array.isArray(trend.labels) ? trend.labels : [],
            present: Array.isArray(trend.present) ? trend.present : [],
            absent: Array.isArray(trend.absent) ? trend.absent : [],
            late: Array.isArray(trend.late) ? trend.late : [],
            totals: Array.isArray(trend.totals) ? trend.totals : []
        };
    }

    window.DashboardCharts = {
        renderAttendanceTrend: function (canvasId, trend) {
            const data = normalizeTrend(trend);
            if (data.labels.length === 0) return;
            const teacher = isTeacherTheme();
            const admin = isAdminTheme();
            const superAdmin = isSuperAdminTheme();

            const datasets = [];
            if (data.present.length) {
                datasets.push({
                    label: "Present",
                    data: data.present,
                    borderColor: teacher ? "#10B981" : (superAdmin ? controlCenter[1] : (admin ? purple : green)),
                    backgroundColor: teacher ? "rgba(16, 185, 129, 0.25)" : (superAdmin ? "rgba(16, 185, 129, 0.25)" : (admin ? "rgba(196, 181, 253, 0.35)" : greenLight)),
                    tension: 0.3,
                    fill: false
                });
            }
            if (data.late.length) {
                datasets.push({ label: "Late", data: data.late, borderColor: amber, backgroundColor: "#fde68a", tension: 0.3, fill: false });
            }
            if (data.absent.length) {
                datasets.push({ label: "Absent", data: data.absent, borderColor: red, backgroundColor: "#fecaca", tension: 0.3, fill: false });
            }
            if (data.totals.length) {
                datasets.push({
                    label: "Total Records",
                    data: data.totals,
                    borderColor: teacher ? teacherBlue : (superAdmin ? controlCenter[0] : (admin ? "#7e22ce" : green)),
                    backgroundColor: teacher ? "rgba(37, 99, 235, 0.18)" : (superAdmin ? "rgba(245, 158, 11, 0.2)" : (admin ? "rgba(126, 34, 206, 0.18)" : greenLight)),
                    tension: 0.3,
                    fill: true
                });
            }
            if (datasets.length === 0) {
                datasets.push({
                    label: "Records",
                    data: data.labels.map(function () { return 0; }),
                    borderColor: teacher ? teacherSky : (superAdmin ? controlCenter[2] : (admin ? purple : green)),
                    backgroundColor: teacher ? "rgba(59, 130, 246, 0.2)" : (superAdmin ? "rgba(59, 130, 246, 0.2)" : (admin ? purpleLight : greenLight)),
                    tension: 0.3,
                    fill: false
                });
            }
            renderLineChart(canvasId, data.labels, datasets);
        },

        renderPerformanceBar: function (canvasId, chart) {
            if (!chart || typeof chart !== "object") return;
            const labels = Array.isArray(chart.labels) ? chart.labels : [];
            const data = chart.grades || chart.rates || [];
            if (labels.length === 0) return;
            const label = chart.grades ? "Final Grade" : "Attendance Rate %";
            const teacher = isTeacherTheme();
            const admin = isAdminTheme();
            const superAdmin = isSuperAdminTheme();
            const backgroundColor = teacher
                ? data.map(function (_, i) { return teacherBlues[i % teacherBlues.length]; })
                : (superAdmin
                    ? data.map(function (_, i) { return controlCenter[i % controlCenter.length]; })
                    : (admin ? data.map(function (_, i) { return purpleShades[i % purpleShades.length]; }) : greenLight));
            const borderColor = teacher
                ? data.map(function (_, i) { return teacherBlues[i % teacherBlues.length]; })
                : (superAdmin
                    ? data.map(function (_, i) { return controlCenter[i % controlCenter.length]; })
                    : (admin ? data.map(function (_, i) { return purpleShades[i % purpleShades.length]; }) : green));
            renderBarChart(canvasId, labels, [{
                label: label,
                data: data,
                backgroundColor: backgroundColor,
                borderColor: borderColor,
                borderWidth: 1
            }]);
        }
    };
})();
