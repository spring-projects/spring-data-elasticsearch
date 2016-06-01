/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.elasticsearch.config;

import org.elasticsearch.client.Client;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Gioele Ashman
 */

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("shield-namespace.xml")
public class ShieldElasticsearchNamespaceHandlerTests {

    @Autowired
    private Client clientWithoutShield;
    @Autowired
    private Client clientWithAllShieldPropertiesExceptTruststore;
    @Autowired
    private Client clientWithShieldUserAndSslNoKeystore;
    @Autowired
    private Client clientWithShieldUserOnlyNoSslNoKeystore;
    @Autowired
    private Client clientWithEmptyShieldProperties;
    @Autowired
    private Client clientWithAllShieldPropertiesExceptKeystore;

    @Test
    public void shouldContainsNoShieldProperties() {
        assertThat(clientWithoutShield.settings().get("shield.user"), is(nullValue()));
        assertThat(clientWithoutShield.settings().get("shield.transport.ssl"), is(nullValue()));
        assertThat(clientWithoutShield.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(clientWithoutShield.settings().get("shield.ssl.keystore.password"), is(nullValue()));
        assertThat(clientWithoutShield.settings().get("shield.ssl.truststore.path"), is(nullValue()));
        assertThat(clientWithoutShield.settings().get("shield.ssl.truststore.password"), is(nullValue()));
    }

    @Test
    public void shouldContainShieldUserAndSslPropsWithKeystoreForSslWithClientCertAuth() {
        assertThat(clientWithAllShieldPropertiesExceptTruststore.settings().get("shield.user"), is("shieldUserTest:shieldUserPassword"));
        assertThat(clientWithAllShieldPropertiesExceptTruststore.settings().get("shield.transport.ssl"), is(is("true")));
        assertThat(clientWithAllShieldPropertiesExceptTruststore.settings().get("shield.ssl.keystore.path"), is("/dev/null/keystorePath"));
        assertThat(clientWithAllShieldPropertiesExceptTruststore.settings().get("shield.ssl.keystore.password"), is("keystorePassword"));
        assertThat(clientWithAllShieldPropertiesExceptTruststore.settings().get("shield.ssl.truststore.path"), is(nullValue()));
        assertThat(clientWithAllShieldPropertiesExceptTruststore.settings().get("shield.ssl.truststore.password"), is(nullValue()));
    }

    @Test
    public void shouldContainAllShieldPropsWithTruststoreForSslWithoutClientCertAuth() {
        assertThat(clientWithAllShieldPropertiesExceptKeystore.settings().get("shield.user"), is("shieldUserTest:shieldUserPassword"));
        assertThat(clientWithAllShieldPropertiesExceptKeystore.settings().get("shield.transport.ssl"), is(is("true")));
        assertThat(clientWithAllShieldPropertiesExceptKeystore.settings().get("shield.ssl.truststore.path"), is("/truststore/Path"));
        assertThat(clientWithAllShieldPropertiesExceptKeystore.settings().get("shield.ssl.truststore.password"), is("truststorePassword"));
        assertThat(clientWithAllShieldPropertiesExceptKeystore.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(clientWithAllShieldPropertiesExceptKeystore.settings().get("shield.ssl.keystore.password"), is(nullValue()));
    }

    @Test
    public void shouldContainSslShieldUserAndSslEnabled_() {
        assertThat(clientWithShieldUserAndSslNoKeystore.settings().get("shield.user"), is("shieldUserTest:shieldUserPassword"));
        assertThat(clientWithShieldUserAndSslNoKeystore.settings().get("shield.transport.ssl"), is("true"));
        assertThat(clientWithShieldUserAndSslNoKeystore.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(clientWithShieldUserAndSslNoKeystore.settings().get("shield.ssl.keystore.password"), is(nullValue()));
        assertThat(clientWithShieldUserAndSslNoKeystore.settings().get("shield.ssl.truststore.path"), is(nullValue()));
        assertThat(clientWithShieldUserAndSslNoKeystore.settings().get("shield.ssl.truststore.password"), is(nullValue()));
    }

    @Test
    public void shouldContainOnlySslShieldUser_noSslEnabled_noKeystore_noTruststore() {
        assertThat(clientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.user"), is("shieldUserTest:shieldUserPassword"));
        assertThat(clientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.transport.ssl"), is(nullValue()));
        assertThat(clientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(clientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.ssl.keystore.password"), is(nullValue()));
        assertThat(clientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.ssl.truststore.path"), is(nullValue()));
        assertThat(clientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.ssl.truststore.password"), is(nullValue()));
        assertThat(clientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.ssl.truststore.password"), is(nullValue()));
    }

    @Test
    public void noShieldPropertiesWhen_shieldUserEmpty_sslEnabledEmpty_keystoreEmpty_truststoreEmpty() {
        assertThat(clientWithEmptyShieldProperties.settings().get("shield.user"), is(nullValue()));
        assertThat(clientWithEmptyShieldProperties.settings().get("shield.transport.ssl"), is(nullValue()));
        assertThat(clientWithEmptyShieldProperties.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(clientWithEmptyShieldProperties.settings().get("shield.ssl.keystore.password"), is(nullValue()));
        assertThat(clientWithEmptyShieldProperties.settings().get("shield.ssl.truststore.path"), is(nullValue()));
        assertThat(clientWithEmptyShieldProperties.settings().get("shield.ssl.truststore.password"), is(nullValue()));
    }
}
