/**
 * FinSphere Nexus - Common UI Utilities
 * Shared across all modules
 **/

// --- NOTIFICATION SYSTEM ---
// Use 'var' or a window-check to allow the script to reload safely
// Prevents "Identifier already declared" error on reloads
if (typeof Toast === 'undefined') {
    var Toast = Swal.mixin({
        toast: true,
        position: 'top-end',
        showConfirmButton: false,
        timer: 3000,
        timerProgressBar: true,
        didOpen: (toast) => {
            toast.addEventListener('mouseenter', Swal.stopTimer);
            toast.addEventListener('mouseleave', Swal.resumeTimer);
        }
    });
}

if (typeof notify === 'undefined') {
    var notify = {
        success: (msg) => Toast.fire({
            icon: 'success',
            title: msg,
            customClass: { popup: 'industrial-toast success-toast', icon: 'small-icon' }
        }),
        error: (msg) => Toast.fire({
            icon: 'error',
            title: msg,
            customClass: { popup: 'industrial-toast error-toast', icon: 'small-icon' }
        }),
        info: (msg) => Toast.fire({
            icon: 'info',
            title: msg,
            customClass: { popup: 'industrial-toast info-toast', icon: 'small-icon' }
        }),
        successAndRedirect: (msg, url) => {
            sessionStorage.setItem('pendingSuccess', msg);
            window.location.href = url;
        }
    };
}

// --- FORM STATE HELPERS ---
// Function declarations are "hoisted" and don't cause redeclaration errors
function showError(input, message) {
    if (!input) return false;
    input.classList.remove('is-valid');
    input.classList.add('is-invalid');

    let errorDiv = input.parentElement.querySelector('.error-text');
    if (!errorDiv) {
        errorDiv = document.createElement('div');
        errorDiv.className = 'error-text';
        input.parentElement.appendChild(errorDiv);
    }
    errorDiv.innerText = message;
    return false;
}

function showSuccess(input) {
    if (!input) return true;
    input.classList.remove('is-invalid');
    input.classList.add('is-valid');
    const errorDiv = input.parentElement.querySelector('.error-text');
    if (errorDiv) errorDiv.innerText = '';
    return true;
}

function toggleLoading(btn, isLoading, originalText) {
    if (!btn) return;
    btn.disabled = isLoading;
    btn.innerHTML = isLoading ?
        '<span class="spinner-border spinner-border-sm"></span> Processing...' :
        originalText;
}

/**
 * Common Error Parser
 * Dynamically maps backend error maps to frontend inputs
 */
function applyFieldErrors(form, errors) {
    if (!errors) return;
    Object.keys(errors).forEach(key => {
        const input = form.querySelector(`[name="${key}"]`);
        if (input) showError(input, errors[key]);
    });
}

// --- PERSISTENCE CHECKER ---
// Check to ensure we don't attach multiple listeners if script reloads
if (!window.nexusPersistenceLoaded) {
    document.addEventListener('DOMContentLoaded', () => {
        const pendingMsg = sessionStorage.getItem('pendingSuccess');
        if (pendingMsg) {
            notify.success(pendingMsg);
            sessionStorage.removeItem('pendingSuccess');
        }
    });
    window.nexusPersistenceLoaded = true;
}

/**
 * FinSphere Nexus - Global Logout Handler
 */
async function handleLogout() {
    const endpoint = '/auth/logout';

    try {
        const response = await fetch(endpoint, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' }
        });

        // Use text() first to avoid errors if the response is not JSON
        const text = await response.text();
        let result;
        try {
            result = JSON.parse(text);
        } catch (e) {
            result = { success: response.ok };
        }

        if (response.ok && result.success) {
            console.log(">>>> [LOGOUT_SUCCESS] Session cleared");
            const msg = result.message || "Logout successful";
            const target = '/login';

            notify.successAndRedirect(msg, target);
        } else {
            console.error("<<<< [LOGOUT_ERROR]", result.message);
            window.location.href = '/login';
        }
    } catch (error) {
        console.error("!!!! [SYSTEM_ERROR] Logout failed:", error);
        window.location.href = '/login';
    }
}

// Global Back Navigation
function handleBack() {
    if (window.history.length > 1) {
        window.history.back();
    } else {
        window.location.href = '/dashboard';
    }
}

/**
 * Renders a standard FinSphere Pagination Bar
 * @param {Object} data - The Page object from Spring Backend
 * @param {Function} callback - The fetch function to call on page change
 */
function renderPagination(data, callbackName) {
    const nav = document.getElementById('paginationNav');
    const info = document.getElementById('pageInfo');
    const footer = document.querySelector('.pagination-box');
    
    // 1. If data is null or empty, hide the entire footer section
    if (!data || !data.content || data.totalElements === 0) {
        if (footer) footer.classList.add('hidden');
        return;
    }

    // 2. Otherwise, make the footer visible and populate it
    if (footer) footer.classList.remove('hidden');

    info.innerText = `Total Records: ${data.totalElements} | Page ${data.number + 1} of ${data.totalPages}`;
    
    nav.innerHTML = `
        <button class="page-link" ${data.first ? 'disabled' : ''} onclick="${callbackName}(${data.number - 1})">
            <i class="fa-solid fa-chevron-left"></i> Previous
        </button>
        <span class="page-indicator">${data.number + 1}</span>
        <button class="page-link" ${data.last ? 'disabled' : ''} onclick="${callbackName}(${data.number + 1})">
            Next <i class="fa-solid fa-chevron-right"></i>
        </button>
    `;
}

/**
 * Shows a GLOWING message inside a table body
 */
function updateTableStatus(tbodyId, message, isError = false) {
    const icon = isError ? 'fa-circle-exclamation' : 'fa-box-open';
    
    // Custom style for error state (red glow)
    const errorStyle = isError ? 'style="background: linear-gradient(135deg, #ff4757, #ff6b81); -webkit-background-clip: text; -webkit-text-fill-color: transparent; filter: drop-shadow(0 0 10px rgba(255, 71, 87, 0.4));"' : '';

    const tbody = document.getElementById(tbodyId);
    if (!tbody) return;

    tbody.innerHTML = `
        <tr>
            <td colspan="15">
                <div class="empty-state-wrapper">
                    <i class="fa-solid ${icon} empty-icon-glow" ${errorStyle}></i>
                    <div class="empty-text">${message}</div>
                </div>
            </td>
        </tr>`;
}