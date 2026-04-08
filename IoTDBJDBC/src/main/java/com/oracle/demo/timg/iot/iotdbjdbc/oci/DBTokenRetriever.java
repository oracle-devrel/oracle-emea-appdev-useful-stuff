/*Copyright (c) 2026 Oracle and/or its affiliates.

The Universal Permissive License (UPL), Version 1.0

Subject to the condition set forth below, permission is hereby granted to any
person obtaining a copy of this software, associated documentation and/or data
(collectively the "Software"), free of charge and under any and all copyright
rights in the Software, and any and all patent rights owned or freely
licensable by each licensor hereunder covering either (i) the unmodified
Software as contributed to or provided by such licensor, or (ii) the Larger
Works (as defined below), to deal in both

(a) the Software, and
(b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
one is included with the Software (each a "Larger Work" to which the Software
is contributed by such licensors),

without restriction, including without limitation the rights to copy, create
derivative works of, display, perform, and distribute the Software and make,
use, sell, offer for sale, import, export, have made, and have sold the
Software and the Larger Work(s), and to sublicense the foregoing rights on
either these or other terms.

This license is subject to the following condition:
The above copyright notice and either this complete permission notice or at
a minimum a reference to the UPL must be included in all copies or
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

package com.oracle.demo.timg.iot.iotdbjdbc.oci;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import com.oracle.bmc.Region;
import com.oracle.bmc.auth.BasicAuthenticationDetailsProvider;
import com.oracle.bmc.auth.RegionProvider;
import com.oracle.bmc.identitydataplane.DataplaneClient;
import com.oracle.bmc.identitydataplane.model.GenerateScopedAccessTokenDetails;
import com.oracle.bmc.identitydataplane.requests.GenerateScopedAccessTokenRequest;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import oracle.jdbc.AccessToken;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import lombok.extern.java.Log;

@Singleton
@Log
/**
 * This class is based on the IamDbTokenProvider by Philippe Vanhaesendonck but
 * using micronaut dependency injection
 */
@Requires(property = "oci.dbtoken.scope")
public class DBTokenRetriever {

	private final BasicAuthenticationDetailsProvider authProvider;
	private final String scope;
	private final Region region ;

	@Inject
	public DBTokenRetriever(OCIAuthProvider ociAuthProvider, @Property(name = "oci.dbtoken.scope") String scope) {
		if (!(authProvider instanceof RegionProvider regionProvider)) {
			throw new IllegalArgumentException("OCI auth provider must implement RegionProvider");
		}

		region = regionProvider.getRegion();
		if (region == null) {
			throw new IllegalArgumentException("OCI auth provider does not expose a region");
		}
		this.authProvider=ociAuthProvider.getAuthProvider();
		this.scope=scope;
	}
	
	public AccessToken generateAccessToken() {
		 KeyPair keyPair = generateKeyPair();
	        String publicKeyPem = toPublicKeyPem(keyPair);

	        try (DataplaneClient dataplaneClient = DataplaneClient.builder().build(authProvider)) {
	            dataplaneClient.setRegion(region);
	            GenerateScopedAccessTokenRequest request = GenerateScopedAccessTokenRequest.builder()
	                    .generateScopedAccessTokenDetails(
	                            GenerateScopedAccessTokenDetails.builder()
	                                    .scope(scope)
	                                    .publicKey(publicKeyPem)
	                                    .build())
	                    .build();
	            String token = dataplaneClient.generateScopedAccessToken(request).getSecurityToken().getToken();
	            return AccessToken.createJsonWebToken(token.toCharArray(), keyPair.getPrivate());
	        }
	}

	private KeyPair generateKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Failed to generate temporary RSA key pair", exception);
		}
	}

	private String toPublicKeyPem(KeyPair keyPair) {
		return toPem("PUBLIC KEY", keyPair.getPublic().getEncoded());
	}

	static String toPem(String label, byte[] encoded) {
	        Base64.Encoder encoder = Base64.getMimeEncoder(64, new byte[]{'\n'});
	        return "-----BEGIN " + label + "-----\n"
	                + encoder.encodeToString(encoded)
	                + "\n-----END " + label + "-----";
	    }
}

}
