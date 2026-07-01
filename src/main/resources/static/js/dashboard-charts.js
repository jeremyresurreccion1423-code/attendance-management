(function () {
    const green = "#16a34a";
    const greenLight = "#86efac";
    const red = "#dc2626";
    const amber = "#d97706";

    function renderLineChart(canvasId, labels, datasets) {
        const canvas = document.getElementById(canvasId);
        if (!canvas || typeof Chart === "undefined") return;
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
        if (!canvas || typeof Chart === "undefined") return;
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

    window.DashboardCharts = {
        renderAttendanceTrend: function (canvasId, trend) {
            if (!trend || !trend.labels) return;
            const datasets = [];
            if (trend.present) {
                datasets.push({ label: "Present", data: trend.present, borderColor: green, backgroundColor: greenLight, tension: 0.3, fill: false });
            }
            if (trend.late) {
                datasets.push({ label: "Late", data: trend.late, borderColor: amber, backgroundColor: "#fde68a", tension: 0.3, fill: false });
            }
            if (trend.absent) {
                datasets.push({ label: "Absent", data: trend.absent, borderColor: red, backgroundColor: "#fecaca", tension: 0.3, fill: false });
            }
            if (trend.totals) {
                datasets.push({ label: "Total Records", data: trend.totals, borderColor: green, backgroundColor: greenLight, tension: 0.3, fill: true });
            }
            renderLineChart(canvasId, trend.labels, datasets);
        },

        renderPerformanceBar: function (canvasId, chart) {
            if (!chart || !chart.labels || chart.labels.length === 0) return;
            const data = chart.grades || chart.rates || [];
            const label = chart.grades ? "Final Grade" : "Attendance Rate %";
            renderBarChart(canvasId, chart.labels, [{
                label,
                data,
                backgroundColor: greenLight,
                borderColor: green,
                borderWidth: 1
            }]);
        }
    };
})();
