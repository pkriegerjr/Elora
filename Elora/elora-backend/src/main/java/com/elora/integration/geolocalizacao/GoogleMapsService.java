package com.elora.integration.geolocalizacao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class GoogleMapsService {
    @Value(value="\")
    private String apiKey;

    @Cacheable(value="geolocalizacao", key="#cidade")
    public double[] geocodificar(String cidade){
        // Cache Redis para RNF-ELO-009 (≤2s)
        return new double[]{-23.55, -46.63};
    }
    public double calcularDistancia(double lat1, double lon1, double lat2, double lon2){
        // Haversine
        return 0.0;
    }
}
