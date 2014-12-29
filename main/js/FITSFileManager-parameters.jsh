// FITSFileManager-parameters.js

// This file is part of FITSFileManager, see copyright in FITSFileManager.js

// -----------------------------------------------------------------------------------------------
// This script support the parameters loaded/saved by the PI 'Settings' mechanism
// and the configuration information currently only modifiable by editing this file.
// -----------------------------------------------------------------------------------------------

#include <pjsr/DataType.jsh>


// --- Configuration - Change the definitions as needed.
//                     Some definitions need to be consistent with others or with the code,
//                     but this should be fairly obvious.


// Define a version for the PARAMETERS ( a double, independent of the script version)
#define PARAMETERS_VERSION 1.0


// TODO Should not be a global or define, but scoped variables

// Default values of some parameters, used only on the first usage of the script (when the settings are not yet defined)

// Select the first sequence without -_. or the whole name in &1; (second group is non capturing)
#define FFM_DEFAULT_SOURCE_FILENAME_REGEXP /([^-_.]+)(?:[._-]|$)/
#define FFM_DEFAULT_TARGET_FILENAME_TEMPLATE "&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;"
#define FFM_DEFAULT_GROUP_TEMPLATE "&targetDir;"







// Name of key in settings
#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"


// ====================================================================================================================
// Configuration module
// ====================================================================================================================


// ---------------------------------------------------------------------------------------------------------------------
// Definition of ConfigurationSet data
// ---------------------------------------------------------------------------------------------------------------------
// A Configuration is a set of rules that define how to parse a FITS file to
// generate the synthetic variables. 
// The Configurations have a name and are grouped in a ConfigurationSet. Only one Configuration
// is active at a time.
// The Configuration object is a 'pure data' representation of the rules, so they can be serialized easily,
// it contains only strings, numbers, objects (used as map) and arrays. Regexp are represented as strings.
// There are some utility methods to support common functions on a Configuration, but usually the data of 
// a Configuration is manipulated directly by the methods that use it.
// The ConfigurationSet is a singleton read/writen by this module. A copy of the ConfigurationSet is manipulated
// by the Configuration Dialog and replace the singleton only if the Dialog exits with a status OK.
// One configuration is the 'current' configuration. It is a copy if the selected configuration
// and can be complemented by data used for processing (typically the implementation of the resolvers).
// That copy is not modified by the ConfigurationDialog or saved as parameter. If another Configuration
// is selected, a new copy of the selected Configuration will replace the 'current configuration' and all derived
// data will be recalculated. 




// ffM_Configuration module (make local name space)
var ffM_Configuration = (function() {

   var i;

   // Default ConfigurationSet, serve also as example of the structure.
   // The structure is like:s
   // --------------------------------------------------------------------------------------------------
   // configurationSet data (an ordered list of Configurations, the order does not matter for the semantic)
   //    [configuration]

   // configuration data (the variables is an ordered list, they are processed in order)
   //    {name: aString, description: aString, variableList: [variables]}

   // variable:
   //    {name: aString, description: aString, resolver: aName, parameters: {resolverName: {}}}
   // (the content of the 'parameters' object depends on the resolver, it has one key which is the
   //  name of the resolver, allowing to keep data for inactive resolvers)
   // ---------------------------------------------------------------------------------------------------------------------

   var defaultConfigurationSet =
   [
      { name: "Default",
       description: "Common FITS rules",
       variableList:
         [ { name: "type",
             description: "Type of image (flat, bias, ...)",
             show: true,
             resolver: "RegExpList",
             parameters:
               { RegExpList:
                   { key: "IMAGETYP",
                     reChecks:
                       [ { regexp: '/flat/i',
                           replacement: "flat"
                         },
                         { regexp: '/bias/i',
                           replacement: "bias"
                         },
                         { regexp: '/offset/i',
                           replacement: "bias"
                         },
                         { regexp: '/dark/i',
                           replacement: "dark"
                         },
                         { regexp: '/light/i',
                           replacement: "light"
                         },
                         { regexp: '/science/i',
                           replacement: "light"
                         },
                         { regexp: '/.*/',
                           replacement: "&0;"
                         },
                       ]
                   }
               }
           },
           { name: "filter",
             description: "Filter (clear, red, ...)",
             show: true,
             resolver: "RegExpList",
             parameters:
               { RegExpList:
                   { key: "FILTER",
                     reChecks:
                       [ { regexp: '/green/i',
                           replacement: "green"
                         },
                         { regexp: '/blue/i',
                           replacement: "blue"
                         },
                         { regexp: '/red/i',
                           replacement: "red"
                         },
                         { regexp: '/clear/i',
                           replacement: "clear"
                         },
                         { regexp: '/luminance/i',
                           replacement: "luminance"
                         },
                         { regexp: '/.*/',
                           replacement: "&0;"
                         }
                       ]
                       ,
                   }
               }
           },
           { name: "exposure",
             description: "Exposure in seconds",
             show: true,
             resolver: "Integer",
             parameters:
               { Integer:
                   { key: "EXPTIME", // also EXPOSURE
                     abs: true,
                     format: "%4.4d"
                   }
               }
           },
           { name: "temp",
             description: "Temperature in C",
             show: true,
             resolver: "Integer",
             parameters:
               { Integer:
                   { key: "SET-TEMP", // Also CCDTEMP and CCD-TEMP",
                     abs: true,
                     format: "%4.4d"
                   }
               }
           },
           { name: "binning",
             description: "Binning as 1x1, 2x2, ...",
             show: true,
             resolver: "IntegerPair",
             parameters:
               { IntegerPair:
                   { key1: "XBINNING",
                     key2: "YBINNING",
                     format: "%dx%d"
                   }
               }
           },
          //      "NightLongObs" : "LONG-OBS",
         // We should really use DATE-OBS and convert
        // "NightJD"      : "JD",

           { name: "night",
             description: "night (experimental)",
             show: true,
             resolver: "Night",
             parameters:
               { Night:
                   { keyLongObs: "LONG-OBS",
                     keyJD: "JD"
                   }
               }
           },
           { name: "filename",
             description: "Input file name",
             show: false,
             resolver: "FileName",
             parameters:
               { FileName:
                   {
                   }
               }
           },
           { name: "extension",
             description: "Input file extension",
             show: false,
             resolver: "FileExtension",
             parameters:
               { FileExtension:
                   {
                   }
               }
           }
         ],
        builtins: {
           rank: {format: "%4.4d"},
           count: {format: "%4.4d"},
        }
     }

   ];

   // Duplicate the default configuration,
   // patch in the differences
   var defaultConf = defaultConfigurationSet[0];

   // Default configuration for images originated in the CAHA (http://www.caha.es/) observatory
   var userConf = deepCopyData(defaultConf);
   userConf.name = "User CAHA";
   userConf.description = "A default configuration based on images from CAHA via PixInsight";
   // Just the differences from the base configuration - which is about everything
   userConf.variableList =
         [ { name: "type",
             description: "Type of image (flat, bias, ...)",
             show: true,
             resolver: "RegExpList",
             parameters:
               { RegExpList:
                   { key: "IMAGETYP",
                     reChecks:
                       [ { regexp: '/flat/i',
                           replacement: "flat"
                         },
                         { regexp: '/bias/i',
                           replacement: "bias"
                         },
                         { regexp: '/offset/i',
                           replacement: "bias"
                         },
                         { regexp: '/dark/i',
                           replacement: "dark"
                         },
                         { regexp: '/light/i',
                           replacement: "light"
                         },
                         { regexp: '/science/i',
                           replacement: "light"
                         },
                         { regexp: '/.*/',
                           replacement: "&0;"
                         },
                       ]
                   }
               }
           },
           { name: "filter",
             description: "Filter (clear, red, ...)",
             show: true,
             resolver: "RegExpList",
             parameters:
               { RegExpList:
                   { key: "INSFLNAM",
                     reChecks:
                       [ { regexp: '/.*/i',
                           replacement: "&0;"
                         }
                       ]
                   }

               }
           },
           { name: "exposure",
             description: "Exposure in seconds",
             show: true,
             resolver: "Integer",
             parameters:
               { Integer:
                   { key: "EXPTIME", // also EXPOSURE
                     abs: true,
                     format: "%4.4d",
                   }
               }
           },
           { name: "temp",
             description: "Temperature in C",
             show: true,
             resolver: "Integer",
             parameters:
               { Integer:
                   { key: "CCDTEMP", // Also CCDTEMP and CCD-TEMP",
                     abs: true,
                     format: "%4.4d"
                   }
               }
           },
           { name: "binning",
             description: "Binning as 1x1, 2x2, ...",
             show: true,
             resolver: "IntegerPair",
             parameters:
               { IntegerPair:
                   { key1: "CDELT1",
                     key2: "CDELT2",
                     format: "%dx%d"
                   }
               }
           },
           { name: "night",
             description: "night (experimental)",
             show: true,
             resolver: "Night",
             parameters:
               { Night:
                   { keyLongObs: "CAHA TEL GEOLON",
                   // We should really used DATE-OBS (if available) and convert
                    keyJD: "JUL-DATE"
                   }
               }
           },
           { name: "filename",
             description: "Input file name",
             show: false,
             resolver: "FileName",
             parameters:
               { FileName:
                   {
                   }
               }
           },
           { name: "extension",
             description: "Input file extension",
             show: false,
             resolver: "FileExtension",
             parameters:
               { FileExtension:
                   {
                   }
               }
           }
         ];

   defaultConfigurationSet.push(userConf);


   // Default configuration for images originated in the CAHA (http://www.caha.es/) observatory
   var iTelescopeConf = deepCopyData(defaultConf);
   iTelescopeConf.name = "iTelescope";
   iTelescopeConf.description = "A default configuration based on images from iTelescope T16 in nov 2014";
   // Just the differences from the base configuration - which is about everything
   iTelescopeConf.variableList =
         [ { name: "type",
             description: "Type of image (flat, bias, ...)",
             show: true,
             resolver: "RegExpList",
             parameters:
               { RegExpList:
                   { key: "IMAGETYP",
                     reChecks:
                       [ { regexp: '/flat/i',
                           replacement: "flat"
                         },
                         { regexp: '/bias/i',
                           replacement: "bias"
                         },
                         { regexp: '/offset/i',
                           replacement: "bias"
                         },
                         { regexp: '/dark/i',
                           replacement: "dark"
                         },
                         { regexp: '/light/i',
                           replacement: "light"
                         },
                         { regexp: '/science/i',
                           replacement: "light"
                         },
                         { regexp: '/.*/',
                           replacement: "&0;"
                         },
                       ]
                   }
               }
           },
           { name: "filter",
             description: "Filter (clear, red, ...)",
             show: true,
             resolver: "RegExpList",
             parameters:
               { RegExpList:
                   { key: "FILTER",
                     reChecks:
                       [ { regexp: '/green/i',
                           replacement: "G"
                         },
                         { regexp: '/blue/i',
                           replacement: "B"
                         },
                         { regexp: '/red/i',
                           replacement: "R"
                         },
                         { regexp: '/clear/i',
                           replacement: "C"
                         },
                         { regexp: '/luminance/i',
                           replacement: "L"
                         },
                         { regexp: '/^ *V */',
                           replacement: "V"
                         },
                         { regexp: '/.*/',
                           replacement: "&0;"
                         }
                       ]
                       ,
                   }
               }
           },
           { name: "exposure",
             description: "Exposure in seconds",
             show: true,
             resolver: "Integer",
             parameters:
               { Integer:
                   { key: "EXPTIME", // also EXPOSURE
                     abs: true,
                     format: "%4.4d",
                   }
               }
           },
           { name: "temp",
             description: "Temperature in C",
             show: true,
             resolver: "Integer",
             parameters:
               { Integer:
                   { key: "CCDTEMP", // Also CCDTEMP and CCD-TEMP",
                     abs: true,
                     format: "%3.3d"
                   }
               }
           },
           { name: "binning",
             description: "Binning as 1x1, 2x2, ...",
             show: true,
             resolver: "IntegerPair",
             parameters:
               { IntegerPair:
                   { key1: "CDELT1",
                     key2: "CDELT2",
                     format: "%dx%d"
                   }
               }
           },
           { name: "object",
             description: "Name of target object",
             show: true,
             resolver: "Text",
             parameters:
               {Text:
                   {key:"OBJECT",
                    format:"%ls",
                    case:"UP"
                   }
               }
           },
           { name: "night",
             description: "night (experimental)",
             show: true,
             resolver: "Night",
             parameters:
               { Night:
                   { keyLongObs: "LONG-OBS",
                     keyJD: "JD"
                   }
               }
           },
           { name: "filename",
             description: "Input file name",
             show: false,
             resolver: "FileName",
             parameters:
               { FileName:
                   {
                   }
               }
           },
           { name: "extension",
             description: "Input file extension",
             show: false,
             resolver: "FileExtension",
             parameters:
               { FileExtension:
                   {
                   }
               }
           }
         ];

   defaultConfigurationSet.push(iTelescopeConf);




   // --- singletons variables ---------------------------------------------------
   // The configuration set (a table of configurations)
   // Used only by the parameters and the DialogConfiguration handler
   var configurationTable = defaultConfigurationSet;

   // The active configuration name
   // The engine works on a COPY of the active configuration, this is why we keep only
   // track the name of the active configuration.
   var activeConfigurationName = null;


   // Methods used to extract commonly needed information from the ConfigurationSet

   // Get the names of the all the configurations
   var getAllConfigurationNames = function(aConfigurationTable) {
      var names = [];
      for (var i=0; i<aConfigurationTable.length; i++) {
         names.push(aConfigurationTable[i].name);
      }
      return names;
   }

   // Get the rule by name
   var getConfigurationByName = function(aConfigurationTable, name) {
      for (var i=0; i<aConfigurationTable.length; i++) {
         if (aConfigurationTable[i].name === name) return aConfigurationTable[i];
      }
      return null;
   }

   // At least one configuration is required
   var removeConfigurationByName = function(aConfigurationTable, name) {
      if (aConfigurationTable.length>1) {
         for (var i=0; i<aConfigurationTable.length; i++) {
            if (aConfigurationTable[i].name === name) {
               aConfigurationTable.splice(i,1);
               break;
            }
         }
         // Return the name of the new first configuration
         return aConfigurationTable[0].name;
      }
      return null;
   }



   // --- List of all synthethic variables and their comments (2 parallel arrays)
   //     All synthethic variables are currently added to the columns of the file TreeBox
   // Currently just add all variables of the current resolver
   // TODO Handle non resolver variables, move somewhere else
   var syntheticVariableNames = [];
   var syntheticVariableComments = [];


   // The calling code must re-configure the GUI and the engine after calling this function
   var setActiveConfigurationName = function (nameOfNewActiveConfiguration) {
      activeConfigurationName = nameOfNewActiveConfiguration;
#ifdef DEBUG
      Log.debug("Configuration activated: ", activeConfigurationName);
#endif

      // TODO Should be moved
      // Rebuild the list of all synthetic variables that can be shown in the GUI.
      var activeConfiguration = getConfigurationByName(configurationTable,nameOfNewActiveConfiguration);
      syntheticVariableNames = [];
      syntheticVariableComments = [];
      for (var i=0; i<activeConfiguration.variableList.length; i++) {
         var aVar = activeConfiguration.variableList[i];
         syntheticVariableNames.push(aVar.name);
         syntheticVariableComments.push(aVar.description);
      }
   }



   // -- Support for variable handling

   // Model of variable - define a new variable
   var defineVariable = function(name, description, resolver) {
      var initialValues = deepCopyData(ffM_Resolver.resolverByName(resolver).initial);
      var initialParameters = {};
      initialParameters[resolver] = initialValues;
      return {
         name: name,
         description: description,
         show: true,
         resolver: resolver,
         parameters: initialParameters,
      }
   }

   // Support for active configuration

   var getConfigurationTable = function() {
      return configurationTable;
   }
   var getActiveConfigurationName = function() {
      return activeConfigurationName;
   }
   // Return the reference to the active configuration (NOT TO A WORKIGN COPY)
   var getActiveConfigurationElement = function() {
      return getConfigurationByName(configurationTable,activeConfigurationName);
   }

   // Replace the current ConfigurationSet by a new ConfigurationSet, activate a new Configuration
   var replaceConfigurationTable = function(newConfigurationTable, nameOfNewActiveConfiguration) {
      configurationTable = newConfigurationTable;
      setActiveConfigurationName(nameOfNewActiveConfiguration);
   }

   var createWorkingConfiguration = function() {
      var configuration = getConfigurationByName(configurationTable,activeConfigurationName);
      if (configuration == null) {
         throw "FITSFileManager-parameters - Invalid configuration '"+ name + "'";
      }
      return deepCopyData(configuration);
   }



#ifdef NO
  // TODO See how this is defined in the new mecanism
   // List of FITS keywords shown by default (even if not present in any image) in the input files TreeBox
   var defaultShownKeywords_DEFAULT = [
      "IMAGETYP","FILTER","OBJECT"
      //"SET-TEMP","EXPOSURE","IMAGETYP","FILTER","XBINNING","YBINNING","OBJECT"
   ];
   var defaultShownKeywords_CAHA = [
      "IMAGETYP","INSFLNAM","OBJECT"
      //"SET-TEMP","EXPOSURE","IMAGETYP","FILTER","XBINNING","YBINNING","OBJECT"
   ];

#endif



   // Activate some default configuration
   setActiveConfigurationName(configurationTable[0].name);

   // --- public properties and methods ---------------------------------------
   return {

      // The public singletons
      getConfigurationTable: getConfigurationTable,
      getActiveConfigurationName: getActiveConfigurationName,
      getActiveConfigurationElement: getActiveConfigurationElement,

      // Manage active configurations
      createWorkingConfiguration: createWorkingConfiguration,
      setActiveConfigurationName: setActiveConfigurationName,

      // Methods on a configuration set
      replaceConfigurationTable: replaceConfigurationTable,
      getAllConfigurationNames: getAllConfigurationNames,
      getConfigurationByName: getConfigurationByName,
      removeConfigurationByName: removeConfigurationByName,

      // Support for variables
      defineVariable: defineVariable,

   // TODO Should probably be in the engine or variable handling,
      syntheticVariableNames: syntheticVariableNames,
      syntheticVariableComments: syntheticVariableComments,



   };
}) ();



// ====================================================================================================================
// User Interface Parameters
// ====================================================================================================================

// The object FFM_GUIParameters keeps track of the parameters that are saved between executions
// This include the Configurations and many processing options
function FFM_GUIParameters() {
   // Called at end of constructor
   this.initializeParametersToDefaults = function () {

      // Default temp
      this.targetFileNameTemplate = FFM_DEFAULT_TARGET_FILENAME_TEMPLATE;

      // Default regular expression to parse file name
      this.sourceFileNameRegExp = FFM_DEFAULT_SOURCE_FILENAME_REGEXP;

      this.orderBy = "&rank;" // UNUSED - TODO Remove or clarify


      // Initialiy the first configuration is the default
      this.currentConfigurationIndex = 0;




      // Create templates (use defaults if not yet specified), precompile them
      this.groupByTemplate = FFM_DEFAULT_GROUP_TEMPLATE;
      var templateErrors = [];
      this.targetFileNameCompiledTemplate = ffM_template.analyzeTemplate(templateErrors, FFM_DEFAULT_TARGET_FILENAME_TEMPLATE);
      this.groupByCompiledTemplate = ffM_template.analyzeTemplate(templateErrors, FFM_DEFAULT_GROUP_TEMPLATE);
      if (templateErrors.length>0) {
         throw "PROGRAMMING ERROR - default built-in template is invalid";
      }

      // Prepare list of regexp, groupBy template and target file template for use by the user interface.
      // The first element of the list is the last one selected by the user, the others are pre-defined elements
      // (currently hardcoded here - could eventually be made editable)
      // There are two parallel arrays, one for the values, and one for a comment displayed in the selection box
      this.targetFileItemListText = [
            this.targetFileNameCompiledTemplate.templateString, // Will be adapted after parameter loading
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
         regExpToString(this.sourceFileNameRegExp), // Will be adapted after parameter loading
         FFM_DEFAULT_SOURCE_FILENAME_REGEXP,
         "/.*/"
      ];
      this.regexpItemListComment = [
         "last",
         "extract name",
         "(everything)"
      ];


      this.groupItemListText = [
            this.groupByCompiledTemplate.templateString, // Will be adapted after parameter loading
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


   // For debugging and logging - the returned string MUST be escaped if written to the console
   this.toString = function() {
      var s = "GUIParameters:\n";
      s += "  targetFileNameTemplate:         " + this.targetFileNameCompiledTemplate.templateString + "\n";
      s += "  sourceFileNameRegExp:           " + regExpToString(this.sourceFileNameRegExp) + "\n";
      s += "  orderBy:                        " + this.orderBy + "\n";
      s += "  groupByTemplate:                " + this.groupByCompiledTemplate.templateString + "\n";
      s += "  currentConfigurationIndex:      " + this.currentConfigurationIndex + "\n";
      return s;
   }

   this.initializeParametersToDefaults();

}


FFM_GUIParameters.prototype.loadSettings = function() {

    // TODO Better log in case of error loading the configuration, at least indicate that something went wrong
   function load( key, type )
   {
      var setting = Settings.read( FFM_SETTINGS_KEY_BASE + key, type );
#ifdef DEBUG
      var text = (setting===null ? 'null' : setting.toString());
      // To workaround slow console on 1.7
      if (text.length > 100) {
         text=text.substring(0,100) + "...";
      }
      debug("FFM_GUIParameters.load: "+ key+ ": "+ text + ", ok: " + Settings.lastReadOK);
#endif
      return setting;
   }

   function loadIndexed( key, index, type )
   {
      return load( key + '_' + index.toString(), type );
   }

   var o, t, parameterVersion, templateErrors, configurations, activeConfigurationName;
   if ( (parameterVersion = load( "version",    DataType_Double )) !== null ) {
      if (parameterVersion > PARAMETERS_VERSION) {
         Console.show();
         Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' have parameter version ", parameterVersion, " which is higher than the version supported by the scirpt (", PARAMETERS_VERSION, "), settings ignored");
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
               this.sourceFileNameRegExp = RegExpFromString(o);
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

         // After 0.8
   if (parameterVersion>0.8) {
            if ( (o = load( "configurations",          DataType_String )) !== null ) {
               configurations = JSON.parse(o);
            }
            if ( (o = load( "activeConfiguration",          DataType_String )) !== null ) {
               activeConfigurationName = o;
            }
            ffM_Configuration.replaceConfigurationTable(configurations, activeConfigurationName);
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
      var text = value.toString();
      // To workaround slow console on 1.7
      if (text.length > 100) {
         text=text.substring(0,100) + "...";
      }
      debug("saveSettings: key="+key+", type="+ type+ ", value=" +text);
#endif
      Settings.write( FFM_SETTINGS_KEY_BASE + key, type, value );
   }

   function saveIndexed( key, index, type, value ) {
#ifdef DEBUG
      debug("saveSettings: key="+key+", index="+ index+ ", type="+ type+ ", value=" +value.toString());
#endif
      save( key + '_' + index.toString(), type, value );
   }

   save( "version",                    DataType_Double, PARAMETERS_VERSION );
   save( "targetFileNameTemplate",     DataType_String, this.targetFileNameCompiledTemplate.templateString );
   save( "sourceFileNameRegExp",       DataType_String, regExpToString(this.sourceFileNameRegExp) );
   save( "orderBy",                    DataType_String, this.orderBy );
   save( "groupByTemplate",            DataType_String, this.groupByCompiledTemplate.templateString );
   save( "configurations",             DataType_String, JSON.stringify(ffM_Configuration.getConfigurationTable() ));
   save( "activeConfiguration",        DataType_String, ffM_Configuration.getActiveConfigurationName() );
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

