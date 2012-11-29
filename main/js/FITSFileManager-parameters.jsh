// FITSFileManager-parameters.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js

// -----------------------------------------------------------------------------------------------
// This script support the parameters loaded/saved by the PI 'Settings' mechanism
// and the configuration information currently only modifiable by editing this file.
// -----------------------------------------------------------------------------------------------



// --- Configuration - Change the definitions as needed.
//                     Some definitions need to be consistent with others or with the code,
//                     but this should be fairly obvious.


// TODO Should not be a global or define, but scoped variables

// Default values of some parameters, used only on the first usage of the script (when the settings are not yet defined)

// Select the first sequence without -_. or the whole name in &1; (second group is non capturing)
#define FFM_DEFAULT_SOURCE_FILENAME_REGEXP /([^-_.]+)(?:[._-]|$)/
#define FFM_DEFAULT_TARGET_FILENAME_TEMPLATE "&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;"
// #define FFM_DEFAULT_TARGET_FILENAME_TEMPLATE "&filename;_AS_&1;_bin_&binning;_filter_&filter;_temp_&temp;_type_&type;_exp_&exposure;s_count_&count;&extension;";
#define FFM_DEFAULT_GROUP_TEMPLATE "&targetDir;"


// --- List of all synthethic variables and their comments (2 parallel arrays)
//     All synthethic variables are currently added to the columns of the file TreeBox
var syntheticVariableNames = ['type','filter','exposure','temp','binning','night'];
var syntheticVariableComments = ['Type of image (flat, bias, ...)',
   'Filter (clear, red, ...)',
   'Exposure in seconds',
   'Temperature in C',
   'Binning as 1x1, 2x2, ...',
   'night (experimental)'];


// --- Mapping of 'logical' FITS keywords (referenced in the code) to actual FITS keywords

var kwMappingDefault = {
      "SET-TEMP": "SET-TEMP",
      "EXPOSURE": "EXPOSURE",
      "IMAGETYP": "IMAGETYP",
      "FILTER"  : "FILTER",
      "XBINNING": "XBINNING",
      "YBINNING": "YBINNING",
      // Non standard
      "LONG-OBS": "LONG-OBS",
      // We should really use DATE-OBS and convert
      "JD"       : "JD",
};

var kwMappingCaha = {
      "SET-TEMP": "CCDTEMP",
      "EXPOSURE": "EXPTIME",
      "IMAGETYP": "IMAGETYP",
      "FILTER"  : "INSFLNAM",
      "XBINNING": "CDELT1",
      "YBINNING": "CDELT2",
      // Non standard
      "LONG-OBS": "CAHA TEL GEOLON",
      // We should really used DATE-OBS (if available) and convert
      "JD"      : "JUL-DATE",
};

// TODO - The mapping could manage other parameters
// The tables below must be coherent with each other, more mapping can be added
#define KW_MAPPING_DEFAULT_INDEX 0
var kwMappingTables = {
   "DEFAULT" : kwMappingDefault,
   "CAHA"    : kwMappingCaha,
};
var kwMappingList = ["DEFAULT", "CAHA"];
var kwMappingCommentsList = ["Common and Star Arizona mappings", "CAHA mapping"];


// Default list of FITS keywords to show as columns in the input file TreeBox
// Can be updated by the user by the FITS table UI interface
var kwDefaultShownKeywords = [
   "IMAGETYP","FILTER","OBJECT"
   //"SET-TEMP","EXPOSURE","IMAGETYP","FILTER","XBINNING","YBINNING","OBJECT"
];


// Name of key in settings
#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"


// ------------------------------------------------------------------------------------------------------------------------
// User Interface Parameters
// ------------------------------------------------------------------------------------------------------------------------

// The object FFM_GUIParameters keeps track of the parameters that are saved between executions
// (or that should be saved)
function FFM_GUIParameters() {

   // Initialize all parameters to default values
   this.reset = function () {

      // Default temp
      this.targetFileNameTemplate = FFM_DEFAULT_TARGET_FILENAME_TEMPLATE;

      // Default regular expression to parse file name
      this.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;

      this.orderBy = "&rank;" // UNUSED


      // Map to remap keywords used to create synthethic keywords to other values
      this.kwMappingCurrentIndex = KW_MAPPING_DEFAULT_INDEX;

      this.remappedFITSkeywords = kwMappingTables[kwMappingList[this.kwMappingCurrentIndex]];


      // This is the list of keys shown by default (in addition to the synthethic keywords)
      // A possibly empty column is created for all these keywords, so that they are always present
      // in the same order and that the use can see that the column has no value.
      // By default only show the keywords that are significantly transformed by the conversion to synthethic keywords
      this.defaultListOfShownFITSKeywords = [];
      for (var i=0; i<kwDefaultShownKeywords.length; i++) {
         this.defaultListOfShownFITSKeywords[i] = this.remappedFITSkeywords[kwDefaultShownKeywords[i]];
      }



      // Create templates (use defaults if not yet specified), precompile them
      this.groupByTemplate = FFM_DEFAULT_GROUP_TEMPLATE;
      var templateErrors = [];
      this.targetFileNameCompiledTemplate = ffM_template.analyzeTemplate(templateErrors, FFM_DEFAULT_TARGET_FILENAME_TEMPLATE);
      this.groupByCompiledTemplate = ffM_template.analyzeTemplate(templateErrors, FFM_DEFAULT_GROUP_TEMPLATE);
      if (templateErrors.length>0) {
         throw "PROGRAMMING ERROR - default built in templates invalid";
      }

      // Prepare list of regexp, groupBy template and target file template for use by the user interface.
      // The first element of the list is the last one selected by the user, the others are predefiend elements
      // (currently hardcoded here - could eventually be made editable)
      // There are two parallel arrays, one for the values, and one for a comment displayed in the selection box
      this.targetFileItemListText = [
            this.targetFileNameCompiledTemplate.templateString, // Must be adapted after parameter loading
            FFM_DEFAULT_TARGET_FILENAME_TEMPLATE,
            "&type;/&1;_&binning;_&temp;C_&exposure;s_&filter;_&count;&extension;",
            "&OBJECT;_&filter;_&count;&extension;",
            "&1;_&type?light;_&filter?clear;_&count;&extension;",
            ""
      ];
      this.targetFileItemListComment = [
            "last",
            "detailled, using part of file name",
            "directory by type",
            "Object and filter",
            "type and filter with defaults",
            "(clear)"
      ];

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
            "&type?;&filter?;",
            ""
      ];
      this.groupItemListComment = [
            "last",
            "count by directory",
            "count by filter",
            "count by type and filter if present",
            "none (count globally)"
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
   save( "orderBy",                    DataType_String, this.orderBy );
   save( "groupByTemplate",            DataType_String, this.groupByCompiledTemplate.templateString );

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

