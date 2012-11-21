// FITSFileManager-fits.jsh

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js

#include <pjsr/DataType.jsh>


// ------------------------------------------------------------------------------------------------------------------------
// Read the FITS keywords of an image file, supports the HIERARCH convention
// ------------------------------------------------------------------------------------------------------------------------
// Input:  The full path of a file
// Return: An array FITSKeyword, identical to what would be returned by ImageWindow.open().keywords
// Throws: Error if bad format
// The value of a FITSKeyWord value is the empty string if the keyword had no value
// Code adapted from FitsKey and other scripts
// ------------------------------------------------------------------------------------------------------------------------
var ffM_loadFITSKeywordsList =  function loadFITSKeywordsList(fitsFilePath ) {

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

#ifdef DEBUG_FITS
      debug("ffM_loadFITSKeywordsList: - loading '" + fitsFilePath + "'");
#endif

   var f = new File;
   f.openForReading( fitsFilePath );
   try {

   var keywords = [];
   for ( ;; ) {
      var rawData = f.read( DataType_ByteArray, 80 );

      // Console.writeln(rawData.toString());

      var name = rawData.toString( 0, 8 );
      if ( name.toUpperCase() === "END     " ) { // end of HDU keyword list?
         break;
      }
      if ( f.isEOF ) {
         throw new Error( "Unexpected end of file reading FITS keywords, file: " + fitsFilePath );
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


#ifdef DEBUG_FITS
      debug("ffM_loadFITSKeywordsList: - name[" + name + "],["+value+ "],["+comment+"]");
#endif
      // Perform a naive sanity check: a valid FITS file must begin with a SIMPLE=T keyword.
      if ( keywords.length === 0 ) {
         if ( name !== "SIMPLE  " && value.trim() !== 'T' ) {
            throw new Error( "File does not seem to be a valid FITS file (SIMPLE T not found): " + fitsFilePath );
         }
      }

      // Add new keyword.
      keywords.push( new FITSKeyword( name.trim(), value.trim(), comment.trim() ) );
   }
   } finally {
   f.close();
   }
   return keywords;
};


// ------------------------------------------------------------------------------------------------------------------------
// Find a FITS keyword value by name
// fitsKeyWordsArray: an array of FITSKeywords
// name: The (trimed) name of the keyword
// return the value of the first keyword of the specified name (empty string if it has no value) or null if the
// keyword is undefined
// intended to use to find regular (single value) keywords
// ------------------------------------------------------------------------------------------------------------------------
function ffM_findKeyWord(fitsKeyWordsArray, name) {
   for (var k =0; k<fitsKeyWordsArray.length; k++) {
      if (fitsKeyWordsArray[k].name === name)  {
         // keyword found in the file >> extract value
#ifdef DEBUG_FITS
         debug("ffM_findKeyWord: '" + fitsKeyWordsArray[k].name + "' found '"+ fitsKeyWordsArray[k].value + "'");
#endif
         return (fitsKeyWordsArray[k].value)
      }
   }
#ifdef DEBUG_FITS
   debug("ffM_findKeyWord: '" +name + "' not found");
#endif
   return null;
}


// ------------------------------------------------------------------------------------------------------------------------
// Global object to contains the FITS utility methods
var ffm_keywordsOfFile = (function() {


   // ------------------------------------------------------------------------------------------------------------------------
   // imageKeywords keeps track of the FITS keywords of a file, both as an array ordered
   // as in the file and as a map of name to keywords for the values keywords (non null) for quick lookup
   // ------------------------------------------------------------------------------------------------------------------------
   // Common method for imageKeywords
   var imageKeywordsPrototype = {

      reset: function reset() {
         var imageKeywords = this;
         // This two attributes may be accessed by the callers, but this is not recommended
         imageKeywords.fitsKeyWordsMap = {};
         imageKeywords.fitsKeyWordsList = [];
      },

      // -- Clear and load the FITS keywords from the file, adding them to the value map too
      loadFitsKeywords:  function loadFitsKeywords(filePath) {
         var imageKeywords = this;
         this.reset();
         var name, fitsKeyFromList, i;
         imageKeywords.fitsKeyWordsList = ffM_loadFITSKeywordsList(filePath);
         // Make a map of all fits keywords with a value (this remove the comment keywords)
         imageKeywords.fitsKeyWordsMap = {};
         for (i=0; i<imageKeywords.fitsKeyWordsList.length; i++) {
            fitsKeyFromList = imageKeywords.fitsKeyWordsList[i];
            if (!fitsKeyFromList.isNull) {
               name = fitsKeyFromList.name;
               // IMPORTANT: FitsKey is shared with the list
               // TODO Check for duplicates (not supported)
               imageKeywords.fitsKeyWordsMap[name] = fitsKeyFromList;
            }
         }
      },

      // -- return the FITS keyword by name (if keyword has a value), return null otherwise
      getValue: function getValue(name) {
         var imageKeywords = this;
         if (imageKeywords.fitsKeyWordsMap.hasOwnProperty(name)) {
            return imageKeywords.fitsKeyWordsMap[name];
         } else {
            return null;
         }
      },

      // -- return the name of all value key words
      getNamesOfValueKeywords: function getNamesOfValueKeywords() {
         var imageKeywords = this;
         return Object.keys(imageKeywords.fitsKeyWordsMap);
      }

   };

   // Factory method for an empty imageKeywords
   var makeImageKeywords = function makeNew() {
      var imageKeywords = Object.create(imageKeywordsPrototype);
      return imageKeywords;
   }

   // Factory method of an imageKeywords
   var makeImageKeywordsfromFile = function makeImageKeywordsfromFile(filePath) {
      var imageKeywords = makeImageKeywords();
      imageKeywords.loadFitsKeywords(filePath);
      return imageKeywords;
   };



   // ------------------------------------------------------------------------------------------------------------------------
   // Keeps track of all values keywords in a set of files, in a specific order
   // NOT YET USED
   // ------------------------------------------------------------------------------------------------------------------------
   var keywordSetPrototype = {
       putKeyword: function putKeyword(name) {
           var keywordsSet = this;
           if (!keywordsSet.allValueKeywordNames.hasOwnProperty(name)) {
               keywordsSet.allValueKeywordNames[name] = keywordsSet.allValueKeywordNameList.length;
               keywordsSet.allValueKeywordNameList.push(name);
           }
       },
       putAllImageKeywords: function putAllImageKeywords(imageKeywords) {
           var keywordsSet = this;
           var kwList = imageKeywords.fitsKeyWordsList;
           for (var i=0; i<kwList.length; i++) {
               if (!kwList[i].isNull) {
                   keywordsSet.putKeyword(kwList[i].name);
               }
           }
       }
   }
   var makeKeywordsSet = function makeKeywordsSet () {
      var keywordsSet = Object.create(keywordSetPrototype);
      keywordsSet.allValueKeywordNameList = [];
      keywordsSet.allValueKeywordNames = {};
      return keywordsSet;
   }


   // Return public methods of this module
   return {
      makeImageKeywordsfromFile: makeImageKeywordsfromFile,
      makeKeywordsSet: makeKeywordsSet,
   }


}) ();
