const PortalRestClient = (() => {
    let collectionItems = [];
    let selectedItem = null;

    function renderShell() {
        const el = document.getElementById('view-rest-client');
        el.innerHTML = `
            <div class="toolbar">
                <label class="btn btn-secondary" style="margin:0">
                    Import Collection
                    <input type="file" id="collection-file" accept=".json" hidden>
                </label>
                <button class="btn btn-primary" id="execute-btn" type="button">Execute Request</button>
            </div>
            <div class="rest-layout">
                <div class="card">
                    <h3>Collection Requests</h3>
                    <div id="collection-list" class="collection-list">
                        <p style="color:var(--text-secondary)">Import a Bruno or Postman JSON collection to begin.</p>
                    </div>
                </div>
                <div class="card">
                    <h3>Request Builder</h3>
                    <div class="form-grid">
                        <label>Method
                            <select id="rc-method">
                                <option>GET</option><option>POST</option><option>PUT</option>
                                <option>PATCH</option><option>DELETE</option>
                            </select>
                        </label>
                        <label class="full-width">URL
                            <input type="text" id="rc-url" placeholder="http://localhost:8080/WebServices/Gateway/CBISvc">
                        </label>
                        <label class="full-width">Headers (JSON)
                            <textarea id="rc-headers" rows="3">{"Content-Type": "text/xml"}</textarea>
                        </label>
                        <label class="full-width">Body
                            <textarea id="rc-body" rows="8"><Request><serviceMethod>getCustomer</serviceMethod></Request></textarea>
                        </label>
                    </div>
                    <h3>Response</h3>
                    <pre id="rc-response" class="response-panel">No response yet.</pre>
                </div>
            </div>
        `;

        document.getElementById('collection-file').addEventListener('change', importCollection);
        document.getElementById('execute-btn').addEventListener('click', executeRequest);
    }

    function importCollection(event) {
        const file = event.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = () => {
            try {
                const json = JSON.parse(reader.result);
                collectionItems = [];
                if (json.item) {
                    flattenPostmanItems(json.item, '');
                } else if (json.requests) {
                    flattenBrunoExport(json);
                } else {
                    throw new Error('Unsupported collection format');
                }
                renderCollectionList();
            } catch (err) {
                alert(`Failed to import collection: ${err.message}`);
            }
        };
        reader.readAsText(file);
    }

    function flattenPostmanItems(items, prefix) {
        items.forEach(item => {
            const name = prefix ? `${prefix} / ${item.name}` : item.name;
            if (item.item) {
                flattenPostmanItems(item.item, name);
            } else if (item.request) {
                collectionItems.push({
                    name,
                    method: item.request.method || 'GET',
                    url: resolvePostmanUrl(item.request.url),
                    headers: toHeaderMap(item.request.header),
                    body: resolvePostmanBody(item.request.body)
                });
            }
        });
    }

    function flattenBrunoExport(json) {
        (json.requests || []).forEach(req => {
            collectionItems.push({
                name: req.name || req.filename || 'Request',
                method: req.method || 'GET',
                url: req.url || '',
                headers: req.headers || {},
                body: req.body || ''
            });
        });
    }

    function resolvePostmanUrl(urlField) {
        if (typeof urlField === 'string') return urlField;
        if (!urlField) return '';
        if (urlField.raw) return urlField.raw;
        const protocol = urlField.protocol || 'http';
        const host = Array.isArray(urlField.host) ? urlField.host.join('.') : (urlField.host || 'localhost');
        const path = Array.isArray(urlField.path) ? '/' + urlField.path.join('/') : '';
        return `${protocol}://${host}${path}`;
    }

    function resolvePostmanBody(body) {
        if (!body) return '';
        if (body.mode === 'raw') return body.raw || '';
        return '';
    }

    function toHeaderMap(headers) {
        const map = {};
        (headers || []).forEach(h => {
            if (!h.disabled) map[h.key] = h.value;
        });
        return map;
    }

    function renderCollectionList() {
        const list = document.getElementById('collection-list');
        if (!collectionItems.length) {
            list.innerHTML = '<p>No requests found in collection.</p>';
            return;
        }
        list.innerHTML = collectionItems.map((item, index) => `
            <div class="collection-item" data-index="${index}">
                <strong>${escapeHtml(item.name)}</strong><br>
                <small>${item.method} ${escapeHtml(item.url)}</small>
            </div>
        `).join('');

        list.querySelectorAll('.collection-item').forEach(node => {
            node.addEventListener('click', () => selectItem(Number(node.dataset.index)));
        });
    }

    function selectItem(index) {
        selectedItem = collectionItems[index];
        document.querySelectorAll('.collection-item').forEach((node, i) => {
            node.classList.toggle('active', i === index);
        });
        document.getElementById('rc-method').value = selectedItem.method;
        document.getElementById('rc-url').value = selectedItem.url;
        document.getElementById('rc-headers').value = JSON.stringify(selectedItem.headers || {}, null, 2);
        document.getElementById('rc-body').value = selectedItem.body || '';
    }

    async function executeRequest() {
        const responseEl = document.getElementById('rc-response');
        responseEl.textContent = 'Executing...';

        let headers = {};
        try {
            headers = JSON.parse(document.getElementById('rc-headers').value || '{}');
        } catch (err) {
            responseEl.textContent = `Invalid headers JSON: ${err.message}`;
            return;
        }

        const payload = {
            method: document.getElementById('rc-method').value,
            url: document.getElementById('rc-url').value.trim(),
            headers,
            body: document.getElementById('rc-body').value
        };

        try {
            const result = await PortalApi.executeRequest(payload);
            responseEl.textContent = [
                `Status: ${result.statusCode}`,
                `Duration: ${result.durationMs} ms`,
                `Headers: ${JSON.stringify(result.headers, null, 2)}`,
                '',
                result.body
            ].join('\n');
        } catch (err) {
            responseEl.textContent = `Error: ${err.message}`;
        }
    }

    function escapeHtml(value) {
        return String(value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;');
    }

    function init() {
        renderShell();
    }

    return { init };
})();
