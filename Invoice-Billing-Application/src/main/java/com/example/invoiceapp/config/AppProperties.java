package com.example.invoiceapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Device device = new Device();
    private final Cloud cloud = new Cloud();
    private final Supabase supabase = new Supabase();

    public Device getDevice() { return device; }
    public Cloud getCloud() { return cloud; }
    public Supabase getSupabase() { return supabase; }

    public static class Device {
        private String name = "SwiftLab Device";
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Cloud {
        private String apiUrl = "";
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
    }

    public static class Supabase {
        private String url = "";
        private String anonKey = "";
        private String serviceRoleKey = "";
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getAnonKey() { return anonKey; }
        public void setAnonKey(String anonKey) { this.anonKey = anonKey; }
        public String getServiceRoleKey() { return serviceRoleKey; }
        public void setServiceRoleKey(String serviceRoleKey) { this.serviceRoleKey = serviceRoleKey; }
        public boolean isConfigured() {
            return url != null && !url.isBlank() && anonKey != null && !anonKey.isBlank();
        }
    }
}
