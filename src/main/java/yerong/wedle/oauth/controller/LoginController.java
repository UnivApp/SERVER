package yerong.wedle.oauth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import yerong.wedle.common.exception.ResponseCode;
import yerong.wedle.member.dto.MemberRequest;
import yerong.wedle.member.exception.MemberNotFoundException;
import yerong.wedle.oauth.dto.MemberLogoutResponse;
import yerong.wedle.oauth.dto.TokenResponse;
import yerong.wedle.oauth.exception.InvalidRefreshTokenException;
import yerong.wedle.oauth.exception.InvalidTokenException;
import yerong.wedle.oauth.service.AuthService;
import yerong.wedle.oauth.service.JwtBlacklistService;

import java.security.Principal;

@Tag(name = "Authentication API", description = "로그인 및 토큰 갱신 관련 API")
@RestController
@RequiredArgsConstructor
@Slf4j
public class LoginController {

    private final AuthService authService;
    private final JwtBlacklistService jwtBlacklistService;

    @Operation(
            summary = "Apple 로그인",
            description = "Apple 로그인 요청을 처리하고 액세스 토큰을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, 액세스 토큰 반환"),
            @ApiResponse(responseCode = "500", description = "로그인 중 서버 오류 발생")
    })
    @PostMapping("/login/apple")
    public ResponseEntity<?> login(@RequestBody MemberRequest memberRequest) throws Exception {
        try {
            TokenResponse tokenResponse = authService.login(memberRequest);

            HttpHeaders headers = authService.setTokenHeaders(tokenResponse);

            return ResponseEntity.ok().headers(headers).body(tokenResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("로그인 중 오류가 발생했습니다.");
        }

    }

    @Operation(
            summary = "액세스 토큰 갱신",
            description = "유효한 리프레시 토큰을 사용하여 액세스 토큰을 갱신합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "토큰 갱신 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 리프레시 토큰"),
            @ApiResponse(responseCode = "404", description = "회원 정보를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "토큰 갱신 중 서버 오류 발생")
    })
    @PostMapping("/login/refresh")
    public ResponseEntity<?> refreshAccessToken(@RequestHeader("RefreshToken") String refreshToken) {
        try {
            TokenResponse tokenResponse = authService.refreshAccessToken(refreshToken);
            HttpHeaders headers = authService.setTokenHeaders(tokenResponse);

            return ResponseEntity.ok().headers(headers).body(tokenResponse);
        } catch (InvalidRefreshTokenException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 Refresh Token입니다.");
        } catch (MemberNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("회원 정보를 찾을 수 없습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("토큰 갱신 중 오류가 발생했습니다.");
        }
    }
    @Operation(
            summary = "로그인 상태 확인",
            description = "현재 사용자의 로그인 상태를 확인합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 상태 확인 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "500", description = "상태 확인 중 서버 오류 발생")
    })
    @PostMapping("/login/status")
    public ResponseEntity<?> checkLoginStatus(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        String accessToken = authService.extractAccessTokenFromHeader(authorizationHeader);

        try {

            if (jwtBlacklistService.isTokenBlacklisted(accessToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("토큰이 블랙리스트에 등록되어 있습니다.");
            }

            boolean isLoggedIn = authService.isLoggedIn();

            if (isLoggedIn) {
                return ResponseEntity.ok().body("사용자는 로그인 상태입니다.");
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자는 로그인 상태가 아닙니다.");
            }
        } catch (InvalidTokenException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 토큰입니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("상태 확인 중 오류가 발생했습니다.");
        }
    }

    @Operation(
            summary = "로그아웃",
            description = "현재 유효한 액세스 토큰과 리프레시 토큰을 블랙리스트에 등록하여 로그아웃 처리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "요청 헤더에서 유효하지 않은 액세스 토큰 또는 이미 블랙리스트에 등록된 토큰"),
            @ApiResponse(responseCode = "404", description = "해당 Social ID로 회원을 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "서버 오류로 인해 로그아웃 실패")
    })
    @PostMapping("/member/logout")
    public ResponseEntity<?> logout(Principal principal, HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        String accessToken = authService.extractAccessTokenFromHeader(authorizationHeader);

        if (accessToken == null) {
            log.warn("요청 헤더에서 액세스 토큰을 찾을 수 없음.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("요청 헤더에서 액세스 토큰을 찾을 수 없음.");
        }

        if (jwtBlacklistService.isTokenBlacklisted(accessToken)) {
            log.warn("이미 블랙리스트에 있는 액세스 토큰입니다.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("이미 블랙리스트에 있는 액세스 토큰입니다.");
        }

        if (!authService.isTokenValid(accessToken)) {
            log.warn("유효하지 않은 액세스 토큰: {}", accessToken);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("유효하지 않은 토큰입니다.");
        }

        String socialId = SecurityContextHolder.getContext().getAuthentication().getName();
        MemberLogoutResponse memberLogoutResponse = authService.logout(socialId);
        if (memberLogoutResponse == null) {
            log.warn("Social Id {}로 회원을 찾을 수 없음", principal.getName());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Social Id로 회원을 찾을 수 없습니다.");
        }

        try {
            String refreshToken = memberLogoutResponse.getRefreshToken();

            jwtBlacklistService.addTokenToBlacklist(accessToken);
            if (refreshToken != null) {
                jwtBlacklistService.addTokenToBlacklist(refreshToken);
            }
            SecurityContextHolder.clearContext();
            log.info("로그아웃 성공");
            return ResponseEntity.ok("로그아웃에 성공하였습니다.");
        } catch (Exception e) {
            log.error("로그아웃 실패: 사용자 {}", principal.getName(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("로그아웃 실패하였습니다.");
        }
    }
    @Operation(
            summary = "회원탈퇴",
            description = "회원 정보를 삭제하고, 관련된 리프레시 토큰을 무효화합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원탈퇴 성공"),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 토큰"),
            @ApiResponse(responseCode = "500", description = "회원탈퇴 중 서버 오류 발생")
    })
    @DeleteMapping("/member/delete")
    public ResponseEntity<?> deleteMember() {
        try {
            authService.deleteMember();
            SecurityContextHolder.clearContext();
            return ResponseEntity.ok().body("회원탈퇴가 완료되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원탈퇴 중 오류가 발생했습니다.");
        }
    }
}
