"use strict";

// FITSFileManager-attributes-tests


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include "PJSR-unit-tests-support.jsh"

#define DEBUG true


#define VERSION "0.5-tests"

#include "../../main/js/FITSFileManager-attributes.jsh"



// ---------------------------------------------------------------------------------------------------------
// Unit tests
// ---------------------------------------------------------------------------------------------------------


var ffM_allTests = {
   // Validate some assumption on how FITSKeyword works
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
   test_ffM_fkt_trim: function() {
      var kw = new FITSKeyword("  name  ", "  value  ","  comment  ");
      kw.trim(); // Not a function
      pT_assertEquals("{name,value,comment}", kw.toString());
   },
   test_ffM_fkt_numeric: function() {
      pT_assertEquals(12.5, new FITSKeyword("name", " 12.5").numericValue);
   },
   test_ffM_fkt_is_numeric: function() {
      pT_assertTrue(new FITSKeyword("name", " 12.5").isNumeric);
   },
   test_ffM_fkt_is_string: function() {
      pT_assertFalse(new FITSKeyword("name", " 12.5").isString);
   },
   test_ffM_fkt_is_null: function() {
     // isNull test for empty string. null is not allowed
      pT_assertTrue( new FITSKeyword("name","").isNull);
   },
   test_ffM_fkt_is_blank: function() {
      pT_assertFalse(new FITSKeyword("name"," ").isNull);
   },
   test_ffM_fkt_stripped: function() {
      // Trim the value
      pT_assertEquals("aha", new FITSKeyword("name", " aha ").strippedValue);
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




// ---------------------------------------------------------------------------------------------------------
// Auto execute tests on load
// ---------------------------------------------------------------------------------------------------------



pT_executeTests(ffM_allTests);

