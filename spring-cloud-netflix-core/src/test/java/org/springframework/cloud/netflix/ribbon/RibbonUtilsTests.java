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
 *
 */

package org.springframework.cloud.netflix.ribbon;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.loadbalancer.Server;

import static org.hamcrest.Matchers.is;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.isSecure;
import static org.springframework.cloud.netflix.ribbon.RibbonUtils.updateToHttpsIfNeeded;

/**
 * @author Spencer Gibb
 * @author Jacques-Etienne Beaudet
 */
public class RibbonUtilsTests {

	private static final ServerIntrospector NON_SECURE_INTROSPECTOR = new StaticServerIntrospector(false);
	private static final ServerIntrospector SECURE_INTROSPECTOR = new StaticServerIntrospector(true);
	private static final Server SERVER = new Server("localhost", 8080);
	private static final DefaultClientConfigImpl SECURE_CONFIG = getConfig(true);
	private static final DefaultClientConfigImpl NON_SECURE_CONFIG = getConfig(false);
	private static final DefaultClientConfigImpl NO_IS_SECURE_CONFIG = new DefaultClientConfigImpl();

	@Test
	public void noRibbonPropSecureIntrospector() {
		boolean secure = isSecure(NO_IS_SECURE_CONFIG, SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("isSecure was wrong", secure, is(true));
	}

	@Test
	public void noRibbonPropNonSecureIntrospector() {
		boolean secure = isSecure(NO_IS_SECURE_CONFIG, NON_SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("isSecure was wrong", secure, is(false));
	}

	@Test
	public void isSecureRibbonPropSecureIntrospector() {
		boolean secure = isSecure(SECURE_CONFIG, SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("isSecure was wrong", secure, is(true));
	}

	@Test
	public void nonSecureRibbonPropNonSecureIntrospector() {
		boolean secure = isSecure(NON_SECURE_CONFIG, NON_SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("isSecure was wrong", secure, is(false));
	}

	@Test
	public void isSecureRibbonPropNonSecureIntrospector() {
		boolean secure = isSecure(SECURE_CONFIG, NON_SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("isSecure was wrong", secure, is(true));
	}

	@Test
	public void nonSecureRibbonPropSecureIntrospector() {
		boolean secure = isSecure(NON_SECURE_CONFIG, SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("isSecure was wrong", secure, is(false));
	}

	@Test
	public void uriIsNotChangedWhenServerIsNotSecured() throws URISyntaxException {
		URI original = new URI("http://foo");
		URI updated = updateToHttpsIfNeeded(original, NON_SECURE_CONFIG, NON_SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("URI should not have been updated since server is not secured.", original, is(updated));
	}

	@Test
	public void uriIsNotChangedWhenServerIsSecuredAndUriAlreadyInHttps() throws URISyntaxException {
		URI original = new URI("https://foo");
		URI updated = updateToHttpsIfNeeded(original, SECURE_CONFIG, SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("URI should not have been updated since uri is already in https.", original, is(updated));
	}

	@Test
	public void shouldUpgradeUriToHttpsWhenServerIsSecureAndUriNotInHttps() throws URISyntaxException {
		URI original = new URI("http://foo");
		URI updated = updateToHttpsIfNeeded(original, SECURE_CONFIG, SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("URI should have been updated to https.", updated, is(new URI("https://foo")));
	}

	@Test
	public void shouldSubstitutePlusInQueryParam() throws URISyntaxException {
		URI original = new URI("http://foo/%20bar?hello=1+2");
		URI updated = updateToHttpsIfNeeded(original, SECURE_CONFIG, SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("URI should have had its plus sign replaced in query string.", updated, is(new URI(
				"https://foo/%20bar?hello=1%202")));
	}

	@Test
	public void emptyStringUri() throws URISyntaxException {
		URI original = new URI("");
		URI updated = updateToHttpsIfNeeded(original, SECURE_CONFIG, SECURE_INTROSPECTOR, SERVER);
		Assert.assertThat("URI should be the emptry string", updated, is(new URI(
				"")));
	}

	static DefaultClientConfigImpl getConfig(boolean value) {
		DefaultClientConfigImpl config = new DefaultClientConfigImpl();
		config.setProperty(CommonClientConfigKey.IsSecure, value);
		return config;
	}

	static class StaticServerIntrospector implements ServerIntrospector {

		final boolean secure;

		public StaticServerIntrospector(boolean secure) {
			this.secure = secure;
		}

		@Override
		public boolean isSecure(Server server) {
			return this.secure;
		}

		@Override
		public Map<String, String> getMetadata(Server server) {
			return null;
		}
	}
}
