/*
 * Copyright 2025 Signal Messenger, LLC
 * SPDX-License-Identifier: AGPL-3.0-only
 */

package org.whispersystems.signalservice.internal.push.http;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

public class TrustAllCerts implements X509TrustManager {


  @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

  }

  @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s) throws CertificateException {

  }

  @Override public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[]{};
  }
}
