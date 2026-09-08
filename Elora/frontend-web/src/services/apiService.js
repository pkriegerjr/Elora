/**
 * ELORA Platform - API Service Layer (Spring Boot Ready)
 * - Suporta /api relativo (Spring serve static/ + API no mesmo host)
 * - JWT Bearer + refresh token + tratamento de 401 com refresh automático
 * - Quando CONFIG.API.MOCK_MODE=true, mantém compatibilidade com mockData
 */

class ApiService {
    constructor() {
        this.baseURL = CONFIG.API.baseURL; // "/api" em produção
        this.defaultHeaders = { ...CONFIG.API.headers };
        this.requestInterceptor = null;
        this.responseInterceptor = null;
        this.errorInterceptor = null;
        this.isRefreshing = false;
        this.failedQueue = [];
    }

    setRequestInterceptor(fn){ this.requestInterceptor=fn; }
    setResponseInterceptor(fn){ this.responseInterceptor=fn; }
    setErrorInterceptor(fn){ this.errorInterceptor=fn; }

    getAuthHeaders(){
        const token=this.getAuthToken();
        return token ? { [CONFIG.API.auth.headerName]: `${CONFIG.API.auth.tokenPrefix}${token}` } : {};
    }

    getAuthToken(){
        return localStorage.getItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN) || sessionStorage.getItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
    }

    getRefreshToken(){
        return localStorage.getItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN) || sessionStorage.getItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);
    }

    setAuthToken(token){
        if(token){
            localStorage.setItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN, token);
        } else {
            localStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            sessionStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
        }
    }

    setRefreshToken(token){
        if(token){
            localStorage.setItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN, token);
        } else {
            localStorage.removeItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);
            sessionStorage.removeItem(CONFIG.STORAGE_KEYS.REFRESH_TOKEN);
        }
    }

    // Suporta mock mode: se MOCK_MODE e endpoint é mock, não faz fetch real
    shouldMock(){ return CONFIG.API.MOCK_MODE === true; }

    buildUrl(endpoint){
        // Evita //api//auth/login
        return `${this.baseURL}${endpoint.startsWith('/') ? endpoint : '/' + endpoint}`;
    }

    buildOptions(options={}){
        const config={
            ...options,
            headers: { ...this.defaultHeaders, ...this.getAuthHeaders(), ...options.headers },
            // Necessário para Spring com cookies SameSite/JWT refresh via httpOnly se adotado
            credentials: "include"
        };
        // Spring CSRF: se usar CookieCsrfTokenRepository, enviar X-XSRF-TOKEN
        const csrf = this.getCsrfToken();
        if(csrf) config.headers["X-XSRF-TOKEN"]=csrf;
        return this.requestInterceptor ? this.requestInterceptor(config) : config;
    }

    getCsrfToken(){
        const m=document.cookie.match(/XSRF-TOKEN=([^;]+)/);
        return m ? decodeURIComponent(m[1]) : null;
    }

    async processResponse(response){
        let data;
        const ct=response.headers.get("content-type");
        if(ct && ct.includes("application/json")){
            const text=await response.text();
            data=text ? JSON.parse(text) : {};
        } else if(ct && ct.includes("application/pdf")){
            return response; // para download
        } else {
            data=await response.text();
        }
        if(this.responseInterceptor) data=await this.responseInterceptor(data, response);
        if(!response.ok){
            // Spring Boot padrão: {timestamp,status,error,path,message} ou {message, errors[]}
            const err=new Error((data && (data.message || data.error)) || `HTTP ${response.status}`);
            err.status=response.status;
            err.data=data;
            if(this.errorInterceptor) await this.errorInterceptor(err, response);
            throw err;
        }
        // Spring Page: {content, totalElements, totalPages, ...} → retorna direto
        return data;
    }

    async tryRefreshToken(){
        if(this.isRefreshing){
            return new Promise((resolve,reject)=>{
                this.failedQueue.push({resolve,reject});
            });
        }
        this.isRefreshing=true;
        const refreshToken=this.getRefreshToken();
        if(!refreshToken){
            this.isRefreshing=false;
            throw new Error("Sessão expirada");
        }
        try{
            const res=await fetch(this.buildUrl(CONFIG.API.auth.refreshEndpoint),{
                method:"POST",
                headers:{ "Content-Type":"application/json" },
                body: JSON.stringify({ refreshToken }),
                credentials:"include"
            });
            const data=await res.json();
            if(!res.ok) throw new Error(data.message||"Falha ao renovar token");
            this.setAuthToken(data.accessToken);
            if(data.refreshToken) this.setRefreshToken(data.refreshToken);
            this.failedQueue.forEach(p=>p.resolve(data.accessToken));
            this.failedQueue=[];
            return data.accessToken;
        } catch(e){
            this.failedQueue.forEach(p=>p.reject(e));
            this.failedQueue=[];
            // limpa sessão e redireciona
            localStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
            sessionStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            sessionStorage.removeItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
            if(!window.location.pathname.includes("login.html")){
                window.location.href= window.authService ? window.authService.getLoginUrl()+"?expired=1" : "login.html?expired=1";
            }
            throw e;
        } finally { this.isRefreshing=false; }
    }

    async request(endpoint, options={}){
        // Em mock mode, serviços decidem se usam mock; aqui apenas faz fetch real quando chamado
        const url=this.buildUrl(endpoint);
        const config=this.buildOptions(options);
        try{
            const response=await fetch(url, config);
            if(response.status===401 && this.getRefreshToken()){
                try{
                    const newToken=await this.tryRefreshToken();
                    config.headers[CONFIG.API.auth.headerName]=`${CONFIG.API.auth.tokenPrefix}${newToken}`;
                    const retry=await fetch(url, config);
                    return await this.processResponse(retry);
                } catch(_){ throw await this.processResponse(response); }
            }
            return await this.processResponse(response);
        } catch(error){
            if(error.name==="TypeError" && String(error.message).includes("fetch")){
                // Se MOCK_MODE e backend offline, não estoura erro em demo
                if(this.shouldMock()){
                    console.warn("[ELORA] Backend offline e MOCK_MODE=true — usando dados mock. Erro:", error.message);
                    throw error;
                }
                throw new Error("Erro de conexão com a API Spring Boot. Verifique se o backend está em "+this.baseURL);
            }
            throw error;
        }
    }

    async get(endpoint, params={}){ const qs=new URLSearchParams(params).toString(); return this.request(qs?`${endpoint}?${qs}`:endpoint,{method:"GET"}); }
    async post(endpoint, data={}){ return this.request(endpoint,{method:"POST",body:JSON.stringify(data)}); }
    async put(endpoint, data={}){ return this.request(endpoint,{method:"PUT",body:JSON.stringify(data)}); }
    async patch(endpoint, data={}){ return this.request(endpoint,{method:"PATCH",body:JSON.stringify(data)}); }
    async delete(endpoint){ return this.request(endpoint,{method:"DELETE"}); }

    async upload(endpoint, formData){
        // Spring espera MultipartFile com @RequestPart; não setar Content-Type
        return this.request(endpoint,{method:"POST",headers:{...this.getAuthHeaders()},body:formData});
    }

    async download(endpoint, params={}){
        const qs=new URLSearchParams(params).toString();
        const url=this.buildUrl(qs?`${endpoint}?${qs}`:endpoint);
        const res=await fetch(url,{method:"GET",headers:{...this.getAuthHeaders()},credentials:"include"});
        if(!res.ok) throw new Error(`Download failed: ${res.status}`);
        return res.blob();
    }
}

const apiService=new ApiService();

apiService.setErrorInterceptor(async (error)=>{
    if(error.status===401){
        // tenta refresh já tratado em request(); se ainda 401, limpa e redireciona
        if(!apiService.getRefreshToken()){
            localStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            localStorage.removeItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
            sessionStorage.removeItem(CONFIG.STORAGE_KEYS.AUTH_TOKEN);
            sessionStorage.removeItem(CONFIG.STORAGE_KEYS.CURRENT_USER);
            if(!window.location.pathname.includes("login.html")){
                window.location.href= window.authService ? window.authService.getLoginUrl()+"?expired=1" : "login.html?expired=1";
            }
        }
    }
    if(error.status===403) console.warn("Acesso negado:", error.message);
    if(error.status>=500) console.error("Erro servidor Spring:", error.message, error.data);
});

window.ApiService=ApiService;
window.apiService=apiService;
