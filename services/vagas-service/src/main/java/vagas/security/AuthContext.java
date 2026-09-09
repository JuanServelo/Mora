package vagas.security;

public class AuthContext {

    private static final ThreadLocal<JwtClaims> holder = new ThreadLocal<>();

    public static void set(JwtClaims claims) { holder.set(claims); }
    public static JwtClaims get() { return holder.get(); }
    public static void clear() { holder.remove(); }

    private AuthContext() {}
}
