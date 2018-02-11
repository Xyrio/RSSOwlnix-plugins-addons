/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2005, 2008. All Rights Reserved. 
 * 
 * Note to U.S. Government Users Restricted Rights:  Use, 
 * duplication or disclosure restricted by GSA ADP Schedule 
 * Contract with IBM Corp.
 *******************************************************************************/

package org.rssowl.contrib.offline.internal;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;
import org.eclipse.ui.PlatformUI;

/**
 * An action to bring RSSOwl into the offline modus or back online.
 */
public class GoOfflineAction implements IWorkbenchWindowActionDelegate {

	/*
	 * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.IWorkbenchWindow)
	 */
	public void init(IWorkbenchWindow window) {}

	/*
	 * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
	 */
	@SuppressWarnings("restriction")
	public void run(IAction action) {
		boolean isOffline= org.rssowl.ui.internal.Controller.getDefault().isOffline();
		if (isOffline)
			action.setText(Messages.getString("GoOfflineAction.GO_OFFLINE")); //$NON-NLS-1$
		else
			action.setText(Messages.getString("GoOfflineAction.GO_ONLINE")); //$NON-NLS-1$

		/* Toggle Offline Mode */
		org.rssowl.ui.internal.Controller.getDefault().setOffline(!isOffline);

		/* Update Window Titles */
		updateWindowsTitle(!isOffline);

		/* Stop any Updates if going offline */
		if (!isOffline)
			org.rssowl.ui.internal.Controller.getDefault().stopUpdate();
	}

	private void updateWindowsTitle(boolean isOffline) {
		IWorkbenchWindow windows[]= PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow window : windows) {
			String text= window.getShell().getText();
			if (isOffline)
				window.getShell().setText(NLS.bind(Messages.getString("GoOfflineAction.WORKING_OFFLINE"), text)); //$NON-NLS-1$
			else
				window.getShell().setText(Messages.getString("GoOfflineAction.RSSOWL")); //$NON-NLS-1$
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