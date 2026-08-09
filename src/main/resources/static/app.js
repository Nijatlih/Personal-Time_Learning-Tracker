const API_BASE = '/api/v1';

const state = {
    accessToken: localStorage.getItem('accessToken'),
    user: JSON.parse(localStorage.getItem('user') || '{}'),
    activeTab: 'dashboard'
};

// --- DOM Helpers ---
const $ = (id) => document.getElementById(id);
const show = (el) => el && el.classList.remove('hidden');
const hide = (el) => el && el.classList.add('hidden');

// --- API Client ---
async function api(path, options = {}) {
    const headers = { 
        'Content-Type': 'application/json',
        ...(state.accessToken ? { 'Authorization': `Bearer ${state.accessToken}` } : {})
    };
    try {
        const res = await fetch(`${API_BASE}${path}`, { ...options, headers });
        const json = await res.json();
        if (!res.ok) throw new Error(json.error?.message || 'Request failed');
        return json.data;
    } catch (err) {
        if (err.message.includes('401')) logout();
        throw err;
    }
}

// --- Auth Functions ---
async function login(email, password) {
    try {
        const data = await api('/auth/login', {
            method: 'POST',
            body: JSON.stringify({ email, password })
        });
        saveAuth(data, email);
        initApp();
    } catch (err) {
        showAlert('login-alert', err.message);
    }
}

async function register(email, password, workspaceName) {
    try {
        const data = await api('/auth/register', {
            method: 'POST',
            body: JSON.stringify({ email, password, workspaceName })
        });
        saveAuth(data, email);
        initApp();
    } catch (err) {
        showAlert('register-alert', err.message);
    }
}

function saveAuth(data, email) {
    state.accessToken = data.accessToken;
    state.user = { email, userId: data.userId, workspaceId: data.workspaceId };
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('user', JSON.stringify(state.user));
}

function logout() {
    localStorage.clear();
    location.reload();
}

function showAlert(id, msg) {
    const el = $(id);
    el.innerText = msg;
    show(el);
    setTimeout(() => hide(el), 5000);
}

// --- Dashboard Functions ---
async function loadDashboard() {
    try {
        const data = await api('/dashboard');
        renderDashboard(data);
        
        // Also update small focus list on dashboard
        const tasks = await api('/tasks?status=TODO');
        renderMiniTasks(tasks.slice(0, 3));
    } catch (err) {
        console.error('Dashboard load failed', err);
    }
}

function renderDashboard(data) {
    $('stat-stability').innerText = data.currentStabilityScore || '0';
    $('stat-status').innerText = data.currentStabilityStatus || 'UNKNOWN';
    $('stat-green').innerText = data.greenStreak || '0';
    $('stat-uptime').innerText = data.uptimeStreak || '0';

    const chart = $('stability-chart');
    if (data.recentRecords) {
        chart.innerHTML = data.recentRecords.map(r => `
            <div style="flex: 1; background: var(--accent-primary); height: ${r.stabilityScore}%; border-radius: 4px 4px 0 0; opacity: 0.6;" title="${r.date}: ${r.stabilityScore}"></div>
        `).join('');
    }
}

function renderMiniTasks(tasks) {
    const container = $('today-focus-list');
    if (!tasks || tasks.length === 0) {
        container.innerHTML = '<p style="color: var(--text-dim)">All done! Relax.</p>';
        return;
    }
    container.innerHTML = tasks.map(t => `
        <div class="list-item" style="padding: 0.75rem; font-size: 0.875rem">
            <div class="checkbox" onclick="completeTask('${t.id}')"></div>
            <span>${t.title}</span>
        </div>
    `).join('');
}

// --- Task Functions ---
async function loadTasks() {
    try {
        const tasks = await api('/tasks');
        const container = $('full-task-list');
        if (tasks.length === 0) {
            container.innerHTML = '<p style="color: var(--text-dim)">No tasks yet. Create one!</p>';
            return;
        }
        container.innerHTML = tasks.map(t => `
            <div class="list-item ${t.status === 'COMPLETED' ? 'completed' : ''}" style="${t.status === 'COMPLETED' ? 'opacity: 0.5' : ''}">
                <div class="checkbox ${t.status === 'COMPLETED' ? 'checked' : ''}" onclick="completeTask('${t.id}')"></div>
                <div style="flex: 1">
                    <div style="font-weight: 500">${t.title}</div>
                    <div style="font-size: 0.75rem; color: var(--text-dim)">${t.description || 'No description'}</div>
                </div>
                <div style="font-size: 0.75rem; color: var(--accent-primary)">${t.priority}</div>
            </div>
        `).join('');
    } catch (err) {
        console.error('Task load failed', err);
    }
}

async function createTask(title, description, priority) {
    try {
        await api('/tasks', {
            method: 'POST',
            body: JSON.stringify({ title, description, priority })
        });
        hide($('task-modal'));
        loadTasks();
    } catch (err) {
        alert('Failed to create task: ' + err.message);
    }
}

async function completeTask(id) {
    try {
        await api(`/tasks/${id}/complete`, { method: 'POST' });
        if (state.activeTab === 'tasks') loadTasks();
        if (state.activeTab === 'dashboard') loadDashboard();
    } catch (err) {
        alert('Failed to complete task: ' + err.message);
    }
}

// --- Routine Functions ---
async function loadRoutines() {
    try {
        const definitions = await api('/routines/definitions');
        const container = $('routine-list');
        if (definitions.length === 0) {
            container.innerHTML = '<p style="color: var(--text-dim)">No routines defined.</p>';
            return;
        }
        container.innerHTML = definitions.map(r => `
            <div class="list-item">
                <div class="checkbox" onclick="completeRoutine('${r.id}')"></div>
                <div style="flex: 1">
                    <div style="font-weight: 500">${r.name}</div>
                    <div style="font-size: 0.75rem; color: var(--text-dim)">Type: ${r.type} | Reward: +${r.stabilityImpact}</div>
                </div>
                <button class="link-btn" style="margin: 0" onclick="completeRoutine('${r.id}')">Check In</button>
            </div>
        `).join('');
    } catch (err) {
        console.error('Routine load failed', err);
    }
}

async function completeRoutine(definitionId) {
    try {
        await api('/routines/completions', {
            method: 'POST',
            body: JSON.stringify({ 
                routineDefinitionId: definitionId,
                completedAt: new Date().toISOString()
            })
        });
        alert('Routine checked in!');
        loadRoutines();
    } catch (err) {
        alert('Failed: ' + err.message);
    }
}

// --- Tab Switching ---
function switchTab(tabId) {
    state.activeTab = tabId;
    document.querySelectorAll('.nav-item').forEach(el => {
        el.classList.toggle('active', el.dataset.tab === tabId);
    });
    document.querySelectorAll('.tab-content').forEach(el => {
        el.classList.toggle('hidden', el.id !== `tab-${tabId}`);
    });
    $('view-title').innerText = tabId.charAt(0).toUpperCase() + tabId.slice(1);
    
    if (tabId === 'dashboard') loadDashboard();
    if (tabId === 'tasks') loadTasks();
    if (tabId === 'routines') loadRoutines();
}

// --- Bootstrap ---
function initApp() {
    hide($('auth-view'));
    show($('app-view'));
    $('header-user-email').innerText = state.user.email;
    switchTab('dashboard');
}

// Event Listeners
$('show-register').onclick = () => { hide($('login-card')); show($('register-card')); };
$('show-login').onclick = () => { hide($('register-card')); show($('login-card')); };

$('login-form').onsubmit = (e) => { e.preventDefault(); login($('login-email').value, $('login-password').value); };
$('register-form').onsubmit = (e) => { e.preventDefault(); register($('reg-email').value, $('reg-password').value, $('reg-workspace').value); };

$('btn-open-task-modal').onclick = () => show($('task-modal'));
$('btn-close-task-modal').onclick = () => hide($('task-modal'));
$('task-form').onsubmit = (e) => {
    e.preventDefault();
    createTask($('task-title').value, $('task-desc').value, $('task-priority').value);
};

$('logout-btn').onclick = logout;

document.querySelectorAll('.nav-item[data-tab]').forEach(el => {
    el.onclick = () => switchTab(el.dataset.tab);
});

// Start
if (state.accessToken) {
    initApp();
} else {
    show($('auth-view'));
}
