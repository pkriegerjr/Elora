/**
 * ELORA Platform - Authentication Service
 * 
 * This module handles user authentication, session management, and user type routing.
 * It simulates authentication for the prototype and is prepared for future REST API integration.
 * 
 * IMPORTANT: This is a SIMULATION layer. Real authentication should be handled by the backend.
 * Never store real passwords or tokens in localStorage in production!
 */

class AuthService {
    constructor() {
        this.currentUser = null;
        this.listeners = [];
        this.init();
    }

    /**
     * Initialize auth service - restore session from localStorage
     */
    init() {
        // Check localStorage first, then sessionStorage (for "remember me" = false)
        let storedUser = localStorage.getItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
        let storedToken = localStorage.getItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
        if (!storedUser) storedUser = sessionStorage.getItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
        if (!storedToken) storedToken = sessionStorage.getItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
        
        if (storedUser && storedToken) {
            try {
                this.currentUser = JSON.parse(storedUser);
                apiService.setAuthToken(storedToken);
                this.notifyListeners("authChange", this.currentUser);
            } catch (error) {
                console.error("Erro ao restaurar sessão:", error);
                this.logout();
            }
        }
    }

    /**
     * Add auth state change listener
     * @param {Function} callback - Callback function
     */
    onAuthChange(callback) {
        this.listeners.push(callback);
    }

    /**
     * Remove auth state change listener
     * @param {Function} callback - Callback function to remove
     */
    offAuthChange(callback) {
        this.listeners = this.listeners.filter(l => l !== callback);
    }

    /**
     * Notify all listeners of auth change
     * @param {string} event - Event name
     * @param {Object} data - Event data
     */
    notifyListeners(event, data) {
        this.listeners.forEach(callback => {
            try {
                callback(event, data);
            } catch (error) {
                console.error("Erro no listener de auth:", error);
            }
        });
    }

    /**
     * Simulate login for client
     * @param {Object} credentials - Login credentials
     * @returns {Promise<Object>} User data and token
     */
    async loginCliente(credentials) {
        // Spring Boot: quando MOCK_MODE=false, chama API real
        if (!CONFIG.API.MOCK_MODE) {
            const res = await apiService.post(CONFIG.ENDPOINTS.auth.login, { identifier: credentials.identifier, password: credentials.password, userType: "CLIENT" });
            // Esperado: {accessToken, refreshToken, user}
            const userData = { ...res.user, type: CONFIG.USER_TYPES.CLIENT };
            this.setSession(userData, res.accessToken, credentials.rememberMe, res.refreshToken);
            return { user: userData, token: res.accessToken };
        }
        await this.delay(800);
        
        const { identifier, password, rememberMe } = credentials;
        
        if (!identifier || !password) {
            throw new Error("CPF/E-mail e senha são obrigatórios");
        }
        
        // Find mock user
        const mockUsers = this.getMockUsers();
        const user = mockUsers.clients.find(u => 
            (u.email === identifier || u.cpf === identifier) && u.password === password
        );
        
        if (!user) {
            throw new Error("CPF/E-mail ou senha incorretos");
        }
        
        // Create session
        const token = this.generateMockToken(user);
        const userData = this.sanitizeUser(user, CONFIG.USER_TYPES.CLIENT);
        
        this.setSession(userData, token, rememberMe);
        
        return { user: userData, token };
    }

    /**
     * Simulate login for caregiver
     * @param {Object} credentials - Login credentials
     * @returns {Promise<Object>} User data and token
     */
    async loginCuidador(credentials) {
        if (!CONFIG.API.MOCK_MODE) {
            const res = await apiService.post(CONFIG.ENDPOINTS.auth.login, { identifier: credentials.identifier, password: credentials.password, userType: "CAREGIVER" });
            const userData = { ...res.user, type: CONFIG.USER_TYPES.CAREGIVER };
            this.setSession(userData, res.accessToken, credentials.rememberMe, res.refreshToken);
            return { user: userData, token: res.accessToken };
        }
        await this.delay(800);
        
        const { identifier, password, rememberMe } = credentials;
        
        if (!identifier || !password) {
            throw new Error("CPF/E-mail e senha são obrigatórios");
        }
        
        const mockUsers = this.getMockUsers();
        const user = mockUsers.caregivers.find(u => 
            (u.email === identifier || u.cpf === identifier) && u.password === password
        );
        
        if (!user) {
            throw new Error("CPF/E-mail ou senha incorretos");
        }
        
        // Check if caregiver is approved
        if (user.status !== CONFIG.CAREGIVER_STATUS.APPROVED) {
            const statusMessages = {
                [CONFIG.CAREGIVER_STATUS.PENDING]: "Seu cadastro está pendente de análise",
                [CONFIG.CAREGIVER_STATUS.UNDER_REVIEW]: "Seu cadastro está em análise pela nossa equipe",
                [CONFIG.CAREGIVER_STATUS.REJECTED]: "Seu cadastro foi reprovado. Entre em contato com o suporte",
                [CONFIG.CAREGIVER_STATUS.NEEDS_CORRECTION]: "Seu cadastro precisa de correções. Verifique seus documentos"
            };
            throw new Error(statusMessages[user.status] || "Cadastro não aprovado");
        }
        
        const token = this.generateMockToken(user);
        const userData = this.sanitizeUser(user, CONFIG.USER_TYPES.CAREGIVER);
        
        this.setSession(userData, token, rememberMe);
        
        return { user: userData, token };
    }

    /**
     * Simulate login for administrator
     * @param {Object} credentials - Login credentials
     * @returns {Promise<Object>} User data and token
     */
    async loginAdministrador(credentials) {
        if (!CONFIG.API.MOCK_MODE) {
            const res = await apiService.post(CONFIG.ENDPOINTS.auth.login, { identifier: credentials.identifier, password: credentials.password, userType: "ADMIN" });
            const userData = { ...res.user, type: CONFIG.USER_TYPES.ADMIN };
            this.setSession(userData, res.accessToken, credentials.rememberMe, res.refreshToken);
            return { user: userData, token: res.accessToken };
        }
        await this.delay(800);
        
        const { identifier, password, rememberMe } = credentials;
        
        if (!identifier || !password) {
            throw new Error("Usuário/E-mail e senha são obrigatórios");
        }
        
        const mockUsers = this.getMockUsers();
        const user = mockUsers.admins.find(u => 
            (u.email === identifier || u.username === identifier) && u.password === password
        );
        
        if (!user) {
            throw new Error("Usuário/E-mail ou senha incorretos");
        }
        
        const token = this.generateMockToken(user);
        const userData = this.sanitizeUser(user, CONFIG.USER_TYPES.ADMIN);
        
        this.setSession(userData, token, rememberMe);
        
        return { user: userData, token };
    }

    /**
     * Logout current user
     */
    logout() {
        this.currentUser = null;
        apiService.setAuthToken(null);
        localStorage.removeItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
        localStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
        sessionStorage.removeItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
        sessionStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
        this.notifyListeners("authChange", null);
    }

    /**
     * Get current authenticated user
     * @returns {Object|null} Current user or null
     */
    getCurrentUser() {
        return this.currentUser;
    }

    /**
     * Check if user is authenticated
     * @returns {boolean} True if authenticated
     */
    isAuthenticated() {
        return this.currentUser !== null && apiService.getAuthToken() !== null;
    }

    /**
     * Check if user has specific type
     * @param {string} type - User type to check
     * @returns {boolean} True if user has type
     */
    hasUserType(type) {
        return this.isAuthenticated() && this.currentUser.type === type;
    }

    /**
     * Check if user is client
     * @returns {boolean}
     */
    isClient() {
        return this.hasUserType(CONFIG.USER_TYPES.CLIENT);
    }

    /**
     * Check if user is caregiver
     * @returns {boolean}
     */
    isCaregiver() {
        return this.hasUserType(CONFIG.USER_TYPES.CAREGIVER);
    }

    /**
     * Check if user is admin
     * @returns {boolean}
     */
    isAdmin() {
        return this.hasUserType(CONFIG.USER_TYPES.ADMIN);
    }

    /**
     * Resolve URL correctly whether running via file://, http://localhost or subfolder
     * @param {string} url - URL starting with /pages/... or /index.html
     * @returns {string} Relative URL correct for current location
     */
    resolveUrl(url) {
        const clean = url.replace(/^\//, '');
        const isInPages = window.location.pathname.includes('/pages/');
        if (isInPages) {
            if (clean.startsWith('pages/')) return clean.replace('pages/', '');
            if (clean === 'index.html') return '../index.html';
            return clean;
        }
        return clean;
    }

    /**
     * Get user's dashboard URL based on type
     * @returns {string} Dashboard URL
     */
    getDashboardUrl() {
        const map = {
            [CONFIG.USER_TYPES.CLIENT]: "/pages/dashboard-cliente.html",
            [CONFIG.USER_TYPES.CAREGIVER]: "/pages/dashboard-cuidador.html",
            [CONFIG.USER_TYPES.ADMIN]: "/pages/dashboard-admin.html"
        };
        const url = this.isAuthenticated() ? (map[this.currentUser.type] || "/pages/login.html") : "/pages/login.html";
        return this.resolveUrl(url);
    }

    /**
     * Get login URL resolved for current location
     * @param {string} redirect - optional redirect param
     * @returns {string} Login URL
     */
    getLoginUrl(redirect = '') {
        const base = this.resolveUrl("/pages/login.html");
        return redirect ? `${base}?redirect=${redirect}` : base;
    }

    /**
     * Redirect to appropriate dashboard
     */
    redirectToDashboard() {
        window.location.href = this.getDashboardUrl();
    }

    /**
     * Update current user data
     * @param {Object} userData - Updated user data
     */
    updateCurrentUser(userData) {
        this.currentUser = { ...this.currentUser, ...userData };
        localStorage.setItem(CONFIG.STORAGE_KEYS.CURRENT_USER, JSON.stringify(this.currentUser));
        this.notifyListeners("authChange", this.currentUser);
    }

    /**
     * Simulate password reset request
     * @param {string} identifier - Email or CPF
     * @param {string} userType - User type
     * @returns {Promise<void>}
     */
    async requestPasswordReset(identifier, userType) {
        if (!CONFIG.API.MOCK_MODE) {
            return await apiService.post("/auth/password/reset", { identifier, userType });
        }
        await this.delay(1000);
        
        
        const mockUsers = this.getMockUsers();
        let user;
        
        switch (userType) {
            case CONFIG.USER_TYPES.CLIENT:
                user = mockUsers.clients.find(u => u.email === identifier || u.cpf === identifier);
                break;
            case CONFIG.USER_TYPES.CAREGIVER:
                user = mockUsers.caregivers.find(u => u.email === identifier || u.cpf === identifier);
                break;
            case CONFIG.USER_TYPES.ADMIN:
                user = mockUsers.admins.find(u => u.email === identifier || u.username === identifier);
                break;
        }
        
        if (!user) {
            // Don't reveal if user exists for security
            console.log("Password reset requested for:", identifier);
            return;
        }
        
        // In production, send email with reset link
        console.log("Password reset email would be sent to:", user.email);
    }

    /**
     * Simulate user registration
     * @param {Object} userData - User registration data
     * @param {string} userType - User type
     * @returns {Promise<Object>} Created user
     */
    async register(userData, userType) {
        if (!CONFIG.API.MOCK_MODE) {
            // Spring: POST /api/clients ou /api/caregivers (com multipart para cuidador)
            const endpoint = userType === CONFIG.USER_TYPES.CAREGIVER ? CONFIG.ENDPOINTS.caregivers : CONFIG.ENDPOINTS.clients;
            return await apiService.post(endpoint, userData);
        }
        await this.delay(1000);
        
        
        // Validate required fields
        this.validateRegistration(userData, userType);
        
        // Check if user already exists
        const mockUsers = this.getMockUsers();
        const existingUser = this.findExistingUser(mockUsers, userData, userType);
        
        if (existingUser) {
            throw new Error("Usuário já cadastrado com este CPF ou e-mail");
        }
        
        // Create new user object
        const newUser = this.createUserObject(userData, userType);
        
        // In production, this would be saved to database via API
        // For mock, we add to localStorage
        this.saveMockUser(newUser, userType);
        
        return this.sanitizeUser(newUser, userType);
    }

    // Private helper methods

    /**
     * Set user session — suporta accessToken + refreshToken (Spring Security JWT)
     * @param {Object} userData - User data
     * @param {string} token - Access token
     * @param {boolean} rememberMe - Remember me flag
     * @param {string} refreshToken - Refresh token (opcional, vindo do Spring)
     */
    setSession(userData, token, rememberMe, refreshToken = null) {
        this.currentUser = userData;
        apiService.setAuthToken(token);
        if (refreshToken) apiService.setRefreshToken(refreshToken);
        
        const storeUser = rememberMe ? localStorage : sessionStorage;
        const storeOther = rememberMe ? sessionStorage : localStorage;
        // limpa o outro storage para evitar duplicidade
        storeOther.removeItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
        storeOther.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
        storeOther.removeItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);

        storeUser.setItem(CONFIG.STORAGE_KEYS.CURRENT_USER, JSON.stringify(userData));
        storeUser.setItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN, token);
        if (refreshToken) storeUser.setItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
        
        this.notifyListeners("authChange", userData);
    }

    /**
     * Generate mock JWT-like token
     * @param {Object} user - User object
     * @returns {string} Mock token
     */
    generateMockToken(user) {
        const header = btoa(JSON.stringify({ alg: "HS256", typ: "JWT" }));
        const payload = btoa(JSON.stringify({
            sub: user.id,
            type: user.type,
            iat: Date.now(),
            exp: Date.now() + (7 * 24 * 60 * 60 * 1000) // 7 days
        }));
        const signature = btoa("mock_signature_" + Math.random().toString(36).substring(7));
        return `${header}.${payload}.${signature}`;
    }

    /**
     * Sanitize user object for client storage (remove sensitive data)
     * @param {Object} user - User object
     * @param {string} type - User type
     * @returns {Object} Sanitized user
     */
    sanitizeUser(user, type) {
        const { password, ...sanitized } = user;
        return {
            ...sanitized,
            type,
            cpf: this.maskCPF(user.cpf),
            phone: this.maskPhone(user.phone)
        };
    }

    /**
     * Mask CPF for display
     * @param {string} cpf - CPF string
     * @returns {string} Masked CPF
     */
    maskCPF(cpf) {
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
    maskPhone(phone) {
        if (!phone) return "";
        const cleaned = phone.replace(/\D/g, "");
        if (cleaned.length < 10) return phone;
        return `(${cleaned.substring(0,2)}) *****-${cleaned.substring(cleaned.length-4)}`;
    }

    /**
     * Validate registration data
     * @param {Object} data - Registration data
     * @param {string} type - User type
     */
    validateRegistration(data, type) {
        const required = {
            [CONFIG.USER_TYPES.CLIENT]: ["name", "cpf", "birthDate", "phone", "email", "password"],
            [CONFIG.USER_TYPES.CAREGIVER]: ["name", "cpf", "birthDate", "phone", "email", "password", "experience"],
            [CONFIG.USER_TYPES.ADMIN]: ["name", "email", "username", "password"]
        };
        
        const fields = required[type] || [];
        const missing = fields.filter(field => !data[field]);
        
        if (missing.length > 0) {
            throw new Error(`Campos obrigatórios: ${missing.join(", ")}`);
        }
        
        // Validate email format
        if (!CONFIG.VALIDATION.email.test(data.email)) {
            throw new Error("E-mail inválido");
        }
        
        // Validate CPF format (basic)
        if (data.cpf && !CONFIG.VALIDATION.cpf.test(data.cpf)) {
            throw new Error("CPF inválido");
        }
        
        // Validate password strength
        const pwd = data.password;
        if (pwd.length < CONFIG.VALIDATION.password.minLength) {
            throw new Error(`Senha deve ter pelo menos ${CONFIG.VALIDATION.password.minLength} caracteres`);
        }
        if (CONFIG.VALIDATION.password.requireUppercase && !/[A-Z]/.test(pwd)) {
            throw new Error("Senha deve conter pelo menos uma letra maiúscula");
        }
        if (CONFIG.VALIDATION.password.requireLowercase && !/[a-z]/.test(pwd)) {
            throw new Error("Senha deve conter pelo menos uma letra minúscula");
        }
        if (CONFIG.VALIDATION.password.requireNumber && !/\d/.test(pwd)) {
            throw new Error("Senha deve conter pelo menos um número");
        }
        if (CONFIG.VALIDATION.password.requireSpecialChar && !/[!@#$%^&*]/.test(pwd)) {
            throw new Error("Senha deve conter pelo menos um caractere especial (!@#$%^&*)");
        }
    }

    /**
     * Find existing user in mock data
     * @param {Object} mockUsers - Mock users object
     * @param {Object} data - Registration data
     * @param {string} type - User type
     * @returns {Object|null} Existing user or null
     */
    findExistingUser(mockUsers, data, type) {
        let users;
        switch (type) {
            case CONFIG.USER_TYPES.CLIENT:
                users = mockUsers.clients;
                break;
            case CONFIG.USER_TYPES.CAREGIVER:
                users = mockUsers.caregivers;
                break;
            case CONFIG.USER_TYPES.ADMIN:
                users = mockUsers.admins;
                break;
            default:
                return null;
        }
        
        return users.find(u => u.cpf === data.cpf || u.email === data.email);
    }

    /**
     * Create user object from registration data
     * @param {Object} data - Registration data
     * @param {string} type - User type
     * @returns {Object} New user object
     */
    createUserObject(data, type) {
        const baseUser = {
            id: this.generateId(),
            name: data.name,
            email: data.email,
            password: data.password, // In production: hashed!
            createdAt: new Date().toISOString(),
            updatedAt: new Date().toISOString()
        };
        
        switch (type) {
            case CONFIG.USER_TYPES.CLIENT:
                return {
                    ...baseUser,
                    cpf: data.cpf,
                    birthDate: data.birthDate,
                    phone: data.phone,
                    address: data.address || null,
                    careNeeds: data.careNeeds || null
                };
            case CONFIG.USER_TYPES.CAREGIVER:
                return {
                    ...baseUser,
                    cpf: data.cpf,
                    birthDate: data.birthDate,
                    phone: data.phone,
                    address: data.address || null,
                    experience: data.experience,
                    education: data.education || "",
                    certifications: data.certifications || [],
                    specialties: data.specialties || [],
                    bio: data.bio || "",
                    availability: data.availability || {},
                    documents: data.documents || [],
                    status: CONFIG.CAREGIVER_STATUS.PENDING,
                    rating: 0,
                    reviewCount: 0
                };
            case CONFIG.USER_TYPES.ADMIN:
                return {
                    ...baseUser,
                    username: data.username,
                    role: data.role || "admin",
                    permissions: data.permissions || []
                };
            default:
                return baseUser;
        }
    }

    /**
     * Save mock user to localStorage
     * @param {Object} user - User object
     * @param {string} type - User type
     */
    saveMockUser(user, type) {
        const key = `elora_mock_${type}s`;
        const users = JSON.parse(localStorage.getItem(key) || "[]");
        users.push(user);
        localStorage.setItem(key, JSON.stringify(users));
    }

    /**
     * Get mock users from localStorage
     * @returns {Object} Mock users by type
     */
    getMockUsers() {
        return {
            clients: JSON.parse(localStorage.getItem("elora_mock_clients") || "[]"),
            caregivers: JSON.parse(localStorage.getItem("elora_mock_caregivers") || "[]"),
            admins: JSON.parse(localStorage.getItem("elora_mock_admins") || "[]")
        };
    }

    /**
     * Generate unique ID
     * @returns {string} Unique ID
     */
    generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substring(2, 9);
    }

    /**
     * Delay helper for simulating network requests
     * @param {number} ms - Milliseconds to delay
     * @returns {Promise<void>}
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Create singleton instance
const authService = new AuthService();

// Export for use in other modules
window.AuthService = AuthService;
window.authService = authService;