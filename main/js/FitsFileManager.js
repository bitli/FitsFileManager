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

// Set to false when doing hasardous developments...
#define EXECUTE_COMMANDS true


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
// Global refactoring (better MVC structure)
// Speed up if large number of files
// Save parameters, keep list of recent patterns used
// Option for handling of minus and other special characters for form file name being valid PI ids
// Default output directory same as input if no putput directory specified
// Mark in error if duplicate or already exists
// Add FITS keywords as variables, with formatting options
// Add optional indicator to accept missing values '?' and default value
// Add sequence of optional text to ignore if missing variable value ()
// Support specification of an order for 'count'
// Hide common header part of source folders to make file name more visible
// Add a way to use directory of source file as variable  &filedir, &filedirparent for pattern matching and group names
// Support date formatting, number formatting
// Create a log file for record the source files
// Ensure source is refreshed in case of move
// Request confirmation for move
// Add 'clear all' icon in file list
// Possibility to add FITS keywords to copied files (for example original file name)
// Correct bug when manipulating check boxes
// Mark files with missing keywords uncheked (unless optional)
// Allow to open selected files

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

#define FFM_DEFAULT_SOURCE_FILENAME_REGEXP /([^-_.]+)[._-]/

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

   var keywords = new Array;
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

      // Perform a naive sanity check: a valid FITS file must begin with a SIMPLE=T keyword.
      if ( keywords.length == 0 )
         if ( name != "SIMPLE  " && value.trim() != 'T' )
            throw new Error( "File does not seem a valid FITS file: " + fitsFilePath );

      // Add new keyword. Note: use FITSKeyword with PI >= 1.6.1
      keywords.push( new FITSKeyword( name.toString(), value.toString(), comment.toString() ) );
   }
   f.close();
   return keywords;
}


 function findKeyWord(keys, name) {
   // keys = all keywords from current file
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
   var filter = findKeyWord(keys,'FILTER  ');
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

//----------------------------------------------------------------------------
function MyDialog()
{
   this.__base__ = Dialog;
   this.__base__();
   this.inputFiles = []; //Array of filename with full path
   this.inputKeys = [];  //keys of all files. Big RAW array of all file keys.
   this.inputVariables = [];  //variables of all files (derived from FITS). Big RAW array of all variables map.
   this.keyTable = [];   //accumulated names of keywords from all files
   this.keyEnabled = []; //true == selected keywords
   this.defaultKey = ["SET-TEMP","EXPOSURE","IMAGETYP","FILTER  ", "XBINNING","YBINNING"];
   var outputDirectory = "";
   this.engine_mode = 0;       //0=move, 1=copy

#ifdef DEBUG
   outputDirectory = "C:/temp";
#endif

   //------------------------------------------------------------
   this.onShow = function() {
//      this.filesAdd_Button.onClick();
   }

   //hide columns of unchecked keywords---------------------------
   // TODO Repai
   this.hideKey = function () {
      //if (DEBUGGING_MODE_ON) console.clear();

      for (var i in this.keyEnabled) {
         var c = parseInt(i) + 1;
         // debug("Column: " + i + " "  + c + " enabled: " + this.keyEnabled[parseInt(i)] + " " );
         // console.writeln(" *** " + i + " " + typeof i + " " + typeof this.keyEnabled[i] + " " + this.files_TreeBox.numberOfColumns);

         // TODO Does not work unles debug is enabled
         // this.files_TreeBox.showColumn( c, this.keyEnabled[i]);
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
         if (this.dialog.keyTable.length)
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
         this.dialog.buildTargetFiles();
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
   this.UpdateTreeBox = function ()
   {
      this.files_TreeBox.clear();

      this.keyTable = []; // clear

      // Accumulate all KeyName in keyTabe
      for (var i = 0; i < this.inputFiles.length; ++i) {
         var keys = this.inputKeys[i]; // keywords of one file

          // Create TreeBoxNode for file
         var node = new TreeBoxNode( this.files_TreeBox );
         node.setText( 0, this.inputFiles[i] ); //write name of the file to first column
         node.checked = true;


         for ( var j in keys )
         {
            var name = keys[j].name; //name of Keyword from file
            var k = this.keyTable.indexOf(name);// find index of "name" in keyTable
            if (k < 0) // new keyName
            {
               this.keyTable.push(name);//add keyword name to table
               this.files_TreeBox.numberOfColumns++;// add new column
               this.files_TreeBox.setHeaderText(this.keyTable.length, name);//set name of new column
               //console.writeln("*** " + this.files_TreeBox.numberOfColumns + " " + name);
               k = this.keyTable.length-1;
               this.keyEnabled[k] = (this.defaultKey.indexOf(name)> -1);//compare with defauld enabled keywords

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
   this.addFiles = function (fileNames)
   {

#ifdef DEBUG
      debug("addFiles: Adding "+fileNames.length + " files");
#endif

      var qtyNew = 0;
      for ( var i = 0; i<fileNames.length; i++ )
      {
#ifdef DEBUG
         debug("addFiles: Check and add [" + i + "] " + fileNames[i]);
#endif
         if (this.inputFiles.indexOf(fileNames[i]) < 0) //Add file only one times
         {
            var keys = LoadFITSKeywords(fileNames[i]);
            this.inputFiles.push(fileNames[i]);
            this.inputKeys.push(keys);
            var variables = extractVariables(fileNames[i], keys);

            this.inputVariables.push(variables);
            qtyNew++;
         }
      }
      if (qtyNew == 0) {
         console.writeln("No new files");
         return;
      }
#ifdef DEBUG
      debug("addFiles: New " + qtyNew +"\nTotal " +this.inputFiles.length);
#endif
      this.UpdateTreeBox();
      this.QTY.text = "Total files: " + this.inputFiles.length;
      this.setMinWidth(800);
      this.adjustToContents();
      this.dialog.updateButtonState();

      this.buildTargetFiles();
      //this.hideKey(); // *** TEST
   }

   // Total file Label ---------------------------------------------------------------------------
   this.QTY = new Label( this );
   this.QTY.textAlignment = TextAlign_Right|TextAlign_VertCenter;

   //enable/disable buttons
   this.updateButtonState = function()
   {
      var enabled = !((!this.inputFiles.length) || (!outputDirectory));
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
         if ( ofd.execute() ) this.dialog.addFiles(ofd.fileNames);
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
            this.dialog.addFiles(fileNames);
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
         if ( this.dialog.inputFiles.length == 0 ) return;

         for ( var iTreeBox = this.dialog.files_TreeBox.numberOfChildren; --iTreeBox >= 0; )
         {

            if ( this.dialog.files_TreeBox.child( iTreeBox ).selected )
            {
               var nameInTreeBox = this.dialog.files_TreeBox.child(iTreeBox).text(0);
               var i = this.dialog.inputFiles.indexOf(nameInTreeBox);
               if (i < 0) {
                  throw ("SCRIPT ERROR : buildTargetFiles: files_TreeBox[" + iTreeBox +"] = " + nameInTreeBox +" - not found in inputFiles");
               }
               this.dialog.inputFiles.splice(i,1);
               this.dialog.inputKeys.splice(i,1);
               this.dialog.inputVariables.splice(i,1);
               this.dialog.files_TreeBox.remove( iTreeBox );
            }
         }
         this.dialog.QTY.text = "Total files: " + this.dialog.inputFiles.length;
         this.dialog.updateButtonState();
         // Refresh the generated files
         this.dialog.buildTargetFiles();

      }
   }

   this.files_close_all_Button = new ToolButton( this );
   with ( this.files_close_all_Button )
   {
      icon = new Bitmap( ":/images/close_all.png" );
      toolTip = "<p>Removed all images from the list.</p>";
      onClick = function()
      {
#ifdef DEBUG
         debug("Remove all files (" + this.dialog.inputFiles.length + ")");
#endif
         if ( this.dialog.inputFiles.length == 0 ) return;

         // TODO We can probably clear in one go
         for ( var i = this.dialog.files_TreeBox.numberOfChildren; --i >= 0; )
         {
               this.dialog.files_TreeBox.remove( i );
         }
         this.dialog.inputFiles = [];
         this.dialog.inputKeys = [];
         this.dialog.inputVariables = [];
         this.dialog.updateButtonState();
         // Also forget all FITS keys
         this.keyTable = [];
         this.keyEnabled = [];
         // Refresh the generated files
         this.dialog.buildTargetFiles();

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
         this.dialog.buildTargetFiles();
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
         this.dialog.buildTargetFiles();
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
         this.dialog.buildTargetFiles();
      }
   }





   //Output Dir --------------------------------------------------------------------------------------
   this.outputDir_Edit = new Edit( this );
   this.outputDir_Edit.readOnly = true;
   this.outputDir_Edit.text = outputDirectory;
   this.outputDir_Edit.toolTip ="select output directory.";

   this.outputDirSelect_Button = new ToolButton( this );
   with ( this.outputDirSelect_Button )
   {
      icon = new Bitmap( ":/images/icons/select.png" );
      toolTip = "Select output directory";
      onClick = function()
      {
         var gdd = new GetDirectoryDialog;
         gdd.initialPath = outputDirectory;
         gdd.caption = "Select Output Directory";
         if ( gdd.execute() )
         {
            outputDirectory = gdd.directory;
            this.dialog.outputDir_Edit.text = outputDirectory;
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
   // -- ENGINE

   this.buildTargetFiles = function() {

      // Make an array with the resulting file name for each file
      var targetFiles = new Array(this.inputFiles.length);

      // A map of file name to index to check duplicates
      var targetFileNameToIndexInTreeBox = {};

      //var orderedFiles = new Array(this.inputFiles.length);

      // A map of group count values
      var groups = {};


#ifdef DEBUG
      debug("buildTargetFiles: targeFileNamePattern = '" + guiParameters.targeFileNamePattern + "'");
      debug("buildTargetFiles: sourceFileNameRegExp = '" + guiParameters.sourceFileNameRegExp + "'");
      debug("buildTargetFiles: groupByPattern = '" + guiParameters.groupByPattern + "'");
#endif

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

      // List of text accumulating the transformation rules for display
      var listOfTransforms = [];
      // Number of the file in the order they are processed (as sorted by the TreeBox)
      var rank = 0;

      var skip = 0;
      // Initialized inside each loop, declared here for clarity
      var count = 0;
      var group = '';
      for (var iTreeBox = 0; iTreeBox < this.inputFiles.length; ++iTreeBox) {

            if ( !this.files_TreeBox.child(iTreeBox).checked ) { skip++; continue; }
            // Select name in tree box, find corresponding file in inputFiles
            var nameInTreeBox = this.files_TreeBox.child(iTreeBox).text(0);
            var i = this.inputFiles.indexOf(nameInTreeBox);
            if (i < 0) {
               throw ("SCRIPT ERROR : buildTargetFiles: files_TreeBox[" + iTreeBox +"] = " + nameInTreeBox +" - not found in inputFiles");
            }

            var inputFile = this.inputFiles[i];
            var inputFileName =  File.extractName(inputFile);
#ifdef DEBUG
            debug("buildTargetFiles: processing inputFile[" + i + "] = " + inputFile + ", TeexBox[" + iTreeBox + "].text:" + nameInTreeBox);
#endif

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
            rank ++;
            variables['rank'] = rank.pad(FFM_COUNT_PAD);

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
            if (groups.hasOwnProperty(group)) {
               count = groups[group];
            }
            count ++;
            groups[group] = count;
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

            // Check for duplicates
            if (targetFileNameToIndexInTreeBox.hasOwnProperty(targetString)) {
               Console.writeln("**** DUPLICATE "  + targetString + " at " + iTreeBox + " with " + targetFileNameToIndexInTreeBox[targetString]);
            }
            targetFileNameToIndexInTreeBox[targetString] = iTreeBox;

            // Target file but without the output directory
            targetFiles[i] = targetString;

            listOfTransforms.push("File ".concat(inputFile, "\n  to .../",targetString, "\n"));
         }
#ifdef DEBUG
         debug("buildTargetFiles: Total files: ", targetFiles.length,"; Skiped: ",skip,"; Processed: ",targetFiles.length-skip);
#endif
         this.transform_TextBox.text = listOfTransforms.join("");

         return targetFiles;
    }

   //engine----------------------------------------------------------------------------
   this.apply = function () {

      // TODO - do not rebuild, as the order may not be what is presented in the list (or check order)
      var targetFiles =  this.buildTargetFiles();
      var count = 0;

      for (i in targetFiles) {
            var targetString = targetFiles[i];
            var inputFile = this.inputFiles[i];

            var targetFile = outputDirectory + "/" + targetString;

#ifdef DEBUG
            debug("apply: targetFile = " + targetFile );
#endif
            var targetDirectory = File.extractDrive(targetFile) +  File.extractDirectory(targetFile);
#ifdef DEBUG
            debug("apply: targetDirectory = " + targetDirectory );
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
                  debug("apply: tryFilePath= " + tryFilePath );
#endif
                  if ( !File.exists( tryFilePath ) ) { targetFile = tryFilePath; break; }
               }
            }

            if (this.engine_mode==0) {
               console.writeln("move " + inputFile +"\n  to "+ targetFile);
               if (EXECUTE_COMMANDS) File.move(inputFile,targetFile);
            } else {
               console.writeln("copy " + inputFile+"\n  to "+ targetFile);
               if (EXECUTE_COMMANDS)  copyFile(inputFile,targetFile);
            }
            count ++;
            processEvents();

         }
         console.writeln("Total files: ", this.inputFiles.length,"; Processed: ",count);

   };

   //Engine buttons --------------------------------------------------------------------------------------
   this.move_Button = new PushButton( this );
   with ( this.move_Button ) {
      text = "Move files";
      toolTip = "Move Checked files to output directory";
      enabled = false;
      onClick = function()
      {
         parent.engine_mode = 0;
         parent.apply();
         //this.dialog.ok();
      }
   }

   this.refresh_Button = new PushButton( this );
   with ( this.refresh_Button ) {
      text = "Refresh list";
      toolTip = "Refresh the list of operations\nrequired after a sort on an header (there is on onSort event)";
      enabled = true;
      onClick = function()
      {
         parent.buildTargetFiles();
      }
   }

   this.copy_Button = new PushButton( this );
   with ( this.copy_Button ) {
      text = "Copy files";
      toolTip = "Copy Checked files to output directory";
      enabled = false;
      onClick = function()
      {
         parent.engine_mode = 1;
         parent.apply();
         //this.dialog.ok();
      }
   }

   // Export selected fits keywords for checked files
   this.txt_Button = new PushButton( this );
   with ( this.txt_Button ) {
      text = "Export FITS.txt";
      toolTip = "For Checked files write FitKeywords value to file FITS.txt in output directory";
      enabled = false;
      onClick = function()
      {
         var tab = String.fromCharCode(9);
         var f = new File();
         var fileName = "FITS_keys";
         var fileDir = outputDirectory;
         var t = fileDir + "/" + fileName + ".txt";
         // Create numbered file nameto create new file
         if ( File.exists( t ) )
         {
            for ( var u = 1; ; ++u )
            {
               for( var n = u.toString(); n.length < 4 ; n = "0" + n);
               var tryFilePath = File.appendToName( t, '-' + n );
               if ( !File.exists( tryFilePath ) ) { t = tryFilePath; break; }
            }
         }
         f.create(t);

         //output header (tab separated selected fits keyword + 'Filename')
         for ( var i in parent.keyTable)
         {
            if (!parent.keyEnabled[i]) continue;
            f.outTextLn(parent.keyTable[i]+tab);
         }
         f.outTextLn("Filename"+String.fromCharCode(10,13));

         //output FITS data
         var skip = 0;
         for ( var j in parent.inputFiles)
         {
            if ( !parent.files_TreeBox.child(parseInt(j)).checked ) { skip++; continue; }
            var key = parent.inputKeys[j];
            for ( var i in parent.keyTable)
            {
               if (!parent.keyEnabled[i]) continue;
               var name = parent.keyTable[i];
               for (var k in key)
               {
                  if (!(key[k].name == name)) continue;
                  if (key[k].isNumeric)
                     var value = parseFloat(key[k].value)
                  else
                  {
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
            f.outTextLn(parent.inputFiles[j]+String.fromCharCode(10,13));
         }
         f.close();
         console.writeln("FITSKeywords saved to ",t);
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

      // Fill list of keywords from parent keyTable
      for (i in pd.keyTable)
      {
         var node = new TreeBoxNode(this.keyword_TreeBox);
         node.setText( 0, pd.keyTable[i] );
         node.checked = pd.keyEnabled[i];
      }

      // Fill list of files from parent list of files
      this.file_ComboBox.clear();
      for (i in pd.inputFiles)
         this.file_ComboBox.addItem(pd.inputFiles[i]);
      this.file_ComboBox.onItemSelected(0);
      this.setMinSize(400,600);
   }

   // Save list of selected keywords in parent keyEnabled array
   this.onHide = function()
   {
      for (var i in pd.keyTable)
      {
         checked = this.keyword_TreeBox.child( parseInt(i) ).checked;
#ifdef DEBUG
         debug("KeyDialog: Key#= " + parseInt(i) + " checked= " + checked );
#endif
         pd.keyEnabled[i] = checked;
      }
      pd.setMinWidth(800);
   }


   // FITS keyword combox box for file selection
   this.file_ComboBox = new ComboBox( this );
   with ( this.file_ComboBox )
   {
      onItemSelected = function( index )
      {
         for ( var i in pd.keyTable)
            parent.keyword_TreeBox.child(parseInt(i)).setText(1,pd.files_TreeBox.child(index).text(parseInt(i)+1));
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
      setColumnWidth(0,100);
      setColumnWidth(1,200);
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

MyDialog.prototype = new Dialog;
KeyDialog.prototype = new Dialog;
var dialog = new MyDialog;
dialog.execute();
guiParameters.saveSettings();

