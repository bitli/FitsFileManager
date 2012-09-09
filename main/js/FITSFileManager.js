"use strict";

#feature-id    Utilities > FITSFileManager

#feature-info Copy and move files based on FITS keys.<br/>\
   Written by Jean-Marc Lugrin (c) 2012.

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


#define VERSION   "0.30"
#define TITLE     "FITSFileManager"

// --- Debugging control ----------------
// Set to false when doing hasardous developments...
#define EXECUTE_COMMANDS false
// Tracing - define DEBUG if you define any other
// #define DEBUG
//#define DEBUG_EVENTS
//#define DEBUG_FITS
//#define DEBUG_VARS
//#define DEBUG_COLUMNS

#ifdef DEBUG
function debug(str) {
   var s = replaceAmps(str.toString());
   Console.writeln(s);
   Console.flush();
   //processEvents();  // This may interfere with event processing order
}
#endif


// Padding format
#define FFM_COUNT_PAD 4

#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"



// Change log
// 2012-08-27 - 0.1 - Initial Version
// 2012-xx-xx - 0.3 - Enhancements and speedup
//     Code refactoring, speedups
//     Save/restore parameters
//     Corrected mapping of files in tree and list if not sorted as loaded,
//     added refresh button because there is no onSort event,
//     default sort is ascending on FileName
//     Added button remove all
//     Added help label
//     Use SectionBar



// TODO
// keep list of recent templates used
// Option for handling of minus and other special characters for form file name being valid PI ids
// Add FITS keywords as variables, with formatting options
// Add optional indicator to accept missing values '?' and default value
// Check for missing key values
// Add sequence of optional text to ignore if missing variable value ()
// Generate and 'orderBy' column
// Show sythetic keys in table, only show selected keys
// Hide common header part of source folders to make file name more visible
// Add a way to use directory of source file as variable  &filedir, &filedirparent for template matching and group names
// Support date formatting, number formatting
// Create a log file for record the source files
// Ensure source is refreshed in case of move
// Request confirmation for move (or move and copy)
// Add 'reset' icon for rules
// Possibility to add FITS keywords to copied files (for example original file name, or replace erroneous values)
// Allow to open selected files (not required, part of new file manager)
// Configurable list of transformation, especially for filters (ha, ..)
// Normalize directory (remove .., redundant /)
// Ensure that text is stable after update of GUI


// Select the first sequence without -_. or the whole name in &1; (second group is non capturing)
#define FFM_DEFAULT_SOURCE_FILENAME_REGEXP /([^-_.]+)(?:[._-]|$)/
#define FFM_DEFAULT_TARGET_FILENAME_TEMPLATE "&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;"
// #define FFM_DEFAULT_TARGET_FILENAME_TEMPLATE "&filename;_AS_&1;_bin_&binning;_filter_&filter;_temp_&temp;_type_&type;_exp_&exposure;s_count_&count;&extension;";
#define FFM_DEFAULT_GROUP_TEMPLATE "&targetDir;"


#include "FITSFileManager-helpers.jsh"
#include "FITSFileManager-engine.jsh"
#include "FITSFileManager-gui.jsh"




// -------------------------------------------------------------------------------------
function ffM_main() {
   var guiParameters = new FFM_GUIParameters();
   guiParameters.loadSettings();

   var engine = new FFM_Engine(guiParameters);

   var dialog = new MainDialog(engine, guiParameters);
   dialog.execute();
   guiParameters.saveSettings();
}

ffM_main();
