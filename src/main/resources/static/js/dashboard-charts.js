(function () {
    const green = "#16a34a";
    const greenLight = "#86efac";
    const red = "#dc2626";
    const amber = "#d97706";
    const rainbow = ["#ef4444", "#f59e0b", "#22c55e", "#3b82f6", "#a855f7", "#ec4899"];
    const isTeacherTheme = function () {
        return document.body && document.body.classList.contains("teacher-theme");
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

            const datasets = [];
            if (data.present.length) {
                datasets.push({
                    label: "Present",
                    data: data.present,
                    borderColor: teacher ? rainbow[2] : green,
                    backgroundColor: teacher ? "rgba(34, 197, 94, 0.25)" : greenLight,
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
                    borderColor: teacher ? rainbow[4] : green,
                    backgroundColor: teacher ? "rgba(168, 85, 247, 0.2)" : greenLight,
                    tension: 0.3,
                    fill: true
                });
            }
            if (datasets.length === 0) {
                datasets.push({
                    label: "Records",
                    data: data.labels.map(function () { return 0; }),
                    borderColor: teacher ? rainbow[5] : green,
                    backgroundColor: teacher ? "rgba(236, 72, 153, 0.2)" : greenLight,
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
            const backgroundColor = teacher
                ? data.map(function (_, i) { return rainbow[i % rainbow.length]; })
                : greenLight;
            const borderColor = teacher
                ? data.map(function (_, i) { return rainbow[i % rainbow.length]; })
                : green;
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
