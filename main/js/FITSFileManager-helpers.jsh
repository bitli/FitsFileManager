// FITSFileManager-helper.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js

#include <pjsr/DataType.jsh>




//=========================================================================================================================
// String and formatting utility functions
// ------------------------------------------------------------------------------------------------------------------------



// --- String functions



// --- Pad a mumber with leading 0
Number.prototype.pad = function(size){
      var s = String(this);
      while (s.length < size) s = "0" + s;
      return s;
}

// --- RegExp utility functions

function regExpToString(re) {
   if (re === null) {
      return "";
   } else {
   // Remove leading and trailing slahes and trailing flag
      var reString = re.toString();
      //var secondSeparator = reString.lastIndexOf(reString[0]);
      //return  reString.substring(1, secondSeparator);
      return  reString.substring(1, reString.length-1);
   }
}




//=========================================================================================================================
// Object data support
// ------------------------------------------------------------------------------------------------------------------------
// Deep copy an object (with object, array, basic types and RegExp only)
function deepCopyData(object) {
   var i;
   var result;
   if (object === null) {
      result = null;
   } else if (Array.isArray(object)) {
      result = [];
      for (i=0; i<object.length;i++) {
         result.push(deepCopyData(object[i]));
      }
   } else if (typeof object !== "object") {
      result = object;
   } else if (object.constructor === Date) {
      result = new Date(object.getTime());
   } else if (object.constructor === String) {
      result = new String(object);
   } else if (object.constructor === Boolean) {
      result = new Boolean(object);
   } else if (object.constructor === Number) {
      result = new Number(object);
   } else if (object.constructor === RegExp) {
      result = new RegExp(object.toString());
   } else if (object.constructor === Function) {
      throw "Cannot deep copy function";
   } else {
      // Any other data object
      result = {};
      var ps = Object.getOwnPropertyNames(object);
      for ( i=0; i<ps.length;i++) {
         result[ps[i]] = deepCopyData(object[ps[i]]);
      }
   }

   return result;
}





//=========================================================================================================================
// File utility functions
// ------------------------------------------------------------------------------------------------------------------------

function copyFile( sourceFilePath, targetFilePath ) {
   var f = new File;

   f.openForReading( sourceFilePath );
   var buffer = f.read( DataType_ByteArray, f.size );
   f.close();

   f = new File();
   f.createForWriting( targetFilePath );
   f.write( buffer );
   //f.flush(); // optional; remove if immediate writing is not required
   f.close();
}


// Load a file and save it with the new name,
// The file name is added to the ORIGFILE if not already present
// (this is not a standard FITS keyword, another possibility is to use
// the FILENAME FILEXT keywords, but they are not standard either)
// An HISTORY record is also added
#define FFM_FITS_HISTORY_LEADER "PI FITSFileManager from "

function loadSaveFile( sourceFilePath, targetFilePath ) {

   // More than one image may be loaded - this is an issue
   var images = ImageWindow.open( sourceFilePath,"FITSFileManagerLoadSaveAs_Temp", true );
   if (images.length !== 1) {
      throw "File '" + sourceFilePath + "' contains " + images.length + " images, this is not supported.";
   }
   var image = images[0];
   var keywords = image.keywords;
   var firstORIGFILE = false;
   for (var i=0; i<keywords.length; i++) {
      if (keywords[i].name === "ORIGFILE" && keywords[i].value) {
         firstORIGFILE = keywords[i].value;
         break;
      }
   }
   if (firstORIGFILE) {
      Console.writeln("Kept ORIGFILE as: " + firstORIGFILE);
   } else {
      var kw = new FITSKeyword( "ORIGFILE",
               File.extractName(sourceFilePath) + File.extractExtension(sourceFilePath),
               "Original name (FITSFileManager)");
      Console.writeln("Adding " + kw.name + ": '" + kw.value + "'");
      keywords.push( kw );
   }
   var kw = new FITSKeyword( "HISTORY", "", "PI FitsFileManager renamed as " + File.extractName(targetFilePath) + File.extractExtension(targetFilePath));
   keywords.push( kw );
   image.keywords = keywords;

   image.saveAs(targetFilePath,  false, false, false, false);

   image.forceClose();

}





//=========================================================================================================================
// Conversion support
// ------------------------------------------------------------------------------------------------------------------------
var ffM_LookupConverter = function() {

   var backReferenceRegExp = /&[0-9]+;/;
   var allBackReferenceNumberRegExp = /&([0-9])+;/g;

   var converterPrototype = {
      convert: function convert(unquotedName) {
         if (unquotedName === null) { return null }
         for (var i=0; i<this.compiledConversionTable.length; i++) {
            var compiledConversionEntry = this.compiledConversionTable[i];
            if (compiledConversionEntry[0].test(unquotedName)) {
               return compiledConversionEntry[1](compiledConversionEntry,unquotedName);
            }
         }
         // If not recognized, reject (use a . regexp to recognize anything)
         return null;
     }
   };

   // Create a lookup converter
   // Parameters:
   //      conversionTable: Array  of 2 elements array: patter, replacement
   // Return: A converter object that convert a string according to the rules.
   return {
      makeLookupConverter: function makeLookupConverter (conversionTable) {
         var c = Object.create(converterPrototype);
         // The conversion table is 'compiled' in a form where the first element is
         // the regular expression (as received) and the second is the method that
         // does the replacement. Unless a template variable is used in the replacement
         // variable, the function just copy the string. More complex replacement
         // can be done, using the source value and matched parts, and possibly formatting
         // as to lower case.
         var compiledConversionTable = [];
         for (var i=0; i<conversionTable.length; i++) {
            var conversionEntry = conversionTable[i];
            var conversionRegExp = conversionEntry[0];
            var conversionResultTemplate = conversionEntry[1];
            var conversionResultFunction;
            if (conversionResultTemplate==="&0;") {
               // Assumed frequent case of copying input
               conversionResultFunction = function(compiledEntry, unquotedName) {
                  // Cleanup from special characters
                  return ffM_variables.filterFITSValue(unquotedName);
               }
            } else if (backReferenceRegExp.test(conversionResultTemplate)) {
               // There are back refernce, using a replacing function
               conversionResultFunction = (function(conversionResultTemplate){
                  return function(compiledEntry, unquotedName) {
                     // Get the values of the subexpression (before we just tested the presence)
                     var matchedGroups = compiledEntry[0].exec(unquotedName);
                     var replaceHandler = function(fullString, p1, offset, string) {
                        var matchIndex = + p1; // Convert to index
                        if (matchIndex>= matchedGroups.length) {
                           // TODO Generate error in a more firendly way
                           return "BACKREFERENCETOOLARGE"; // Cannot replace, index too large
                        } else {
                           // Cleanup the returned value to avoid special characters
                           return ffM_variables.filterFITSValue(matchedGroups[matchIndex]);
                        }
                     };

                    return conversionResultTemplate.replace(allBackReferenceNumberRegExp,replaceHandler);
                  }
               })(conversionResultTemplate);

            } else {
               // Literal copy of template (no back reference), make sure we reference the value!
               conversionResultFunction = (function(conversionResultString) {
                  return function(ignored1, ignored2) {
                     return conversionResultString;
                  }
               })(conversionResultTemplate);
            }
            compiledConversionTable.push([conversionRegExp, conversionResultFunction]);
         }
         c.compiledConversionTable = compiledConversionTable;
         return c;
      }
   }
} ();







// ====================================================================================================================
// Template parsing and execution module
// ====================================================================================================================

// Create template support in a 'module' like object
var ffM_template = (function() {

  // Something like &stuf;
  var templateRegExp = /&[^&;]+;/g;
  // Extract parts of &var:truepart?falsepart;
  var variableRegExp = /^([^:?]+)(?::([^:?]*))?(?:\?([^:?]*))?/

  // Create a rule that return the parameter literal verbatim
  var makeLiteralRule = function(templateErrors,literal){
    // TODO Check that literal does not contains & ( ) ; < > = ! ( ) and % unless formatting is implemented)
    if (/[&\(\);<>=!%*]/.test(literal)) {
      templateErrors.push("Invalid characters in literal sequence " + literal);
    }
    var literalRule = function(errors) {
      return literal;
    }
    literalRule.toString = function() {return "literalRule('" + literal +"')"};
    return literalRule;
  }

  // Create a rule that interpolate a variable expression
  var makeLookupRule = function(templateErrors, expression)  {
    var variableName, onFoundAction, onMissingAction;
     // expression has & and ; already removed

     // Parse the expression of variable:present?missing parts, resulting in the corresponding elements in execResult
    var execResult = expression.match(variableRegExp);
    if (execResult === null) {
       templateErrors.push("Invalid variable expression '" + expression + "'");
       return null;
    } else {
        variableName = execResult[1];
    }


    // Create the handler for the case ':present'
    // execResult[2] is the text after the colon and before the end or question mark
    if (execResult[2]==='') {
      // Nothing, we copy the null string, so this is a noop
      onFoundAction = function(expandErrors, variableName, value){
        return ''
      }
      onFoundAction.toString = function(){return "copyLiteral('')"};
    } else if (execResult[2]) {
      // Something, the 'present' text is copied verbatim
      onFoundAction = function(expandErrors, variableName, value){
        return execResult[2];
      }
      onFoundAction.toString = function(){return "formatValueAs('"+execResult[2]+"')"};
    } else {
      // No ':present' part, we copy the source value
      onFoundAction = function(expandErrors, variableName, value){
        return value; // TODO SHOULD SUPPORT FORMATTING THE value
      }
      onFoundAction.toString = function(){return "copyValue()"};
    }

    // Create the handler for the case '?missing'
    // execResult[3] is the text after the question mark
    if (execResult[3]==='') {
      // Nothing, an optional value, we copy the null string
      onMissingAction = function(expandErrors){
        return '';
      }
      onMissingAction.toString = function(){return "copyLiteral('')"};
    } else if (execResult[3]) {
      // The 'missing' text is copied verbatim
      onMissingAction = function(expandErrors){
        return execResult[3]; // There should be no format
      }
      onMissingAction.toString = function(){return "copyLiteral('"+execResult[2]+"')"};
    } else {
      // No ?missing' part, we cannot generate the template in case of missing value
      onMissingAction = function(expandErrors, variableName){
         expandErrors.push("No value for the variable '" + variableName + "'");
         return '';
      }
      onMissingAction.toString = function(){return "reject()"};
    }

    // The lookup variable rule itself, that will use the handlers above
    var lookUpRule = function(expandErrors,variableResolver) {
      var value = variableResolver(variableName);
      // do not use 'undefined' to be 'use strict' friendly
      if (value !== null) {
        return onFoundAction(expandErrors, variableName, value);
      } else {
        return onMissingAction(expandErrors, variableName, value);
      }
    }
    lookUpRule.toString = function() { return "lookUpRule('" + variableName + "':[onFound:" + onFoundAction + "]"+ ":[onMissing:" + onMissingAction + "])"; }
    return lookUpRule;
  }


   // --- public properties and methods ---------------------------------------
  return {
    // Analyze a template string and return the compiled template (or null),
    // push the errors to templateErrors, which must be an array
    analyzeTemplate: function(templateErrors, template) {
#ifdef DEBUG_TEMPLATE
      debug("analyzeTemplate:'" + template + "'");
#endif
      // The replacing handler global variables
      var rules = []; // Invalid if error is not empty
      var iNext = 0; // next character that will be examined
      var replaceHandler = function(match, offset, string) {
        //print ("  rh: ", match, offset, string);
        if (offset>iNext) {
          rules.push(makeLiteralRule(templateErrors,string.substring(iNext, offset)));
        }
        rules.push(makeLookupRule(templateErrors,match.substring(1,match.length-1)));
        iNext = offset + match.length;
        return ''; // replace by nothing, ignored anyhow
      }
      // Each match will create the rule for the preceding literal text and the current match
      // Use 'replace' as it provides the need match information, if the replacement is not really used.
      template.replace(templateRegExp, replaceHandler);
      // If required add literal rule for trailing literal text
      if (template.length>iNext) {
          rules.push(makeLiteralRule(templateErrors,template.substring(iNext)));
      }

      // -- Defines the compiled Template
      // This objects is returned when a template has been analyzed (it is like a compiled regexp,
      // although it is for generating text rather than parsing it).
      var templateRuleSet = {
         // toString for debugging
         toString: function() {return rules.toString();},
         // Original string as a property
         templateString: template,
         requiredVariables: [], // TODO
         optionalVariables: [], // TODO

         // Method to expand the template using the variables returned by the variableResolver,
         // return the exanded string, null in case of error
         // The expandErrors must be an array, errors will be pushed to that array.
         // If any error is pushed, the returned value is meaningless
         expandTemplate: function(expandErrors, variableResolver) {
            // Execute the rules one by one, pushing the result
            var result = [];
            for (var i = 0; i<rules.length; i++) {
               result.push(rules[i](expandErrors, variableResolver));
            }
            return result.join('');
         }
      };

      return templateRuleSet;
    } // analyzeTemplate

  } // ffM_template object
})();



// ====================================================================================================================
// Variable definition and lookup module
// ====================================================================================================================

var ffM_variables = (function() {



   //   &binning     Binning from XBINNING and YBINNING formated as a pair of integers, like 2x2.
   function binningParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords) {
      var xBinning = parseInt(imageKeywords.getValue(remappedFITSkeywords['BinningX']));
      var yBinning = parseInt(imageKeywords.getValue(remappedFITSkeywords['BinningY']));
      if (isNaN(xBinning) || isNaN(yBinning)) {
         return null;
      } else {
         return xBinning.toFixed(0)+"x"+yBinning.toFixed(0);
      }
   }

   //   &exposure;   The exposure from EXPOSURE, formatted as an integer (assume seconds)
   function exposureParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords) {
      var exposure = imageKeywords.getValue(remappedFITSkeywords['Exposure']);
      var exposureF =  parseFloat(exposure);
      if (isNaN(exposureF)) {
         return null;
      } else {
         return exposureF.toFixed(0);
      }
   }

   //   &extension;   The extension of the source file (with the dot)
   function extensionParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords, inputFile) {
      return  File.extractExtension(inputFile);
   }

   //   &filename;   The file name part of the source file
   function filenameParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords, inputFile) {
       return  File.extractName(inputFile);
   }

   //   &filter:     The filter name from FILTER as lower case trimmed normalized name.
   function filterParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords) {
      var filter = imageKeywords.getUnquotedValue(remappedFITSkeywords['Filter']);
      return filterConverter.convert(filter);
   }

   //   &temp;       The SET-TEMP temperature in C as an integer
   function tempParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords) {
      var temp = imageKeywords.getValue(remappedFITSkeywords['Temp']);
      var tempF = parseFloat(temp);
      if (isNaN(tempF)) {
         return null;
      } else {
         return tempF.toFixed(0);
      }
   }

   //   &type:       The IMAGETYP normalized to 'flat', 'bias', 'dark', 'light'
   function typeParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords) {
      var imageType = imageKeywords.getUnquotedValue(remappedFITSkeywords['Type']);
      return typeConverter.convert(imageType);
   }


   //  &night;     EXPERIMENTAL
   function nightParser(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords) {
      var longObs = imageKeywords.getValue(remappedFITSkeywords['NightLongObs']); // East in degree
      // longObs = -110;
      // TODO Support default longObs
      var jd = imageKeywords.getValue(remappedFITSkeywords['NightJD']);
      if (longObs && jd) {
         var jdLocal = Number(jd) + (Number(longObs) / 360.0) ;
         var nightText = (Math.floor(jdLocal) % 1000).toString();
         return nightText;
      } else {
         return null;
      }
   }




   // Variable definitions
   // Each definiton has a name and a factory method
   // All definitions are in an array (ordered)
   var variableDefinitions = [];
   function defineVariable(name, description, method) {
      return {
         name: name,
         description: description,
         method: method,
      }
   }


   variableDefinitions.push(defineVariable('type','Type of image (flat, bias, ...)',typeParser));
   variableDefinitions.push(defineVariable('filter','Filter (clear, red, ...)',filterParser));
   variableDefinitions.push(defineVariable('exposure','Exposure in seconds',exposureParser));
   variableDefinitions.push(defineVariable('temp','Temperature in C',tempParser));
   variableDefinitions.push(defineVariable('binning','Binning as 1x1, 2x2, ...',binningParser));
   variableDefinitions.push(defineVariable('night','night (experimental)',nightParser));
   var shownVariablesNumber = variableDefinitions.length;
   variableDefinitions.push(defineVariable('filename','Input file name',filenameParser));
   variableDefinitions.push(defineVariable('extension','Input file extension',extensionParser));


   // --- List of all synthethic variables and their comments (2 parallel arrays)
   //     All synthethic variables are currently added to the columns of the file TreeBox
   // TODO Should directly access definitions
   var syntheticVariableNames = [];
   var syntheticVariableComments = [];
   for (var i=0; i<shownVariablesNumber; i++) {
      syntheticVariableNames.push(variableDefinitions[i].name);
      syntheticVariableComments.push(variableDefinitions[i].description);
   }





// Extract the variables to form group names and file names from the file name and the FITS keywords
// They act as 'synthethic' keywords (the purpose is to normalize their representation for ease of use)
// The list of all synthethic keywords must be in the global array syntheticVariableNames in FITSFileManager-gui.js
// Parameters:
//    inputFile: Full path of input file (to extract file anme etc...)
//    imageKeywords: A FitsFileManager imageKeyword object (all FITS keywords of the image)
//    remappedFITSKeywords: Map from logical to real keywords
//    filterConverter: The method to convert filter values
//    typeConverter: The method to convert type values
function makeSynthethicVariables(inputFile, imageKeywords, remappedFITSkeywords, filterConverter, typeConverter) {

   var inputFileName =  File.extractName(inputFile);

   var variables = [];

   for (var i=0; i<variableDefinitions.length; i++) {
      var vd = variableDefinitions[i];
      variables[vd.name] = vd.method(remappedFITSkeywords, typeConverter, filterConverter, imageKeywords, inputFileName);
   }

#ifdef DEBUG
   debug("makeSynthethicVariables: made " + Object.keys(variables).length + " synthetics keys for file " + inputFileName);
#endif

   return variables;

}




// Remove special characters from FITS key values to avoid bizare or illegal file names
// Leading and trailing blank and invalid characters are removed
// Embedded invalid characters are collapsed to one underline.
// An all blank value will return null, considering the keyword as 'missing' when used in templates,
// this helps supporting files created with a program like SIPS that write keywords as OBJECT even
// when it is all blank.
// Parameter
//    value: an unquoted string (without the FITS quotes, embedded quote will be handled as special characters)
// Return:
//    null if it is an all space value, the cleaned up string
function filterFITSValue(value) {
   if (value === null) {
      return null;
   }
   var name = value.trim();
   var result = '';
   var i = 0;
   var hadValidChar = false;
   var mustAddUnderline = false;
   while (i<value.length) {
     var c = name.charAt(i);
     // TODO Make the list of special characters configurable
     if ( ("0" <= c && c <= "9") || ("a" <= c && c <= "z") || ("A" <= c && c <= "Z") || (c === '-') || (c === '.') || (c === '_') ) {
        if (mustAddUnderline) {
           result = result + '_';
           mustAddUnderline = false;
        };
        result = result + c;
        hadValidChar = true;
     } else if (hadValidChar) {
        mustAddUnderline = true;
     }
     i++;
   }
   return (result.length>0) ? result : null;
}


// NOT YET USED
// From mschuster
// ...something strange like l`~!@#$%^&()_-+= {}[];',ïnput.fit maps to l___nput.
function filterViewId(id) {
   var fId = "";
   if (id.length == 0) {
      return "_";
   }
   var c = id.charAt(0);
   if ("0" <= c && c <= "9") {
      fId = fId + "_";
   }
   for (var i = 0; i != id.length; ++i) {
      c = id.charAt(i);
      fId = fId + (
         (("0" <= c && c <= "9") || ("a" <= c && c <= "z") || ("A" <= c && c <= "Z")) ? c : "_"
      );
      if (fId.length > 3 && fId.substring(fId.length - 4, fId.length) == "____") {
         fId = fId.substring(0, fId.length - 1);
      }
   }
   return fId;
}

   // --- public properties and methods ---------------------------------------
   return {
      makeSynthethicVariables: makeSynthethicVariables,
      filterFITSValue: filterFITSValue,
      filterViewId: filterViewId,
      syntheticVariableNames: syntheticVariableNames,
      syntheticVariableComments: syntheticVariableComments,
   }

}) ();



