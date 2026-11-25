/**
 * ELORA Platform - Location Service (sem APIs pagas)
 *
 * - ViaCEP: endereço a partir de CEP (gratuito, somente Brasil).
 * - Nominatim/OpenStreetMap: geocodificação (lat/lng) gratuita.
 * - Monta um rótulo único com rua, número, bairro, cidade/UF e país.
 */

class LocationService {
    constructor() {
        this.lastGeocodeAt = 0;
        this.intercept = 1200; // Nominatim: no máximo ~1 requisição/segundo
    }

    onlyDigits(value) {
        return String(value || '').replace(/\D/g, '');
    }

    maskCep(value) {
        const d = this.onlyDigits(value).slice(0, 8);
        return d.length > 5 ? `${d.slice(0, 5)}-${d.slice(5)}` : d;
    }

    formatLabel(loc) {
        const street = loc.street ? `${loc.street}${loc.number ? ', ' + loc.number : ''}` : '';
        const detail = [street, loc.neighborhood].filter(Boolean).join(' - ');
        const cityState = [loc.city, loc.state].filter(Boolean).join('/');
        const country = loc.country || 'Brasil';
        return [detail, cityState, country].filter(Boolean).join(', ');
    }

    _waitForRateLimit() {
        const now = Date.now();
        const wait = Math.max(0, this.intercept - (now - this.lastGeocodeAt));
        this.lastGeocodeAt = now + wait;
        return wait ? new Promise(resolve => setTimeout(resolve, wait)) : Promise.resolve();
    }

    /** Consulta o ViaCEP. */
    async fetchCep(cep) {
        const digits = this.onlyDigits(cep);
        if (digits.length !== 8) throw new Error('CEP deve ter 8 dígitos');

        const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`);
        if (!response.ok) throw new Error('Falha ao consultar o CEP (ViaCEP)');

        const data = await response.json();
        if (data.erro) throw new Error('CEP não encontrado');

        return data;
    }

    /** Geocodifica um texto (ex.: "Rua Paulino Borrelli, Praia Grande, SP") via Nominatim. */
    async geocode(query) {
        if (!query || !String(query).trim()) return null;

        await this._waitForRateLimit();

        const url = 'https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&accept-language=pt-BR&q=' +
            encodeURIComponent(query);
        const response = await fetch(url, { headers: { 'Accept': 'application/json' } });
        if (!response.ok) throw new Error('Falha ao geocodificar o endereço');

        const results = await response.json();
        if (!results || !results.length) return null;

        const first = results[0];
        return {
            lat: parseFloat(first.lat),
            lng: parseFloat(first.lon),
            displayName: first.display_name
        };
    }

    /** Autocomplete: até `limit` sugestões de lugares para um texto digitado. */
    async autocomplete(query, limit = 5, bias = null) {
        const text = String(query || '').trim();
        if (text.length < 3) return [];

        const params = {
            format: 'jsonv2', addressdetails: '1', limit: String(limit),
            'accept-language': 'pt-BR', countrycodes: 'br'
        };
        // Viés para perto de onde a pessoa está: evita encontrar o mesmo bairro
        // em outra cidade (ex.: interior da Paraíba).
        if (bias && isFinite(bias.lat) && isFinite(bias.lng)) {
            params.lat = String(bias.lat);
            params.lon = String(bias.lng);
        }

        // Primeiro tenta o bairro dentro da cidade/UF atual; depois sem esse filtro.
        const queries = (bias && bias.city && bias.state)
            ? [`${text}, ${bias.city}, ${bias.state}, Brasil`, text]
            : [text];

        const seen = new Set();
        const out = [];
        for (const q of queries) {
            await this._waitForRateLimit();
            const url = 'https://nominatim.openstreetmap.org/search?' +
                new URLSearchParams({ ...params, q }).toString();
            const response = await fetch(url, { headers: { 'Accept': 'application/json' } });
            if (!response.ok) throw new Error('Falha ao buscar localidades');

            const results = await response.json();
            for (const item of (results || [])) {
                const loc = this._resultToLocation(item, text);
                const key = loc.displayName || loc.label;
                if (key && !seen.has(key)) {
                    seen.add(key);
                    out.push(loc);
                }
            }
            if (out.length >= limit) break;
        }
        const preferNeighborhood = !!(bias && bias.city && bias.state &&
            !text.toLowerCase().includes(bias.city.toLowerCase()));

        return out.slice(0, limit).sort((a, b) =>
            (preferNeighborhood ? (a.neighborhood ? 0 : 1) - (b.neighborhood ? 0 : 1) : 0)
        );
    }

    /** Reverse geocoding: coordenas -> endereço legível. */
    async reverse(lat, lng) {
        await this._waitForRateLimit();

        const url = `https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&addressdetails=1` +
            `&lat=${encodeURIComponent(lat)}&lon=${encodeURIComponent(lng)}`;
        const response = await fetch(url, { headers: { 'Accept': 'application/json' } });
        if (!response.ok) throw new Error('Falha ao identificar o local');

        const data = await response.json();
        if (data.error) return null;
        return this._resultToLocation(data, data.display_name);
    }

    _resultToLocation(item, fallbackLabel) {
        const a = item.address || {};
        const lat = parseFloat(item.lat);
        const lng = parseFloat(item.lon);
        let neighborhood = a.suburb || a.neighbourhood || a.quarter || '';
        // Nominatim nem sempre preenche `suburb`: aproveita o primeiro trecho do display_name.
        if (!neighborhood && item.display_name) {
            const first = String(item.display_name).split(',')[0].trim();
            if (first && first.toLowerCase() !== String(a.city || '').toLowerCase() && !/^\d/.test(first)) {
                neighborhood = first;
            }
        }
        const loc = {
            lat,
            lng,
            cep: a.postcode || '',
            street: a.road || a.pedestrian || a.footway || '',
            number: a.house_number || '',
            neighborhood,
            city: a.city || a.town || a.village || a.municipality || '',
            state: a.state || '',
            country: a.country || 'Brasil',
            displayName: item.display_name || fallbackLabel
        };
        loc.label = this.formatLabel(loc);
        return loc;
    }

    /** Resolve CEP (+ número opcional) em uma localização completa. */
    async resolveCep(cep, numero) {
        const data = await this.fetchCep(cep);
        const number = String(numero || '').trim();

        const base = {
            cep: data.cep,
            street: data.logradouro || '',
            number,
            neighborhood: data.bairro || '',
            city: data.localidade || '',
            state: data.uf || '',
            country: 'Brasil',
            lat: null,
            lng: null
        };

        // Tenta primeiro o endereço completo; se falhar, usa cidade/UF.
        const endereco = [data.logradouro, number, data.bairro, data.localidade, data.uf, 'Brasil']
            .filter(Boolean).join(', ');
        let geo = await this.geocode(endereco);
        if (!geo) {
            geo = await this.geocode(`${data.localidade}, ${data.uf}, Brasil`);
        }

        if (geo) {
            base.lat = geo.lat;
            base.lng = geo.lng;
        }

        base.label = this.formatLabel(base);
        return base;
    }

    /** Aceita "lat,lng" ou texto livre (cidade, endereço). */
    async resolveText(input) {
        const value = String(input || '').trim();
        const match = value.match(/^\s*(-?\d+(?:\.\d+)?)\s*,\s*(-?\d+(?:\.\d+)?)\s*$/);
        if (match) {
            const lat = parseFloat(match[1]);
            const lng = parseFloat(match[2]);
            return { lat, lng, label: `${lat.toFixed(5)},${lng.toFixed(5)}`, city: '', state: '', country: '' };
        }

        const geo = await this.geocode(value);
        if (!geo) return null;

        const parts = geo.displayName ? geo.displayName.split(',') : [];
        return {
            lat: geo.lat,
            lng: geo.lng,
            label: value,
            city: (parts[0] || '').trim(),
            state: (parts[1] || '').trim(),
            country: (parts[parts.length - 1] || 'Brasil').trim()
        };
    }
}

const locationService = new LocationService();
window.LocationService = LocationService;
window.locationService = locationService;
