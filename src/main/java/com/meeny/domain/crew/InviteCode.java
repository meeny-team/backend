package com.meeny.domain.crew;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.security.SecureRandom;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InviteCode {

    private static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int LENGTH = 6;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Column(name = "invite_code", nullable = false, unique = true, length = LENGTH)
    private String value;

    private InviteCode(String value) {
        this.value = value;
    }

    public static InviteCode generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
        }
        return new InviteCode(sb.toString());
    }

    public static InviteCode of(String value) {
        return new InviteCode(value);
    }
}
