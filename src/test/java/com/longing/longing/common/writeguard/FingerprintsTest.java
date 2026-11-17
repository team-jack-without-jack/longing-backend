package com.longing.longing.common.writeguard;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FingerprintsTest {

    @Test
    void sha256HexMatchesKnownVectors() {
        assertThat(Fingerprints.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(Fingerprints.sha256Hex(""))
                .isEqualTo("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855");
    }

    @Test
    void hashesUtf8TextAndDistinguishesWhitespace() {
        assertThat(Fingerprints.sha256Hex("한글"))
                .isEqualTo("bd87f9bb68b67d2fa1cb82b6751820e946d5b1316d25d5fd96512fb4be44a2a8")
                .isNotEqualTo(Fingerprints.sha256Hex("한글 "));
    }
}
