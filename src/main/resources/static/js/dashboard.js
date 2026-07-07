const PortalDashboard = (() => {
    let topChart;
    let latencyChart;
    let refreshTimer;

    function renderShell() {
        const el = document.getElementById('view-dashboard');
        el.innerHTML = `
            <div class="stats-grid">
                <div class="stat-card"><h4>Requests Served</h4><div class="value" id="stat-total">0</div></div>
                <div class="stat-card"><h4>Success Rate</h4><div class="value" id="stat-success">0%</div></div>
                <div class="stat-card"><h4>Failure Rate</h4><div class="value" id="stat-failure">0%</div></div>
                <div class="stat-card"><h4>Avg Latency</h4><div class="value" id="stat-avg-latency">0 ms</div></div>
                <div class="stat-card"><h4>P95 Latency</h4><div class="value" id="stat-p95-latency">0 ms</div></div>
            </div>
            <div class="charts-grid">
                <div class="card">
                    <h3>Top 10 Called Services</h3>
                    <canvas id="top-services-chart" height="220"></canvas>
                </div>
                <div class="card">
                    <h3>Response Latency Distribution</h3>
                    <canvas id="latency-chart" height="220"></canvas>
                </div>
            </div>
        `;
    }

    function updateCharts(stats) {
        const labels = stats.topServices.map(s => s.serviceName);
        const counts = stats.topServices.map(s => s.callCount);

        if (topChart) topChart.destroy();
        topChart = new Chart(document.getElementById('top-services-chart'), {
            type: 'bar',
            data: {
                labels,
                datasets: [{
                    label: 'Calls',
                    data: counts,
                    backgroundColor: '#4d9fff',
                    borderRadius: 6
                }]
            },
            options: {
                responsive: true,
                plugins: { legend: { display: false } },
                scales: { y: { beginAtZero: true } }
            }
        });

        const latencyLabels = stats.latencyDistribution.map(b => b.range);
        const latencyCounts = stats.latencyDistribution.map(b => b.count);

        if (latencyChart) latencyChart.destroy();
        latencyChart = new Chart(document.getElementById('latency-chart'), {
            type: 'doughnut',
            data: {
                labels: latencyLabels,
                datasets: [{
                    data: latencyCounts,
                    backgroundColor: ['#62a0ff', '#4d9fff', '#1e56a0', '#2569c4', '#0d1b2a']
                }]
            },
            options: { responsive: true }
        });
    }

    async function refresh() {
        try {
            const stats = await PortalApi.getDashboard();
            document.getElementById('stat-total').textContent = stats.totalRequests;
            document.getElementById('stat-success').textContent = `${stats.successRate}%`;
            document.getElementById('stat-failure').textContent = `${stats.failureRate}%`;
            document.getElementById('stat-avg-latency').textContent = `${stats.averageLatencyMs} ms`;
            document.getElementById('stat-p95-latency').textContent = `${stats.p95LatencyMs} ms`;
            updateCharts(stats);
        } catch (err) {
            console.error('Dashboard refresh failed', err);
        }
    }

    function init() {
        renderShell();
        refresh();
        refreshTimer = setInterval(refresh, 10000);
    }

    function destroy() {
        if (refreshTimer) clearInterval(refreshTimer);
        if (topChart) topChart.destroy();
        if (latencyChart) latencyChart.destroy();
    }

    return { init, destroy, refresh };
})();
