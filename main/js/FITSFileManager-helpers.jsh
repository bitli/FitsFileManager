// FITSFileManager-help.js

// This file is part of FITSFileManager

#include <pjsr/DataType.jsh>


// ------------------------------------------------------------------------------------------------------------------------
// String utility functions
// ------------------------------------------------------------------------------------------------------------------------



// ------- string functions

function replaceAll (txt, replace, with_this) {
  return txt.replace(new RegExp(replace, 'g'),with_this);
}

var FFM_replaceAmpsRegExp = new RegExp('&', 'g');

function replaceAmps (txt) {
  return txt.replace(FFM_replaceAmpsRegExp,'&amp;');
}



// Remove quotes and trim
function unQuote (s) {
   var t = s.trim();
   if (t.length>0 && t[0]==="'" && t[t.length-1]==="'") {
      return t.substring(1,t.length-1).trim();
   }
   return t;
}


// ------------------------------------------------------------------------------------------------------------------------
// Formatting utility functions
// ------------------------------------------------------------------------------------------------------------------------

// Pad a mumber with leading 0
Number.prototype.pad = function(size){
      var s = String(this);
      while (s.length < size) s = "0" + s;
      return s;
}


// ------------------------------------------------------------------------------------------------------------------------
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




// ------------------------------------------------------------------------------------------------------------------------
// FITS keywords utility functions
// ------------------------------------------------------------------------------------------------------------------------

// Code from FitsKey and/or other examples
// Read the fits keywords of a file, return an array FITSKeyword (value is empty string if there is no value)
function loadFITSKeywords( fitsFilePath )
{
   function searchCommentSeparator( b )
   {
      var inString = false;
      for ( var i = 9; i < 80; ++i )
         switch ( b.at( i ) )
         {
         case 39: // single quote
            inString ^= true;
            break;
         case 47: // slash
            if ( !inString )
               return i;
            break;
         }
      return -1;
   }

   var f = new File;
   f.openForReading( fitsFilePath );

   var keywords = [];
   for ( ;; )
   {
      var rawData = f.read( DataType_ByteArray, 80 );

      var name = rawData.toString( 0, 8 );
      if ( name.toUpperCase() === "END     " ) // end of HDU keyword list?
         break;

      if ( f.isEOF )
         throw new Error( "Unexpected end of file: " + fitsFilePath );

      var value;
      var comment;
      if ( rawData.at( 8 ) === 61 ) // value separator (an equal sign at byte 8) present?
      {
         // This is a valued keyword
         var cmtPos = searchCommentSeparator( rawData ); // find comment separator slash
         if ( cmtPos < 0 ) // no comment separator?
            cmtPos = 80;
         value = rawData.toString( 9, cmtPos-9 ); // value substring
         if ( cmtPos < 80 )
            comment = rawData.toString( cmtPos+1, 80-cmtPos-1 ); // comment substring
         else
            comment = new String;
      }
      else
      {
         // No value in this keyword
         value = new String;
         comment = rawData.toString( 8, 80-8 );
      }

#ifdef DEBUG_FITS
   debug("loadFITSKeywords: - name[" + name + "],["+value+ "],["+comment+"]");
#endif
      // Perform a naive sanity check: a valid FITS file must begin with a SIMPLE=T keyword.
      if ( keywords.length === 0 )
         if ( name !== "SIMPLE  " && value.trim() !== 'T' )
            throw new Error( "File does not seem a valid FITS file: " + fitsFilePath );

      // Add new keyword.
      keywords.push( new FITSKeyword( name.trim(), value.trim(), comment.trim() ) );
   }
   f.close();
   return keywords;
}

// Find a FITS keyword by name in an array of FITSKeywords, return its value or null if undefined
function findKeyWord(fitsKeyWordsArray, name) {
   // keys = array of all FITSKeyword of a file
   // for in all keywords of the file
   for (var k =0; k<fitsKeyWordsArray.length; k++) {
      //debug("kw: '" + keys[k].name + "' '"+ keys[k].value + "'");
      if (fitsKeyWordsArray[k].name === name)  {
         // keyword found in the file >> extract value
#ifdef DEBUG_FITS
         debug("findKeyWord: '" + fitsKeyWordsArray[k].name + "' found '"+ fitsKeyWordsArray[k].value + "'");
#endif
         return (fitsKeyWordsArray[k].value)
      }
   }
#ifdef DEBUG_FITS
   debug("findKeyWord: '" +name + "' not found");
#endif
   return null;
}


// ------------------------------------------------------------------------------------------------------------------------
// Conversion support functions
// ------------------------------------------------------------------------------------------------------------------------
function FFM_Converter() {
   // array of {regexp, replacement}
   this.conversions = [];
}

FFM_Converter.prototype = {
   convert: function(sourceString) {
      for (var i=0; i<this.conversions.length; i++) {
         if ( this.conversions[i].regexp.test(sourceString)) {
            return sourceString.replace( this.conversions[i].regexp,  this.conversions[i].replacement);
         }
      }
      return null;
    }

}


function convertFilter(rawFilterName) {
   if (rawFilterName === null) { return null}
   var filterConversions = [
      [/.*green.*/i, 'green'],
      [/.*red.*/i, 'red'],
      [/.*blue.*/i, 'blue'],
      [/.*clear.*/i, 'clear'],
   ];
   var unquotedName = unQuote(rawFilterName);
   for (var i=0; i<filterConversions.length; i++) {
      var filterName = unquotedName.replace(filterConversions[i][0],filterConversions[i][1]);
      if (filterName !== unquotedName) {
         return filterName;
      }
   }
   // TODO Remove internal spaces etc...
   // Maybe use batch preprocssing: filter.replace( /[^a-zA-Z0-9\+\-_]/g, '_' ).replace( /_+/g, '_' );
   return unquotedName.toLowerCase();
}

function convertType(rawTypeName) {
   if (rawTypeName === null) { return null}
   var typeConversions = [
      [/.*flat.*/i, 'flat'],
      [/.*bias.*/i, 'bias'],
      [/.*offset.*/i, 'bias'],
      [/.*dark.*/i, 'dark'],
      [/.*light.*/i, 'light'],
      [/.*science.*/i, 'light'],
   ];
   var unquotedName = unQuote(rawTypeName);
   for (var i=0; i<typeConversions.length; i++) {
      var typeName = unquotedName.replace(typeConversions[i][0],typeConversions[i][1]);
      if (typeName !== unquotedName) {
         return typeName;
      }
   }
   // TODO Remove internal spaces etc...
   // Maybe use batch preprocssing: filter.replace( /[^a-zA-Z0-9\+\-_]/g, '_' ).replace( /_+/g, '_' );
   return unquotedName.toLowerCase();
}

// ------------------------------------------------------------------------------------------------------------------------
// RegExp utility functions
// ------------------------------------------------------------------------------------------------------------------------

function regExpToString(re) {
   if (re === null) {
      return "";
   } else {
   // Remove lading and trainling slahes
      var reString = re.toString();
      return  reString.substring(1, reString.length-1);
   }
}


// ------------------------------------------------------------------------------------------------------------------------
// Template parsing and execution
// ------------------------------------------------------------------------------------------------------------------------

// Establish module
var ffM_template = (function() {

  var templateRegExp = /&[^&;]+;/g;
  var variableRegExp = /^([^:?]+)(?::([^:?]*))?(?:\?([^:?]*))?/

  // Create a rule that return to parameter literal
  var makeLiteralRule = function(literal){
    // TODO Check that literal does not contains & ( ) ;
    var literalRule = function() {
      return literal;
    }
    literalRule.toString = function() {return "literalRule('" + literal +"')"};
    return literalRule;
  }

  // Create a rule that interpolate a variable expression
  var makeLookupRule = function(expression)  {
    var variableName, onFoundAction, onMissingAction;
     // expression has & and ; already removed

     // Parse the expression of variable:present?missing parts, resulting in the corresponding elements in execResult
    var execResult = expression.match(variableRegExp);
    if (execResult === null) {
       throw "Invalid variable expression '" + expression + "'";
    } else {
        variableName = execResult[1];
    }


    // Create the handler for the case ':present'
    if (execResult[2]) {
        onFoundAction = function(value){
        return execResult[2]; // TODO SHOULD FORMAT  value
      }
      onFoundAction.toString = function(){return "formatValueAs('"+execResult[2]+"')"};
    } else {
      onFoundAction = function(value){
        return value;
      }
      onFoundAction.toString = function(){return "copyValue()"};
    }

    // Create the handler for the case '?missing'
    if (execResult[3]==='') {
      onMissingAction = function(){
        return '';   // Optional value
      }
      onMissingAction.toString = function(){return "copyLiteral('')"};
    } else if (execResult[3]) {
      onMissingAction = function(){
        return execResult[3]; // There should be no format
      }
      onMissingAction.toString = function(){return "copyLiteral('"+execResult[2]+"')"};
    } else {
      onMissingAction = function(){
        throw "No value for the variable";
      }
      onMissingAction.toString = function(){return "reject()"};
    }

    // The lookup variable rule itself, that will use the handlers above
    var lookUpRule = function(table) {
      var value = table[variableName];
      if (value) {
        onFoundAction(value);
      } else {
        onMissingAction(value);
      }
    }
    lookUpRule.toString = function() { return "lookUpRule('" + variableName + "':[onFound:" + onFoundAction + "]"+ ":[onMissing:" + onMissingAction + "])"; }
    return lookUpRule;
  }


  // Public interface
  return {
    analyzeTemplate: function(template) {
#ifdef DEBUG_TEMPLATE
      debug("analyzeTemplate:'" + template + "'");
#endif
      // The replacing handler global variables
      var rules = []; // Invalid if error is not empty
      var iNext = 0; // next character that will be examined
      var replaceHandler = function(match, offset, string) {
        //print ("  rh: ", match, offset, string);
        if (offset>iNext) {
          rules.push(makeLiteralRule(string.substring(iNext, offset)));
        }
        rules.push(makeLookupRule(match.substring(1,match.length-1)));
        iNext = offset + match.length;
        return ''; // replace by nothing, ignored anyhow
      }
      // Each match will create the rule for the preceding literal text and the current match
      // Use 'replace' as it provides the need match information, if the replacement is not really used.
      template.replace(templateRegExp, replaceHandler);
      // If required add literal rule for trailing literal text
      if (template.length>iNext) {
          rules.push(makeLiteralRule(template.substring(iNext)));
      }
      var templateRuleSet = {
         toString: function() {return rules.toString();}
      };

      return templateRuleSet;
    } // analyzeTemplate

  } // ffM_template object
})();







// Parsing the keywords in the targetFileNameTemplate (1 characters will be removed at
// head (&) and tail (;), this is hard coded and must be modified if required
//var variableRegExp = /&[a-zA-Z0-9]+;/g;
var variableRegExp = /&[^&]+;/g;




// --- Variable handling
var shownSyntheticVariables = ['type','filter','exposure','temp','binning'];
var shownSyntheticComments = ['Type of image (flat, bias, ...)',
   'Filter (clear, red, ...)',
   'Exposure in seconds',
   'Temperature in C',
   'Binning as 1x1, 2x2, ...'];


#ifdef DO_NOT_COMPILE

// -- Support to analyze template and extract variables info
function extractVariables(template) {

   var variables = [];
   // We could also use exec, but it seems even more complex
   // Method to handle replacement of variables in target file name template
   var extractVariable = function(matchedSubstring, index, originalString) {
      debug("*** matchedSubstrings " + matchedSubstring);
      var varName = matchedSubstring.substring(1,matchedSubstring.length-1);
      variables.push(varName);
      return matchedSubstring;
   };
   template.replace(variableRegExp,extractVariable);


   debug("*** variables " + variables);

   return variables;
}

function analyzeVariable(variable) {
   var parts = {};
   var extractVariableParts = function(matchedSubstring, index, originalString) {
      debug("*** parts " + matchedSubstring);
      if (matchedSubstring[0]===':') {
         parts.trueFormat = matchedSubstring.substring(1);
      } else if (matchedSubstring[0]==='?') {
         parts.falseFormat = matchedSubstring.substring(1);
      } else {
         parts.name = matchedSubstring.trim();
      }
      return matchedSubstring;
   };
   variable.replace(/(^|[:?])([^:?]+)/g,extractVariableParts);
   debug("*** part " + parts.name);

   return parts;
}

function analyzeVariables(template) {
   var result = [];
   var variables = extractVariables(template);
   debug("*** analyzeVariables variables " + variables);
   for (var i = 0; i<variables.length; i++) {
      result.push(analyzeVariable(variables[i]));
   }
   debug("*** analyzeVariables result " + result);
   return result;
}

         analyzeVariables(this.text).forEach(
            function(p) {
               debug("*** analyzeVariables each " + p.name + " " + p.trueFormat + " "  + p.falseFormat);
            }
         );

#endif

// Extract the variables to form group names and file names from the file name, FITS keywords
function makeSynthethicVariables(inputFile, keys) {

   var inputFileName =  File.extractName(inputFile);


   var variables = [];

   //   &binning     Binning from XBINNING and YBINNING as integers, like 2x2.
   var xBinning = parseInt(findKeyWord(keys,'XBINNING'));
   var yBinning = parseInt(findKeyWord(keys,'YBINNING'));
   if (isNaN(xBinning) || isNaN(yBinning)) {
      variables['binning'] = null;
   } else {
       variables['binning'] = xBinning.toFixed(0)+"x"+yBinning.toFixed(0);
   }

   // 'count' and 'rank' depends on the order, will be recalculated when files are processed,
   // here for documentation purpose
   variables['count'] = 0 .pad(FFM_COUNT_PAD);
   variables['rank'] = 0 .pad(FFM_COUNT_PAD);

   //   &exposure;   The exposure from EXPOSURE, as an integer (assume seconds)
   var exposure = findKeyWord(keys,'EXPOSURE');
   var exposureF =  parseFloat(exposure);
   if (isNaN(exposureF)) {
      variables['exposure'] = null;
   } else {
      variables['exposure'] = exposureF.toFixed(0);
   }

   //   &extension;   The extension of the source file (with the dot)
   variables['extension'] = File.extractExtension(inputFile);

   //   &filename;   The file name part of the source file
   variables['filename'] = inputFileName;

   //   &filter:     The filter name from FILTER as lower case trimmed normalized name.
   var filter = findKeyWord(keys,'FILTER');
   variables['filter'] = convertFilter(filter);

   //   &temp;       The SET-TEMP temperature in C as an integer
   var temp = findKeyWord(keys,'SET-TEMP');
   var tempF = parseFloat(temp);
   if (isNaN(tempF)) {
      variables['temp'] = null;
   } else {
      variables['temp'] = tempF.toFixed(0);
   }

   //   &type:       The IMAGETYP normalized to 'flat', 'bias', 'dark', 'light'
   var imageType = findKeyWord(keys,'IMAGETYP');
   variables['type'] = convertType(imageType);

   //   &FITSKW;     (NOT IMPLEMENTED)


   return variables;

}

Console.writeln("aa");


