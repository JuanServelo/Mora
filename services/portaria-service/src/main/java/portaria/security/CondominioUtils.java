package portaria.security;

import portaria.exception.AcessoNegadoException;

/**
 * Extrai o condominioId efetivo do JWT armazenado no AuthContext.
 *
 * ADMIN_GERAL é identificado pelo condominioId "default" ou null — nesses casos
 * este método retorna null para indicar "sem filtro" (acesso a todos).
 * Para todos os outros perfis, retorna o condominioId da claim, garantindo isolamento.
 */
public final class CondominioUtils {

    private static final String ADMIN_GERAL_CONDOMINIO = "default";

    private CondominioUtils() {}

    public static String condominioIdEfetivo() {
        JwtClaims claims = AuthContext.get();
        if (claims == null) return null;
        String cid = claims.condominioId();
        if (cid == null || ADMIN_GERAL_CONDOMINIO.equals(cid)) return null;
        return cid;
    }

    /**
     * Resolve o condominioId efetivo considerando o query param da requisição.
     * - Usuário escopado: usa sempre seu próprio condominioId; lança 403 se o param for diferente.
     * - ADMIN_GERAL: usa o param se fornecido, senão null (sem filtro).
     */
    public static String resolverEscopo(String condominioIdParam) {
        JwtClaims claims = AuthContext.get();
        if (claims == null) return condominioIdParam;
        String cid = claims.condominioId();
        if (cid == null || ADMIN_GERAL_CONDOMINIO.equals(cid)) {
            return condominioIdParam;
        }
        if (condominioIdParam != null && !condominioIdParam.equals(cid)) {
            throw new AcessoNegadoException("Acesso negado ao condomínio especificado.");
        }
        return cid;
    }
}
