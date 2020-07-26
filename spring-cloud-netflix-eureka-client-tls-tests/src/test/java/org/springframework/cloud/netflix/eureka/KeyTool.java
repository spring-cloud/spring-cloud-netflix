/*
 * Copyright 2018-2019 the original author or authors.
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

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class KeyTool {

	private static final long ONE_DAY = 1000L * 60L * 60L * 24L;

	private static final long TEN_YEARS = ONE_DAY * 365L * 10L;

	public KeyAndCert createCA(String ca) throws Exception {
		KeyPair keyPair = createKeyPair();
		X509Certificate certificate = createCert(keyPair, ca);
		return new KeyAndCert(keyPair, certificate);
	}

	public KeyAndCert signCertificate(String subject, KeyAndCert signer)
			throws Exception {
		return signCertificate(createKeyPair(), subject, signer);
	}

	public KeyAndCert signCertificate(KeyPair keyPair, String subject, KeyAndCert signer)
			throws Exception {
		X509Certificate certificate = createCert(keyPair.getPublic(), signer.privateKey(),
				signer.subject(), subject);
		KeyAndCert result = new KeyAndCert(keyPair, certificate);

		return result;
	}

	public KeyPair createKeyPair() throws Exception {
		return createKeyPair(1024);
	}

	public KeyPair createKeyPair(int keySize) throws Exception {
		KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
		gen.initialize(keySize, new SecureRandom());
		return gen.generateKeyPair();
	}

	public X509Certificate createCert(KeyPair keyPair, String ca) throws Exception {
		JcaX509v3CertificateBuilder builder = certBuilder(keyPair.getPublic(), ca, ca);
		builder.addExtension(Extension.keyUsage, true,
				new KeyUsage(KeyUsage.keyCertSign));
		builder.addExtension(Extension.basicConstraints, false,
				new BasicConstraints(true));

		return signCert(builder, keyPair.getPrivate());
	}

	public X509Certificate createCert(PublicKey publicKey, PrivateKey privateKey,
			String issuer, String subject) throws Exception {
		JcaX509v3CertificateBuilder builder = certBuilder(publicKey, issuer, subject);
		builder.addExtension(Extension.keyUsage, true,
				new KeyUsage(KeyUsage.digitalSignature));
		builder.addExtension(Extension.basicConstraints, false,
				new BasicConstraints(false));

		GeneralName[] names = new GeneralName[] {
				new GeneralName(GeneralName.dNSName, "localhost") };
		builder.addExtension(Extension.subjectAlternativeName, false,
				GeneralNames.getInstance(new DERSequence(names)));

		return signCert(builder, privateKey);
	}

	private JcaX509v3CertificateBuilder certBuilder(PublicKey publicKey, String issuer,
			String subject) {
		X500Name issuerName = new X500Name(String.format("dc=%s", issuer));
		X500Name subjectName = new X500Name(String.format("dc=%s", subject));

		long now = System.currentTimeMillis();
		BigInteger serialNum = BigInteger.valueOf(now);
		Date notBefore = new Date(now - ONE_DAY);
		Date notAfter = new Date(now + TEN_YEARS);

		return new JcaX509v3CertificateBuilder(issuerName, serialNum, notBefore, notAfter,
				subjectName, publicKey);
	}

	private X509Certificate signCert(JcaX509v3CertificateBuilder builder,
			PrivateKey privateKey) throws Exception {
		ContentSigner signer = new JcaContentSignerBuilder("SHA256WithRSA")
				.build(privateKey);
		X509CertificateHolder holder = builder.build(signer);

		return new JcaX509CertificateConverter().getCertificate(holder);
	}

}
