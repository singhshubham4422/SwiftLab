package com.example.invoiceapp.dto;

public class AuthResponse {
    private String token;
    private UserDTO user;
    private OrganizationDTO organization;

    public AuthResponse() {}

    public AuthResponse(String token, UserDTO user, OrganizationDTO organization) {
        this.token = token;
        this.user = user;
        this.organization = organization;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public UserDTO getUser() { return user; }
    public void setUser(UserDTO user) { this.user = user; }
    public OrganizationDTO getOrganization() { return organization; }
    public void setOrganization(OrganizationDTO organization) { this.organization = organization; }
}
