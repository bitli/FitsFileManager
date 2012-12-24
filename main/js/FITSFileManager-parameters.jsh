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
#define FFM_DEFAULT_GROUP_TEMPLATE "&targetDir;"







// Name of key in settings
#define FFM_SETTINGS_KEY_BASE  "FITSFileManager/"


// ====================================================================================================================
// Configuration support module
// ====================================================================================================================

// ========================================================================================================================
// Model
// ========================================================================================================================

// There is a 'pure data' representation of the rules (so they can be serialized)
// and the Model complement this representation with convenience methods

// RuleSet data (ordered list of rules, the order does not matter for the semantic)
//    [rule]

// Rule data (the variables is an ordered list)
//    {name: aString, description: aString, variableList: []}

// Variable: (the parameters dictionary depends on the resolver, the parameters of a resolver
//            are qualified by the resolver name allowing multiple resolvers at the same
//            time while in memory)
//    {name: aString, description: aString, resolver: aName, parameters: {resolverName: {}}}



var ffM_RuleSet_Model = (function(){


   // -- RuleSet support functions
   // Get the names of the rules
   var ruleNames = function(ruleSet) {
      var names = [];
      for (var i=0; i<ruleSet.length; i++) {
         names.push(ruleSet[i].name);
      }
      return names;
   }

   // Get the rule by name
   var ruleByName = function(ruleSet, name) {
      for (var i=0; i<ruleSet.length; i++) {
         if (ruleSet[i].name === name) return ruleSet[i];
      }
      return null;
   }


   // Model of variable - define a new variable
   var defineVariable = function(name, description, resolver) {
      var initialValues = deepCopyData(ffM_Resolver.resolverByName(resolver).initial);
      var initialParameters = {};
      initialParameters[resolver] = initialValues;
      return {
         name: name,
         description: description,
         resolver: resolver,
         parameters: initialParameters,
      }
   }

   // Get the rule by name
   var variableByName = function(variableList, name) {
      for (var i=0; i<variableList.length; i++) {
         if (variableList[i].name === name) return variableList[i];
      }
      return null;
   }



   return {
      ruleNames: ruleNames,
      ruleByName: ruleByName,
      defineVariable: defineVariable,
   }
}) ();



var ffM_Configuration = (function() {
   // This module is a singleton that supports 'named configuration' data objects,
   // and the set of all named configurations known to FITSFileManager.

   // A named configuration is a named set of parameters that can be selected as a whole,
   // typically the configuration suitable for an observatory, an instrument and a user.
   // NOT ALL ELEMENTS OF A CONFIGURATION CAN YET BE EDITED INTERACTIVELY.

   // --- constants (predefined configuration elements)  ----------------------

   // --- Mapping of 'logical' FITS keywords (referenced in the code) to actual FITS keywords
   //     The logical keyword is by convention a 'PascalCased' name of the variable using
   //     the keyword and some differentiating suffix if multiple keywords are required.
   //     This does not really matter, just more mnemotecnic than random text
   // TODO Support optional and multiple keywords (as EXPOSURE and EXPTIME) and default value
   var keywordMapping_DEFAULT = {
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

   var keywordMapping_CAHA = {
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

   // -- Define the predefined named configurations
   // The configuration is a map with the following entries:
   //    name - its name (used to save in settings)
   //    description - A one line description of the configuration (shown in UI)
   //    keywordMappingTable - The map of logical name to FITS key names
   //    filterConversion - The list of filter conversion operations
   //    typeConversion - The list of filter conversion operations
   var configuration_DEFAULT = {
     name: "DEFAULT",
     description: "Common and Star Arizona mappings",
     keywordMappingTable: keywordMapping_DEFAULT,
     filterConversions: filterConversions_DEFAULT,
     typeConversions: typeConversions_DEFAULT,
     defaultShownKeywords: defaultShownKeywords_DEFAULT,
   };
   var configuration_CAHA = {
     name: "CAHA",
     description: "CAHA mapping",
     keywordMappingTable: keywordMapping_CAHA,
     filterConversions: filterConversions_CAHA,
     typeConversions: typeConversions_CAHA,
     defaultShownKeywords: defaultShownKeywords_CAHA,
   };




// --------------------------------------------------------------------------------------------------

var defaultRuleSet =
[ { name: "Default",
    description: "Common FITS rules",
    variableList:
      [ { name: "type",
          description: "Type of image (flat, bias, ...)",
          resolver: "RegExpList",
          parameters:
            { RegExpList:
                { key: "IMAGETYP",
                  reChecks:
                    [ { regexp: /flat/i,
                        replacement: "flat"
                      },
                      { regexp: /bias/i,
                        replacement: "bias"
                      },
                      { regexp: /offset/i,
                        replacement: "boas"
                      },
                      { regexp: /dark/i,
                        replacement: "dark"
                      },
                      { regexp: /light/i,
                        replacement: "light"
                      },
                      { regexp: /science/i,
                        replacement: "light"
                      },
                      { regexp: /.*/,
                        replacement: "&0;"
                      },
                    ]
                }
            }
        },
        { name: "filter",
          description: "Filter (clear, red, ...)",
          resolver: "RegExpList",
          parameters:
            { RegExpList:
                { key: "FILTER",
                  reChecks:
                    [ { regexp: /green/i,
                        replacement: "green"
                      },
                      { regexp: /blue/i,
                        replacement: "blue"
                      },
                      { regexp: /red/i,
                        replacement: "red"
                      },
                      { regexp: /clear/i,
                        replacement: "celar"
                      },
                      { regexp: /luminance/i,
                        replacement: "luminance"
                      },
                      { regexp: /.*/,
                        replacement: "&0"
                      }
                    ]
                    ,
                }
            }
        },
        { name: "exposure",
          description: "Exposure in seconds",
          resolver: "Integer",
          parameters:
            { Integer:
                { key: "EXPTIME", // also EXPOSURE
                  format: "%4.4d"
                }
            }
        },
        { name: "temp",
          description: "Temperature in C",
          resolver: "Integer",
          parameters:
            { Integer:
                { key: "SET-TEMP", // Also CCDTEMP and CCD-TEMP",
                  format: "%4.4d"
                }
            }
        },
        { name: "binning",
          description: "Binning as 1x1, 2x2, ...",
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
          resolver: "Constant",
          parameters:
            { Constant:
                { value: ""
                }
            }
        },
        { name: "filename",
          description: "Input file name",
          resolver: "Constant",
          parameters:
            { Constant:
                { value: ""
                }
            }
        },
        { name: "extension",
          description: "Input file extension",
          resolver: "Constant",
          parameters:
            { Constant:
                { value: ""
                }
            }
        }
      ]
  }
]
;

   // --- private variables ---------------------------------------------------
   // name string -> configuration
   var configurationTable = defaultRuleSet;

   // array of names
   var configurationList = ffM_RuleSet_Model.ruleNames(configurationTable);

   // --- method implementations ----------------------------------------------
   function addConfiguration(configuration) {
      configurationTable[configuration.name] = configuration;
      configurationList.push(configuration.name);
   }

   function getConfigurationByName(name) {
      return ffM_RuleSet_Model.ruleByName(configurationTable,name);
   }
   function getConfigurationByIndex(index) {
      return configurationTable[index];
   }


   // --- List of all synthethic variables and their comments (2 parallel arrays)
   //     All synthethic variables are currently added to the columns of the file TreeBox
   // TODO Should directly access definitions
   var syntheticVariableNames = [];
   var syntheticVariableComments = [];
#ifdef NO
for (var i=0; i<shownVariablesNumber; i++) {
      syntheticVariableNames.push(variableDefinitions[i].name);
      syntheticVariableComments.push(variableDefinitions[i].description);
   }
#endif

   // --- public properties and methods ---------------------------------------
   return {
       configurationList: configurationList, // Consider readonly
       getConfigurationByName: getConfigurationByName,
       getConfigurationByIndex: getConfigurationByIndex,
   // TODO For curernt config
      syntheticVariableNames: syntheticVariableNames,
      syntheticVariableComments: syntheticVariableComments,

   };
}) ();



// ====================================================================================================================
// User Interface Parameters
// ====================================================================================================================

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


      // Initialiy the first configuration is the default
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


   // For debugging and logging - result MUST be escaped if written to the console
   this.toString = function() {
      var s = "GUIParameters:\n";
      s += "  targetFileNameTemplate:         " + this.targetFileNameCompiledTemplate.templateString + "\n";
      s += "  sourceFileNameRegExp:           " + regExpToString(this.sourceFileNameRegExp) + "\n";
      s += "  orderBy:                        " + this.orderBy + "\n";
      s += "  groupByTemplate:                " + this.groupByCompiledTemplate.templateString + "\n";
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
      debug("FFM_GUIParameters.load: "+ key+ ": "+ (setting===null ? 'null' : setting.toString()));
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
      debug("saveSettings: key="+key+", type="+ type+ ", value=" +value.toString());
#endif
      Settings.write( FFM_SETTINGS_KEY_BASE + key, type, value );
   }

   function saveIndexed( key, index, type, value ) {
#ifdef DEBUG
      debug("saveSettings: key="+key+", index="+ index+ ", type="+ type+ ", value=" +value.toString());
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

