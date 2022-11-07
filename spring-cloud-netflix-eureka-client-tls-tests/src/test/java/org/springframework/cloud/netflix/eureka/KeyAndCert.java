/*
 * Copyright 2018-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.netflix.eureka;

import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

public class KeyAndCert {

	private final KeyPair keyPair;

	private final X509Certificate certificate;

	public KeyAndCert(KeyPair keyPair, X509Certificate certificate) {
		this.keyPair = keyPair;
		this.certificate = certificate;
	}

	public KeyPair keyPair() {
		return keyPair;
	}

	public PublicKey publicKey() {
		return keyPair.getPublic();
	}

	public PrivateKey privateKey() {
		return keyPair.getPrivate();
	}

	public X509Certificate certificate() {
		return certificate;
	}

	public String subject() {
		String dn = certificate.getSubjectX500Principal().getName();
		int index = dn.indexOf('=');
		return dn.substring(index + 1);
	}

	public KeyAndCert sign(String subject) throws Exception {
		KeyTool tool = new KeyTool();
		return tool.signCertificate(subject, this);
	}

	public KeyAndCert sign(KeyPair keyPair, String subject) throws Exception {
		KeyTool tool = new KeyTool();
		return tool.signCertificate(keyPair, subject, this);
	}

	public KeyStore storeKeyAndCert(String keyPassword) throws Exception {
		KeyStore result = KeyStore.getInstance("PKCS12");
		result.load(null);

		result.setKeyEntry(subject(), keyPair.getPrivate(), keyPassword.toCharArray(), certChain());
		return result;
	}

	private Certificate[] certChain() {
		return new Certificate[] { certificate() };
	}

	public KeyStore storeCert() throws Exception {
		return storeCert("PKCS12");
	}

	public KeyStore storeCert(String storeType) throws Exception {
		KeyStore result = KeyStore.getInstance(storeType);
		result.load(null);

		result.setCertificateEntry(subject(), certificate());
		return result;
	}

}
