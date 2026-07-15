const PortalApp = (() => {
    const views = {
        dashboard: {
            title: 'Dashboard',
            subtitle: 'Gateway traffic overview and health metrics',
            init: () => PortalDashboard.init(),
            destroy: () => PortalDashboard.destroy()
        },
        hosts: {
            title: 'Allowed Hosts',
            subtitle: 'Manage allowed host entries and refresh the in-memory cache',
            init: () => PortalHosts.init()
        },
        routes: {
            title: 'Route Management',
            subtitle: 'View and update SOAP service-method → Consul/Envoy target routing',
            init: () => PortalRoutes.init()
        },
        'rest-client': {
            title: 'REST Client',
            subtitle: 'Import Bruno or Postman collections and execute requests',
            init: () => PortalRestClient.init()
        },
        profile: {
            title: 'Profile',
            subtitle: 'Signed-in user details and portal settings',
            init: () => PortalProfile.init()
        }
    };

    let currentView = null;

    function switchView(name) {
        if (currentView && views[currentView]?.destroy) {
            views[currentView].destroy();
        }

        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.view === name);
        });
        document.querySelectorAll('.view').forEach(section => {
            section.classList.toggle('active', section.id === `view-${name}`);
        });

        const meta = views[name];
        document.getElementById('page-title').textContent = meta.title;
        document.getElementById('page-subtitle').textContent = meta.subtitle;
        meta.init();
        if (name === 'profile') PortalProfile.refresh();
        currentView = name;
    }

    function init() {
        PortalTheme.init();
        PortalAuth.init();

        document.querySelectorAll('.nav-item').forEach(btn => {
            btn.addEventListener('click', () => switchView(btn.dataset.view));
        });

        switchView('dashboard');
    }

    document.addEventListener('DOMContentLoaded', init);
})();
