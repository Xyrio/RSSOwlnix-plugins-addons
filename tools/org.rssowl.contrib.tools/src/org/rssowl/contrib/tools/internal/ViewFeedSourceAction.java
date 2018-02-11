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

package org.rssowl.contrib.tools.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.Owl;
import org.rssowl.core.connection.ConnectionException;
import org.rssowl.core.connection.IAbortable;
import org.rssowl.core.connection.IProtocolHandler;
import org.rssowl.core.persist.IBookMark;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

/**
 * Displays the source of a feed in the default editor after downloading.
 *
 * @author bpasero
 */
public class ViewFeedSourceAction implements IWorkbenchWindowActionDelegate {

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
   */
  public void init(IWorkbenchWindow window) {}

  /*
   * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
   */
  @SuppressWarnings("restriction")
  public void run(IAction action) {
    org.rssowl.ui.internal.editors.feed.FeedView activeFeedView = org.rssowl.ui.internal.OwlUI.getActiveFeedView();
    if (activeFeedView != null) {
      IEditorInput input = activeFeedView.getEditorInput();
      if (input instanceof org.rssowl.ui.internal.editors.feed.FeedViewInput) {
        org.rssowl.ui.internal.editors.feed.FeedViewInput feedInput = (org.rssowl.ui.internal.editors.feed.FeedViewInput) input;
        if (feedInput.getMark() instanceof IBookMark) {
          final IBookMark bm = (IBookMark) feedInput.getMark();
          final URI feedLink = bm.getFeedLinkReference().getLink();

          try {
            final IProtocolHandler handler = Owl.getConnectionService().getHandler(feedLink);
            if (handler instanceof org.rssowl.core.internal.connection.DefaultProtocolHandler) {
              Job downloadJob = new Job(Messages.ViewFeedSourceAction_DOWNLOAD_FEED_SOURCE) {
                @Override
                protected IStatus run(IProgressMonitor monitor) {
                  monitor.beginTask(bm.getName(), IProgressMonitor.UNKNOWN);

                  File tmpFile = null;
                  InputStream in = null;
                  FileOutputStream out = null;
                  boolean canceled = false;
                  Exception error = null;
                  try {
                    tmpFile = File.createTempFile("feed", ".txt"); //$NON-NLS-1$ //$NON-NLS-2$
                    tmpFile.deleteOnExit();

                    byte[] buffer = new byte[8192];

                    in = handler.openStream(feedLink, monitor, null);
                    out = new FileOutputStream(tmpFile);
                    while (true) {

                      /* Check for Cancellation and Shutdown */
                      if (monitor.isCanceled() || org.rssowl.ui.internal.Controller.getDefault().isShuttingDown()) {
                        canceled = true;
                        return Status.CANCEL_STATUS;
                      }

                      /* Read from Stream */
                      int read = in.read(buffer);
                      if (read == -1)
                        break;

                      out.write(buffer, 0, read);
                    }
                  } catch (FileNotFoundException e) {
                    error = e;
                    Activator.log(e);
                  } catch (IOException e) {
                    error = e;
                    Activator.log(e);
                  } catch (ConnectionException e) {
                    error = e;
                    Activator.log(e);
                  } finally {
                    monitor.done();

                    if (out != null) {
                      try {
                        out.close();
                      } catch (IOException e) {
                        Activator.log(e);
                      }
                    }

                    if (in != null) {
                      try {
                        if ((canceled || error != null) && in instanceof IAbortable)
                          ((IAbortable) in).abort();
                        else
                          in.close();
                      } catch (IOException e) {
                        Activator.log(e);
                      }
                    }
                  }

                  /* Open in Default Program */
                  if (tmpFile != null)
                    Program.launch(tmpFile.toString());

                  return Status.OK_STATUS;
                }
              };
              downloadJob.schedule();
            }
          } catch (ConnectionException e) {
            Activator.log(e);
          }
        }
      }
    }
  }

  /*
   * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
   */
  public void selectionChanged(IAction action, ISelection selection) {}

  /*
   * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
   */
  public void dispose() {}
}