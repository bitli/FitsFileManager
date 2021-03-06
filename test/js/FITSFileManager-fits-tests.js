"use strict";

// FITSFileManager-fits-tests

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


// Test the FITS related functionality of FITSFileManager,
// Can be run as a script in the development environment of FITSFileManager.



#include "PJSR-unit-tests-support.jsh"

// Tracing - define DEBUG if you define any other DEBUG_xxx
#define DEBUG
//#define DEBUG_SHOW_FITS
//#define DEBUG_FITS


#define VERSION "1.4-tests"

// Unit testing, refrain to include other files to avoid conflict in tests
#include "../../main/js/FITSFileManager-fits.jsh"

#include "../../main/js/PJSR-logging.jsh"


// ---------------------------------------------------------------------------------------------------------
// Unit tests
// ---------------------------------------------------------------------------------------------------------

#ifdef DEBUG
   Console.show();
#endif

// From FITSFileManager-helper.js
// Return a full normalized path of a directory or a file, received in unix or windows format
function getDirectoryWithDriveLetter( a_directory_path )
{
   let unix_path = File.windowsPathToUnix( a_directory_path );
   let pathNormalized = File.fullPath(unix_path);
   //Console.writeln("*** getDirectoryWithDriveLetter\n    a_directory_path '" + a_directory_path + "'\n    unix_path '" + unix_path  + "'\n    pathNormalized '" + pathNormalized +"'");
   return pathNormalized;
}

// Return a full normalized path to the directory containing the parameter file (or directory)
function getDirectoryOfFileWithDriveLetter( a_file_path )
{
   let unix_path = File.windowsPathToUnix( a_file_path );
   let pathNormalized = File.fullPath(unix_path);
   let directoryWithDrive = File.extractDrive( pathNormalized ) + File.extractDirectory(pathNormalized);
   //Console.writeln("*** getDirectoryOfFileWithDriveLetter\n    a_file_path '" + a_file_path + "\n    unix_path '" + unix_path + "'\n    pathNormalized '" + pathNormalized + "' \n    directoryWithDrive '" + directoryWithDrive +"'");
   return directoryWithDrive;
}



// var PROJECT_BASE_DIR = File.fullPath(File.extractDrive(#__FILE__)+File.extractDirectory(#__FILE__)+"/../..");
var SCRIPT_DIR = getDirectoryOfFileWithDriveLetter(#__FILE__);
var PROJECT_BASE_DIR = getDirectoryWithDriveLetter(SCRIPT_DIR + "/../..");

var PROJECT_IMAGE_DIR = PROJECT_BASE_DIR+"/test/images";

Console.writeln("Project base directory is   '" + PROJECT_BASE_DIR + "'.");
Console.writeln("        images directory is '" + PROJECT_IMAGE_DIR + "'.")


var ffM_allTests = {



   // --- Validate some assumption on how PJSR FITSKeyword works,
   //     as some code may depend on it.

   // Creation from the parameters, the 'value' and toString are litteral images of the input strings,
   // even for String that looks like number
   test_ffM_fkt_create_2_params: function() {
      pT_assertEquals("{name,value,comment}", new FITSKeyword("name","value","comment").toString());
   },
   test_ffM_fkt_create_3_params: function() {
      pT_assertEquals("{name,value,}", new FITSKeyword("name","value").toString());
   },
   test_ffM_fkt_create_string_value_not_trimmed: function() {
      pT_assertEquals("{name, 123 ,}", new FITSKeyword("name"," 123 ").toString());
   },
   test_ffM_fkt_create_string_value_not_unquoted: function() {
      pT_assertEquals("{name,'quoted',}", new FITSKeyword("name","'quoted'").toString());
   },
   test_ffM_fkt_create_no_value_trimmed: function() {
      pT_assertEquals("{  name  ,  value  ,  comment  }", new FITSKeyword("  name  ", "  value  ","  comment  ").toString());
   },

   // Check behavior of trim() - does no unquote, trim both leading and trailing
   test_ffM_fkt_trim_unquoted: function() {
      var kw = new FITSKeyword("  name  ", "  value  ","  comment  ");
      kw.trim(); // Not a function
      pT_assertEquals("{name,value,comment}", kw.toString());
   },
   test_ffM_fkt_trim_quoted: function() {
      var kw = new FITSKeyword("  name  ", "'  quoted  '","  comment  ");
      kw.trim(); // Not a functio
      pT_assertEquals("{name,'  quoted  ',comment}", kw.toString());
   },

   // Check behavior of strip - removes quotes BUT ALSO trim leading space and does not handle internal quotes
   // (both is a problem, if the files are read like this)
   test_ffM_fkt_stripped_unquoted: function() {
      // Trim the value
      pT_assertEquals("aha", new FITSKeyword("name", " aha ").strippedValue);
   },
   test_ffM_fkt_stripped_quoted: function() {
      // Trim the value
      pT_assertEquals("quoted", new FITSKeyword("name", "'  quoted  '").strippedValue);
   },
   test_ffM_fkt_stripped_quoted_inside: function() {
      // Trim the value
      pT_assertEquals("quot''ed", new FITSKeyword("name", "'  quot''ed  '").strippedValue);
   },
   test_ffM_fkt_stripped_numeric: function() {
      // Trim the value
      pT_assertEquals("123", new FITSKeyword("name", "123").strippedValue);
      pT_assertEquals("string", typeof (new FITSKeyword("name", "123").strippedValue));
   },
   test_ffM_fkt_stripped_boolean: function() {
      // Trim the value
      pT_assertEquals("T", new FITSKeyword("name", "T").strippedValue);
   },



   // Test type recognition
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
      // trimmed space is null - this is a problem
      var kw = new FITSKeyword("name"," ");
      kw.trim();
      pT_assertTrue(kw.isNull);
   },
   // Boolean is FITS T,F (trimmed) not the Javascript true/false values or empty !
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
   // Non numeric value show as 0, but do have isNumeric false
   test_ffM_fkt_boolean: function() {
      pT_assertEquals(0, new FITSKeyword("name", "T").numericValue);
   },


   // Test loading keywords from file by PI
   test_ffM_compare_files_simple: function() {
      pT_compareTwoLoads(26,PROJECT_IMAGE_DIR+ "/" + "m31_Green_0028.fit");
   },
   test_ffM_compare_files_hierarch: function() {
      pT_compareTwoLoads(151,PROJECT_IMAGE_DIR+ "/" + "dsaI_0008.fits");
   },
   test_ffM_compare_files_manycases: function() {
      pT_compareTwoLoads(151,PROJECT_IMAGE_DIR+ "/" +"manycases.fits");
   },


   // Test of ffM_unquote
   test_ffM_unquote_null : function() {
      pT_assertNull(ffM_FITS_Keywords.UT.unquote(null));
   },
   test_ffM_unquote_number : function() {
      pT_assertEquals("1234",ffM_FITS_Keywords.UT.unquote("1234"));
   },
   test_ffM_unquote_true : function() {
      pT_assertEquals("T",ffM_FITS_Keywords.UT.unquote("T"));
   },
   test_ffM_unquote_non_fits_string : function() {
      // This should not occurs
      pT_assertEquals(" abc ",ffM_FITS_Keywords.UT.unquote(" abc "));
   },
   test_ffM_unquote_non_trimmed_string : function() {
      // This should not occurs
      pT_assertEquals(" ' abc ' ",ffM_FITS_Keywords.UT.unquote(" ' abc ' "));
   },
   // Various case of unuqoting
   test_ffM_unquoted_string_simple : function() {
      pT_assertEquals("abc",ffM_FITS_Keywords.UT.unquote("'abc'"));
   },
   test_ffM_unquoted_string_trim_tail : function() {
      pT_assertEquals("  abc",ffM_FITS_Keywords.UT.unquote("'  abc   '"));
   },
   test_ffM_unquoted_string_quote_1 : function() {
      pT_assertEquals("ab'c",ffM_FITS_Keywords.UT.unquote("'ab''c'"));
   },
   test_ffM_unquoted_string_quote_2 : function() {
      pT_assertEquals("ab''c",ffM_FITS_Keywords.UT.unquote("'ab''''c'"));
   },
   test_ffM_unquoted_string_quote_start_end : function() {
      pT_assertEquals("'abc'",ffM_FITS_Keywords.UT.unquote("'''abc''"));
   },
   test_ffM_unquoted_string_space_only : function() {
      pT_assertEquals(" ",ffM_FITS_Keywords.UT.unquote("'    '"));
   },
   test_ffM_unquoted_string_one_space : function() {
      pT_assertEquals(" ",ffM_FITS_Keywords.UT.unquote("' '"));
   },
   // This should not be converted to the 'space' string
   test_ffM_unquoted_strng_empty_string : function() {
      pT_assertEquals("",ffM_FITS_Keywords.UT.unquote("''"));
   },
   test_ffM_unquoted_many_spaces : function() {
      pT_assertEquals(" ",ffM_FITS_Keywords.UT.unquote("'        '"));
   },



   // Test the ffM_FITS_Keywords.fitsKeywords

   test_ffM_load_fits: function() {
      var path = PROJECT_IMAGE_DIR + "/" +"m31_Green_0028.fit";
      var errorList = new Array;
      var attrs = ffM_FITS_Keywords.makeImageKeywordsfromFile(path, errorList);
      pT_assertEquals(0,errorList.length);
      pT_assertEquals(26, attrs.fitsKeywordsList.length);
      // Only some are keywords with value
      pT_assertEquals(14, Object.getOwnPropertyNames(attrs.fitsKeywordsMap).length);
      pT_assertEquals(14, attrs.getNamesOfValueKeywords().length);
      // Check with an arbitratry key
      pT_assertEquals(158,attrs.getValueKeyword("NAXIS2").numericValue);
      pT_assertEquals(null,attrs.getValueKeyword("nothere"));
      pT_assertEquals(null,attrs.getValue("nothere"));

      /* Test of multiple HDU
      var errorList = new Array;
      ffM_FITS_Keywords.makeImageKeywordsfromFile("D:/Temp/source/flat-FILTER_clear-BINNING_1.fit", errorList);
      pT_assertEquals(1,errorList.length);
      */

   },

   test_ffM_load_bad_fits: function() {
     var errorList = new Array;
     try {
         ffM_FITS_Keywords.makeImageKeywordsfromFile(PROJECT_IMAGE_DIR+ "/" + "badfitsmissingend.fit", errorList);
      } catch (error) {
         pT_assertEquals(0,error.toString().indexOf("Error: Unexpected end of file reading FITS keywords"));
         return;
      }
      throw "Bad fits not detected";
   },

   // Test the ffM_FITS_Keywords.keywordSet
   test_ffM_keywordsSet: function() {
      var errorList = new Array;
      var kwof = ffM_FITS_Keywords.makeImageKeywordsfromFile(PROJECT_IMAGE_DIR+ "/" + "m31_Green_0028.fit", errorList);
      pT_assertEquals(0,errorList.length);
      var kws = ffM_FITS_Keywords.makeKeywordsSet();
      kws.putAllImageKeywords(kwof);
      pT_assertEquals(14, Object.keys(kws.allValueKeywordNames).length);
      pT_assertEquals(14, kws.allValueKeywordNameList.length);
   },


   // ---------------------------------------------------------------------------------------------------------
   // Test of FITS file and HDU functions
   // ---------------------------------------------------------------------------------------------------------

   testFITSFile: function() {
      let path = PROJECT_IMAGE_DIR+ "/" + "m31_Green_0028.fit";
      let ff = ffM_FITS_Files.makeFITSFile(path);
      pT_assertEquals(path,ff.getPath());
      ff.loadPrimaryHeader();
      pT_assertEquals(26,ff.HDUs[0].fitsKeywordsList.length);
   }
}

// Utility method to load FITS file keys by PI and by script and compare the result
function pT_compareTwoLoads(expectedNumberOfKeywords, sourceFilePath) {

      // Load by PI
      var images = ImageWindow.open( sourceFilePath,"test_ffM_compare_file_1", "", true );
      var image = images[0];
      var piKeywords = image.keywords;
      image.close();
      pT_assertEquals(expectedNumberOfKeywords, piKeywords.length);

      // Load by script
      var errorList = [];
      var jsKeywords = ffM_loadFITSKeywordsList(sourceFilePath, errorList);
      pT_assertEquals( 0, errorList.length);
      pT_assertEquals( piKeywords.length, jsKeywords.length);

      for (var i=0; i< piKeywords.length; i++) {
         var kw1 = piKeywords[i];
         var kw2 = jsKeywords[i];
#ifdef DEBUG_FITS
         debug("pT_compareTwoLoads: From PI: " + kw1  + ", from ffm: " + kw2);
#endif
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
//pT_executeTests({'test': ffM_allTests.test_ffM_compare_files_manycases});

