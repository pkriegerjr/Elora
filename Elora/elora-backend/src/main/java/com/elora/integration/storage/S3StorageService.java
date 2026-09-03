package com.elora.integration.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class S3StorageService {
    @Value(value="\")
    private String bucket;
    public String upload(String key, byte[] content){ return "s3://"+bucket+"/"+key; }
    public byte[] download(String key){ return new byte[0]; }
}
