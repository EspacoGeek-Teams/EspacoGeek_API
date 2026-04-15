package com.espacogeek.geek.config;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.lang.NonNull;

public class JwtRuntimeHints implements RuntimeHintsRegistrar {

    /**
     * Registers reflection hints and service resource patterns required by JJWT
     * at runtime in a GraalVM native image.
     *
     * <p>Tested against {@code io.jsonwebtoken:jjwt-impl:0.13.0} and
     * {@code io.jsonwebtoken:jjwt-jackson:0.13.0}.  Both artifacts are declared
     * {@code runtimeOnly} in build.gradle, so all JJWT impl classes must be
     * referenced by name via {@link TypeReference#of(String)}.
     */

    private static final MemberCategory[] ALL_MEMBER_CATEGORIES = MemberCategory.values();

    @Override
    public void registerHints(@NonNull RuntimeHints hints, ClassLoader classLoader) {
        hints.reflection()
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwtBuilder"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwtBuilder$Supplier"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwtParserBuilder"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwtParserBuilder$Supplier"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwtParser"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultClaims"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultHeader"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJws"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwt"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwe"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultUnprotectedHeader"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJweHeader"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultJwsHeader"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.DefaultMutableJweHeader"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.compression.DeflateCompressionAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.compression.GzipCompressionAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.lang.Services"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.DefaultAeadAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.DefaultHashAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.DefaultKeyAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.DefaultRsaKeyAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.DefaultSignatureAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.EcSignatureAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.HmacAesAesKwAlgorithm"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.impl.security.DefaultEllipticCurve"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.jackson.io.JacksonSerializer"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.jackson.io.JacksonSerializer$Supplier"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.jackson.io.JacksonDeserializer"), ALL_MEMBER_CATEGORIES)
                .registerType(TypeReference.of("io.jsonwebtoken.jackson.io.JacksonDeserializer$Supplier"), ALL_MEMBER_CATEGORIES);

        hints.resources()
                .registerPattern("META-INF/services/io.jsonwebtoken.io.Serializer")
                .registerPattern("META-INF/services/io.jsonwebtoken.io.Deserializer")
                .registerPattern("META-INF/services/io.jsonwebtoken.JwtBuilder")
                .registerPattern("META-INF/services/io.jsonwebtoken.JwtParserBuilder")
                .registerPattern("META-INF/services/io.jsonwebtoken.impl.lang.Services$ServiceLoaderTemplate");
    }
}
