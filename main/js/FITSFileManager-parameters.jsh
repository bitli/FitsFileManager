// FITSFileManager-parameters.js

// This file is part of FITSFileManager, see copyright in FITSFileManager.js

// -----------------------------------------------------------------------------------------------
// This script support the parameters loaded/saved by the PI 'Settings' mechanism
// and the configuration information currently only modifiable by editing this file.
// -----------------------------------------------------------------------------------------------

#include <pjsr/DataType.jsh>
#include <pjsr/FileMode.jsh>


// --- Configuration - Change the definitions as needed.
//                     Some definitions need to be consistent with others or with the code,
//                     but this should be fairly obvious.


// Define a version for the PARAMETERS ( a double, independent of the script version)
// History:
//   0.8 - RC versions, did not include 'configurations'
//   1.0 - First supported version - includes 'configurations'
//   1.1 - Add 
#define PARAMETERS_VERSION 1.1


// TODO Should not be a global or define, but scoped variables

// Default values of some parameters, used only on the first usage of the script (when the settings are not yet defined)

// Select the first sequence without -_. or the whole name in &1; (second group is non capturing)
#define FFM_DEFAULT_SOURCE_FILENAME_REGEXP /([^-_.]+)(?:[._-]|$)/
#define FFM_DEFAULT_TARGET_FILENAME_TEMPLATE "&1;_&binning;_&temp;C_&type;_&exposure;s_&filter;_&count;&extension;"
#define FFM_DEFAULT_GROUP_TEMPLATE "&targetDir;"







// Name of key in settings
#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"


#define FFM_CONFIGURATION_FILE_SENTINEL "FITSFileManager.configurations"

#define FFM_CONFIGURATION_FILES_FILTER "*.ffm-configs"


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
// One configuration is the 'current' configuration. It is a copy of the selected configuration
// and can be complemented by data used for processing (typically the implementation of the resolvers).
// That copy is not modified by the ConfigurationDialog or saved as parameter. If another Configuration
// is selected, a new copy of the selected Configuration will replace the 'current configuration' and all derived
// data will be recalculated.




// ffM_Configuration module (make local name space)
var ffM_Configuration = (function() {

   var i;

   // Default ConfigurationSet, serve also as example of the structure.
   // The structure is like:
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

   // Used only on initial start, if neither the settings nor the script directory contains configur
   var defaultConfigurationSet =
   [
      { name: "Default",
       description: "Common FITS rules",
       version: PARAMETERS_VERSION,
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

   


   // --- singletons variables ---------------------------------------------------
   // The configuration set (a table of configurations)
   // Used only by the parameters and the DialogConfiguration handler
   var configurationTable = defaultConfigurationSet;

   // The active configuration name
   // The engine works on a COPY of the active configuration, this is why we keep only
   // track the name of the active configuration.
   var activeConfigurationName = null;


   // Methods used to extract commonly needed information from the ConfigurationSet

   // Get the names of the all the loade configurations
   var getAllConfigurationNames = function(aConfigurationTable) {
      var names = [];
      for (var i=0; i<aConfigurationTable.length; i++) {
         names.push(aConfigurationTable[i].name);
      }
      return names;
   }

   // Get a configuration by name
   var getConfigurationByName = function(aConfigurationTable, name) {
      for (var i=0; i<aConfigurationTable.length; i++) {
         if (aConfigurationTable[i].name === name) return aConfigurationTable[i];
      }
      return null;
   }

   // Remove a configuration by name, but ensure that at least one configuration is present
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

   var restoreDefaultConfiguration = function() {
     configurationTable = defaultConfigurationSet;
     this.setActiveConfigurationName(configurationTable[0].name);
   }



   // --- List of all synthethic variables and their comments (2 parallel arrays)
   //     All synthethic variables are currently added to the columns of the file TreeBox
   // Currently just add all variables of the current resolver
   // TODO Handle non resolver variables, move somewhere else
   var syntheticVariableNames = [];
   var syntheticVariableComments = [];


   // Note: The caller must request re-configuration of the GUI and of the engine after calling this function
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

   // Define a  new variable based on the model embedded in this code
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

   // Accessors and support for managing the active configuration

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


  // Support to save/load configuration

  

  // Load a configuration file, parse it and check its validity,
  // return an object with the list of messages (empty array if none) and a configuration,
  // the configuration is null if there is a fatal error.
  var loadConfigurationFile = function loadConfigurationFile(fileName) {
    var file = null;
    try {
      file = new File(fileName, FileMode_Read);
      var buffer = file.read( DataType_ByteArray, file.size );
    } catch (ex) {
      return ({messages: ["The file '" + fileName +"' is not readable."], configurations: null});
    } finally {
      if (file != null) file.close();
    }
    var loadedJSON = buffer.toString();
    try {
      var configurationData = JSON.parse(loadedJSON);
    } catch (ex) {
      return ({messages: ["File '" + fileName +"' is not a valid FITSFileManager configuration file, not JSON format."], configurations: null});
    }
    // Check that it is a proper configuration file 
    if (! configurationData.hasOwnProperty("sentinel")  || configurationData.sentinel != FFM_CONFIGURATION_FILE_SENTINEL) {
      return ({messages: ["File '" + fileName +"' is not a valid FITSFileManager configuration file, has not expected structure (missing or bad 'sentinel')."], configurations: null});      
    }
    if (! configurationData.hasOwnProperty("configurations") ) {
      return ({messages: ["File '" + fileName +"' is not a valid FITSFileManager configuration file, has not expected structure (missing 'configurations')."], configurations: null});      
    }
    if (! configurationData.hasOwnProperty("version") ) {
      return ({messages: ["File '" + fileName +"' is not a valid FITSFileManager configuration file, has not expected structure (missing 'version')."], configurations: null});      
    }
     if (typeof configurationData.version !== 'number') {
            return ({messages: ["File '" + fileName +"' is not a valid FITSFileManager configuration file, has not expected structure ('version' not numeric)."], configurations: null});      
     }
    if (configurationData.version > PARAMETERS_VERSION) {
      return ({messages: ["File '" + fileName +"' is a FITSFileManager configuration file, but has version" +
              configurationData.version + " higher than current version " + PARAMETERS_VERSION + "."], configurations: null});      
    }

    var messages = [];
    var configurations = this.validateConfigurationData(configurationData.configurations, messages);

    return {messages: messages, configurations: configurations};
  }

  // Merge configurations
  //   check what is merged,  replace or add

  // Save configuration
  //   Get single or all, format json, validate, write/overwrite file,
  // Save a configuration table (possibly consisting of a single configuration)
  // as a JSON file. The file has a header part in addition to the configuration
  var saveConfigurationFile = function saveConfigurationFile(fileName, configurations) {
    var configurationData = {sentinel: FFM_CONFIGURATION_FILE_SENTINEL, version: PARAMETERS_VERSION, configurations: configurations};
    var configurationJSON = JSON.stringify(configurationData, null, 2);
    var file;
    try {
       file = new File(fileName, FileMode_Create);
       file.outText(configurationJSON, DataType_String8 );
    } catch (ex) {
       return ("The file '" + fileName +"' is not writeable: " + ex);
    } finally {
       if (file != null) file.close();
    }
    return null;
  }
  

  // Validation:
  //    Check presence and type of elements of a configuration table
  //    return  configurations possibly adapted or null if fatal error
  //    Append any warning or error to the array messages (tehre are errors if null is returned)
  var validateConfigurationData = function validateConfigurationData(configurations, messages) {
    try {
      if (!Array.isArray(configurations)) throw("Not array");
      if (configurations.length==0) throw "Empty array";

      for (var i=0; i<configurations.length; i++) {
        var configuration = configurations[i];
        if (typeof configuration !== 'object') throw("Not object element");

        if (!configuration.hasOwnProperty("name")) throw("Missing 'name'");
        if (typeof configuration.name !== 'string') throw("'name' not string");
        
        if (!configuration.hasOwnProperty("description")) throw("Missing 'description'");
        if (typeof configuration.description !== 'string') throw("'description' not string");
        
        if (!configuration.hasOwnProperty("variableList")) throw("Missing 'variableList'");
        var variableList = configuration.variableList
        if (!Array.isArray(variableList)) throw("'variableList' not array");
      
        for (var j=0; j<variableList.length; j++) {
          var variable = variableList[j];
          if (typeof variable !== 'object') throw("variableList["+j+"] not object element");
          if (!variable.hasOwnProperty("name")) throw("Missing 'variableList["+j+"].name'");
          if (typeof variable.name !== 'string') throw("'variableList["+j+"].name' not string");
          if (!variable.hasOwnProperty("description")) throw("Missing 'variableList["+j+"].description'");
          if (typeof variable.description !== 'string') throw("'variableList["+j+"].description' not string");
          if (!variable.hasOwnProperty("show")) throw("Missing 'variableList["+j+"].show'");
          if (typeof variable.show !== 'boolean') throw("'variableList["+j+"].show' not boolean");
          if (!variable.hasOwnProperty("resolver")) throw("Missing 'variableList["+j+"].resolver'");
          if (typeof variable.resolver !== 'string') throw("'variableList["+j+"].resolver' not string");
          if (!variable.hasOwnProperty("parameters")) throw("Missing 'variableList["+j+"].parameters'");
          if (typeof variable.parameters != 'object') throw("'variableList["+j+"].parameters' not object");
        }

        if (!configuration.hasOwnProperty("builtins")) throw("Missing 'builtins'");
        var builtins = configuration.builtins
        if (typeof builtins != 'object') throw("'builtins' is not object");      
        if (!builtins.hasOwnProperty("rank")) throw("Missing 'builtins.rank'");
        if (typeof builtins.rank !== 'object') throw("'builtins.rank' is not object"); 
        if (!builtins.rank.hasOwnProperty("format")) throw("Missing 'builtins.rank.format'");
        if (typeof builtins.rank.format !== 'string') throw("'builtins.rank.format' is not string"); 

        if (!builtins.hasOwnProperty("count")) throw("Missing 'builtins.resolver'");
        if (typeof builtins.count !== 'object') throw("'builtins.count' is not object"); 
        if (!builtins.count.hasOwnProperty("format")) throw("Missing 'builtins.count.format'");
        if (typeof builtins.count.format !== 'string') throw("'builtins.count.format' is not string"); 

      }

      return configurations;

    } catch (ex) {
      messages.push(ex);
      return null;
    }
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
      restoreDefaultConfiguration: restoreDefaultConfiguration,

      // Manage active configurations
      createWorkingConfiguration: createWorkingConfiguration,
      setActiveConfigurationName: setActiveConfigurationName,

      // Methods on a configuration set
      replaceConfigurationTable: replaceConfigurationTable,
      getAllConfigurationNames: getAllConfigurationNames,
      getConfigurationByName: getConfigurationByName,
      removeConfigurationByName: removeConfigurationByName,

      saveConfigurationFile: saveConfigurationFile,
      loadConfigurationFile: loadConfigurationFile,
      validateConfigurationData: validateConfigurationData,

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
// This include the Configurations and other processing options
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


      // Use some reasonable default for the directory to use as the default path to load/save configurations
      this.lastConfigurationDirectory = File.homeDirectory;


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

      // File templates and the corresponding comments
      this.targetFileItemListText = [
            this.targetFileNameCompiledTemplate.templateString, // Will be adapted after parameter loading
            FFM_DEFAULT_TARGET_FILENAME_TEMPLATE,
            "&type;/&1;_&binning;_&temp;C_&exposure;s_&filter;_&count;&extension;",
            "&OBJECT;_&filter;_&count;&extension;",
            "&1;_&type?light;_&filter?clear;_&count;&extension;",
            "&object;_&telescope;_&dateobs;_&binning;_&temp;C_&exposure;s_&filter;_&PIERSIDE;_&count;_cal&extension;",
            ""
      ];
      this.targetFileItemListComment = [
            "last",
            "detailled, using part of file name",
            "directory by type",
            "object and filter",
            "type and filter with defaults",
            "iTelescope calibrated image",
            "(clear)"
      ];

      // Regular expressions to parse file names and their corresponding comments
      this.regexpItemListText = [
         regExpToString(this.sourceFileNameRegExp), // Will be adapted after parameter loading
         regExpToString(FFM_DEFAULT_SOURCE_FILENAME_REGEXP),
         "/-([0-9]+)-/",
         "/.*/"
      ];
      this.regexpItemListComment = [
         "last",
         "extract name",
         "extract date part from file in iTelescope file format",
         "(everything)"
      ];


      // Template for generating group names and their comments
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


   // For debugging and logging - the returned string MUST be escaped (for html special chararacters) if written to the console
   this.toString = function() {
      var s = "GUIParameters:\n";
      s += "  targetFileNameTemplate:         " + this.targetFileNameCompiledTemplate.templateString + "\n";
      s += "  sourceFileNameRegExp:           " + regExpToString(this.sourceFileNameRegExp) + "\n";
      s += "  orderBy:                        " + this.orderBy + "\n";
      s += "  groupByTemplate:                " + this.groupByCompiledTemplate.templateString + "\n";
      s += "  currentConfigurationIndex:      " + this.currentConfigurationIndex + "\n";
      s += "  lastConfigurationDirectory:     " + this.lastConfigurationDirectory + "\n";
      return s;
   }

   this.initializeParametersToDefaults();

}


// Load the settings (this include the set of 'Configurations') from the PI settings of the application
// The configurations are loaded from a JSon string in the settings
FFM_GUIParameters.prototype.loadSettings = function() {

    // TODO Better log in case of error loading the configuration, at least indicate that something went wrong
    // TODO Possibly recover in case of error, validate the type of data
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

   // --- loadSettings
   // Check if we have parameters, laod from parameters if present
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
               this.sourceFileNameRegExp = regExpFromString(o);
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
            // TODO Protect this agains error, move to a separate method, refactor validation with load from file
            configurations = JSON.parse(o);
          }
          if ( (o = load( "activeConfiguration",          DataType_String )) !== null ) {
             activeConfigurationName = o;
          }
          ffM_Configuration.replaceConfigurationTable(configurations, activeConfigurationName);
        }

        // After 1.0
        if (parameterVersion>1.0) {
          if ( (o = load( "lastConfigurationDirectory",  DataType_String )) !== null ) {
            if (File.directoryExists(o)) {
              this.lastConfigurationDirectory = o;
            }
          }
        }
        

      }

   } else {
      // There is no parameter, load all from script directory
      Console.show();
      Console.writeln("Warning: Settings '", FFM_SETTINGS_KEY_BASE, "' do not have a 'version' key, settings ignored.");
      Console.flush();

      // Initialize with built-in defaults in case none is found
      configurations = deepCopyData(ffM_Configuration.getConfigurationTable());
      var defaultConfigurationName = configurations[0].name;

      var scriptDir = getDirectoryOfFileWithDriveLetter(#__FILE__);
      Console.writeln("Loading initial configurations from directory '" + scriptDir + "'");

      var find = new FileFind;
     
      if ( find.begin( scriptDir + "/" + FFM_CONFIGURATION_FILES_FILTER) ) {
         do
         {
            if ( find.name != "." && find.name != ".." && find.isFile ) {
              var filePath = scriptDir + "/" + find.name;
              Console.writeln("Add configurations from file '" + find.name + "'.");
              var result = ffM_Configuration.loadConfigurationFile(filePath);
              var loadedConfigurations = result.configurations;
              if ( loadedConfigurations != null) {
                // TODO Refactor with load configuration
                for (var i=0; i<loadedConfigurations.length; i++) {
                  var replaced = false;
                  for (var j=0; j<configurations.length; j++) {
                    if (loadedConfigurations[i].name === configurations[j].name) {
                      replaced = true;
                      configurations.splice(j,1);
                    }
                  }
                  configurations.push(loadedConfigurations[i]);
                  Console.writeln("   " + (replaced ? "replaced '" : "loaded '") + loadedConfigurations[i].name + "'");
                }
              } else {
                Console.writeln("ERRORS loading configuration: " + result.messages.join(", "))
              }
            }
         } while ( find.next() );

         ffM_Configuration.replaceConfigurationTable(configurations, defaultConfigurationName);

      }
      
   }

};


// Save the settings including the 'configuration's to the PI application settings
// The configurations are saved as a JSon array
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

   // --- saveSettings
   save( "version",                    DataType_Double, PARAMETERS_VERSION );
   save( "targetFileNameTemplate",     DataType_String, this.targetFileNameCompiledTemplate.templateString );
   save( "sourceFileNameRegExp",       DataType_String, regExpToString(this.sourceFileNameRegExp) );
   save( "orderBy",                    DataType_String, this.orderBy );
   save( "groupByTemplate",            DataType_String, this.groupByCompiledTemplate.templateString );
   save( "configurations",             DataType_String, JSON.stringify(ffM_Configuration.getConfigurationTable() ));
   save( "activeConfiguration",        DataType_String, ffM_Configuration.getActiveConfigurationName() );
   save( "lastConfigurationDirectory", DataType_String, this.lastConfigurationDirectory);
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

