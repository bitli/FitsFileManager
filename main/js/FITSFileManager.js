"use strict";

#feature-id    Utilities > FITSFileManager

#feature-info Copy and move files based on FITS keys.<br/>\
   Written by Jean-Marc Lugrin (c) 2012,2013.

// ==================================================================================================
// The complete source code is hosted at https://bitbucket.org/bitli/fitsfilemanager
// with test scripts
// ==================================================================================================

// Copyright (c) 2012-2013 Jean-Marc Lugrin
// Copyright (c) 2003-2012 Pleiades Astrophoto S.L.
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
#define VERSION   "1.0-development"
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




// Change log
// 2012-08-27 - 0.1 - Initial Version
// 2012-09-11 - 0.5 - Significant enhancements
//     Code refactoring, speedups
//     Save/restore parameters
//     Corrected mapping of files in tree and list if not sorted as loaded,
//     added refresh button because there is no onSort event,
//     default sort is ascending on FileName
//     Added button remove all
//     Added help label
//     Use SectionBar
//     Added optional indicator to accept missing values '?' and default value
//     Check for missing key values, show message
//     Source file list is refreshed after a move
//     Supressed Export FITS keys as incompletely implemented and may be integrated in 1.8
// 2012-09-26 - 0.6 - Many enhancements
//     Use TreeBox instead of TextBox
//     Added button to check/uncheck boxes
//     List types in keyword table
//     More dynamic layout
//     Added predefined templates and regexps
//     Added copy via FITS load/save with added KEYWORD
//     Show conversion definitions
// 2012-11-11 - 0.7 - Bug correction, keyword enhancements
//     Corrected bug on display of FITS keyword in image table
//     Add the input &extension if the output file has no extension
//     Added &object as a synthethic keyword
//     Added &night as an experimental keyword
//     Added alternate FITS keywords for creation of synthetic keywords
//     Load HIERARCH FITS keywords
// 2012-11-19 - 0.8 - Refactor, bug corrections, more configurability
//     Refactored FITS keyword loading (separate js file), parameters (separate file) and quite some code
//     Correcting error of selection of file in FITSKeyword window
//     Corrected error of list entry reported by Vicent
//     Document &kw:present?absent;
//     Show alternate FITS keyword in 'Remapping' section
//     Allow selection of visibility of synthetic variable in inputFile table
//     Accept FITS keywords as variables, clean FITS kewyword value to make clean file names
//     Removed the &object; variable as this can now be done with &OBJECT;
//     Added predefiend named configuration to select kewyword mappign and conversions,
//     two configurations predefined (DEFAULT and CAHA)
//     Reworked conversion rules for filters and types, support back references in right hand side.
//     Enhance unquoting of string (respect FITS standard)
//     Handle cursor and console during file move/copy operations
//     Additional tests
//     Various presentation enhancements

// 2012-12-28 - 0.9
//     Very large rewrite of configuration mecanism
// 2013-02-06 - 1.0
//     Bug corrections on selection,
//    made minimum dialog size smaller (hopefully support screens down to approx 1200 x 760)
//    added shopw/hide fullPath on inputTreeBox
//    added exit button and confirmation for remove all

// TODO
// Make cleaning of FITS keyword values used in file name configurable
// Support optional reformatting of file name as valid PI identifier
// Add mark of sequence of text to ignore if missing variable value (in parentheses for example)
// Enhance control of ordering (support for ordering of non string values if possible)
// Add a way to use directory of source file as variable  &filedir, &filedirparent for template matching and group names
// Create a log file to record the operations
// Support export of FITS keywords as CSV or tab delimite
// Request confirmation for move (or move and copy)
// Possibility to add FITS keywords to copied files (to replace erroneous values or add missing ones)
// Possibility to check FITS header for suspect values and consistency (for example with size)
// May be allow to preview or open a selected file
// Possibly alternate configuration depending on IMAGETYP
// Batch mode (non GUI)
// Normalize directory (remove .., redundant /)
// Check # of images in file (when using load image), or use direct writes to update fits headers
// Enhance 'night' with accepting other dates and possibly midnight offset
// Management of alternate source keywords for synthethic keywords, possibly default values in keyword mapping
// Make creation of HISTORY/ORIGFILE keyword optional, make name of ORIGFILE key configurable
// Support date formatting
// Allow choice of 'cleaned up' strings (make optiona lremove of special chraracters)

// See http://heasarc.gsfc.nasa.gov/docs/software/ftools/fitsverify/




#include "PJSR-logging.jsh"

#include "FITSFileManager-json.jsh"

#include "FITSFileManager-parameters.jsh"
#include "FITSFileManager-fits.jsh"
#include "FITSFileManager-helpers.jsh"
#include "FITSFileManager-engine.jsh"
#include "FITSFileManager-text.jsh"

#include <pjsr/Sizer.jsh>
//#include <pjsr/FrameStyle.jsh>
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
