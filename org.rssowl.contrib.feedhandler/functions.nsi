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
 * Methods used in the RSSOwl Feed Handler Helper.
 *
 * @author bpasero
 */

 !macro GETPARAMETERVALUE SWITCH DEFAULT
   Push $0
   Push $1
   Push $2
   Push $3
   Push $4

   ;$CMDLINE='"My Setup\Setup.exe" /L=1033 /S'
   Push "$CMDLINE"
   Push '${SWITCH}"'
   !insertmacro StrStr
   Pop $0
   StrCmp "$0" "" gpv_notquoted
   ;$0='/L="1033" /S'
   StrLen $2 "$0"
   Strlen $1 "${SWITCH}"
   IntOp $1 $1 + 1
   StrCpy $0 "$0" $2 $1
   ;$0='1033" /S'
   Push "$0"
   Push '"'
   !insertmacro StrStr
   Pop $1
   StrLen $2 "$0"
   StrLen $3 "$1"
   IntOp $4 $2 - $3
   StrCpy $0 $0 $4 0
   Goto gpv_done

   gpv_notquoted:
   Push "$CMDLINE"
   Push "${SWITCH}"
   !insertmacro StrStr
   Pop $0
   StrCmp "$0" "" gpv_done
   ;$0='/L="1033" /S'
   StrLen $2 "$0"
   Strlen $1 "${SWITCH}"
   StrCpy $0 "$0" $2 $1
   ;$0=1033 /S'
   Push "$0"
   Push ' '
   !insertmacro StrStr
   Pop $1
   StrLen $2 "$0"
   StrLen $3 "$1"
   IntOp $4 $2 - $3
   StrCpy $0 $0 $4 0
   Goto gpv_done

   gpv_done:
   StrCmp "$0" "" 0 +2
   StrCpy $0 "${DEFAULT}"

   Pop $4
   Pop $3
   Pop $2
   Pop $1
   Exch $0
 !macroend

; And I had to modify StrStr a tiny bit.
; Possible upgrade switch the goto's to use ${__LINE__}

!macro STRSTR
  Exch $R1 ; st=haystack,old$R1, $R1=needle
  Exch    ; st=old$R1,haystack
  Exch $R2 ; st=old$R1,old$R2, $R2=haystack
  Push $R3
  Push $R4
  Push $R5
  StrLen $R3 $R1
  StrCpy $R4 0
  ; $R1=needle
  ; $R2=haystack
  ; $R3=len(needle)
  ; $R4=cnt
  ; $R5=tmp
 ;  loop;
    StrCpy $R5 $R2 $R3 $R4
    StrCmp $R5 $R1 +4
    StrCmp $R5 "" +3
    IntOp $R4 $R4 + 1
    Goto -4
 ;  done;
  StrCpy $R1 $R2 "" $R4
  Pop $R5
  Pop $R4
  Pop $R3
  Pop $R2
  Exch $R1
!macroend