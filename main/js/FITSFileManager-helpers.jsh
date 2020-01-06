// FITSFileManager-helper.js

// This file is part of FITSFileManager, see copyright in FITSFileManager.js

#include <pjsr/DataType.jsh>

#include <pjsr/UndoFlag.jsh>


//=========================================================================================================================
// String and formatting utility functions
// ------------------------------------------------------------------------------------------------------------------------



// --- RegExp utility functions


// Return the RE as if it would be in source (with / and flags) or null for a null parameter
function regExpToString(re) {
   if (re === null) {
      return "";
   } else {
      var reStr = re.toSource();
      // This should not occur, trying to find a rare error
//      if (typeof reStr !=='string') {
//         throw "PROGRAMMING ERROR - Unexpected result of regexp.toSource(), got a '" + typeof reStr + "', expected a 'string'"
//      }
      return reStr;
   }
}


// Parse a well formed RegExp string (WITH the '/' and flags), throw exception in case of error,
// return a RegExp object or null for a null parameter.
function regExpFromString(reString) {
   if (reString === null) {
      return null;
   }
   if (typeof reString !=='string') {
      throw "PROGRAMMING ERROR - Invalid regexp string, got a '" + typeof reString + "', expected a 'string'"
   }

   if (reString.length===0) {
      return null;
   } else if (reString.length<2) {
      throw "Invalid regular expression - '" + reString + "' is emtpy or too small, need at least two / (slashes)"
   } else {
      var reChar = reString.charCodeAt(0);
      if (reChar !== 47) {
         throw "Invalid regular expression - '" + reString + "' does not start with a / (slash)"
      }
      var lastSlash = reString.lastIndexOf("/");
      if (lastSlash<=0 || lastSlash>reString.length) {
         throw "Invalid regular expression - '" + reString + "' has no terminating / (slash)"
      }
      var rePart = reString.substring(1,lastSlash);
      var flagsPart = reString.substring(lastSlash+1);

      return new RegExp(rePart, flagsPart);
   }
}

// Parse a regular expression typed by the user. If it does not start with /,
// add the surrounding slashed and assumes that it has no flag.
// If staring with a /, assume that it is a valid regexp in string format, with
// terminating slash and possible options.
// Return it as a RegExp object (return null if null was received), throw an exception if regexp is invalid
function regExpFromUserString(reString) {
   if (reString === null) {
      return null;
   }
   if (typeof reString !=='string') {
      throw "PROGRAMMING ERROR - Invalid regexp string";
   }

   if (reString.length>=2 && reString.charCodeAt(0) !== 47) {
      reString = "/" + reString + "/";
   }
   return regExpFromString(reString);

}

function ensureIsString(value) {
   if (typeof value !== 'string') {
      throw "INTERNAL ERROR - expected a 'string', got a '" + typeof value + "' for '" + value.toString() +"'";
   }
   return value;
}


// Create a unique name with the same prefix than a list of existing similar name
function createUniqueName(baseName, existingNames) {
   var reSuffix = /^[^_]+_(\d+)/;
   var reNoSuffix = /_\d+$/;
   var largestSuffix = 0;
   var baseWithoutSuffix = baseName.replace(reNoSuffix,'');
   var reSuffix = /^[^_]+_(\d+)/;
   for (var i=0; i<existingNames.length;i++) {
      var existingName = existingNames[i];
      var existingNameWithoutSuffix = existingName.replace(reNoSuffix,'');
      if (existingNameWithoutSuffix === baseWithoutSuffix) {
         var suffixMatch = existingName.match(reSuffix);
         if (suffixMatch) {
            var n = parseInt(suffixMatch[1]);
            if (n>largestSuffix) {
               largestSuffix = n;
            }
         }
      }
   }
   return baseWithoutSuffix +"_" +(largestSuffix+1)
}

// --- Date formatting support
// Calculate the julian day (truncated) from a javascript date
function julianDay(date) {
    return Math.floor((date.getTime() / 86400000) + 2440587.5);
}

/*
Format a timestamp (a javascript Date) according to the following rules:

The format string consists of zero or more conversion specifications and 
ordinary characters.
A conversion specification consists of a % character and a terminating 
conversion character that determines the conversion specification's behaviour. 
All ordinary characters are copied unchanged into the array. 
If copying takes place between objects that overlap, the behaviour is undefined. 
Each conversion specification is replaced by appropriate characters as described 
in the following list. 

The appropriate characters are determined by the 
program's locale and by the values contained in the structure pointed to by timptr.

Local timezone information is used as though strftime() called tzset().

%d
    is replaced by the day of the month as a decimal number [01,31]. 
%H
    is replaced by the hour (24-hour clock) as a decimal number [00,23]. 
%j  
    is replaced by the julian day 
%L  
    is replaced by the milliseconds padded to 3 digits
%m
    is replaced by the month as a decimal number [01,12]. 
%M
    is replaced by the minute as a decimal number [00,59]. 
%S
    is replaced by the second as a decimal number [00,61]. 
%y
    is replaced by the year without century as a decimal number [00,99]. 
%Y
    is replaced by the year with century as a decimal number. 
%%
    is replaced by %. 

If a conversion specification does not correspond to any of the above, the format character itself is output.

*/

function formatDate(format, timestamp) {
    // Accept % followed by any char or % (to allow to escape %)
    var formatChr = /%([a-zA-Z%])/gi;
    
    var formats = {
       'd': function() {
            return _pad(timestamp.getDate(),2);
        },
       'H': function() {
            return _pad(timestamp.getHours(),2);
        },
       'j': function() {
            return julianDay(timestamp);
        },
       'L': function() {
            return _pad(timestamp.getMilliseconds (),3);
        },
       'm': function() {
            return _pad(timestamp.getMonth()+1,2);
        },
       'M': function() {
            return _pad(timestamp.getMinutes(),2);
        },
       'S': function() {
            return _pad(timestamp.getSeconds(),2);
        },
       'y': function() {
            return (timestamp.getFullYear()).toString().substring(2);
        },
       'Y': function() {
            return timestamp.getFullYear();
        },
    }
    
    var _pad = function(n, c) {
      n = String(n);
      while (n.length < c) {
         n = '0' + n;
      }
      return n;
   };

    function processFormatChar(fullIgnored, chr) {
        //console.log('FMT ' + chr );
        return formats[chr] ? formats[chr]() : chr;
    }
    
    return format.replace(formatChr, processFormatChar);
}

// Parse date or date time string, see FITS standard
// Date format is YYYY-MM-DDThh:mm:ss[.sss. . . ]
// optionally restricted to the date part (without the letter T). 
// They year must be four digits.
// The letter T is not checked in parsing to accept slightly erroneous data
// The subsecond part may be any (reasonable) precision
function parseFITSDateTime(dateTimeString) {
   // The first 3 fields must alwaqys be present, the time part is option, in it the fraction is itself optional.
   var matchDateTime = /(\d\d\d\d)-(\d\d)-(\d\d)(?:.(\d\d):(\d\d):(\d\d)(?:.(\d+))?)?/;
   var res = matchDateTime.exec(dateTimeString);
    if (res === null) {
      throw "Cannot parse date/time '" + dateTimeString + "', format YYYY-MM-DDThh:mm:ss[.sss. . . ]";
   }
   // Allow missing time component
   var h=res[4], m=res[5], s=res[6];
   if (res[4]===undefined && res[5]===undefined && res[6]===undefined) {
      h=0; m=0; s=0;
   }

   // Convert optional fraction part to milliseconds
   var ms = 0;
   if (res[7] !== undefined) {
      var mspart = parseInt(res[7]);
      if (Number.isNaN(ms)) {
         throw "Cannot parse date/time '" + dateTimeString + "', bad ms field, format YYYY-MM-DDThh:mm:ss[.sss. . . ]";
      }
      ms = Math.floor(mspart / Math.pow(10, res[7].length) * 1000.0);
      // Console.writeln("*** parseFITSDateTime mspart " + ms + " L " + res[7].length + " P " + Math.pow(10, res[7].length) + " ms " + ms);
  }
   return new Date(res[1],res[2],res[3],h,m,s, ms);
}

 

//=========================================================================================================================
// Object data support
// ------------------------------------------------------------------------------------------------------------------------
// Deep copy an object (with object, array, basic types and RegExp only, Date and RegExp are in general not used
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
      result = regExpFromString(regExpToString(object));
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

// as per http://pixinsight.com/forum/index.php?topic=8155.msg53446#msg53446
function getDirectoryWithDriveLetter( a_path )
{
   let path = File.windowsPathToUnix( a_path );
   return File.extractDrive( path ) + File.extractDirectory( path );
}


// Mind parameter order TODO : Change
function copyFile( sourceFilePath, targetFilePath ) {

   File.copyFile( targetFilePath, sourceFilePath);

}


// Load a file and save it with the new name,
// The file name is added to the ORIGFILE if not already present
// (this is not a standard FITS keyword, another possibility is to use
// the FILENAME FILEXT keywords, but they are not standard either)
// An HISTORY record is also added
#define FFM_FITS_HISTORY_LEADER "PI FITSFileManager from "

function updateKeywords(keywords, sourceFilePath, targetFilePath) {
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

// FOR TESTS #define ADHOC_KW
#ifdef ADHOC_KW
   // ADHOC keywords operations
   Console.writeln("ADHOC PROCESSING OF KEYWORDS");
   ensureKeyword(keywords,  new FITSKeyword( "IMAGETYP", "Bias Frame", "Bias, dark, flat or light"));
   ensureKeyword(keywords,  new FITSKeyword( "FILTER", "halpha", "Optical filter used to take the image"));
   ensureKeyword(keywords,  new FITSKeyword( "XBINNING", "1", "Binning factor in X"));
   ensureKeyword(keywords,  new FITSKeyword( "YBINNING", "1", "Binning factor in Y"));
#endif

   return keywords;

}

function inputHints ()
{
   // Input format hints:
   return "signed-is-physical "; //  + (this.upBottomFITS ? "up-bottom" : "bottom-up");
};

function outputHints ()
{
   // Output format hints:
   // * XISF: properties fits-keywords no-compress-data block-alignment 4096 max-inline-block-size 3072 no-embedded-data no-resolution
   // * FITS: up-bottom|bottom-up
   return "" ; // (this.upBottomFITS ? "up-bottom" : "bottom-up");
};


#define IO_WITH_HINTS 1

function loadSaveFile( sourceFilePath, targetFilePath ) {

#ifdef IO_WITH_HINTS
   var extin = File.extractExtension( sourceFilePath );
   var Fin = new FileFormat( extin, true/*toRead*/, false/*toWrite*/ );
   if ( Fin.isNull )
      throw new Error( "No installed file format can read \'" + extin + "\' files." ); // shouldn't happen

   var fin = new FileFormatInstance( Fin );
   if ( fin.isNull )
      throw new Error( "Unable to instantiate file format: " + Fin.name );

   var d = fin.open( sourceFilePath, inputHints() );
   if ( d.length < 1 )
      throw new Error( "Unable to open file: " + sourceFilePath );
   if ( d.length > 1 )
      throw "File '" + sourceFilePath + "' contains " + d.length + " images, this is not supported by this script.";

   var imageWindow = new ImageWindow( 1, 1, 1,/*numberOfChannels*/ 32,/*bitsPerSample*/ true/*floatSample*/ );

   var view = imageWindow.mainView;
   // Protect to be ready if driver handles exceptions
   let beingProcessed = null;
   try {
      view.beginProcess( UndoFlag_NoSwapFile );
      beingProcessed = view;
      if ( !fin.readImage( view.image ) )
         throw new Error( "Unable to read file: " + sourceFilePath );
      imageWindow.keywords = fin.keywords;
      view.endProcess();
      beingProcessed = null;
   } finally {
      if ( beingProcessed )
      {
         beingProcessed.endProcess();
         beingProcessed = null;
      }
   }


   fin.close();

   var keywords = updateKeywords(imageWindow.keywords, sourceFilePath, targetFilePath);

   
   var ext = File.extractExtension( targetFilePath );
   var Fout = new FileFormat( ext, false/*toRead*/, true/*toWrite*/ );
   if ( Fout.isNull )
      throw new Error( "No installed file format can write " + ext + " files." ); // shouldn't happen

   var fout = new FileFormatInstance( Fout );
   if ( fout.isNull )
      throw new Error( "Unable to instantiate file format: " + F.name );

   var hints = outputHints();
   if ( !fout.create( targetFilePath, hints ) )
      throw new Error( "Error creating output file: " + targetFilePath );

   // var d = new ImageDescription;
   // d.bitsPerSample = 32;
   // d.ieeefpSampleFormat = true;
   // if ( !f.setOptions( d ) )
   //    throw new Error( "Unable to set output file options: " + targetFilePath );

   fout.keywords = keywords;
   //f.keywords = imageWindow.keywords;

   if ( !fout.writeImage( imageWindow.mainView.image ) )
      throw new Error( "Error writing output file: " + targetFilePath );

   fout.close();

   imageWindow.forceClose();;

#else
   // More than one image may be loaded - this is an issue
   var images = ImageWindow.open( sourceFilePath,"FITSFileManagerLoadSaveAs_Temp", true );
   if (images.length !== 1) {
      throw "File '" + sourceFilePath + "' contains " + images.length + " images, this is not supported by this script.";
   }
   var image = images[0];
   var keywords = image.keywords;

   keywords = updateKeywords(keywords, sourceFilePath, targetFilePath);

   image.keywords = keywords;

   image.saveAs(targetFilePath,  false, false, false, false);

   image.forceClose();
#endif
}

// set the keyword to the provided value, override if present, create if not
function setKeyword(keywords, kw) {
   for (var i=0; i<keywords.length; i++) {
      if (keywords[i].name === kw.name) {
         keywords[i].value = kw[i].value;
         return;
      }
   }
   Console.writeln("Adding " + kw.name + ": '" + kw.value + "'");
   keywords.push( kw );
}

// Ensure that the keyword, if present, has the specified value (log warning otherwise), add it if needed
function ensureKeyword(keywords, kw) {
   for (var i=0; i<keywords.length; i++) {
      if (keywords[i].name === kw.name) {
         if (keywords[i].value != kw.value) {
            Console.writeln("WARNING - Keyword " + kw.name + " does not have the expected value " + kw.value + " but " + keywords[i].value + ", leave as is." );
         }
         return;
      }
   }
   Console.writeln("Adding " + kw.name + ": '" + kw.value + "'");
   keywords.push( kw );
}

// Add keyword, log warning if already present
function addNewKeyword(keywords, kw) {
   for (var i=0; i<kw.length; i++) {
      if (keywords[i].name === kw.name) {
         Console.writeln("WARNING - Keyword " + kw.name + " already present");
         return;
      }
   }
   Console.writeln("Adding " + kw.name + ": '" + kw.value + "'");
   keywords.push( kw );
}

// Add keyword at end even if already present (for COMMENT and HISTORY)
function appendKeyword(keywords, kw) {
   Console.writeln("Adding " + kw.name + ": '" + kw.value + "'");
   keywords.push( kw );
}

function removeKeywords(keywords, kw) {
   for (var i =kw.length-1; i>=0; i--) {
      if (keywords[i].name === kw.name) {
         Console.writeln("Removing " + kw.name);
         keywords.splice(i,1);
         return;
      }
   }
}




//=========================================================================================================================
// Conversion support
// ------------------------------------------------------------------------------------------------------------------------
var ffM_LookupConverter = (function() {

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

   // Create a regular expression lookup converter
   // Parameters:
   //      conversionTable: Array  of {regexp:, replacement:}, the regexp must be formatted as a string
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
            //Console.writeln("DEBUG: makeLookupConverter - conversionEntry - " + i + " regexp " + conversionEntry.regexp + " replacement " + conversionEntry.replacement);
            try {
               var conversionRegExp = regExpFromString(conversionEntry.regexp);
            } catch (ex) {
               throw("ERROR: Invalid RegExp when loading settings or configuration, likely corrupted file or settings");
               // Difficult to replace regexp here by a default
            }
            var conversionResultTemplate = conversionEntry.replacement;
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
                           // TODO Generate error in a more friendly way
                           return "BACKREFERENCETOOLARGE"; // Cannot replace, index too large
                        } else {
                           // Cleanup the returned value to avoid special characters in file names
                            var cleanedBackReference =  ffM_variables.filterFITSValue(matchedGroups[matchIndex]);
                           // If the match is empty after cleanup, we get a null. In this case
                           // use an empty string to avoid error in the caller.
                           // TODO Return as error like BACKREFERENCETOOLARGE above
                           return (cleanedBackReference === null) ? '' : cleanedBackReference; 
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
}) ();




// TODO Review location of module

// Global object containing the resolver definitions
// These definitions are complemented by the modules when they are loaded
var ffM_Resolver = (function(){

   var i;

   // Describe the resolver types
   // The 'initial' property value will be deep copied to the parameter property when a new definition is created
   // The 'control' property will be populatedby the GUI when they are created
   // For regexp, the regexp must be a string (as only strings are saved/restored)
   var resolvers = [
      {name: 'RegExpList', description: 'Type of image (flat, bias, ...)',
            initial:{key: '?', reChecks: [{regexp: "/.*/", replacement: '?'}]},  control: null, parserFactory:null},
      {name: 'Text', description: 'Text of FITS keyword value',
            initial:{key: '?', format: '%ls', case: 'NONE'}, control: null, parserFactory:null},
      {name: 'Integer', description: 'Integer value',
            initial:{key: '?', abs: true, format:'%4.4d'}, control: null, parserFactory:null},
      {name: 'IntegerPair', description: 'Pair of integers (binning)',
            initial:{key1: '?', key2: '?', format:'%dx%d'}, control: null, parserFactory:null},
      {name: 'Constant', description: 'Constant value',
            initial:{value: ''}, control: null, parserFactory:null},
      {name: 'DateTime', description: 'Date or date+time value',
            initial:{key: '?', format: '%Y%m%d'}, control: null, parserFactory:null},
      {name: 'FileName', description: 'Source file name',
            initial:{}, control: null, parserFactory:null},
      {name: 'FileExtension', description: 'Source file extension',
            initial:{}, control: null, parserFactory:null},
      {name: 'Night', description: 'Night (experimental)',
            initial:{keyLongObs: 'LONG-OBS', keyJD: 'JD'}, control: null, parserFactory:null}
   ];

   var resolverByName = function(name) {
      for (var i=0; i<resolvers.length; i++) {
         if (resolvers[i].name === name) return resolvers[i];
      }
      return null;
   }
   var resolverNames = [];
   for ( i=0; i<resolvers.length; i++) {
      resolverNames.push(resolvers[i].name);
   }
   return {
      resolverNames: resolverNames,
      resolverByName: resolverByName,
      resolvers: resolvers,
   }
}) ();



// ====================================================================================================================
// Template parsing and execution module
// ====================================================================================================================

// Create template support in a 'module' like object
var ffM_template = (function() {

  // Something like &stuf;
  var templateRegExp = /&[^&;]+;/g;
  // Extract parts of &var:truepart?falsepart;
  var variableRegExp = /^([^:?]+)(?::([^:?]*))?(?:\?([^:?]*))?/

  var testInvalidLiteralRegExp = /[&\(\);<>=!%*]/;

  // Create a rule that return the parameter literal verbatim
  var makeLiteralRule = function(templateErrors,literal){
    // TODO Check that literal does not contains & ( ) ; < > = ! ( ) and % unless formatting is implemented)
    if (testInvalidLiteralRegExp.test(literal)) {
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

   // -- Install all the parsers in the configuration being copied (the configuration is a pure data object
   // and does not contains the code like the parser, only the name of the parser). It 
   // must be called once only when copying a configuration)
   var installParsers = function(configuration) {
#ifdef DEBUG
      debug("ffM_variables.installParsers: for",configuration.name);
#endif
      var variableList = configuration.variableList;
      for (var i=0; i<variableList.length; i++) {
         try {
            var variableDefinition = variableList[i];
            var resolverImpl = ffM_Resolver.resolverByName(variableDefinition.resolver);
            var parameters = variableDefinition.parameters[variableDefinition.resolver];
            variableDefinition.parser = resolverImpl.parserFactory(configuration, parameters);
         } catch (ex) {
            throw "Error encountered when compiling variable " + (i+1) + 
                " of configuration '" + configuration.name + "' " + ex;
         }
      }
#ifdef DEBUG
      debug("ffM_variables.installParsers: nmb parsers installed:",variableList.length);
#endif
   }

   // All parsing rules have the own configuration parameters object as argument
   // the fits values and the variables parsed to far

   ffM_Resolver.resolverByName('Integer').parserFactory = function(configuration, parameters){
#ifdef DEBUG
      debug("resolver factory Integer for :",Log.pp(parameters));
#endif
     return (
         function parseInteger(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            var valueString = imageKeywords.getValue(ruleParameters.key);
            // Accept float also
            var valueF =  parseFloat(valueString);
            if (isNaN(valueF)) {
               return null;
            } else {
               if (ruleParameters.abs) { valueF = Math.abs(valueF);}
               // Force the value to be an Int32 as far as Math.format is concerned
               var roundedValueF = Math.round(valueF) | 0;
               try {
                  return format(ruleParameters.format, Math.round( valueF));
               } catch (e) {
                  mutableErrorList.push("Error formatting '" + ruleParameters.format +
                  "' with parameters '" + roundedValueF + "' + for key " + ruleParameters.key);
                  return null;
               }
            }
         }
      )
   }

   ffM_Resolver.resolverByName('Text').parserFactory = function(configuration, parameters){
#ifdef DEBUG
      debug("resolver factory Text for :",Log.pp(parameters));
#endif
     return (
         function parseText(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            var valueString = imageKeywords.getValue(ruleParameters.key);
            if (valueString === null) {
               return null;
            } else {
               var cleanedValue = filterFITSValue(valueString);
               if (cleanedValue === null) {
                  mutableErrorList.push("Value of key " + ruleParameters.key +
                  " is empty afer trimming/removing illegal characters, was '" + valueString + "'");
                  return null;
               }
               if (ruleParameters.case === 'UP') {
                  cleanedValue = cleanedValue.toUpperCase();
               } else if (ruleParameters.case === 'DOWN') {
                  cleanedValue = cleanedValue.toLowerCase();
               }
               try {
                  return format(ruleParameters.format, cleanedValue);
               } catch (e) {
                  mutableErrorList.push("Error formatting '" + ruleParameters.format +
                  "' with parameters '" + cleanedValue + "' for key " + ruleParameters.key);
                  return null;
               }
            }
         }
      )
   }

   ffM_Resolver.resolverByName('IntegerPair').parserFactory = function(configuration, parameters){
#ifdef DEBUG
      debug("resolver factory IntegerPair for :",Log.pp(parameters));
#endif
     return (
         function parseIntegerPair(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            var valueString1 = imageKeywords.getValue(ruleParameters.key1);
            var valueString2 = imageKeywords.getValue(ruleParameters.key2);
            // Accept float also
            var valueF1 =  parseFloat(valueString1);
            var valueF2 =  parseFloat(valueString2);
            if (isNaN(valueF1) || isNaN(valueF2)) {
               return null;
            } else {
               try {
                  // Force the value to be an Int32 as far as Math.format is concerned
                  var roundedValueF1 =  Math.round( valueF1) | 0;
                  var roundedValueF2 =  Math.round( valueF2) | 0;
                  return format(ruleParameters.format, roundedValueF1, roundedValueF2);
               } catch (e) {
                  mutableErrorList.push("Error formatting '" + ruleParameters.format +
                  "' with parameters '" + roundedValueF1 + "', '" + roundedValueF1 + "' for keys " + ruleParameters.key1 + "/" + ruleParameters.key2);
                  return null;
               }
            }
         }
      )
   }

   ffM_Resolver.resolverByName('Constant').parserFactory = function(configuration, parameters){
#ifdef DEBUG
      debug("resolver factory Constant for :",Log.pp(parameters));
#endif
     return (
         function parseConstant(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            return ruleParameters.value;
         }
      )
   }

   ffM_Resolver.resolverByName('DateTime').parserFactory = function(configuration, parameters){
#ifdef DEBUG
      debug("resolver factory DateTime for :",Log.pp(parameters));
#endif
     return (
         function parseDateTimeFormat(ruleParameters,imageKeywords,imageVariables,inputFile, mutableErrorList) {
            var formattedDate, dateTime;
            var dateString = imageKeywords.getValue(ruleParameters.key);
            if (dateString === null) {
               return null;
            } else {
               
              try {
                dateTime = parseFITSDateTime(dateString);
               } catch (e) {
                  mutableErrorList.push(e + " when parsing " + ruleParameters.key);
                  return null;
               }
 
               try {
                  formattedDate = formatDate(ruleParameters.format, dateTime);
               } catch (e) {
                    mutableErrorList.push("Error formatting date/time with format '" + ruleParameters.format + 
                     " for key " + ruleParameters.key);
                  return null;
               }
                // Make sure we do not generate baroque file names (should probably done at file name generation only)
               var cleanedValue = filterFITSValue(formattedDate);
               if (cleanedValue === null) {
                  mutableErrorList.push("Value of formatted date of " + ruleParameters.key +
                     " is empty afer trimming/removing illegal characters, was '" + formattedDate + "'");
                  return null;
               }

               return cleanedValue;
            }

         }
      )
   }


   ffM_Resolver.resolverByName('RegExpList').parserFactory = function(configuration, parameters) {
      // Prepare lookup converter
      var lookupConverter = ffM_LookupConverter.makeLookupConverter(parameters.reChecks);
#ifdef DEBUG
      debug("resolver factory RegExpList for :",Log.pp(parameters));
#endif
      return (
         function parseRegExpList(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            var value = imageKeywords.getUnquotedValue(ruleParameters.key);
            if (value === null) return null;
            // Console.writeln("DEBUG value is '" + value + "'");
            return lookupConverter.convert(value);
         }
      )
   }

   ffM_Resolver.resolverByName('FileName').parserFactory = function(configuration, parameters){
#ifdef DEBUG
     debug("resolver factory FileName for :",Log.pp(parameters));
#endif
     return (
         function parseFileName(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            return  File.extractName(inputFile);
         }
      )
   }

   ffM_Resolver.resolverByName('FileExtension').parserFactory = function(configuration, parameters){
#ifdef DEBUG
      debug("resolver factory FileExtension for :",Log.pp(parameters));
#endif
     return (
         function parseFileExtension(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            return  File.extractExtension(inputFile);
         }
      )
   }

   ffM_Resolver.resolverByName('Night').parserFactory = function(configuration, parameters){
#ifdef DEBUG
      debug("resolver factory Night for :",Log.pp(parameters));
#endif
     return (
         function parseNight(ruleParameters,imageKeywords,imageVariables,inputFile,mutableErrorList) {
            var longObs = imageKeywords.getValue(ruleParameters.keyLongObs); // East in degree
            // longObs = -110;
            // TODO Support default longObs
            var jd = imageKeywords.getValue(ruleParameters.keyJD);
            if (longObs && jd) {
               var jdLocal = Number(jd) + (Number(longObs) / 360.0) ;
               var nightText = (Math.floor(jdLocal) % 1000).toString();
               return nightText;
            } else {
               return null;
            }
         }
      )
   }





   // Extract the variables to form group names and file names from the file name and the FITS keywords
   // They act as 'synthethic' keywords (the purpose is to normalize their representation for ease of use)
   // Parameters:
   //    inputFile: Full path of input file (to extract file anme etc...)
   //    imageKeywords: A FitsFileManager imageKeyword object (all FITS keywords of the image)
   //    variableList: The variable definitions of the current rule
   //    mutableErrorList: Array of error messages
   function makeSyntheticVariables(inputFile, imageKeywords, variableList, mutableErrorList) {

      var inputFileName =  File.extractName(inputFile);

      var variables = {};

      for (var i=0; i<variableList.length; i++) {
         var variableDefinition = variableList[i];
         var parameters = variableDefinition.parameters[variableDefinition.resolver];
         variables[variableDefinition.name] = variableDefinition.parser(parameters,imageKeywords, variables, inputFile, mutableErrorList);
      }

#ifdef DEBUG
      debug("makeSyntheticVariables: made " + Object.keys(variables).length + " synthetics keys for file " + inputFileName);
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
      makeSyntheticVariables: makeSyntheticVariables,
      filterFITSValue: filterFITSValue,
      filterViewId: filterViewId,
      installParsers: installParsers,
   }

}) ();







