// FITSFileManager-gui.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include <pjsr/DataType.jsh>
#include <pjsr/Sizer.jsh>
//#include <pjsr/FrameStyle.jsh>
#include <pjsr/TextAlign.jsh>
#include <pjsr/StdIcon.jsh>
#include <pjsr/StdButton.jsh>
#include <pjsr/FrameStyle.jsh>
#include <pjsr/Color.jsh>

#include <pjsr/ButtonCodes.jsh>
#include <pjsr/FocusStyle.jsh>



// For icons, see http://pixinsight.com/forum/index.php?topic=1953.msg12267#msg12267
// For Help see http://pixinsight.com/forum/index.php?topic=4598.msg31979#msg31979
//  It should support html 4 and some css

// Help texts (combined in various ways for field help and global help)

#define BASE_HELP_TEXT "\
<p>FITSFileManager allows you to copy or move FITS image files to new locations, generating the \
new location path from a template with the replacement of variables with values extracted from FITS keys \
and other information.\
<p/>You select the files to move/copy (files can be individually checked or un-checked) \
and select a predefined template or enter a new template to generate the target path using variables to substitute values \
based on the source image file name, FITS keywords or synthethic variables. \
Various other parameters can be adapted to fine tune the path generation. \
The list of transformation is updated as you type templates and other parameters. \
"
#define VARIABLE_HELP_TEXT "\
<p/>The variables have the general form '&amp;name:present?missing;', although most of the time \
they have simply the form '&amp;name;'. The 'name' identifies the variable, it may be a FITS key or a synthetic variable name. \
<ul><li>The optional 'present' part is the string that will be used as the replacement value if the variable \
has a value, Usually ':present' is not specified and the value of the variable is used as the replacement string. You can also \
have an empty 'present' value (as &amp;TELESCOP:;), in which case the variable is checked for presence (an error is \
generated if the variable is missing) but its value does not contribute to the target path.</li> \
<li>The optional '?missing' part is used if the variable is not present in the file (for example '&OBJECT?unknown;'). \
You can also have an empty 'missing' value (like '&amp;binning?;') in which case there is no error if the variable  \
has no value. </li>\
</ul><p>The synthetic variables are described in the section 'target template' below. They are built from the FITS keywords, \
the number of the file being processed or are result of a regular expression applied to the file name. \
The source file regular expression can be used, for example, to extract the part of the file name \
before the first dash and use it as a prefix for all files. \
<p/>The files are processed in the order they appear in the table (variable '&amp;rank;'). \
In addition a 'group' string can be generated using the same template rules and a '&amp;count;' \
variable is increased for each different group (for example each target directory). \
The values are cleaned up of special characters, so that they form legal file names. \
"


#define TARGET_TEMPLATE_TOOLTIP_A "\
Define how the target file path will be generated. The text of this field is used \
as the output path, except that the variables are replaced by their value.<br/>\
"
// Part used only in tooltip
#define TARGET_TEMPLATE_TOOLTIP_B "\
Variables (like  &amp;name; or &amp;name:present?absent;) are replaced by values defined from the file \
information and FITS keywords. The details on variables, especially the use of 'present' and 'absent' \
is defined in the help available by the icon at bottom right.<br/>\
"

#define TARGET_TEMPLATE_TOOLTIP_C "\
The variables include the FITS keywords and the following synthetic variables:<\br/>\
<dl>\
   <dt>&amp;binning;</dt><dd>Binning from XBINNING and YBINNING as integers, like 2x2.</dd>\
   <dt>&amp;exposure;</dt><dd>The exposure from EXPOSURE, but as an integer (assume seconds).<\dd>\
   <dt>&amp;extension;</dt><dd>The extension of the source file (with the dot.), will use input extension if not specified<\dd>\
   <dt>&amp;filename;</dt><dd>The file name part of the source file.<\dd>\
   <dt>&amp;filter;</dt><dd>The filter name from FILTER as lower case trimmed normalized name.<\dd>\
   <dt>&amp;night;</dt><dd>An integer identifying the night, requires JD and LONG-OBS - EXPERIMENTAL.<\dd>\
   <dt>&amp;temp;</dt><dd>The SET-TEMP temperature in C as an integer.<\dd>\
   <dt>&amp;type;</dt><dd>The IMAGETYP normalized to 'flat', 'bias', 'dark', 'light'.<\dd>\
   <dt>&amp;0; &amp;1;, ... </dt><dd>The corresponding match from the source file name regular expression field.<\dd>\
</dl>\
<p>The following keywords are dynamic (their values depends on the file order):\
<dl>\
   <dt>&amp;count;</dt><dd>The number of the file being moved/copied int the current group, padded to COUNT_PAD.<\dd>\
   <dt>&amp;rank;</dt><dd>The number of the file in the order of the input file list, padded to COUNT_PAD.<\dd>\
</dl>You can enter the template or select one of the predefined one and optionaly modify it.\
"


#define SOURCE_FILENAME_REGEXP_TOOLTIP "\
Defines a regular expression (without the surrounding slashes) that will be applied to all file names, \
without the extension. The 'match' array resulting from the regular expression matching can be used \
in the target file name template as numbered variables. '&0;' represent the whole matched expression, '&1' the first group, and so on \
In case of error the field turns red. \
You can enter the regexp or select one of the predefined one and optionally modify it. \
<br/>See https:\/\/developer.mozilla.org\/en-US\/docs\/JavaScript\/Guide\/Regular_Expressions for more informations on regular expresssions. \
<p>"


#define GROUP_TEMPLATE_TOOLTIP "\
Defines the template to generate a group name used by the synthetic variable '&count;'. \
Each FITS image generate a group name exactly as it generates a path, but using the group template. \
A count of image will be kept for each different group name and can be used as the variable '&count;' in the \
target path template. \
All variables can be used in the group template, except &count;. In addition you can use the following variable:\
<dl><dt>&targetDir;</dt><dd>The directory part of the target file name.</dd>\
</dl>Leave blank or use a fixed name have a single global counter.<br/>\
Example: '&targetDir;' counts images in each target directory. \
'&filter;' counts the images separetely for each filter (independently of the target directory).<br/> \
You can enter the template or select one of the predefined one and optionaly modify it.\
"


#define HELP_OPERATIONS "<p>The operations Copy/Move copy or move the files directly, without \
adding any FITS keywords.  The operation Load/SaveAs loads each image temporarily in the workspace \
and save it to the new location. An ORIGFILE keyword with the original file name is added if it is not already present. \
<br/>The operation buttons may be disabled if the operation is not possible (for example if the \
output directory is not specified).</p>\
"

// Combine help for global help
#define HELP_TEXT ("<html>" + \
"<h1><font color=\"#06F\">FITSFileManager</font></h1>" + BASE_HELP_TEXT + \
"<h3><font color=\"#06F\">Variables</font></h3/>" + VARIABLE_HELP_TEXT + \
"<h3><font color=\"#06F\">Target template</font></h3/>" + TARGET_TEMPLATE_TOOLTIP_A + TARGET_TEMPLATE_TOOLTIP_C + \
"</dl>Example of template:\<br/><tt>&nbsp;&nbsp;&nbsp;&amp;1;_&amp;binning;_&amp;temp;C_&amp;type;_&amp;exposure;s_&amp;filter;_&amp;count;&amp;extension;</tt>"+\
"<h3><font color=\"#06F\">Source filename reg exp</font></h3>" + SOURCE_FILENAME_REGEXP_TOOLTIP + \
"Example of regular expression:<br/><tt>&nbsp;&nbsp;&nbsp;([^-_.]+)(?:[._-]|$)</tt><p>" + \
"<h3><font color=\"#06F\">Group template</font></h3>" +  GROUP_TEMPLATE_TOOLTIP + \
"Example of group definition:<br/><tt>&nbsp;&nbsp;&nbsp;&amp;targetdir;</tt><p> " + \
"<h3><font color=\"#06F\">Operations</font></h3>" + HELP_OPERATIONS + \
"</html>")

// Constants
var CompletionDialog_doneContinue = 0;
var CompletionDialog_doneKeep = 1;
var CompletionDialog_doneRemove = 2;
var CompletionDialog_doneLeave= 3;




// ------------------------------------------------------------------------------------------------------------------------
// SectionBar Control from Juan: http://pixinsight.com/forum/index.php?topic=4610.msg32012#msg32012
// This does not work well on Mac, unfortunately
// ------------------------------------------------------------------------------------------------------------------------

#define contract_icon   new Bitmap( ":/images/icons/contract_v.png" )
#define expand_icon     new Bitmap( ":/images/icons/expand_v.png" )

function SectionBar( parent, initialyCollapsed ) {
   this.__base__ = Control;
   if ( parent )
      this.__base__( parent );
   else
      this.__base__();

   this.section = null;
   this.collapedTitle = "";
   this.expandedTitle = "";

#ifgteq __PI_BUILD__ 854
   var bgColor = Settings.readGlobal( "InterfaceWindow/SectionBarColor", DataType_UInt32 );
   var fgColor = Settings.readGlobal( "InterfaceWindow/SectionBarTextColor", DataType_UInt32 );
#else
   // PJSR access to global settings is broken in PI 1.7
   var bgColor = Color.rgbaColor( 192, 192, 168, 255 );
   var fgColor = Color.rgbaColor(   0,   0, 255, 255 );
#endif

   this.backgroundColor = bgColor;
   this.focusStyle = FocusStyle_NoFocus;

   this.label = new Label( this );
   this.label.textAlignment = TextAlign_Left|TextAlign_VertCenter;
   this.label.styleSheet =
      "QLabel { color: " + Color.rgbColorToHexString( fgColor ) + "; " +
               "background: " + Color.rgbColorToHexString( bgColor ) + "; }" +
      "QLabel:disabled { color: gray; }";

   this.button = new ToolButton( this );
   this.button.icon = contract_icon;
   this.button.setFixedSize( 17, 17 );
   this.button.focusStyle = FocusStyle_NoFocus;
   this.button.onClick = function()
   {
      this.parent.toggleSection();
   };

   var hSizer = new HorizontalSizer;
   hSizer.addSpacing( 4 );
   hSizer.add( this.label );
   hSizer.addStretch();
   hSizer.add( this.button );
   hSizer.addSpacing( 4 );

   this.sizer = new VerticalSizer;
   this.sizer.addSpacing( 1 );
   this.sizer.add( hSizer );
   this.sizer.addSpacing( 1 );

   this.adjustToContents();
   this.setFixedHeight();


   this.onMousePress = function( x, y, button, buttonState, modifiers )
   {
      if ( button == MouseButton_Left )
         this.button.onClick();
   };

   this.onShow = function()
   {
      this.updateTitle();
      this.updateIcon();
   };

   this.toggleSection = function()
   {
      if ( this.section )
      {
         this.setFixedWidth();

         if ( this.section.visible ) {
            this.section.hide();
         } else {
            this.section.show();
         }
         this.updateTitle();
         this.updateIcon();

         // Original adjustToContent
         this.dialog.adjustToContents();

         this.setVariableWidth();

      }
   };

   this.updateTitle = function() {
      if ( this.section &&  this.section.visible ) {
         this.label.text = this.expandedTitle;
      } else {
         this.label.text = this.collapsedTitle;
      }
   }

   this.updateIcon = function()
   {
      if ( this.section )
         if ( this.section.visible )
            this.button.icon = contract_icon;
         else
            this.button.icon = expand_icon;
   };



   // Public interface

   this.setTitle = function( title )
   {
      this.expandedTitle = title;
      this.collapsedTitle = title;
      this.updateTitle();
   };
   this.setExpandedTitle = function( title )
   {
      this.expandedTitle = title;
      this.updateTitle();
   };
   this.setCollapsedTitle = function( title )
   {
      this.collapsedTitle = title;
      this.updateTitle();
   };

   this.setSection = function( section )
   {
      this.section = section;
      if (initialyCollapsed) {
          this.section.hide();
      }
      this.updateIcon();
   };


}

SectionBar.prototype = new Control;




// ------------------------------------------------------------------------------------------------------------------------
// GUI Main Dialog
// ------------------------------------------------------------------------------------------------------------------------

function MainDialog(engine, guiParameters) {
   this.__base__ = Dialog;
   this.__base__();
   this.engine = engine;
   this.guiParameters = guiParameters;

   var labelWidth = this.font.width( "MMMMMMMMMMMMMM" ) ;


   // -- FITSKeyword Dialog (opened as a child on request)
   this.fitsKeysDialog = new FITSKeysDialog( this, engine );


   // -- CompletionDialog Dialog (opened as a child on request)
   this.completionDialog = new CompletionDialog( this, engine );

      // Set 'is visible' for the list of default keywords
      for (var i = 0; i < this.guiParameters.defaultListOfShownFITSKeywords.length; ++i) {
          var name = this.guiParameters.defaultListOfShownFITSKeywords[i];
          this.engine.shownFITSKeyNames[name] = true;
      }
      for (var i = 0; i<syntheticVariableNames.length;i++) {
         var name = syntheticVariableNames[i];
         this.engine.shownSyntheticKeyNames[name] = true;
      }


   // -- HelpLabel
   var helpLabel = new Label( this );
   helpLabel.frameStyle = FrameStyle_Box;
   helpLabel.margin = 4;
   helpLabel.wordWrapping = true;
   helpLabel.useRichText = true;
   helpLabel.text = "<b>" + TITLE + " v" + VERSION + "</b> &mdash; Copy or move FITS image " +
           "files using values derived from FITS keywords and from original file name, using a template " +
           "to create the target directory/file name. See the help for more details.";


   //----------------------------------------------------------------------------------
   // Input file list section
   //----------------------------------------------------------------------------------
   this.filesTreeBox = new TreeBox( this );

   this.filesTreeBox.rootDecoration = false;
   this.filesTreeBox.numberOfColumns = 1;
   this.filesTreeBox.multipleSelection = true;
   this.filesTreeBox.headerVisible = true;
   this.filesTreeBox.headerSorting = true;
   this.filesTreeBox.setHeaderText(0, "Filename");
   this.filesTreeBox.sort(0,true);
   this.filesTreeBox.setMinSize( 700, 200 );

   // Assume that 'check' is the only operation that update the nodes,
   // this may not be true...
   this.filesTreeBox.onNodeUpdated = function( node, column )  {
   // Invert CheckMark
#ifdef DEBUG_EVENTS
         debug("filesTreeBox: onNodeUpdated("+node+","+column+")");
#endif
      // Check box was likely changed
      this.dialog.updateTotal();
      this.dialog.refreshTargetFiles();
   };


   // -- Actions for input file list ---------------------------------------------------------------------------------------

   // -- Open FITS keyword dialog
   this.keyButton = new ToolButton( this );
   this.keyButton.icon = new Bitmap( ":/images/icons/text.png" );
   this.keyButton.toolTip = "Variables and FITS Keywords management";
   this.keyButton.onClick = function() {
   if (this.dialog.engine.keywordsSet.size()) {
         this.dialog.fitsKeysDialog.execute();
         this.dialog.showOrHideFITSkey();
      }
   }



   // --  Add files
   this.filesAdd_Button = new ToolButton( this );
   this.filesAdd_Button.icon = new Bitmap( ":/images/image_container/add_files.png" );
   this.filesAdd_Button.toolTip = "Add files";
   this.filesAdd_Button.onClick = function()
      {
         var ofd = new OpenFileDialog;
         ofd.multipleSelections = true;
         ofd.caption = "Select FITS Files";
         ofd.filters = [["FITS Files", "*.fit", "*.fits", "*.fts"]];
         if ( ofd.execute() ) {
            this.dialog.addFilesAction(ofd.fileNames);
         }
      }



   // -- Add Directory
   this.dirAdd_Button = new ToolButton( this );
   this.dirAdd_Button.icon = new Bitmap( ":/images/icons/folders.png" );
   this.dirAdd_Button.toolTip = "Add folder including subfolders";
   this.dirAdd_Button.onClick = function()
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


   // -- Check selected files
   this.checkSelected_Button = new ToolButton( this );
   this.checkSelected_Button.icon = new Bitmap( ":/images/process_explorer/expand_all.png" );
   this.checkSelected_Button.toolTip = "<p>Check selected images.</p>";
   this.checkSelected_Button.onClick = function() {
#ifdef DEBUG
      debug("checkSelected_Button: onClick");
#endif
      for (var i=0; i < this.dialog.filesTreeBox.selectedNodes.length; i++) {
            this.dialog.filesTreeBox.selectedNodes[i].checked = true;
      }
      this.dialog.updateTotal();
      this.dialog.refreshTargetFiles();
   }

   // -- uncheck selected files
   this.uncheckSelected_Button = new ToolButton( this );
   this.uncheckSelected_Button.icon = new Bitmap( ":/images/process_explorer/collapse_all.png" );
   this.uncheckSelected_Button.toolTip = "<p>Uncheck selected images.</p>";
   this.uncheckSelected_Button.onClick = function() {
#ifdef DEBUG
      debug("uncheckSelected_Button: onClick");
#endif
      for (var i=0; i < this.dialog.filesTreeBox.selectedNodes.length; i++) {
            this.dialog.filesTreeBox.selectedNodes[i].checked = false;
      }
      this.dialog.updateTotal();
      this.dialog.refreshTargetFiles();
   }

   // -- Remove selected files
   this.remove_files_Button = new ToolButton( this );
   this.remove_files_Button.icon = new Bitmap( ":/images/close.png" );
   this.remove_files_Button.toolTip = "<p>Remove selected images from the list.</p>";
   this.remove_files_Button.onClick = function() {
#ifdef DEBUG
      debug("remove_files_Button: onClick");
#endif

      for ( var iTreeBox = this.dialog.filesTreeBox.numberOfChildren; --iTreeBox >= 0; ) {
         if ( this.dialog.filesTreeBox.child( iTreeBox ).selected ) {
            var nameInTreeBox = this.dialog.filesTreeBox.child(iTreeBox).text(0);

            this.dialog.engine.removeFiles(nameInTreeBox);
            this.dialog.filesTreeBox.remove( iTreeBox );
         }
      }
      this.dialog.updateTotal();
      this.dialog.updateButtonState();
      // Refresh the generated files
      this.dialog.refreshTargetFiles();
   }


   // -- Remove all files
   this.remove_all_files_Button = new ToolButton( this );
   this.remove_all_files_Button.icon = new Bitmap( ":/images/close_all.png" );
   this.remove_all_files_Button.toolTip = "<p>Remove all images from the list.</p>";
   this.remove_all_files_Button.onClick = function() {
#ifdef DEBUG
      debug("remove_all_files_Button: onClick");
#endif

      // TODO We can probably clear in one go
      for ( var i = this.dialog.filesTreeBox.numberOfChildren; --i >= 0; ) {
            this.dialog.filesTreeBox.remove( i );
      }
      this.dialog.engine.reset();
      this.dialog.updateTotal();
      this.dialog.updateButtonState();
      // Refresh the generated files
      this.dialog.refreshTargetFiles();

   }

   // -- Total file Label ---------------------------------------------------------------------------
   this.inputSummaryLabel = new Label( this );
   this.inputSummaryLabel.textAlignment = TextAlign_Right|TextAlign_VertCenter;

   // -- Sizer for Input Files Section

   this.fileButonSizer = new HorizontalSizer;
   this.fileButonSizer.margin = 6;
   this.fileButonSizer.spacing = 4;
   this.fileButonSizer.add( this.keyButton );
   this.fileButonSizer.addSpacing( 5 );
   this.fileButonSizer.add( this.filesAdd_Button );
   this.fileButonSizer.add( this.dirAdd_Button );
   this.fileButonSizer.add( this.checkSelected_Button );
   this.fileButonSizer.add( this.uncheckSelected_Button );
   this.fileButonSizer.addSpacing( 5 );
   this.fileButonSizer.add( this.remove_files_Button );
   this.fileButonSizer.add( this.remove_all_files_Button );
   this.fileButonSizer.add( this.inputSummaryLabel );
   this.fileButonSizer.addStretch();


   this.inputFiles_GroupBox = new GroupBox( this );
   this.inputFiles_GroupBox.sizer = new VerticalSizer;
   this.inputFiles_GroupBox.sizer.margin = 6;
   this.inputFiles_GroupBox.sizer.spacing = 4;
   this.inputFiles_GroupBox.sizer.add( this.filesTreeBox,100 );
   this.inputFiles_GroupBox.sizer.add( this.fileButonSizer );


   this.barInput = new SectionBar( this );
   this.barInput.setTitle( "Input" );
   this.barInput.setCollapsedTitle( "Input - No file" );
   this.barInput.setSection( this.inputFiles_GroupBox );




   //----------------------------------------------------------------------------------
   // Rules section
   //----------------------------------------------------------------------------------

   // Target template --------------------------------------------------------------------------------------

   this.targetFileTemplate_ComboBox = new ComboBox( this );
   this.targetFileTemplate_ComboBox.toolTip = TARGET_TEMPLATE_TOOLTIP_A+TARGET_TEMPLATE_TOOLTIP_B+TARGET_TEMPLATE_TOOLTIP_C;
   this.targetFileTemplate_ComboBox.enabled = true;
   this.targetFileTemplate_ComboBox.editEnabled = true;
   for (var it = 0; it<guiParameters.targetFileItemListText.length; it++) {
      this.targetFileTemplate_ComboBox.addItem("'" + guiParameters.targetFileItemListText[it] +  "' - " + guiParameters.targetFileItemListComment[it]);
   }
   this.targetFileTemplate_ComboBox.editText = guiParameters.targetFileItemListText[0];


   this.targetFileTemplate_ComboBox.onEditTextUpdated = function() {
#ifdef DEBUG
      debug("targetFileTemplate_ComboBox: onEditTextUpdated " + this.editText);
#endif

      var text = this.editText;
      var templateErrors = [];

      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors, text);
      if (templateErrors.length === 0) {
         this.textColor = 0x000000;
         guiParameters.targetFileNameCompiledTemplate  = t;
         this.dialog.refreshTargetFiles();
      } else {
         this.textColor = 0xFF0000;
      }
   }

   this.targetFileTemplate_ComboBox.onItemSelected = function() {
#ifdef DEBUG
      debug("targetFileTemplate_ComboBox: onItemSelected " + this.currentItem);
#endif
      var text =guiParameters.targetFileItemListText[this.currentItem];
      this.dialog.targetFileTemplate_ComboBox.editText = text;
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors, text);
      if (templateErrors.length === 0) {
         this.textColor = 0x000000;
         guiParameters.targetFileNameCompiledTemplate  = t;
         this.dialog.refreshTargetFiles();
      } else {
         this.textColor = 0xFF0000;
      }
   }


   // Regular expression on source file --------------------------------------------------------------------------------------
   this.regexp_ComboBox = new ComboBox( this );
   this.regexp_ComboBox.toolTip = SOURCE_FILENAME_REGEXP_TOOLTIP;
   this.regexp_ComboBox.enabled = true;
   this.regexp_ComboBox.editEnabled = true;
   for (var it = 0; it<guiParameters.regexpItemListText.length; it++) {
      this.regexp_ComboBox.addItem("'" + guiParameters.regexpItemListText[it] +  "' - " + guiParameters.regexpItemListComment[it]);
   }
   this.regexp_ComboBox.editText = guiParameters.regexpItemListText[0];


   this.regexp_ComboBox.onEditTextUpdated = function() {
#ifdef DEBUG
      debug("regexp_ComboBox: onEditTextUpdated " + this.editText);
#endif

      var re = this.editText.trim();
      if (re.length === 0) {
         guiParameters.sourceFileNameRegExp = null;
#ifdef DEBUG
         debug("sourceTemplate_Edit: onTextUpdated:- cancel regexp");
#endif
      } else {
         try {
            guiParameters.sourceFileNameRegExp = RegExp(re);
            this.textColor = 0x000000;
#ifdef DEBUG
            debug("sourceTemplate_Edit: onTextUpdated: regexp: " + guiParameters.sourceFileNameRegExp);
#endif
         } catch (err) {
            guiParameters.sourceFileNameRegExp = null;
            this.textColor = 0xFF0000;
#ifdef DEBUG
            debug("sourceTemplate_Edit: onTextUpdated:  bad regexp - err: " + err);
#endif
         }
      }
   }

   this.regexp_ComboBox.onItemSelected = function() {
#ifdef DEBUG
      debug("regexp_ComboBox: onItemSelected " + this.currentItem);
#endif
      var text = regExpToString(guiParameters.regexpItemListText[this.currentItem]);
      this.dialog.regexp_ComboBox.editText = text;
      var re = text.trim();
      if (re.length === 0) {
         guiParameters.sourceFileNameRegExp = null;
#ifdef DEBUG
         debug("sourceTemplate_Edit: onTextUpdated:- cancel regexp");
#endif
      } else {
         try {
            guiParameters.sourceFileNameRegExp = RegExp(re);
            this.textColor = 0x000000;
#ifdef DEBUG
            debug("sourceTemplate_Edit: onTextUpdated: regexp: " + guiParameters.sourceFileNameRegExp);
#endif
         } catch (err) {
            guiParameters.sourceFileNameRegExp = null;
            this.textColor = 0xFF0000;
#ifdef DEBUG
            debug("sourceTemplate_Edit: onTextUpdated:  bad regexp - err: " + err);
#endif
         }
      }
   }


   // Group template --------------------------------------------------------------------------------------

   this.groupTemplate_ComboBox = new ComboBox( this );
   this.groupTemplate_ComboBox.toolTip = GROUP_TEMPLATE_TOOLTIP;
   this.groupTemplate_ComboBox.enabled = true;
   this.groupTemplate_ComboBox.editEnabled = true;
   for (var it = 0; it<guiParameters.groupItemListText.length; it++) {
      this.groupTemplate_ComboBox.addItem("'" + guiParameters.groupItemListText[it] +  "' - " + guiParameters.groupItemListComment[it]);
   }
   this.groupTemplate_ComboBox.editText = guiParameters.groupItemListText[0];


   this.groupTemplate_ComboBox.onEditTextUpdated = function() {
#ifdef DEBUG
      debug("groupTemplate_ComboBox: onEditTextUpdated " + this.editText);
#endif

      var text = this.editText;
      var templateErrors = [];

      var t = ffM_template.analyzeTemplate(templateErrors,text);
      if (templateErrors.length === 0) {
         this.textColor = 0x000000;
         guiParameters.groupByCompiledTemplate  = t;
         this.dialog.refreshTargetFiles();
      } else {
         this.textColor = 0xFF0000;
      }
   }

   this.groupTemplate_ComboBox.onItemSelected = function() {
#ifdef DEBUG
      debug("groupTemplate_ComboBox: onItemSelected " + this.currentItem);
#endif
      var text = guiParameters.groupItemListText[this.currentItem];
      this.dialog.groupTemplate_ComboBox.editText = text;
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,text);
      if (templateErrors.length === 0) {
         this.textColor = 0x000000;
         guiParameters.groupByCompiledTemplate  = t;
         this.dialog.refreshTargetFiles();
      } else {
         this.textColor = 0xFF0000;
      }
   }


   // Sizers for Rules section

   this.targetFileTemplate_Edit_sizer = new HorizontalSizer;
   this.targetFileTemplate_Edit_sizer.margin = 4;
   this.targetFileTemplate_Edit_sizer.spacing = 2;
   var label = new Label();
   label.setFixedWidth(labelWidth);
   label.text		= "Target file template: ";
   label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

   this.targetFileTemplate_Edit_sizer.add( label );
   this.targetFileTemplate_Edit_sizer.add( this.targetFileTemplate_ComboBox,100 );


   this.regexp_ComboBox_sizer = new HorizontalSizer;
   this.regexp_ComboBox_sizer.margin = 4;
   this.regexp_ComboBox_sizer.spacing = 2;
   var label = new Label();
   label.setFixedWidth(labelWidth);;
   label.text		= "File name RegExp: ";
   label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

   this.regexp_ComboBox_sizer.add( label );
   this.regexp_ComboBox_sizer.add( this.regexp_ComboBox,100 );


   this.groupTemplate_ComboBox_sizer = new HorizontalSizer;
   this.groupTemplate_ComboBox_sizer.margin = 4;
   this.groupTemplate_ComboBox_sizer.spacing = 2;
   var label = new Label();
   label.setFixedWidth(labelWidth);;
   label.text		= "Group template: ";
   label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

   this.groupTemplate_ComboBox_sizer.add( label );
   this.groupTemplate_ComboBox_sizer.add( this.groupTemplate_ComboBox,100);


   this.rules_GroupBox = new GroupBox( this );

   this.rules_GroupBox.sizer = new VerticalSizer;
   this.rules_GroupBox.sizer.margin = 6;
   this.rules_GroupBox.sizer.spacing = 4;

   this.rules_GroupBox.sizer.add( this.targetFileTemplate_Edit_sizer, 100);
   this.rules_GroupBox.sizer.add( this.regexp_ComboBox_sizer );
   this.rules_GroupBox.sizer.add( this.groupTemplate_ComboBox_sizer );

   this.barRules = new SectionBar( this );
   this.barRules.setTitle( "Rules" );
   this.barRules.setSection( this.rules_GroupBox );



   //----------------------------------------------------------------------------------
   // Conversion definition section
   //----------------------------------------------------------------------------------



   // Keywords to use
   var keywordNames_GroupBox = new GroupBox(this);

   keywordNames_GroupBox.title = "Keyword remapping ";
   keywordNames_GroupBox.toolTip = "The left side are the keywords used by default,\nThe right side are the keywords used in the current configuration. ";

   var keywordNames_TreeBox = new TreeBox(keywordNames_GroupBox);
   keywordNames_TreeBox.rootDecoration = false;
   keywordNames_TreeBox.numberOfColumns = 2;
   keywordNames_TreeBox.headerVisible = false;


   var refreshRemappedFITSkeywordsNames = function (keywordNames_TreeBox) {
      keywordNames_TreeBox.clear();
      var remappedFITSkeywordsNames = Object.keys(guiParameters.remappedFITSkeywords);
      for (var ic=0; ic<remappedFITSkeywordsNames.length; ic++) {
         var node = new TreeBoxNode(keywordNames_TreeBox);
         node.setText( 0, remappedFITSkeywordsNames[ic] );
         node.setText( 1, guiParameters.remappedFITSkeywords[remappedFITSkeywordsNames[ic]] );
         node.checkable = false;
      }
   }

   refreshRemappedFITSkeywordsNames(keywordNames_TreeBox);
   // Conversion of type names
   var typeConversion_GroupBox = new GroupBox(this);

   typeConversion_GroupBox.title = "Remapping of IMAGETYP ";
   typeConversion_GroupBox.toolTip = "The value of the IMAGETYP keywords are tested with each regular expression in turn,\n" +
           "the result is the first match or the original value if none matched.";

   var typeConversion_TreeBox = new TreeBox(typeConversion_GroupBox);
   typeConversion_TreeBox.rootDecoration = false;
   typeConversion_TreeBox.numberOfColumns = 2;
   typeConversion_TreeBox.headerVisible = false;

   for (var ic=0; ic<typeConversions.length; ic++) {
      var node = new TreeBoxNode(typeConversion_TreeBox);
      node.setText( 0, typeConversions[ic][0].toString() );
      node.setText( 1, typeConversions[ic][1] );
      node.checkable = false;
   }

   // Conversion of filter names
   var filterConversion_GroupBox = new GroupBox(this);
   filterConversion_GroupBox.title = "Remapping of FILTER";
   filterConversion_GroupBox.toolTip = "The value of the FILTER keywords are tested with each regular expression in turn,\n" +
           "the result is the first match or the original value if none matched.";

   var filterConversion_TreeBox = new TreeBox(filterConversion_GroupBox);
   filterConversion_TreeBox.rootDecoration = false;
   filterConversion_TreeBox.numberOfColumns = 2;
   filterConversion_TreeBox.headerVisible = false;
   for (var ic=0; ic<filterConversions.length; ic++) {
      var node = new TreeBoxNode(filterConversion_TreeBox);
      node.setText( 0, filterConversions[ic][0].toString() );
      node.setText( 1, filterConversions[ic][1] );
      node.checkable = false;
   }

   // Selection of mapping rules
   var mappingRules_ComboBox = new ComboBox( this );
   mappingRules_ComboBox.toolTip = "Select rules";
   mappingRules_ComboBox.enabled = true;
   for (var it = 0; it<kwMappingList.length; it++) {
      mappingRules_ComboBox.addItem( kwMappingList[it] +  " - " + kwMappingCommentsList[it]);
   }

   mappingRules_ComboBox.onItemSelected = function() {
#ifdef DEBUG
      debug("mappingRules_ComboBox: onItemSelected " + this.currentItem );
#endif
       guiParameters.kwMappingCurrentIndex = this.currentItem;
       guiParameters.remappedFITSkeywords =  kwMappingTables[kwMappingList[guiParameters.kwMappingCurrentIndex]];
       refreshRemappedFITSkeywordsNames(keywordNames_TreeBox);

      // If the rules are changed, all variables must be recalculated
      // TODO RECALCULATE VARIABLES
      // TODO We can probably clear in one go
      for ( var i = this.dialog.filesTreeBox.numberOfChildren; --i >= 0; ) {
            this.dialog.filesTreeBox.remove( i );
      }
      this.dialog.engine.reset();

      // TODO - Merge with action on add files
      //this.dialog.rebuildFilesTreeBox();
      this.dialog.updateButtonState();
      this.dialog.updateTotal();
      this.dialog.refreshTargetFiles();
    }



   // Group the list boxed of the current mapping and conversions
   var currentState_GroupBox = new Control( this );

   currentState_GroupBox.sizer = new HorizontalSizer;
   currentState_GroupBox.sizer.margin = 6;
   currentState_GroupBox.sizer.spacing = 4;

   currentState_GroupBox.sizer.add( keywordNames_GroupBox);
   currentState_GroupBox.sizer.add( typeConversion_GroupBox);
   currentState_GroupBox.sizer.add( filterConversion_GroupBox);
   // TODO Find other way to fix minimal size
   currentState_GroupBox.setMinHeight(150);


   // Group and create section bar

   this.conversion_GroupBox = new GroupBox( this );

   this.conversion_GroupBox.sizer = new VerticalSizer;
   this.conversion_GroupBox.sizer.margin = 6;
   this.conversion_GroupBox.sizer.spacing = 4;

   this.conversion_GroupBox.sizer.add( mappingRules_ComboBox);
   this.conversion_GroupBox.sizer.add( currentState_GroupBox, 100);

   this.barConversions = new SectionBar( this, true );
   this.barConversions.setTitle( "Remapping of keywords and values" );
   this.barConversions.setSection( this.conversion_GroupBox );
   //this.barConversions.toggleSection();



   //----------------------------------------------------------------------------------
   // Output directory section
   //----------------------------------------------------------------------------------

   //Output Dir --------------------------------------------------------------------------------------
   this.outputDir_Edit = new Edit( this );
   this.outputDir_Edit.readOnly = true;
   this.outputDir_Edit.text = this.engine.outputDirectory;
   this.outputDir_Edit.toolTip ="select output directory.";

   this.outputDirSelect_Button = new ToolButton( this );
   this.outputDirSelect_Button.icon = new Bitmap( ":/images/icons/select.png" );
   this.outputDirSelect_Button.toolTip = "Select output directory";
   this.outputDirSelect_Button.onClick = function() {
      var gdd = new GetDirectoryDialog;
      gdd.initialPath = engine.outputDirectory;
      gdd.caption = "Select Output Directory";
      if ( gdd.execute() ) {
         this.dialog.engine.outputDirectory = gdd.directory;
         this.dialog.outputDir_Edit.text = this.dialog.engine.outputDirectory;
         this.dialog.barOutput.setCollapsedTitle( "Output base directory - " +  this.dialog.engine.outputDirectory);
         this.dialog.updateButtonState();
      }
   }

   this.outputDir_GroupBox = new GroupBox( this );
   this.outputDir_GroupBox.sizer = new HorizontalSizer;
   this.outputDir_GroupBox.sizer.margin = 6;
   this.outputDir_GroupBox.sizer.spacing = 4;
   this.outputDir_GroupBox.sizer.add( this.outputDir_Edit, 100 );
   this.outputDir_GroupBox.sizer.add( this.outputDirSelect_Button );

   this.barOutput = new SectionBar( this );
   this.barOutput.setTitle( "Output base directory" );
   this.barOutput.setCollapsedTitle( "Output base directory - not specified" );
   this.barOutput.setSection( this.outputDir_GroupBox );


   //----------------------------------------------------------------------------------
   // Operation list and action section
   //----------------------------------------------------------------------------------


   // Result operations --------------------------------------------------------------------------------------
   this.transform_TreeBox = new TreeBox( this );

   this.transform_TreeBox.rootDecoration = false;
   this.transform_TreeBox.numberOfColumns = 1;
   this.transform_TreeBox.multipleSelection = false;
   this.transform_TreeBox.headerVisible = false;
   this.transform_TreeBox.headerSorting = false;
   this.transform_TreeBox.setHeaderText(0, "Filename");
   //this.transform_TreeBox.sort(0,false);
   this.transform_TreeBox.setMinSize( 700, 200 );

   this.outputSummaryLabel = new Label( this );
   this.outputSummaryLabel.textAlignment = TextAlign_Left|TextAlign_VertCenter;

   this.outputFiles_GroupBox = new GroupBox( this );
   this.outputFiles_GroupBox.sizer = new VerticalSizer;
   this.outputFiles_GroupBox.sizer.margin = 6;
   this.outputFiles_GroupBox.sizer.spacing = 4;
   this.outputFiles_GroupBox.sizer.add( this.transform_TreeBox, 100);
   this.outputFiles_GroupBox.sizer.add( this.outputSummaryLabel );


   this.barResult = new SectionBar( this );
   this.barResult.setTitle( "Resulting operations" );
   this.barResult.setCollapsedTitle( "Resulting operations - None" );
   this.barResult.setSection( this.outputFiles_GroupBox );



   // -- Action buttons --------------------------------------------------------------------------------------

   this.check_Button = new PushButton( this );
   this.check_Button.text = "Check validity";
   this.check_Button.toolTip = "Check that the target files are valid\nthis is automatically done before any other operation";
   this.check_Button.enabled = true;
   this.check_Button.onClick = function() {
      var listOfFiles = this.parent.makeListOfCheckedFiles();
      var errors = this.parent.engine.checkValidTargets(listOfFiles);
      if (errors.length > 0) {
         var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
         msg.execute();
      } else if (this.parent.engine.targetFiles.length === 0) {
         var msg = new MessageBox(
            "There is no file to move or copy", "Check irrelevant", StdIcon_Information, StdButton_Ok );
         msg.execute();
      } else {
         var text = "" + this.parent.engine.targetFiles.length + " files checked" ;
            if (this.parent.engine.nmbFilesTransformed >0) {
               text += ", " + this.parent.engine.nmbFilesTransformed + " to copy/move";
            }

         //      this.engine.nmbFilesSkipped;
         if (this.parent.engine.nmbFilesInError >0) {
            // Should not happens
            text += ", " + this.parent.engine.nmbFilesInError + " IN ERROR";
         }

         if (!this.parent.engine.outputDirectory) {
            text += ",\nbut output directory is not defined";
         }

         var msg = new MessageBox(text,
            "Check successful", StdIcon_Information, StdButton_Ok );
         msg.execute();
      }
   }

   this.refresh_Button = new PushButton( this );
   this.refresh_Button.text = "Refresh list";
   this.refresh_Button.toolTip = "Refresh the list of operations\nrequired after a sort on an header (there is on onSort event)";
   this.refresh_Button.enabled = true;
   this.refresh_Button.onClick = function() {
      this.parent.removeDeletedFiles();
      this.parent.refreshTargetFiles();
   }



   this.move_Button = new PushButton( this );
   this.move_Button.text = "Move files";
   this.move_Button.toolTip = "Move the checked files to the output directory.\nNo HISTORY keyword added";
   this.move_Button.enabled = false;
   this.move_Button.onClick = function() {
      var listOfFiles = this.parent.makeListOfCheckedFiles();
      var errors = this.parent.engine.checkValidTargets(listOfFiles);
      if (errors.length > 0) {
         var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
         msg.execute();
         return;
      }
      var resultText = this.parent.engine.executeFileOperations(0);
      this.parent.removeDeletedFiles();
      this.parent.refreshTargetFiles();

      this.dialog.completionDialog.setResultText(resultText + "\nMoved files were removed from the input list");
      this.dialog.completionDialog.setResultModeMove();
      var completionCode =  this.dialog.completionDialog.execute();
      switch (completionCode) {
         case CompletionDialog_doneContinue:
         case CompletionDialog_doneKeep:
         case CompletionDialog_doneRemove:
            // Nothing to do, only Continue makes sense above
         break;
         case CompletionDialog_doneLeave:
            this.dialog.ok();
         break;
      }
   }


   this.copy_Button = new PushButton( this );
   this.copy_Button.text = "Copy files";
   this.copy_Button.toolTip = "Copy the checked files in the output directory.\nNo HISTORY keyword added";
   this.copy_Button.enabled = false;
   this.copy_Button.onClick = function() {
      var listOfFiles = this.parent.makeListOfCheckedFiles();
      var errors = this.parent.engine.checkValidTargets(listOfFiles);
      if (errors.length > 0) {
            var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
            msg.execute();
            return;
      }
      var resultText =  this.parent.engine.executeFileOperations(1);

      this.dialog.completionDialog.setResultText(resultText + "\nCopied files are still checked in the input list");
      this.dialog.completionDialog.setResultModeCopy();
      var completionCode =  this.dialog.completionDialog.execute();
      switch (completionCode) {
         case CompletionDialog_doneContinue:
         case CompletionDialog_doneKeep:
            return;
         break;
         case CompletionDialog_doneRemove:
            for ( var iTreeBox = this.dialog.filesTreeBox.numberOfChildren; --iTreeBox >= 0; ) {
            if ( this.dialog.filesTreeBox.child( iTreeBox ).checked ) {
               var nameInTreeBox = this.dialog.filesTreeBox.child(iTreeBox).text(0);
               this.dialog.engine.removeFiles(nameInTreeBox);
               this.dialog.filesTreeBox.remove( iTreeBox );
            }
         }
         this.dialog.updateTotal();
         this.dialog.updateButtonState();
         this.dialog.refreshTargetFiles();
         break;
         case CompletionDialog_doneLeave:
            this.dialog.ok();
         break;
      }
   }



   this.loadSave_Button = new PushButton( this );
   this.loadSave_Button.text = "Load / SaveAs files";
   this.loadSave_Button.toolTip = "Load the checked files and save them in the output directory.\nAdd ORIGFILE keyword with original file name if not already present.\n(Does not work for files with multiple images)\n";
   this.loadSave_Button.enabled = false;
   this.loadSave_Button.onClick = function() {
      var listOfFiles = this.parent.makeListOfCheckedFiles();
      var errors = this.parent.engine.checkValidTargets(listOfFiles);
      if (errors.length > 0) {
            var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
            msg.execute();
            return;
      }
      var resultText = this.parent.engine.executeFileOperations(2);

      this.dialog.completionDialog.setResultText(resultText+ "\nLoad/saved files are still checked in the input list");
      this.dialog.completionDialog.setResultModeCopy();
      var completionCode =  this.dialog.completionDialog.execute();
      switch (completionCode) {
         case CompletionDialog_doneContinue:
         case CompletionDialog_doneKeep:
            return;
         break;
         case CompletionDialog_doneRemove:
            for ( var iTreeBox = this.dialog.filesTreeBox.numberOfChildren; --iTreeBox >= 0; ) {
            if ( this.dialog.filesTreeBox.child( iTreeBox ).checked ) {
               var nameInTreeBox = this.dialog.filesTreeBox.child(iTreeBox).text(0);
               this.dialog.engine.removeFiles(nameInTreeBox);
               this.dialog.filesTreeBox.remove( iTreeBox );
            }
         }
         this.dialog.updateTotal();
         this.dialog.updateButtonState();
         this.dialog.refreshTargetFiles();

         break;
         case CompletionDialog_doneLeave:
            this.dialog.ok();
         break;
      }
   }


#ifdef IMPLEMENTS_FITS_EXPORT
// Export FITS values button
   this.txt_Button = new PushButton( this );
   this.txt_Button.text = "Export FITS.txt";
   this.txt_Button.toolTip = "For Checked files write FitKeywords value to file FITS.txt in output directory";
   this.txt_Button.enabled = false;
   this.txt_Button.onClick = function() {
      this.parent.engine.exportFITSKeywords();
   }
#endif

   // Help buton
   this.helpButton = new ToolButton( this );
   this.helpButton.icon = new Bitmap( ":/images/interface/browseDocumentationButton.png" );
   this.helpButton.toolTip = "Browse Documentation";
   this.helpDialog = new HelpDialog(this);
   this.helpButton.onClick = function() {
      this.dialog.helpDialog.execute();
   }




   // Sizer for Operation List and Actions section

   this.buttonSizer = new HorizontalSizer;
   this.buttonSizer.spacing = 2;
   this.buttonSizer.add( this.refresh_Button);
   this.buttonSizer.add( this.check_Button);
   this.buttonSizer.add( this.move_Button);
   this.buttonSizer.add( this.copy_Button);
   this.buttonSizer.add( this.loadSave_Button);
#ifdef IMPLEMENTS_FITS_EXPORT
   this.buttonSizer.add( this.txt_Button);
#endif
   this.buttonSizer.addStretch();
   this.buttonSizer.add( this.helpButton);




   // --------------------------------------------------------------------------------------------
   // Sizer for dialog

   this.sizer = new VerticalSizer;
   this.sizer.margin = 2;
   this.sizer.spacing = 2;
   this.sizer.add( helpLabel );
   this.sizer.add(this.barInput);
   this.sizer.add( this.inputFiles_GroupBox,50 );
   this.sizer.add(this.barRules);
   this.sizer.add(this.rules_GroupBox);
   this.sizer.add(this.barConversions);
   this.sizer.add(this.conversion_GroupBox);
   this.sizer.add(this.barOutput);
   this.sizer.add( this.outputDir_GroupBox );
   this.sizer.add(this.barResult);
   this.sizer.add(this.outputFiles_GroupBox,501);
   this.sizer.add( this.buttonSizer );






   //----------------------------------------------------------------------------------
   // Support methods
   //----------------------------------------------------------------------------------


   // -- Set visibility of synthetic and FITS keywords columns (called to apply changes)
   this.showOrHideFITSkey = function () {
      var allFITSKeyNames = this.engine.keywordsSet.allValueKeywordNameList;
      // +1 as the file name is always visible
      for (var i = 0; i<syntheticVariableNames.length;i++) {
         var c = i + 1;
         this.filesTreeBox.showColumn( c, this.engine.shownSyntheticKeyNames.hasOwnProperty(syntheticVariableNames[i]));
      }
      for (var i = 0; i<allFITSKeyNames.length;i++) {
         var c = i + 1 + syntheticVariableNames.length;
         this.filesTreeBox.showColumn( c, this.engine.shownFITSKeyNames.hasOwnProperty(allFITSKeyNames[i]));
      }
   }


   // -- Rebuild the TreeBox content
   this.rebuildFilesTreeBox = function () {
      var i, keys, node, name, iKeyOfFile, k;

#ifdef DEBUG
      debug("rebuildFilesTreeBox: rebuilding filesTreeBox - " + this.engine.inputFiles.length + " input files");
#endif

      this.filesTreeBox.clear();
      this.filesTreeBox.numberOfColumns = 1; // Filename

      this.engine.keywordsSet = ffm_keywordsOfFile.makeKeywordsSet(); // clear

      // Add the synthetic keys columns
      for (var iSynthKey = 0; iSynthKey<syntheticVariableNames.length; iSynthKey++) {
         var name = syntheticVariableNames[iSynthKey];
         this.filesTreeBox.numberOfColumns++;// add new column
         this.filesTreeBox.setHeaderText(this.filesTreeBox.numberOfColumns-1, name);//set name of new column
#ifdef DEBUG
         debug("rebuildFilesTreeBox: added synthetic key header '" + name +  "' as col " + this.filesTreeBox.numberOfColumns);
#endif
      }



      // Add all lines (nodes), one for each file
      // The node will contain the file name, then the synthethic keys, then the FITS keys.
      // All synthethic and FITS keys are added, even if they are not shown or used (this simplifies some code,
      // but maybe could be optimized).
      // The list of synthethic keys is currently fixed.
      // The list of FITS key is dynamic and FITS keys coluns are added as needed when an image
      // has a FITS keyword not yet mapped to a column. The mapping of key name to column index
      // is built on the fly.
      // Only the keys with a value (not of type isNull) are considered (this skip comments)
      var longestFileName = "          "; // File name column will have at least 10 characters

      for (var i = 0; i < this.engine.inputFiles.length; ++i) {

         if (this.engine.inputFiles[i].length>longestFileName.length) {longestFileName = this.engine.inputFiles[i];}

#ifdef DEBUG
         debug("rebuildFilesTreeBox: adding file '" +this.engine.inputFiles[i] + "' to row " + i);
#endif

         var imageKeywords = this.engine.inputFITSKeywords[i]; // all FITS keywords/Values of the current file
         var keys=imageKeywords.fitsKeywordsList;
         var syntheticKeywords = this.engine.inputVariables[i]; // Map of all synthethic keywords and values of the current file

         // Create TreeBoxNode (line) for the current file
         var node = new TreeBoxNode( this.filesTreeBox );
         // put name of the file int the first column
         node.setText( 0, this.engine.inputFiles[i] );
         node.checked = true;
         // Reserve column for file name
         var colOffset = 1;

         // Add synthethic keyword columns (based on fixed list of syntethic keywords)
#ifdef DEBUG
         debug("rebuildFilesTreeBox: adding " + Object.keys(syntheticKeywords) + " synthetics keys, " + keys.length + " FITS keys to row " + i);
#endif
         for (var iSynthKey = 0; iSynthKey<syntheticVariableNames.length; iSynthKey++) {
            var name = syntheticVariableNames[iSynthKey];
            var textSynthKey = syntheticKeywords[name];
            node.setText(iSynthKey+colOffset, textSynthKey ? textSynthKey : "");
         }
         // Skip next columns
         colOffset += syntheticVariableNames.length;

         // Adding FITS keyword columns (based on FITS keywords in current file and map of keywords to column index)
#ifdef DEBUG
         debug("rebuildFilesTreeBox: setting " + keys.length + " FITS keys to row " + i + ", colOffset=" +colOffset);
#endif
         for ( var iKeyOfFile = 0; iKeyOfFile<keys.length; iKeyOfFile++) {
            var key = keys[iKeyOfFile];

            // Only show the value keywords (not the comment keywords)
            if (! keys[iKeyOfFile].isNull) {
               var name = key.name; //name of Keyword from file
               var allFITSKeyNames = this.engine.keywordsSet.allValueKeywordNameList;
               var indexOfKey = allFITSKeyNames.indexOf(name);// find index of "name" in allFITSKeyNames
               if (indexOfKey < 0)  {
               // new FITS keyword, not yet mapped to a column, add new value keyword
#ifdef DEBUG_COLUMNS
                  debug("rebuildFilesTreeBox: Creating new column " + this.filesTreeBox.numberOfColumns + " for value keywords '"  + name + "', total col len " + this.engine.allFITSKeyNames.size());
#endif
                  allFITSKeyNames.push(name);//add keyword name to table
                  this.filesTreeBox.numberOfColumns++;// add new column
                  this.filesTreeBox.setHeaderText(this.filesTreeBox.numberOfColumns-1, name);//set name of new column
                  indexOfKey = this.filesTreeBox.numberOfColumns-colOffset-1;
               }
               // Set column content to value of keyword
#ifdef DEBUG_COLUMNS
               debug("rebuildFilesTreeBox: Set column, colOffset " + colOffset + ", index "  + indexOfKey + ", value " + keys[iKeyOfFile].value);
#endif
               var formattedValue = key.strippedValue;
               if (key.isNumeric) {
                  // Remove leading 0 and trailing 0 of decimal values to use less space
                  var n = key.numericValue;
                  if (n % 1 === 0) {
                     // Will be formatted without decimal point and leading 0
                     formattedValue = n.toString();
                  } else {
                     // TODO Supress possible leading zero of floating (leave 1 before decimal point)
                  }
               }
               node.setText(colOffset + indexOfKey, formattedValue);
            }
         }
      }


      // hide the columns of unchecked FITS keywords
      this.showOrHideFITSkey();

      // Keep the File name colmn reasonably sized
      if (longestFileName.length > 80) {
         longestFileName=longestFileName.substr(0,80);
      }
      this.filesTreeBox.setColumnWidth(0,this.font.width(longestFileName + "MMMM") );

   }


   // -- enable/disable operation buttons depending on context
   this.updateButtonState = function()
   {
      var enabled = this.dialog.engine.canDoOperation();
      this.dialog.move_Button.enabled = enabled;
      this.dialog.copy_Button.enabled = enabled;
      this.dialog.loadSave_Button.enabled = enabled;
#ifdef IMPLEMENTS_FITS_EXPORT
      this.dialog.txt_Button.enabled = enabled;
#endif
   }


   // -- Add a list of files to the TreeBox, refresh the TreeBox
   this.addFilesAction = function (fileNames)
   {
      this.engine.addFiles(fileNames);

      this.rebuildFilesTreeBox();
      this.setMinWidth(800);
      this.adjustToContents();
      this.dialog.updateButtonState();
      this.dialog.updateTotal();

      this.refreshTargetFiles();
      //this.showOrHideFITSkey(); // *** TEST
   }


   // -- Update the input file list total after each add/remove/check toggle
   this.updateTotal = function() {
      // Should be same as this.engine.inputFiles.length
      var countTotal = this.filesTreeBox.numberOfChildren;
      var countChecked = 0;
      for (var iTreeBox = 0; iTreeBox < this.filesTreeBox.numberOfChildren; ++iTreeBox) {
         if ( this.filesTreeBox.child(iTreeBox).checked ) {
            countChecked += 1;
         }
      }
      var countText;
      if (countTotal === 0 && countChecked ===0) {
        countText = "No file";
      } else if (countChecked===0) {
         countText = "No checked file / " + countTotal + " file" + (countTotal>1 ? "s" : "");
      } else {
         countText = "" + countChecked + " checked file" +  (countChecked>1 ? "s" : "") + " / " + countTotal + " file" + (countTotal>1 ? "s" : "");
      }
      this.inputSummaryLabel.text = countText;
      this.barInput.setCollapsedTitle("Input - " + countText);
   }


   // -- Return an array of the files that with chekd box ticked
   this.makeListOfCheckedFiles = function() {
      var listOfFiles = [];

      for (var iTreeBox = 0; iTreeBox < this.filesTreeBox.numberOfChildren; ++iTreeBox) {

         if ( this.filesTreeBox.child(iTreeBox).checked ) {
            // Select name in tree box, find corresponding file in inputFiles
            var nameInTreeBox = this.filesTreeBox.child(iTreeBox).text(0);
            listOfFiles.push(nameInTreeBox);
         }
      }

      return listOfFiles;
   }


   // -- Update the output operations indications
   this.refreshTargetFiles = function() {

#ifdef DEBUG
      debug("refreshTargetFiles() called");
#endif
      var listOfFiles = this.makeListOfCheckedFiles();

      this.engine.buildTargetFiles(listOfFiles);


      // List of text accumulating the transformation rules for display
      var listOfTransforms = this.engine.makeListOfTransforms();

      this.transform_TreeBox.clear();
      var firstNode = null;
      for (var i=0; i<listOfTransforms.length; i++) {
         var node = new TreeBoxNode( this.transform_TreeBox );
         if (i===0) { firstNode = node}
         node.setText( 0, listOfTransforms[i] );
         // TODO Use better status information than text
         if (listOfTransforms[i].indexOf("Error")>0) {
            node.setTextColor(0,0x00FF0000);
         }
      }
      if (firstNode) {this.transform_TreeBox.currentNode = firstNode;}

      var nmbFilesExamined = this.engine.targetFiles.length;

      var barResultTitle = "";
      if (nmbFilesExamined === 0) {
          barResultTitle += "None";
      } else {
         barResultTitle += "" + nmbFilesExamined + " files checked" ;
         if (this.engine.nmbFilesTransformed >0) {
            barResultTitle += ", " + this.engine.nmbFilesTransformed + " to copy/move";
         }

         //      this.engine.nmbFilesSkipped;
         if (this.engine.nmbFilesInError >0) {
            barResultTitle += ", " + this.engine.nmbFilesInError + " IN ERROR";
         }
      }
      this.outputSummaryLabel.text = barResultTitle;
      this.barResult.setCollapsedTitle("Resulting operations - " + barResultTitle);


    }


    // -- Support for refresh and move, remove all input files that are not present anymore in the file system
    this.removeDeletedFiles = function() {
      for ( var iTreeBox = this.dialog.filesTreeBox.numberOfChildren; --iTreeBox >= 0; ) {

         var nameInTreeBox = this.dialog.filesTreeBox.child(iTreeBox).text(0);
         if (!File.exists(nameInTreeBox)) {
#ifdef DEBUG
            debug("File '" + nameInTreeBox + "' removed from input list as not present on file system anymore.");
#endif
            this.dialog.engine.removeFiles(nameInTreeBox);
            this.dialog.filesTreeBox.remove( iTreeBox );
         }

         this.dialog.updateTotal();
         this.dialog.updateButtonState();
         // Caller must refresh the generated files
       }
    }

}
MainDialog.prototype = new Dialog;


// ------------------------------------------------------------------------------------------------------------------------
// Documentation dialog
// ------------------------------------------------------------------------------------------------------------------------


// See http://pixinsight.com/developer/pcl/doc/20120901/html/classpcl_1_1Console.html
// for formatting instructions

function HelpDialog( parentDialog, engine ) {
   this.__base__ = Dialog;
   this.__base__();

   this.windowTitle = "FITSFileManager Help";

   this.helpBox = new TextBox( this );
   this.helpBox.readOnly = true;
   this.helpBox.text = HELP_TEXT;
   this.helpBox.setMinSize( 800, 400 );
   this.helpBox.caretPosition = 0;

   this.sizer = new HorizontalSizer;
   this.sizer.margin = 6;
   this.sizer.add( this.helpBox );
   this.setVariableSize();
   this.adjustToContents();
}

HelpDialog.prototype = new Dialog;


// ------------------------------------------------------------------------------------------------------------------------
// Completion dialog
// ------------------------------------------------------------------------------------------------------------------------


function CompletionDialog( parentDialog, engine ) {
   this.__base__ = Dialog;
   this.__base__();

   this.windowTitle = "FITSFileManager operation result";

   this.resultBox = new TextBox( this );
   this.resultBox.readOnly = true;
   this.resultBox.text = "TEXT NOT INITIALIZED";
   this.resultBox.setMinSize( 600, 200 );
   this.resultBox.caretPosition = 0;

   this.continue_Button = new PushButton( this );
   this.continue_Button.text = "Continue in FITSFileManager";
   this.continue_Button.toolTip = "Continue working in FITSFileManager, moved files have been removed from input list";
   this.continue_Button.enabled = true;
   this.continue_Button.onClick = function() {
      this.dialog.done(CompletionDialog_doneContinue);
   }
   this.keep_Button = new PushButton( this );
   this.keep_Button.text = "Continue in FITSFileManager\nKeep checked files";
   this.keep_Button.toolTip = "Keep checked files in input list";
   this.keep_Button.enabled = true;
   this.keep_Button.onClick = function() {
      this.dialog.done(CompletionDialog_doneKeep);
   }
   this.remove_Button = new PushButton( this );
   this.remove_Button.text = "Continue in FITSFileManager\nRemove checked files";
   this.remove_Button.toolTip = "Remove checked files from input list";
   this.remove_Button.enabled = true;
   this.remove_Button.onClick = function() {
      this.dialog.done(CompletionDialog_doneRemove);
   }
   this.leave_Button = new PushButton( this );
   this.leave_Button.text = "Leave FITSFileManager";
   this.leave_Button.toolTip = "Exit FITS file manager";
   this.leave_Button.enabled = true;
   this.leave_Button.onClick = function() {
      this.dialog.done(CompletionDialog_doneLeave);
   }



   // Sizer for Operation List and Actions section

   this.buttonSizer = new HorizontalSizer;
   this.buttonSizer.spacing = 2;
   this.buttonSizer.add( this.continue_Button);
   this.buttonSizer.add( this.keep_Button);
   this.buttonSizer.add( this.remove_Button);
   this.buttonSizer.add( this.leave_Button);
   this.buttonSizer.addStretch();


   this.sizer = new VerticalSizer;
   this.sizer.margin = 6;
   this.sizer.add( this.resultBox );
   this.sizer.add( this.buttonSizer );
   this.setVariableSize();
   this.adjustToContents();

   this.setResultText = function (text) {
      this.resultBox.text = text;
   }
   this.setResultModeMove = function() {
      this.continue_Button.enabled = true;
      this.keep_Button.enabled = false;
      this.remove_Button.enabled = false;
   }
   this.setResultModeCopy = function () {
      this.continue_Button.enabled = false;
      this.keep_Button.enabled = true;
      this.remove_Button.enabled = true;
   }
}

CompletionDialog.prototype = new Dialog;




// ------------------------------------------------------------------------------------------------------------------------
// FITS and synthetic keys dialog
// ------------------------------------------------------------------------------------------------------------------------
// Present a dialog with:
//   A selection of the files (drop down)
//   A list of  FITS keywords (selection box, keyword, value)  of the selected file, as a TreeBox
// ---------------------------------------------------------------------------------------------------------
function FITSKeysDialog( parentDialog, engine) {
   this.__base__ = Dialog;
   this.__base__();
   this.windowTitle = "Select FITS keywords to show in main window";

   // ComboBox to select the file to display values
   this.file_ComboBox = new ComboBox( this );

   // A file was selected (also called to initialize)
   this.file_ComboBox.onItemSelected = function( indexInComboBox ) {
      var fileName = this.itemText(indexInComboBox);
      var indexInFiles = engine.inputFiles.indexOf(fileName);
#ifdef DEBUG
      debug("FITSKeysDialog: file_ComboBox: onItemSelected(" + indexInComboBox + ") -  indexInFiles = " + indexInFiles + ", keywordsSet.size() = " + engine.keywordsSet.size());
#endif

     this.dialog.populate(indexInFiles);

   }

   // TreeBox to display list of FITS keywords
   this.keyword_TreeBox = new TreeBox( this );
   this.keyword_TreeBox.toolTip = "Synthetic and value keywords of the image selected in the drop box at top,\n" +
      "Tick the check mark to include the keyword value in the input file table\nThe red color indicates that the keyword is not in selected image, but appears in some other loaded images.";
   this.keyword_TreeBox.rootDecoration = false;
   this.keyword_TreeBox.numberOfColumns = 3;
   this.keyword_TreeBox.setHeaderText(0, "name");
   this.keyword_TreeBox.setHeaderText(1, "value");
   this.keyword_TreeBox.setHeaderText(2, "type");
   this.keyword_TreeBox.setHeaderText(3, "comment");
   this.keyword_TreeBox.setColumnWidth(0,150);
   this.keyword_TreeBox.setColumnWidth(1,200);
   this.keyword_TreeBox.setColumnWidth(2,50);
   this.keyword_TreeBox.setColumnWidth(3,600);


   // Buttons
   this.cancel_Button = new PushButton( this );
   this.cancel_Button.text = "Cancel";
   this.cancel_Button.enabled = true;
   this.cancel_Button.onClick = function() {
      this.dialog.cancel();
   }
   this.ok_Button = new PushButton( this );
   this.ok_Button.text = "OK";
   this.ok_Button.enabled = true;
   this.ok_Button.onClick = function() {
#ifdef DEBUG
      debug("FITSKeysDialog: ok_Button: onClick - save check/uncheck status in shownFITSKeyNames ");
#endif
      var fitsRoootNode = this.parent.keyword_TreeBox.child(1);
      var allFITSKeyNames = engine.keywordsSet.allValueKeywordNameList;
      // Clear and rebuild list of keywords to show
      engine.shownFITSKeyNames = {};
      for (var i = 0; i< allFITSKeyNames.length; i++) {
         var checked = fitsRoootNode.child(i).checked; // List and rows are in same order
         var name = allFITSKeyNames[i];
         if (checked) {
             engine.shownFITSKeyNames[name] = true;
         }
      }
      var variableRoootNode = this.parent.keyword_TreeBox.child(0);
      engine.shownSyntheticKeyNames = {};
      for (var i = 0; i< syntheticVariableNames.length; i++) {
         var checked = variableRoootNode.child(i).checked; // List and rows are in same order
         var name = syntheticVariableNames[i];
         if (checked) {
             engine.shownSyntheticKeyNames[name] = true;
         }
      }
      this.dialog.ok();
   }

   this.buttonsSizer = new HorizontalSizer;
   this.buttonsSizer.spacing = 2;
   this.buttonsSizer.addStretch();
   this.buttonsSizer.add( this.cancel_Button);
   this.buttonsSizer.add( this.ok_Button);


   // Assemble FITS keyword Dialog
   this.sizer = new VerticalSizer;
   this.sizer.margin = 4;
   this.sizer.spacing = 4;
   this.sizer.add( this.file_ComboBox );
   this.sizer.add( this.keyword_TreeBox );
   this.sizer.add(this.buttonsSizer);
   this.adjustToContents();

   // ------------------------------------------------------------
   // Recreate the content when the dialog is showns
   // This method recreate the columnsm key names, populate the list of file and call initializer for data
   this.onShow = function()
   {
#ifdef DEBUG
      debug("FITSKeysDialog: file_ComboBox: onShow()");
#endif
      // -- Locate dialog
      var p = new Point( parentDialog.position );
      p.moveBy( 16,16 );
      this.position = p;

      // -- Update the DropDown box - Fill list of files from parent list of files, to be in the same order
      // Note - this requires finding the proper index in inputFile[] when a selection is done, as order may be different
      this.file_ComboBox.clear();
      for (i = 0; i<parentDialog.filesTreeBox.numberOfChildren; i++) {
         this.file_ComboBox.addItem(parentDialog.filesTreeBox.child(i).text(0));
      }
////    OLD: This is filling the list in the loaded file order
//      for (i = 0; i< engine.inputFiles.length; i++) {
//         this.file_ComboBox.addItem(engine.inputFiles[i]);
//      }

      var selectedFileIndex = 0;
      var fileIndex = 0;
      if (parentDialog.filesTreeBox.selectedNodes.length >0) {
         // Show first selected node
         var firstSelectedNode = parentDialog.filesTreeBox.selectedNodes[0];
         var fileName = firstSelectedNode.text(0);
         fileIndex = engine.inputFiles.indexOf(fileName);
         selectedFileIndex = parentDialog.filesTreeBox.childIndex(firstSelectedNode);
#ifdef DEBUG
         debug("FITSKeysDialog: file_ComboBox: onShow - fileName="+fileName+", fileIndex=" + fileIndex+ ", selectedFileIndex=" + selectedFileIndex);
#endif
      }
      // Select the file in the combo box
      this.file_ComboBox.currentItem = selectedFileIndex;


      // -- Rebuild the 3 lists of keywords in the tree box

      this.keyword_TreeBox.clear();

      // Create list of synthetic keywords as a fist subtree
      var synthRootNode = new TreeBoxNode(this.keyword_TreeBox);
      synthRootNode.expanded = true;
      synthRootNode.setText(0,"Synthetic keywords");


      // Fill the name column form a fixed list of synthetic keywords names
      for (var i =0; i<syntheticVariableNames.length; i++) {
         var node = new TreeBoxNode(synthRootNode);
         node.setText( 0, syntheticVariableNames[i] );
         node.checked = engine.shownSyntheticKeyNames.hasOwnProperty(syntheticVariableNames[i]);;
      }

      // Create list of FITS keywords used as variables as a second subtree
      var fitsVarRootNode = new TreeBoxNode(this.keyword_TreeBox);
      fitsVarRootNode.expanded = true;
      fitsVarRootNode.setText(0,"FITS keywords used as variable");


      // Fill the name columns from the from allFITSKeyNames (accumulated names of all keywords)
      var allFITSKeyNames = engine.keywordsSet.allValueKeywordNameList;
      for (var i =0; i<allFITSKeyNames.length; i++) {
         var node = new TreeBoxNode(fitsVarRootNode);
         node.setText( 0, allFITSKeyNames[i] );
         node.checked = engine.shownFITSKeyNames.hasOwnProperty(allFITSKeyNames[i]);
      }


      // Populate with default file
      this.populate(fileIndex);


      this.setMinSize(700,600);
   };

   this.getTypeString = function (fitsKey) {
      if (fitsKey.isBoolean) {
         return "bool";
      } else if (fitsKey.isNumeric) {
         return "num";
      } else if (fitsKey.isString) {
         return "str";
      } else if (fitsKey.isNull) {
         return "null";
      }
      return "";

   }

   // Populate from information of inputFile[index] and inputFITSKeywords[index]
   this.populate = function (index) {

#ifdef DEBUG
      debug("FITSKeysDialog: file_ComboBox: populate() - index=" + index + " for file " + engine.inputFiles[index] );
#endif

      // Update the values of the synthethic keywords from a predefined list and engine values
      var synthRootNode = this.keyword_TreeBox.child(0);

      for (var i =0; i<syntheticVariableNames.length; i++) {
         var keyName = syntheticVariableNames[i];
         var variables = engine.inputVariables[index];
         var variable = variables[keyName];
         if (variable !== null) {
            synthRootNode.child(i).setTextColor(0,0x00000000);
            // TODO ADHOC TEMPORARY TEST
            if (typeof variable !== 'string') {
               variable = '?[' + typeof variable +  ']';
            }

            synthRootNode.child(i).setText(1,variable);
            synthRootNode.child(i).setText(2,'');
            synthRootNode.child(i).setText(3,syntheticVariableComments[i]);
         } else {
            synthRootNode.child(i).setTextColor(0,0x00FF0000);
            synthRootNode.child(i).setText(1,'');
            synthRootNode.child(i).setText(2,'');
            synthRootNode.child(i).setText(3,'');
         }
      }

      // Update FITS key words values from engine information
      var fitsVarRootNode = this.keyword_TreeBox.child(1);

      var allFITSKeyNames = engine.keywordsSet.allValueKeywordNameList;
      var imageKeywords = engine.inputFITSKeywords[index];
      for (var i = 0; i<allFITSKeyNames.length; i++) {
         var keyName = allFITSKeyNames[i];
         var keyValue = imageKeywords.getValueKeyword(keyName);
#ifdef DEBUG_FITS
         debug("FITSKeysDialog: file_ComboBox: onItemSelected - keyName=" + keyName + ",  keyValue=" + keyValue );
#endif
         if (keyValue !== null) {
            fitsVarRootNode.child(i).setTextColor(0,0x00000000);
            fitsVarRootNode.child(i).setText(1,keyValue.value);
            fitsVarRootNode.child(i).setText(2,this.getTypeString(keyValue));
            fitsVarRootNode.child(i).setText(3,keyValue.comment);
         } else {
            fitsVarRootNode.child(i).setTextColor(0,0x00FF0000);
            fitsVarRootNode.child(i).setText(1,'');
            fitsVarRootNode.child(i).setText(2,'');
            fitsVarRootNode.child(i).setText(3,'');
         }
      }

   }

};


FITSKeysDialog.prototype = new Dialog;


