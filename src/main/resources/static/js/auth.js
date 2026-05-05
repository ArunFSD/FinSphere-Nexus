/**
 * FinSphere Nexus - Validation Engine
 * Synchronized with RegistrationRequest.java DTO
 */

const rules = {
    fullName: {
        required: "Full name is mandatory",
        pattern: /^[a-zA-Z\s]+$/,
        patternMsg: "Full name can only contain letters and spaces",
        max: 100,
        maxMsg: "Full name must not exceed 100 characters"
    },
    phoneNumber: {
        required: "Phone number is mandatory",
        pattern: /^[6-9]\d{9}$/,
        patternMsg: "Invalid phone number"
    },
    email: {
        required: false, // Matches DTO (no @NotBlank)
        email: true,
        emailMsg: "Invalid email format",
        max: 100,
        maxMsg: "Email must not exceed 100 characters"
    },
    password: {
        required: "Password is mandatory",
        min: 8,
        minMsg: "Password must be at least 8 characters",
        max: 30,
        maxMsg: "Password must not exceed 30 characters"
    },
    address: {
        required: "Address is mandatory",
        max: 500,
        maxMsg: "Address must not exceed 500 characters"
    },
    careOf: {
        required: false, // Matches DTO
        pattern: /^[a-zA-Z\s]*$/,
        patternMsg: "Care of (C/O) can only contain letters and spaces",
        max: 100,
        maxMsg: "Care of (C/O) must not exceed 100 characters"
    },
    role: {
        required: "Role is mandatory",
        pattern: /^(CUSTOMER|ADMIN)$/,
        patternMsg: "Role must be either CUSTOMER or ADMIN"
    },
    identifier: {
        required: "Phone number or email is mandatory"
    }
};

/**
 * AUTO-CHECK FOR PENDING MESSAGES ON LOAD
 * Executes whenever any page with auth.js loads
 */
document.addEventListener('DOMContentLoaded', () => {
    // 1. Check for stored notifications
    const pendingMsg = sessionStorage.getItem('pendingSuccess');
    if (pendingMsg) {
        notify.success(pendingMsg);
        sessionStorage.removeItem('pendingSuccess');
    }

    // 2. Auth Form Handler
    const authForm = document.querySelector('form');
    if (!authForm) return;

    authForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        // Validation Logic
        const inputs = authForm.querySelectorAll('input, select, textarea');
        let isFormValid = true;
        inputs.forEach(input => {
            if (typeof validateField === "function" && !validateField(input)) {
                isFormValid = false;
            }
        });
        if (!isFormValid) return;

        // Prepare Request
        const submitBtn = authForm.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn.innerText;
        const formData = Object.fromEntries(new FormData(authForm));
        const endpoint = '/auth/register';

        try {
            if (typeof toggleLoading === "function") toggleLoading(submitBtn, true, originalBtnText);

            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            const result = await response.json();

            // Handle Success with Redirect Persistence
            if (response.status === 201 || result.success) {
                notify.successAndRedirect(
                    result.message || "Account successfully created", 
                    '/register'
                );
            }
            // Handle Validation Errors from Server
            else if (response.status === 400 && result.errors) {
                Object.keys(result.errors).forEach(fieldName => {
                    const input = authForm.querySelector(`[name="${fieldName}"]`);
                    if (typeof showError === "function") {
                        showError(input, result.errors[fieldName]);
                    }
                });
                notify.error("Invalid details! Please check and try again");
            }
            else {
                notify.error(result.message || "Registration Failed");
            }

        } catch (err) {
            notify.error("Connection error! We are unable to reach the server at this time");
        } finally {
            if (typeof toggleLoading === "function") toggleLoading(submitBtn, false, originalBtnText);
        }
    });
});

/**
 * Core Validation Engine
 */
function validateField(input) {
    const fieldName = input.name;
    const value = input.value.trim();
    const rule = rules[fieldName];

    if (!rule) return true;

    // 1. Mandatory Check (FirstOrder)
    if (rule.required && !value) {
        return showError(input, rule.required);
    }

    // 2. Skip remaining checks if optional field is empty (Email, CareOf)
    if (!rule.required && !value) {
        return showSuccess(input);
    }

    // 3. Minimum Length Check (SecondOrder)
    if (rule.min && value.length < rule.min) {
        return showError(input, rule.minMsg);
    }

    // 4. Maximum Length Check (ThirdOrder)
    if (rule.max && value.length > rule.max) {
        return showError(input, rule.maxMsg);
    }

    // 5. Pattern/Regex Check (SecondOrder)
    if (rule.pattern && !rule.pattern.test(value)) {
        return showError(input, rule.patternMsg);
    }

    // 6. Email Specific Format (SecondOrder)
    if (rule.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) {
        return showError(input, rule.emailMsg);
    }

    return showSuccess(input);
}

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
    input.classList.remove('is-invalid');
    input.classList.add('is-valid');
    const errorDiv = input.parentElement.querySelector('.error-text');
    if (errorDiv) errorDiv.innerText = '';
    return true;
}

function toggleLoading(btn, isLoading, originalText) {
    btn.disabled = isLoading;
    btn.innerHTML = isLoading ?
        '<span class="spinner-border spinner-border-sm"></span> Processing...' :
        originalText;
}

/**
 * FinSphere Nexus Notification System
 * Feature: Hover to pause, Dynamic Colors, Auto-redirect
 */
const Toast = Swal.mixin({
    toast: true,
    position: 'top-end', // Stays 'top-end' here; CSS media queries handle mobile centering
    showConfirmButton: false,
    timer: 4000,
    timerProgressBar: true,
    // INTERFACE: Pause timer on hover
    didOpen: (toast) => {
        toast.addEventListener('mouseenter', Swal.stopTimer);
        toast.addEventListener('mouseleave', Swal.resumeTimer);
    }
});

const notify = {
    /**
     * Success Notification
     */
    success: (msg) => Toast.fire({ 
        icon: 'success', 
        title: msg,
        customClass: { 
            popup: 'industrial-toast success-toast', 
            icon: 'small-icon' 
        } 
    }),

    /**
     * Error Notification
     */
    error: (msg) => Toast.fire({ 
        icon: 'error', 
        title: msg,
        customClass: { 
            popup: 'industrial-toast error-toast', 
            icon: 'small-icon' 
        } 
    }),

    /**
     * Info Notification
     */
    info: (msg) => Toast.fire({ 
        icon: 'info', 
        title: msg,
        customClass: { 
            popup: 'industrial-toast info-toast', 
            icon: 'small-icon' 
        } 
    }),

    /**
     * SUCCESS AFTER RELOAD/REDIRECT
     * Saves message to session storage before the page changes
     */
    successAndRedirect: (msg, url) => {
        sessionStorage.setItem('pendingSuccess', msg);
        window.location.href = url;
    }
};