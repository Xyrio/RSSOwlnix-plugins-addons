/*   **********************************************************************  **
 **   Copyright notice                                                       **
 **                                                                          **
 **   (c) 2005-2009 RSSOwl Development Team                                  **
 **   http://www.rssowl.org/                                                 **
 **                                                                          **
 **   All rights reserved                                                    **
 **                                                                          **
 **   This program and the accompanying materials are made available under   **
 **   the terms of the Eclipse Public License v1.0 which accompanies this    **
 **   distribution, and is available at:                                     **
 **   http://www.rssowl.org/legal/epl-v10.html                               **
 **                                                                          **
 **   A copy is found in the file epl-v10.html and important notices to the  **
 **   license from the team is found in the textfile LICENSE.txt distributed **
 **   in this package.                                                       **
 **                                                                          **
 **   This copyright notice MUST APPEAR in all copies of the file!           **
 **                                                                          **
 **   Contributors:                                                          **
 **     RSSOwl Development Team - initial API and implementation             **
 **                                                                          **
 **  **********************************************************************  */

package org.rssowl.contrib.internal.nntp;

import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.IProxyCredentials;
import org.rssowl.core.connection.PlatformCredentialsProvider;
import org.rssowl.core.util.URIUtils;

import java.net.URI;

/**
 * Subclass of <code>DefaultCredentialsProvider</code> normalizing any
 * <code>URI</code> to its scheme and host. Avoids having to add credentials
 * for any group of the server that the user wants to subscribe.
 * <p>
 * TODO The delete-methods are not implemented, because deleting the normalized
 * link would mean that all subscriptions to other groups of the same server
 * would loose their credentials too.
 * </p>
 *
 * @author bpasero
 */
public class NewsGroupCredentialsProvider extends PlatformCredentialsProvider {

  /*
   * @see org.rssowl.core.connection.PlatformCredentialsProvider#deleteAuthCredentials(java.net.URI,
   * java.lang.String)
   */
  @Override
  public synchronized void deleteAuthCredentials(URI link, String realm) throws CredentialsException {
    super.deleteAuthCredentials(URIUtils.normalizeUri(link), realm);
  }

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#deleteProxyCredentials(java.net.URI)
   */
  @Override
  public void deleteProxyCredentials(URI link) {}

  /*
   * @see org.rssowl.core.connection.PlatformCredentialsProvider#getAuthCredentials(java.net.URI,
   * java.lang.String)
   */
  @Override
  public synchronized ICredentials getAuthCredentials(URI link, String realm) throws CredentialsException {
    return super.getAuthCredentials(URIUtils.normalizeUri(link), realm);
  }

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#getProxyCredentials(java.net.URI)
   */
  @Override
  public IProxyCredentials getProxyCredentials(URI link) {
    return super.getProxyCredentials(URIUtils.normalizeUri(link));
  }

  /*
   * @see org.rssowl.core.connection.PlatformCredentialsProvider#setAuthCredentials(org.rssowl.core.connection.ICredentials,
   * java.net.URI, java.lang.String)
   */
  @Override
  public void setAuthCredentials(ICredentials credentials, URI link, String realm) throws CredentialsException {
    super.setAuthCredentials(credentials, URIUtils.normalizeUri(link), realm);
  }

  /*
   * @see org.rssowl.core.connection.internal.DefaultCredentialsProvider#setProxyCredentials(org.rssowl.core.connection.auth.IProxyCredentials,
   * java.net.URI)
   */
  @Override
  public void setProxyCredentials(IProxyCredentials credentials, URI link) {
    super.setProxyCredentials(credentials, URIUtils.normalizeUri(link));
  }
}