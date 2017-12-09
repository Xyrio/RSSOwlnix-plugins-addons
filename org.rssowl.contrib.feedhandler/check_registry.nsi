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

/**
 * Helper to read from the Windows Registry to determine if to register RSSOwl as feed handler
 * for the feed:// protocol.
 *
 * @author bpasero
 */

;##### Required Methods ######
!include functions.nsi

;#####   Variables  ######
!define VER_DISPLAY "1.0.0"

;#####  Version Information ######
VIProductVersion "${VER_DISPLAY}.0"
VIAddVersionKey "ProductName" "Register RSSOwl as Feed Handler"
VIAddVersionKey "CompanyName" "RSSOwl Team"
VIAddVersionKey "LegalCopyright" "Benjamin Pasero"
VIAddVersionKey "FileDescription" "Register RSSOwl as Feed Handler"
VIAddVersionKey "FileVersion" "${VER_DISPLAY}"

Name "Register RSSOwl as Feed Handler"
Icon "rssowl.ico"
OutFile "check_registry.exe"

SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow
RequestExecutionLevel user

;##### Required Vars ######
var file_param

;##### Entry Point ######
Section ""

  ;### Check for parameter -file ######
  !insertmacro GetParameterValue "-app " ""
  Pop $R0
  StrCpy $file_param $R0
  
  StrCmp $file_param '' QuitNormally Proceed
  
  ;### Register to feed Protocol ###
  Proceed:
    ReadRegStr $0 HKCR feed ""
    StrCmp $0 "URL:feed Protocol" ProceedA QuitNeedsUpdate
    
  ProceedA:
    ReadRegStr $1 HKCR feed "URL Protocol"
    StrCmp $1 "" ProceedB QuitNeedsUpdate
    
  ProceedB:
    ReadRegStr $2 HKCR feed\DefaultIcon ""
    StrCmp $2 "$\"$file_param$\"" ProceedC QuitNeedsUpdate
    
  ProceedC:
    ReadRegStr $3 HKCR feed\shell\open\command ""
    StrCmp $3 "$\"$file_param$\" $\"%1$\"" QuitNormally QuitNeedsUpdate
    
  
  QuitNeedsUpdate:
    SetErrorLevel 1
    Quit
    
  QuitNormally:
  
SectionEnd