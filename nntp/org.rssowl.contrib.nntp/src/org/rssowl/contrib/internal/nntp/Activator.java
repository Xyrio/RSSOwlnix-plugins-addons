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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.BundleContext;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends Plugin {

  /* The shared instance */
  private static Activator fgPlugin;

  /* This plugins ID */
  private static final String PLUGIN_ID = "org.rssowl.contrib.nntp"; //$NON-NLS-1$

  /**
   * The constructor
   */
  public Activator() {}

  /*
   * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
   */
  @Override
  public void start(BundleContext context) throws Exception {
    super.start(context);
    fgPlugin = this;
  }

  /*
   * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
   */
  @Override
  public void stop(BundleContext context) throws Exception {
    fgPlugin = null;
    super.stop(context);
  }

  /**
   * @param e the exception to log.
   */
  public static void log(Exception e) {
    if (fgPlugin != null)
      fgPlugin.getLog().log(createErrorStatus(e.getMessage(), e));
  }

  /**
   * Create a Error IStatus out of the given message and exception.
   *
   * @param msg The message describing the error.
   * @param e The Exception that occured.
   * @return An IStatus out of the given message and exception.
   */
  public static IStatus createErrorStatus(String msg, Exception e) {
    return new Status(IStatus.ERROR, PLUGIN_ID, IStatus.ERROR, msg, e);
  }
}