// FITSFileMannager-engine.js


// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


// ------------------------------------------------------------------------------------------------------------------------
// Engine
// ------------------------------------------------------------------------------------------------------------------------

function FFM_Engine(guiParameters) {

   // This is from the GUI
   this.outputDirectory = "";
#ifdef DEBUG
   this.outputDirectory = "C:/temp";
#endif

   // Will be initialiezd by setConfiguration but must not be cleared by a reset() (unless it is immediately reconfigured)
   this.filterConverter = function() {throw "configuration not set - filterConverter"};
   this.typeConverter =  function() {throw "configuration not set - typeConverter"};
   this.remappedFITSkeywords = null;


   // Variables that can be reset (when doing clear all)
   this.reset = function() {
      // Cache of file information. 3 parallel array. The order of the elements is usually NOT the same as shown in the GUI
      this.inputFiles = []; // Array of the full path of the input files
      this.inputFITSKeywords = [];  // Array of 'imageKeywords' for the corresponding input file
      this.inputVariables = [];  // Array of Map of stable synthethic variables for the corresponding input file

      // Global FITS key information - (the index of the name in keywordsSet.allValueKeywordNameList give also the column offset in the GUI)
      this.keywordsSet = ffM_keywordsOfFile.makeKeywordsSet();   // array of names of all accumulated FITS keywords from all files
      this.shownFITSKeyNames = {}; // A FITSKeyWord is shown in the source file table if its name is a key of this object
      this.shownSyntheticKeyNames = {}; // A synthethic variable is shown in the source file table if its name is a key of this object

       this.resetTarget();
    };

    this.resetTarget = function () {
      // Target files is a subset of the inputFiles, each file is defined by an index
      // in input file and the corresponding new file name. Unchecked files are not
      // present in the list (the values are undefined).
      // The targetFilesIndices is in the order of the TreeBox and contain the index in the inputFiles
      // The targetFiles and errorPerFile are in the order of the inputFiles.
      // The targetFiles contains the file name without the base directory (common to all files) or null in case of error.
      // The errorPerFile contains the errors String or null if no error,
      this.targetFilesIndices = [];
      this.targetFiles = [];
      this.errorPerFile = [];
      this.nmbFilesTransformed = 0;
      this.nmbFilesInError = 0;
      this.nmbFilesSkipped = 0;
    };

    this.setConfiguration = function(configuration) {
#ifdef DEBUG
      debug("FFM_Engine.setConfiguration - " + configuration.name);
#endif
      this.filterConverter = ffM_LookupConverter.makeLookupConverter(configuration.filterConversions);
      this.typeConverter = ffM_LookupConverter.makeLookupConverter(configuration.typeConversions);
      this.remappedFITSkeywords = configuration.keywordMappingTable;

      // We add default keywords, we do not remove old ones
      // Set 'is visible' for the list of default keywords
      var defaultShownKeywords = configuration.defaultShownKeywords;
      for (var i = 0; i < configuration.defaultShownKeywords.length; ++i) {
         var name = configuration.defaultShownKeywords[i];
         this.shownFITSKeyNames[name] = true;
      }
      // Currently all synthetic variables are visible by default
      for (var i = 0; i<syntheticVariableNames.length;i++) {
         var name = syntheticVariableNames[i];
         this.shownSyntheticKeyNames[name] = true;
      }

    }


   // -- Add a list of files
   this.addFiles = function (fileNames) {

#ifdef DEBUG
      debug("addFiles: Adding "+fileNames.length + " files");
#endif

      var nmbFilesAdded = 0;
      var nmbFilesDuplicated = 0;
      for ( var i = 0; i<fileNames.length; i++ ) {
#ifdef DEBUG
         debug("addFiles: Adding file " + i + ": '" + fileNames[i] + "', first check for duplicate, if ok add at " + this.inputFiles.length);
#endif
         if (this.inputFiles.indexOf(fileNames[i]) < 0) // Skip files already in the list
         {
            var imageKeywords = ffM_keywordsOfFile.makeImageKeywordsfromFile(fileNames[i]);
            this.inputFiles.push(fileNames[i]);
            this.inputFITSKeywords.push(imageKeywords);
            // Create the synthethic variables using the desired rules
            var variables = makeSynthethicVariables(fileNames[i], imageKeywords,
                this.remappedFITSkeywords,
                this.filterConverter, this.typeConverter);

            this.inputVariables.push(variables);
            nmbFilesAdded++;
         } else {
            nmbFilesDuplicated ++;
         }
      }

      Console.writeln("" + nmbFilesAdded + " file" + (nmbFilesAdded >1?"s":"") + " added" +
         (nmbFilesDuplicated>0? (", " + nmbFilesDuplicated + " duplicated file(s) ignored.") : "."));

   }

   // -- Remove a file by name
   this.removeFiles = function (fileName) {
      var index = this.inputFiles.indexOf(fileName);
      if (index < 0) {
         throw ("SCRIPT ERROR : removeFiles: file " + fileName + " not in inputFiles");
      }
      this.inputFiles.splice(index,1);
      this.inputFITSKeywords.splice(index,1);
      this.inputVariables.splice(index,1);
   }



   // ---  Build the list of target files for the checked input files, result stored in object variables
   this.buildTargetFiles = function(listOfFiles) {

#ifdef DEBUG
      debug("buildTargetFiles: list of " + listOfFiles.length + " files");
      debug("buildTargetFiles: targetFileNameTemplate = '" + guiParameters.targetFileNameCompiledTemplate.templateString  + "'");
      debug("buildTargetFiles: sourceFileNameRegExp = '" + guiParameters.sourceFileNameRegExp + "'");
      debug("buildTargetFiles: groupByTemplate = '" + guiParameters.groupByCompiledTemplate.templateString  + "'");
#endif

      this.resetTarget();


      // Separate directory from file name part in target template
      var targetFileNameTemplateString = guiParameters.targetFileNameCompiledTemplate.templateString;
      var indexOfLastSlash = targetFileNameTemplateString.lastIndexOf('/');
      if (indexOfLastSlash>0) {
         var targetDirectoryTemplate= targetFileNameTemplateString.substring(0,indexOfLastSlash);
         var targetNameTemplate= targetFileNameTemplateString.substring(indexOfLastSlash+1);
      } else {
         var targetDirectoryTemplate = '';
         var targetNameTemplate= targetFileNameTemplateString;
      }
#ifdef DEBUG
      debug("buildTargetFiles: targetDirectoryTemplate = '" + targetDirectoryTemplate + "', targetNameTemplate = '" +  targetNameTemplate + "'");
#endif

      // Compile directory template (copy others)
      var templateErrors = [];
      var targetDirectoryCompiledTemplate = ffM_template.analyzeTemplate(templateErrors,targetDirectoryTemplate);
      var groupByCompiledTemplate = guiParameters.groupByCompiledTemplate;
      var targetFileNameCompiledTemplate = guiParameters.targetFileNameCompiledTemplate;

#ifdef DEBUG
      debug("buildTargetFiles: targetDirectoryCompiledTemplate = " + targetDirectoryCompiledTemplate);
      debug("buildTargetFiles: groupByCompiledTemplate = " + groupByCompiledTemplate);
      debug("buildTargetFiles: targetFileNameCompiledTemplate = " + targetFileNameCompiledTemplate);
#endif

      // A map of group count values
      var countingGroups = {};


      // Variables used in the loop
      var group;
      var count;
      var expansionErrors;
      // For use by variable resolver (they are updated at each loop iteration and accessed by the lexical scope)
      var variables;
      var rankString;
      var countString;
      var fitsKeywords;

      // Replace variables, rank and regexp results (accessed by lexical scope)
      var targetDirectoryVariableResolver = function(v) {
               if (variables.hasOwnProperty(v)) {
                  return variables[v];
               } else if (v === "rank") {
                  return rankString;
               } else if (regexpVariables.hasOwnProperty(v)) {
                  return regexpVariables[v];
               } else {
                  return filterFITSValue(fitsKeywords.getUnquotedValue(v));
               }
      };


      // Replace variables, rank, regexp results and targetDir (accessed by lexical scope)
      var groupByVariableResolver = function(v) {
               if (variables.hasOwnProperty(v)) {
                  return variables[v];
               } else if (v === "rank") {
                  return rankString;
               } else if (v === "targetDir") {
                  return targetDirectory;
               } else if (regexpVariables.hasOwnProperty(v) ) {
                  return regexpVariables[v];
               } else {
                  return filterFITSValue(fitsKeywords.getUnquotedValue(v));
               }
      };

      // Replace variables, rank, regexp results and count (accessed by lexical scope)
      var targetFileVariableResolver = function(v) {
               if (variables.hasOwnProperty(v)) {
                  return variables[v];
               } else if (v === "rank") {
                  return rankString;
               } else if (v === "count") {
                  return countString;
               } else if (regexpVariables.hasOwnProperty(v) ) {
                  return regexpVariables[v];
               } else {
                  return filterFITSValue(fitsKeywords.getUnquotedValue(v));
               }
      };



      for (var inputOrderIndex = 0; inputOrderIndex < listOfFiles.length; ++inputOrderIndex) {

            var inputFile = listOfFiles[inputOrderIndex];

            var inputFileIndex = this.inputFiles.indexOf(inputFile);
            if (inputFileIndex < 0) {
               throw ("SCRIPT ERROR : buildTargetFiles: file not in inputFiles: " + inputFile + " (" + inputOrderIndex + ")");
            }
#ifdef DEBUG
            debug("buildTargetFiles: " + inputOrderIndex + ": processing inputFile[" + inputFileIndex + "] = " + inputFile);
#endif

            var inputFileName =  File.extractName(inputFile);

            variables = this.inputVariables[inputFileIndex];
            fitsKeywords = this.inputFITSKeywords[inputFileIndex];


            // The file name part is calculated at each scan as the regxep may have been changed
            //   &1; &2;, ... The corresponding match from the sourceFileNameRegExp
            var regexpVariables = [];
            if (guiParameters.sourceFileNameRegExp !== null) {
               var inputFileNameMatch = guiParameters.sourceFileNameRegExp.exec(inputFileName);
#ifdef DEBUG
               debug ("buildTargetFiles: inputFileNameMatch= " + inputFileNameMatch);
#endif
               if (inputFileNameMatch !== null) {
                  for (var j = 0; j<inputFileNameMatch.length; j++) {
                     regexpVariables[j.toString()] = inputFileNameMatch[j]
                  }
               }
            }
            //   &rank;      The rank in the list of files of the file being moved/copied, padded to COUNT_PAD.
            rankString = inputOrderIndex.pad(FFM_COUNT_PAD);


            expansionErrors = [];
            var targetDirectory = targetDirectoryCompiledTemplate.expandTemplate(expansionErrors,targetDirectoryVariableResolver);
#ifdef DEBUG
            debug("buildTargetFiles: expanded targetDirectory = " + targetDirectory + ", errors = " + expansionErrors);
#endif

            if (expansionErrors.length ===0) {
            // Expand the groupByTemplate to form the id of the counter (targetDir may be used)
               group = groupByCompiledTemplate.expandTemplate(expansionErrors,groupByVariableResolver);
#ifdef DEBUG
               debug("buildTargetFiles: expanded group = " + group + ", errors: " + expansionErrors.join(","));
#endif
               if (expansionErrors.length === 0) {

                  //   &count;    The number of file in the same group
                  count = 0;
                  if (countingGroups.hasOwnProperty(group)) {
                   count = countingGroups[group];
                  }
                  count ++;
                  countingGroups[group] = count;
                  countString = count.pad(FFM_COUNT_PAD);
#ifdef DEBUG
                  debug("buildTargetFiles: for group = " + group + ", count = " + countString);
#endif

                  var targetString = targetFileNameCompiledTemplate.expandTemplate(expansionErrors,targetFileVariableResolver);
#ifdef DEBUG
                  debug("buildTargetFiles: expanded targetString = " + targetString + ", errors: " + expansionErrors.join(","));
#endif
                  // Add a default extension
                  if (File.extractExtension(targetString).length === 0) {
                     targetString += variables['extension'];
                  }
               }
            }

            this.targetFilesIndices.push(inputFileIndex);
            if (expansionErrors.length>0) {
               this.targetFiles.push(null);
               this.errorPerFile.push(expansionErrors.join(", "));
               this.nmbFilesInError += 1;
            } else {
               this.targetFiles.push(targetString);
               this.errorPerFile.push(null);
               this.nmbFilesTransformed += 1;
            }
         }
#ifdef DEBUG
         debug("buildTargetFiles: Total files: " + this.targetFiles.length);
#endif

    }



    // --- Check that the operations can be executed for a list of files ------------------------------
    this.checkValidTargets = function(listOfFiles) {

      var errors = [];

      // Check if files are still in the same order, otherwise the &count; may have changed
      for (var i = 0; i < listOfFiles.length; ++i) {
         var inputFile = listOfFiles[i];
         var inputFileIndex = this.inputFiles.indexOf(inputFile);
         if (inputFileIndex < 0) {
            throw ("SCRIPT ERROR : check: file not in inputFiles: " + inputFile + " (" + i + ")");
         }
         if (this.targetFilesIndices.length<i ||
            this.targetFilesIndices[i] !== inputFileIndex) {
            // Sort order changed
            return ["The order of some column changed since last refresh, please refresh"];
         }
      }

      for (var i = 0; i < listOfFiles.length; ++i) {
         var inputFile = listOfFiles[i];
         if (! File.exists(inputFile)) {
            errors.push("File '"  + inputFile + "' is not present any more, please refresh'");
         }
      }

      // Check if any file is in error
      for (var i=0; i<this.errorPerFile.length; i++) {
         var index = this.targetFilesIndices[i];
         var inputFile = this.inputFiles[index];
         if (this.errorPerFile[i]) {
            errors.push("File '"+ inputFile + "' has variable expansion error");
         }
      }

      // Check duplicates target names
      var targetFileNameInputFile = {};
      for (var i=0; i<this.targetFiles.length; i++) {
         var index = this.targetFilesIndices[i];
         var targetString = this.targetFiles[i];
         // Null are skipped files or files in error
         if (targetString !== null) {
            var inputFile = this.inputFiles[index];
            if (targetFileNameInputFile.hasOwnProperty(targetString)) {
               errors.push("File '"  + inputFile + "' generates same file '" + targetString + "' as '" + targetFileNameInputFile[targetString] +"'");
            }
            targetFileNameInputFile[targetString] = inputFile;
         }
      }

      // Check bad names (empty, /, ...)

      // Check existing target files
      for (var i=0; i<this.targetFiles.length; i++) {
         var index = this.targetFilesIndices[i];
         var targetString = this.targetFiles[i];
         // Null are skipped files or files in error
         if (targetString !== null) {
            var inputFile = this.inputFiles[index];
          var targetFilePath = this.outputDirectory + "/" + targetString;
            if (File.exists(targetFilePath)) {
               errors.push("File '"  + inputFile + "' generates the already existing file '" + targetFilePath + "'");
            }
         }
      }

      return errors;
    }



    // -- Make List of text accumulating the transformation rules for display --------------
    this.makeListOfTransforms = function() {
      var listOfTransforms = [];
      for (var i = 0; i<this.targetFiles.length; i++) {
         var index = this.targetFilesIndices[i];
         var inputFile = this.inputFiles[index];
         var targetFile = this.targetFiles[i];
         var errorList = this.errorPerFile[i];
         if (targetFile) {
            listOfTransforms.push("File ".concat(inputFile));
            listOfTransforms.push("  to .../".concat(targetFile));
         } else {
            listOfTransforms.push("File ".concat(inputFile));
            listOfTransforms.push("     Error ".concat(errorList));
         }
      }
      return listOfTransforms;
    }


   // -- Execute copy, move or loadSave operation ----------------------------------------------------
   // Return a text that may be show to the user
   this.executeFileOperations = function (engine_mode) {

      var count = 0;

      for (var i=0; i<this.targetFiles.length; i++) {

         var index = this.targetFilesIndices[i];
         var targetString = this.targetFiles[i];
         var inputFile = this.inputFiles[index];

         var targetFile = this.outputDirectory + "/" + targetString;

#ifdef DEBUG
         debug("executeFileOperations: targetFile = " + targetFile );
#endif
         var targetDirectory = File.extractDrive(targetFile) +  File.extractDirectory(targetFile);
#ifdef DEBUG
         debug("executeFileOperations: targetDirectory = " + targetDirectory );
#endif

         // Create target directory if required
         if (!File.directoryExists(targetDirectory)) {
            console.writeln("mkdir " + targetDirectory);
            if (EXECUTE_COMMANDS) {
                  File.createDirectory(targetDirectory, true);
            } else {
                  console.writeln("*** COMMAND NOT EXECUTED - EXECUTE_COMMANDS IS FALSE FOR DEBUGGING PURPOSE");
            }
         }

         // TO BE ON SAFE SIDE, was already checked
         if (File.exists(targetFile)) {
         for ( var u = 1; ; ++u )  {
            for( var n = u.toString(); n.length < 4 ; n = "0" + n);
            // TODO This does not take 'extension' into account
               var tryFilePath = File.appendToName( targetFile, '-' + n );
#ifdef DEBUG
               debug("executeFileOperations: tryFilePath= " + tryFilePath );
#endif
               if ( !File.exists( tryFilePath ) ) { targetFile = tryFilePath; break; }
            }
         }

         switch (engine_mode) {
            case 0:
            console.writeln("move " + inputFile +"\n  to "+ targetFile);
            if (EXECUTE_COMMANDS) File.move(inputFile,targetFile);
            break;
         case 1:
            console.writeln("copy " + inputFile+"\n  to "+ targetFile);
            if (EXECUTE_COMMANDS)  copyFile(inputFile,targetFile);
            break;
         case 2:
            console.writeln("load  " + inputFile+"\n write "+ targetFile);
            if (EXECUTE_COMMANDS)  loadSaveFile(inputFile,targetFile);
            break;

         }

         count ++;

         // May be this allows abort ?
         processEvents();
         // May be useful as we load /save a lot of data or images
         gc();

      }
      var action = ["moved","copied","load/saved"][engine_mode];
      var text = count.toString() + " checked file(s) where " + action + " out of " + this.inputFiles.length + " file(s) in input list";
      console.writeln(text);
      return text;
   };

#ifdef IMPLEMENTS_FITS_EXPORT
   // -- Export the keywords of a list of files
   this.exportFITSKeywords = function() {
      var tab = String.fromCharCode(9);
      var f = new File();
      var fileName = "FITS_keys";
      var fileDir = this.outputDirectory;
      var t = fileDir + "/" + fileName + ".txt";
      // Create numbered file nameto create new file
      if ( File.exists( t ) ) {
         for ( var u = 1; ; ++u ) {
            for( var n = u.toString(); n.length < 4 ; n = "0" + n);
            var tryFilePath = File.appendToName( t, '-' + n );
            if ( !File.exists( tryFilePath ) ) { t = tryFilePath; break; }
         }
      }
      f.create(t);

      // output header (tab separated selected fits keyword + 'Filename')
      var allFITSKeyNames = this.keywordsSet.allValueKeywordNameList;
      for ( var i =0; i<allFITSKeyNames.length; i++) {
         var name = allFITSKeyNames[i];
         if (this.shownFITSKeyNames.hasOwnProperty(name)) {
            f.outTextLn(key+tab);
         }
      }
      f.outTextLn("Filename"+String.fromCharCode(10,13)); // LF, CR

      // output FITS data
      for ( var j =0; j< this.targetFilesIndices.length; j++) {
         var inputIndex = this.targetFilesIndices[i];

         // LIKEY NOT CORRECT
         var key = this.inputFITSKeywords[inputIndex].fitsKeywordsList;
         for ( var i = 0; i< allFITSKeyNames.length; i++) {

            var name = allFITSKeyNames[i];
            if (!this.shownFITSKeyNames.hasOwnProperty(name)) continue;
            for (var k in key) {
               if (!(key[k].name === name)) continue;
               if (key[k].isNumeric) {
                  var value = parseFloat(key[k].value)
               } else {
                  var value = key[k].value;
                  value = value.replace( /'/g, "" );
                  value = value.replace( / /g, "" ); //delete left space
                  value = value.replace( /:/g, "." );
               }

               f.outText(value.toString());
               for (var w = value.toString().length; w < 8; w++) f.outText(" ");
               f.outText(tab);
               k=-1;
               break;
            }
            if (k > -1) f.outText("        "+tab);
         }
         f.outTextLn(this.inputFiles[j]+String.fromCharCode(10,13));
      }
      f.close();
      console.writeln("FITSKeywords saved to ",t);
   }
#endif

   // -- Return true if move or copy is possible
   this.canDoOperation = function() {
         return !((!this.inputFiles.length) || (!this.outputDirectory));
   }

   // ----------------------------------------------------------------------------------------------------
   this.reset();
   // ----------------------------------------------------------------------------------------------------

}

