/**
 * FinSphere Nexus - Auth Management
 * Handles Login and Registration via Gateway (7080)
 */

document.addEventListener('DOMContentLoaded', () => {
    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    // --- LOGIN HANDLER ---
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const submitBtn = loginForm.querySelector('button[type="submit"]');
            const originalBtnText = submitBtn.innerHTML;

            // Extract Data
            const formData = {
                identifier: document.querySelector('input[name="identifier"]').value,
                password: document.querySelector('input[name="password"]').value
            };

            try {
                setLoading(submitBtn, true);

                // Note: Calling /auth/login on the Gateway (Current Port 7080)
                const response = await fetch('/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    showToast('Success! Redirecting...', 'success');
                    // Redirect to dashboard or home after successful login
                    setTimeout(() => window.location.href = '/', 1000);
                } else {
                    showToast(result.message || 'Login Failed', 'danger');
                }
            } catch (error) {
                console.error("Auth Error:", error);
                showToast('Server connection failed', 'danger');
            } finally {
                setLoading(submitBtn, false, originalBtnText);
            }
        });
    }

    // --- REGISTRATION HANDLER ---
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const submitBtn = registerForm.querySelector('button[type="submit"]');
            const originalBtnText = submitBtn.innerHTML;

            // Extract Data using the th:field names from your DTO
            const formData = {
                fullName: document.querySelector('input[name="fullName"]').value,
                phoneNumber: document.querySelector('input[name="phoneNumber"]').value,
                email: document.querySelector('input[name="email"]').value,
                password: document.querySelector('input[name="password"]').value,
                address: document.querySelector('textarea[name="address"]').value,
                careOf: document.querySelector('input[name="careOf"]').value,
                role: document.querySelector('select[name="role"]').value
            };

            try {
                setLoading(submitBtn, true);

                const response = await fetch('/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });

                const result = await response.json();

                if (response.ok && result.success) {
                    showToast('Registration Successful! Please login.', 'success');
                    setTimeout(() => window.location.href = '/login', 2000);
                } else {
                    showToast(result.message || 'Registration failed', 'danger');
                }
            } catch (error) {
                console.error("Auth Error:", error);
                showToast('Unable to reach auth service', 'danger');
            } finally {
                setLoading(submitBtn, false, originalBtnText);
            }
        });
    }
});

/**
 * Utility: UI Loading State
 */
function setLoading(button, isLoading, originalText = '') {
    if (isLoading) {
        button.disabled = true;
        button.innerHTML = `<span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span> Processing...`;
    } else {
        button.disabled = false;
        button.innerHTML = originalText;
    }
}

/**
 * Utility: Quick Notification (Bootstrap-style alerts)
 * You can replace this with a proper Toast library like SweetAlert2 or Toastify later.
 */
function showToast(message, type) {
    const alertBox = document.createElement('div');
    alertBox.className = `alert alert-${type} position-fixed top-0 start-50 translate-middle-x mt-3 shadow-lg`;
    alertBox.style.zIndex = "9999";
    alertBox.style.minWidth = "300px";
    alertBox.innerText = message;

    document.body.appendChild(alertBox);

    setTimeout(() => {
        alertBox.style.opacity = '0';
        setTimeout(() => alertBox.remove(), 500);
    }, 3000);
}