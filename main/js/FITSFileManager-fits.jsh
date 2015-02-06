// FITSFileManager-fits.jsh

// This file is part of FITSFileManager, see copyright in FITSFileManager.js

#include <pjsr/DataType.jsh>


// ------------------------------------------------------------------------------------------------------------------------
// Read the FITS keywords of an image file, supports the HIERARCH convention
// ------------------------------------------------------------------------------------------------------------------------
// Input:  The opened file
// Return: An array FITSKeyword, identical to what would be returned by ImageWindow.open().keywords.
//         at least for correct keywords (errors and border line cases may be handled differently)
// Throws: Error if bad format
// The value of a FITSKeyword value is the empty string if the keyword had no value
// Code adapted from FitsKey and other scripts
// TODO - Support CONTINUE ?
// TODO - Support blank comment as a continuation of a previous coomment ?
// ------------------------------------------------------------------------------------------------------------------------
var ffM_loadFITSKeywordsList =  function loadFITSKeywordsList(fitsFilePath, mutableErrorList) {
   var f = new File;
   f.openForReading( fitsFilePath );
   try {
      return local_loadFITSKeywordsList(f, mutableErrorList);
   } finally {
      f.close();
   }
}


// TODO Move to module
var local_loadFITSKeywordsList =  function loadFITSKeywordsList(f, mutableErrorList) {


   function searchCommentSeparator( b ) {
      var inString = false;
      for ( var i = 10; i < 80; ++i )
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

   // in HIERARCH the = sign is after the real keyword name
   function searchHierarchValueIndicator( b ) {
      for ( var i = 9; i < 80; ++i )
         switch ( b.at( i ) )
         {
         case 39: // single quote, = cannot be later
            return -1;
         case 47: // slash, cannot be later
            return -1;
         case 61: // =, may be value indicator after all
            return i;
         }
      return -1;
   }

#ifdef DEBUG_SHOW_FITS
      debug("ffM_loadFITSKeywordsList: loading '" + f.path + "'");
#endif

   var keywords = [];
   for ( ;; ) {
      var rawData = f.read( DataType_ByteArray, 80 );
#ifdef DEBUG_FITS
      debug("ffM_loadFITSKeywordsList: line - '" + rawData.toString() + "'");
#endif

      var name = rawData.toString( 0, 8 );
      if ( name.toUpperCase() === "END     " ) { // end of HDU keyword list?
         break;
      }
      if ( f.isEOF ) {
         throw new Error( "Unexpected end of file reading FITS keywords, file: " + f.path );
      }

      var value = "";
      var comment = "";
      var hasValue = false;

      // value separator (an equal sign at byte 8) present?
      if ( rawData.at( 8 ) === 61 ) {
         // This is a valued keyword
         hasValue = true;
         // find comment separator slash
         var cmtPos = searchCommentSeparator( rawData );
         if ( cmtPos < 0) {
            // no comment separator
            cmtPos = 80;
         }
         // value substring
         value = rawData.toString( 9, cmtPos-9 );
         if ( cmtPos < 80 ) {
            // comment substring
            comment = rawData.toString( cmtPos+1, 80-cmtPos-1 );
         }
      } else if (name === 'HIERARCH') {
         var viPos = searchHierarchValueIndicator(rawData);
         if (viPos > 0) {
            hasValue = true;
            name = rawData.toString(9, viPos-10);
            // find comment separator slash
            var cmtPos = searchCommentSeparator( rawData );
            if ( cmtPos < 0 ) {
               // no comment separator
               cmtPos = 80;
            }
            // value substring
            value = rawData.toString( viPos+1, cmtPos-viPos-1 );
            if ( cmtPos < 80 ) {
               // comment substring
               comment = rawData.toString( cmtPos+1, 80-cmtPos-1 );
            }
         }
      }

      // If no value in this keyword
      if (! hasValue) {
         comment = rawData.toString( 8, 80-8 ).trim();
      }


      // Perform a naive sanity check: a valid FITS file must begin with a SIMPLE=T keyword.
      if ( keywords.length === 0 ) {
         if ( name !== "SIMPLE  " && value.trim() !== 'T' ) {
            throw new Error( "File does not seem to be a valid FITS file (SIMPLE T not found): " + f.path );
         }
      }

      // Add new keyword.
      var fitsKeyWord = new FITSKeyword( name, value, comment);
      fitsKeyWord.trim();
      keywords.push(fitsKeyWord);
#ifdef DEBUG_SHOW_FITS
      debug("ffM_loadFITSKeywordsList: - " + fitsKeyWord);
#endif

   }


#ifdef DEBUG_SHOW_FITS
      debug("ffM_loadFITSKeywordsList: (end) ");
#endif

   return keywords;
};






// ====================================================================================================================
// FITS Keywords support module
// ====================================================================================================================

var ffM_FITS_Keywords = (function() {


   // --- private properties and methods ---------------------------------------

   // Unquote a string value
   var unquote = function unquote(s) {
      if (s===null) { return null;} // Could this happen ?
      if (s.length > 0 && s.charCodeAt(0) === 39 && s.charCodeAt(s.length-1) === 39) {
         // Unquoted string
         s = s.substring(1,s.length-1);
         // Replace double quotes inside by single quotes
         s = s.replace("''","'","g");
         if (s.length>0) {
            // Not the empty string,
            // trim trailing spaces only, but keep the first character if it is a space
            s = s.trimRight();
            if (s.length===0) {
               s = ' ';
            }
         }
      }
      return s;
   }

 


   // ------------------------------------------------------------------------------------------------------------------------
   // ImageKeywords support - An 'ImageKeywords' keeps track of the FITS keywords of a file, both as an array ordered
   // as in the file PDU and as a map of name to FITSKeyword for the value keywords (keywords that are not null).
   // It also keep track of some key characteristics of the file - especially if it is a single HDU image file
   // ------------------------------------------------------------------------------------------------------------------------
   // Prototype for methods operating on ImageKeywords
   var imageKeywordsPrototype = {

      // -- Load the FITS keywords from the file, adding them to the value map too
      loadFitsKeywords:  function loadFitsKeywords(fitsFilePath, mutableErrorList) {
         var imageKeywords = this;
         var name, fitsKeyFromList, i;
         var hdu = {};

         var f = new File;
         f.openForReading( fitsFilePath );
         try {
            hdu.fileSize =  f.size;
            imageKeywords.fitsKeywordsList = local_loadFITSKeywordsList(f, mutableErrorList);
         } finally {
            f.close();
         }

         // Make a map of all fits keywords with a value (this ignores the comment keywords)
         imageKeywords.fitsKeywordsMap = {};
         for (i=0; i<imageKeywords.fitsKeywordsList.length; i++) {
            fitsKeyFromList = imageKeywords.fitsKeywordsList[i];
            if (!fitsKeyFromList.isNull) {
               name = fitsKeyFromList.name;
               // IMPORTANT: FitsKey is shared with the list
               // TODO Check for duplicates (not supported, should be a warning)
               imageKeywords.fitsKeywordsMap[name] = fitsKeyFromList;
            }
         }

         // Complet info on HDU
         hdu.numberOfCards = imageKeywords.fitsKeywordsList.length;
         hdu.blockNumber = Math.floor((hdu.numberOfCards + 35 ) / 36);       

         // Check the characteristics of the (primary) HDU and if it is a single HDU
         // Note: imageKeywords must be initialized
         // TODO Error if the keyword is missing
         var bitpix = imageKeywords.getValueKeyword('BITPIX').numericValue;
         var naxis = imageKeywords.getValueKeyword('NAXIS').numericValue;
         var axisProduct = 1;
         for (var i=1; i<=naxis; i++) {
            var axis = imageKeywords.getValueKeyword('NAXIS' + i).numericValue; 
            axisProduct *= axis;        
         }
         var elementSize = 0;
         if (bitpix === 8)        {elementSize = 1; }
         else if (bitpix === 16)  {elementSize = 2;}
         else if (bitpix === 32)  {elementSize = 4;}
         else if (bitpix === 64)  {elementSize = 8;}
         else if (bitpix === -32) {elementSize = 4;}
         else if (bitpix === -64) {elementSize = 8;}

         hdu.dataSize = elementSize * axisProduct;

         hdu.paddedDataSize = Math.floor((hdu.dataSize+2880-1)/2880) * 2880;

         // Estimate size of header 
         hdu.headerSize =  Math.floor((imageKeywords.fitsKeywordsList.length*80+2880-1)/2880) * 2880;
         hdu.totalSize = hdu.paddedDataSize + hdu.headerSize;   

         Console.writeln("**** HDU " + Object.keys(hdu).map(function (k) {return k + "->" + hdu[k]}));
         if (hdu.totalSize < hdu.fileSize) {
            mutableErrorList.push("Likely mutliple HDU in file - primary HDU size " + hdu.totalSize +", file size " + hdu.fileSize);
         }

         f.close();

      },

      // -- return the FITSKeyword by name (if keyword has a value), return null otherwise
      getValueKeyword: function getValueKeyword(name) {
         var imageKeywords = this;
         if (imageKeywords.fitsKeywordsMap.hasOwnProperty(name)) {
            return imageKeywords.fitsKeywordsMap[name];
         } else {
            return null;
         }
      },
      // -- Return the value (that is the raw String, as read) of a FITSKeyword
      getValue: function getValue(name) {
         var kw = this.getValueKeyword(name);
         if (kw === null) {
            return null;
         } else {
            return kw.value;
         }
      },
      // -- Return the value as a stripped from outside quotes and trimmed (the PI way)
      getStrippedValue: function getStrippedValue(name) {
         var kw = this.getValueKeyword(name);
         if (kw === null) {
            return null;
         } else {
            return kw.strippedValue;
         }
      },
      // -- Return the FITS keyword value as a string, unquoted if it was a string, following the FITS rules
      // (remove outside quote and inside double quote, trim trailing spaces but not leading spaces)
      // Note that it is not possible to distinguish a string value from a boolean or numeric value with
      // the same representation (both '123' and 123 will result in the same string)
      // The unquoted value is suitable for display
      // This assumes that the value was trimmed (the first and last characters must be the quotes if it is a string value)
      getUnquotedValue: function getUnquotedValue(keywordName) {
         var kw = this.getValueKeyword(keywordName);
         if (kw === null) {
            return null;
         } else {
            return unquote(kw.value);
         }
      },


      // -- return the name of all value key words
      getNamesOfValueKeywords: function getNamesOfValueKeywords() {
         var imageKeywords = this;
         return Object.keys(imageKeywords.fitsKeywordsMap);
      }

   };



   // Factory method for an empty ImageKeywords (seems not used)
   var makeImageKeywords = function makeNew() {
      var imageKeywords = Object.create(imageKeywordsPrototype);
      imageKeywords.fitsKeywordsMap = {};
      imageKeywords.fitsKeywordsList = [];
      return imageKeywords;
   }

   // Factory method of an imageKeywords from a file
   var makeImageKeywordsfromFile = function makeImageKeywordsfromFile(filePath, mutableErrorList) {
      var imageKeywords = makeImageKeywords();
      imageKeywords.loadFitsKeywords(filePath, mutableErrorList);
      return imageKeywords;
   };



   // ------------------------------------------------------------------------------------------------------------------------
   // KeywordsSet support - Keeps track of the name of all values keywords used a set of files, in discovery mode
   // (used mostly to show the same list of keywords to the users for all images, to make GUI more predictable)
   // ------------------------------------------------------------------------------------------------------------------------
   // private prototype
   var keywordsSetPrototype = {
       putKeyword: function putKeyword(name) {
           var keywordsSet = this;
           if (!keywordsSet.allValueKeywordNames.hasOwnProperty(name)) {
               keywordsSet.allValueKeywordNames[name] = keywordsSet.allValueKeywordNameList.length;
               keywordsSet.allValueKeywordNameList.push(name);
           }
       },
       putAllImageKeywords: function putAllImageKeywords(imageKeywords) {
           var keywordsSet = this;
           var kwList = imageKeywords.fitsKeywordsList;
           for (var i=0; i<kwList.length; i++) {
               if (!kwList[i].isNull) {
                   keywordsSet.putKeyword(kwList[i].name);
               }
           }
       },
       // Use size on purpose, as this is a function (length of array is a property)
       size: function size() {
         return this.allValueKeywordNameList.length;
       }
   }

   // Factory method to create empty KeywordsSet
   var makeKeywordsSet = function makeKeywordsSet () {
      var keywordsSet = Object.create(keywordsSetPrototype);
      keywordsSet.allValueKeywordNameList = [];
      keywordsSet.allValueKeywordNames = {}; // Keyword name to keyword index (position in list)
      return keywordsSet;
   }



   // --- public properties and methods ---------------------------------------

   // Return public methods of this module
   return {
      makeImageKeywordsfromFile: makeImageKeywordsfromFile,
      makeKeywordsSet: makeKeywordsSet,
       // Made public for short format of columns
      unquote: unquote,

      // For unit testing only
      UT: {
         unquote: unquote,
      }
   }


}) ();
