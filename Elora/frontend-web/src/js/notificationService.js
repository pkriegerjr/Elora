/**
 * ELORA Platform - Notification Service
 * 
 * This module handles all notification-related operations including
 * retrieving, marking as read, sending, and real-time updates.
 * Prepared for future WebSocket/push notification integration.
 */

class NotificationService {
    constructor() {
        this.listeners = [];
        this.unreadCount = 0;
        this.pollingInterval = null;
        this.init();
    }

    /**
     * Initialize notification service
     */
    init() {
        // Listen for auth changes
        authService.onAuthChange((event, user) => {
            if (user) {
                this.startPolling();
                this.updateUnreadCount();
            } else {
                this.stopPolling();
                this.unreadCount = 0;
                this.notifyListeners("unreadCountChange", 0);
            }
        });
        
        // If already authenticated, start polling
        if (authService.isAuthenticated()) {
            this.startPolling();
            this.updateUnreadCount();
        }
    }

    /**
     * Add notification listener
     * @param {Function} callback - Callback function
     */
    onNotification(callback) {
        this.listeners.push(callback);
    }

    /**
     * Remove notification listener
     * @param {Function} callback - Callback function
     */
    offNotification(callback) {
        this.listeners = this.listeners.filter(l => l !== callback);
    }

    /**
     * Notify all listeners
     * @param {string} event - Event name
     * @param {*} data - Event data
     */
    notifyListeners(event, data) {
        this.listeners.forEach(callback => {
            try {
                callback(event, data);
            } catch (error) {
                console.error("Erro no listener de notificação:", error);
            }
        });
    }

    /**
     * Start polling for new notifications
     */
    startPolling() {
        if (this.pollingInterval) return;
        
        // Poll every 30 seconds
        this.pollingInterval = setInterval(() => {
            this.checkForNewNotifications();
        }, 30000);
    }

    /**
     * Stop polling
     */
    stopPolling() {
        if (this.pollingInterval) {
            clearInterval(this.pollingInterval);
            this.pollingInterval = null;
        }
    }

    /**
     * Check for new notifications (polling)
     */
    async checkForNewNotifications() {
        try {
            const notifications = await this.getNotifications({ unreadOnly: true, limit: 5 });
            if (notifications.length > 0) {
                this.notifyListeners("newNotifications", notifications);
            }
            this.updateUnreadCount();
        } catch (error) {
            console.error("Erro ao verificar notificações:", error);
        }
    }

    /**
     * Update unread count
     */
    async updateUnreadCount() {
        if (!authService.isAuthenticated()) {
            this.unreadCount = 0;
            return;
        }
        
        try {
            const notifications = await this.getNotifications({ unreadOnly: true });
            this.unreadCount = notifications.length;
            this.notifyListeners("unreadCountChange", this.unreadCount);
        } catch (error) {
            console.error("Erro ao atualizar contagem:", error);
        }
    }

    /**
     * Get notifications for current user
     * @param {Object} options - Query options
     * @returns {Promise<Array>} Notifications
     */
    async getNotifications(options = {}) {
        // In production: return await apiService.get("/notifications", options);
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const notifications = this.getMockNotifications();
        let userNotifications = notifications.filter(n => n.userId === currentUser.id);
        
        if (options.unreadOnly) {
            userNotifications = userNotifications.filter(n => !n.read);
        }
        
        if (options.type) {
            userNotifications = userNotifications.filter(n => n.type === options.type);
        }
        
        // Sort by date (newest first)
        userNotifications.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        
        if (options.limit) {
            userNotifications = userNotifications.slice(0, options.limit);
        }
        
        return userNotifications;
    }

    /**
     * Get unread notification count
     * @returns {Promise<number>} Unread count
     */
    async getUnreadCount() {
        // In production: return await apiService.get("/notifications/unread-count");
        
        await this.delay(100);
        return this.unreadCount;
    }

    /**
     * Mark notification as read
     * @param {string} notificationId - Notification ID
     * @returns {Promise<Object>} Updated notification
     */
    async markAsRead(notificationId) {
        // In production: return await apiService.patch(`/notifications/${notificationId}`, { read: true });
        
        await this.delay(200);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const notifications = this.getMockNotifications();
        const index = notifications.findIndex(n => n.id === notificationId && n.userId === currentUser.id);
        
        if (index === -1) {
            throw new Error("Notificação não encontrada");
        }
        
        notifications[index].read = true;
        notifications[index].readAt = new Date().toISOString();
        localStorage.setItem("elora_mock_notifications", JSON.stringify(notifications));
        
        this.updateUnreadCount();
        
        return notifications[index];
    }

    /**
     * Mark all notifications as read
     * @returns {Promise<number>} Count of marked notifications
     */
    async markAllAsRead() {
        // In production: return await apiService.patch("/notifications/read-all");
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const notifications = this.getMockNotifications();
        let count = 0;
        
        notifications.forEach((n, index) => {
            if (n.userId === currentUser.id && !n.read) {
                notifications[index].read = true;
                notifications[index].readAt = new Date().toISOString();
                count++;
            }
        });
        
        localStorage.setItem("elora_mock_notifications", JSON.stringify(notifications));
        this.updateUnreadCount();
        
        return count;
    }

    /**
     * Delete notification
     * @param {string} notificationId - Notification ID
     * @returns {Promise<void>}
     */
    async deleteNotification(notificationId) {
        // In production: return await apiService.delete(`/notifications/${notificationId}`);
        
        await this.delay(200);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const notifications = this.getMockNotifications();
        const index = notifications.findIndex(n => n.id === notificationId && n.userId === currentUser.id);
        
        if (index === -1) {
            throw new Error("Notificação não encontrada");
        }
        
        notifications.splice(index, 1);
        localStorage.setItem("elora_mock_notifications", JSON.stringify(notifications));
        
        this.updateUnreadCount();
    }

    /**
     * Send notification (admin/system)
     * @param {Object} notification - Notification data
     * @returns {Promise<Object>} Created notification
     */
    async sendNotification(notification) {
        // In production: return await apiService.post("/notifications", notification);
        // This would be called by backend services
        
        await this.delay(300);
        
        // Validate required fields
        if (!notification.userId || !notification.title || !notification.message) {
            throw new Error("Campos obrigatórios: userId, title, message");
        }
        
        const newNotification = {
            id: this.generateId(),
            ...notification,
            read: false,
            createdAt: new Date().toISOString()
        };
        
        const notifications = this.getMockNotifications();
        notifications.push(newNotification);
        localStorage.setItem("elora_mock_notifications", JSON.stringify(notifications));
        
        // If it's for current user, notify listeners
        const currentUser = authService.getCurrentUser();
        if (currentUser && currentUser.id === notification.userId) {
            this.notifyListeners("newNotifications", [newNotification]);
            this.updateUnreadCount();
        }
        
        return newNotification;
    }

    /**
     * Send notification to multiple users
     * @param {Array<string>} userIds - Array of user IDs
     * @param {Object} notification - Notification data (without userId)
     * @returns {Promise<Array>} Created notifications
     */
    async sendBulkNotification(userIds, notification) {
        // In production: return await apiService.post("/notifications/bulk", { userIds, notification });
        
        const results = [];
        for (const userId of userIds) {
            const result = await this.sendNotification({ ...notification, userId });
            results.push(result);
        }
        return results;
    }

    /**
     * Get notification preferences
     * @returns {Promise<Object>} User preferences
     */
    async getPreferences() {
        // In production: return await apiService.get("/notifications/preferences");
        
        await this.delay(200);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const prefs = JSON.parse(localStorage.getItem(`${CONFIG.STORAGE_KEYS.USER_PREFERENCES}_${currentUser.id}`) || "{}");
        
        return {
            email: prefs.email !== false,
            push: prefs.push !== false,
            inApp: prefs.inApp !== false,
            types: prefs.types || {
                contract: true,
                payment: true,
                caregiver: true,
                review: true,
                system: true
            }
        };
    }

    /**
     * Update notification preferences
     * @param {Object} preferences - New preferences
     * @returns {Promise<Object>} Updated preferences
     */
    async updatePreferences(preferences) {
        // In production: return await apiService.patch("/notifications/preferences", preferences);
        
        await this.delay(300);
        
        const currentUser = authService.getCurrentUser();
        if (!currentUser) {
            throw new Error("Usuário não autenticado");
        }
        
        const key = `${CONFIG.STORAGE_KEYS.USER_PREFERENCES}_${currentUser.id}`;
        const currentPrefs = JSON.parse(localStorage.getItem(key) || "{}");
        const newPrefs = { ...currentPrefs, ...preferences };
        localStorage.setItem(key, JSON.stringify(newPrefs));
        
        return newPrefs;
    }

    /**
     * Show toast notification (UI helper)
     * @param {Object} options - Toast options
     */
    showToast(options) {
        const {
            title = "",
            message = "",
            type = "info", // success, error, warning, info
            duration = 5000,
            action = null
        } = options;
        
        // Create toast container if not exists
        let container = document.getElementById("toast-container");
        if (!container) {
            container = document.createElement("div");
            container.id = "toast-container";
            container.className = "toast-container position-fixed bottom-0 end-0 p-3";
            container.style.zIndex = "1080";
            document.body.appendChild(container);
        }
        
        // Create toast element
        const toastId = "toast-" + Date.now();
        const icons = {
            success: "bi-check-circle-fill text-success",
            error: "bi-x-circle-fill text-danger",
            warning: "bi-exclamation-triangle-fill text-warning",
            info: "bi-info-circle-fill text-primary"
        };
        
        const toastHtml = `
            <div id="${toastId}" class="toast align-items-center text-white bg-${type === "error" ? "danger" : type} border-0" role="alert" aria-live="assertive" aria-atomic="true">
                <div class="d-flex">
                    <div class="toast-body d-flex align-items-center">
                        <i class="bi ${icons[type] || icons.info} me-2" style="font-size: 1.25rem;"></i>
                        <div>
                            ${title ? `<strong>${title}</strong><br>` : ""}
                            ${message}
                        </div>
                    </div>
                    <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Fechar"></button>
                </div>
            </div>
        `;
        
        container.insertAdjacentHTML("beforeend", toastHtml);
        
        const toastElement = document.getElementById(toastId);
        const toast = new bootstrap.Toast(toastElement, { delay: duration });
        toast.show();
        
        // Remove from DOM after hidden
        toastElement.addEventListener("hidden.bs.toast", () => {
            toastElement.remove();
        });
        
        // Handle action click
        if (action) {
            toastElement.addEventListener("click", (e) => {
                if (!e.target.closest(".btn-close")) {
                    action.callback();
                    toast.hide();
                }
            });
        }
        
        return toast;
    }

    /**
     * Show success toast
     * @param {string} message - Message
     * @param {string} title - Title
     */
    showSuccess(message, title = "Sucesso") {
        this.showToast({ title, message, type: "success" });
    }

    /**
     * Show error toast
     * @param {string} message - Message
     * @param {string} title - Title
     */
    showError(message, title = "Erro") {
        this.showToast({ title, message, type: "error" });
    }

    /**
     * Show warning toast
     * @param {string} message - Message
     * @param {string} title - Title
     */
    showWarning(message, title = "Atenção") {
        this.showToast({ title, message, type: "warning" });
    }

    /**
     * Show info toast
     * @param {string} message - Message
     * @param {string} title - Title
     */
    showInfo(message, title = "Informação") {
        this.showToast({ title, message, type: "info" });
    }

    /**
     * Get mock notifications from localStorage
     * @returns {Array} Array of notifications
     */
    getMockNotifications() {
        return JSON.parse(localStorage.getItem("elora_mock_notifications") || "[]");
    }

    /**
     * Generate unique ID
     * @returns {string} Unique ID
     */
    generateId() {
        return Date.now().toString(36) + Math.random().toString(36).substring(2, 9);
    }

    /**
     * Delay helper
     * @param {number} ms - Milliseconds
     * @returns {Promise<void>}
     */
    delay(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }
}

// Create singleton instance
const notificationService = new NotificationService();

// Export for use in other modules
window.NotificationService = NotificationService;
window.notificationService = notificationService;