package com.project.devcommunity.security.oauth2;

import com.project.devcommunity.domain.user.AuthProvider;
import com.project.devcommunity.domain.user.User;
import com.project.devcommunity.domain.user.UserRepository;
import com.project.devcommunity.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    /*
    OAuth2 공급자로부터 사용자 정보 가져오는 메서드
     */
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException{
        OAuth2User oAuth2User = super.loadUser(userRequest);

        try{
            return processOAuth2User(userRequest, oAuth2User);
        } catch(AuthenticationException e){
            throw e;
        } catch(Exception e){
            // Throwing an instance of AuthenticationException will trigger the OAuth2AuthenticationFailureHandler
            throw new InternalAuthenticationServiceException(e.getMessage(), e.getCause());
        }
    }

    /*
    사용자 정보 추출 메서드
     */
    private OAuth2User processOAuth2User(OAuth2UserRequest userRequest, OAuth2User oAuth2User) {
        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(userRequest.getClientRegistration().getRegistrationId(), oAuth2User.getAttributes());
        if(StringUtils.isEmpty(oAuth2UserInfo.getEmail())){
            throw new OAuth2AuthenticationProcessingException("OAuth2 공급자에서 이메일을 찾을 수 없습니다.");
        }

        Optional<User> userOptional = userRepository.findByEmail(oAuth2UserInfo.getEmail());
        User user;
        if(userOptional.isPresent()){ //기존 등록된 사용자인 경우
            user = userOptional.get();
            if(!user.getProvider().equals(AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId()))){
                throw new OAuth2AuthenticationProcessingException(
                  user.getProvider() + "계정을 사용하기 위해서 로그인이 필요합니다."
                );
            }
            user = updateExistingUser(user, oAuth2UserInfo); //사용자 정보 업데이트 메소드 호출
        } else{ //등록되지 않은 사용자인 경우
            user = registerNewUser(userRequest, oAuth2UserInfo);
        }

        return UserPrincipal.create(user, oAuth2User.getAttributes());
    }

    /*
    DB에 존재하지 않은 경우 새로 사용자 등록하는 메서드
     */
    private User registerNewUser(OAuth2UserRequest userRequest, OAuth2UserInfo oAuth2UserInfo) {

        return userRepository.save(User.builder()
                .name(oAuth2UserInfo.getName())
                .email(oAuth2UserInfo.getEmail())
                .imageUrl(oAuth2UserInfo.getImageUrl())
                .provider(AuthProvider.valueOf(userRequest.getClientRegistration().getRegistrationId()))
                .providerId(oAuth2UserInfo.getId())
                .build()
        );
    }

    /*
    DB에 존재하는 경우 사용자 정보 업데이트하는 메서드
     */
    private User updateExistingUser(User existingUser, OAuth2UserInfo oAuth2UserInfo) {

        return userRepository.save(existingUser
            .update(
                    oAuth2UserInfo.getName(),
                    oAuth2UserInfo.getImageUrl()
            )
        );
    }
}
