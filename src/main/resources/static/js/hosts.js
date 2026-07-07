const PortalHosts = (() => {
    let hosts = [];

    function renderShell() {
        const el = document.getElementById('view-hosts');
        el.innerHTML = `
            <div class="toolbar">
                <button class="btn btn-primary" id="host-add-btn" type="button">Add Host</button>
                <button class="btn btn-secondary" id="host-refresh-btn" type="button">Refresh Cache</button>
                <button class="btn btn-ghost" id="host-reload-btn" type="button">Reload Table</button>
            </div>
            <div class="table-wrap">
                <table>
                    <thead>
                        <tr>
                            <th>Hostname</th>
                            <th>IP</th>
                            <th>Description</th>
                            <th>DMZ Server</th>
                            <th>Status</th>
                            <th>Created By</th>
                            <th>Updated By</th>
                            <th>Actions</th>
                        </tr>
                    </thead>
                    <tbody id="hosts-tbody"></tbody>
                </table>
            </div>
        `;

        document.getElementById('host-add-btn').addEventListener('click', () => {
            PortalAuth.requireLogin(() => openForm());
        });
        document.getElementById('host-refresh-btn').addEventListener('click', () => refreshCache());
        document.getElementById('host-reload-btn').addEventListener('click', () => loadHosts());
        document.getElementById('host-form').addEventListener('submit', saveHost);
        document.getElementById('host-cancel').addEventListener('click', closeForm);
        document.querySelector('#host-modal .modal-backdrop').addEventListener('click', closeForm);
    }

    function renderTable() {
        const tbody = document.getElementById('hosts-tbody');
        if (!tbody) return;
        tbody.innerHTML = hosts.map(host => `
            <tr>
                <td>${escapeHtml(host.hostname)}</td>
                <td>${escapeHtml(host.ip || '')}</td>
                <td>${escapeHtml(host.description || '')}</td>
                <td>${escapeHtml(host.dmzServer || '')}</td>
                <td><span class="status-pill ${host.status?.toLowerCase()}">${host.status}</span></td>
                <td>${escapeHtml(host.createdBy || '')}</td>
                <td>${escapeHtml(host.updatedBy || '')}</td>
                <td>
                    <button class="btn btn-ghost" data-action="edit" data-id="${host.id}">Edit</button>
                    <button class="btn btn-danger" data-action="delete" data-id="${host.id}">Delete</button>
                </td>
            </tr>
        `).join('');

        tbody.querySelectorAll('button').forEach(btn => {
            btn.addEventListener('click', () => {
                const id = Number(btn.dataset.id);
                if (btn.dataset.action === 'edit') {
                    PortalAuth.requireLogin(() => openForm(hosts.find(h => h.id === id)));
                } else {
                    PortalAuth.requireLogin(() => deleteHost(id));
                }
            });
        });
    }

    async function loadHosts() {
        hosts = await PortalApi.listHosts();
        renderTable();
    }

    async function refreshCache() {
        const result = await PortalApi.refreshHosts();
        alert(`Cache refreshed. Active hosts: ${result.activeHostCount}`);
        await loadHosts();
    }

    function openForm(host) {
        document.getElementById('host-modal-title').textContent = host ? 'Edit Allowed Host' : 'Add Allowed Host';
        document.getElementById('host-id').value = host?.id || '';
        document.getElementById('host-hostname').value = host?.hostname || '';
        document.getElementById('host-ip').value = host?.ip || '';
        document.getElementById('host-dmz').value = host?.dmzServer || '';
        document.getElementById('host-status').value = host?.status || 'ACTIVE';
        document.getElementById('host-description').value = host?.description || '';
        document.getElementById('host-form-error').classList.add('hidden');
        document.getElementById('host-modal').classList.remove('hidden');
    }

    function closeForm() {
        document.getElementById('host-modal').classList.add('hidden');
        document.getElementById('host-form').reset();
    }

    async function saveHost(event) {
        event.preventDefault();
        const errorEl = document.getElementById('host-form-error');
        const payload = {
            hostname: document.getElementById('host-hostname').value.trim(),
            ip: document.getElementById('host-ip').value.trim(),
            description: document.getElementById('host-description').value.trim(),
            dmzServer: document.getElementById('host-dmz').value.trim(),
            status: document.getElementById('host-status').value
        };
        const id = document.getElementById('host-id').value;

        try {
            if (id) {
                await PortalApi.updateHost(id, payload);
            } else {
                await PortalApi.createHost(payload);
            }
            closeForm();
            await loadHosts();
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
    }

    async function deleteHost(id) {
        if (!confirm('Delete this allowed host?')) return;
        await PortalApi.deleteHost(id);
        await loadHosts();
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;');
    }

    function init() {
        renderShell();
        loadHosts();
    }

    return { init };
})();
