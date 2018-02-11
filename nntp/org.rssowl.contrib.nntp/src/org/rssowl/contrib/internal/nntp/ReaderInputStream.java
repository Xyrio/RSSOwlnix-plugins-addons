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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * The <code>ReaderInputStream</code> is an instance of
 * <code>InputStream</code> wrapping around a Reader. This allows using any
 * Reader in APIs where streams are required.
 * 
 * @author bpasero
 */
public class ReaderInputStream extends InputStream {
  private Reader fReader;

  /**
   * Creates a new instance of <code>ReaderInputStream</code> wrapping around
   * a <code>Reader</code>.
   * 
   * @param reader The <code>Reader</code> to wrap around.
   */
  public ReaderInputStream(Reader reader) {
    fReader = reader;
  }

  /*
   * @see java.io.InputStream#read()
   */
  @Override
  public int read() throws IOException {
    return fReader.read();
  }

  /*
   * @see java.io.InputStream#close()
   */
  @Override
  public void close() throws IOException {
    fReader.close();
  }

  /*
   * @see java.io.InputStream#mark(int)
   */
  @Override
  public synchronized void mark(int readlimit) {
    try {
      fReader.mark(readlimit);
    } catch (IOException e) {
      /* Ignore */
    }
  }

  /*
   * @see java.io.InputStream#markSupported()
   */
  @Override
  public boolean markSupported() {
    return fReader.markSupported();
  }

  /*
   * @see java.io.InputStream#reset()
   */
  @Override
  public synchronized void reset() throws IOException {
    fReader.reset();
  }

  /*
   * @see java.io.InputStream#skip(long)
   */
  @Override
  public long skip(long n) throws IOException {
    return fReader.skip(n);
  }
}