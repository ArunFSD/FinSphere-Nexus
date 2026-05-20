/**
 * FinSphere Nexus - Scheme Management Logic
 */
const SCHEME_API = {
    pageSize: 10,
    url: '/fsn/schemes/active'
};

document.addEventListener('DOMContentLoaded', () => fetchSchemes(0));

async function fetchSchemes(page = 0) {
    updateTableStatus('schemeTableBody', 'Synchronizing Portfolios...');

    try {
        const response = await fetch(`${SCHEME_API.url}?page=${page}&size=${SCHEME_API.pageSize}`);
        const result = await response.json();
        
        if (result.success && result.data && result.data.content && result.data.content.length > 0) {
            renderSchemeRows(result.data.content);
            renderPagination(result.data, 'fetchSchemes');
        } else {
            updateTableStatus('schemeTableBody', 'No active records found in the database.');
            renderPagination(null, ''); 
        }
    } catch (error) {
        console.error("Fetch Error:", error);
        updateTableStatus('schemeTableBody', 'Unable to connect to FinSphere Services.', true);
        renderPagination(null, '');
    }
}

function renderSchemeRows(plans) {
    const tbody = document.getElementById('schemeTableBody');
    tbody.innerHTML = plans.map(plan => `
        <tr>
            <td class="font-bold">${plan.name}</td>
            <td class="text-accent font-bold">₹${plan.totalValue.toLocaleString('en-IN')}</td>
            <td>₹${plan.monthlyInstallment.toLocaleString('en-IN')}</td>
            <td>${plan.durationMonths} Months</td>
            <td><i class="fa-solid fa-user-group text-muted"></i> ${plan.totalMembers}</td>
            <td>${new Date(plan.startDate).toLocaleDateString('en-IN', {day:'2-digit', month:'short', year:'numeric'})}</td>
            <td><span class="badge-active">ACTIVE</span></td>
            <td>
                <button class="logout-minimal-btn" onclick="viewLedger('${plan.id}')">
                    Ledger
                </button>
            </td>
        </tr>
    `).join('');
}