document.addEventListener('DOMContentLoaded', function() {

    const loginForm = document.getElementById('loginForm');
    const registerForm = document.getElementById('registerForm');

    // 1. Handle Login
    if (loginForm) {
        loginForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = Object.fromEntries(new FormData(loginForm));

            try {
                const response = await fetch('/auth/login', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });

                const result = await response.json();

                if (result.success) {
                    // Redirect based on the role returned in your Map<String, String>
                    const role = result.data.role;
                    window.location.href = (role === 'ADMIN') ? '/admin/dashboard' : '/dashboard';
                } else {
                    alert(result.message || "Login Failed");
                }
            } catch (error) {
                console.error("Login Error:", error);
                alert("Connection to Gateway failed.");
            }
        });
    }

    // 2. Handle Registration
    if (registerForm) {
        registerForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = Object.fromEntries(new FormData(registerForm));

            try {
                const response = await fetch('/auth/register', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(formData)
                });

                const result = await response.json();

                if (result.success) {
                    alert("Registration Successful! Please login.");
                    window.location.href = '/login';
                } else {
                    // Handle DomainException business errors (like phone already exists)
                    if (result.errors) {
                        let errorMsg = Object.values(result.errors).join("\n");
                        alert(errorMsg);
                    } else {
                        alert(result.message);
                    }
                }
            } catch (error) {
                alert("Registration service unavailable.");
            }
        });
    }
});