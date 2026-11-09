package com.threadflow.buyer.portal;

import com.threadflow.buyer.Buyer;
import java.util.Collection;
import java.util.List;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class BuyerAuthenticationToken extends AbstractAuthenticationToken {
    private static final List<GrantedAuthority> AUTHORITIES = List.of(new SimpleGrantedAuthority("ROLE_BUYER"));
    private final Buyer buyer;

    public BuyerAuthenticationToken(Buyer buyer) {
        super(AUTHORITIES);
        this.buyer = buyer;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public Buyer getPrincipal() {
        return buyer;
    }

    @Override
    public String getName() {
        return buyer.getBuyerName() + " (" + buyer.getCountry() + ")";
    }
}
