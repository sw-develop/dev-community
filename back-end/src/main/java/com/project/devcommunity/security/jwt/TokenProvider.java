package com.project.devcommunity.security.jwt;

import com.project.devcommunity.config.AppProperties;
import com.project.devcommunity.domain.user.User;
import com.project.devcommunity.security.UserPrincipal;
import io.jsonwebtoken.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class TokenProvider {

    private static final Logger logger = LoggerFactory.getLogger(TokenProvider.class);

    private AppProperties appProperties;

    public TokenProvider(AppProperties appProperties){
        this.appProperties = appProperties;
    }

    /*
    Token 생성 메서드
     */
    public String createToken(Authentication authentication){
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + appProperties.getAuth().getTokenExpirationMsec());

        return Jwts.builder()
                .setSubject(Long.toString(userPrincipal.getId())) //User의 id..?
                .setIssuedAt(new Date())
                .setExpiration(expiryDate)
                .signWith(SignatureAlgorithm.HS512, appProperties.getAuth().getTokenSecret())
                .compact();
    }

    /*
    Token으로부터 사용자 정보 반환하는 메서드
    */
    public Long getUserIdFromToken(String token){
        Claims claims = Jwts.parser()
                .setSigningKey(appProperties.getAuth().getTokenSecret())
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    /*
    Token 유효성 검사 메서드
    */
    public boolean validateToken(String authToken){
        try{
            Jwts.parser().setSigningKey(appProperties.getAuth().getTokenSecret()).parseClaimsJws(authToken);
            return true;
        } catch (SignatureException e){
            logger.error("유요하지 않은 JWT 서명");
        } catch (MalformedJwtException e){
            logger.error("유요하지 않은 JWT 토큰");
        } catch (ExpiredJwtException e){
            logger.error("만료된 JWT 토큰");
        } catch (UnsupportedJwtException e){
            logger.error("지원하지 않는 JWT 토큰");
        } catch (IllegalArgumentException e){
            logger.error("비어있는 JWT");
        }
        return false; //예외 발생 시
    }
}
