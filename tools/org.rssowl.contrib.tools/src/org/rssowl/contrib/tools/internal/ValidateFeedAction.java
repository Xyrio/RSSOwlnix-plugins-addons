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

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.rssowl.core.persist.IBookMark;
import org.rssowl.core.util.URIUtils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Validates the Feed using the W3 online validation service.
 *
 * @author bpasero
 */
public class ValidateFeedAction implements IWorkbenchWindowActionDelegate {

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
          IBookMark bm = (IBookMark) feedInput.getMark();
          try {
            URI uri = new URI("http://www.feedvalidator.org/check.cgi?url=" + URIUtils.urlEncode(bm.getFeedLinkReference().getLinkAsText())); //$NON-NLS-1$

            org.rssowl.ui.internal.actions.OpenInBrowserAction openAction = new org.rssowl.ui.internal.actions.OpenInBrowserAction();
            openAction.selectionChanged(null, new StructuredSelection(uri));
            openAction.run();
          } catch (URISyntaxException ex) {
            Activator.log(ex);
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