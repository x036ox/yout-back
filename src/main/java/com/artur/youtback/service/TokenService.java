package com.artur.youtback.service;

import com.artur.youtback.model.user.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Collectors;

@Service
public class TokenService {

    @Autowired
    private JwtEncoder jwtEncoder;
    @Autowired
    private JwtDecoder jwtDecoder;

    public static boolean isExpired(Jwt jwtToken){
        return jwtToken.getExpiresAt().isBefore(Instant.now());
    }

    public String generateAccessToken(Authentication authentication){
        Instant now = Instant.now();
        String scope = StringUtils.collectionToCommaDelimitedString(authentication.getAuthorities());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.SECONDS))
                .subject(authentication.getName())
                .claim("type", "access")
                .claim("scope", scope)
                .build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateAccessToken(User user){
        Instant now = Instant.now();
        String scope = StringUtils.collectionToCommaDelimitedString(user.getAuthorities());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.MINUTES))
                .subject(user.getId().toString())
                .claim("type", "access")
                .claim("scope", scope)
                .build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateRefreshToken(Authentication authentication){
        Instant now = Instant.now();
        String scope = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(" "));

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .subject(authentication.getName())
                .claim("type", "refresh")
                .claim("scope", scope)
                .build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateRefreshToken(User user){
        Instant now = Instant.now();
        String scope = StringUtils.collectionToCommaDelimitedString(user.getAuthorities());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("self")
                .issuedAt(now)
                .expiresAt(now.plus(30, ChronoUnit.DAYS))
                .subject(user.getId().toString())
                .claim("type", "refresh")
                .claim("scope", scope)
                .build();
        return this.jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public boolean isTokenValid(String token){
        if(token == null || token.length() == 0){
            return false;
        }
        try {
            this.decode(token);
            return true;
        } catch (JwtException e){
            return false;
        }
    }


    public Jwt decode(String token) throws JwtException{
        return jwtDecoder.decode(token);
    }
}
