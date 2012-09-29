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

