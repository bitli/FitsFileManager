#feature-id    Utilities > FITSFileManager

#feature-info Copy and move files based on FITS keys.<br/>


#define VERSION   "0.20"
#define TITLE     "FITSFileManager"

// Tracing - define DEBUG if you define any other
#define DEBUG
//#define DEBUG_EVENTS
//#define DEBUG_FITS
//#define DEBUG_VARS



// Significantly modified from FITSkey_0.06 of Nikolay (I hope he doesn't mind)

// Author: Jean-Marc Lugrin

// Use at your own risk - this script move and copy files


#include <pjsr/DataType.jsh>
#include <pjsr/DataType.jsh>
#include <pjsr/Sizer.jsh>
//#include <pjsr/FrameStyle.jsh>
#include <pjsr/TextAlign.jsh>
#include <pjsr/StdIcon.jsh>
#include <pjsr/StdButton.jsh>
// Set to false when doing hasardous developments...
#define EXECUTE_COMMANDS false


// Change log
// 2012-08-27 - 0.1 - Initial Version
// 2012-xx-xx - 0.2 - Enhancements and speedup
//     Code refactoring, speedups
//     Save/restore parameters
//     Corrected mapping of files in tree and list if not sorted as loaded,
//     added refresh button because there is no onSort event,
//     default sort is ascending on FileName
//     Added button remove all



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

// For icons, see http://pixinsight.com/forum/index.php?topic=1953.msg12267#msg12267

// Help texts

#define TARGET_PATTERN_TOOLTIP "\
Define how the target file name will be generated. Text is copied\n\
as is to the output name. Keywords (between & and semicolon) are\n\
defined from the file information and FITS keywordsas follows:\n\
   &binning;    Binning from XBINNING and YBINNING as integers, like 2x2.\n\
   &count;      The number of the file being moved/copied int the current group, padded to COUNT_PAD.\n\
   &rank;       The number of the file in the order of the input file list, padded to COUNT_PAD.\n\
   &exposure;   The exposure from EXPOSURE, but as an integer (assume seconds).\n\
   &extension;  The extension of the source file (with the dot.)\n\
   &filename;   The file name part of the source file.\n\
   &filter:     The filter name from FILTER as lower case trimmed normalized name.\n\
   &temp;       The SET-TEMP temperature in C as an integer.\n\
   &type:       The IMAGETYP normalized to 'flat', 'bias', 'dark', 'light'.\n\
   &FITSKW;     (NOT IMPLEMENTED).\n\
   &0; &1;, ... The corresponding match from the source file name pattern field.\n\
The target file name pattern may contain forward slashes that will be used\n\
as directory separator. Keywords may appear multiple time and may also be part of directory names.\n\
Unknown keywords are replaced by their name in upper case.\n\
The default pattern has no directory and use a part of the original file name as a prefix.\n\
"

#define SOURCE_FILENAME_REGEXP_TOOLTIP "\
Define  a regular expression (without the surround slashes) that will be applied to all file names\n\
without the extension. The 'match' array resulting from the regular expression matching can be used\n\
in the target file name pattern as &0; (whole expression), &1 (first group), ...\n\
The default extract the part of the name before the first dash (you can replace the\n\
two dashes by two underlines for example).\n\
In case of error the field turns red\n\
"

#define GROUP_PATTERN_TOOLTIP "\
Define the pattern to generate a group name used by &count;.\n\
Each group has its own group number starting at 1. You can use the same variables\n\
as for the target file name, except &count;. In addition you can use:\n\
   &targetDir;    The directory part of the target file name (except that &count; is not replaced).\n\
Leave blank or use a fixed name to have a single counter. The default &targetDir; count in each target\n\
directory. &filter; would count separetely for each filter.\n\
"

// ------------------------------------------------------------------------------------------------------------------------
// User Interface Parameters
// ------------------------------------------------------------------------------------------------------------------------

// The GUI parameters keeps track of this information in
// a form easy to be saved and presented to the user.



#define FFM_COUNT_PAD 4

#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"

// Select the first sequence without -_. or the whole name in &1; (second group is non capturing)
#define FFM_DEFAULT_SOURCE_FILENAME_REGEXP /([^-_.]+)(?:[._-]|$)/

function FFM_GUIParameters() {

   this.reset = function () {

      // SETTINGS: Saved latest correct GUI state
      this.targeFileNamePattern = "&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;";
      //this.targeFileNamePattern = "&filename;_AS_&1;_bin_&binning;_filter_&filter;_temp_&temp;_type_&type;_exp_&exposure;s_count_&count;&extension;";

      // Default file name reguler expression
      this.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;

      this.orderBy = "&rank;"
      this.groupByPattern = "&targetDir;";
    }
   this.reset();


   // For debugging and logging
   this.toString = function() {
      var s = "GUIParameters:\n";
      s += "  targeFileNamePattern:           " + replaceAmps(this.targeFileNamePattern) + "\n";
      s += "  sourceFileNameRegExp:           " + replaceAmps(regExpToString(this.sourceFileNameRegExp)) + "\n";
      s += "  orderBy:                        " + replaceAmps(this.orderBy) + "\n";
      s += "  groupByPattern:                 " + replaceAmps(this.groupByPattern) + "\n";
      return s;
   }
}

FFM_GUIParameters.prototype.loadSettings = function()
{
   function load( key, type )
   {
      var setting = Settings.read( FFM_SETTINGS_KEY_BASE + key, type );
#ifdef DEBUG
      Console.writeln("FFM_GUIParameters.load: ", key, ": ", (setting===null ? 'null' : replaceAmps(setting.toString())));
#endif
      return setting;
   }

   function loadIndexed( key, index, type )
   {
      return load( key + '_' + index.toString(), type );
   }

   var o;
   if ( (o = load( "version",    DataType_Double )) != null ) {
      if (o > VERSION) {
         Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' have version ", o, " later than script version ", VERSION, ", settings ignored");
      } else {
         if ( (o = load( "targeFileNamePattern",    DataType_String )) != null ) {
            this.targeFileNamePattern = o;
         };
         if ( (o = load( "sourceFileNameRegExp",    DataType_String )) != null ) {
            try {
               this.sourceFileNameRegExp = RegExp(o);
            } catch (err) {
               // Default in case of error in load
               guiParameters.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;
#ifdef DEBUG
               debug("loadSettings: bad regexp - err: " + err);
#endif
            }
         };
         if ( (o = load( "orderBy",                 DataType_String )) != null )
            this.orderBy = o;
         if ( (o = load( "groupByPattern",          DataType_String )) != null )
            this.groupByPattern = o;
      }
   } else {
      Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' do not have a 'version' key, settings ignored");
   }

};

FFM_GUIParameters.prototype.saveSettings = function()
{
   function save( key, type, value ) {
#ifdef DEBUG
      Console.writeln("saveSettings: key=",key,", type=", type, ", value=" ,replaceAmps(value.toString()));
#endif
      Settings.write( FFM_SETTINGS_KEY_BASE + key, type, value );
   }

   function saveIndexed( key, index, type, value ) {
#ifdef DEBUG
      Console.writeln("saveSettings: key=",key,", index=", index, ", type=", type, ", value=" ,replaceAmps(value.toString()));
#endif
      save( key + '_' + index.toString(), type, value );
   }

   save( "version",                  DataType_Double,  parseFloat(VERSION) );
   save( "targeFileNamePattern",     DataType_String,  this.targeFileNamePattern );
   save( "sourceFileNameRegExp",     DataType_String,  regExpToString(this.sourceFileNameRegExp) );
   save( "orderBy",                  DataType_String,  this.orderBy );
   save( "groupByPattern",           DataType_String,  this.groupByPattern );

}





// ------------------------------------------------------------------------------------------------------------------------
// Utility functions
// ------------------------------------------------------------------------------------------------------------------------


// -- Utility methods

#ifdef DEBUG
function debug(str) {
   var s = replaceAmps(str.toString());
   Console.writeln(s);
   Console.flush();
   //processEvents();  // This may interfere with event processing order
}
#endif


// ------- string functions

function replaceAll (txt, replace, with_this) {
  return txt.replace(new RegExp(replace, 'g'),with_this);
}

FFM_replaceAmpsRegExp = new RegExp('&', 'g');

function replaceAmps (txt) {
  return txt.replace(FFM_replaceAmpsRegExp,'&amp;');
}



// Remove quotes and trim
function unQuote (s) {
   var t = s.trim();
   if (t.length>0 && t[0]=="'" && t[t.length-1]=="'") {
      return t.substring(1,t.length-1).trim();
   }
   return t;
}


// ------- formatting functions

// Pad a mumber with leading 0
Number.prototype.pad = function(size){
      var s = String(this);
      while (s.length < size) s = "0" + s;
      return s;
}


// ------- file functions

function copyFile( sourceFilePath, targetFilePath ) {
   var f = new File;

   f.openForReading( sourceFilePath );
   var buffer = f.read( DataType_ByteArray, f.size );
   f.close();

   f = new File();
   f.createForWriting( targetFilePath );
   f.write( buffer );
   //f.flush(); // optional; remove if immediate writing is not required
   f.close();
}





// ------- FITS utility methods
// Code from FitsKey and/or other examples
// Read the fits keywords of a file, return an array FITSKeyword (value is empty string if there is no value)
function LoadFITSKeywords( fitsFilePath )
{
   function searchCommentSeparator( b )
   {
      var inString = false;
      for ( var i = 9; i < 80; ++i )
         switch ( b.at( i ) )
         {
         case 39: // single quote
            inString ^= true;
            break;
         case 47: // slash
            if ( !inString )
               return i;
            break;
         }
      return -1;
   }

   var f = new File;
   f.openForReading( fitsFilePath );

   var keywords = [];
   for ( ;; )
   {
      var rawData = f.read( DataType_ByteArray, 80 );

      var name = rawData.toString( 0, 8 );
      if ( name.toUpperCase() == "END     " ) // end of HDU keyword list?
         break;

      if ( f.isEOF )
         throw new Error( "Unexpected end of file: " + fitsFilePath );

      var value;
      var comment;
      if ( rawData.at( 8 ) == 61 ) // value separator (an equal sign at byte 8) present?
      {
         // This is a valued keyword
         var cmtPos = searchCommentSeparator( rawData ); // find comment separator slash
         if ( cmtPos < 0 ) // no comment separator?
            cmtPos = 80;
         value = rawData.toString( 9, cmtPos-9 ); // value substring
         if ( cmtPos < 80 )
            comment = rawData.toString( cmtPos+1, 80-cmtPos-1 ); // comment substring
         else
            comment = new String;
      }
      else
      {
         // No value in this keyword
         value = new String;
         comment = rawData.toString( 8, 80-8 );
      }

#ifdef DEBUG_FITS
   debug("LoadFITSKeywords: - name[" + name + "],["+value+ "],["+comment+"]");
#endif
      // Perform a naive sanity check: a valid FITS file must begin with a SIMPLE=T keyword.
      if ( keywords.length == 0 )
         if ( name != "SIMPLE  " && value.trim() != 'T' )
            throw new Error( "File does not seem a valid FITS file: " + fitsFilePath );

      // Add new keyword.
      keywords.push( new FITSKeyword( name.trim(), value.trim(), comment.trim() ) );
   }
   f.close();
   return keywords;
}


 function findKeyWord(keys, name) {
   // keys = array of all FITSKeyword of a file
   // for in all keywords of the file
   for (var k in keys) {
      //debug("kw: '" + keys[k].name + "' '"+ keys[k].value + "'");
      if (keys[k].name == name)  {
         // keyword found in the file >> extract value
#ifdef DEBUG_FITS
         debug("findKeyWord: '" + keys[k].name + "' found '"+ keys[k].value + "'");
#endif
         return (keys[k].value)
      }
   }
#ifdef DEBUG_FITS
   debug("findKeyWord: '" +name + "' not found");
#endif
   return '';
}


// ------ Conversion support functions

function convertFilter(rawFilterName) {
   var filterConversions = [
      [/green/i, 'green'],
      [/red/i, 'red'],
      [/blue/i, 'blue'],
      [/clear/i, 'clear'],
   ];
   var unquotedName = unQuote(rawFilterName);
   for (var i=0; i<filterConversions.length; i++) {
      var filterName = unquotedName.replace(filterConversions[i][0],filterConversions[i][1]);
      if (filterName != unquotedName) {
         return filterName;
      }
   }
   // TODO Remove internal spaces etc...
   // Maybe use batch preprocssing: filter.replace( /[^a-zA-Z0-9\+\-_]/g, '_' ).replace( /_+/g, '_' );
   return unquotedName.toLowerCase();
}

function convertType(rawTypeName) {
   var typeConversions = [
      [/.*flat.*/i, 'flat'],
      [/.*bias.*/i, 'bias'],
      [/.*offset.*/i, 'bias'],
      [/.*dark.*/i, 'dark'],
      [/.*light.*/i, 'light'],
      [/.*science.*/i, 'light'],
   ];
   var unquotedName = unQuote(rawTypeName);
   for (var i=0; i<typeConversions.length; i++) {
      var typeName = unquotedName.replace(typeConversions[i][0],typeConversions[i][1]);
      if (typeName != unquotedName) {
         return typeName;
      }
   }
   // TODO Remove internal spaces etc...
   // Maybe use batch preprocssing: filter.replace( /[^a-zA-Z0-9\+\-_]/g, '_' ).replace( /_+/g, '_' );
   return unquotedName.toLowerCase();
}

// --- Pattern matching and RegExp functions

function regExpToString(re) {
   if (re == null) {
      return "";
   } else {
   // Remove lading and trainling slahes
      var reString = re.toString();
      return  reString.substring(1, reString.length-1);
   }
}

// Parsing the keywords in the targetFileNamePattern (1 characters will be removed at
// head (&) and tail (;), this is hard coded and must be modified if required
var variableRegExp = /&[a-zA-Z0-9]+;/g;




// --- Variable handling

// Extract the variables to form group names and file names from the file name, FITS keywords
function extractVariables(inputFile, keys) {

   var inputFileName =  File.extractName(inputFile);

   var variables = [];

   //   &binning     Binning from XBINNING and YBINNING as integers, like 2x2.
   var xBinning =parseInt(findKeyWord(keys,'XBINNING'));
   var yBinning =parseInt(findKeyWord(keys,'YBINNING'));
   variables['binning'] = xBinning.toFixed(0)+"x"+yBinning.toFixed(0);

   // 'count' and 'rank' depends on the order, will be recalculated when files are processed,
   // here for documentation purpose
   variables['count'] = 0 .pad(FFM_COUNT_PAD);
   variables['rank'] = 0 .pad(FFM_COUNT_PAD);

   //   &exposure;   The exposure from EXPOSURE, as an integer (assume seconds)
   var exposure = findKeyWord(keys,'EXPOSURE');
   variables['exposure'] = parseFloat(exposure).toFixed(0);

   //   &extension;   The extension of the source file (with the dot)
   variables['extension'] = File.extractExtension(inputFile);

   //   &filename;   The file name part of the source file
   variables['filename'] = inputFileName;

   //   &filter:     The filter name from FILTER as lower case trimmed normalized name.
   var filter = findKeyWord(keys,'FILTER');
   variables['filter'] = convertFilter(filter);

   //   &temp;       The SET-TEMP temperature in C as an integer
   var temp = findKeyWord(keys,'SET-TEMP');
   variables['temp'] = parseFloat(temp).toFixed(0);

   //   &type:       The IMAGETYP normalized to 'flat', 'bias', 'dark', 'light'
   var imageType = findKeyWord(keys,'IMAGETYP');
   variables['type'] = convertType(imageType);

   //   &FITSKW;     (NOT IMPLEMENTED)


   return variables;


}


// ------------------------------------------------------------------------------------------------------------------------
// Engine
// ------------------------------------------------------------------------------------------------------------------------

function FFM_Engine() {

   // This is from the GUI
   this.outputDirectory = "";
#ifdef DEBUG
  this.outputDirectory = "C:/temp";
#endif

   // TODO Make a global parameter
   this.defaultKey = ["SET-TEMP","EXPOSURE","IMAGETYP","FILTER  ", "XBINNING","YBINNING"];


   // Variables that can be reset (when doing clear all)
   this.reset = function() {
      // Cache of file information. 3parallel arrays, the order is usually NOT the same as in the GUI
      this.inputFiles = []; //Array of filename with full path
      this.inputKeys = [];  // Array of an array of FITSKeywords for the corresponding file
      this.inputVariables = [];  // Array of Map of stable variables for the corresponding file

      // Cache global FITS key information (two parallel arrays)
      this.keyTable = [];   //accumulated names of keywords from all files
      this.keyEnabled = []; //true == selected keywords

      // Target files is a subset of the inputFiles, each file is defined by an index
      // in input file and the corresponding new file name. Unchecked files are not
      // present in the list. Initially empty. 2 parallel arrays.
      this.targetFilesIndices = [];
      this.targetFiles = [];
    }

   // -- Add a list of files
   this.addFiles = function (fileNames) {

#ifdef DEBUG
      debug("addFiles: Adding "+fileNames.length + " files");
#endif

      var nmbFilesAdded = 0;
      var nmbFilesDuplicated = 0;
      for ( var i = 0; i<fileNames.length; i++ ) {
#ifdef DEBUG
         debug("addFiles: Check for duplicate and add fileNames[" + i + "] " + fileNames[i]);
#endif
         if (this.inputFiles.indexOf(fileNames[i]) < 0) //Add file only one times
         {
            var keys = LoadFITSKeywords(fileNames[i]);
            this.inputFiles.push(fileNames[i]);
            this.inputKeys.push(keys);
            var variables = extractVariables(fileNames[i], keys);

            this.inputVariables.push(variables);
            nmbFilesAdded++;
         } else {
            nmbFilesDuplicated ++;
         }
      }

      Console.writeln("" + nmbFilesAdded + " files added, " + nmbFilesDuplicated + " duplicated file ignored");

   }

   // -- remove a file by name ----------------------------------------------------------
   this.removeFiles = function (fileName) {
      var index = this.inputFiles.indexOf(fileName);
      if (index < 0) {
         throw ("SCRIPT ERROR : removeFiles: file " + fileName + " not in inputFiles");
      }
      this.inputFiles.splice(index,1);
      this.inputKeys.splice(index,1);
      this.inputVariables.splice(index,1);
   }



   // --  Build the list of target files for the selected input files -------------------
   this.buildTargetFiles = function(listOfFiles) {

#ifdef DEBUG
      debug("buildTargetFiles: list of " + listOfFiles.length + " files");
      debug("buildTargetFiles: targeFileNamePattern = '" + guiParameters.targeFileNamePattern + "'");
      debug("buildTargetFiles: sourceFileNameRegExp = '" + guiParameters.sourceFileNameRegExp + "'");
      debug("buildTargetFiles: groupByPattern = '" + guiParameters.groupByPattern + "'");
#endif

      // Reinitialize the target files indices and mapping
      this.targetFilesIndices = [];
      this.targetFiles = [];


      // A map of group count values
      var countingGroups = {};


      // Separate directory from file name part in target pattern
      var indexOfLastSlash = guiParameters.targeFileNamePattern.lastIndexOf('/');
      if (indexOfLastSlash>0) {
         var targetDirectoryPattern= guiParameters.targeFileNamePattern.substring(0,indexOfLastSlash);
         var targetNamePattern= guiParameters.targeFileNamePattern.substring(indexOfLastSlash+1);
      } else {
         var targetDirectoryPattern = guiParameters.targeFileNamePattern;
         var targetNamePattern= '';
      }
#ifdef DEBUG
      debug("buildTargetFiles: targetDirectoryPattern = '" + targetDirectoryPattern + "', targetNamePattern = '" +  targetNamePattern + "'");
#endif

      // Initialized inside each loop, declared here for clarity
      var count = 0;
      var group = '';
      for (var i = 0; i < listOfFiles.length; ++i) {

            var inputFile = listOfFiles[i];

            var inputFileIndex = this.inputFiles.indexOf(inputFile);
            if (inputFileIndex < 0) {
               throw ("SCRIPT ERROR : buildTargetFiles: file not in inputFiles: " + inputFile + " (" + i + ")");
            }
#ifdef DEBUG
            debug("buildTargetFiles: " + i + ": processing inputFile[" + inputFileIndex + "] = " + inputFile);
#endif

            var inputFileName =  File.extractName(inputFile);

            var variables = this.inputVariables[i];
            // Method to handle replacement of variables in target file name pattern
            var replaceVariables = function(matchedSubstring, index, originalString) {
               var varName = matchedSubstring.substring(1,matchedSubstring.length-1);
               if (variables.hasOwnProperty(varName)) {
#ifdef DEBUG_VARS
                  debug("replaceVariables: match '" + matchedSubstring + "' '" + index + "' '" +  originalString + "' '" + varName + "' by '" + variables[varName] + "'");
#endif
                  return variables[varName];
               } else {
#ifdef DEBUG_VARS
                  debug("replaceVariables: match '" + matchedSubstring + "' '" + index + "' '" +  originalString + "' '" + varName + "' not found");
#endif
                  return  varName.toUpperCase();
               }
            };

            //   &rank;      The rank in the list of files of the file being moved/copied, padded to COUNT_PAD.
            variables['rank'] = i.pad(FFM_COUNT_PAD);

            // The file name part is calculated at each scan as the regxep may have been changed
            // TODO Optimize this maybe, clear the numbered variables of a previous scan
            //   &1; &2;, ... The corresponding match from the sourceFileNameRegExp
            if (guiParameters.sourceFileNameRegExp != null) {
               var inputFileNameMatch = guiParameters.sourceFileNameRegExp.exec(inputFileName);
#ifdef DEBUG
               debug ("buildTargetFiles: inputFileNameMatch= " + inputFileNameMatch);
#endif
               if (inputFileNameMatch != null) {
                  for (var j = 0; j<inputFileNameMatch.length; j++) {
                     variables[j.toString()] = inputFileNameMatch[j]
                  }
               }
            }

            // Use only directory part, count should not be used, used to initialie 'targetdir'
            variables['count'] = 'COUNT';
            var targetDirectory =  targetDirectoryPattern.replace(variableRegExp,replaceVariables);
            variables['targetDir'] = targetDirectory;

            // Expand the groupByPattern to form the id of the counter (targetDir may be used)
            group = guiParameters.groupByPattern.replace(variableRegExp, replaceVariables);
            count = 0;
            if (countingGroups.hasOwnProperty(group)) {
               count = countingGroups[group];
            }
            count ++;
            countingGroups[group] = count;
            variables['count'] = count.pad(FFM_COUNT_PAD);
#ifdef DEBUG
            debug("buildTargetFiles: group = " + group + ", count = " + count);
#endif

            // We should not use 'targetDir' in the expansion of the file name
            variables['targetDir'] = 'TARGETDIR';
            // The resulting name may include directories
            var targetString = guiParameters.targeFileNamePattern.replace(variableRegExp,replaceVariables);
#ifdef DEBUG
            debug("buildTargetFiles: targetString = " + targetString );
#endif


            // Target file but without the output directory
            this.targetFilesIndices.push(inputFileIndex);
            this.targetFiles.push(targetString);

         }
#ifdef DEBUG
         debug("buildTargetFiles: Total files: ", this.targetFiles.length);
#endif

    }

    // Check that the operations can be executed for a list of files ------------------------------
    this.checkValidTargets = function(listOfFiles) {

      var errors = [];

      // Check if files are still in the same order, otherwise the &count; may have changed
      for (var i = 0; i < listOfFiles.length; ++i) {
         var inputFile = listOfFiles[i];
         var inputFileIndex = this.inputFiles.indexOf(inputFile);
         if (inputFileIndex < 0) {
            throw ("SCRIPT ERROR : check: file not in inputFiles: " + inputFile + " (" + i + ")");
         }
         if (this.targetFilesIndices.length<i ||
            this.targetFilesIndices[i] != inputFileIndex) {
            // Sort order changed
            return ["The order of some column changed since last refresh, please refresh"];
         }
      }

      for (var i = 0; i < listOfFiles.length; ++i) {
         var inputFile = listOfFiles[i];
         if (! File.exists(inputFile)) {
            errors.push("File '"  + inputFile + "' is not present any more, please refresh'");
         }
      }

      // Check duplicates target names
      var targetFileNameInputFile = {};
      for (var i=0; i<this.targetFiles.length; i++) {
         var index = this.targetFilesIndices[i];
         var targetString = this.targetFiles[i];
         var inputFile = this.inputFiles[index];
         if (targetFileNameInputFile.hasOwnProperty(targetString)) {
            errors.push("File '"  + inputFile + "' generates same file '" + targetString + "' as '" + targetFileNameInputFile[targetString] +"'");
         }
         targetFileNameInputFile[targetString] = inputFile;
      }

      // Check bad names (empty, /, ...)

      // Check existing target files
      for (var i=0; i<this.targetFiles.length; i++) {
         var index = this.targetFilesIndices[i];
         var targetString = this.targetFiles[i];
         var inputFile = this.inputFiles[index];
         var targetFilePath = this.outputDirectory + "/" + targetString;
         if (File.exists(targetFilePath)) {
            errors.push("File '"  + inputFile + "' generates the already existing file '" + targetFilePath + "'");
         }
      }

      return errors;
    }



    // -- Make List of text accumulating the transformation rules for display --------------
    this.makeListOfTransforms = function() {
      var listOfTransforms = [];
      for (var i = 0; i<this.targetFiles.length; i++) {
         var index = this.targetFilesIndices[i];
         var inputFile = this.inputFiles[index];
         listOfTransforms.push("File ".concat(inputFile, "\n  to .../",this.targetFiles[i], "\n"));
      }
      return listOfTransforms;
    }


   // -- Execute copy or move operation ----------------------------------------------------
   this.executeFileOperations = function (engine_mode) {

      var count = 0;

      for (var i=0; i<this.targetFiles.length; i++) {

            var index = this.targetFilesIndices[i];
            var targetString = this.targetFiles[i];
            var inputFile = this.inputFiles[index];

            var targetFile = this.outputDirectory + "/" + targetString;

#ifdef DEBUG
            debug("executeFileOperations: targetFile = " + targetFile );
#endif
            var targetDirectory = File.extractDrive(targetFile) +  File.extractDirectory(targetFile);
#ifdef DEBUG
            debug("executeFileOperations: targetDirectory = " + targetDirectory );
#endif

            // Create target directory if required
            if (!File.directoryExists(targetDirectory)) {
               console.writeln("mkdir " + targetDirectory);
               if (EXECUTE_COMMANDS) File.createDirectory(targetDirectory, true);
            }

            // TO BE ON SAFE SIDE
            if (File.exists(targetFile)) {
            for ( var u = 1; ; ++u )  {
               for( var n = u.toString(); n.length < 4 ; n = "0" + n);
               // TODO This does not take 'extension' into account
                  var tryFilePath = File.appendToName( targetFile, '-' + n );
#ifdef DEBUG
                  debug("executeFileOperations: tryFilePath= " + tryFilePath );
#endif
                  if ( !File.exists( tryFilePath ) ) { targetFile = tryFilePath; break; }
               }
            }

            if (engine_mode==0) {
               console.writeln("move " + inputFile +"\n  to "+ targetFile);
               if (EXECUTE_COMMANDS) File.move(inputFile,targetFile);
            } else {
               console.writeln("copy " + inputFile+"\n  to "+ targetFile);
               if (EXECUTE_COMMANDS)  copyFile(inputFile,targetFile);
            }
            count ++;

            // To allow abort ?
            processEvents();

         }
         //console.writeln("Total files: ", this.inputFiles.length,"; Processed: ",count);

   };

   // -- Export the keywords of a list of files
   this.exportFITSKeyWords = function() {
      var tab = String.fromCharCode(9);
      var f = new File();
      var fileName = "FITS_keys";
      var fileDir = this.outputDirectory;
      var t = fileDir + "/" + fileName + ".txt";
      // Create numbered file nameto create new file
      if ( File.exists( t ) ) {
         for ( var u = 1; ; ++u ) {
            for( var n = u.toString(); n.length < 4 ; n = "0" + n);
            var tryFilePath = File.appendToName( t, '-' + n );
            if ( !File.exists( tryFilePath ) ) { t = tryFilePath; break; }
         }
      }
      f.create(t);

      //output header (tab separated selected fits keyword + 'Filename')
      for ( var i =0; i<this.keyTable.length; i++) {
         if (!this.keyEnabled[i]) continue;
         f.outTextLn(this.keyTable[i]+tab);
      }
      f.outTextLn("Filename"+String.fromCharCode(10,13));

      //output FITS data
      for ( var j =0; j< this.targetFilesIndices.length; j++) {
         var inputIndex = this.targetFilesIndices[i];

         var key = this.inputKeys[inputIndex];
         for ( var i = 0; i< this.keyTable.length; i++) {
            if (!this.keyEnabled[i]) continue;
            var name = this.keyTable[i];
            for (var k in key) {
               if (!(key[k].name == name)) continue;
               if (key[k].isNumeric) {
                  var value = parseFloat(key[k].value)
               } else {
                  var value = key[k].value;
                  value = value.replace( /'/g, "" );
                  value = value.replace( / /g, "" ); //delete left space
                  value = value.replace( /:/g, "." );
               }

               f.outText(value.toString());
               for (var w = value.toString().length; w < 8; w++) f.outText(" ");
               f.outText(tab);
               k=-1;
               break;
            }
            if (k > -1) f.outText("        "+tab);
         }
         f.outTextLn(this.inputFiles[j]+String.fromCharCode(10,13));
      }
      f.close();
      console.writeln("FITSKeywords saved to ",t);
   }

   // -- Return true if move or copy is possible
   this.canDoOperation = function() {
         return !((!this.inputFiles.length) || (!this.outputDirectory));
   }



   this.reset();
}


// ------------------------------------------------------------------------------------------------------------------------
// GUI
// ------------------------------------------------------------------------------------------------------------------------

function MyDialog(engine)
{
   this.__base__ = Dialog;
   this.__base__();
   this.engine = engine;


   //------------------------------------------------------------
   this.onShow = function() {
//      this.filesAdd_Button.onClick();
   }

   //hide columns of unchecked keywords---------------------------
   // TODO Repai
   this.hideKey = function () {
      //if (DEBUGGING_MODE_ON) console.clear();

      for (var i = 0; i<this.engine.keyEnabled.length;i++) {
         var c = i + 1;
         // debug("Column: " + i + " "  + c + " enabled: " + this.keyEnabled[parseInt(i)] + " " );
         // console.writeln(" *** " + i + " " + typeof i + " " + typeof this.keyEnabled[i] + " " + this.files_TreeBox.numberOfColumns);

         // TODO Does not work unles debug is enabled
         this.files_TreeBox.showColumn( c, this.engine.keyEnabled[i]);
      }
   }

   //----------------------------------------------------------------------------------
   // KeyWord Dialog
   this.SD = new KeyDialog( this );
   this.Key_button = new ToolButton( this );
   with ( this.Key_button )
   {
      icon = new Bitmap( ":/images/icons/text.png" );
      toolTip = "KeyWord Dialog";
      onClick = function()
      {
         if (this.dialog.engine.keyTable.length)
         {
            this.dialog.SD.execute();
            this.dialog.hideKey();
         }
      }
   }

   //----------------------------------------------------------
   // File List TreeBox
   this.files_TreeBox = new TreeBox( this );
   with ( this.files_TreeBox )
   {
      rootDecoration = false;
      numberOfColumns = 1;
      multipleSelection = true;
      headerVisible = true;
      headerSorting = true;
      setHeaderText(0, "Filename");
      sort(0,true);

      setMinSize( 400, 200 );

      // Assume that 'check' is the only operation that update the nodes,
      // this may not be true...
      onNodeUpdated = function( node, column ) // Invert CheckMark
      {

#ifdef DEBUG_EVENTS
         debug("files_TreeBox: onNodeUpdated("+node+","+column+")");
#endif
         for (var i=0; i < this.selectedNodes.length; i++)
         {
            if ( node === this.selectedNodes[i] ) continue; // skip curent clicked node, because it will inverted automaticaly
            this.selectedNodes[i].checked = !this.selectedNodes[i].checked;
         }
         this.dialog.refreshTargetFiles();
      };
#ifdef DEBUG_EVENTS
      onCurrentNodeUpdated = function(node) {
         debug("files_TreeBox: onCurrentNodeUpdated("+node+")");
      };
      onNodeActivated = function(node) {
         debug("files_TreeBox: onNodeActivated("+node+")");
      };
      onNodeClicked = function(node) {
         debug("files_TreeBox: onNodeClicked("+node+")");
      };
      onNodeCollapsed = function(node) {
         debug("files_TreeBox: onNodeCollapsed("+node+")");
      };
      onNodeDoubleClicked = function(node) {
         debug("files_TreeBox: onNodeDoubleClicked("+node+")");
      };
      onNodeEntered = function(node) {
         // this is not called unless mouse events are enabled
         debug("files_TreeBox: onNodeEntered("+node+")");
      };
      onNodeExpanded = function(node) {
         debug("files_TreeBox: onNodeExpanded("+node+")");
      };
      onNodeSelectionUpdated = function() {
         debug("files_TreeBox: onNodeSelectionUpdated()");
      };
#endif
   }



   //----------------------------------------------------------
   // Rebuild the TreeBox content
   this.rebuildFilesTreeBox = function ()
   {
      this.files_TreeBox.clear();

      // TODO IN ENGINE
      this.engine.keyTable = []; // clear
      this.engine.keyEnabled = []; // clear

      // Accumulate all KeyName in keyTabe
      for (var i = 0; i < this.engine.inputFiles.length; ++i) {
         var keys = this.engine.inputKeys[i]; // keywords of one file

         // Create TreeBoxNode for file
         var node = new TreeBoxNode( this.files_TreeBox );
         //write name of the file to first column
         node.setText( 0, this.engine.inputFiles[i] );
         node.checked = true;

#ifdef DEBUG
         debug("rebuildFilesTreeBox: adding " + keys.length + " column data");
#endif
         for ( var j = 0; j<keys.length; j++) {
            var name = keys[j].name; //name of Keyword from file
            var k = this.engine.keyTable.indexOf(name);// find index of "name" in keyTable
            if (k < 0)  {
               // new keyName
#ifdef DEBUG_COLUMNS
               debug("rebuildFilesTreeBox: Creating new column " + name + " at " + this.files_TreeBox.numberOfColumns);
#endif
               this.engine.keyTable.push(name);//add keyword name to table
               this.files_TreeBox.numberOfColumns++;// add new column
               this.files_TreeBox.setHeaderText(this.engine.keyTable.length, name);//set name of new column
               //console.writeln("*** " + this.files_TreeBox.numberOfColumns + " " + name);
               this.engine.keyEnabled.push (this.engine.defaultKey.indexOf(name)> -1);//compare with default enabled keywords
               k = this.engine.keyTable.length-1;

               //this.files_TreeBox.showColumn( this.files_TreeBox.numberOfColumns, this.keyEnabled[k]);
            }
            // TODO Supports other formatting (dates ?) or show raw text
            if (keys[j].isNumeric) {
               node.setText( k+1, Number(keys[j].value).toFixed(3) );
            } else {
               node.setText( k+1, keys[j].value.trim() );
            }
         }
      }
      this.hideKey(); //hide the columns of unchecked FITS keywords
   }

   //---------------------------------------------------------------------------------------
   // Add a list of files to the TreeBox (remove duplicates)
   this.addFilesAction = function (fileNames)
   {
      this.engine.addFiles(fileNames);

      this.rebuildFilesTreeBox();
      this.QTY.text = "Total files: " + this.engine.inputFiles.length;
      this.setMinWidth(800);
      this.adjustToContents();
      this.dialog.updateButtonState();

      this.refreshTargetFiles();
      //this.hideKey(); // *** TEST
   }

   // Total file Label ---------------------------------------------------------------------------
   this.QTY = new Label( this );
   this.QTY.textAlignment = TextAlign_Right|TextAlign_VertCenter;

   //enable/disable buttons
   this.updateButtonState = function()
   {
      var enabled = this.dialog.engine.canDoOperation();
      this.dialog.move_Button.enabled = enabled;
      this.dialog.copy_Button.enabled = enabled;
      this.dialog.txt_Button.enabled = enabled;
   }

   // Add files ---------------------------------------------------------------------------
   this.filesAdd_Button = new ToolButton( this );
   with ( this.filesAdd_Button )
   {
      icon = new Bitmap( ":/images/image_container/add_files.png" );
      toolTip = "Add files";
      onClick = function()
      {
         var ofd = new OpenFileDialog;
         ofd.multipleSelections = true;
         ofd.caption = "Select FITS Files";
         ofd.filters = [["FITS Files", "*.fit", "*.fits", "*.fts"]];
         if ( ofd.execute() ) {
            this.dialog.addFilesAction(ofd.fileNames);
         }
      }
   }


   // Add Dir ---------------------------------------------------------------------------
   this.dirAdd_Button = new ToolButton( this );
   with ( this.dirAdd_Button )
   {
      icon = new Bitmap( ":/images/icons/folders.png" );
      toolTip = "Add folder including subfolders";
      onClick = function()
      {
         var gdd = new GetDirectoryDialog;
         //gdd.initialPath = outputDirectory;
         gdd.caption = "Select Input Directory";
         if ( gdd.execute() )
         {
#ifdef DEBUG
            debug("Start searching FITS file in SubFolders");
#endif
            // TODO Make configurable
            var fileNames = searchDirectory(gdd.directory+"/*.fit" ,true)
            .concat(searchDirectory(gdd.directory+"/*.fits",true))
            .concat(searchDirectory(gdd.directory+"/*.fts",true));
#ifdef DEBUG
            debug("Finish searching FITS file in SubFolders");
#endif
            this.dialog.addFilesAction(fileNames);
         }
      }
   }

   // Close selected files ---------------------------------------------------------------------------
   this.files_close_Button = new ToolButton( this );
   with ( this.files_close_Button )
   {
      icon = new Bitmap( ":/images/close.png" );
      toolTip = "<p>Removed selected images from the list.</p>";
      onClick = function()
      {
#ifdef DEBUG
         debug("Remove files");
#endif

         for ( var iTreeBox = this.dialog.files_TreeBox.numberOfChildren; --iTreeBox >= 0; )
         {

            if ( this.dialog.files_TreeBox.child( iTreeBox ).selected )
            {
               var nameInTreeBox = this.dialog.files_TreeBox.child(iTreeBox).text(0);

               this.dialog.engine.removeFiles(nameInTreeBox);
               this.dialog.files_TreeBox.remove( iTreeBox );
            }
         }
         this.dialog.QTY.text = "Total files: " + this.dialog.engine.inputFiles.length;
         this.dialog.updateButtonState();
         // Refresh the generated files
         this.dialog.refreshTargetFiles();

      }
   }

   // Close all files ---------------------------------------------------------------------------
   this.files_close_all_Button = new ToolButton( this );
   with ( this.files_close_all_Button )
   {
      icon = new Bitmap( ":/images/close_all.png" );
      toolTip = "<p>Removed all images from the list.</p>";
      onClick = function()
      {
#ifdef DEBUG
         debug("Remove all files (" + this.dialog.engine.inputFiles.length + ")");
#endif

         // TODO We can probably clear in one go
         for ( var i = this.dialog.files_TreeBox.numberOfChildren; --i >= 0; )
         {
               this.dialog.files_TreeBox.remove( i );
         }
         this.dialog.engine.reset();
         this.dialog.updateButtonState();
         // Refresh the generated files
         this.dialog.refreshTargetFiles();

      }
   }



   // Target pattern --------------------------------------------------------------------------------------
   this.targetFilePattern_Edit = new Edit( this );
   with ( this.targetFilePattern_Edit )
   {
      text = guiParameters.targeFileNamePattern;
      toolTip = TARGET_PATTERN_TOOLTIP;
      enabled = true;
      onTextUpdated = function()
      {
         guiParameters.targeFileNamePattern = text;
         this.dialog.refreshTargetFiles();
      }
   }

   // Source file name pattern --------------------------------------------------------------------------------------
   this.sourcePattern_Edit = new Edit( this );
   with ( this.sourcePattern_Edit )
   {
      text = regExpToString(guiParameters.sourceFileNameRegExp);
      toolTip = SOURCE_FILENAME_REGEXP_TOOLTIP;
      enabled = true;
      onTextUpdated = function()
      {
         var re = this.text.trim();
         if (re.length == 0) {
            guiParameters.sourceFileNameRegExp = null;
#ifdef DEBUG
            debug("sourcePattern_Edit: onTextUpdated:- cancel regexp");
#endif
         } else {
            try {
               guiParameters.sourceFileNameRegExp = RegExp(re);
               this.textColor = 0;
#ifdef DEBUG
               debug("sourcePattern_Edit: onTextUpdated: regexp: " + guiParameters.sourceFileNameRegExp);
#endif
            } catch (err) {
               guiParameters.sourceFileNameRegExp = null;
               this.textColor = 0xFF0000;
#ifdef DEBUG
               debug("sourcePattern_Edit: onTextUpdated:  bad regexp - err: " + err);
#endif
            }
         }
         // Refresh the generated files
         this.dialog.refreshTargetFiles();
      }
   }

   // Group pattern --------------------------------------------------------------------------------------
   this.groupPattern_Edit = new Edit( this );
   with ( this.groupPattern_Edit )
   {
      text = guiParameters.groupByPattern;
      toolTip = GROUP_PATTERN_TOOLTIP;
      enabled = true;
      onTextUpdated = function()
      {
         guiParameters.groupByPattern = text;
         this.dialog.refreshTargetFiles();
      }
   }





   //Output Dir --------------------------------------------------------------------------------------
   this.outputDir_Edit = new Edit( this );
   this.outputDir_Edit.readOnly = true;
   this.outputDir_Edit.text = this.engine.outputDirectory;
   this.outputDir_Edit.toolTip ="select output directory.";

   this.outputDirSelect_Button = new ToolButton( this );
   with ( this.outputDirSelect_Button )
   {
      icon = new Bitmap( ":/images/icons/select.png" );
      toolTip = "Select output directory";
      onClick = function()
      {
         var gdd = new GetDirectoryDialog;
         gdd.initialPath = this.engine.outputDirectory;
         gdd.caption = "Select Output Directory";
         if ( gdd.execute() )
         {
            this.engine.outputDirectory = gdd.directory;
            this.dialog.outputDir_Edit.text = this.engine.outputDirectory;
            this.dialog.updateButtonState();
         }
      }
   }

   // Source file name pattern --------------------------------------------------------------------------------------
   this.transform_TextBox = new TextBox( this );
   with ( this.transform_TextBox )
   {
      text = '';
      toolTip = "Transformations that will be executed";
      enabled = true;
      readOnly = true;
   }

   // ===================================================================================
   //

   this.makeListOfCheckedFiles = function() {
      var listOfFiles = [];

      for (var iTreeBox = 0; iTreeBox < this.files_TreeBox.numberOfChildren; ++iTreeBox) {

         if ( this.files_TreeBox.child(iTreeBox).checked ) {
            // Select name in tree box, find corresponding file in inputFiles
            var nameInTreeBox = this.files_TreeBox.child(iTreeBox).text(0);
            listOfFiles.push(nameInTreeBox);
         }
      }

      return listOfFiles;
   }


   this.refreshTargetFiles = function() {

#ifdef DEBUG
      debug("refreshTargetFiles() called");
#endif

      var listOfFiles = this.makeListOfCheckedFiles();

      this.engine.buildTargetFiles(listOfFiles);

      // List of text accumulating the transformation rules for display
      var listOfTransforms = this.engine.makeListOfTransforms();
      this.transform_TextBox.text = listOfTransforms.join("");

    }

    this.removeDeletedFiles = function() {
      for ( var iTreeBox = this.dialog.files_TreeBox.numberOfChildren; --iTreeBox >= 0; ) {

         var nameInTreeBox = this.dialog.files_TreeBox.child(iTreeBox).text(0);
         if (!File.exists(nameInTreeBox)) {

            this.dialog.engine.removeFiles(nameInTreeBox);
            this.dialog.files_TreeBox.remove( iTreeBox );
         }

         this.dialog.QTY.text = "Total files: " + this.dialog.engine.inputFiles.length;
         this.dialog.updateButtonState();
         // Caller must refresh the generated files
       }
    }


   //Engine buttons --------------------------------------------------------------------------------------
   this.check_Button = new PushButton( this );
   with ( this.check_Button ) {
      text = "Check validity";
      toolTip = "Check that the target files are valid\nthis is automatically done before any other operation";
      enabled = true;
      onClick = function()
      {
         var listOfFiles = parent.makeListOfCheckedFiles();
         var errors = parent.engine.checkValidTargets(listOfFiles);
         if (errors.length > 0) {
            var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
            msg.execute();
         } else {
            var msg = new MessageBox("Check ok",
            "Check successfull", StdIcon_Information, StdButton_Ok );
             msg.execute();
         }
      }
   }

   this.move_Button = new PushButton( this );
   with ( this.move_Button ) {
      text = "Move files";
      toolTip = "Move Checked files to output directory";
      enabled = false;
      onClick = function()
      {
         var listOfFiles = parent.makeListOfCheckedFiles();
         var errors = parent.engine.checkValidTargets(listOfFiles);
         if (errors.length > 0) {
            var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
            msg.execute();
            return;
         }
         parent.engine.executeFileOperations(0);
         parent.removeDeletedFiles();
         parent.refreshTargetFiles();
         //this.dialog.ok();
         // TODO Refresh source
      }
   }

   this.refresh_Button = new PushButton( this );
   with ( this.refresh_Button ) {
      text = "Refresh list";
      toolTip = "Refresh the list of operations\nrequired after a sort on an header (there is on onSort event)";
      enabled = true;
      onClick = function()
      {
         parent.removeDeletedFiles();
         parent.refreshTargetFiles();
      }
   }

   this.copy_Button = new PushButton( this );
   with ( this.copy_Button ) {
      text = "Copy files";
      toolTip = "Copy Checked files to output directory";
      enabled = false;
      onClick = function()
      {
         var listOfFiles = parent.makeListOfCheckedFiles();
         var errors = parent.engine.checkValidTargets(listOfFiles);
         if (errors.length > 0) {
            var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
            msg.execute();
            return;
         }
         parent.engine.executeFileOperations(1);
         //this.dialog.ok();
      }
   }

   // Export selected fits keywords for checked files
   this.txt_Button = new PushButton( this );
   with ( this.txt_Button ) {
      text = "Export FITS.txt";
      toolTip = "For Checked files write FitKeywords value to file FITS.txt in output directory";
      enabled = false;
      onClick = function() {
         parent.engine.exportFITSKeyWords();
      }

   }


   //Sizer------------------------------------------------------------

   this.fileButonSizer = new HorizontalSizer;
   with ( this.fileButonSizer )
   {
      margin = 6;
      spacing = 4;
      add( this.Key_button );
      add( this.filesAdd_Button );
      add( this.dirAdd_Button );
      add( this.files_close_Button );
      add( this.files_close_all_Button );
      add( this.QTY );
      addStretch();
   }

   this.inputFiles_GroupBox = new GroupBox( this );
   with (this.inputFiles_GroupBox)
   {
      title = "Input";
      sizer = new VerticalSizer;
      sizer.margin = 6;
      sizer.spacing = 4;
      sizer.add( this.files_TreeBox,100 );
      sizer.add( this.fileButonSizer );
   }

   this.targetFilePattern_Edit_sizer = new HorizontalSizer;
   with (this.targetFilePattern_Edit_sizer) {
      margin = 4;
      spacing = 2;
      var label = new Label();
      label.minWidth			= 100;
		label.text		= "Target file pattern: ";
		label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

      add( label );
      add( this.targetFilePattern_Edit );
   }

   this.sourcePattern_Edit_sizer = new HorizontalSizer;
   with (this.sourcePattern_Edit_sizer) {
      margin = 4;
      spacing = 2;
      var label = new Label();
      label.minWidth			= 100;
		label.text		= "File name RegExp: ";
		label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

      add( label );
      add( this.sourcePattern_Edit );
   }

   this.groupPattern_Edit_sizer = new HorizontalSizer;
   with (this.groupPattern_Edit_sizer) {
      margin = 4;
      spacing = 2;
      var label = new Label();
      label.minWidth			= 100;
		label.text		= "Group pattern: ";
		label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

      add( label );
      add( this.groupPattern_Edit );
   }


   this.rules_GroupBox = new GroupBox( this );
   with (this.rules_GroupBox)
   {
      title = "Rules";

      sizer = new VerticalSizer;
      sizer.margin = 6;
      sizer.spacing = 4;

      sizer.add( this.targetFilePattern_Edit_sizer, 100);
      sizer.add( this.sourcePattern_Edit_sizer );
      sizer.add( this.groupPattern_Edit_sizer );
   }


   this.outputDir_GroupBox = new GroupBox( this );
   with (this.outputDir_GroupBox)
   {
      title = "Output base directory";
      sizer = new HorizontalSizer;
      sizer.margin = 6;
      sizer.spacing = 4;
      sizer.add( this.outputDir_Edit, 100 );
      sizer.add( this.outputDirSelect_Button );
   }


   this.sizer2 = new HorizontalSizer;
   with ( this.sizer2 )
   {
      spacing = 2;
      add( this.refresh_Button);
      add( this.check_Button);
      add( this.move_Button);
      add( this.copy_Button);
      add( this.txt_Button);
      addStretch();
   }


   this.sizer = new VerticalSizer;
   with ( this.sizer )
   {
      margin = 2;
      spacing = 2;
      add( this.inputFiles_GroupBox );
      add(this.rules_GroupBox);
      add( this.outputDir_GroupBox );
      add(this.transform_TextBox);
      add( this.sizer2 );
   }
   //this.move(50,100); // move dialog to up-left corner

}
//End if Main Dialog------------------------------------------------------------

// ---------------------------------------------------------------------------------------------------------

// Present a dialog with:
//   A seclection of the files (drop down)
//   A list of  FITS keywords (selection box, keyword, value)  of the selected file, as a TreeBox
//Second Dialog------------------------------------------------------------
function KeyDialog( pd ) //pd - parentDialog
{
   this.__base__ = Dialog;
   this.__base__();
   this.windowTitle = "Select KeyWords";

   this.onShow = function()
   {
      var p = new Point( pd.position );
      p.moveBy( 16,16 );
      this.position = p;

      this.keyword_TreeBox.clear();

      var testRootNode = new TreeBoxNode(this.keyword_TreeBox);
      testRootNode.expanded = true;
      testRootNode.setText(0,"FITS keywords");


      // Fill list of keywords from parent keyTable
      for (var i =0; i<pd.engine.keyTable.length; i++) {
         // TEST var node = new TreeBoxNode(this.keyword_TreeBox);
         var node = new TreeBoxNode(testRootNode);
         node.setText( 0, pd.engine.keyTable[i] );
         node.checked = pd.engine.keyEnabled[i];
      }

      // Fill list of files from parent list of files
      this.file_ComboBox.clear();
      for (i = 0; i< pd.engine.inputFiles.length; i++) {
         this.file_ComboBox.addItem(pd.engine.inputFiles[i]);
      }
      this.file_ComboBox.onItemSelected(0);
      this.setMinSize(700,600);
   }

   // Save list of selected keywords in parent keyEnabled array
   this.onHide = function()
   {
#ifdef DEBUG
          debug("file_ComboBox: onHide");
#endif
      var fitsParentNode = this.keyword_TreeBox.child(0);
      for (var i =0; i<pd.engine.keyTable.length; i++) {
         checked = fitsParentNode.child(i).checked;
#ifdef DEBUG
         // debug("KeyDialog: Key#= " + parseInt(i) + " checked= " + checked );
#endif
         pd.engine.keyEnabled[i] = checked;
      }
      pd.setMinWidth(800);
   }


   // FITS keyword combox box for file selection
   this.file_ComboBox = new ComboBox( this );
   with ( this.file_ComboBox )
   {
      onItemSelected = function( index )
      {
         // Assume that index in combox is same as index in inputfiles
#ifdef DEBUG
          debug("file_ComboBox: onItemSelected - " + index + " key table length = " + pd.engine.keyTable.length);
#endif
        // TEST var fitsParentNode = parent.keyword_TreeBox;
        var fitsParentNode = parent.keyword_TreeBox.child(0);

         for (var i = 0; i<pd.engine.keyTable.length; i++) {
            // Copying from original treebox (assume all in same order)
           //  var keyValue = pd.files_TreeBox.child(index).text(i+1);

            var keyName = pd.engine.keyTable[i];
            var keys = pd.engine.inputKeys[index];
            var keyWord = null;
            for (var j = 0; j<keys.length; j++) {
               if (keys[j].name === keyName) {
                  keyWord = keys[j];
                  break;
               }
            }
#ifdef DEBUG_FITS
            debug("file_ComboBox: onItemSelected - keyName=" + keyName + ",  keyWord=" + keyWord );
#endif
            if (keyWord != null) {
               fitsParentNode.child(i).setText(1,keyWord.value);
               fitsParentNode.child(i).setText(2,keyWord.comment);
            }
         }
      }

   }

   //----------------------------------------------------------
   // FITS keyword List TreeBox
   this.keyword_TreeBox = new TreeBox( this );
   with ( this.keyword_TreeBox )
   {
      toolTip = "Checkmark to include to report";
      rootDecoration = false;
      numberOfColumns = 2;
      setHeaderText(0, "name");
      setHeaderText(1, "value");
      setHeaderText(2, "comment");
      setColumnWidth(0,100);
      setColumnWidth(1,200);
      setColumnWidth(2,600);
   }

   // Assemble FITS keyword Dialog
   this.sizer = new VerticalSizer;
   this.sizer.margin = 4;
   this.sizer.spacing = 4;
   this.sizer.add( this.file_ComboBox );
   this.sizer.add( this.keyword_TreeBox );
   this.adjustToContents();
}

// TODO Should be in main()
guiParameters = new FFM_GUIParameters();
guiParameters.loadSettings();

var engine = new FFM_Engine();

MyDialog.prototype = new Dialog;
KeyDialog.prototype = new Dialog;
var dialog = new MyDialog(engine);
dialog.execute();
guiParameters.saveSettings();

