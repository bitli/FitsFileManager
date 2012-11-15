// FITSFileManager-attributes.jsh

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js

#include <pjsr/DataType.jsh>

// *************** REFACTORING IN PROGRESS - THIS FILE IS NOT CURRENTY USED ******************

var ffM_Attributes = (function() {


   // Code from FitsKey and/or other examples
   // Read the FITS keywords of an image file, supports the HIERARCH convention
   // Input: The full path of a file
   // Return: An array FITSKeyword
   // The value of a FITSKeyWord value is the empty string if the keeyword had no value
   var loadFITSKeywordsList =  function(fitsFilePath ) {

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
      // value separator (an equal sign at byte 8) present?
      if ( rawData.at( 8 ) === 61 ) {
         // This is a valued keyword
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
         } else {
            comment = new String;
         }
      }
      else if (name === 'HIERARCH') {
         var hasValue = false;
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
            } else {
               comment = new String;
            }
         }

         // If mo value in this keyword
         if (! hasValue) {
            value = new String;
            comment = rawData.toString( 8, 80-8 ).trim();
         }
      }

#ifdef DEBUG_FITS
      debug("loadFITSKeywords: - name[" + name + "],["+value+ "],["+comment+"]");
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
   f.close();
   return keywords;
};


   // Common method for ImageAttribute
   var imageAttributesPrototype = {

      // Load or reload the FITS keywords from the file
      loadFitsKeywords:  function() {
         var imageAttributes = this;
         var name, fitsKeyFromList, i;
         imageAttributes.fitsKeyWordsList = loadFITSKeywordsList(imageAttributes.filePath);
         imageAttributes.fitsKeyWordsMap = {};
         for (i=0; i<imageAttributes.fitsKeyWordsList.length; i++) {
            fitsKeyFromList = imageAttributes.fitsKeyWordsList[i];
            name = fitsKeyFromList.name;
            if  (imageAttributes.fitsKeyWordsMap[name]) {
               if (typeof imageAttributes.fitsKeyWordsMap[name].prototype === 'Array') {
                  imageAttributes.fitsKeyWordsMap[name].push(fitsKeyFromList);
               } else {
                  imageAttributes.fitsKeyWordsMap[name] = fitsKeyFromList;
               }
            } else {
               imageAttributes.fitsKeyWordsMap[name] = fitsKeyFromList;
            }
         }
      },

   };

   // Factory method of an ImageAttributes
   var makeImageAttributes = function makeImageAttributes(filePath) {
      var imageAttributes = Object.create(imageAttributesPrototype);
      imageAttributes.filePath = filePath;
      imageAttributes.fitsKeyWordsMap = {};
      imageAttributes.fitsKeyWordsList = [];
      return imageAttributes;
   };

   return {
      makeImageAttributes: makeImageAttributes
   }


}) ();
