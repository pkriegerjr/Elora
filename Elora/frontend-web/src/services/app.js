/**
 * ELORA Platform - Main Application Entry Point
 * 
 * This file initializes the application, sets up global event listeners,
 * handles routing, and provides common utility functions.
 */

// Global app state
const App = {
    currentPage: null,
    isInitialized: false,
    components: {}
};

/**
 * Initialize the application
 */
async function initApp() {
    if (App.isInitialized) return;
    
    try {
        // Initialize mock data
        if (window.MockData && window.MockData.initializeMockData) {
            window.MockData.initializeMockData();
        }
        
        // Initialize auth service (restores session)
        // authService is already initialized as singleton
        
        // Initialize notification service
        // notificationService is already initialized as singleton
        
        // Set up global event listeners
        setupGlobalListeners();
        
        // Handle page-specific initialization
        handlePageInit();
        
        // Update UI based on auth state
        updateAuthUI();
        
        App.isInitialized = true;
        console.log("ELORA App inicializado com sucesso");
        
    } catch (error) {
        console.error("Erro ao inicializar app:", error);
    }
}

/**
 * Set up global event listeners
 */
function setupGlobalListeners() {
    // Handle logout buttons
    document.addEventListener("click", (e) => {
        const logoutBtn = e.target.closest("[data-action='logout']");
        if (logoutBtn) {
            e.preventDefault();
            handleLogout();
        }
    });
    
    // Handle navigation to dashboard
    document.addEventListener("click", (e) => {
        const dashboardLink = e.target.closest("[data-action='dashboard']");
        if (dashboardLink) {
            e.preventDefault();
            window.location.href = authService.getDashboardUrl();
        }
    });
    
    // Handle back buttons
    document.addEventListener("click", (e) => {
        const backBtn = e.target.closest("[data-action='back']");
        if (backBtn) {
            e.preventDefault();
            window.history.back();
        }
    });
    
    // Close modals on escape key
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape") {
            const openModal = document.querySelector(".modal.show");
            if (openModal) {
                const modal = bootstrap.Modal.getInstance(openModal);
                if (modal) modal.hide();
            }
        }
    });
    
    // Auto-dismiss alerts after 5 seconds
    document.addEventListener("DOMContentLoaded", () => {
        setTimeout(() => {
            document.querySelectorAll(".alert-dismissible:not(.persist)").forEach(alert => {
                const bsAlert = new bootstrap.Alert(alert);
                bsAlert.close();
            });
        }, 5000);
    });
}

/**
 * Handle page-specific initialization
 */
function handlePageInit() {
    const path = window.location.pathname;
    const pageName = path.split("/").pop().replace(".html", "") || "index";
    App.currentPage = pageName;
    
    // Page-specific init functions
    const pageInits = {
        "index": initHomepage,
        "login": initLoginPage,
        "cadastro-cliente": initClientRegistration,
        "cadastro-cuidador": initCaregiverRegistration,
        "buscar-cuidadores": initCaregiverSearch,
        "perfil-cuidador": initCaregiverProfile,
        "contratacao": initHiringPage,
        "contrato": initContractPage,
        "pagamento": initPaymentPage,
        "dashboard-cliente": initClientDashboard,
        "dashboard-cuidador": initCaregiverDashboard,
        "dashboard-admin": initAdminDashboard
    };
    
    if (pageInits[pageName]) {
        pageInits[pageName]();
    }
    
    // Initialize tooltips and popovers globally
    initializeBootstrapComponents();
}

/**
 * Initialize Bootstrap components (tooltips, popovers, etc.)
 */
function initializeBootstrapComponents() {
    // Tooltips
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Popovers
    const popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
}

/**
 * Update UI based on authentication state
 */
function updateAuthUI() {
    const isAuth = authService.isAuthenticated();
    const user = authService.getCurrentUser();
    
    // Update navigation
    const authButtons = document.querySelectorAll("[data-auth-buttons]");
    const userMenu = document.querySelectorAll("[data-user-menu]");
    const userNameElements = document.querySelectorAll("[data-user-name]");
    const userTypeElements = document.querySelectorAll("[data-user-type]");
    
    if (isAuth && user) {
        authButtons.forEach(el => el.classList.add("d-none"));
        userMenu.forEach(el => el.classList.remove("d-none"));
        userNameElements.forEach(el => el.textContent = user.name);
        userTypeElements.forEach(el => {
            const labels = {
                "client": "Cliente",
                "caregiver": "Cuidador",
                "admin": "Administrador"
            };
            el.textContent = labels[user.type] || user.type;
        });
    } else {
        authButtons.forEach(el => el.classList.remove("d-none"));
        userMenu.forEach(el => el.classList.add("d-none"));
    }
    
    // Update notification badge
    updateNotificationBadge();
}

/**
 * Update notification badge count
 */
async function updateNotificationBadge() {
    if (!authService.isAuthenticated()) return;
    
    try {
        const count = await notificationService.getUnreadCount();
        const badges = document.querySelectorAll("[data-notification-badge]");
        badges.forEach(badge => {
            if (count > 0) {
                badge.textContent = count > 99 ? "99+" : count;
                badge.classList.remove("d-none");
            } else {
                badge.classList.add("d-none");
            }
        });
    } catch (error) {
        console.error("Erro ao atualizar badge:", error);
    }
}

/**
 * Handle logout
 */
function handleLogout() {
    if (confirm("Tem certeza que deseja sair?")) {
        authService.logout();
        notificationService.showInfo("Você foi desconectado com sucesso");
        setTimeout(() => {
            window.location.href = authService.resolveUrl("/index.html");
        }, 1000);
    }
}

/**
 * Show loading spinner on element
 * @param {HTMLElement} element - Element to show loading on
 * @param {string} text - Loading text
 */
function showLoading(element, text = "Carregando...") {
    if (!element) return;
    
    element.dataset.originalContent = element.innerHTML;
    element.innerHTML = `
        <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
        ${text}
    `;
    element.disabled = true;
}

/**
 * Hide loading spinner on element
 * @param {HTMLElement} element - Element to hide loading on
 */
function hideLoading(element) {
    if (!element || !element.dataset.originalContent) return;
    
    element.innerHTML = element.dataset.originalContent;
    element.disabled = false;
    delete element.dataset.originalContent;
}

/**
 * Show toast notification (wrapper for notificationService)
 * @param {Object} options - Toast options
 */
function showToast(options) {
    return notificationService.showToast(options);
}

/**
 * Show success toast
 * @param {string} message - Message
 * @param {string} title - Title
 */
function showSuccess(message, title = "Sucesso") {
    notificationService.showSuccess(message, title);
}

/**
 * Show error toast
 * @param {string} message - Message
 * @param {string} title - Title
 */
function showError(message, title = "Erro") {
    notificationService.showError(message, title);
}

/**
 * Show warning toast
 * @param {string} message - Message
 * @param {string} title - Title
 */
function showWarning(message, title = "Atenção") {
    notificationService.showWarning(message, title);
}

/**
 * Show info toast
 * @param {string} message - Message
 * @param {string} title - Title
 */
function showInfo(message, title = "Informação") {
    notificationService.showInfo(message, title);
}

/**
 * Format currency (BRL)
 * @param {number} value - Value to format
 * @returns {string} Formatted currency
 */
function formatCurrency(value) {
    return new Intl.NumberFormat("pt-BR", {
        style: "currency",
        currency: "BRL"
    }).format(value);
}

/**
 * Format date (PT-BR)
 * @param {string|Date} date - Date to format
 * @param {Object} options - Format options
 * @returns {string} Formatted date
 */
function formatDate(date, options = {}) {
    const defaultOptions = {
        day: "2-digit",
        month: "2-digit",
        year: "numeric"
    };
    return new Intl.DateTimeFormat("pt-BR", { ...defaultOptions, ...options }).format(new Date(date));
}

/**
 * Format date time (PT-BR)
 * @param {string|Date} date - Date to format
 * @returns {string} Formatted date time
 */
function formatDateTime(date) {
    return formatDate(date, { hour: "2-digit", minute: "2-digit" });
}

/**
 * Mask CPF for display
 * @param {string} cpf - CPF string
 * @returns {string} Masked CPF
 */
function maskCPF(cpf) {
    if (!cpf) return "";
    const cleaned = cpf.replace(/\D/g, "");
    if (cleaned.length !== 11) return cpf;
    return `***.***.${cleaned.substring(6,9)}-${cleaned.substring(9)}`;
}

/**
 * Mask phone for display
 * @param {string} phone - Phone string
 * @returns {string} Masked phone
 */
function maskPhone(phone) {
    if (!phone) return "";
    const cleaned = phone.replace(/\D/g, "");
    if (cleaned.length < 10) return phone;
    return `(${cleaned.substring(0,2)}) *****-${cleaned.substring(cleaned.length-4)}`;
}

/**
 * Get day name from number (0 = Sunday)
 * @param {number} dayNum - Day number
 * @returns {string} Day name
 */
function getDayName(dayNum) {
    const days = ["Domingo", "Segunda", "Terça", "Quarta", "Quinta", "Sexta", "Sábado"];
    return days[dayNum] || "";
}

/**
 * Get day short name from number
 * @param {number} dayNum - Day number
 * @returns {string} Short day name
 */
function getDayShortName(dayNum) {
    const days = ["Dom", "Seg", "Ter", "Qua", "Qui", "Sex", "Sáb"];
    return days[dayNum] || "";
}

/**
 * Validate Brazilian CPF
 * @param {string} cpf - CPF string
 * @returns {boolean} Valid or not
 */
function validateCPF(cpf) {
    const cleaned = cpf.replace(/\D/g, "");
    
    if (cleaned.length !== 11) return false;
    if (/^(\d)\1{10}$/.test(cleaned)) return false; // All same digits
    
    // Validate first check digit
    let sum = 0;
    for (let i = 0; i < 9; i++) {
        sum += parseInt(cleaned[i]) * (10 - i);
    }
    let digit1 = 11 - (sum % 11);
    if (digit1 >= 10) digit1 = 0;
    if (digit1 !== parseInt(cleaned[9])) return false;
    
    // Validate second check digit
    sum = 0;
    for (let i = 0; i < 10; i++) {
        sum += parseInt(cleaned[i]) * (11 - i);
    }
    let digit2 = 11 - (sum % 11);
    if (digit2 >= 10) digit2 = 0;
    if (digit2 !== parseInt(cleaned[10])) return false;
    
    return true;
}

/**
 * Validate Brazilian CEP
 * @param {string} cep - CEP string
 * @returns {boolean} Valid or not
 */
function validateCEP(cep) {
    return CONFIG.VALIDATION.cep.test(cep);
}

/**
 * Fetch address from CEP (ViaCEP API)
 * @param {string} cep - CEP string
 * @returns {Promise<Object|null>} Address data or null
 */
async function fetchAddressFromCEP(cep) {
    const cleaned = cep.replace(/\D/g, "");
    if (!validateCEP(cleaned)) return null;
    
    try {
        const response = await fetch(`https://viacep.com.br/ws/${cleaned}/json/`);
        const data = await response.json();
        
        if (data.erro) return null;
        
        return {
            cep: data.cep,
            state: data.uf,
            city: data.localidade,
            neighborhood: data.bairro,
            street: data.logradouro,
            complemento: data.complemento
        };
    } catch (error) {
        console.error("Erro ao buscar CEP:", error);
        return null;
    }
}

/**
 * Debounce function
 * @param {Function} func - Function to debounce
 * @param {number} wait - Wait time in ms
 * @returns {Function} Debounced function
 */
function debounce(func, wait) {
    let timeout;
    return function executedFunction(...args) {
        const later = () => {
            clearTimeout(timeout);
            func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
    };
}

/**
 * Generate unique ID
 * @returns {string} Unique ID
 */
function generateId() {
    return Date.now().toString(36) + Math.random().toString(36).substring(2, 9);
}

// Page-specific initialization functions (placeholders - implemented in each page)

function initHomepage() {
    console.log("Inicializando homepage");
    // Initialize location service for search component
    if (typeof initializeLocationService === "function") {
        initializeLocationService();
    }
}

function initLoginPage() {
    console.log("Inicializando página de login");
}

function initClientRegistration() {
    console.log("Inicializando cadastro de cliente");
}

function initCaregiverRegistration() {
    console.log("Inicializando cadastro de cuidador");
}

function initCaregiverSearch() {
    console.log("Inicializando busca de cuidadores");
    if (typeof initializeLocationService === "function") {
        initializeLocationService();
    }
}

function initCaregiverProfile() {
    console.log("Inicializando perfil de cuidador");
}

function initHiringPage() {
    console.log("Inicializando página de contratação");
}

function initContractPage() {
    console.log("Inicializando página de contrato");
}

function initPaymentPage() {
    console.log("Inicializando página de pagamento");
}

function initClientDashboard() {
    console.log("Inicializando dashboard do cliente");
    // Require authentication
    if (!authService.isClient()) {
        window.location.href = authService.getLoginUrl("dashboard-cliente");
        return;
    }
}

function initCaregiverDashboard() {
    console.log("Inicializando dashboard do cuidador");
    // Require authentication
    if (!authService.isCaregiver()) {
        window.location.href = authService.getLoginUrl("dashboard-cuidador");
        return;
    }
}

function initAdminDashboard() {
    console.log("Inicializando dashboard administrativo");
    // Require authentication
    if (!authService.isAdmin()) {
        window.location.href = authService.getLoginUrl("dashboard-admin");
        return;
    }
}

// Initialize app when DOM is ready
document.addEventListener("DOMContentLoaded", initApp);

// Also initialize if already loaded
if (document.readyState !== "loading") {
    initApp();
}

// Export utilities globally
window.App = App;
window.initApp = initApp;
window.showLoading = showLoading;
window.hideLoading = hideLoading;
window.showToast = showToast;
window.showSuccess = showSuccess;
window.showError = showError;
window.showWarning = showWarning;
window.showInfo = showInfo;
window.formatCurrency = formatCurrency;
window.formatDate = formatDate;
window.formatDateTime = formatDateTime;
window.maskCPF = maskCPF;
window.maskPhone = maskPhone;
window.getDayName = getDayName;
window.getDayShortName = getDayShortName;
window.validateCPF = validateCPF;
window.validateCEP = validateCEP;
window.fetchAddressFromCEP = fetchAddressFromCEP;
window.debounce = debounce;
window.generateId = generateId;
window.updateAuthUI = updateAuthUI;
window.updateNotificationBadge = updateNotificationBadge;