const PortalApi = (() => {
    const base = '';

    function getUser() {
        const raw = sessionStorage.getItem('portalUser');
        return raw ? JSON.parse(raw) : null;
    }

    async function request(path, options = {}) {
        const headers = { ...(options.headers || {}) };
        if (options.body && !headers['Content-Type']) {
            headers['Content-Type'] = 'application/json';
        }
        const user = getUser();
        if (user?.username) {
            headers['X-Portal-User'] = user.username;
        }

        const response = await fetch(`${base}${path}`, { ...options, headers });
        if (!response.ok) {
            const text = await response.text();
            let message = text;
            try {
                const json = JSON.parse(text);
                message = json.detail || json.message || text;
            } catch (_) { /* plain text */ }
            throw new Error(message || `Request failed (${response.status})`);
        }
        if (response.status === 204) return null;
        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return response.json();
        }
        return response.text();
    }

    return {
        getUser,
        getDashboard: () => request('/api/portal/dashboard'),
        login: (username, password) => request('/api/portal/auth/login', {
            method: 'POST',
            body: JSON.stringify({ username, password })
        }),
        listHosts: () => request('/api/portal/allowed-hosts'),
        createHost: (payload) => request('/api/portal/allowed-hosts', {
            method: 'POST',
            body: JSON.stringify(payload)
        }),
        updateHost: (id, payload) => request(`/api/portal/allowed-hosts/${id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        }),
        deleteHost: (id) => request(`/api/portal/allowed-hosts/${id}`, { method: 'DELETE' }),
        refreshHosts: () => request('/api/portal/allowed-hosts/refresh', { method: 'POST' }),
        executeRequest: (payload) => request('/api/portal/rest-client/execute', {
            method: 'POST',
            body: JSON.stringify(payload)
        })
    };
})();
