const PortalAuth = (() => {
    let pendingAction = null;

    function updateBadge() {
        const user = PortalApi.getUser();
        const badge = document.getElementById('user-badge');
        const loginBtn = document.getElementById('login-btn');
        if (user?.username) {
            badge.textContent = user.displayName || user.username;
            loginBtn.textContent = 'Sign Out';
        } else {
            badge.textContent = 'Not signed in';
            loginBtn.textContent = 'AD Login';
        }
    }

    function showModal() {
        document.getElementById('login-modal').classList.remove('hidden');
        document.getElementById('login-error').classList.add('hidden');
    }

    function hideModal() {
        document.getElementById('login-modal').classList.add('hidden');
        document.getElementById('login-form').reset();
    }

    function requireLogin(action) {
        if (PortalApi.getUser()?.username) {
            return action();
        }
        pendingAction = action;
        showModal();
        return false;
    }

    async function handleLogin(event) {
        event.preventDefault();
        const username = document.getElementById('login-username').value.trim();
        const password = document.getElementById('login-password').value;
        const errorEl = document.getElementById('login-error');

        try {
            const result = await PortalApi.login(username, password);
            if (!result.authenticated) {
                errorEl.textContent = result.message || 'Authentication failed';
                errorEl.classList.remove('hidden');
                return;
            }
            sessionStorage.setItem('portalUser', JSON.stringify(result));
            hideModal();
            updateBadge();
            if (pendingAction) {
                const action = pendingAction;
                pendingAction = null;
                action();
            }
        } catch (err) {
            errorEl.textContent = err.message;
            errorEl.classList.remove('hidden');
        }
    }

    function signOut() {
        sessionStorage.removeItem('portalUser');
        updateBadge();
    }

    function init() {
        updateBadge();
        document.getElementById('login-form').addEventListener('submit', handleLogin);
        document.getElementById('login-cancel').addEventListener('click', hideModal);
        document.querySelector('#login-modal .modal-backdrop').addEventListener('click', hideModal);
        document.getElementById('login-btn').addEventListener('click', () => {
            if (PortalApi.getUser()?.username) {
                signOut();
            } else {
                showModal();
            }
        });

        if (!PortalApi.getUser()?.username) {
            setTimeout(showModal, 400);
        }
    }

    return { init, requireLogin, updateBadge };
})();
