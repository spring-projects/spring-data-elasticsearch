/*
 * Copyright 2013 the original author or authors.
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
    private Client transportClientWithoutShield;
    @Autowired
    private Client transportClientWithAllShieldProperties;
    @Autowired
    private Client transportClientWithShieldUserAndSslNoKeystore;
    @Autowired
    private Client transportClientWithShieldUserOnlyNoSslNoKeystore;
    @Autowired
    private Client transportClientWithEmptyShieldProperties;

    @Test
    public void shouldContainsNoShieldProperties() {
        assertThat(transportClientWithoutShield.settings().get("shield.user"), is(nullValue()));
        assertThat(transportClientWithoutShield.settings().get("shield.transport.ssl"), is(nullValue()));
        assertThat(transportClientWithoutShield.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(transportClientWithoutShield.settings().get("shield.ssl.keystore.password"), is(nullValue()));
    }

    @Test
    public void shouldContainAllShieldUserAndSslProperties() {
        assertThat(transportClientWithAllShieldProperties.settings().get("shield.user"), is("shieldUserTest:shieldUserPassword"));
        assertThat(transportClientWithAllShieldProperties.settings().get("shield.transport.ssl"), is(is("true")));
        assertThat(transportClientWithAllShieldProperties.settings().get("shield.ssl.keystore.path"), is("/dev/null/keystorePath"));
        assertThat(transportClientWithAllShieldProperties.settings().get("shield.ssl.keystore.password"), is("keystorePassword"));
    }

    @Test
    public void shouldContainSslShieldUserAndSslEnabled_withoutKeystoreProperty() {
        assertThat(transportClientWithShieldUserAndSslNoKeystore.settings().get("shield.user"), is("shieldUserTest:shieldUserPassword"));
        assertThat(transportClientWithShieldUserAndSslNoKeystore.settings().get("shield.transport.ssl"), is("true"));
        assertThat(transportClientWithShieldUserAndSslNoKeystore.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(transportClientWithShieldUserAndSslNoKeystore.settings().get("shield.ssl.keystore.password"), is(nullValue()));
    }

    @Test
    public void shouldContainOnlySslShieldUser_noSslEnabled_noKeystore() {
        assertThat(transportClientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.user"), is("shieldUserTest:shieldUserPassword"));
        assertThat(transportClientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.transport.ssl"), is(nullValue()));
        assertThat(transportClientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(transportClientWithShieldUserOnlyNoSslNoKeystore.settings().get("shield.ssl.keystore.password"), is(nullValue()));
    }

    @Test
    public void noShieldPropertiesWhen_shieldUserEmpty_sslEnabledEmpty_keystoreEmpty() {
        assertThat(transportClientWithEmptyShieldProperties.settings().get("shield.user"), is(nullValue()));
        assertThat(transportClientWithEmptyShieldProperties.settings().get("shield.transport.ssl"), is(nullValue()));
        assertThat(transportClientWithEmptyShieldProperties.settings().get("shield.ssl.keystore.path"), is(nullValue()));
        assertThat(transportClientWithEmptyShieldProperties.settings().get("shield.ssl.keystore.password"), is(nullValue()));
    }

}
