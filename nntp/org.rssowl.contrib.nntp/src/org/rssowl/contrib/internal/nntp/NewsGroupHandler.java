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

import org.apache.commons.net.nntp.NNTPClient;
import org.apache.commons.net.nntp.NewsgroupInfo;
import org.apache.james.mime4j.AbstractContentHandler;
import org.apache.james.mime4j.BodyDescriptor;
import org.apache.james.mime4j.MimeStreamParser;
import org.apache.james.mime4j.decoder.Base64InputStream;
import org.apache.james.mime4j.decoder.DecoderUtil;
import org.apache.james.mime4j.decoder.QuotedPrintableInputStream;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.osgi.service.url.AbstractURLStreamHandlerService;
import org.osgi.service.url.URLStreamHandlerService;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.AuthenticationRequiredException;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.CredentialsException;
import org.rssowl.core.connection.IConnectionPropertyConstants;
import org.rssowl.core.connection.ICredentials;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.persist.IConditionalGet;
import org.rssowl.core.persist.IFeed;
import org.rssowl.core.persist.IModelFactory;
import org.rssowl.core.persist.INews;
import org.rssowl.core.persist.IPerson;
import org.rssowl.core.util.DateUtils;
import org.rssowl.core.util.RegExUtils;
import org.rssowl.core.util.StringUtils;
import org.rssowl.core.util.Triple;
import org.rssowl.core.util.URIUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>NewsGroupHandler</code> is capable of creating a <code>IFeed</code>
 * with News from a Newsgroup-Server.
 * <p>
 * TODO The performance of reading an article could be improved by a custom
 * <code>Reader</code> that would perform the String-Replacement while reading
 * bytes (including link-conversion to HTML anchors).
 * </p>
 *
 * @author bpasero
 */
public class NewsGroupHandler implements IProtocolHandler {

  /* The Default Connection Timeout */
  private static final int DEFAULT_CON_TIMEOUT = 30000;

  /* The number of news to download on a first reload */
  private static final int INITIAL_NEWS = 50;

  /* Protocol Separator for Links */
  private static final String PROTOCOL_SEPARATOR = "://"; //$NON-NLS-1$

  /* Some NNTP Protcol specific constants */
  private static final String MODE_READER = "mode reader"; //$NON-NLS-1$
  private static final String HEADER_MESSAGE_ID = "Message-ID: "; //$NON-NLS-1$
  private static final String HEADER_DATE = "Date: "; //$NON-NLS-1$
  private static final String HEADER_SUBJECT = "Subject: "; //$NON-NLS-1$
  private static final String HEADER_FROM = "From: "; //$NON-NLS-1$
  private static final String HEADER_REFERENCES = "References: "; //$NON-NLS-1$

  /* Some NNTP Status Values */
  private static final int NO_SUCH_ARTICLE_ERROR = 423;
  private static final int STATUS_ARTICLE_POINTER_OK = 223;
  private static final int STATUS_AUTH_REQUIRED = 502;
  private static final int STATUS_AUTH_REQUIRED_ALTERNATIVE = 480;

  /* Some Mime Types */
  private static final String MIME_TEXT = "text/"; //$NON-NLS-1$
  private static final String MIME_TEXT_HTML = "text/html"; //$NON-NLS-1$
  private static final String MIME_TEXT_PLAIN = "text/plain"; //$NON-NLS-1$

  /* The Default Encoding of mime4j */
  private static final String DEFAULT_ENCODING = "us-ascii"; //$NON-NLS-1$

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#reload(java.net.URI,
   * org.eclipse.core.runtime.IProgressMonitor, java.util.Map)
   */
  public Triple<IFeed, IConditionalGet, URI> reload(URI link, IProgressMonitor monitor, Map<Object, Object> properties) throws CoreException {
    IModelFactory factory = Owl.getModelFactory();

    /* Create a new empty feed from the existing one */
    IFeed feed = factory.createFeed(null, link);

    /* Retrieve last article pointer (If-None-Match header value) */
    Integer lastArticleId = getLastArticleId(properties);

    /* Create a new HttpClient */
    NNTPClient client = new NNTPClient();

    try {

      /* Support early cancellation */
      if (monitor.isCanceled())
        return null;

      /* Connect to Server */
      if (link.getPort() != -1)
        client.connect(link.getHost(), link.getPort());
      else
        client.connect(link.getHost());

      /* Set Timeout */
      setTimeout(client, properties);

      /* Support early cancellation */
      if (monitor.isCanceled())
        return null;

      /* Authentication if provided */
      setupAuthentication(link, client);

      /* Check Authentication Required */
      checkAuthenticationRequired(client, link);

      /* Support early cancellation */
      if (monitor.isCanceled())
        return null;

      /* Enable Reader Mode */
      client.sendCommand(MODE_READER);

      /* Support early cancellation */
      if (monitor.isCanceled())
        return null;

      /* Select Newsgroup */
      String newsgroup = link.getPath().replace("/", ""); //$NON-NLS-1$ //$NON-NLS-2$
      NewsgroupInfo groupInfo = new NewsgroupInfo();
      boolean selected = client.selectNewsgroup(newsgroup, groupInfo);

      /* Check Authentication Required */
      checkAuthenticationRequired(client, link);

      /* Check Newsgroup Selected */
      if (!selected)
        throwConnectionException(Messages.NewsGroupHandler_ERROR_SELECT_NEWSGROUP, client);

      /* Support early cancellation */
      if (monitor.isCanceled())
        return null;

      boolean downloadWithoutPointer = (lastArticleId == null); //Last article servers as pointer

      /* Subsequent reload: Set pointer to last retrieved News and go from there */
      if (lastArticleId != null) {

        /* Set Article Pointer to last retrieved News */
        int status = client.stat(lastArticleId);
        if (status == NO_SUCH_ARTICLE_ERROR)
          downloadWithoutPointer = true; //This can happen if the last article id was not found, grab the latest news then
        else if (status != STATUS_ARTICLE_POINTER_OK)
          throwConnectionException(Messages.NewsGroupHandler_ERROR_RETRIEVE_NEWS, client);

        /* Retrieve all the following News */
        if (!downloadWithoutPointer) {
          while (client.next() == STATUS_ARTICLE_POINTER_OK && !monitor.isCanceled())
            createNews(client.retrieveArticle(), feed, monitor);
        }
      }

      /* Retrieve the last 50 News of the group */
      if (downloadWithoutPointer) {

        /* Set Article Pointer to last Article */
        int status = client.stat(groupInfo.getLastArticle());
        if (status != STATUS_ARTICLE_POINTER_OK)
          throwConnectionException(Messages.NewsGroupHandler_ERROR_RETRIEVE_NEWS, client);

        /* Retrieve initial news */
        for (int i = 0; i < INITIAL_NEWS && !monitor.isCanceled(); i++) {
          createNews(client.retrieveArticle(), feed, monitor);

          /* Goto previous news if provided */
          int result = client.last();
          if (result != STATUS_ARTICLE_POINTER_OK)
            break;
        }
      }

      /* Remember last article's ID */
      lastArticleId = groupInfo.getLastArticle();
    }

    /* Wrap Exceptions */
    catch (IOException e) {
      throw new ConnectionException(Activator.createErrorStatus(e.getMessage(), e));
    }

    /* Disconnect */
    finally {
      try {
        if (client.isConnected())
          client.disconnect();
      } catch (IOException e) {
        throw new ConnectionException(Activator.createErrorStatus(e.getMessage(), e));
      }
    }

    /* Create Conditional Get Object */
    IConditionalGet conditionalGet = null;
    if (lastArticleId != null)
      conditionalGet = factory.createConditionalGet(null, link, String.valueOf(lastArticleId));

    return Triple.create(feed, conditionalGet, link);
  }

  private Integer getLastArticleId(Map<Object, Object> properties) {
    Object property = properties.get(IConnectionPropertyConstants.IF_NONE_MATCH);
    Integer lastArticleId = null;

    if (property instanceof String) {
      try {
        lastArticleId = Integer.parseInt((String) property);
      } catch (NumberFormatException e) {
        Activator.log(e);
      }
    }

    return lastArticleId;
  }

  private void createNews(Reader articleReader, IFeed feed, final IProgressMonitor monitor) throws IOException {

    /* Support early cancellation */
    if (monitor.isCanceled())
      return;

    /* In case Article is unavailable */
    if (articleReader == null)
      return;

    IModelFactory factory = Owl.getModelFactory();
    final INews news = factory.createNews(null, feed, new Date());
    final Map<String, StringBuilder> mimeToContent = new HashMap<String, StringBuilder>();

    /* Create parser for this message */
    final MimeStreamParser parser = new MimeStreamParser();
    parser.setContentHandler(new AbstractContentHandler() {
      boolean fBodyReached = false;

      @Override
      public void field(String fieldData) {

        /* Support early cancellation */
        if (monitor.isCanceled()) {
          parser.stop();
          return;
        }

        /* Not yet in Body */
        if (!fBodyReached) {

          /* From */
          if (fieldData.startsWith(HEADER_FROM))
            interpretFrom(news, DecoderUtil.decodeEncodedWords(fieldData.substring(HEADER_FROM.length())));

          /* Subject */
          else if (fieldData.startsWith(HEADER_SUBJECT))
            interpretSubject(news, DecoderUtil.decodeEncodedWords(fieldData.substring(HEADER_SUBJECT.length())));

          /* Date */
          else if (fieldData.startsWith(HEADER_DATE))
            interpretDate(news, DecoderUtil.decodeEncodedWords(fieldData.substring(HEADER_DATE.length())));

          /* Message ID */
          else if (fieldData.startsWith(HEADER_MESSAGE_ID))
            interpretMessageId(news, DecoderUtil.decodeEncodedWords(fieldData.substring(HEADER_MESSAGE_ID.length())));

          /* References */
          else if (fieldData.startsWith(HEADER_REFERENCES))
            interpretReferences(news, DecoderUtil.decodeEncodedWords(fieldData.substring(HEADER_REFERENCES.length())));
        }
      }

      @Override
      public void body(BodyDescriptor bd, InputStream is) throws IOException {

        /* Support early cancellation */
        if (monitor.isCanceled()) {
          parser.stop();
          return;
        }

        /* Require a mimetype */
        String mimeType = bd.getMimeType();
        if (mimeType == null)
          return;

        /* Require a text-mime */
        if (!mimeType.contains(MIME_TEXT))
          return;

        /* Assign StringBuilder with Mime-Type */
        StringBuilder strBuilder = mimeToContent.get(mimeType);
        if (strBuilder == null) {
          strBuilder = new StringBuilder();
          mimeToContent.put(mimeType, strBuilder);
        }

        /* Handle encodings */
        if (bd.isBase64Encoded())
          is = new Base64InputStream(is);
        else if (bd.isQuotedPrintableEncoded())
          is = new QuotedPrintableInputStream(is);

        /* Read Body */
        BufferedReader reader;
        if (!DEFAULT_ENCODING.equals(bd.getCharset()) && Charset.isSupported(bd.getCharset()))
          reader = new BufferedReader(new InputStreamReader(is, bd.getCharset()));
        else
          reader = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while ((line = reader.readLine()) != null && !monitor.isCanceled()) {

          /* Check for quote */
          boolean isQuote = line.startsWith(">"); //$NON-NLS-1$
          if (line.startsWith(">>>>")) //$NON-NLS-1$
            strBuilder.append("<span class=\"quote_lvl4\">"); //$NON-NLS-1$
          else if (line.startsWith(">>>")) //$NON-NLS-1$
            strBuilder.append("<span class=\"quote_lvl3\">"); //$NON-NLS-1$
          else if (line.startsWith(">>")) //$NON-NLS-1$
            strBuilder.append("<span class=\"quote_lvl2\">"); //$NON-NLS-1$
          else if (line.startsWith(">")) //$NON-NLS-1$
            strBuilder.append("<span class=\"quote_lvl1\">"); //$NON-NLS-1$

          /* Beautify Body (if non-html) */
          if (!MIME_TEXT_HTML.equals(mimeType))
            strBuilder.append(beautifyBody(line)).append("<br>\n"); //$NON-NLS-1$
          else
            strBuilder.append(line);

          /* Check for quote */
          if (isQuote)
            strBuilder.append("</span>"); //$NON-NLS-1$
        }
      }
    });

    /* Parse Body */
    ReaderInputStream inS = new ReaderInputStream(articleReader);
    parser.parse(inS);

    /* Prefer HTML over text/plain */
    if (mimeToContent.containsKey(MIME_TEXT_HTML))
      news.setDescription(mimeToContent.get(MIME_TEXT_HTML).toString());

    /* Use text/plain but replace links with HTML anchors */
    else if (mimeToContent.containsKey(MIME_TEXT_PLAIN)) {
      String description = mimeToContent.get(MIME_TEXT_PLAIN).toString();
      if (description.contains(PROTOCOL_SEPARATOR)) {
        List<String> links = RegExUtils.extractLinksFromText(description, true);
        for (String link : links) {
          StringBuilder strB = new StringBuilder("<a href=\""); //$NON-NLS-1$
          strB.append(link).append("\"/>"); //$NON-NLS-1$
          strB.append(link).append("</a>"); //$NON-NLS-1$

          description = StringUtils.replaceAll(description, link, strB.toString());
        }
      }

      news.setDescription(description);
    }
  }

  private String beautifyBody(String str) {
    str = StringUtils.replaceAll(str, "  ", "&nbsp;&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$
    str = StringUtils.replaceAll(str, "\t", "&nbsp;&nbsp;"); //$NON-NLS-1$ //$NON-NLS-2$
    str = StringUtils.replaceAll(str, "<", "&lt;"); //$NON-NLS-1$ //$NON-NLS-2$
    str = StringUtils.replaceAll(str, ">", "&gt;"); //$NON-NLS-1$ //$NON-NLS-2$

    return str;
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#getFeedIcon(java.net.URI,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  public byte[] getFeedIcon(URI link, IProgressMonitor monitor) {
    return loadFavicon(link, false, monitor);
  }

  private void interpretFrom(INews news, String value) {
    IPerson person = Owl.getModelFactory().createPerson(null, news);
    value = value.trim();

    /* Complex value */
    if (value.contains(" ")) { //$NON-NLS-1$

      /* Remove quotes first */
      value = value.replace("\"", ""); //$NON-NLS-1$//$NON-NLS-2$
      value = value.replace("'", ""); //$NON-NLS-1$ //$NON-NLS-2$

      /* foo@bar.com (Forename Name) */
      if (value.contains("(") && value.contains(")")) { //$NON-NLS-1$ //$NON-NLS-2$
        int start = value.indexOf('(');
        int end = value.indexOf(')');

        /* E-Mail */
        if (start > 0)
          person.setEmail(URIUtils.createURI(value.substring(0, start)));

        /* Name */
        if (start < end)
          person.setName(value.substring(start + 1, end).trim());
      }

      /* Forename Name <foo@bar.com> */
      if (value.contains("<") && value.contains(">")) { //$NON-NLS-1$ //$NON-NLS-2$
        int start = value.indexOf('<');
        int end = value.indexOf('>');

        /* Name */
        if (start > 0)
          person.setName(value.substring(0, start).trim());

        /* E-Mail */
        if (start < end)
          person.setEmail(URIUtils.createURI(value.substring(start + 1, end)));
      }
    }

    /* Simple Value (EMail) */
    else if (value.contains("@")) //$NON-NLS-1$
      person.setEmail(URIUtils.createURI(value));

    /* Simple Value (Name) */
    else
      person.setName(value);

    news.setAuthor(person);
  }

  private void interpretSubject(INews news, String value) {
    news.setTitle(value.trim());
  }

  private void interpretDate(INews news, String value) {
    news.setPublishDate(DateUtils.parseDate(value));
  }

  private void interpretMessageId(INews news, String value) {
    Owl.getModelFactory().createGuid(news, value.trim(), null);
    news.setInReplyTo(value);
  }

  private void interpretReferences(INews news, String value) {
    value = value.trim();
    if (StringUtils.isSet(value)) {

      /* Retrieve the first References-ID */
      int endOfFirstId = value.indexOf('>');

      if (endOfFirstId != -1 && endOfFirstId < value.length())
        news.setInReplyTo(value.substring(0, endOfFirstId + 1));
    }
  }

  private void setupAuthentication(URI link, NNTPClient client) throws CredentialsException, IOException {
    ICredentials authCredentials = Owl.getConnectionService().getAuthCredentials(link, null);
    if (authCredentials != null)
      client.authenticate(authCredentials.getUsername(), authCredentials.getPassword());
  }

  private void setTimeout(NNTPClient client, Map<Object, Object> properties) {

    /* Retrieve Connection Timeout from Properties if set */
    int conTimeout = DEFAULT_CON_TIMEOUT;
    if (properties != null && properties.containsKey(IConnectionPropertyConstants.CON_TIMEOUT))
      conTimeout = (Integer) properties.get(IConnectionPropertyConstants.CON_TIMEOUT);

    /* Socket Timeout - Max. time to wait for an answer */
    try {
      client.setSoTimeout(conTimeout);
    } catch (SocketException e) {
      /* Ignore */
    }
  }

  private void checkAuthenticationRequired(NNTPClient client, URI link) throws AuthenticationRequiredException {
    if (client.getReplyCode() == STATUS_AUTH_REQUIRED || client.getReplyCode() == STATUS_AUTH_REQUIRED_ALTERNATIVE) {
      try {
        Owl.getConnectionService().getCredentialsProvider(link).deleteAuthCredentials(link, null);
      } catch (CredentialsException e) {
        Activator.log(e);
      }
      throw new AuthenticationRequiredException(null, Activator.createErrorStatus(Messages.NewsGroupHandler_ERROR_AUTH_REQUIRED, null));
    }
  }

  private void throwConnectionException(String msg, NNTPClient client) throws ConnectionException {
    StringBuilder str = new StringBuilder();
    str.append(msg);
    str.append(" (").append(client.getReplyString()).append(")"); //$NON-NLS-1$//$NON-NLS-2$

    throw new ConnectionException(Activator.createErrorStatus(str.toString(), null));
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#getURLStreamHandler()
   */
  public URLStreamHandlerService getURLStreamHandler() {
    return new AbstractURLStreamHandlerService() {

      @Override
      public URLConnection openConnection(URL u) {
        return null;
      }
    };
  }

  /* Load a possible Favicon from the given Feed */
  byte[] loadFavicon(URI link, boolean rewriteHost, IProgressMonitor monitor) {
    try {
      URI faviconLink = URIUtils.toFaviconUrl(link, rewriteHost);
      if (faviconLink == null)
        return null;

      /* Pass to HTTP Protocol Handler */
      return Owl.getConnectionService().getFeedIcon(faviconLink, monitor);
    } catch (URISyntaxException e) {
      /* Ignore */
    } catch (ConnectionException e) {
      /* Ignore */
    }

    return null;
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#getLabel(java.net.URI,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  public String getLabel(URI link, IProgressMonitor monitor) {
    String path = link.getPath();

    if (StringUtils.isSet(path))
      return path.replace("/", ""); //$NON-NLS-1$//$NON-NLS-2$

    return link.toString();
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#getFeed(java.net.URI,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  public URI getFeed(URI website, IProgressMonitor monitor) {
    return website;
  }

  /*
   * @see org.rssowl.core.connection.IProtocolHandler#openStream(java.net.URI,
   * org.eclipse.core.runtime.IProgressMonitor, java.util.Map)
   */
  public InputStream openStream(URI link, IProgressMonitor monitor, Map<Object, Object> properties) {
    throw new UnsupportedOperationException();
  }
}