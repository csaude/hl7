package mz.org.fgh.hl7.lib.service;

import static mz.org.fgh.hl7.lib.Constants.C_SAUDE_SECRET_KEY_ALIAS;
import static mz.org.fgh.hl7.lib.Constants.DISA_SECRET_KEY_ALIAS;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class HL7KeyStoreServiceTest {

    private HL7KeyStoreService hl7KeyStoreService;

    public HL7KeyStoreServiceTest() {
        hl7KeyStoreService = new HL7KeyStoreService("src/test/resources/keyStore.jceks", "password");
    }

    @Test
    public void shouldReturnEntries() throws IOException {
        Map<String, byte[]> entries = hl7KeyStoreService.getEntries();
        assertThat(new String(entries.get(C_SAUDE_SECRET_KEY_ALIAS))).isEqualTo("cSaudeSecretKey");
        assertThat(new String(entries.get(DISA_SECRET_KEY_ALIAS))).isEqualTo("disaSecretKey");
    }
}
