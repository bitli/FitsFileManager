// FITSFileManager-text.js

// This file is part of FITSFileManager, see copyright in FITSFileManager.js


// This module contains the fixed text for help, tooltips, GUI labels and buttons,
// it does NOT contain the text of templates or used as part of the
// algorithms or configuration/settings (as FITS key names, last template used, ...)

// Text formatted as html is in the 'H' variable,
// Plain text (that could contain & and other xml markup entities) are in the variable 'T'.

// For the format of help see http://pixinsight.com/forum/index.php?topic=4598.msg31979#msg31979
//  It should support html 4 and some css



var Text = (function() {

// --- Local text, used to assemble other texts  ------------------------------
// Some texts are used as tooltip and reused to form the help text

var BASE_HELP_TEXT = "\
<p>FITSFileManager allows you to copy or move FITS image files to new locations, generating the \
new location path from a template with the replacement of variables with values extracted from FITS keys \
and other information.\
<p/>You select the files to move/copy (files can be individually checked or un-checked) \
and select a predefined template or enter a new template to generate the target path using variables to substitute values \
based on the source image file name, FITS keywords or synthethic variables. \
Various other parameters can be adapted to fine tune the path generation. \
The list of transformation is updated as you type templates and other parameters. \
The synthetic variables are defined in a 'configuration' that you may edit. \
";

var VARIABLE_HELP_TEXT = "\
<p/>The variables have the general form '&amp;name:present?missing;', although most of the time \
they have simply the form '&amp;name;'. The 'name' identifies the variable, it may be a FITS key or a synthetic variable name. \
<ul><li>The optional 'present' part is the string that will be used as the replacement value if the variable \
has a value, Usually ':present' is not specified and the value of the variable is used as the replacement string. You can also \
have an empty 'present' value (as &amp;TELESCOP:;), in which case the variable is checked for presence (an error is \
generated if the variable is missing) but its value does not contribute to the target path.</li> \
<li>The optional '?missing' part is used if the variable is not present in the file (for example '&OBJECT?unknown;'). \
You can also have an empty 'missing' value (like '&amp;binning?;') in which case there is no error if the variable  \
has no value. </li>\
</ul><p>The synthetic variables are described in the section 'target template' below. They are built \
from the FITS keywords (following rules specified in a 'configuration'). \
from the number of the file being processed or from the a regular expression applied to the file name. \
The source file regular expression can be used, for example, to extract the part of the file name \
before the first dash and use it as a prefix for all files. \
<p/>The files are processed in the order they appear in the table, the variable '&amp;rank;' provides \
the image number to help generating a unique file file. \
In addition a 'group' string can be generated using from the 'group' template. A '&amp;count;' \
variable is increased for each different group (for example each target directory if the group template \
contained the target directory). \
The values are cleaned up of special characters, so that they form legal file names. \
";


var TARGET_TEMPLATE_TOOLTIP_A = "\
Define how the target file path will be generated. The text of this field is used \
as the output path, except that the variables are replaced by their value according to \
the current 'configuration'.<br/>\
";

// Part used only in tooltip
var TARGET_TEMPLATE_TOOLTIP_B = "\
Variables (like  &amp;name; or &amp;name:present?absent;) are replaced by values defined from the file \
information and FITS keywords. The details on variables, especially the use of 'present' and 'absent' \
is defined in the help available by the icon at bottom right.<br/>\
";


var TARGET_TEMPLATE_TOOLTIP_C = "\
The variables include the FITS keywords and the synthetic variables defined in the configuration. \
A typical configuration contains the following keywords (your actual configuration may be different):<\br/>\
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
<p>The following keywords are dynamic (their values depends on the file order) and are always present:\
<dl>\
   <dt>&amp;count;</dt><dd>The number of the file being moved/copied int the current group.<\dd>\
   <dt>&amp;rank;</dt><dd>The number of the file in the order of the input file list.<\dd>\
</dl>You can enter the template or select one of the predefined one and optionaly modify it.<br/>\
";

var SOURCE_FILENAME_REGEXP_TOOLTIP = "\
Defines a regular expression (without the surrounding slashes) that will be applied to all file names, \
without the extension. The 'match' array resulting from the regular expression matching can be used \
in the target file name template as numbered variables. '&0;' represent the whole matched expression, '&1' the first group, and so on \
In case of error the field turns red. \
You can enter the regexp or select one of the predefined one and optionally modify it. \
<br/>See https:\/\/developer.mozilla.org\/en-US\/docs\/JavaScript\/Guide\/Regular_Expressions for more informations on regular expresssions. \
<p>\
";



var GROUP_TEMPLATE_TOOLTIP = "\
Defines the template to generate a group name used by the synthetic variable '&count;'. \
Each FITS image generate a group name exactly as it generates a path, but using the group template. \
A count of image will be kept for each different group name and can be used as the variable '&count;' in the \
target path template. \
All variables can be used in the group template, except &count;. In addition you can use the following variable:\
<dl><dt>&targetDir;</dt><dd>The directory part of the target file name.</dd>\
</dl>Leave blank or use a fixed name to have a single global counter.\
<p>Example of group templates:<dl>\
<dt>'&targetDir;'</dt><dd>counts images in each target directory.</dd> \
<dt>'&filter;'</dt><dd>counts the images separetely for each filter (independently of the target directory).<dd/> \
</dl>You can enter the template or select one of the predefined one and optionaly modify it.\
</p>";

var HELP_CONFIGURATION = "\
The configuration defines how the synthetic variables are created, for example the FITS key to \
use, the format of the value, specific operations like changing case or renaming filter names. \
FITSFileManager keeps track of multiple configurations, only one at a time may be active. \
Currently the list of configuration is fixed (this will be made more dynamic in the future). \
It is expected that most users could use the default configuration, or may be the default with \
some simple adjustments. It is also possible to have a set of configuration for different \
image type, but currently you must still change the configuration or the template between \
image types.  Some cofiguration options may require more advanced understanding of regular \
expressions and the FITS key words. \
<br/>Click on the Manage configurations... button to change or edit the configuration. \
";

var HELP_OPERATIONS = "<p>The operations Copy/Move copy or move the files directly, without \
adding any FITS keywords.  The operation Load/SaveAs loads each image temporarily in the workspace \
and save it to the new location. An ORIGFILE keyword with the original file name is added if it is not already present. \
<br/>The operation buttons may be disabled if the operation is not possible (for example if the \
output directory is not specified).</p>\
";


// --- Exported texts, refer as Text.H.xxx  -----------------------------------

return {
H: {

   TARGET_FILE_TEMPLATE_TOOLTIP: (
      TARGET_TEMPLATE_TOOLTIP_A+TARGET_TEMPLATE_TOOLTIP_B+TARGET_TEMPLATE_TOOLTIP_C),

   SOURCE_FILENAME_REGEXP_TOOLTIP:
      SOURCE_FILENAME_REGEXP_TOOLTIP,

   GROUP_TEMPLATE_TOOLTIP :
      GROUP_TEMPLATE_TOOLTIP,

   HELP_LABEL: ("<b>" + TITLE + " v" + VERSION + "</b> &mdash; Copy or move FITS image " +
           "files using values derived from FITS keywords and from original file name, using a template " +
           "to create the target directory/file name. See the help for more details."),

   // Tools tips CAN be html

   FILES_TREEBOX_TOOLTIP: ( "List of input files - you can add and remove files with the buttons below.\n" +
                               "Select the files you want to operate on with the check box (all by default),\n"+
                               "The columns shown the file name (always) and selected synthethic and loaded FITS keywords,\n" +
                               "select the columns you want to see with the 'text' icon button below at left.\n"+
                               "The FITS keywords and the variables can be used in a template even if they are not in a visible column.\n" +
                               "You can sort the files by clicking on a column header. Then click the 'refresh' button.\n"+
                               "Beware - sort is in alphabetical order even for numbers"),

   KEY_BUTTON_TOOLTIP: "Show all keywords and variables of the selected file,\nalso select the column to shown in the Input file list",

   DIRADD_BUTTON_TOOLTIP: "Add folder including subfolders",

   CHECK_SELECTED_BUTTON_TOOLTIP: "Check selected images",

   CHECK_UNSELECTED_BUTTON_TOOLTIP: "Uncheck selected images",

   REMOVE_FILES_BUTTON_TOOLTIP:  "Remove selected images from the list",

   REMOVE_ALL_FILES_BUTTON_TOOLTIP: "Remove all images from the list",



   KEYWORDNAMES_GROUPBOX_TOOLTIP: "The left side are the keywords used by default,\nThe right side are the keywords used in the current configuration. ",

   TYPECONVERSION_GROUPBOX_TOOLTIP: ("The value of the IMAGETYP keywords are tested with each regular expression in turn (left column),\n" +
           "at the first match, the corresponding value (right column) is returned as the synthetic variable &type;.\n" +
           "The variables &0;, &1; ... may be used to insert the matching groups of the regular expression.\n"+
           "The replaced values are 'cleaned' of special characters."),

   FILTERCONVERSION_GROUPBOX_TOOLTIP: ("The value of the FILTER keywords are tested with each regular expression in turn (left column),\n" +
           "at the first match, the corresponding value (right column) is returned as the synthetic variable &type;.\n" +
           "The variables &0;, &1; ... may be used to insert the matching groups of the regular expression.\n"+
           "The replaced values are 'cleaned' of special characters."),

   OUTPUTDIR_TOOLTIP: "Select the base output directory.\nAny directory created by your template will be below this directory.",

   OUTPUTDIR_SELECT_TOOLTIP: "Select the base output directory.",

   TRANSFORM_TREEBOX_TOOLTIP: ("List of selected files and how they will be converted.\n" +
                               "If there is any error, the corresponding file will be in red.\n"+
                               "You can correct the error or uncheck the corresponding input file\n"+
                               "The files are in the order of the input (do a Refresh if you sorted the input),\n"+
                               "This matters for the &count; and &rank; keywords."),

   CHECK_BUTTON_TOOLTIP: "Check that the target files are valid\nthis is automatically done before any other operation",

   REFRESH_BUTTON_TOOLTIP:  "Refresh the list of operations\nrequired after a sort on an header (there is on onSort event)",

   MOVE_BUTTON_TOOLTIP:  "Move the checked files to the output directory.\nNo HISTORY or ORIGFILE keyword added",

   COPY_BUTTON_TOOLTIP: "Copy the checked files in the output directory.\nNo HISTORY or ORIGFILE keyword added",

   LOADSAVE_BUTTON_TOOLTIP:  ("Load the checked files and save them in the output directory.\n" +
         "BEWARE: Not supported for files containing multiple HDU (multiple images).\n"+
         "        Data will be interpreted by PI, PEDESTAL may become irrelevant or ignored.\n"+
         "Add ORIGFILE keyword with original file name if not already present.\n" +
         "Add HISTORY keyword with new file name.\n"),

   CONFIGURE_BUTTON_TOOLTIP:  ("Select a new current configuration or modify it.\n"+
                               "A configuration defines the synthetic variables and their format.\n"),
   SELECT_CONFIGURATION_BUTTON_TOOLTIP: ("Select the configuration to use as current configuration"),

   HELP_BUTTON_TOOLTIP:  "Browse Documentation",

   COMPLETION_CONTINUE_BUTTON_TOOLTIP : "Continue working in FITSFileManager, moved files have been removed from input list",
   COMPLETION_KEEP_BUTTON_TOOLTIP : "Keep checked files in input list",
   COMPLETION_REMOVE_BUTTON_TOOLTIP : "Remove checked files from input list",
   COMPLETION_LEAVE_BUTTON_TOOLTIP : "Exit FITS file manager",


   VARIABLE_SELECTION_TOOLTIP: ("Synthetic variables available in this configuration.<br/>" +
      "You may add, remove or move variable using the buttons below the list.<br/>" +
      "Select a variable to view or edit its parameters."
      ),

   VARIABLE_RESOLVER_TOOLTIP: ("Select one of the variable resolution algorithm:<dl>"+
      "<dt>Text</dt><dd>The text of the FITS key, filtered from special characters.</dd>" +
      "<dt>Integer</dt><dd>A FITS keyword value parsed as an integer value</dd>"+
      "<dt>IntegerPair</dt><dd>Two FITS keyword values parsed as two integers value</dd>" +
      "<dt>DateTime</dt><dd>A FITS keyword values parsed as date or date and time</dd>" +
      "<dt>RegExpList</dt><dd>A FITS keyword and a list of regular expression and replacements</dd>" +
      "<dt>Constant</dt><dd>A fixed value (for test or parameterization)</dd>" +
      "<dt>Night</dt><dd>An algorithm to help classify by night (used mostly for sorting)</dd>" +
      "<dt>FileName</dt><dd>The source file name</dd>" +
      "<dt>FileExtension</dt><dd>The source file extension</dd>" +
      "</dl>"
      ),
   },

   // T: raw text
   T: {
      KEYWORDNAMES_GROUPBOX_TITLE: "Keyword remapping ",
      TYPECONVERSION_GROUPBOX_TITLE: "Remapping of IMAGETYP ",
      FILTERCONVERSION_GROUPBOX_TITLE: "Remapping of FILTER ",
      REMAPPING_SECTION_PART_TEXT: "Remapping of keywords and values",
      OUPUT_SECTION_TEXT_PART:  "Output base directory",
      GET_DIRECTORY_DIALOG_CAPTION: "Select Output Directory",
      RESULT_SECTION_PART_TEXT:  "Resulting operations" ,

      CHECK_BUTTON_TEXT: "Check validity",
      REFRESH_BUTTON_TEXT: "Refresh list",
      MOVE_BUTTON_TEXT: "Move files",
      COPY_BUTTON_TEXT: "Copy files",
      LOADSAVE_BUTTON_TEXT:  "Load / SaveAs files",
      CONFIGURE_BUTTON_TEXT:  "Manage configurations...",

      COMPLETION_TITLE: "FITSFileManager operation result",
      COMPLETION_CONTINUE_BUTTON_TEXT : "Continue in FITSFileManager",
      COMPLETION_KEEP_BUTTON_TEXT : "Continue in FITSFileManager\nKeep checked files",
      COMPLETION_REMOVE_BUTTON_TEXT : "Continue in FITSFileManager\nRemove checked files",
      COMPLETION_LEAVE_BUTTON_TEXT : "Leave FITSFileManager",

   }

   }; // return
}) ();

