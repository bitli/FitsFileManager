// FITSFileManager-parameters.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js

// -----------------------------------------------------------------------------------------------
// This script support the parameters loaded/saved by the PI 'Settings' mechanism
// and the configuration information currently only modifiable by editing this file.
// -----------------------------------------------------------------------------------------------

#include <pjsr/DataType.jsh>


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
// TODO Should be in the module where they are created
var syntheticVariableNames = ['type','filter','exposure','temp','binning','night'];
var syntheticVariableComments = ['Type of image (flat, bias, ...)',
   'Filter (clear, red, ...)',
   'Exposure in seconds',
   'Temperature in C',
   'Binning as 1x1, 2x2, ...',
   'night (experimental)'];





// Name of key in settings
#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"


// ========================================================================================================================
// Named configuration
// ========================================================================================================================
var ffM_Configuration = (function() {

   // A named configuration is a named set of parameters that can be selected as a whole,
   // typically the configuration suitable for an observatory, an instrument and a user.
   // NOT ALL ELEMENTS OF A CONFIGURATION CAN YET BE EDITED INTERACTIVELY.
   // The configuration is a map with the following entries:
   // name - its name (used to save in settings)
   // description - A one line description for the user
   // kwMappingTable - The map of logical name to FITS key names
   // filterConversion - The list of filter conversion operations
   // typeConversion - The list of filter conversion operations

   // Preconfigured configuration properties

   // --- Mapping of 'logical' FITS keywords (referenced in the code) to actual FITS keywords
   //     The logical keyword is by convention a 'PascalCased' name of the variable using
   //     the keyword and some differentiating suffix if multiple keywords are required.
   //     This does not really matter, just more mnemotecnic than random text
   // TODO Support optional and multiple keywords (as EXPOSURE and EXPTIME) and default value
   var kwMappingDefault = {
       // Commonly used
      "BinningX": "XBINNING",
      "BinningY": "YBINNING",
      "Exposure": "EXPOSURE", // Could also be EXPTIME
      "Filter"  : "FILTER",
      "Temp"    : "SET-TEMP", // Also CCDTEMP and CCD-TEMP
      "Type"    : "IMAGETYP",
      // For experimental keyword
      "NightLongObs" : "LONG-OBS",
      // We should really use DATE-OBS and convert
      "NightJD"      : "JD",
   };

   var kwMappingCaha = {
      // Commonly used
      "BinningX": "CDELT1",
      "BinningY": "CDELT2",
      "Exposure": "EXPTIME",
      "Filter"  : "INSFLNAM",
      "Temp"    : "CCDTEMP",
      "Type"    : "IMAGETYP",
      // For experimental keyword
      "NightLongObs": "CAHA TEL GEOLON",
      // We should really used DATE-OBS (if available) and convert
      "NightJD"     : "JUL-DATE",
   };

   // -- Rules of conversion
   // Array of 2 element arrays,
   // regexp to match source value, followed by replacement string
   // Back reference using the &<number>; syntac is allowed
   // CURRENTLY USED ONLY AT INITIALIZATION TIME OF ENGINE

   var filterConversions_DEFAULT = [
      [/green/i,     'green'],
      [/red/i,       'red'],
      [/blue/i,      'blue'],
      [/clear/i,     'clear'],
      [/luminance/i, 'luminance'],
      [/.*/i,        '&0;'],
   ];
   var filterConversions_CAHA = [
      [/.*/i,        '&0;'],
   ];


   var typeConversions_DEFAULT = [
      [/flat/i,     'flat'],
      [/bias/i,     'bias'],
      [/offset/i,   'bias'],
      [/dark/i,     'dark'],
      [/light/i,    'light'],
      [/science/i,  'light'],
      [/.*/i,       '&0;'],
   ];

   var typeConversions_CAHA = [
      [/flat/i,     'flat'],
      [/bias/i,     'bias'],
      [/offset/i,   'bias'],
      [/dark/i,     'dark'],
      [/light/i,    'light'],
      [/science/i,  'light'],
      [/.*/i,       '&0;'],
   ];

   // List of FITS keywords shown by default (even if not present in any image) in the input files TreeBox
   var defaultShownKeywords_DEFAULT = [
      "IMAGETYP","FILTER","OBJECT"
      //"SET-TEMP","EXPOSURE","IMAGETYP","FILTER","XBINNING","YBINNING","OBJECT"
   ];
   var defaultShownKeywords_CAHA = [
      "IMAGETYP","INSFLNAM","OBJECT"
      //"SET-TEMP","EXPOSURE","IMAGETYP","FILTER","XBINNING","YBINNING","OBJECT"
   ];

   // -- Define the named configuration content
   var configuration_DEFAULT = {
     name: "DEFAULT",
     description: "Common and Star Arizona mappings",
     kwMappingTable: kwMappingDefault,
     filterConversions: filterConversions_DEFAULT,
     typeConversions: typeConversions_DEFAULT,
     defaultShownKeywords: defaultShownKeywords_DEFAULT,
   };
   var configuration_CAHA = {
     name: "CAHA",
     description: "CAHA mapping",
     kwMappingTable: kwMappingCaha,
     filterConversions: filterConversions_CAHA,
     typeConversions: typeConversions_CAHA,
     defaultShownKeywords: defaultShownKeywords_CAHA,
   };


   // Private tables (in principles)
   var configurationTable = {};
   var configurationList = [];

   function addConfiguration(configuration) {
      configurationTable[configuration.name] = configuration;
      configurationList.push(configuration.name);
   }

   function getConfigurationByName(name) {
      return configurationTable[name];
   }
   function getConfigurationByIndex(index) {
      var name = configurationList[index];
      return configurationTable[name];
   }

   // Add in the order they should be in the select list
   addConfiguration(configuration_DEFAULT);
   addConfiguration(configuration_CAHA);

   // Return a 'module' with public properties and methods
   return {
       //configurationTable: configurationTable,  // Consider readonly
       configurationList: configurationList, // Consider readonly
       getConfigurationByName: getConfigurationByName,
       getConfigurationByIndex: getConfigurationByIndex,
   };
}) ();



// ========================================================================================================================
// User Interface Parameters
// ========================================================================================================================

// The object FFM_GUIParameters keeps track of the parameters that are saved between executions
// (or that should be saved)
function FFM_GUIParameters() {
   // Called at end of constructor
   this.initializeParametersToDefaults = function () {

      // Default temp
      this.targetFileNameTemplate = FFM_DEFAULT_TARGET_FILENAME_TEMPLATE;

      // Default regular expression to parse file name
      this.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;

      this.orderBy = "&rank;" // UNUSED


      // Initialiy the first configuration is the defaukt
      this.currentConfigurationIndex = 0;



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


   // For debugging and logging
   this.toString = function() {
      var s = "GUIParameters:\n";
      s += "  targetFileNameTemplate:         " + replaceAmps(this.targetFileNameCompiledTemplate.templateString) + "\n";
      s += "  sourceFileNameRegExp:           " + replaceAmps(regExpToString(this.sourceFileNameRegExp)) + "\n";
      s += "  orderBy:                        " + replaceAmps(this.orderBy) + "\n";
      s += "  groupByTemplate:                " + replaceAmps(this.groupByCompiledTemplate.templateString) + "\n";
      s += "  currentConfigurationIndex:          " + this.currentConfigurationIndex + "\n";
      return s;
   }

   this.initializeParametersToDefaults();


}

FFM_GUIParameters.prototype.loadSettings = function() {


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

   var o, t, parameterVersion, templateErrors, ki;
   if ( (parameterVersion = load( "version",    DataType_Double )) !== null ) {
      if (parameterVersion > VERSION) {
         Console.show();
         Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' have version ", parameterVersion, " later than script version ", VERSION, ", settings ignored");
         Console.flush();
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

         // After 0.7
         if (parameterVersion>0.7) {
            if ( (o = load( "mappingName",          DataType_String )) !== null ) {
               ki = ffM_Configuration.configurationList.indexOf(o);
               if(ki>=0) {
                  this.currentConfigurationIndex = ki;
               } else {
                  Console.show();
                  Console.writeln("Mapping rules '" + o + "' unknown, using '" + ffM_Configuration.configurationList[this.currentConfigurationIndex ] + "'");
                  Console.flush();
               }
            }
         }
      }
   } else {
      Console.show();
      Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' do not have a 'version' key, settings ignored");
      Console.flush();
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
   save( "mappingName",                DataType_String, ffM_Configuration.configurationList[this.currentConfigurationIndex ]);

}

FFM_GUIParameters.prototype.getCurrentConfiguration =
   function getCurrentConfiguration() {
      return ffM_Configuration.getConfigurationByIndex(this.currentConfigurationIndex);
   };


FFM_GUIParameters.prototype.targetTemplateSelection =  [
   FFM_DEFAULT_TARGET_FILENAME_TEMPLATE
];
FFM_GUIParameters.prototype.groupTemplateSelection = [
   FFM_DEFAULT_GROUP_TEMPLATE
];
FFM_GUIParameters.prototype.regexpSelection = [
   FFM_DEFAULT_SOURCE_FILENAME_REGEXP.toString()
];

