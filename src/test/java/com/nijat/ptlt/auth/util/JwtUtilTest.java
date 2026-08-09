package com.nijat.ptlt.auth.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET = "unit-test-signing-key-that-is-long-enough-32";
    private static final long ACCESS_VALIDITY = 900_000L;
    private static final long REFRESH_VALIDITY = 604_800_000L;

    private static JwtUtil jwtUtil() {
        return new JwtUtil(SECRET, ACCESS_VALIDITY, REFRESH_VALIDITY);
    }

    // --- secret guard -------------------------------------------------------

    @Test
    void rejectsBlankSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil("", ACCESS_VALIDITY, REFRESH_VALIDITY));
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null, ACCESS_VALIDITY, REFRESH_VALIDITY));
    }

    @Test
    void rejectsShortSecret() {
        assertThrows(
                IllegalStateException.class,
                () -> new JwtUtil("too-short", ACCESS_VALIDITY, REFRESH_VALIDITY));
    }

    /** The exact value that used to ship in application.yml. */
    @Test
    void rejectsTheCommittedPlaceholderSecret() {
        IllegalStateException error =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                new JwtUtil(
                                        "your-256-bit-secret-key-change-in-production-minimum-32-characters",
                                        ACCESS_VALIDITY,
                                        REFRESH_VALIDITY));

        assertTrue(error.getMessage().contains("placeholder"), error.getMessage());
    }

    @Test
    void acceptsAStrongCustomSecret() {
        assertDoesNotThrow(JwtUtilTest::jwtUtil);
    }

    // --- token type separation ---------------------------------------------

    @Test
    void accessTokenValidatesAsAccessToken() {
        JwtUtil util = jwtUtil();
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();

        String token = util.generateAccessToken(userId, workspaceId);

        assertTrue(util.validateAccessToken(token));
        assertEquals(userId, util.getUserIdFromToken(token));
        assertEquals(workspaceId, util.getWorkspaceIdFromToken(token));
    }

    /**
     * The core of the fix. validateToken previously checked only the signature and expiry, so a
     * refresh token authenticated every endpoint that needs a userId — including
     * DELETE /api/v1/workspaces/{id} — and logout provided no protection at all.
     */
    @Test
    void refreshTokenIsRejectedAsAccessToken() {
        JwtUtil util = jwtUtil();

        String refreshToken = util.generateRefreshToken(UUID.randomUUID());

        assertFalse(util.validateAccessToken(refreshToken));
    }

    @Test
    void accessTokenIsRejectedAsRefreshToken() {
        JwtUtil util = jwtUtil();

        String accessToken = util.generateAccessToken(UUID.randomUUID(), UUID.randomUUID());

        assertFalse(util.validateRefreshToken(accessToken));
    }

    @Test
    void refreshTokenValidatesAsRefreshToken() {
        JwtUtil util = jwtUtil();
        UUID userId = UUID.randomUUID();

        String refreshToken = util.generateRefreshToken(userId);

        assertTrue(util.validateRefreshToken(refreshToken));
        assertEquals(userId, util.getUserIdFromToken(refreshToken));
    }

    // --- signature isolation ------------------------------------------------

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtUtil issuer = new JwtUtil("a-completely-different-signing-key-32-chars", ACCESS_VALIDITY, REFRESH_VALIDITY);
        JwtUtil verifier = jwtUtil();

        String token = issuer.generateAccessToken(UUID.randomUUID(), UUID.randomUUID());

        assertFalse(verifier.validateAccessToken(token));
    }

    @Test
    void garbageTokenIsRejectedWithoutThrowing() {
        JwtUtil util = jwtUtil();

        assertFalse(util.validateAccessToken("not-a-jwt"));
        assertFalse(util.validateRefreshToken("not-a-jwt"));
        assertFalse(util.validateAccessToken(""));
    }

    @Test
    void expiredAccessTokenIsRejected() throws Exception {
        JwtUtil shortLived = new JwtUtil(SECRET, 1L, REFRESH_VALIDITY);

        String token = shortLived.generateAccessToken(UUID.randomUUID(), UUID.randomUUID());
        Thread.sleep(50);

        assertFalse(shortLived.validateAccessToken(token));
    }
}
