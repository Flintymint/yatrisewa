package com.example.yatrisewa.security;

import com.example.yatrisewa.models.ReservationApiException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {
    @Value("${app.jwt-secret}")
    private String jwtSecretKey;
    @Value("${app.jwt-expiration-milliseconds}")
    private Long expiration;

    public String generateToken(Authentication authentication) {
        String userName = authentication.getName();
        Date expireDate = new Date(new Date().getTime() + expiration);
        return Jwts.builder()
                .setSubject(userName)
                .setIssuedAt(new Date())
                .setExpiration(expireDate)
                .signWith(key())
                .compact();
    }

    public String getUserName(String token) {
        Claims claims = Jwts.parser()
                .setSigningKey(key())
                .build()
                .parseClaimsJws(token)
                .getBody();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .setSigningKey(key())
                    .build()
                    .parse(token);
            return true;
        }catch (MalformedJwtException e){
            throw new ReservationApiException(HttpStatus.BAD_REQUEST,"Invalid token");
        }catch (ExpiredJwtException e){
            throw new ReservationApiException(HttpStatus.BAD_REQUEST,"Token Expired");
        }catch (UnsupportedJwtException e){
            throw new ReservationApiException(HttpStatus.BAD_REQUEST,"Unsupported token");
        }catch (IllegalArgumentException e){
            throw new ReservationApiException(HttpStatus.BAD_REQUEST,"Invalid arguments");
        }
    }

    private Key key(){
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecretKey));
    }
}
