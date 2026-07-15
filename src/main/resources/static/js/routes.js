const PortalRoutes = (() => {
    let routes = [];

    function renderShell() {
        const el = document.getElementById('view-routes');
        el.innerHTML = `
            <div class="toolbar">
                <button class="btn btn-primary" id="route-add-btn" type="button">Add Route</button>
                <button class="btn btn-secondary" id="route-refresh-btn" type="button">Refresh from Store</button>
                <button class="btn btn-ghost" id="route-reload-btn" type="button">Reload Table</button>
            </div>
            <div class="card" style="margin-bottom:1rem;font-size:0.85rem;color:var(--text-secondary)">
                <strong>Route Format:</strong> <code>serviceName[/upstreamPath][:port]</code>&nbsp;&nbsp;
                Consul-DNS mode builds URL as <code>http://serviceName.service.consul:port/upstreamPath</code>.
                Set mode in <code>application.yml → forwarding.envoy.mode</code>.
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Service Method</th>
                            <th>Route Value</th>
                            <th>Resolved Host</th>
                            <th>Upstream Path</th>
                            <th>Port</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="routes-tbody"></tbody>
                </table>
            </div>
        `;

        document.getElementById('route-add-btn').addEventListener('click', () => {
            PortalAuth.requireLogin(() => openForm());
        });
        document.getElementById('route-refresh-btn').addEventListener('click', () => refreshFromStore());
        document.getElementById('route-reload-btn').addEventListener('click', () => loadRoutes());
        document.getElementById('route-form').addEventListener('submit', saveRoute);
        document.getElementById('route-cancel').addEventListener('click', closeForm);
        document.querySelector('#route-modal .modal-backdrop').addEventListener('click', closeForm);
    }

    function renderTable() {
        const tbody = document.getElementById('routes-tbody');
        if (!tbody) return;
        tbody.innerHTML = routes.map(r => `
            <tr>
                <td><code>${escapeHtml(r.serviceMethod)}</code></td>
                <td><code>${escapeHtml(r.routeValue)}</code></td>
                <td>${escapeHtml(r.resolvedHost)}</td>
                <td>${escapeHtml(r.upstreamPath || '(original path)')}</td>
                <td>${r.port > 0 ? r.port : '(default)'}</td>
                <td>
                    <button class="btn btn-ghost" data-action="edit" data-method="${escapeHtml(r.serviceMethod)}">Edit</button>
                    <button class="btn btn-danger" data-action="delete" data-method="${escapeHtml(r.serviceMethod)}">Delete</button>
                </td>
            </tr>
        `).join('');

        tbody.querySelectorAll('button').forEach(btn => {
            btn.addEventListener('click', () => {
                const method = btn.dataset.method;
                if (btn.dataset.action === 'edit') {
                    PortalAuth.requireLogin(() => {
                        const route = routes.find(r => r.serviceMethod === method);
                        if (route) openForm(route);
                    });
                } else {
                    PortalAuth.requireLogin(() => deleteRoute(method));
                }
            });
        });
    }

    async function loadRoutes() {
        routes = await PortalApi.listRoutes();
        renderTable();
    }

    async function refreshFromStore() {
        const result = await PortalApi.refreshRoutes();
        alert(`Routes refreshed from backing store. Route count: ${result.routeCount}`);
        await loadRoutes();
    }

    function openForm(route) {
        document.getElementById('route-modal-title').textContent = route ? 'Edit Route' : 'Add Route';
        document.getElementById('route-method-orig').value = route?.serviceMethod || '';
        document.getElementById('route-service-method').value = route?.serviceMethod || '';
        document.getElementById('route-value').value = route?.routeValue || '';
        document.getElementById('route-service-method').readOnly = !!route;
        document.getElementById('route-form-error').classList.add('hidden');
        document.getElementById('route-modal').classList.remove('hidden');
    }

    function closeForm() {
        document.getElementById('route-modal').classList.add('hidden');
        document.getElementById('route-form').reset();
    }

    async function saveRoute(event) {
        event.preventDefault();
        const errorEl = document.getElementById('route-form-error');
        const serviceMethod = document.getElementById('route-service-method').value.trim();
        const routeValue = document.getElementById('route-value').value.trim();

        try {
            await PortalApi.upsertRoute(serviceMethod, routeValue);
            closeForm();
            await loadRoutes();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
    }

    async function deleteRoute(serviceMethod) {
        if (!confirm(`Delete route for "${serviceMethod}"?`)) return;
        await PortalApi.deleteRoute(serviceMethod);
        await loadRoutes();
    }

    function escapeHtml(value) {
        return String(value || '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;');
    }

    function init() {
        renderShell();
        loadRoutes();
    }

    return { init };
})();
