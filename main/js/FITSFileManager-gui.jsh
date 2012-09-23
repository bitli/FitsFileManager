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

// Help texts


#define TARGET_TEMPLATE_TOOLTIP "\
Define how the target file name will be generated. The text is copied \
<em>as is</em> to the output except for keywords.<br\>\
Keywords (like  &amp;keyword;) are replaced by values defined from the file information and FITS keywords as follows:\
<dl>\
   <dt>&amp;binning;</dt><dd>Binning from XBINNING and YBINNING as integers, like 2x2.</dd>\
   <dt>&amp;exposure;</dt><dd>The exposure from EXPOSURE, but as an integer (assume seconds).<\dd>\
   <dt>&amp;extension;</dt><dd>The extension of the source file (with the dot.)<\dd>\
   <dt>&amp;filename;</dt><dd>The file name part of the source file.<\dd>\
   <dt>&amp;filter;</dt><dd>The filter name from FILTER as lower case trimmed normalized name.<\dd>\
   <dt>&amp;temp;</dt><dd>The SET-TEMP temperature in C as an integer.<\dd>\
   <dt>&amp;type;</dt><dd>The IMAGETYP normalized to 'flat', 'bias', 'dark', 'light'.<\dd>\
   <dt>&amp;FITSKW;</dt><dd>(NOT IMPLEMENTED).<\dd>\
   <dt>&amp;0; &amp;1;, ... </dt><dd>The corresponding match from the source file name template field.<\dd>\
</dl>\
<p>The following keywords are dynamic (their values depends on the file order):\
<dl>\
   <dt>&amp;count;</dt><dd>The number of the file being moved/copied int the current group, padded to COUNT_PAD.<\dd>\
   <dt>&amp;rank;</dt><dd>The number of the file in the order of the input file list, padded to COUNT_PAD.<\dd>\
</dl>You can enter the template or select one of the predefined one and optionaly modify it.\
"


#define SOURCE_FILENAME_REGEXP_TOOLTIP "\
Define  a regular expression (without the surround slashes) that will be applied to all file names \
without the extension. The 'match' array resulting from the regular expression matching can be used \
in the target file name template as &0; (whole expression), &1 (first group), ... \
In case of error the field turns red. \
You can enter the regexp or select one of the predefined one and optionaly modify it. \
<br/>See https:\/\/developer.mozilla.org\/en-US\/docs\/JavaScript\/Guide\/Regular_Expressions for more informations on regular expresssions. \
"


#define GROUP_TEMPLATE_TOOLTIP "\
Define the template to generate a group name used by &count;. \
Each group has its own group number starting at 1. You can use the same variables \
as for the target file name, except &count;. In addition you can use:\
<dl><dt>&targetDir;</dt><dd>The directory part of the target file name (except that &count; is not replaced).</dd>\
</dl>Leave blank or use a fixed name to have a single counter. For example '&targetDir;' count in each target \
directory.' &filter;' count separetely for each filter. \
You can enter the template or select one of the predefined one and optionaly modify it.\
"

#define BASE_HELP_TEXT "\
<p>FITSFileManager allow to copy or move image files to new locations, building the \
new location from a template and replacement of variables extracted from FITS keys \
and other information.\
<p/>You select the files to move/copy (files can be individually checked or un-checked) \
and enter the template to generate the target name using variables to substitute values \
based on image file name or keywords. \
<p/>The variables have the general form &name:present?absent;. The 'name' represent the variable. \
<ul><li>The 'present' part is the string that will be used if the variable \
is present - usually ':present' is not specified and the value of the variable is used as the replacement string. It can also \
be empty, in which case the variable is checked for presence (an error is generated if it is missing) \
but its value does not contribute to the target string.</li> \
<li>The '?absent' part is used if the variable is not present in the file (for example '&type?light;'). \
You can also have an empty absent part (like '&binning?;') in which case there is no error if the variable  \
is not present. </li>\
</ul><p>The variables are defined in the section 'target template' below. They are built from the FITS keywords, \
the number of the file being processed or are result of a regular expression applied to the file name. \
The regular expression can be used, for example, to extract the part of the file name \
before the first dash. \
<p/>The files are processed in the order they appear in the table (variable &amp;rank;). \
In addition a 'group' string can be generated using the same template rules and a &amp;count; \
variable is increased for each different group (for example each target directory). \
"


#define HELP_TEXT ("<html>" + \
"<h1><font color=\"#06F\">FITSFileManager</font></h1>" + BASE_HELP_TEXT + \
"<h3><font color=\"#06F\">Target template</font></h3/>" + TARGET_TEMPLATE_TOOLTIP + \
"</dl>Example of template:\<br\><tt>&nbsp;&nbsp;&nbsp;&amp;1;_&amp;binning;_&amp;temp;C_&amp;type;_&amp;exposure;s_&amp;filter;_&amp;count;&amp;extension;</tt>"+\
"<h3><font color=\"#06F\">Source filename reg exp</font></h3>" + SOURCE_FILENAME_REGEXP_TOOLTIP + \
"Example of regular expression:<br\><tt>&nbsp;&nbsp;&nbsp;([^-_.]+)(?:[._-]|$)</tt><p>" + \
"<h3><font color=\"#06F\">Group template</font></h3>" +  GROUP_TEMPLATE_TOOLTIP + \
"Example of group definition:<br/><tt>&nbsp;&nbsp;&nbsp;&amp;targetdir;</tt><p> " + \
"</html>")



// ------------------------------------------------------------------------------------------------------------------------
// User Interface Parameters
// ------------------------------------------------------------------------------------------------------------------------

// The GUI parameters keeps track of the parameters that are saved between executions

function FFM_GUIParameters() {

   this.reset = function () {

      // SETTINGS: Saved latest correct GUI state
      this.targetFileNameTemplate = FFM_DEFAULT_TARGET_FILENAME_TEMPLATE;

      // Default regular expression to parse file name
      this.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;

      this.orderBy = "&rank;" // UNUSED
      // Default template to create groups
      this.groupByTemplate = FFM_DEFAULT_GROUP_TEMPLATE;

      var templateErrors = [];
      this.targetFileNameCompiledTemplate = ffM_template.analyzeTemplate(templateErrors, FFM_DEFAULT_TARGET_FILENAME_TEMPLATE);
      this.groupByCompiledTemplate = ffM_template.analyzeTemplate(templateErrors, FFM_DEFAULT_GROUP_TEMPLATE);
      if (templateErrors.length>0) {
         throw "PROGRAMMING ERROR - default built in templates invalid";
      }

      // The predefined templates and regexp, an array of value and an arary of comments
      // In theGUI parameters as they could be made configurable by the user
      this.regexpItemListText = [
         regExpToString(this.sourceFileNameRegExp), // Must be adapted after parameter loading
         FFM_DEFAULT_SOURCE_FILENAME_REGEXP,
         "/.*/"
      ];
      this.regexpItemListComment = [
         "last",
         "extract name",
         "(everything)"
      ];


      this.groupItemListText = [
            this.groupByCompiledTemplate.templateString, // Must be adapted after parameter loading
            FFM_DEFAULT_GROUP_TEMPLATE,
            "&filter;",
            ""
      ];
      this.groupItemListComment = [
            "last",
            "by directory (default)",
            "by filter",
            "none"
      ];

      this.targetFileItemListText = [
            this.targetFileNameCompiledTemplate.templateString, // Must be adapted after parameter loading
            FFM_DEFAULT_TARGET_FILENAME_TEMPLATE,
            "&type;/&1;_&binning;_&temp;C_&exposure;s_&filter;_&count;&extension;",
            "&filter;_&count;&extension;",
            "&1;_&type?light;_&filter?clear;_&count;&extension;",
            ""
      ];
      this.targetFileItemListComment = [
            "last",
            "detailled",
            "directory by type",
            "just filter",
            "type and filter with defaults",
            "(clear)"
      ];


   }
   this.reset();


   // For debugging and logging
   this.toString = function() {
      var s = "GUIParameters:\n";
      s += "  targetFileNameTemplate:         " + replaceAmps(this.targetFileNameCompiledTemplate.templateString) + "\n";
      s += "  sourceFileNameRegExp:           " + replaceAmps(regExpToString(this.sourceFileNameRegExp)) + "\n";
      s += "  orderBy:                        " + replaceAmps(this.orderBy) + "\n";
      s += "  groupByTemplate:                " + replaceAmps(this.groupByCompiledTemplate.templateString) + "\n";
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

   var o, t, templateErrors;
   if ( (o = load( "version",    DataType_Double )) !== null ) {
      if (o > VERSION) {
         Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' have version ", o, " later than script version ", VERSION, ", settings ignored");
      } else {
         if ( (o = load( "targetFileNameTemplate",    DataType_String )) !== null ) {
           templateErrors = [];
           t =   ffM_template.analyzeTemplate(templateErrors,o);
           if (templateErrors.length===0) {
               this.targetFileNameCompiledTemplate = t; // Template correct
           }

         };
         if ( (o = load( "sourceFileNameRegExp",    DataType_String )) !== null ) {
            try {
               this.sourceFileNameRegExp = RegExp(o);
            } catch (err) {
               // Default in case of error in load
               this.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;
#ifdef DEBUG
               debug("loadSettings: bad regexp - err: " + err);
#endif
            }
         };
         if ( (o = load( "orderBy",                  DataType_String )) !== null ) {
            this.orderBy = o;
         }
         if ( (o = load( "groupByTemplate",          DataType_String )) !== null ) {
            templateErrors = [];
            t = ffM_template.analyzeTemplate(templateErrors, o);
            if (templateErrors.length ===0) {
               this.groupByCompiledTemplate = t;
            }
         }

         // Restore the 'last' value in the list of predfined choices
         this.regexpItemListText[0] = regExpToString(this.sourceFileNameRegExp);
         this.groupItemListText[0] = this.groupByCompiledTemplate.templateString;
         this.targetFileItemListText[0] = this.targetFileNameCompiledTemplate.templateString;
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

   save( "version",                    DataType_Double, parseFloat(VERSION) );
   save( "targetFileNameTemplate",     DataType_String, this.targetFileNameCompiledTemplate.templateString );
   save( "sourceFileNameRegExp",       DataType_String, regExpToString(this.sourceFileNameRegExp) );
   save( "orderBy",                    DataType_String,  this.orderBy );
   save( "groupByTemplate",            DataType_String,  this.groupByCompiledTemplate.templateString );

}

FFM_GUIParameters.prototype.targetTemplateSelection =  [
   FFM_DEFAULT_TARGET_FILENAME_TEMPLATE
];
FFM_GUIParameters.prototype.groupTemplateSelection = [
   FFM_DEFAULT_GROUP_TEMPLATE
];
FFM_GUIParameters.prototype.regexpSelection = [
   FFM_DEFAULT_SOURCE_FILENAME_REGEXP.toString()
];



// ------------------------------------------------------------------------------------------------------------------------
// SectionBar Control from Juan: http://pixinsight.com/forum/index.php?topic=4610.msg32012#msg32012
// ------------------------------------------------------------------------------------------------------------------------

#define contract_icon   new Bitmap( ":/images/icons/contract_v.png" )
#define expand_icon     new Bitmap( ":/images/icons/expand_v.png" )

function SectionBar( parent )
{
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

   var labelWidth = this.font.width( "MMMMMMMMMMMMMM" ) ;


   // -- FITSKeyWord Dialog (opened as a child on request)
   this.fitsKeysDialog = new FITSKeysDialog( this, engine );


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
   this.keyButton.toolTip = "KeyWord Dialog";
   this.keyButton.onClick = function() {
   if (this.dialog.engine.keyTable.length) {
         this.dialog.fitsKeysDialog.execute();
         this.dialog.hideKey();
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


   this.bar1 = new SectionBar( this );
   this.bar1.setTitle( "Input" );
   this.bar1.setCollapsedTitle( "Input - No file" );
   this.bar1.setSection( this.inputFiles_GroupBox );


   //----------------------------------------------------------------------------------
   // Rules section
   //----------------------------------------------------------------------------------

   // Target template --------------------------------------------------------------------------------------


   this.targetFileTemplate_ComboBox = new ComboBox( this );
   this.targetFileTemplate_ComboBox.toolTip = TARGET_TEMPLATE_TOOLTIP;
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
      var text = this.dialog.targetFileItemListText[this.currentItem];
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
      var text = regExpToString(this.dialog.regexpItemListText[this.currentItem]);
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
      var text = this.dialog.groupItemListText[this.currentItem];
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

   this.bar2 = new SectionBar( this );
   this.bar2.setTitle( "Rules" );
   this.bar2.setSection( this.rules_GroupBox );



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
         this.dialog.bar3.setCollapsedTitle( "Output base directory - " +  this.dialog.engine.outputDirectory);
         this.dialog.updateButtonState();
      }
   }

   this.outputDir_GroupBox = new GroupBox( this );
   this.outputDir_GroupBox.sizer = new HorizontalSizer;
   this.outputDir_GroupBox.sizer.margin = 6;
   this.outputDir_GroupBox.sizer.spacing = 4;
   this.outputDir_GroupBox.sizer.add( this.outputDir_Edit, 100 );
   this.outputDir_GroupBox.sizer.add( this.outputDirSelect_Button );

   this.bar3 = new SectionBar( this );
   this.bar3.setTitle( "Output base directory" );
   this.bar3.setCollapsedTitle( "Output base directory - not specified" );
   this.bar3.setSection( this.outputDir_GroupBox );


   //----------------------------------------------------------------------------------
   // Operation list and action section
   //----------------------------------------------------------------------------------


   // Result operations --------------------------------------------------------------------------------------
#ifdef USE_TREEBOX
   this.transform_TreeBox = new TreeBox( this );

   this.transform_TreeBox.rootDecoration = false;
   this.transform_TreeBox.numberOfColumns = 1;
   this.transform_TreeBox.multipleSelection = false;
   this.transform_TreeBox.headerVisible = false;
   this.transform_TreeBox.headerSorting = false;
   this.transform_TreeBox.setHeaderText(0, "Filename");
   //this.transform_TreeBox.sort(0,false);
   this.transform_TreeBox.setMinSize( 700, 200 );
#else
   this.transform_TextBox = new TextBox( this );
   this.transform_TextBox.frameStyle = FrameStyle_Box;
   this.transform_TextBox.text = '';
   this.transform_TextBox.toolTip = "Transformations that will be executed";
   this.transform_TextBox.enabled = true;
   this.transform_TextBox.readOnly = true;
#endif

   this.outputSummaryLabel = new Label( this );
   this.outputSummaryLabel.textAlignment = TextAlign_Left|TextAlign_VertCenter;

   this.outputFiles_GroupBox = new GroupBox( this );
   this.outputFiles_GroupBox.sizer = new VerticalSizer;
   this.outputFiles_GroupBox.sizer.margin = 6;
   this.outputFiles_GroupBox.sizer.spacing = 4;
#ifdef USE_TREEBOX
   this.outputFiles_GroupBox.sizer.add( this.transform_TreeBox, 100);
#else
   this.outputFiles_GroupBox.sizer.add( this.transform_TextBox, 100);
#endif
   this.outputFiles_GroupBox.sizer.add( this.outputSummaryLabel );


   this.bar4 = new SectionBar( this );
   this.bar4.setTitle( "Resulting operations" );
   this.bar4.setCollapsedTitle( "Resulting operations - None" );
   this.bar4.setSection( this.outputFiles_GroupBox );



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


   this.move_Button = new PushButton( this );
   this.move_Button.text = "Move files";
   this.move_Button.toolTip = "Move Checked files to output directory";
   this.move_Button.enabled = false;
   this.move_Button.onClick = function()
      {
         var listOfFiles = this.parent.makeListOfCheckedFiles();
         var errors = this.parent.engine.checkValidTargets(listOfFiles);
         if (errors.length > 0) {
            var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
            msg.execute();
            return;
         }
         this.parent.engine.executeFileOperations(0);
         this.parent.removeDeletedFiles();
         this.parent.refreshTargetFiles();
         //this.dialog.ok();
         // TODO Refresh source
      }


   this.refresh_Button = new PushButton( this );
   this.refresh_Button.text = "Refresh list";
   this.refresh_Button.toolTip = "Refresh the list of operations\nrequired after a sort on an header (there is on onSort event)";
   this.refresh_Button.enabled = true;
   this.refresh_Button.onClick = function()
      {
         this.parent.removeDeletedFiles();
         this.parent.refreshTargetFiles();
      }


   this.copy_Button = new PushButton( this );
   this.copy_Button.text = "Copy files";
      this.copy_Button.toolTip = "Copy Checked files to output directory";
      this.copy_Button.enabled = false;
      this.copy_Button.onClick = function()
      {
         var listOfFiles = this.parent.makeListOfCheckedFiles();
         var errors = this.parent.engine.checkValidTargets(listOfFiles);
         if (errors.length > 0) {
            var msg = new MessageBox( errors.join("\n"),
                   "Check failed", StdIcon_Error, StdButton_Ok );
            msg.execute();
            return;
         }
         this.parent.engine.executeFileOperations(1);
         //this.dialog.ok();
      }


#ifdef IMPLEMENTS_FITS_EXPORT
// Export FITS values button
   this.txt_Button = new PushButton( this );
   this.txt_Button.text = "Export FITS.txt";
   this.txt_Button.toolTip = "For Checked files write FitKeywords value to file FITS.txt in output directory";
   this.txt_Button.enabled = false;
   this.txt_Button.onClick = function() {
      this.parent.engine.exportFITSKeyWords();
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
   this.sizer.add(this.bar1);
   this.sizer.add( this.inputFiles_GroupBox,50 );
   this.sizer.add(this.bar2);
   this.sizer.add(this.rules_GroupBox);
   this.sizer.add(this.bar3);
   this.sizer.add( this.outputDir_GroupBox );
   this.sizer.add(this.bar4);
   this.sizer.add(this.outputFiles_GroupBox,50);
   this.sizer.add( this.buttonSizer );






   //----------------------------------------------------------------------------------
   // Support methods
   //----------------------------------------------------------------------------------

   var synthKeyList = ['type','binning','temp','filter','exposure'];

   // -- Hide columns of unchecked keywords (called to apply changes)
   this.hideKey = function () {
      for (var i = 0; i<this.engine.keyEnabled.length;i++) {
         var c = i + 1 + synthKeyList.length;
         this.filesTreeBox.showColumn( c, this.engine.keyEnabled[i]);
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


      this.engine.keyTable = []; // clear
      this.engine.keyEnabled = []; // clear


      for (var iSynthKey = 0; iSynthKey<synthKeyList.length; iSynthKey++) {
         var name = synthKeyList[iSynthKey];
         this.filesTreeBox.numberOfColumns++;// add new column
         this.filesTreeBox.setHeaderText(this.filesTreeBox.numberOfColumns-1, name);//set name of new column
#ifdef DEBUG
         debug("rebuildFilesTreeBox: added synth key header '" + name +  "' as col " + this.filesTreeBox.numberOfColumns);
#endif
      }



      // Accumulate all unique FITS keys in keyTable
      for (var i = 0; i < this.engine.inputFiles.length; ++i) {
         var keys = this.engine.inputKeys[i]; // keywords of one file
         var syntheticKeyWords = this.engine.inputVariables[i];

         // Create TreeBoxNode for file
         var node = new TreeBoxNode( this.filesTreeBox );
         //write name of the file to first column
         node.setText( 0, this.engine.inputFiles[i] );
         node.checked = true;

#ifdef DEBUG
         debug("rebuildFilesTreeBox: adding " + Object.keys(syntheticKeyWords) + " synthetics keys, " + keys.length + " FITS keys to row " + i);
#endif
         for (var iSynthKey = 0; iSynthKey<synthKeyList.length; iSynthKey++) {
            var name = synthKeyList[iSynthKey];
            // col 1 is filename
            var textSynthKey = syntheticKeyWords[name];
            node.setText(iSynthKey+1, textSynthKey ? textSynthKey : "");
         }

#ifdef DEBUG
         debug("rebuildFilesTreeBox: setting " + keys.length + " FITS keys to row " + i);
#endif

         for ( var iKeyOfFile = 0; iKeyOfFile<keys.length; iKeyOfFile++) {
            var name = keys[iKeyOfFile].name; //name of Keyword from file
            var k = this.engine.keyTable.indexOf(name);// find index of "name" in keyTable
            if (k < 0)  {
               // new keyName
#ifdef DEBUG_COLUMNS
               debug("rebuildFilesTreeBox: Creating new column " + this.filesTreeBox.numberOfColumns + " for '"  + name + "', total col len " + this.engine.keyTable.length);
#endif
               this.engine.keyTable.push(name);//add keyword name to table
               this.filesTreeBox.numberOfColumns++;// add new column
               this.filesTreeBox.setHeaderText(this.filesTreeBox.numberOfColumns-1, name);//set name of new column
               //console.writeln("*** " + this.filesTreeBox.numberOfColumns + " " + name);
               this.engine.keyEnabled.push (this.engine.defaultKey.indexOf(name)> -1);//compare with default enabled keywords

               //this.filesTreeBox.showColumn( this.filesTreeBox.numberOfColumns, this.keyEnabled[k]);
            }
            // TODO Supports other formatting (dates ?) or show raw text
            if (keys[iKeyOfFile].isNumeric) {
               node.setText(this.filesTreeBox.numberOfColumns-1, Number(keys[iKeyOfFile].value).toFixed(3) );
            } else {
               node.setText( this.filesTreeBox.numberOfColumns-1, keys[iKeyOfFile].value.trim() );
            }
         }
      }
      this.hideKey(); //hide the columns of unchecked FITS keywords
   }


   // -- enable/disable operation buttons
   this.updateButtonState = function()
   {
      var enabled = this.dialog.engine.canDoOperation();
      this.dialog.move_Button.enabled = enabled;
      this.dialog.copy_Button.enabled = enabled;
#ifdef IMPLEMENTS_FITS_EXPORT
      this.dialog.txt_Button.enabled = enabled;
#endif
   }

   // -- Add a list of files to the TreeBox (remove duplicates)
   this.addFilesAction = function (fileNames)
   {
      this.engine.addFiles(fileNames);

      this.rebuildFilesTreeBox();
      this.setMinWidth(800);
      this.adjustToContents();
      this.dialog.updateButtonState();
      this.dialog.updateTotal();

      this.refreshTargetFiles();
      //this.hideKey(); // *** TEST
   }

   // -- update the input file list total after each add/remove/check toogle
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
      this.bar1.setCollapsedTitle("Input - " + countText);
   }


   //.. return an array of files that are checked (ticked)
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
#ifdef DEBUG_TIMING
      var startTime, elapsedTime;
      startTime = Date.now().valueOf();
#endif

#ifdef DEBUG
      debug("refreshTargetFiles() called");
#endif
      var listOfFiles = this.makeListOfCheckedFiles();

      this.engine.buildTargetFiles(listOfFiles);


      // List of text accumulating the transformation rules for display
      var listOfTransforms = this.engine.makeListOfTransforms();
#ifdef DEBUG_TIMING
      elapsedTime = Date.now().valueOf() - startTime;
      console.writeln("refreshTargetFiles - rebuilt in " + elapsedTime + " ms");
#endif

#ifdef USE_TREEBOX
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
#else
      this.transform_TextBox.text = listOfTransforms.join("");
      this.transform_TextBox.caretPosition = 0;
#endif
#ifdef DEBUG_TIMING
      elapsedTime = Date.now().valueOf() - startTime;
      console.writeln("refreshTargetFiles - rebuilt and refreshed in " + elapsedTime + " ms");
#endif

      var nmbFilesExamined = this.engine.targetFiles.length;

      var bar4Title = "";
      if (nmbFilesExamined === 0) {
          bar4Title += "None";
      } else {
         bar4Title += "" + nmbFilesExamined + " files checked" ;
         if (this.engine.nmbFilesTransformed >0) {
            bar4Title += ", " + this.engine.nmbFilesTransformed + " to copy/move";
         }

         //      this.engine.nmbFilesSkipped;
         if (this.engine.nmbFilesInError >0) {
            bar4Title += ", " + this.engine.nmbFilesInError + " IN ERROR";
         }
      }
      this.outputSummaryLabel.text = bar4Title;
      this.bar4.setCollapsedTitle("Resulting operations - " + bar4Title);


    }


    // -- Support for refresh and move, remove all input files that are not present anymore
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

function HelpDialog( parentDialog, engine )
{
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
// FITS and syntehetic keys dialog
// ------------------------------------------------------------------------------------------------------------------------
// Present a dialog with:
//   A selection of the files (drop down)
//   A list of  FITS keywords (selection box, keyword, value)  of the selected file, as a TreeBox
// ---------------------------------------------------------------------------------------------------------
function FITSKeysDialog( parentDialog, engine)
{
   this.__base__ = Dialog;
   this.__base__();
   this.windowTitle = "Select FITS keywords for report";


   // ComboBox to select the file to display values
   this.file_ComboBox = new ComboBox( this );

   // A file was selected (also called to initialize)
   this.file_ComboBox.onItemSelected = function( index ) {
      // Assume that index in combox is same as index in inputfiles
#ifdef DEBUG
      debug("FITSKeysDialog: file_ComboBox: onItemSelected - " + index + " key table length = " + engine.keyTable.length);
#endif

     this.dialog.populate(index);

   }

   // TreeBox to display list of FITS keywords
   this.keyword_TreeBox = new TreeBox( this );
   this.keyword_TreeBox.toolTip = "Check mark to include in table\nname in red of keyword not in current file";
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


   // Export selected fits keywords for checked files
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
      debug("FITSKeysDialog: ok_Button: onClick");
#endif
      var fitsRoootNode = this.parent.keyword_TreeBox.child(1);
      for (var i =0; i< engine.keyTable.length; i++) {
         var checked = fitsRoootNode.child(i).checked;
         engine.keyEnabled[i] = checked;
      }
      parentDialog.setMinWidth(800);
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
      // -- Locate dialog
      var p = new Point( parentDialog.position );
      p.moveBy( 16,16 );
      this.position = p;


      // -- Rebuild the list of keywords

      this.keyword_TreeBox.clear();

      // Create list of synthetic keywords as a fist subtree
      var synthRoootNode = new TreeBoxNode(this.keyword_TreeBox);
      synthRoootNode.expanded = true;
      synthRoootNode.setText(0,"Synthetic keywords");


      // Fill list of keywords global variable
      for (var i =0; i<shownSyntheticVariables.length; i++) {
         var node = new TreeBoxNode(synthRoootNode);
         node.setText( 0, shownSyntheticVariables[i] );
         node.checked = true;
      }

      // Create list of FITS keywords as a second subtree
      var fitsRoootNode = new TreeBoxNode(this.keyword_TreeBox);
      fitsRoootNode.expanded = true;
      fitsRoootNode.setText(0,"FITS keywords");


      // Fill FITS keyword names from keyTable (accumulated name of all keywords)
      for (var i =0; i<engine.keyTable.length; i++) {
         var node = new TreeBoxNode(fitsRoootNode);
         node.setText( 0, engine.keyTable[i] );
         node.checked = engine.keyEnabled[i];
      }


      // -- Update the DropDown box - Fill list of files from parent list of files
      this.file_ComboBox.clear();
      for (i = 0; i< engine.inputFiles.length; i++) {
         this.file_ComboBox.addItem(engine.inputFiles[i]);
      }

      // TODO position on select file on input, if any
      this.populate(0);

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

   // Populate from information of inputFile[index]
   this.populate = function (index) {

      // Update the values of the synthethic keywords from a predefined list and engine values
      var synthRootNode = this.keyword_TreeBox.child(0);

      for (var i =0; i<shownSyntheticVariables.length; i++) {
         var keyName = shownSyntheticVariables[i];
         var variables = engine.inputVariables[index];
         var variable = variables[keyName];
         if (variable !== null) {
            synthRootNode.child(i).setTextColor(0,0x00000000);
            synthRootNode.child(i).setText(1,variable);
            synthRootNode.child(i).setText(2,'');
            synthRootNode.child(i).setText(3,shownSyntheticComments[i]);
         } else {
            synthRootNode.child(i).setTextColor(0,0x00FF0000);
            synthRootNode.child(i).setText(1,'');
            synthRootNode.child(i).setText(2,'');
            synthRootNode.child(i).setText(3,'');
         }
      }

      // Update FITS key words values from engine information
      var fitsRoootNode = this.keyword_TreeBox.child(1);

      for (var i = 0; i<engine.keyTable.length; i++) {
         var keyName = engine.keyTable[i];
         var keys = engine.inputKeys[index];
         // TODO - Refactor lookup to engine
         var keyWord = null;
         for (var j = 0; j<keys.length; j++) {
            if (keys[j].name === keyName) {
               keyWord = keys[j];
               break;
            }
         }
#ifdef DEBUG_FITS
         debug("FITSKeysDialog: file_ComboBox: onItemSelected - keyName=" + keyName + ",  keyWord=" + keyWord );
#endif
         if (keyWord !== null) {
            fitsRoootNode.child(i).setTextColor(0,0x00000000);
            fitsRoootNode.child(i).setText(1,keyWord.value);
            fitsRoootNode.child(i).setText(2,this.getTypeString(keyWord));
            fitsRoootNode.child(i).setText(3,keyWord.comment);
         } else {
            fitsRoootNode.child(i).setTextColor(0,0x00FF0000);
            fitsRoootNode.child(i).setText(1,'');
            fitsRoootNode.child(i).setText(2,'');
            fitsRoootNode.child(i).setText(3,'');
         }
      }

   }

};




FITSKeysDialog.prototype = new Dialog;


