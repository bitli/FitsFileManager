
// From FITSkey_0.06

#feature-id    Utilities > FITS Tools
#define VERSION   0.06

#include <pjsr/DataType.jsh>
#include <pjsr/Sizer.jsh>
//#include <pjsr/FrameStyle.jsh>
#include <pjsr/TextAlign.jsh>

#define DEBUGGING_MODE_ON false
#define EXECUTE_COMMANDS true

// TODO
// Handle undefined
// Speed up
// refactoring data
// Option for handling of minus and other special characters
// refactor methods to extract fits data
// Save parameters, keep list of recent (possibly by type)
// Default output directory same as input if none present
// Move/rename label
// Mark in error if duplicate or already exists
// Add FITS keywords
// Add formatting for fits values
// Add optional indicator
// Add optional sequence ()
// Use count by order of key word
// Allow count by folder (using existin files...)
// Hide common header part of source folders
// Allow spurce folder in selection criteria
// Date date and ordering by date
// Create file for renaming instructions and summary
// Ensure source is refreshed in case of move
// Request confirmation for move
// Support group by for mutliple destinations ?


#define TARGET_PATTERN_TOOLTIP "\
Define how the target file name will be generated. Text is copied\n\
as is to the output name. Keywords (between & and semicolon) are\n\
defined from the file information and FITS keywordsas follows:\n\
   &binning;     Binning from XBINNING and YBINNING as integers, like 2x2.\n\
   &count;      The number of the file being moved/copied, padded to COUNT_PAD.\n\
   &exposure;   The exposure from EXPOSURE, but as an integer (assume seconds).\n\
   &extension;   The extension of the source file (with the dot.)\n\
   &filename;   The file name part of the source file.\n\
   &filter:     The filter name from FILTER as lower case trimmed normalized name.\n\
   &temp;       The SET-TEMP temperature in C as an integer.\n\
   &type:       The IMAGETYP normalized to 'flat', 'bias', 'dark', 'light'.\n\
   &FITSKW;     (NOT IMPLEMENTED).\n\
   &0; &1;, ... The corresponding match from the source file name pattern field.\n\
The target file name pattern may contain slashes that will be used\n\
as directory separator. Keywords may appear multiple time and also as part of directory names.\n\
"

#define SOURCE_FILENAME_REGEXP_TOOLTIP "\
Define  a regular expression (without the surround slashes) that will be applied to all file names\n\
without the extension. The 'match' array resulting from the regular expression matching can be used\n\
in the target file name pattern as &0; (whole expression), &1 (first group), ...\n\
"


//----------------------------------------------------------------------------


// -- Default patterns
var targeFileNamePattern = "&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;";
//var targeFileNamePattern = "&filename;_AS_&1;_bin_&binning;_filter_&filter;_temp_&temp;_type_&type;_exp_&exposure;s_count_&count;&extension;";
var sourceFileNameRegExp = /([^-]+)-/;

#define COUNT_PAD 4

//----------------------------------------------------------------------------

// Parsing the keywords in the targetFileNamePattern (1 characters will be removed at
// head (&) and tail (;), this is hard coded and must be modified if required
var variableRegExp = /&[a-zA-Z0-9]+;/g;

// -- Utility methods


function debug(str) {
   if (DEBUGGING_MODE_ON) {
      var s = replaceAll(str.toString(),'&','&amp;');
      Console.writeln(s);
      processEvents();
   }
   null;
}


function replaceAll (txt, replace, with_this) {
  return txt.replace(new RegExp(replace, 'g'),with_this);
}



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



// Remove quotes and trim
function unQuote (s) {
   var t = s.trim();
   if (t.length>0 && t[0]=="'" && t[t.length-1]=="'") {
      return t.substring(1,t.length-1).trim();
   }
   return t;
}

// Pad a mumber with leading 0
Number.prototype.pad = function(size){
      var s = String(this);
      while (s.length < size) s = "0" + s;
      return s;
}

// -- FITS utility methods

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
         debug("findKeyWord: '" + keys[k].name + "' found '"+ keys[k].value + "'");
         return (keys[k].value)
      }
   }
   debug("findKeyWord: '" +name + "' not found");
   return '';
}

// -- Conversion support
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
   return unquotedName.toLowerCase();
}

function convertType(rawTypeName) {
   var typeConversions = [
      [/.*flat.*/i, 'flat'],
      [/.*bias.*/i, 'bias'],
      [/.*offset.*/i, 'bias'],
      [/.*dark.*/i, 'dark'],
      [/.*light.*/i, 'light'],
   ];
   var unquotedName = unQuote(rawTypeName);
   for (var i=0; i<typeConversions.length; i++) {
      var typeName = unquotedName.replace(typeConversions[i][0],typeConversions[i][1]);
      if (typeName != unquotedName) {
         return typeName;
      }
   }
   // TODO Remove internal spaces etc...
   return unquotedName.toLowerCase();
}



//----------------------------------------------------------------------------
function MyDialog()
{
   this.__base__ = Dialog;
   this.__base__();
   this.inputFiles = new Array(); //Array of filename with full path
   this.inputKeys = new Array();  //keys of all files. Big RAW array of all file keys.
   this.keyTable = new Array();   //accumulated names of keywords from all files
   this.keyEnabled = new Array(); //true == selected keywords
   this.defaultKey = new Array("SET-TEMP","EXPOSURE","IMAGETYP","FILTER  ", "XBINNING","YBINNING");
   var outputDirectory = "";
   this.engine_mode = 0;       //0=move, 1=copy

   if (DEBUGGING_MODE_ON) var outputDirectory = "C:/temp";

   //------------------------------------------------------------
   this.onShow = function() {
//      this.filesAdd_Button.onClick();
   }

   //hide columns of unchecked keywords---------------------------
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

      onNodeUpdated = function( node, column ) // Invert CheckMark
      {
         for (var i=0; i < this.selectedNodes.length; i++)
         {
            if ( node == this.selectedNodes[i] ) continue; // skip curent clicked node, because it will inverted automaticaly
            this.selectedNodes[i].checked = !this.selectedNodes[i].checked;
         }
      }
   }


   //----------------------------------------------------------
   this.UpdateTreeBox = function ()
   {
      this.files_TreeBox.clear();
      this.keyTable = new Array(); // clear
      // Accumulate all KeyName in keyTabe
      for ( var i in this.inputFiles)
      {
         var key = this.inputKeys[i]; // keywords of one file
         var node = new TreeBoxNode( this.files_TreeBox );
         node.setText( 0, this.inputFiles[i] ); //write name to first column
         node.checked = true;

         for ( var j in key )
         {
            var name = key[j].name; //name of Keyword from file
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
            if (key[j].isNumeric) node.setText( k+1, Number(key[j].value).toFixed(3) );
            else node.setText( k+1, key[j].value.trim() );
         }
      }
      this.hideKey(); //hide columns of unchecked keywords
   }

   //---------------------------------------------------------------------------------------
   this.getFiles = function (fileNames)
   {

      debug("Found "+fileNames.length);
      debug("Checked:.");

         //for ( var b=0; b<fileNames.length.toString().length; b++)
         //   console.write(".");


      var qtyNew = 0;
      for ( var i in fileNames )
      {
         /*if (DEBUGGING_MODE_ON)
         {
            for ( var b=0; b<i.toString().length; b++)
               console.write("\b");
            console.write(parseInt(i)+1); processEvents();
         } */
         if (this.inputFiles.indexOf(fileNames[i]) < 0) //Add file only one times
         {
            var key = LoadFITSKeywords(fileNames[i]);
            this.inputFiles.push(fileNames[i]);
            this.inputKeys.push(key);
            qtyNew++;
         }
      }
      console.writeln(" ");
      if (qtyNew == 0) {console.writeln("No new files"); return;}
      console.writeln("New ",qtyNew,"\nTotal ",this.inputFiles.length);
      this.UpdateTreeBox();
      this.QTY.text = "Total files: " + this.inputFiles.length;
      this.setMinWidth(800);
      this.adjustToContents();
      this.dialog.update();

      this.buildTargetFiles();
      //this.hideKey(); // *** TEST
   }

   // Total file Label ---------------------------------------------------------------------------
   this.QTY = new Label( this );
   this.QTY.textAlignment = TextAlign_Right|TextAlign_VertCenter;

   //enable/disable buttons
   this.update = function()
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
         if ( ofd.execute() ) this.dialog.getFiles(ofd.fileNames);
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
            debug("Start searching FITS file in SubFolders");
            var fileNames = searchDirectory(gdd.directory+"/*.fit" ,true)
            .concat(searchDirectory(gdd.directory+"/*.fits",true))
            .concat(searchDirectory(gdd.directory+"/*.fts",true));
            debug("Finish searching FITS file in SubFolders");
            this.dialog.getFiles(fileNames);
         }
      }
   }

   // Close selected files ---------------------------------------------------------------------------
   this.files_close_Button = new ToolButton( this );
   with ( this.files_close_Button )
   {
      icon = new Bitmap( ":/images/close.png" );
      toolTip = "<p>Close selected images.</p>";
      onClick = function()
      {
         if ( this.dialog.inputFiles.length == 0 ) return;
         for ( var i = this.dialog.files_TreeBox.numberOfChildren; --i >= 0; )
         {
            if ( this.dialog.files_TreeBox.child( i ).selected )
            {
               this.dialog.inputFiles.splice(i,1);
               this.dialog.inputKeys.splice(i,1);
               this.dialog.files_TreeBox.remove( i );
            }
         }
         this.dialog.QTY.text = "Total files: " + this.dialog.inputFiles.length;
         this.dialog.update();
      }
   }


   // Target pattern --------------------------------------------------------------------------------------
   this.targetFilePattern_Edit = new Edit( this );
   with ( this.targetFilePattern_Edit )
   {
      text = targeFileNamePattern;
      toolTip = TARGET_PATTERN_TOOLTIP;
      enabled = true;
      onTextUpdated = function()
      {
         targeFileNamePattern = text;
         parent.buildTargetFiles();
      }
   }

   // Source file name pattern --------------------------------------------------------------------------------------
   this.sourcePattern_Edit = new Edit( this );
   with ( this.sourcePattern_Edit )
   {
      if (sourceFileNameRegExp==null) {
         text = "";
      } else {
         var re = sourceFileNameRegExp.toString();
         text = re.substring(1, re.length-1);
      }
      toolTip = SOURCE_FILENAME_REGEXP_TOOLTIP;
      enabled = true;
      onTextUpdated = function()
      {
         var re = text.trim();
         if (re.length == 0) {
            sourceFileNameRegExp = null;
            debug("sourcePattern_Edit - cancel regexp");
         } else {
            try {
               sourceFileNameRegExp = RegExp(text);
               this.textColor = 0;
               debug("sourcePattern_Edit - regexp: " + sourceFileNameRegExp);
            } catch (err) {
               sourceFileNameRegExp = null;
               this.textColor = 0xFF0000;
               debug("sourcePattern_Edit - bad regexp - err: " + err);
            }
         }
         // Refresh the generated files
         parent.buildTargetFiles();
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
            this.dialog.update();
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


   // -- ENGINE

   this.buildTargetFiles = function() {

     // Make an array with the root directory for each file
      var targetFiles = new Array(this.inputFiles.length);

      debug("** targeFileNamePattern '" + targeFileNamePattern + "'");
      debug("** sourceFileNameRegExp '" + sourceFileNameRegExp + "'");
      debug("** variableRegExp '" + variableRegExp + "'");

      var listOfTransforms = "";
      var skip = 0;
      var count = 0;
      for ( var i in this.inputFiles) {
            if ( !this.files_TreeBox.child(parseInt(i)).checked ) { skip++; continue; }

            var inputFile = this.inputFiles[i];
            var inputFileName =  File.extractName(inputFile);

            var keys = this.inputKeys[i];


            // Initialize variables
            count ++;
            var variables = [];


            //   &binning     Binning from XBINNING and YBINNING as integers, like 2x2.
            var xBinning =parseInt(findKeyWord(keys,'XBINNING'));
            var yBinning =parseInt(findKeyWord(keys,'YBINNING'));
            variables['binning'] = xBinning.toFixed(0)+"x"+yBinning.toFixed(0);

            //   &count;      The number of the file being moved/copied, padded to COUNT_PAD.
            variables['count'] = count.pad(COUNT_PAD);

            //   &exposure;   The exposure from EXPOSURE, but as an integer (assume seconds)
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


            //   &1; &2;, ... The corresponding match from the sourceFileNameRegExp
            if (sourceFileNameRegExp != null) {
               var match = sourceFileNameRegExp.exec(inputFileName);
               debug ("match: " + match);
               if (match != null) {
                  for (var j = 0; j<match.length; j++) {
                     variables[j.toString()] = match[j]
                  }
               }
            }



            // Method to handle replacement of variables
            var replaceVariables = function(matchedSubstring, index, originalString) {
               var varName = matchedSubstring.substring(1,matchedSubstring.length-1);
               debug("replaceVariables: match '" + matchedSubstring + "' '" + index + "' '" +  originalString + "' '" + varName + "' by '" + variables[varName] + "'");
               return variables[varName];
            };
            // The resulting name may include directories
            var targetString = targeFileNamePattern.replace(variableRegExp,replaceVariables);
            debug("targetString: " + targetString );

            // Target file but without the output directory
            targetFiles[i] = targetString;

            listOfTransforms = listOfTransforms.concat("File ",inputFile, "\n  to .../",
                targetString, "\n");
         }
         debug("Total files: ", targetFiles.length,"; Skiped: ",skip,"; Processed: ",targetFiles.length-skip);
         this.transform_TextBox.text = listOfTransforms;

         return targetFiles;
    }

   //engine----------------------------------------------------------------------------
   this.apply = function () {

      var targetFiles =  this.buildTargetFiles();
      var count = 0;

      for (i in targetFiles) {
            var targetString = targetFiles[i];
            var inputFile = this.inputFiles[i];

            var targetFile = outputDirectory + "/" + targetString;

            debug("targetFile: " + targetFile );
            var targetDirectory = File.extractDrive(targetFile) +  File.extractDirectory(targetFile);
            debug("targetDirectory: " + targetDirectory );

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
                  debug("tryFilePath: " + tryFilePath );
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
      text = "Move";
      toolTip = "Move Checked files to output directory";
      enabled = false;
      onClick = function()
      {
         parent.engine_mode = 0;
         parent.apply();
         //this.dialog.ok();
      }
   }

   this.copy_Button = new PushButton( this );
   with ( this.copy_Button ) {
      text = "Copy";
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
      text = "FITS.txt";
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

   this.outputDir_GroupBox = new GroupBox( this );
   with (this.outputDir_GroupBox)
   {
      title = "Output";
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
      add( this.outputDir_GroupBox );
      add(this.targetFilePattern_Edit);
      add(this.sourcePattern_Edit);
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
         debug("Key#: " + parseInt(i) + " checked: " + checked );
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

MyDialog.prototype = new Dialog;
KeyDialog.prototype = new Dialog;
var dialog = new MyDialog;
dialog.execute();
