"use strict";

#feature-id    Utilities > FITSFileManager

#feature-info Copy and move files based on FITS keys.<br/>\
   Written by Jean-Marc Lugrin (c) 2012.

// ==================================================================================================
// The complete source code is hosted at https://bitbucket.org/bitli/fitsfilemanager
// with test scripts
// ==================================================================================================

// Copyright (c) 2012 Jean-Marc Lugrin
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

// Base on FITSkey_0.06 of Nikolay but almost completely rewritten with another approach
// Thanks to Nikolay for sharing the original FITSKey


#define VERSION   "0.8-snapshot"
#define TITLE     "FITSFileManager"

// --- Debugging control ----------------
// Set to false when doing hasardous developments...
#define EXECUTE_COMMANDS true

// Tracing - define DEBUG if you define any other DEBUG_xxx
//#define DEBUG
//#define DEBUG_EVENTS
//#define DEBUG_SHOW_FITS
//#define DEBUG_FITS
//#define DEBUG_VARS
//#define DEBUG_COLUMNS
// ------------------------------------


#ifdef DEBUG
function debug(str) {
   var s = replaceAmps(str.toString());
   Console.writeln(s);
   Console.flush();
   //processEvents();  // This may interfere with event processing order
}
#endif

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
// 2012-11-19 - 0.8 - Refactor, bug corrections
//     Refactored FITS keyword loading (separate js file), parameters (separate file) and quite some code
//     Correcting error of selection of file in FITSKeyword window
//     Corrected error of list entry reported by Vicent
//     Small presentation enhancements
//     Document &kw:present?absent;
//     Show alternate FITS keyword in 'Remapping' section
//     Allow selection of visibility of synthetic variable in inputFile table
//     Accept FITS keywords as variables, clean the value result
//     Removed the &object; variable as this can now be done with &OBJECT;
//     Added selection of mapping rules
//     Reworked conversion rules for filters and types
//     Enhance unquoting of string (respect FITS standard)
//     Additional tests




// TODO
// Option for handling of minus and other special characters to form a file name being valid PI ids
// Add mark of sequence of text to ignore if missing variable value (in parentheses for example)
// Generate an 'orderBy' column
// Recalculate all variables when rules changed
// Hide common header part of source folders to make file name more visible
// Add a way to use directory of source file as variable  &filedir, &filedirparent for template matching and group names
// Support date formatting, number formatting
// Create a log file to record the source files
// Request confirmation for move (or move and copy)
// Possibility to add FITS keywords to copied files (to replace erroneous values or add missing ones)
// Add bar for action on fits header: add, add if missing, add or replace, replace if present, remove, remove all
// Allow to open selected files (not required, part of new file manager)
// Rules set: Keep list of recent templates used (currently one 1 kept)
//            Configurable list of transformation, especially for filters (ha, ..)
// Normalize directory (remove .., redundant /)
// Check that move is on one drive
// Check # of images in file (when using load image), or use direct writes to update fits headers
// add .* as last action for conversion
// Change cursor to mark busy during move/copy
// Use - standard as collapsed title of conversions
// Document and add default for long-obs and night
// Check details of FITSKeywords, especialy type and null, processing of quoted characters and leading spaces.
// Display and management of alternate keywords for synthethic keywords
// Allow selection of synthethic keywords in table, enhance table view
// Make COUNT_PAD a parameter or use a configurable default format

// Format indicators
// %<stuff>
// %s (for maximum size, fixed sizre with padding)
// %S For cleanup strings
// %d decimal number (minimun size, 0 padding)
// %b boolean T,F or two values)
// % Y M D h m s t for date (conflic with %s) or %{params}d

// See http://heasarc.gsfc.nasa.gov/docs/software/ftools/fitsverify/





#include "FITSFileManager-fits.jsh"
#include "FITSFileManager-helpers.jsh"
#include "FITSFileManager-engine.jsh"
#include "FITSFileManager-parameters.jsh"
#include "FITSFileManager-gui.jsh"




// -------------------------------------------------------------------------------------
function ffM_main() {
#ifdef DEBUG
   Console.show();
#endif
   var guiParameters = new FFM_GUIParameters();
   guiParameters.loadSettings();

   var engine = new FFM_Engine(guiParameters);

   var dialog = new MainDialog(engine, guiParameters);
   dialog.execute();
   guiParameters.saveSettings();
}

ffM_main();
