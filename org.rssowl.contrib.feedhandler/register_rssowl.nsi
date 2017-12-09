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
 * Helper to write to the Windows Registry and register RSSOwl as feed handler
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
OutFile "register_rssowl.exe"

SilentInstall silent
AutoCloseWindow true
ShowInstDetails nevershow

;##### Required Vars ######
var file_param

;##### Entry Point ######
Section ""

  ;### Check for parameter -file ######
  !insertmacro GetParameterValue "-app " ""
  Pop $R0
  StrCpy $file_param $R0
  
  StrCmp $file_param '' Quit Proceed
  
  ;### Register to feed Protocol ###
  Proceed:
    WriteRegStr HKCR "feed" "" "URL:feed Protocol"
    WriteRegStr HKCR "feed" "URL Protocol" ""
    WriteRegStr HKCR "feed\DefaultIcon" "" "$\"$file_param$\""
    WriteRegStr HKCR "feed\shell\open\command" "" "$\"$file_param$\" $\"%1$\""
    
  Quit:
  
SectionEnd