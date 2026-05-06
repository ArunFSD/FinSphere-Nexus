/**
 * FinSphere Nexus - Common UI Utilities
 * Shared across all modules
 */

// --- NOTIFICATION SYSTEM ---
const Toast = Swal.mixin({
    toast: true,
    position: 'top-end',
    showConfirmButton: false,
    timer: 4000,
    timerProgressBar: true,
    didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer);
        toast.addEventListener('mouseleave', Swal.resumeTimer);
    }
});

const notify = {
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

// --- FORM STATE HELPERS ---
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
document.addEventListener('DOMContentLoaded', () => {
    const pendingMsg = sessionStorage.getItem('pendingSuccess');
    if (pendingMsg) {
        notify.success(pendingMsg);
        sessionStorage.removeItem('pendingSuccess');
    }
});