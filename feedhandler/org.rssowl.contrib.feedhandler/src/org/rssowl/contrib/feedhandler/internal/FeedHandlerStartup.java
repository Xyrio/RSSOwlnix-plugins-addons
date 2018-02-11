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

package org.rssowl.contrib.feedhandler.internal;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.rssowl.core.util.CoreUtils;
import org.rssowl.core.util.StreamGobbler;

/**
 * Implementation of {@link IStartup} to check if RSSOwl is the registered
 * application for the feed:// protocol and do the necessary updates to the
 * Windows Registry.
 *
 * @author bpasero
 */
public class FeedHandlerStartup implements IStartup {

  /* Constants */
  private static final String RSSOWL_EXE = "rssowlnix.exe"; //$NON-NLS-1$
  private static final String REGISTER_RSSOWL_EXE = "register_rssowl.exe"; //$NON-NLS-1$
  private static final String CHECK_REGISTRY_EXE = "check_registry.exe"; //$NON-NLS-1$
  private static final String APP_PARAMETER = "-app"; //$NON-NLS-1$

  /*
   * @see org.eclipse.ui.IStartup#earlyStartup()
   */
  public void earlyStartup() {
    Job job = new Job("") { //$NON-NLS-1$

      @Override
      protected IStatus run(IProgressMonitor monitor) {
        try {
          updateRegistry();
        } catch (Throwable e) {
          Activator.log(e);
        }
        return Status.OK_STATUS;
      }
    };
    job.setSystem(true);
    job.schedule(500);
  }

  private void updateRegistry() throws IOException, InterruptedException {

    /* Find rssowl.exe in Installation Folder */
    File rssowlExe = getRSSOwlExecutable();
    if (rssowlExe == null)
      return;

    /* Locate Executables in Workspace or copy over if not existing */
    Plugin bundle = Activator.getDefault();
    if (bundle == null)
      return;

    File checkRegistryExe = getOrCreateFromStateLocation(CHECK_REGISTRY_EXE, bundle);
    File registerAppExe = getOrCreateFromStateLocation(REGISTER_RSSOWL_EXE, bundle);
    if (checkRegistryExe == null || registerAppExe == null)
      return;

    /* Check if an Update is Required */
    if (!isUpdateRequired(checkRegistryExe, rssowlExe))
      return;

    /* Update Registry */
    updateRegistry(registerAppExe, rssowlExe);
  }

  private boolean isUpdateRequired(File checkRegistryExe, File rssowlExe) throws IOException, InterruptedException {
    List<String> commands = new ArrayList<String>();
    commands.add(checkRegistryExe.toString());
    commands.add(APP_PARAMETER);
    commands.add(rssowlExe.toString());

    Process proc = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));

    /* Let StreamGobbler handle error message */
    StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

    /* Let StreamGobbler handle output */
    StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

    /* Flush both error and output streams */
    errorGobbler.schedule();
    outputGobbler.schedule();

    return proc.waitFor() == 0 ? false : true;
  }

  private void updateRegistry(File registerAppExe, File rssowlExe) throws IOException {

    /* Ask User for Confirmation */
    final AtomicBoolean doUpdate = new AtomicBoolean(false);
    Display.getDefault().syncExec(new Runnable() {
      @SuppressWarnings("restriction")
      public void run() {
        Shell shell = org.rssowl.ui.internal.OwlUI.getActiveShell();
        if (shell != null && !shell.isDisposed())
          doUpdate.set(MessageDialog.openQuestion(shell, Messages.FeedHandlerStartup_RSSOWL, Messages.FeedHandlerStartup_MAKE_RSSOWL_DEFAULT));
      }
    });

    /* User hit "No" */
    if (!doUpdate.get())
      return;

    /* Execute Registry Updater with rssowl.exe location */
    List<String> commands = new ArrayList<String>();
    commands.add(registerAppExe.toString());
    commands.add(APP_PARAMETER);
    commands.add(rssowlExe.toString());

    Process proc = Runtime.getRuntime().exec(commands.toArray(new String[commands.size()]));

    /* Let StreamGobbler handle error message */
    StreamGobbler errorGobbler = new StreamGobbler(proc.getErrorStream());

    /* Let StreamGobbler handle output */
    StreamGobbler outputGobbler = new StreamGobbler(proc.getInputStream());

    /* Flush both error and output streams */
    errorGobbler.schedule();
    outputGobbler.schedule();
  }

  private File getOrCreateFromStateLocation(String fileName, Plugin bundle) {
    IPath stateLocation = bundle.getStateLocation();
    IPath fileLocation = stateLocation.append(fileName);

    File file = fileLocation.toFile();
    if (!file.exists()) {
      try {
        if (!file.createNewFile())
          return null;
      } catch (IOException e) {
        Activator.log(e);
        return null;
      }

      InputStream inS = getClass().getResourceAsStream("/" + fileName); //$NON-NLS-1$
      FileOutputStream outS = null;
      try {
        outS = new FileOutputStream(file);
        CoreUtils.copy(inS, outS);
      } catch (FileNotFoundException e) {
        Activator.log(e);
        return null;
      } finally {
        try {
          inS.close();
        } catch (IOException e) {
        }

        if (outS != null) {
          try {
            outS.close();
          } catch (IOException e) {
          }
        }
      }
    }

    return file;
  }

  private File getRSSOwlExecutable() {

    /* Retrieve Install Location */
    Location installLocation = Platform.getInstallLocation();
    if (installLocation == null || installLocation.getURL() == null)
      return null;

    /* Retrieve Program Dir as File Object */
    File programDir = toFile(installLocation.getURL());
    if (programDir == null || !programDir.isDirectory() || !programDir.exists())
      return null;

    /* Retrieve the RSSOwl Executable */
    File rssowlExe = new File(programDir, RSSOWL_EXE);
    if (!rssowlExe.exists())
      return null;

    return rssowlExe;
  }

  private static File toFile(URL url) {
    try {
      return new File(url.toURI());
    } catch (URISyntaxException e) {
      return new File(url.getPath());
    }
  }
}