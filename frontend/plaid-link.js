// Plaid Link Integration for Budget Planner

// Initialize Plaid Link with the given link token
window.initPlaidLink = function(linkToken, onSuccessCallback, onExitCallback) {
    // Load Plaid Link script if not already loaded
    if (!window.Plaid) {
        const script = document.createElement('script');
        script.src = 'https://cdn.plaid.com/link/v2/stable/link-initialize.js';
        script.async = true;
        script.onload = () => {
            createPlaidHandler(linkToken, onSuccessCallback, onExitCallback);
        };
        document.head.appendChild(script);
    } else {
        createPlaidHandler(linkToken, onSuccessCallback, onExitCallback);
    }
};

function createPlaidHandler(linkToken, onSuccessCallback, onExitCallback) {
    const handler = Plaid.create({
        token: linkToken,
        onSuccess: function(public_token, metadata) {
            console.log('Plaid Link Success:', metadata);
            // Call the Java callback with the public token and metadata
            if (onSuccessCallback) {
                onSuccessCallback(public_token, JSON.stringify(metadata));
            }
        },
        onExit: function(err, metadata) {
            console.log('Plaid Link Exit:', err, metadata);
            if (onExitCallback) {
                if (err) {
                    onExitCallback(false, err.error_message || 'User exited');
                } else {
                    onExitCallback(false, 'User exited');
                }
            }
        },
        onEvent: function(eventName, metadata) {
            console.log('Plaid Link Event:', eventName, metadata);
        }
    });
    
    // Open the Plaid Link modal
    handler.open();
    
    // Fix z-index conflict - Plaid iframe needs to be above everything
    setTimeout(() => {
        const plaidIframe = document.querySelector('[id^="plaid-link-iframe"]');
        if (plaidIframe && plaidIframe.parentElement) {
            plaidIframe.parentElement.style.zIndex = '10000';
        }
    }, 100);
}
