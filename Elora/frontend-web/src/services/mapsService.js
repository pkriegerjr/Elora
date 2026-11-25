/**
 * ELORA Platform - Maps Service
 *
 * Usa Leaflet + OpenStreetMap (gratuitos, sem API key paga) para exibir o mapa,
 * marcadores de cuidadores e a localização do usuario (via CONFIG.LOCATION_IP).
 * Fallback: mapa simulado se a biblioteca/CDN nao carregar.
 */

class MapsService {
    constructor() {
        this.map = null;
        this.markers = [];
        this.infoWindows = [];
        this.userLocation = null;
        this.searchRadius = CONFIG.MAP_SEARCH_RADIUS.default;
        this.isInitialized = false;
        this.loadPromise = null;
    }

    /**
     * Carrega Leaflet + CSS uma vez (CDN, sem chave).
     * @returns {Promise<void>}
     */
    async loadMapLibrary(){
        if (typeof L !== "undefined") return;

        if (!document.querySelector('link[data-elora-leaflet-css]')) {
            const css = document.createElement('link');
            css.dataset.eloraLeafletCss = "true";
            css.rel = "stylesheet";
            css.href = CONFIG.MAPS.leafletCss;
            document.head.appendChild(css);
        }

        if (!document.querySelector('script[data-elora-leaflet-js]') && !this.loadPromise) {
            this.loadPromise = new Promise((resolve, reject) => {
                const js = document.createElement('script');
                js.dataset.eloraLeafletJs = "true";
                js.src = CONFIG.MAPS.leafletJs;
                js.async = true;
                js.onload = resolve;
                js.onerror = reject;
                document.head.appendChild(js);
            });
        }

        if (this.loadPromise) {
            try { await this.loadPromise; } finally { this.loadPromise = null; }
        }
    }

    // Alias mantido para compatibilidade com chamadas antigas.
    async loadGoogleMapsScript(){ return this.loadMapLibrary(); }

    async initializeMap(options = {}) {
        // Mapa real (Leaflet/OpenStreetMap) — sem API paga.
        try{ await this.loadMapLibrary(); } catch(e){ console.warn("Falha ao carregar Leaflet:", e.message); }

        if (this.isInitialized && this.map) {
            return this.map;
        }

        if (typeof L === "undefined") {
            console.warn("Leaflet indisponível. Usando mapa simulado.");
            return this.initializeMockMap(options);
        }

        try {
            const container = document.getElementById(options.containerId || "map");
            if (!container) {
                throw new Error("Container do mapa não encontrado");
            }

            const center = options.center || CONFIG.MAPS.defaultCenter;
            const zoom = options.zoom || CONFIG.MAPS.defaultZoom;

            this.map = L.map(container, {
                center: [center.lat, center.lng],
                zoom,
                scrollWheelZoom: false
            });

            const tiles = L.tileLayer(CONFIG.MAPS.tileUrl, {
                maxZoom: 19,
                attribution: CONFIG.MAPS.attribution
            });

            // Diagnóstico: avisa no console quando um tile do OpenStreetMap falha
            // (rede, extensão ou política do provedor). Não altera o comportamento.
            let tileErrors = 0;
            tiles.on("tileerror", (event) => {
                tileErrors += 1;
                const failedUrl = (event && event.tile && event.tile.src) || "(URL desconhecida)";
                if (tileErrors <= 10) {
                    console.warn(`[Elora] Falha ao carregar tile do mapa (${tileErrors}):`, failedUrl);
                } else if (tileErrors === 11) {
                    console.warn("[Elora] Mais falhas de tile — silenciando novos avisos.");
                }
                if (tileErrors === 5) {
                    console.warn(
                        "[Elora] Vários tiles falharam. Verifique bloqueio de rede/extensão para",
                        CONFIG.MAPS.tileUrl
                    );
                }
            });

            tiles.addTo(this.map);

            // Add user location marker if available
            if (this.userLocation) {
                this.addUserLocationMarker();
            }

            this.isInitialized = true;
            return this.map;

        } catch (error) {
            console.error("Erro ao inicializar mapa:", error);
            return this.initializeMockMap(options);
        }
    }

    /**
     * Initialize mock map for prototype
     * @param {Object} options - Map options
     * @returns {Object} Mock map instance
     */
    initializeMockMap(options = {}) {
        const container = document.getElementById(options.containerId || "map");
        if (container) {
            container.innerHTML = `
                <div class="mock-map-container d-flex align-items-center justify-content-center h-100 bg-light rounded">
                    <div class="text-center p-4">
                        <i class="bi bi-geo-alt text-primary" style="font-size: 3rem;"></i>
                        <h5 class="mt-3 text-muted">Mapa Interativo</h5>
                        <p class="text-muted small">Mapa com OpenStreetMap (Leaflet)</p>
                        <p class="text-muted small">Verifique a conexão com o CDN/tiles do mapa</p>
                        <div class="mt-3">
                            <button class="btn btn-outline-primary btn-sm" onclick="mapsService.getUserLocation()">
                                <i class="bi bi-geo-fill me-1"></i> Minha Localização
                            </button>
                        </div>
                    </div>
                </div>
            `;
        }

        this.isInitialized = true;
        
        // Return mock map object with basic methods
        this.map = {
            setCenter: (latLng) => console.log("Mock: setCenter", latLng),
            setZoom: (zoom) => console.log("Mock: setZoom", zoom),
            addListener: () => {},
            getCenter: () => ({ lat: () => -23.5505, lng: () => -46.6333 }),
            getZoom: () => 12,
            fitBounds: () => {}
        };

        return this.map;
    }

    /**
     * Get custom map styles for ELORA brand
     * @returns {Array} Map styles array
     */
    getMapStyles() {
        return [
            {
                featureType: "poi",
                elementType: "labels",
                stylers: [{ visibility: "off" }]
            },
            {
                featureType: "transit",
                elementType: "labels",
                stylers: [{ visibility: "off" }]
            },
            {
                featureType: "road",
                elementType: "geometry",
                stylers: [{ color: "#f5f5f5" }]
            },
            {
                featureType: "water",
                elementType: "geometry",
                stylers: [{ color: "#c9e6f2" }]
            }
        ];
    }

    /**
     * Get user's current location using browser Geolocation API
     * @returns {Promise<Object>} Location {lat, lng}
     */
    /**
     * Get user's current location.
     * Prefers the IP data from CONFIG.LOCATION_IP (no paid APIs) and falls
     * back to the browser Geolocation API when no IP data is configured.
     * @returns {Promise<Object>} Location {lat, lng}
     */
    async getUserLocation() {
        // Sem chaves de API pagas: usa a localização por IP configurada.
        const ipData = (typeof CONFIG !== "undefined" && CONFIG.LOCATION_IP) || null;
        if (ipData && ipData.latitude && ipData.longitude) {
            this.userLocation = {
                lat: Number(ipData.latitude),
                lng: Number(ipData.longitude)
            };

            // Update map center if map exists
            if (this.map && typeof L !== "undefined") {
                this.map.setView([this.userLocation.lat, this.userLocation.lng], this.map.getZoom());
                this.addUserLocationMarker();
            }

            return Promise.resolve(this.userLocation);
        }

        return new Promise((resolve, reject) => {
            if (!navigator.geolocation) {
                reject(new Error("Geolocalização não suportada neste navegador"));
                return;
            }

            navigator.geolocation.getCurrentPosition(
                (position) => {
                    this.userLocation = {
                        lat: position.coords.latitude,
                        lng: position.coords.longitude
                    };
                    
                    // Update map center if map exists
                    if (this.map && typeof L !== "undefined") {
                        this.map.setView([this.userLocation.lat, this.userLocation.lng], this.map.getZoom());
                        this.addUserLocationMarker();
                    }
                    
                    resolve(this.userLocation);
                },
                (error) => {
                    let message = "Erro ao obter localização";
                    switch (error.code) {
                        case error.PERMISSION_DENIED:
                            message = "Permissão de localização negada";
                            break;
                        case error.POSITION_UNAVAILABLE:
                            message = "Localização indisponível";
                            break;
                        case error.TIMEOUT:
                            message = "Tempo esgotado ao obter localização";
                            break;
                    }
                    reject(new Error(message));
                },
                {
                    enableHighAccuracy: true,
                    timeout: 10000,
                    maximumAge: 300000 // 5 minutes
                }
            );
        });
    }

    /**
     * Add user location marker to map
     */
    addUserLocationMarker() {
        if (!this.userLocation || !this.map) return;

        if (typeof L !== "undefined") {
            // Remove existing user marker
            this.markers.filter(m => m.isUserMarker).forEach(m => this.map.removeLayer(m));
            this.markers = this.markers.filter(m => !m.isUserMarker);

            const icon = L.divIcon({
                className: "elora-user-marker",
                html: '<div class="elora-user-ball" style="width:22px;height:22px;border-radius:50%;background:#0d6efd;border:4px solid #fff;box-shadow:0 2px 8px rgba(13,110,253,.55);cursor:grab"></div>',
                iconSize: [22, 22],
                iconAnchor: [11, 11]
            });

            const marker = L.marker([this.userLocation.lat, this.userLocation.lng], {
                icon,
                draggable: true,
                title: "Sua localização (arraste para ajustar)"
            }).addTo(this.map);

            marker.bindPopup("<strong>Você está aqui</strong><br><small>Arraste a bola azul para ajustar</small>");
            marker.on('dragend', () => {
                const p = marker.getLatLng();
                this.userLocation = { lat: p.lat, lng: p.lng };
                if (typeof this.onUserLocationDrag === 'function') {
                    this.onUserLocationDrag(p.lat, p.lng);
                }
            });

            marker.isUserMarker = true;
            this.markers.push(marker);
        }
    }

    /**
     * Search for nearby caregivers
     * @param {Object} options - Search options
     * @returns {Promise<Array>} Array of caregivers with distance
     */
    async searchNearbyCaregivers(options = {}) {
        const {
            location = this.userLocation,
            radius = this.searchRadius,
            filters = {}
        } = options;

        if (!location) {
            await this.getUserLocation();
        }

        // In production: call backend API with location and radius
        // const response = await apiService.post("/caregivers/nearby", { location, radius, filters });
        
        await this.delay(800);
        
        // Mock search using caregiver service
        const caregivers = await caregiverService.searchCaregivers({
            ...filters,
            userLocation: location,
            distance: radius
        });

        // Add markers to map if available
        if (this.map && typeof L !== "undefined") {
            this.createCaregiverMarkers(caregivers);
        }

        return caregivers;
    }

    /**
     * Create markers for caregivers on map
     * @param {Array} caregivers - Array of caregiver objects
     */
    createCaregiverMarkers(caregivers) {
        if (!this.map || typeof L === "undefined") return;

        // Clear existing caregiver markers
        this.clearCaregiverMarkers();

        caregivers.forEach(caregiver => {
            if (!caregiver.address || !caregiver.address.lat || !caregiver.address.lng) return;

            const position = [caregiver.address.lat, caregiver.address.lng];

            const marker = L.marker(position, {
                icon: this.getCaregiverMarkerIcon(caregiver),
                title: caregiver.name
            }).addTo(this.map);

            marker.bindPopup(this.createInfoWindowContent(caregiver));
            marker.caregiverId = caregiver.id;
            this.markers.push(marker);
        });

        // Fit bounds to show all markers
        if (caregivers.length > 0) {
            const points = [];
            if (this.userLocation) {
                points.push([this.userLocation.lat, this.userLocation.lng]);
            }
            caregivers.forEach(cg => {
                if (cg.address?.lat && cg.address?.lng) {
                    points.push([cg.address.lat, cg.address.lng]);
                }
            });
            if (points.length) {
                this.map.fitBounds(L.latLngBounds(points), { padding: [30, 30] });
            }
        }
    }

    /**
     * Get marker icon for caregiver (Leaflet divIcon)
     * @param {Object} caregiver - Caregiver object
     * @returns {Object} Leaflet div icon
     */
    getCaregiverMarkerIcon(caregiver) {
        const color = caregiver.verified ? "#198754" : "#ffc107";
        const size = caregiver.rating >= 4.5 ? 22 : 18;

        return L.divIcon({
            className: "elora-caregiver-marker",
            html: `<div style="width:${size}px;height:${size}px;border-radius:50%;background:${color};border:2px solid white;box-shadow:0 1px 5px rgba(0,0,0,.35)"></div>`,
            iconSize: [size, size],
            iconAnchor: [size / 2, size / 2]
        });
    }

    /**
     * Create info window content for caregiver
     * @param {Object} caregiver - Caregiver object
     * @returns {string} HTML content
     */
    createInfoWindowContent(caregiver) {
        const esc = (typeof window !== 'undefined' && window.escapeHtml) ? window.escapeHtml : (v => String(v ?? ''));
        const stars = "★".repeat(Math.floor(caregiver.rating)) + "☆".repeat(5 - Math.floor(caregiver.rating));
        const distance = caregiver.distance ? `${esc(caregiver.distance)} km` : "Distância não disponível";

        return `
            <div class="p-2" style="min-width: 250px;">
                <div class="d-flex align-items-center">
                    <div class="d-flex align-items-center justify-content-center bg-light rounded-circle me-2" style="width:50px;height:50px;"><i class="bi bi-person-circle" style="font-size:1.8rem;color:#0E7A7B"></i></div>
                    <div>
                        <h6 class="mb-1">${esc(caregiver.name)}</h6>
                        <small class="text-muted">${distance}</small>
                    </div>
                </div>
                <div class="mt-2">
                    <span class="text-warning">${stars}</span>
                    <small class="text-muted ms-1">(${esc(caregiver.reviewCount)} avaliações)</small>
                </div>
                <div class="mt-2">
                    ${caregiver.specialties?.slice(0, 3).map(s => 
                        `<span class="badge bg-primary-subtle text-primary me-1">${esc(s)}</span>`
                    ).join("") || ""}
                </div>
                <div class="mt-2">
                    <small class="text-success">${caregiver.verified ? 
                        '<i class="bi bi-shield-check me-1"></i>Perfil verificado' : 
                        '<i class="bi bi-hourglass me-1"></i>Em verificação'}</small>
                </div>
                <div class="mt-3">
                    <button class="btn btn-primary btn-sm w-100" 
                            onclick="window.location.href=(window.authService?authService.resolveUrl('/src/pages/busca/perfil-cuidador.html'):'../busca/perfil-cuidador.html')+'?id=${encodeURIComponent(caregiver.id)}'">
                        Ver Perfil
                    </button>
                </div>
            </div>
        `;
    }

    /**
     * Clear caregiver markers
     */
    clearCaregiverMarkers() {
        this.markers.filter(m => m.caregiverId).forEach(marker => {
            if (this.map && typeof L !== "undefined") {
                this.map.removeLayer(marker);
            }
        });
        this.infoWindows.forEach(iw => iw.close && iw.close());
        this.markers = this.markers.filter(m => !m.caregiverId);
        this.infoWindows = [];
    }

    /**
     * Calculate distance between two points
     * @param {Object} origin - Origin {lat, lng}
     * @param {Object} destination - Destination {lat, lng}
     * @returns {Promise<number>} Distance in km
     */
    async calculateDistance(origin, destination) {
        // In production: use Google Maps Distance Matrix API
        // const service = new google.maps.DistanceMatrixService();
        // return new Promise((resolve, reject) => {
        //     service.getDistanceMatrix({
        //         origins: [origin],
        //         destinations: [destination],
        //         travelMode: google.maps.TravelMode.DRIVING,
        //         unitSystem: google.maps.UnitSystem.METRIC
        //     }, (response, status) => {
        //         if (status === 'OK') {
        //             resolve(response.rows[0].elements[0].distance.value / 1000);
        //         } else {
        //             reject(new Error(status));
        //         }
        //     });
        // });

        // Mock calculation
        await this.delay(100);
        return this.haversineDistance(origin, destination);
    }

    /**
     * Calculate distance using Haversine formula
     * @param {Object} loc1 - Location 1
     * @param {Object} loc2 - Location 2
     * @returns {number} Distance in km
     */
    haversineDistance(loc1, loc2) {
        const R = 6371;
        const dLat = (loc2.lat - loc1.lat) * Math.PI / 180;
        const dLon = (loc2.lng - loc1.lng) * Math.PI / 180;
        const a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                  Math.cos(loc1.lat * Math.PI / 180) * Math.cos(loc2.lat * Math.PI / 180) *
                  Math.sin(dLon/2) * Math.sin(dLon/2);
        const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return Math.round(R * c * 10) / 10;
    }

    /**
     * Filter caregivers on map
     * @param {Object} filters - Filter criteria
     */
    async filterCaregivers(filters) {
        this.searchRadius = filters.radius || this.searchRadius;
        const caregivers = await this.searchNearbyCaregivers({ filters });
        return caregivers;
    }

    /**
     * Get address from coordinates (reverse geocoding)
     * @param {Object} latLng - Latitude and longitude
     * @returns {Promise<string>} Formatted address
     */
    async getAddressFromCoordinates(latLng) {
        // In production: use Google Maps Geocoding API
        // const geocoder = new google.maps.Geocoder();
        // return new Promise((resolve, reject) => {
        //     geocoder.geocode({ location: latLng }, (results, status) => {
        //         if (status === 'OK' && results[0]) {
        //             resolve(results[0].formatted_address);
        //         } else {
        //             reject(new Error(status));
        //         }
        //     });
        // });

        await this.delay(200);
        return `Lat: ${latLng.lat.toFixed(4)}, Lng: ${latLng.lng.toFixed(4)}`;
    }

    /**
     * Get coordinates from address (geocoding)
     * @param {string} address - Address string
     * @returns {Promise<Object>} Coordinates {lat, lng}
     */
    async getCoordinatesFromAddress(address) {
        // In production: use Google Maps Geocoding API
        // const geocoder = new google.maps.Geocoder();
        // return new Promise((resolve, reject) => {
        //     geocoder.geocode({ address }, (results, status) => {
        //         if (status === 'OK' && results[0]) {
        //             const loc = results[0].geometry.location;
        //             resolve({ lat: loc.lat(), lng: loc.lng() });
        //         } else {
        //             reject(new Error(status));
        //         }
        //     });
        // });

        await this.delay(200);
        // Return São Paulo center as mock
        return CONFIG.MAPS.defaultCenter;
    }

    /**
     * Initialize location autocomplete for address inputs
     * @param {HTMLInputElement} input - Input element
     * @param {Function} onSelect - Callback when place selected
     */
    initializeAutocomplete(input, onSelect) {
        // Autocomplete de endereco exigia Google Places (pago). Sem chave, o
        // preenchimento de endereco continua disponivel via ViaCEP no cadastro.
        console.info("Autocomplete de endereço indisponível (Google Places desativado).");
        return null;
    }

    /**
     * Set search radius
     * @param {number} radius - Radius in km
     */
    setSearchRadius(radius) {
        this.searchRadius = Math.max(
            CONFIG.MAP_SEARCH_RADIUS.min,
            Math.min(CONFIG.MAP_SEARCH_RADIUS.max, radius)
        );
    }

    /**
     * Get current search radius
     * @returns {number} Radius in km
     */
    getSearchRadius() {
        return this.searchRadius;
    }

    /**
     * Destroy map and clean up
     */
    destroy() {
        this.clearCaregiverMarkers();
        this.markers.forEach(marker => {
            if (marker.setMap) marker.setMap(null);
        });
        this.markers = [];
        this.map = null;
        this.isInitialized = false;
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
const mapsService = new MapsService();

// Export for use in other modules
window.MapsService = MapsService;
window.mapsService = mapsService;

/**
 * Initialize location service - called from homepage search component
 * This prepares the geolocation service for future Google Maps API integration
 */
async function initializeLocationService() {
    try {
        // In production, this would load the Google Maps API script
        // await loadGoogleMapsAPI();
        
        // For now, just initialize the mock map service
        await mapsService.initializeMap({ containerId: "map" });
        
        // Try to get user location
        try {
            await mapsService.getUserLocation();
        } catch (error) {
            console.log("Localização do usuário não obtida:", error.message);
        }
        
        console.log("Serviço de localização inicializado");
    } catch (error) {
        console.error("Erro ao inicializar serviço de localização:", error);
    }
}

// Make function globally available
window.initializeLocationService = initializeLocationService;