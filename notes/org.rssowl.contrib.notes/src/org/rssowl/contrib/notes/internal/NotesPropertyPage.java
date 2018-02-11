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

package org.rssowl.contrib.notes.internal;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.rssowl.core.persist.IEntity;
import org.rssowl.core.util.StringUtils;
import org.rssowl.ui.dialogs.properties.IEntityPropertyPage;
import org.rssowl.ui.dialogs.properties.IPropertyDialogSite;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link IEntityPropertyPage} to write some Notes to
 * {@link org.rssowl.core.persist.IFolderChild}.
 *
 * @author bpasero
 */
public class NotesPropertyPage implements IEntityPropertyPage {
  private static final String NOTES_PREF_ID = "org.rssowl.contrib.notes.pref.Note"; //$NON-NLS-1$

  private Text fNotesInput;
  private List<IEntity> fEntities;

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#createContents(org.eclipse.swt.widgets.Composite)
   */
  public Control createContents(Composite parent) {
    Composite container = new Composite(parent, SWT.NONE);
    container.setLayout(new GridLayout(1, false));

    fNotesInput = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL);
    fNotesInput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

    if (fEntities.size() == 1) {
      Object notes = fEntities.get(0).getProperty(NOTES_PREF_ID);
      if (notes != null)
        fNotesInput.setText(notes.toString());
    }

    return container;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#init(org.rssowl.ui.dialogs.properties.IPropertyDialogSite, java.util.List)
   */
  public void init(IPropertyDialogSite site, List<IEntity> entities) {
    fEntities = entities;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#performOk(java.util.Set)
   */
  public boolean performOk(Set<IEntity> entitiesToSave) {
    if (fEntities.size() == 1) {
      IEntity entity = fEntities.get(0);

      Object oldNotesObj = entity.getProperty(NOTES_PREF_ID);
      String oldNotes = (oldNotesObj != null) ? oldNotesObj.toString() : ""; //$NON-NLS-1$
      String newNotes = fNotesInput.getText();

      boolean save = false;

      if (!StringUtils.isSet(newNotes) && StringUtils.isSet(oldNotes)) {
        entity.removeProperty(NOTES_PREF_ID);
        save = true;
      } else if (!newNotes.equals(oldNotes)) {
        entity.setProperty(NOTES_PREF_ID, newNotes);
        save = true;
      }

      if (save)
        entitiesToSave.add(entity);
    }

    return true;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#setFocus()
   */
  public void setFocus() {
    fNotesInput.setFocus();
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#getImage()
   */
  public ImageDescriptor getImage() {
    return null;
  }

  /*
   * @see org.rssowl.ui.dialogs.properties.IEntityPropertyPage#finish()
   */
  public void finish() {}
}