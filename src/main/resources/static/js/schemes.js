/**
 * FinSphere Nexus - Scheme Management Logic
 */

document.addEventListener('DOMContentLoaded', () => {
    console.log(">>>> [NEXUS_UI] Scheme Portfolio Management Module Active.");
});

/**
 * Handles explicit ledger review requests for a selected scheme block.
 * @param {string|number} planId - Unique identification structure for the target chit layout
 */
function viewLedger(planId) {
    if (!planId) {
        if (typeof notify !== 'undefined') {
            notify.error("Unable to load ledger. Invalid Scheme Reference.");
        }
        return;
    }
    
    // Smoothly redirect to your scheme specific statements path
    window.location.href = `/fsn/schemes/ledger/${planId}`;
}