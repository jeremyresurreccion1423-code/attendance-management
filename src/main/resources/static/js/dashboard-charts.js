(function () {
    const green = "#16a34a";
    const greenLight = "#86efac";
    const red = "#dc2626";
    const amber = "#d97706";
    const teacherBlue = "#800020";
    const teacherSky = "#A52A2A";
    const teacherBlues = ["#5C0013", "#73001A", "#800020", "#A52A2A", "#C0392B", "#D6A4AE"];
    const adminBlue = "#6B9BD2";
    const adminBlueLight = "#E8F1F8";
    const adminBlues = ["#3D5A7A", "#4A78AF", "#5A89C0", "#6B9BD2", "#7EABD8", "#A8C5DE"];
    const isTeacherTheme = function () {
        return document.body && document.body.classList.contains("teacher-theme");
    };
    const isAdminTheme = function () {
        return document.body && document.body.classList.contains("admin-theme");
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

            const datasets = [];
            if (data.present.length) {
                datasets.push({
                    label: "Present",
                    data: data.present,
                    borderColor: teacher ? "#10B981" : (admin ? adminBlue : green),
                    backgroundColor: teacher ? "rgba(16, 185, 129, 0.25)" : (admin ? "rgba(107, 155, 210, 0.28)" : greenLight),
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
                    borderColor: teacher ? teacherBlue : (admin ? "#5A89C0" : green),
                    backgroundColor: teacher ? "rgba(128, 0, 32, 0.18)" : (admin ? "rgba(90, 137, 192, 0.18)" : greenLight),
                    tension: 0.3,
                    fill: true
                });
            }
            if (datasets.length === 0) {
                datasets.push({
                    label: "Records",
                    data: data.labels.map(function () { return 0; }),
                    borderColor: teacher ? teacherSky : (admin ? adminBlue : green),
                    backgroundColor: teacher ? "rgba(165, 42, 42, 0.2)" : (admin ? adminBlueLight : greenLight),
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
            const backgroundColor = teacher
                ? data.map(function (_, i) { return teacherBlues[i % teacherBlues.length]; })
                : (admin ? data.map(function (_, i) { return adminBlues[i % adminBlues.length]; }) : greenLight);
            const borderColor = teacher
                ? data.map(function (_, i) { return teacherBlues[i % teacherBlues.length]; })
                : (admin ? data.map(function (_, i) { return adminBlues[i % adminBlues.length]; }) : green);
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
