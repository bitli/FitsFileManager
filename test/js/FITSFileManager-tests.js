"use strict";

// FITSFileManager-tests


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


#include "PJSR-unit-tests-support.jsh"

#define DEBUG true


#define VERSION "0.8-tests"

#include "../../main/js/FITSFileManager-helpers.jsh"
#include "../../main/js/FITSFileManager-engine.jsh"
#include "../../main/js/FITSFileManager-parameters.jsh"
#include "../../main/js/FITSFileManager-gui.jsh"



// ---------------------------------------------------------------------------------------------------------
// Unit tests
// ---------------------------------------------------------------------------------------------------------

// Variable resolver factory for a resolver using a single object,
// to make mock variable resolvers for the tests
var ffM_rv = function(obj) {
   return (function(v) {
      if (obj.hasOwnProperty(v)) {
         return obj[v];
      } else {
         // do not use 'undefined' to be 'use strict' friendly
         return null;
      }
   });
}


var ffM_allTests = {
   // Test our mock variable resolver
   test_ffM_rv_found: function() {
      pT_assertEquals("toto", ffM_rv({titi: "toto"})("titi"));
   },
   test_ffM_rv_not_found: function() {
      pT_assertNull(ffM_rv({titi: "toto"})("toto"));
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of converter
   // Note that converter first test of the MATCH (anyhwere) of the regexp,
   // then replace by the conversion result, doing pattern replacement ON THE CONVERSION RESULT
   // not on the string received
   // ---------------------------------------------------------------------------------------------------------
   testConverter_empty: function () {
      var c = ffM_LookupConverter.makeLookupConverter([]);
      pT_assertNull(c.convert("thereIsNoConversionDefined"));
   },
   testConverter_single_conversion: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /abc/, "matched"]
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("matched",c.convert("abc"));
   },
   testConverter_valid_second_conversion: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /first/, "match1"],
            [ /second/, "match2" ],
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("match2",c.convert("second"));
      pT_assertEquals("match1",c.convert("first"));
   },
   testConverter_valid_last_conversion: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /first/,  "match1"],
            [ /second/, "match2" ],
            [ /third/,  "match3" ]
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("match3",c.convert("third"));
      pT_assertEquals("match2",c.convert("second"));
      pT_assertEquals("match1",c.convert("first"));
   },
   testConverter_detect_partial: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /TEXT/, "matched"]
         ]);
      pT_assertNull(c.convert("nomatch"));
      pT_assertEquals("matched",c.convert("aTEXTinside"));
   },
   testConverter_obey_anchored: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /^TEXT/, "matched"]
         ]);
      pT_assertNull(c.convert("aTEXTinside"));
   },
   testConverter_obey_ignore_case: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /TEXT/i, "Matched"]
         ]);
         pT_assertEquals("Matched",c.convert("isText"));
         pT_assertEquals("Matched",c.convert("isTEXT"));
         pT_assertEquals("Matched",c.convert("is-text"));
   },

   // Last chance handler
   testConverter_sourceGroupReference_match_any_char: function () {
      // This case &0; is optimized
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /TEXT/, "specificMatch"],
            [ /./, "&0;"] // Match any character
         ]);
      pT_assertEquals("allchars",c.convert("allchars"));
      pT_assertEquals("specificMatch",c.convert("aTEXT"));
   },
   testConverter_sourceGroupReference_match_any: function () {
      // This case &0; is optimized
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /TEXT/, "specificMatch"],
            [ /.*/, "&0;" ]
         ]);
      pT_assertEquals("allchars",c.convert("allchars"));
      pT_assertEquals("specificMatch",c.convert("aTEXT"));
   },

   testConverter_sourceGroupReference_whole_inside: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /.*/, "_&0;/"]
         ]);
      pT_assertEquals("_red/",c.convert("red"));
   },
   // Not clear if it should do it
   testConverter_sourceGroupReference_convert_lower_case: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /.*/, "&0;"]
         ]);
      pT_assertEquals("lowerupper",c.convert("lowerUPPER"));
   },

   testConverter_sourceGroupReference_subgroup: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /TEXT/, "specificMatch"],
            [ /filter-(.*)/, "_&1;/"]
         ]);
      pT_assertEquals("specificMatch",c.convert("TEXT"));
      pT_assertEquals("_red/",c.convert("filter-red"));
   },
   testConverter_multiple_back_references: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /FIRST/, "&0;"],
            [ /SECOND/i, "&0;"],
            [ /THIRD/i, "&0;"],
         ]);
      pT_assertNull(c.convert("nomatch"));
      // Currently back refernce as convert to lower case
      pT_assertEquals("afirst",c.convert("aFIRST"));
      pT_assertEquals("thesecond",c.convert("THEsecond"));
      pT_assertEquals("isthird",c.convert("isThird"));
      pT_assertNull(c.convert("nomatchagain"));
   },
   testConverter_sourceGroupReference_multiple_ref: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /([0-9]+)-filter-([a-z]+)/, "&2;_&1;/"],
         ]);
      pT_assertEquals("green_450/",c.convert("450-filter-green"));
   },
   testConverter_sourceGroupReference_multiple_ref_and_tests: function () {
      var c = ffM_LookupConverter.makeLookupConverter([
            [ /BEFORE/, "specificMatchBefore"],
            [ /([0-9]+)-filter-([a-z]+)/, "&2;_&1;/"],
            [ /AFTER/, "specificMatchAfter"],
            [ /UNUSED/, "unused"],
         ]);
      pT_assertEquals("specificMatchBefore",c.convert("BEFORE"));
      pT_assertEquals("specificMatchAfter",c.convert("AFTER"));
      pT_assertEquals("red_123/",c.convert("123-filter-red"));
   },
   // This may help discover subtle bugs with improepr lexical scoping
   testConverter_twoOfThem: function () {
      var c1 = ffM_LookupConverter.makeLookupConverter([
            [ /abc/,   "toto"],
            [ /def/,   "ti&0;ti"]
         ]);
      var c2 = ffM_LookupConverter.makeLookupConverter([
            [ /123/, "NUMBER"]
         ]);
      pT_assertNull(c1.convert("123"));
      pT_assertEquals("tidefti",c1.convert("def"));
      pT_assertNull(c2.convert("abc"));
      pT_assertEquals("NUMBER",c2.convert("hereAnumber:123"));
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of filter conversion
   // ---------------------------------------------------------------------------------------------------------
   testConvertFilter_base: function () {
      var f = convertFilter("some blue");
      pT_assertEquals("blue",f);
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of type conversion
   // ---------------------------------------------------------------------------------------------------------
   testConvertType_base: function () {
      var t = convertType("FLAT");
      pT_assertEquals("flat",t);
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of template mechanism
   // ---------------------------------------------------------------------------------------------------------

   testTemplate_text: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"textonly");
      pT_assertEquals("literalRule('textonly')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_inside: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_start: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&VAR;atend");
      pT_assertEquals("lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()]),literalRule('atend')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_end: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"atbeg&VAR;");
      pT_assertEquals("literalRule('atbeg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()])",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_alone: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&VAR;");
      pT_assertEquals("lookUpRule('VAR':[onFound:copyValue()]:[onMissing:reject()])",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_two: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR1;middle&VAR2;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR1':[onFound:copyValue()]:[onMissing:reject()]),literalRule('middle'),lookUpRule('VAR2':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_together: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR1;&VAR2;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR1':[onFound:copyValue()]:[onMissing:reject()]),lookUpRule('VAR2':[onFound:copyValue()]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },

   testTemplate_present: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR:present;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:formatValueAs('present')]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_missing: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR?missing;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:copyValue()]:[onMissing:copyLiteral('undefined')]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_present_missing: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&VAR:present?missing;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('VAR':[onFound:formatValueAs('present')]:[onMissing:copyLiteral('present')]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_none: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&NONE:;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('NONE':[onFound:copyLiteral('')]:[onMissing:reject()]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_opt: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&OPT?;end");
      pT_assertEquals("literalRule('beg'),lookUpRule('OPT':[onFound:copyValue()]:[onMissing:copyLiteral('')]),literalRule('end')",t.toString());
      pT_assertEquals(0, templateErrors.length);
   },
   testTemplate_error: function () {
      var templateErrors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"beg&no-semi-colon-end");
      pT_assertEquals(1, templateErrors.length);
   },

   // Test resolver
   testResolve_text: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"textonly");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("textonly",t.expandTemplate(errors,null));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("value",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var:found;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("found",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found_2: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var:found?notfound;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("found",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_found_empty: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var:;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("",t.expandTemplate(errors,ffM_rv({'var':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_not_found: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var?notfound;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("notfound",t.expandTemplate(errors,ffM_rv({'NOPE':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_optional: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"&var?;");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("",t.expandTemplate(errors,ffM_rv({'NOPE':'value'})));
      pT_assertEquals(0, errors.length);
   },
   testResolve_var_complex: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"AB,&v1;,CD,&v2:v2fmt?v2missing;,EF,&v3?;,GH,&v4?v4opt;,IJ,&v5;,KL");
      pT_assertEquals(0, templateErrors.length);
      pT_assertEquals("AB,v1val,CD,v2fmt,EF,,GH,v4opt,IJ,v5val,KL",
            t.expandTemplate(errors,ffM_rv({v1:'v1val', v2:'NO', v5:'v5val'})));
      pT_assertEquals(0, errors.length);
   },

   testResolve_error: function () {
      var templateErrors = [];
      var errors = [];
      var t = ffM_template.analyzeTemplate(templateErrors,"abc&required;def");
      pT_assertEquals(0, templateErrors.length);
      t.expandTemplate(errors,ffM_rv({v1:'v1val', v2:'NO', v5:'v5val'}));
      pT_assertEquals("No value for the variable 'required'", errors.join(""));
   },

   // ---------------------------------------------------------------------------------------------------------
   // Test of gui parameters and settings
   // ---------------------------------------------------------------------------------------------------------
   testSettings: function () {
      var guiParameters = new FFM_GUIParameters();
      // This ensures that there are default settings in first execution
      guiParameters.loadSettings();
      guiParameters.reset();
      guiParameters.saveSettings();

      guiParameters.loadSettings();
      // TODO make some real tests
     // pT_assertEquals("string",guiParameters.targetFileNameCompiledTemplate);
   },




}




// ---------------------------------------------------------------------------------------------------------
// Auto execute tests on load
// ---------------------------------------------------------------------------------------------------------



pT_executeTests(ffM_allTests);

