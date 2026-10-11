package com.finsphere.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.jspecify.annotations.Nullable;

/**
 * Personal details of an account. Shares the primary key of {@link UserAccount}. Table
 * {@code fsn_identity.customer_profiles}.
 */
@Entity
@Table(name = "customer_profiles")
public class CustomerProfile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserAccount user;

    /** Required for new registrations; 1.x rows may not have it. */
    @Column(name = "full_name", length = 100)
    private @Nullable String fullName;

    @Column(name = "care_of", length = 100)
    private @Nullable String careOf;

    /** Required for new registrations; 1.x rows may not have it. */
    @Column(name = "address", length = 500)
    private @Nullable String address;

    public Long getId() {
        return this.id;
    }

    public UserAccount getUser() {
        return this.user;
    }

    void setUser(UserAccount user) {
        this.user = user;
    }

    public @Nullable String getFullName() {
        return this.fullName;
    }

    public void setFullName(@Nullable String fullName) {
        this.fullName = (fullName != null) ? fullName.trim() : null;
    }

    public @Nullable String getCareOf() {
        return this.careOf;
    }

    public void setCareOf(@Nullable String careOf) {
        this.careOf = (careOf == null || careOf.isBlank()) ? null : careOf.trim();
    }

    public @Nullable String getAddress() {
        return this.address;
    }

    public void setAddress(@Nullable String address) {
        this.address = (address != null) ? address.trim() : null;
    }
}
