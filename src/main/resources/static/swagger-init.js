window.onload = function() {
    if (window.ui) {
        // This must match the name in your filter
        window.ui.preauthorizeApiKey("x-api-key", "secret123");
        console.log("API key preauthorized");
    } else {
        console.log("Swagger UI object not yet ready");
    }
};
