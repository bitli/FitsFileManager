"use strict";

// FITSFileManager-fits-tests


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include "PJSR-unit-tests-support.jsh"

#define DEBUG true


#define VERSION "0.5-tests"

#include "../../main/js/FITSFileManager-fits.jsh"



// ---------------------------------------------------------------------------------------------------------
// Unit tests
// ---------------------------------------------------------------------------------------------------------


var ffM_allTests = {

   // --- Validate some assumption on how FITSKeyword works

   // Creation from the values, format, no trim() by default
   test_ffM_fkt_1: function() {
      pT_assertEquals("{name,value,comment}", new FITSKeyword("name","value","comment").toString());
   },
   test_ffM_fkt_2: function() {
      pT_assertEquals("{name,value,}", new FITSKeyword("name","value").toString());
   },
   test_ffM_fkt_must_be_string: function() {
      pT_assertEquals("{name, 123 ,}", new FITSKeyword("name"," 123 ").toString());
   },
   test_ffM_fkt_does_not_trim: function() {
      pT_assertEquals("{  name  ,  value  ,  comment  }", new FITSKeyword("  name  ", "  value  ","  comment  ").toString());
   },

   // Trim
   test_ffM_fkt_trim: function() {
      var kw = new FITSKeyword("  name  ", "  value  ","  comment  ");
      kw.trim(); // Not a function
      pT_assertEquals("{name,value,comment}", kw.toString());
   },
   test_ffM_fkt_stripped: function() {
      // Trim the value
      pT_assertEquals("aha", new FITSKeyword("name", " aha ").strippedValue);
   },
   // Test types
   test_ffM_fkt_is_not_numeric: function() {
      pT_assertTrue(new FITSKeyword("name", " 12.5").isNumeric);
   },
   test_ffM_fkt_is_numeric: function() {
      pT_assertFalse(new FITSKeyword("name", " T ").isNumeric);
   },
   test_ffM_fkt_is_string: function() {
      pT_assertFalse(new FITSKeyword("name", " 12.5").isString);
   },
   test_ffM_fkt_is_null: function() {
      // isNull test for empty string, null is not allowed as a parameter for the value
      pT_assertTrue( new FITSKeyword("name","").isNull);
   },
   test_ffM_fkt_is_blank: function() {
      // Space is not null
      pT_assertFalse(new FITSKeyword("name"," ").isNull);
   },
   test_ffM_fkt_is_blank_trimmed: function() {
      // trimmed space is null
      var kw = new FITSKeyword("name"," ");
      kw.trim();
      pT_assertTrue(kw.isNull);
   },
   // Boolean is FITS T,F (trimmed) not true/false or empty !
   test_ffM_fkt_is_boolean_T: function() {
      pT_assertTrue(new FITSKeyword("name","T").isBoolean);
   },
   test_ffM_fkt_is_boolean_T_not_trimmed: function() {
      pT_assertFalse(new FITSKeyword("name"," T ").isBoolean);
   },
   test_ffM_fkt_is_boolean_true: function() {
      pT_assertFalse(new FITSKeyword("name","true").isBoolean);
   },

   test_ffM_fkt_is_boolean_F: function() {
      pT_assertTrue(new FITSKeyword("name","F").isBoolean);
   },
   test_ffM_fkt_is_boolean_false: function() {
      pT_assertFalse(new FITSKeyword("name","false").isBoolean);
   },
   test_ffM_fkt_is_boolean_empty: function() {
      pT_assertFalse(new FITSKeyword("name","").isBoolean);
   },

   // Get typed values
   test_ffM_fkt_numeric: function() {
      pT_assertEquals(12.5, new FITSKeyword("name", " 12.5").numericValue);
   },
   // Non numeric value show as 0, but do not have the isNumeric
   test_ffM_fkt_boolean: function() {
      pT_assertEquals(0, new FITSKeyword("name", " T ").numericValue);
   },

   // Quotes in strings, strippedValue also remove quotes
   test_ffM_fkt_quotes: function() {
      pT_assertEquals("' quoted '", new FITSKeyword("name'", "' quoted '").value);
   },
   test_ffM_fkt_quotes2: function() {
      // Strip external quotes and trim
      pT_assertEquals("quoted", new FITSKeyword("name", "' quoted '").strippedValue);
   },
   test_ffM_fkt_quotes3: function() {
      // Does not strip internal quotes
      pT_assertEquals("quote''d", new FITSKeyword("name", "' quote''d '").strippedValue);
   },
   test_ffM_fkt_quotes4: function() {
      // trim does not strip
      var kw = new FITSKeyword("name", "' quoted '");
      kw.trim();
      pT_assertEquals("' quoted '", kw.value);
   },

   // Test loading keywords from file by PI
   test_ffM_compare_files_simple: function() {
      pT_compareTwoLoads(26,"C:/Users/jmlugrin/Documents/Astronomie/Programs/PixInsight/PI my Scripts/FitsFileManager/sources/test/images/m31_Green_0028.fit");
   },
   test_ffM_compare_files_hierarch: function() {
      pT_compareTwoLoads(153,"C:/Users/jmlugrin/Documents/Astronomie/Programs/PixInsight/PI my Scripts/FitsFileManager/sources/test/images/dsaI_0008.fits");
   },



   test_ffM_create: function() {
      pT_assertEquals("toto", ffM_Attributes.makeImageAttributes("toto").filePath);
   },
   test_ffM_load_fits: function() {
      var attrs = ffM_Attributes.makeImageAttributes("C:/Users/jmlugrin/Documents/Astronomie/Programs/PixInsight/PI my Scripts/FitsFileManager/sources/test/images/m31_Green_0028.fit");
      pT_assertEquals(0, attrs.fitsKeyWordsList.length);
      attrs.loadFitsKeywords();
      pT_assertEquals(26, attrs.fitsKeyWordsList.length);
   },



}

// Utility method to load FITS file keys by PI and by script and compare the result
function pT_compareTwoLoads(expectedNumberOfKeywords, sourceFilePath) {

      // Load by PI
      var images = ImageWindow.open( sourceFilePath,"test_ffM_compare_file_1", true );
      var image = images[0];
      var piKeywords = image.keywords;
      image.close();
      pT_assertEquals(expectedNumberOfKeywords, piKeywords.length);

      // Load by script
      var jsKeywords = ffM_loadFITSKeywordsList(sourceFilePath);
      pT_assertEquals( piKeywords.length, jsKeywords.length);

      for (var i=0; i< piKeywords.length; i++) {
         var kw1 = piKeywords[i];
         var kw2 = jsKeywords[i];
//       Console.writeln("pi: " +kw1);
//       Console.writeln("fm: " +kw2);
//       Console.writeln("value " + kw1.name + ": " + kw1.value + " " + kw2.value);
         pT_assertEquals(kw1.name, kw2.name);
         pT_assertEquals(kw1.value, kw2.value);
         pT_assertEquals(kw1.comment, kw2.comment);
         pT_assertEquals(kw1.numericValue, kw2.numericValue);
         pT_assertEquals(kw1.strippedValue, kw2.strippedValue);
         pT_assertEquals(kw1.isBoolean, kw2.isBoolean);
         pT_assertEquals(kw1.isNumeric, kw2.isNumeric);
         pT_assertEquals(kw1.isNull, kw2.isNull);
         pT_assertEquals(kw1.isString, kw2.isString);
      }

}

// ---------------------------------------------------------------------------------------------------------
// Auto execute tests on load
// ---------------------------------------------------------------------------------------------------------



pT_executeTests(ffM_allTests);

