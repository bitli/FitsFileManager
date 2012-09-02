"use strict";

#feature-id    Utilities > FITSFileManager

#feature-info Copy and move files based on FITS keys.<br/>

// Base on FITSkey_0.06 of Nikolay but almost completely rewritten with another approach (I hope he doesn't mind)

// Author: Jean-Marc Lugrin


#define VERSION   "0.30"
#define TITLE     "FITSFileManager"

// --- Debugging control ----------------
// Set to false when doing hasardous developments...
#define EXECUTE_COMMANDS false
// Tracing - define DEBUG if you define any other
//#define DEBUG
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
// --- Debugging control ----------------


#define FFM_COUNT_PAD 4

#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"



// Change log
// 2012-08-27 - 0.1 - Initial Version
// 2012-xx-xx - 0.2 - Enhancements and speedup
//     Code refactoring, speedups
//     Save/restore parameters
//     Corrected mapping of files in tree and list if not sorted as loaded,
//     added refresh button because there is no onSort event,
//     default sort is ascending on FileName
//     Added button remove all
//     Added help label



// TODO
// keep list of recent patterns used
// Option for handling of minus and other special characters for form file name being valid PI ids
// Add FITS keywords as variables, with formatting options
// Add optional indicator to accept missing values '?' and default value
// Check for missing key values
// Add sequence of optional text to ignore if missing variable value ()
// Generate and 'orderBy' column
// Show sythetic keys in table, only show selected keys
// Hide common header part of source folders to make file name more visible
// Add a way to use directory of source file as variable  &filedir, &filedirparent for pattern matching and group names
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
#define FFM_DEFAULT_TARGET_FILENAME_PATTERN "&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;"
// #define FFM_DEFAULT_TARGET_FILENAME_PATTERN "&filename;_AS_&1;_bin_&binning;_filter_&filter;_temp_&temp;_type_&type;_exp_&exposure;s_count_&count;&extension;";
#define FFM_DEFAULT_GROUP_PATTERN "&targetDir;"


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
