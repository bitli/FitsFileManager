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

//#define HELP_STYLE "<style>{font-family: \"DejaVu Sans\",Verdana,Arial,Helvetica,sans-serif; font-size:12px;}" +\
//"h1 {font-family: \"DejaVu Sans\",Verdana,Arial,Helvetica,sans-serif; font-size: 24pt; font-weight: normal; line-height: 1.2em; letter-spacing: -0.5px; margin-top: 1em; margin-bottom: 0.5em; color: #06F;}" +\
//"h3 {clear: both; border-bottom: 1px solid #999; padding-bottom: 0.1em; margin-top: 1.5em; margin-bottom: 1.25em; font-size: 16pt; font-weight: normal; line-height: 1.2em; color: #06F;}</style>"

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
</dl>\
"


#define SOURCE_FILENAME_REGEXP_TOOLTIP "\
Define  a regular expression (without the surround slashes) that will be applied to all file names\n\
without the extension. The 'match' array resulting from the regular expression matching can be used\n\
in the target file name template as &0; (whole expression), &1 (first group), ...\n\
The default extract the part of the name before the first dash (you can replace the\n\
two dashes by two underlines for example).\n\
In case of error the field turns red\n\
"

#define GROUP_TEMPLATE_TOOLTIP "\
Define the template to generate a group name used by &count;.\n\
Each group has its own group number starting at 1. You can use the same variables\n\
as for the target file name, except &count;. In addition you can use:\n\
   &targetDir;    The directory part of the target file name (except that &count; is not replaced).\n\
Leave blank or use a fixed name to have a single counter. The default &targetDir; count in each target\n\
directory. &filter; would count separetely for each filter.\n\
"

#define BASE_HELP_TEXT "\
<p>FITSFileManager allow to copy or move image files to new locations, building the \
new location from a template and replacement of variables extracted from FITS keys \
and other information\
</p>"

// #define HELP_TEXT ("<style>" + HELP_STYLE + "</style><body>" + \
// "<h1>FITSFileManager</h1>" + BASE_HELP_TEXT + \
// "<h3>Target template</h3>" + TARGET_TEMPLATE_TOOLTIP + \
// "<h3>Source filename template</h3>" + SOURCE_FILENAME_REGEXP_TOOLTIP + \
// "<h3>Group template</h3>" +  GROUP_TEMPLATE_TOOLTIP + "</body>")

#define HELP_TEXT ("<html>" + \
"<h1><font color=\"#06F\">FITSFileManager</font></h1>" + BASE_HELP_TEXT + \
"<h3><font color=\"#06F\">Target template</font></h3/>" + TARGET_TEMPLATE_TOOLTIP + \
"<h3><font color=\"#06F\">Source filename template</font></h3>" + SOURCE_FILENAME_REGEXP_TOOLTIP + \
"<h3><font color=\"#06F\">Group template</font></h3>" +  GROUP_TEMPLATE_TOOLTIP + \
"</html>")



// ------------------------------------------------------------------------------------------------------------------------
// User Interface Parameters
// ------------------------------------------------------------------------------------------------------------------------

// The GUI parameters keeps track of the parameters that are saved between executions

function FFM_GUIParameters() {

   this.reset = function () {

      // SETTINGS: Saved latest correct GUI state
      this.targeFileNameTemplate = FFM_DEFAULT_TARGET_FILENAME_TEMPLATE;

      // Default regular expression to parse file name
      this.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;

      this.orderBy = "&rank;" // UNUSED
      // Default template to create groups
      this.groupByTemplate = FFM_DEFAULT_GROUP_TEMPLATE;
    }
   this.reset();


   // For debugging and logging
   this.toString = function() {
      var s = "GUIParameters:\n";
      s += "  targeFileNameTemplate:           " + replaceAmps(this.targeFileNameTemplate) + "\n";
      s += "  sourceFileNameRegExp:           " + replaceAmps(regExpToString(this.sourceFileNameRegExp)) + "\n";
      s += "  orderBy:                        " + replaceAmps(this.orderBy) + "\n";
      s += "  groupByTemplate:                 " + replaceAmps(this.groupByTemplate) + "\n";
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
   if ( (o = load( "version",    DataType_Double )) !== null ) {
      if (o > VERSION) {
         Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' have version ", o, " later than script version ", VERSION, ", settings ignored");
      } else {
         if ( (o = load( "targeFileNameTemplate",    DataType_String )) !== null ) {
            this.targeFileNameTemplate = o;
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
         if ( (o = load( "orderBy",                 DataType_String )) !== null )
            this.orderBy = o;
         if ( (o = load( "groupByTemplate",          DataType_String )) !== null )
            this.groupByTemplate = o;
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
   save( "targeFileNameTemplate",     DataType_String,  this.targeFileNameTemplate );
   save( "sourceFileNameRegExp",     DataType_String,  regExpToString(this.sourceFileNameRegExp) );
   save( "orderBy",                  DataType_String,  this.orderBy );
   save( "groupByTemplate",           DataType_String,  this.groupByTemplate );

}



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

function MainDialog(engine, guiParameters)
{
   this.__base__ = Dialog;
   this.__base__();
   this.engine = engine;

   // -- FITSKeyWord Dialog (opened as a child on request)
   this.fitsKeysDialog = new FITSKeysDialog( this, engine );


   // -- HelpLabel
   var helpLabel = new Label( this );
   helpLabel.frameStyle = FrameStyle_Box;
   helpLabel.margin = 4;
   helpLabel.wordWrapping = true;
   helpLabel.useRichText = true;
   helpLabel.text = "<b>" + TITLE + " v" + VERSION + "</b> &mdash; Copy or move FITS image " +
           "files using selected FITS keyword values or original file name template " +
           "to create the target directory/file name.";


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

      this.filesTreeBox.setMinSize( 600, 200 );

      // Assume that 'check' is the only operation that update the nodes,
      // this may not be true...
      this.filesTreeBox.onNodeUpdated = function( node, column ) // Invert CheckMark
      {

#ifdef DEBUG_EVENTS
         debug("filesTreeBox: onNodeUpdated("+node+","+column+")");
#endif
         for (var i=0; i < this.selectedNodes.length; i++)
         {
            if ( node === this.selectedNodes[i] ) continue; // skip curent clicked node, because it will inverted automaticaly
            this.selectedNodes[i].checked = !this.selectedNodes[i].checked;
         }
         this.dialog.updateTotal();
         this.dialog.refreshTargetFiles();
      };
#ifdef DEBUG_EVENTS
      this.filesTreeBox.onCurrentNodeUpdated = function(node) {
         debug("filesTreeBox: onCurrentNodeUpdated("+node+")");
      };
      this.filesTreeBox.onNodeActivated = function(node) {
         debug("filesTreeBox: onNodeActivated("+node+")");
      };
      this.filesTreeBox.onNodeClicked = function(node) {
         debug("filesTreeBox: onNodeClicked("+node+")");
      };
      this.filesTreeBox.onNodeCollapsed = function(node) {
         debug("filesTreeBox: onNodeCollapsed("+node+")");
      };
      this.filesTreeBox.onNodeDoubleClicked = function(node) {
         debug("filesTreeBox: onNodeDoubleClicked("+node+")");
      };
      this.filesTreeBox.onNodeEntered = function(node) {
         // this is not called unless mouse events are enabled
         debug("filesTreeBox: onNodeEntered("+node+")");
      };
      this.filesTreeBox.onNodeExpanded = function(node) {
         debug("filesTreeBox: onNodeExpanded("+node+")");
      };
      this.filesTreeBox.onNodeSelectionUpdated = function() {
         debug("filesTreeBox: onNodeSelectionUpdated()");
      };
#endif


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


   // -- Remove selected files
   this.remove_files_Button = new ToolButton( this );
   this.remove_files_Button.icon = new Bitmap( ":/images/close.png" );
   this.remove_files_Button.toolTip = "<p>Removed selected images from the list.</p>";
   this.remove_files_Button.onClick = function()
      {
#ifdef DEBUG
         debug("Remove files");
#endif

         for ( var iTreeBox = this.dialog.filesTreeBox.numberOfChildren; --iTreeBox >= 0; )
         {

            if ( this.dialog.filesTreeBox.child( iTreeBox ).selected )
            {
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
   this.remove_all_files_Button.toolTip = "<p>Removed all images from the list.</p>";
   this.remove_all_files_Button.onClick = function()
      {
#ifdef DEBUG
         debug("Remove all files (" + this.dialog.engine.inputFiles.length + ")");
#endif

         // TODO We can probably clear in one go
         for ( var i = this.dialog.filesTreeBox.numberOfChildren; --i >= 0; )
         {
               this.dialog.filesTreeBox.remove( i );
         }
         this.dialog.engine.reset();
         this.dialog.updateTotal();
         this.dialog.updateButtonState();
         // Refresh the generated files
         this.dialog.refreshTargetFiles();

      }

   // -- Total file Label ---------------------------------------------------------------------------
   this.QTY = new Label( this );
   this.QTY.textAlignment = TextAlign_Right|TextAlign_VertCenter;

   // -- Sizer for Input Files Section

   this.fileButonSizer = new HorizontalSizer;
   this.fileButonSizer.margin = 6;
   this.fileButonSizer.spacing = 4;
   this.fileButonSizer.add( this.keyButton );
   this.fileButonSizer.add( this.filesAdd_Button );
   this.fileButonSizer.add( this.dirAdd_Button );
   this.fileButonSizer.add( this.remove_files_Button );
   this.fileButonSizer.add( this.remove_all_files_Button );
   this.fileButonSizer.add( this.QTY );
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
   this.targetFileTemplate_Edit = new Edit( this );
   this.targetFileTemplate_Edit.text = guiParameters.targeFileNameTemplate;
   this.targetFileTemplate_Edit.toolTip = TARGET_TEMPLATE_TOOLTIP;
   this.targetFileTemplate_Edit.enabled = true;
   this.targetFileTemplate_Edit.onTextUpdated = function()
      {
         guiParameters.targeFileNameTemplate = this.text;
         this.dialog.refreshTargetFiles();

      }


   // Source file name template --------------------------------------------------------------------------------------
   this.sourceTemplate_Edit = new Edit( this );
   this.sourceTemplate_Edit.text = regExpToString(guiParameters.sourceFileNameRegExp);
   this.sourceTemplate_Edit.toolTip = SOURCE_FILENAME_REGEXP_TOOLTIP;
   this.sourceTemplate_Edit.enabled = true;
   this.sourceTemplate_Edit.onTextUpdated = function()
      {
         var re = this.text.trim();
         if (re.length === 0) {
            guiParameters.sourceFileNameRegExp = null;
#ifdef DEBUG
            debug("sourceTemplate_Edit: onTextUpdated:- cancel regexp");
#endif
         } else {
            try {
               guiParameters.sourceFileNameRegExp = RegExp(re);
               this.textColor = 0;
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
         // Refresh the generated files
         this.dialog.refreshTargetFiles();
      }


   // Group template --------------------------------------------------------------------------------------
   this.groupTemplate_Edit = new Edit( this );
   this.groupTemplate_Edit.text = guiParameters.groupByTemplate;
   this.groupTemplate_Edit.toolTip = GROUP_TEMPLATE_TOOLTIP;
   this.groupTemplate_Edit.enabled = true;
   this.groupTemplate_Edit.onTextUpdated = function()
   {
      guiParameters.groupByTemplate = this.text;
      this.dialog.refreshTargetFiles();
   }


   // Sizers for Rules section

   this.targetFileTemplate_Edit_sizer = new HorizontalSizer;
   this.targetFileTemplate_Edit_sizer.margin = 4;
   this.targetFileTemplate_Edit_sizer.spacing = 2;
   var label = new Label();
   label.minWidth			= 100;
   label.text		= "Target file template: ";
   label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

   this.targetFileTemplate_Edit_sizer.add( label );
   this.targetFileTemplate_Edit_sizer.add( this.targetFileTemplate_Edit );


   this.sourceTemplate_Edit_sizer = new HorizontalSizer;
   this.sourceTemplate_Edit_sizer.margin = 4;
   this.sourceTemplate_Edit_sizer.spacing = 2;
   var label = new Label();
   label.minWidth			= 100;
   label.text		= "File name RegExp: ";
   label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

   this.sourceTemplate_Edit_sizer.add( label );
   this.sourceTemplate_Edit_sizer.add( this.sourceTemplate_Edit );


   this.groupTemplate_Edit_sizer = new HorizontalSizer;
   this.groupTemplate_Edit_sizer.margin = 4;
   this.groupTemplate_Edit_sizer.spacing = 2;
   var label = new Label();
   label.minWidth			= 100;
   label.text		= "Group template: ";
   label.textAlignment	= TextAlign_Right | TextAlign_VertCenter;

   this.groupTemplate_Edit_sizer.add( label );
   this.groupTemplate_Edit_sizer.add( this.groupTemplate_Edit );



   this.rules_GroupBox = new GroupBox( this );

   this.rules_GroupBox.sizer = new VerticalSizer;
   this.rules_GroupBox.sizer.margin = 6;
   this.rules_GroupBox.sizer.spacing = 4;

   this.rules_GroupBox.sizer.add( this.targetFileTemplate_Edit_sizer, 100);
   this.rules_GroupBox.sizer.add( this.sourceTemplate_Edit_sizer );
   this.rules_GroupBox.sizer.add( this.groupTemplate_Edit_sizer );

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
   this.bar3.setSection( this.outputDir_GroupBox );


   //----------------------------------------------------------------------------------
   // Operation list and action section
   //----------------------------------------------------------------------------------


   // Result operations --------------------------------------------------------------------------------------
   this.transform_TextBox = new TextBox( this );
   this.transform_TextBox.frameStyle = FrameStyle_Box;
   this.transform_TextBox.text = '';
   this.transform_TextBox.toolTip = "Transformations that will be executed";
   this.transform_TextBox.enabled = true;
   this.transform_TextBox.readOnly = true;

   this.bar4 = new SectionBar( this );
   this.bar4.setTitle( "Resulting operations" );
   this.bar4.setSection( this.transform_TextBox );



   // -- Action buttons --------------------------------------------------------------------------------------

   this.check_Button = new PushButton( this );
   this.check_Button.text = "Check validity";
   this.check_Button.toolTip = "Check that the target files are valid\nthis is automatically done before any other operation";
   this.check_Button.enabled = true;
   this.check_Button.onClick = function()
      {
         var listOfFiles = this.parent.makeListOfCheckedFiles();
         var errors = this.parent.engine.checkValidTargets(listOfFiles);
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
   this.refresh_Button.default = true;
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


   // Export FITS values button
   this.txt_Button = new PushButton( this );
   this.txt_Button.text = "Export FITS.txt";
   this.txt_Button.toolTip = "For Checked files write FitKeywords value to file FITS.txt in output directory";
   this.txt_Button.enabled = false;
   this.txt_Button.onClick = function() {
      this.parent.engine.exportFITSKeyWords();
   }

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
   this.buttonSizer.add( this.txt_Button);
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
   this.sizer.add(this.transform_TextBox,50);
   this.sizer.add( this.buttonSizer );

   //this.move(50,100); // move dialog to up-left corner





   //----------------------------------------------------------------------------------
   // Support methods
   //----------------------------------------------------------------------------------

   // -- Hide columns of unchecked keywords (called to apply changes)
   this.hideKey = function () {
      for (var i = 0; i<this.engine.keyEnabled.length;i++) {
         var c = i + 1;
         this.filesTreeBox.showColumn( c, this.engine.keyEnabled[i]);
      }
   }

   // Rebuild the TreeBox content
   this.rebuildFilesTreeBox = function () {
      var i, keys, node, name, iKeyOfFile, k;

#ifdef DEBUG
         debug("rebuildFilesTreeBox: rebuilding filesTreeBox - " + this.engine.inputFiles.length + " input files");
#endif

      this.filesTreeBox.clear();
      this.filesTreeBox.numberOfColumns = 0;

     // TODO move key building code in engine

      this.engine.keyTable = []; // clear
      this.engine.keyEnabled = []; // clear

      // Accumulate all unique FITS keys in keyTable
      for (var i = 0; i < this.engine.inputFiles.length; ++i) {
         var keys = this.engine.inputKeys[i]; // keywords of one file

         // Create TreeBoxNode for file
         var node = new TreeBoxNode( this.filesTreeBox );
         //write name of the file to first column
         node.setText( 0, this.engine.inputFiles[i] );
         node.checked = true;

#ifdef DEBUG
         debug("rebuildFilesTreeBox: adding " + keys.length + " FITS keys to row " + i);
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
               this.filesTreeBox.setHeaderText(this.engine.keyTable.length, name);//set name of new column
               //console.writeln("*** " + this.filesTreeBox.numberOfColumns + " " + name);
               this.engine.keyEnabled.push (this.engine.defaultKey.indexOf(name)> -1);//compare with default enabled keywords
               k = this.engine.keyTable.length-1;

               //this.filesTreeBox.showColumn( this.filesTreeBox.numberOfColumns, this.keyEnabled[k]);
            }
            // TODO Supports other formatting (dates ?) or show raw text
            if (keys[iKeyOfFile].isNumeric) {
               node.setText( k+1, Number(keys[iKeyOfFile].value).toFixed(3) );
            } else {
               node.setText( k+1, keys[iKeyOfFile].value.trim() );
            }
         }
      }
      this.hideKey(); //hide the columns of unchecked FITS keywords
   }


   //enable/disable buttons
   this.updateButtonState = function()
   {
      var enabled = this.dialog.engine.canDoOperation();
      this.dialog.move_Button.enabled = enabled;
      this.dialog.copy_Button.enabled = enabled;
      this.dialog.txt_Button.enabled = enabled;
   }

   //--  Add a list of files to the TreeBox (remove duplicates)
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
      this.QTY.text =countText;
      this.bar1.setCollapsedTitle("Input - " + countText);
   }

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
      for ( var iTreeBox = this.dialog.filesTreeBox.numberOfChildren; --iTreeBox >= 0; ) {

         var nameInTreeBox = this.dialog.filesTreeBox.child(iTreeBox).text(0);
         if (!File.exists(nameInTreeBox)) {

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

   // TreeBox to display list of FITS keywords
   this.keyword_TreeBox = new TreeBox( this );
   this.keyword_TreeBox.toolTip = "Check mark to include in report\nname in red of keyword not in current file";
   this.keyword_TreeBox.rootDecoration = false;
   this.keyword_TreeBox.numberOfColumns = 3;
   this.keyword_TreeBox.setHeaderText(0, "name");
   this.keyword_TreeBox.setHeaderText(1, "value");
   this.keyword_TreeBox.setHeaderText(2, "comment");
   this.keyword_TreeBox.setColumnWidth(0,150);
   this.keyword_TreeBox.setColumnWidth(1,200);
   this.keyword_TreeBox.setColumnWidth(2,600);

   // ComboBox to select the file to display values
   this.file_ComboBox = new ComboBox( this );

   // A file was selected (also called to initialize)
   this.file_ComboBox.onItemSelected = function( index ) {
      // TODO Refactor action code to be in keyword_TreeBox
      // Assume that index in combox is same as index in inputfiles
#ifdef DEBUG
      debug("FITSKeysDialog: file_ComboBox: onItemSelected - " + index + " key table length = " + engine.keyTable.length);
#endif

      var keyword_TreeBox = this.parent.keyword_TreeBox;

      // Update the values of the synthethic keywords from a predefined list and engine values
      var synthRootNode = keyword_TreeBox.child(0);

      for (var i =0; i<shownSyntheticVariables.length; i++) {
         var keyName = shownSyntheticVariables[i];
         var variables = engine.inputVariables[index];
         var variable = variables[keyName];
         if (variable !== null) {
            synthRootNode.child(i).setTextColor(0,0x00000000);
            synthRootNode.child(i).setText(1,variable);
            synthRootNode.child(i).setText(2,shownSyntheticComments[i]);
         } else {
            synthRootNode.child(i).setTextColor(0,0x00FF0000);
            synthRootNode.child(i).setText(1,'');
            synthRootNode.child(i).setText(2,'');
         }
      }

      // Update FITS key words from engine information
      var fitsRoootNode = keyword_TreeBox.child(1);

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
            fitsRoootNode.child(i).setText(2,keyWord.comment);
         } else {
            fitsRoootNode.child(i).setTextColor(0,0x00FF0000);
            fitsRoootNode.child(i).setText(1,'');
            fitsRoootNode.child(i).setText(2,'');
         }
      }

   }


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

   this.sizer2 = new HorizontalSizer;
   this.sizer2.spacing = 2;
   this.sizer2.addStretch();
   this.sizer2.add( this.cancel_Button);
   this.sizer2.add( this.ok_Button);


   // Assemble FITS keyword Dialog
   this.sizer = new VerticalSizer;
   this.sizer.margin = 4;
   this.sizer.spacing = 4;
   this.sizer.add( this.file_ComboBox );
   this.sizer.add( this.keyword_TreeBox );
   this.sizer.add(this.sizer2);
   this.adjustToContents();

   // ------------------------------------------------------------
   // Recreate the content (key names and list of files) when the dialog is showns
   this.onShow = function()
   {
      var p = new Point( parentDialog.position );
      p.moveBy( 16,16 );
      this.position = p;

      // Rebuild the list of FITS keywords

      this.keyword_TreeBox.clear();

      // Create list of synthetic keywords as a fist subtree
      var synthRoootNode = new TreeBoxNode(this.keyword_TreeBox);
      synthRoootNode.expanded = true;
      synthRoootNode.setText(0,"Synthetic keywords");


      // Fill list of keywords from parent keyTable
      for (var i =0; i<shownSyntheticVariables.length; i++) {
         var node = new TreeBoxNode(synthRoootNode);
         node.setText( 0, shownSyntheticVariables[i] );
         node.checked = true;
      }

      // Create list of FITS keywords as a second subtree
      var fitsRoootNode = new TreeBoxNode(this.keyword_TreeBox);
      fitsRoootNode.expanded = true;
      fitsRoootNode.setText(0,"FITS keywords");


      // Fill list of keywords from parent keyTable
      for (var i =0; i<engine.keyTable.length; i++) {
         var node = new TreeBoxNode(fitsRoootNode);
         node.setText( 0, engine.keyTable[i] );
         node.checked = engine.keyEnabled[i];
      }


      // Update the DropDown box - Fill list of files from parent list of files
      this.file_ComboBox.clear();
      for (i = 0; i< engine.inputFiles.length; i++) {
         this.file_ComboBox.addItem(engine.inputFiles[i]);
      }
      this.file_ComboBox.onItemSelected(0);


      this.setMinSize(700,600);
   }

}
FITSKeysDialog.prototype = new Dialog;


