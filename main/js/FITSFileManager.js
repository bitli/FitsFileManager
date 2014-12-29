"use strict";

#feature-id    Utilities > FITSFileManager

#feature-info Copy and move files based on FITS keys.<br/>\
   Written by Jean-Marc Lugrin (c) 2012-2014.

// ==================================================================================================
// The complete source code is hosted at https://bitbucket.org/bitli/fitsfilemanager
// with test scripts
// ==================================================================================================

// Copyright (c) 2012-2014 Jean-Marc Lugrin
// Copyright (c) 2003-2014 Pleiades Astrophoto S.L.
//
// Redistribution and use in both source and binary forms, with or without
// modification, is permitted provided that the following conditions are met:
//
// 1. All redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//
// 2. All redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// 3. Neither the names "PixInsight" and "Pleiades Astrophoto", nor the names
//    of their contributors, may be used to endorse or promote products derived
//    from this software without specific prior written permission. For written
//    permission, please contact info@pixinsight.com.
//
// 4. All products derived from this software, in any form whatsoever, must
//    reproduce the following acknowledgment in the end-user documentation
//    and/or other materials provided with the product:
//
//    "This product is based on software from the PixInsight project, developed
//    by Pleiades Astrophoto and its contributors (http://pixinsight.com/)."
//
//    Alternatively, if that is where third-party acknowledgments normally
//    appear, this acknowledgment must be reproduced in the product itself.
//
// THIS SOFTWARE IS PROVIDED BY PLEIADES ASTROPHOTO AND ITS CONTRIBUTORS
// "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
// TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL PLEIADES ASTROPHOTO OR ITS
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, BUSINESS
// INTERRUPTION; PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; AND LOSS OF USE,
// DATA OR PROFITS) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
// CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
// ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
// POSSIBILITY OF SUCH DAMAGE.
// ****************************************************************************

// Base on FITSkey_0.06 of Nikolay but completely rewritten with another approach
// Thanks to Nikolay for sharing the original FITSKey

// NOTE : The parameters have their own version number in FITSFileManger-parameters.jsh
#define VERSION   "1.3-SNAPSHOTs"
#define TITLE     "FITSFileManager"

// --- Debugging control ----------------
// Set to false when doing hasardous developments...
#define EXECUTE_COMMANDS true

// Debug supprt is in the module PJSR-logging
// Tracing - define DEBUG if you define any other DEBUG_xxx
//#define DEBUG
//#define DEBUG_EVENTS
//#define DEBUG_SHOW_FITS
//#define DEBUG_FITS
//#define DEBUG_VARS
//#define DEBUG_COLUMNS
// ------------------------------------





// FITS keyword implementation is not yet fully OK and could not be implemented as may be part of PI 1.8
// #define IMPLEMENTS_FITS_EXPORT

// Padding format
#define FFM_COUNT_PAD 4

// TODO: See file todo.txt

#include "PJSR-logging.jsh"

#include "FITSFileManager-parameters.jsh"
#include "FITSFileManager-fits.jsh"
#include "FITSFileManager-helpers.jsh"
#include "FITSFileManager-engine.jsh"
#include "FITSFileManager-text.jsh"

#include <pjsr/Sizer.jsh>
#include <pjsr/TextAlign.jsh>
#include <pjsr/StdIcon.jsh>
#include <pjsr/StdCursor.jsh>
#include <pjsr/StdButton.jsh>
#include <pjsr/FrameStyle.jsh>
#include <pjsr/Color.jsh>

#include <pjsr/ButtonCodes.jsh>
#include <pjsr/FocusStyle.jsh>


#include "FITSFileManager-config-gui.jsh"
#include "FITSFileManager-gui.jsh"




// -------------------------------------------------------------------------------------
function ffM_main() {
#ifdef DEBUG
   Console.show();
#endif
   var guiParameters = new FFM_GUIParameters();
   guiParameters.loadSettings();

   var engine = new FFM_Engine(guiParameters);
   engine.setConfiguration(ffM_Configuration.createWorkingConfiguration());

   var dialog = new MainDialog(engine, guiParameters);
   dialog.execute();
   guiParameters.saveSettings();
}

ffM_main();
