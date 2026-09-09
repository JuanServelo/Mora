package comunicacao.security;

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
}
