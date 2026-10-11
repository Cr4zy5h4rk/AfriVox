package com.afrivox.app.web.rest.vm;

import com.afrivox.app.service.dto.AdminUserDTO;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * View Model extending the AdminUserDTO, which is meant to be used in the user management UI.
 */
public class ManagedUserVM extends AdminUserDTO {

    public static final int PASSWORD_MIN_LENGTH = 8;

    public static final int PASSWORD_MAX_LENGTH = 100;

    @Size(min = PASSWORD_MIN_LENGTH, max = PASSWORD_MAX_LENGTH)
    private String password;

    /** Role choisi a l'inscription (obligatoire a /register, ignore ailleurs). Jamais ADMIN. */
    @Pattern(regexp = "CONTRIBUTOR|DEVELOPER|ORGANIZATION")
    private String role;

    /** Version du consentement acceptee (obligatoire a /register). */
    @Size(max = 32)
    private String consentVersion;

    /** Confirmation « j'ai 18 ans ou plus » (obligatoire pour un contributeur). */
    private Boolean adult;

    /** Code de la langue maternelle, ou {@code other} (obligatoire pour un contributeur). */
    @Size(max = 20)
    @Pattern(regexp = "[A-Za-z0-9_-]*")
    private String nativeLanguage;

    public ManagedUserVM() {
        // Empty constructor needed for Jackson.
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getAdult() {
        return adult;
    }

    public void setAdult(Boolean adult) {
        this.adult = adult;
    }

    public String getNativeLanguage() {
        return nativeLanguage;
    }

    public void setNativeLanguage(String nativeLanguage) {
        this.nativeLanguage = nativeLanguage;
    }

    public String getConsentVersion() {
        return consentVersion;
    }

    public void setConsentVersion(String consentVersion) {
        this.consentVersion = consentVersion;
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "ManagedUserVM{" + super.toString() + "} ";
    }
}
