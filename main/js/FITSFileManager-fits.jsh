// FITSFileManager-fits.jsh

// This file is part of FITSFileManager, see copyright in FITSFileManager.js

#include <pjsr/DataType.jsh>
#include <pjsr/SeekMode.jsh>


// ------------------------------------------------------------------------------------------------------------------------
// Read the FITS keywords of the first HDU of an image file, supports the HIERARCH convention
// ------------------------------------------------------------------------------------------------------------------------
// Input:  The opened file
// Return: An array of FITSKeyword objects, identical to what would be returned by ImageWindow.open().keywords,
//         at least for correct keywords (errors and border line cases may be handled differently).
// Throws: Error if bad format, update the mutableErrorList if non fatal error
// The value of a FITSKeyword value is the empty string if the keyword had no value
// Code adapted from FitsKey and other scripts
// See https://fits.gsfc.nasa.gov/standard40/fits_standard40aa-le.pdf for FITS standard and
// https://heasarc.gsfc.nasa.gov/fitsio/c/f_user/node28.html for HIERARCH convention.
// TODO - Support CONTINUE ?
// TODO - Support blank comment as a continuation of a previous coomment ?
// DEBUG support: DEBUG_SHOW_FITS - List the FITS keyword loaded
//                DEBUG_FITS - More detailled on the operations
// TESTING: See test/js/FITSFileNamanger-fits-test.js
// ------------------------------------------------------------------------------------------------------------------------

// Method used for testing (read the first HDU)
var ffM_loadFITSKeywordsList =  function loadFITSKeywordsList(fitsFilePath, mutableErrorList) {
   var f = new File;
   f.openForReading( fitsFilePath );
   try {
      return local_loadFITSKeywordsList(f, mutableErrorList);
   } finally {
      f.close();
   }
}


// ------------------------------------------------------------------------------------

// TODO Move to module

// Internal method to read the FITS keywords of an opened file from current location
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
   // Example: HIERARCH LongKeyword = 47.5 / Keyword has > 8 characters, and mixed case
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

      // Parse name part
      var name = rawData.toString( 0, 8 );
      if ( name.toUpperCase() === "END     " ) { // end of HDU keyword list?
         break;
      }
      if ( f.isEOF ) {
         throw new Error( "Unexpected end of file reading FITS keywords, file: " + f.path );
      }

      // Parse value / comment parts , handle HIERARCH
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

      // Create the PJSR FITS keyword and add it to the array.
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
// Define imageKeywordsPrototype for handling FITSkeyword from the other modules.
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
   // as in the file PDU and as a map of name to FITSKeyword for the value keywords (the keywords that are not null).
   // It also keep track of some key characteristics of the file - especially if it is a single HDU image file and
   // if it has a thumbnail extension.
   // This class loads the FITS keywords and also provide various accessor methods.
   // ------------------------------------------------------------------------------------------------------------------------
   // Prototype for methods operating on ImageKeywords
   var imageKeywordsPrototype = {

      // -- Load the FITS keywords of the first HDU of a file, creating a new fitsKeywordsMap
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

         // Complete info on HDU
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

         // See if there is an known extension often used by PixInsight (this is not normally the case
         // for images from cameras)
         // Make sure to accept of there is not EXTEND keyword ! PI seems to always add it.
         hdu.mayExtend = imageKeywords.getValueKeyword('EXTEND')!= null &&  imageKeywords.getValueKeyword('EXTEND').value == 'T';
         hdu.hasThumbnail = hdu.mayExtend &&  imageKeywords.getValueKeyword('THUMBIMG') != null && imageKeywords.getStrippedValue('THUMBIMG') == 'Thumbnail';
#ifdef DEBUG_FITS
            debug("HDU extension: " + hdu.mayExtend + ", thumbnail: " +  hdu.hasThumbnail);
#endif        

         // Calculate the size of the header (use the list of all keywords)
         hdu.headerSize =  Math.floor((imageKeywords.fitsKeywordsList.length*80+2880-1)/2880) * 2880;
         hdu.totalSize = hdu.paddedDataSize + hdu.headerSize;   

         hdu.multiple = hdu.totalSize < hdu.fileSize;

#ifdef DEBUG_FITS
         debug("Loaded HDU properties: " + Object.keys(hdu).map(function (k) {return k + "->" + hdu[k]}));
#endif

         if (hdu.multiple) {
            // mutableErrorList.push("Likely multiple HDU in file - primary HDU size " + hdu.totalSize +", file size " + hdu.fileSize);
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


      // -- return the name of all value keywords
      getNamesOfValueKeywords: function getNamesOfValueKeywords() {
         var imageKeywords = this;
         return Object.keys(imageKeywords.fitsKeywordsMap);
      }

   };



   // Factory method for an empty ImageKeywords (seems not to be used)
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

// ----------------------------------------------------------------------------
// FITS File object
var ffM_FITS_Files = (function() {

   // ------------------------------------------------------------------------------
   // --- Local methods
   // ------------------------------------------------------------------------------
   let loadFITSKeywordsList =  function loadFITSKeywordsList(f, mutableErrorList) {

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
      };

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
      };

#ifdef DEBUG_SHOW_FITS
      debug("loadFITSKeywordsList: loading '" + f.path + "'");
#endif

      let isPrimaryHDU = (f.position === 0);

      let keywords = [];
      for ( ;; ) {

         // TODO read by block
         let rawData = f.read( DataType_ByteArray, 80 );
#ifdef DEBUG_FITS
         debug("loadFITSKeywordsList: line - '" + rawData.toString() + "'");
#endif

         let name = rawData.toString( 0, 8 );
         if ( name.toUpperCase() === "END     " ) { // end of HDU keyword list?
            break;
         }
         if ( f.isEOF ) {
            mutableErrorList.push("Unexpected end of file reading FITS keywords.");
            return null;
         }

         let value = "";
         let comment = "";
         let hasValue = false;

         // value separator (an equal sign at byte 8) present?
         if ( rawData.at( 8 ) === 61 ) {
            // This is a valued keyword
            hasValue = true;
            // find comment separator slash
            let cmtPos = searchCommentSeparator( rawData );
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
            let viPos = searchHierarchValueIndicator(rawData);
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

         // If no value in this keyword, fill with spaces
         if (! hasValue) {
            comment = rawData.toString( 8, 80-8 ).trim();
         }


         // Perform a naive sanity check: a valid FITS file must begin with a SIMPLE=T keyword.
         if ( isPrimaryHDU && keywords.length === 0 ) {
            if ( name !== "SIMPLE  " && value.trim() !== 'T' ) {
               mutableErrorList.push("Not a valid FITS file (SIMPLE T not found).");
               return null;
            }
         }

         // Add new keyword.
         let fitsKeyWord = new FITSKeyword( name, value, comment);
         fitsKeyWord.trim();
         keywords.push(fitsKeyWord);
#ifdef DEBUG_SHOW_FITS
         debug("loadFITSKeywordsList: - " + fitsKeyWord);
#endif

      }


#ifdef DEBUG_SHOW_FITS
         debug("loadFITSKeywordsList: (end) ");
#endif

      return keywords;
   };


   // -------------------------------------------------------------------------------
   let makeFITSValueKeywordsMap = function makeFITSValueKeywordsMap(fitsKeywordsList) {
     // Make a map of all fits keywords with a value (this ignores the comment keywords)
      let fitsKeywordsMap = {};
      for (let i=0; i<fitsKeywordsList.length; i++) {
         let fitsKeyFromList = fitsKeywordsList[i];
         if (!fitsKeyFromList.isNull) {
            // It has a value
            let name = fitsKeyFromList.name;
            // IMPORTANT: FitsKey is shared with the list
            // TODO Add check for duplicates (they are not supported, should be a warning)
            fitsKeywordsMap[name] = fitsKeyFromList;
         }
      }
      return fitsKeywordsMap;

   }





   // ------------------------------------------------------------------------------
   // ---- HDU
   // ------------------------------------------------------------------------------
   let HDU = function HDU() {
      this.fitsValueKeywordsMap = {};
      this.fitsKeywordsList = [];
   }

   HDU.prototype._addKeywords = function _addKeywords(keywords) {
      this.fitsKeywordsList = keywords;
      this.fitsValueKeywordsMap = makeFITSValueKeywordsMap(keywords);

      // Complete information on keyword size and offset of data
      this.numberOfCards = keywords.length;
      this.blockNumber = Math.floor((this.numberOfCards + 35 ) / 36);       
   }

   HDU.prototype._checkAndCompleteHDU = function _checkAndCompleteHDU (startOfHeader, endOfHeader, mutableErrorList) {

      // Make location of header and data
      this.startOfHeader = startOfHeader;
      // TODO Wrong, calculated location
      this.endOfHeader = endOfHeader;


      //let simpleKW = imageKeywords.getValueKeyword('SIMPLE');
      let bitpixKW = this.getValueKeyword('BITPIX');
      let naxisKW = this.getValueKeyword('NAXIS');

      // TODO  review error reporting
      if (bitpixKW == null || naxisKW === null) {
         mutableErrorList.push("Bad FITS file format, missing BITPIX or NAXIS.");
         return;
      }

      // -------------------------------------------------------------------------------
      // Check the characteristics of the (primary) HDU and if it is a single HDU
       // TODO Error if the keyword is missing
      var bitpix = bitpixKW.numericValue;
      var naxis = naxisKW.numericValue;
      var axisProduct = 1;
      for (var i=1; i<=naxis; i++) {
         let axisKWname = 'NAXIS' + i;
         let axisKW = this.getValueKeyword(axisKWname); 
         // TODO  review error reporting
         if (axisKW == null) {
            mutableErrorList.push("Bad FITS file format, missing '"  + axisKWname + "'");
            return;
         }
         var axis = axisKW.numericValue; 
         axisProduct *= axis;        
      }
      var elementSize = 0;
      if (bitpix === 8)        {elementSize = 1; }
      else if (bitpix === 16)  {elementSize = 2;}
      else if (bitpix === 32)  {elementSize = 4;}
      else if (bitpix === 64)  {elementSize = 8;}
      else if (bitpix === -32) {elementSize = 4;}
      else if (bitpix === -64) {elementSize = 8;}

      this.dataSize = elementSize * axisProduct;
      this.paddedDataSize = Math.floor((this.dataSize+2880-1)/2880) * 2880;

      // Calculate size of header (the END keyword is not in the list) 
      this.headerSize = (this.fitsKeywordsList.length+1)*80;
      this.paddedHeaderSize =  Math.floor((this.headerSize +2880-1)/2880) * 2880;

      // Total (padded) size of HDU
      this.totalSize = this.paddedDataSize + this.paddedHeaderSize;   

    };


   // -- return the FITSKeyword by name (if keyword has a value), return null otherwise
   HDU.prototype.getValueKeyword = function getValueKeyword(name) {
      if (this.fitsValueKeywordsMap.hasOwnProperty(name)) {
         return this.fitsValueKeywordsMap[name];
      } else {
         return null;
      }
   };

      // -- Return the value (that is the raw String, as read) of a FITSKeyword
   HDU.prototype.getValue = function getValue(name) {
      let kw = this.getValueKeyword(name);
      if (kw === null) {
         return null;
      } else {
         return kw.value;
      }
   };

      // -- Return the value as a stripped from outside quotes and trimmed (the PI way)
   HDU.prototype.getStrippedValue = function getStrippedValue(name) {
      let kw = this.getValueKeyword(name);
      if (kw === null) {
         return null;
      } else {
         return kw.strippedValue;
      }
   };
      
   // -- Return the FITS keyword value as a string, unquoted if it was a string, following the FITS rules
   // (remove outside quote and inside double quote, trim trailing spaces but not leading spaces)
   // Note that it is not possible to distinguish a string value from a boolean or numeric value with
   // the same representation (both '123' and 123 will result in the same string)
   // The unquoted value is suitable for display
   // This assumes that the value was trimmed (the first and last characters must be the quotes if it is a string value)
   HDU.prototype.getUnquotedValue = function getUnquotedValue(keywordName) {
      let kw = this.getValueKeyword(keywordName);
      if (kw === null) {
         return null;
      } else {
         return unquote(kw.value);
      }
   };


   // -- return the name of all value key words
   HDU.prototype.getNamesOfValueKeywords = function getNamesOfValueKeywords() {
      return Object.keys(this.fitsValueKeywordsMap);
   };


   let makePrimaryHDU = function makePrimaryHDU(keywords, startOfHeader, endOfHeader, mutableErrorList) {
      let hdu = new HDU();
      hdu.primary = true;
      hdu._addKeywords(keywords);
      hdu._checkAndCompleteHDU(startOfHeader, endOfHeader);
      return hdu;
   };

   let makeExtendedHDU = function makeExtendedHDU(keywords, startOfHeader, endOfHeader, mutableErrorList) {
      let hdu = new HDU();
      hdu.primary = false;
      hdu._addKeywords(keywords);
      hdu._checkAndCompleteHDU(startOfHeader, endOfHeader);
      return hdu;
   };



   // ------------------------------------------------------------------------------
   // ---- FITSFile
   // ------------------------------------------------------------------------------

   let FITSFile = function FITSFile(path) {
      this._path = path;
      this.HDUs = new Array;
      this.errors = new Array;
      // Populated during load of primary HDU
      this._size = 0;
      this._multiExtension = null;
   };

   FITSFile.prototype.loadPrimaryHeader = function loadPrimaryHeader() {

      this.errors = new Array;

      let f = new File;
      f.openForReading( this._path );
      try {
         this._size =  f.size;
         var startOfHeader = 0;
         var primaryHDUkws = loadFITSKeywordsList(f, this.errors);
         var endOfHeader = f.position;
      } finally {
         f.close();
      }

      if (this.errors.length>0) return;

      let hdu = makePrimaryHDU(primaryHDUkws, startOfHeader, endOfHeader, this.errors);
      this.HDUs = [hdu];

      //Console.writeln("**** this " + Object.keys(this).map(function (k) {return k + "->" + this[k]}));
      this._multiExtension = (hdu.totalSize < this._size);

   }

   FITSFile.prototype.loadAllHeaders = function loadAllHeaders() {

      this.errors = new Array;

      let f = new File;
      f.openForReading( this._path );
      try {
         this._size =  f.size;
         let startOfHeader = 0;
         let keywords = loadFITSKeywordsList(f, this.errors);
         // TODO Wrong
         var endOfHeader = f.position;
    
         let hdu = makePrimaryHDU(keywords, startOfHeader, endOfHeader, this.errors);
         this._multiExtension = (hdu.totalSize < this._size);

         this.HDUs = [hdu];
      
         if (this._multiExtension) {
            let posOfNextHeader = hdu.startOfHeader + hdu.totalSize;
            while (posOfNextHeader<this._size) {
               f.position(posOfNextHeader);
               keywords = loadFITSKeywordsList(f, this.errors);
               // TODO Wrong
               endOfHeader = f.position;
               hdu = makePrimaryHDU(keywords, posOfNextHeader, endOfHeader, this.errors);
               this.HDUs.push(hdu);
               posOfNextHeader = hdu.startOfHeader + hdu.totalSize;
            }

         }



      } finally {
         f.close();
      }

       this.HDUs = [hdu];

 
   }


   FITSFile.prototype.getPath = function gePath() {
      return this._path;
   }




   let makeFITSFile = function makeFITSFile(path) {
      return new FITSFile(path);
   }


   return {
      makeFITSFile: makeFITSFile
   }
}) ();



