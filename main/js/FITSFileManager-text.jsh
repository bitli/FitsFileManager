// FITSFileManager-text.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


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
</ul><p>The synthetic variables are described in the section 'target template' below. They are built from the FITS keywords, \
the number of the file being processed or are result of a regular expression applied to the file name. \
The source file regular expression can be used, for example, to extract the part of the file name \
before the first dash and use it as a prefix for all files. \
<p/>The files are processed in the order they appear in the table (variable '&amp;rank;'). \
In addition a 'group' string can be generated using the same template rules and a '&amp;count;' \
variable is increased for each different group (for example each target directory). \
The values are cleaned up of special characters, so that they form legal file names. \
";


var TARGET_TEMPLATE_TOOLTIP_A = "\
Define how the target file path will be generated. The text of this field is used \
as the output path, except that the variables are replaced by their value.<br/>\
";

// Part used only in tooltip
var TARGET_TEMPLATE_TOOLTIP_B = "\
Variables (like  &amp;name; or &amp;name:present?absent;) are replaced by values defined from the file \
information and FITS keywords. The details on variables, especially the use of 'present' and 'absent' \
is defined in the help available by the icon at bottom right.<br/>\
";


var TARGET_TEMPLATE_TOOLTIP_C = "\
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
</dl>Leave blank or use a fixed name have a single global counter.<br/>\
Example: '&targetDir;' counts images in each target directory. \
'&filter;' counts the images separetely for each filter (independently of the target directory).<br/> \
You can enter the template or select one of the predefined one and optionaly modify it.\
";

var HELP_CONFIGURATION = "\
The configuration of mapping rules allows you to select one of the preconfigured set of rules. \
Currently the loadable set of rules only includes the mapping of FITS keywords use to generate the synthetic keywords. \
The mapping section also show the rules used to map the values of the IMAGETYP and FILTER keywords to their \
corresponding synthetic variables &amp;type; and &amp;filter;.<br/>\
The values of the keyword are tested with each regular expression (left column) in turn, \
at the first match, the corresponding value (right column) is returned as the value of the corresponding \
synthetic variable. The variables &amp;0;, &amp;1; ... may be used to insert the matching groups of the regular expression, \
after cleaning of special characters.<br/>\
Currently the list of mapping can only be modified in the initialization code.\
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

  // Combine help for global help
  HELP_TEXT: (
      "<html>" +
      "<h1><font color=\"#06F\">FITSFileManager</font></h1>" + BASE_HELP_TEXT +
      "<h3><font color=\"#06F\">Variables</font></h3/>" + VARIABLE_HELP_TEXT +
      "<h3><font color=\"#06F\">Target template</font></h3/>" + TARGET_TEMPLATE_TOOLTIP_A + TARGET_TEMPLATE_TOOLTIP_C +\
      "</dl>Example of template:\<br/><tt>&nbsp;&nbsp;&nbsp;&amp;1;_&amp;binning;_&amp;temp;C_&amp;type;_&amp;exposure;s_&amp;filter;_&amp;count;&amp;extension;</tt>"+
      "<h3><font color=\"#06F\">Source filename reg exp</font></h3>" + SOURCE_FILENAME_REGEXP_TOOLTIP +
      "Example of regular expression:<br/><tt>&nbsp;&nbsp;&nbsp;([^-_.]+)(?:[._-]|$)</tt><p>" +
      "<h3><font color=\"#06F\">Group template</font></h3>" +  GROUP_TEMPLATE_TOOLTIP +
      "Example of group definition:<br/><tt>&nbsp;&nbsp;&nbsp;&amp;targetdir;</tt><p> " +
      "<h3><font color=\"#06F\">Configuration of mappings</font></h3>" +  HELP_CONFIGURATION +
      "<h3><font color=\"#06F\">Operations</font></h3>" + HELP_OPERATIONS +
      "</html>"),

   TARGET_FILE_TEMPLATE_TOOLTIP: (
      TARGET_TEMPLATE_TOOLTIP_A+TARGET_TEMPLATE_TOOLTIP_B+TARGET_TEMPLATE_TOOLTIP_C),

   SOURCE_FILENAME_REGEXP_TOOLTIP:
      SOURCE_FILENAME_REGEXP_TOOLTIP,

   GROUP_TEMPLATE_TOOLTIP :
      GROUP_TEMPLATE_TOOLTIP,

   },

// T: raw text (none)

   }; // return
}) ();

