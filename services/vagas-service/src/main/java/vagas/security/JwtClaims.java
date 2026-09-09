package vagas.security;

public record JwtClaims(String authUserId, String email, String perfil, String condominioId) {}
