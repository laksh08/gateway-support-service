const PortalProfile = (() => {
    function renderShell() {
        const el = document.getElementById('view-profile');
        el.innerHTML = `
            <div class="profile-grid">
                <div class="card">
                    <h3>User Profile</h3>
                    <p><strong>Username:</strong> <span id="profile-username">-</span></p>
                    <p><strong>Display Name:</strong> <span id="profile-display">-</span></p>
                    <p><strong>Auth Status:</strong> <span id="profile-status">Not authenticated</span></p>
                </div>
                <div class="card">
                    <h3>Portal Settings</h3>
                    <p><strong>Theme:</strong> <span id="profile-theme">light</span></p>
                    <p><strong>Session:</strong> Browser session storage</p>
                    <p><strong>Audit:</strong> Username sent via X-Portal-User header</p>
                </div>
                <div class="card">
                    <h3>Gateway Info</h3>
                    <p><strong>Service:</strong> Gateway Support Service</p>
                    <p><strong>Role:</strong> Smart Router (not API Gateway)</p>
                    <p><strong>Endpoint:</strong> POST /WebServices/Gateway/CBISvc</p>
                </div>
            </div>
        `;
    }

    function refresh() {
        const user = PortalApi.getUser();
        document.getElementById('profile-username').textContent = user?.username || '-';
        document.getElementById('profile-display').textContent = user?.displayName || '-';
        document.getElementById('profile-status').textContent = user?.authenticated ? 'Authenticated' : 'Not authenticated';
        document.getElementById('profile-theme').textContent = document.documentElement.getAttribute('data-theme');
    }

    function init() {
        renderShell();
        refresh();
    }

    return { init, refresh };
})();
