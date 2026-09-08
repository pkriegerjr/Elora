/**
 * ELORA Platform - Maps Service
 * 
 * This module handles Google Maps integration for geolocation,
 * nearby caregiver search, distance calculation, and map interactions.
 * 
 * IMPORTANT: Google Maps API key should NEVER be hardcoded in production.
 * It should be obtained from a secure backend configuration endpoint.
 * 
 * For this prototype, the map functionality is simulated.
 * Real implementation requires a valid Google Maps JavaScript API key.
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
     * Initialize Google Maps
     * @param {Object} options - Map options
     * @returns {Promise<google.maps.Map>} Map instance
     */
    async loadGoogleMapsScript(){
        // Spring Boot: busca chave segura em GET /api/config/maps-key
        if(!CONFIG.API.MOCK_MODE && !CONFIG.GOOGLE_MAPS.apiKey.startsWith("YOUR")){
            // já tem chave
        } else if(!CONFIG.API.MOCK_MODE){
            try{
                const { apiKey } = await apiService.get(CONFIG.GOOGLE_MAPS.configEndpoint);
                CONFIG.GOOGLE_MAPS.apiKey = apiKey;
            } catch(e){ console.warn("Falha ao obter Maps key do Spring:", e.message); }
        }
        if(CONFIG.GOOGLE_MAPS.apiKey && CONFIG.GOOGLE_MAPS.apiKey.startsWith("YOUR")) return;
        if(document.querySelector('script[data-elora-maps]')) return;
        await new Promise((resolve,reject)=>{
            const s=document.createElement('script');
            s.dataset.eloraMaps="true";
            s.src=`https://maps.googleapis.com/maps/api/js?key=${CONFIG.GOOGLE_MAPS.apiKey}&libraries=${CONFIG.GOOGLE_MAPS.libraries.join(',')}`;
            s.async=true; s.defer=true;
            s.onload=resolve; s.onerror=reject;
            document.head.appendChild(s);
        });
    }

    async initializeMap(options = {}) {
        // Tenta carregar script real se MOCK_MODE=false
        if(!CONFIG.API.MOCK_MODE){
            try{ await this.loadGoogleMapsScript(); } catch(e){ console.warn(e); }
        }
        
        if (this.isInitialized && this.map) {
            return this.map;
        }

        // Check if Google Maps API is available
        if (typeof google === "undefined" || !google.maps) {
            console.warn("Google Maps API não carregada. Usando modo de simulação.");
            return this.initializeMockMap(options);
        }

        try {
            const container = document.getElementById(options.containerId || "map");
            if (!container) {
                throw new Error("Container do mapa não encontrado");
            }

            const center = options.center || CONFIG.GOOGLE_MAPS.defaultCenter;
            const zoom = options.zoom || CONFIG.GOOGLE_MAPS.defaultZoom;

            this.map = new google.maps.Map(container, {
                center,
                zoom,
                mapTypeControl: false,
                streetViewControl: false,
                fullscreenControl: true,
                styles: this.getMapStyles()
            });

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
                        <p class="text-muted small">Integração com Google Maps JavaScript API</p>
                        <p class="text-muted small">Configure a API Key em config/config.js</p>
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
    async getUserLocation() {
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
                    if (this.map && typeof google !== "undefined") {
                        this.map.setCenter(this.userLocation);
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

        if (typeof google !== "undefined" && google.maps) {
            // Remove existing user marker
            this.markers = this.markers.filter(m => !m.isUserMarker);
            
            const marker = new google.maps.Marker({
                position: this.userLocation,
                map: this.map,
                title: "Sua localização",
                icon: {
                    path: google.maps.SymbolPath.CIRCLE,
                    scale: 12,
                    fillColor: "#0d6efd",
                    fillOpacity: 1,
                    strokeColor: "#ffffff",
                    strokeWeight: 3
                },
                zIndex: 1000
            });
            
            marker.isUserMarker = true;
            this.markers.push(marker);

            // Add accuracy circle
            if (navigator.geolocation) {
                navigator.geolocation.getCurrentPosition(pos => {
                    const circle = new google.maps.Circle({
                        strokeColor: "#0d6efd",
                        strokeOpacity: 0.3,
                        strokeWeight: 1,
                        fillColor: "#0d6efd",
                        fillOpacity: 0.1,
                        map: this.map,
                        center: this.userLocation,
                        radius: pos.coords.accuracy
                    });
                    this.markers.push(circle);
                });
            }
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
        if (this.map && typeof google !== "undefined") {
            this.createCaregiverMarkers(caregivers);
        }

        return caregivers;
    }

    /**
     * Create markers for caregivers on map
     * @param {Array} caregivers - Array of caregiver objects
     */
    createCaregiverMarkers(caregivers) {
        if (!this.map || typeof google === "undefined") return;

        // Clear existing caregiver markers
        this.clearCaregiverMarkers();

        caregivers.forEach(caregiver => {
            if (!caregiver.address || !caregiver.address.lat || !caregiver.address.lng) return;

            const position = {
                lat: caregiver.address.lat,
                lng: caregiver.address.lng
            };

            const marker = new google.maps.Marker({
                position,
                map: this.map,
                title: caregiver.name,
                icon: this.getCaregiverMarkerIcon(caregiver),
                caregiverId: caregiver.id
            });

            // Create info window
            const infoWindow = new google.maps.InfoWindow({
                content: this.createInfoWindowContent(caregiver)
            });

            marker.addListener("click", () => {
                // Close other info windows
                this.infoWindows.forEach(iw => iw.close());
                infoWindow.open(this.map, marker);
            });

            this.markers.push(marker);
            this.infoWindows.push(infoWindow);
        });

        // Fit bounds to show all markers
        if (caregivers.length > 0) {
            const bounds = new google.maps.LatLngBounds();
            if (this.userLocation) {
                bounds.extend(this.userLocation);
            }
            caregivers.forEach(cg => {
                if (cg.address?.lat && cg.address?.lng) {
                    bounds.extend({ lat: cg.address.lat, lng: cg.address.lng });
                }
            });
            this.map.fitBounds(bounds);
        }
    }

    /**
     * Get marker icon for caregiver
     * @param {Object} caregiver - Caregiver object
     * @returns {Object} Marker icon config
     */
    getCaregiverMarkerIcon(caregiver) {
        const color = caregiver.verified ? "#198754" : "#ffc107";
        const scale = caregiver.rating >= 4.5 ? 14 : 12;

        return {
            path: google.maps.SymbolPath.CIRCLE,
            scale,
            fillColor: color,
            fillOpacity: 1,
            strokeColor: "#ffffff",
            strokeWeight: 2,
            labelOrigin: new google.maps.Point(0, 0)
        };
    }

    /**
     * Create info window content for caregiver
     * @param {Object} caregiver - Caregiver object
     * @returns {string} HTML content
     */
    createInfoWindowContent(caregiver) {
        const stars = "★".repeat(Math.floor(caregiver.rating)) + "☆".repeat(5 - Math.floor(caregiver.rating));
        const distance = caregiver.distance ? `${caregiver.distance} km` : "Distância não disponível";

        return `
            <div class="p-2" style="min-width: 250px;">
                <div class="d-flex align-items-center">
                    <div class="d-flex align-items-center justify-content-center bg-light rounded-circle me-2" style="width:50px;height:50px;"><i class="bi bi-person-circle" style="font-size:1.8rem;color:#0E7A7B"></i></div>
                    <div>
                        <h6 class="mb-1">${caregiver.name}</h6>
                        <small class="text-muted">${distance}</small>
                    </div>
                </div>
                <div class="mt-2">
                    <span class="text-warning">${stars}</span>
                    <small class="text-muted ms-1">(${caregiver.reviewCount} avaliações)</small>
                </div>
                <div class="mt-2">
                    ${caregiver.specialties?.slice(0, 3).map(s => 
                        `<span class="badge bg-primary-subtle text-primary me-1">${s}</span>`
                    ).join("") || ""}
                </div>
                <div class="mt-2">
                    <small class="text-success">${caregiver.verified ? 
                        '<i class="bi bi-shield-check me-1"></i>Perfil verificado' : 
                        '<i class="bi bi-hourglass me-1"></i>Em verificação'}</small>
                </div>
                <div class="mt-3">
                    <button class="btn btn-primary btn-sm w-100" 
                            onclick="window.location.href=(window.authService?authService.resolveUrl('/pages/perfil-cuidador.html'):'perfil-cuidador.html')+'?id=${caregiver.id}'">
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
        this.markers.forEach(marker => {
            if (marker.caregiverId && marker.setMap) {
                marker.setMap(null);
            }
        });
        this.infoWindows.forEach(iw => iw.close());
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
        return CONFIG.GOOGLE_MAPS.defaultCenter;
    }

    /**
     * Initialize location autocomplete for address inputs
     * @param {HTMLInputElement} input - Input element
     * @param {Function} onSelect - Callback when place selected
     */
    initializeAutocomplete(input, onSelect) {
        if (typeof google === "undefined" || !google.maps.places) {
            console.warn("Google Places API não disponível");
            return null;
        }

        const autocomplete = new google.maps.places.Autocomplete(input, {
            types: ["geocode"],
            componentRestrictions: { country: "br" },
            fields: ["geometry", "formatted_address", "address_components"]
        });

        autocomplete.addListener("place_changed", () => {
            const place = autocomplete.getPlace();
            if (place.geometry) {
                onSelect({
                    address: place.formatted_address,
                    location: {
                        lat: place.geometry.location.lat(),
                        lng: place.geometry.location.lng()
                    },
                    components: place.address_components
                });
            }
        });

        return autocomplete;
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