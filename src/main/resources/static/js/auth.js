/**
 * FinSphere Nexus - Auth Module
 * Dependent on common.js
 */

const authRules = {
    fullName: { required: "Full name is mandatory", pattern: /^[a-zA-Z\s]+$/, patternMsg: "Letters and spaces only", max: 100 },
    phoneNumber: { required: "Phone number is mandatory", pattern: /^[6-9]\d{9}$/, patternMsg: "Invalid phone number" },
    email: { required: false, email: true, emailMsg: "Invalid email format", max: 100 },
    password: { required: "Password is mandatory", min: 8, minMsg: "Minimum 8 characters", max: 30 },
    address: { required: "Address is mandatory", max: 500 },
    careOf: { required: false, pattern: /^[a-zA-Z\s]*$/, patternMsg: "Letters and spaces only", max: 100 },
    role: { required: "Role is mandatory", pattern: /^(CUSTOMER|ADMIN)$/ },
    identifier: { required: "Phone number or email is mandatory" }
};

document.addEventListener('DOMContentLoaded', () => {
    const authForm = document.querySelector('form');
    if (!authForm) return;

    authForm.addEventListener('submit', async (e) => {
        e.preventDefault();

        // 1. Validate Fields
        const inputs = authForm.querySelectorAll('input, select, textarea');
        let isFormValid = true;
        inputs.forEach(input => {
            if (!validateAuthField(input)) isFormValid = false;
        });
        if (!isFormValid) return;

        // 2. Configuration
        const isLoginPage = window.location.pathname.includes('login');
        const endpoint = isLoginPage ? '/auth/login' : '/auth/register';
        const submitBtn = authForm.querySelector('button[type="submit"]');
        const originalBtnText = submitBtn.innerText;
        const formData = Object.fromEntries(new FormData(authForm));

        try {
            toggleLoading(submitBtn, true, originalBtnText);

            // 3. API Call
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            const result = await response.json();

            // 4. Handle Success
            if (response.ok && result.success) {
                const target = isLoginPage ? `/${result.data.role.toUpperCase()}/dashboard` : '/login';
                const msg = isLoginPage ? "Login successful! Welcome back." : (result.message || "Account created!");
                notify.successAndRedirect(msg, target);
            } 
            // 5. Handle Errors
            else {
                handleAuthErrors(response.status, result, authForm);
            }

        } catch (err) {
            notify.error("Connection error! The server is unreachable.");
        } finally {
            toggleLoading(submitBtn, false, originalBtnText);
        }
    });
});

/**
 * Validates a single input based on authRules
 */
function validateAuthField(input) {
    const rule = authRules[input.name];
    if (!rule) return true;

    const val = input.value.trim();
    
    if (rule.required && !val) return showError(input, rule.required);
    if (!rule.required && !val) return showSuccess(input);
    if (rule.min && val.length < rule.min) return showError(input, rule.minMsg);
    if (rule.max && val.length > rule.max) return showError(input, `Max ${rule.max} characters`);
    if (rule.pattern && !rule.pattern.test(val)) return showError(input, rule.patternMsg || "Invalid format");
    if (rule.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(val)) return showError(input, rule.emailMsg);

    return showSuccess(input);
}

/**
 * Maps Backend status codes to UI feedback
 * Uses commonized applyFieldErrors for dynamic mapping
 */
function handleAuthErrors(status, result, form) {
    if (status === 400 && result.errors) {
        applyFieldErrors(form, result.errors);
        notify.error("Invalid details provided.");
    } else if (status === 401 || status === 409) {
        applyFieldErrors(form, result.errors);
        notify.error(result.message || "Authentication failed.");
    } else {
        notify.error(result.message || "Unexpected error occurred.");
    }
}