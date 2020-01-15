// FITSFileMannager-engine.js


// This file is part of FITSFileManager, see copyright in FITSFileManager.js


// ------------------------------------------------------------------------------------------------------------------------
// Engine
// Own and manage the file lists, their variables and the generation of the target file names.
// Own the current 'compiled' Configuration, the rules currently active with prepared parsers and lookup methods
// ------------------------------------------------------------------------------------------------------------------------

function FFM_Engine(guiParameters) {

   // This is from the GUI
   this.outputDirectory = "";
#ifdef DEBUG
   this.outputDirectory = "C:/temp";
#endif

   // Configuration related variables
   this.currentConfiguration = null;
   this.shownSyntheticKeyNames = {}; // A synthetic variable is shown in the source file table if its name is a key of this object


   // Variables that can be reset (when doing clear all)
   this.reset = function() {
      // Cache of file information. 3 parallel array. The order of the elements is usually NOT the same as the order
      // shown in the GUI input box or tree box, as the input box is sorted on a field.
      this.inputFiles = []; // Array of the full path of the input files
      this.inputFITSKeywords = [];  // Array of 'imageKeywords' for the corresponding input file
      this.inputVariables = [];  // Array of Map of stable synthethic variables for the corresponding input file
      this.inputErrors = []; // Array of errors discovered durin the addition of the file (typically error parsing the
                            //   FITS keys), so that they can be shown if the file is used

      // Global FITS key information - (the index of the name in keywordsSet.allValueKeywordNameList gives also the column offset in the GUI)
      this.keywordsSet = ffM_FITS_Keywords.makeKeywordsSet();   // array of names of all accumulated FITS keywords from all files
      this.shownFITSKeyNames = {}; // A FITSKeyWord is shown in the source file table if its name is a key of this object

      this.resetTarget();
    };

    this.resetTarget = function () {
      // The target files is a subset of the inputFiles, each file is defined by an index
      // in input file and the corresponding new file name. Unchecked files are not
      // present in the list (the corresponding values are undefined).
      // The array includes only the selected files in the order of the TreeBox (which is not the same
      // as the order of the 'inputFiles' array usually)
      // The targetFilesIndices contains the index of the corresponding target file in the inputFiles
      // array (note that not all inputFiles have a corresponging targetFile)
      // The targetFiles and errorPerFile are in the order of the inputFiles.
      // The targetFiles contains the file name without the base directory (common to all files) or null in case of error.
      // The errorPerFile contains the the error message as a String in case of error or null if no error,
      this.targetFilesIndices = [];
      this.targetFiles = [];
      this.errorPerFile = [];
      this.nmbFilesTransformed = 0;
      this.nmbFilesInError = 0;
      this.nmbFilesSkipped = 0;
      this.someFilesHaveMultipleHDU = false;
      this.someFilesAreConverted = false;
    };



   // -- Set the rules to load variables from images and context information
   // Called at initialization and in case of change of configuration with a COPY of the active configuration
   // IMPORTANT: Must be called with a COPY of the configuration to install, as we had objects that cannot be
   // edited or saved to the active configuration
    this.setConfiguration = function(configuration) {
#ifdef DEBUG
      debug("FFM_Engine.setConfiguration - " + configuration.name);
#endif
      this.currentConfiguration = configuration; // This is a working copy;

      // Complete the configuration (which is pure data) with the code to create the synthetic keywords
      ffM_variables.installParsers(this.currentConfiguration);

      // We add default keywords to show, we do not remove the previous ones
      // Set 'is visible' for the list of default keywords
      var defaultShownKeywords = ["IMAGETYP","FILTER","OBJECT"]; // TODO use or create configuration.defaultShownKeywords;
      for (var i = 0; i < defaultShownKeywords.length; ++i) {
         var name = defaultShownKeywords[i];
         this.shownFITSKeyNames[name] = true;
      }
      // Use the 'shown' attribute in the configuration to create the list of shown synthethic keywords
      this.shownSyntheticKeyNames = {};
      var vl = this.currentConfiguration.variableList;
      for (var i = 0; i<vl.length;i++) {
         var name = vl[i].name;
         if (vl[i].show) {
            this.shownSyntheticKeyNames[name] = true;
         }
      }

#ifdef DEBUG
      debug("FFM_Engine.setConfiguration - shownFITSKeyNames size",Object.keys(this.shownFITSKeyNames).length,
        "shownSyntheticKeyNames size",Object.keys(this.shownSyntheticKeyNames).length);
      // debug("FFM_Engine.setConfiguration - " + Log.pp(this.currentConfiguration));
#endif

    }

   this.isVariableVisible = function(name) {
       var v = this.shownSyntheticKeyNames.hasOwnProperty(name);
#ifdef DEBUG
      //debug("FFM_Engine.isVariableVisible -",name,v);
#endif
       return v;
   }


   // Rebuild after change of configuration (the GUI must also refresh itself))
   this.rebuildAll = function() {
      for (var i=0; i<this.inputFiles.length; i++) {
         var fileName = this.inputFiles[i];
         this.inputErrors[i] = new Array;
         this.inputFITSKeywords[i] = ffM_FITS_Keywords.makeImageKeywordsfromFile(fileName, this.inputErrors[i]);
         if (this.inputErrors[i].length == 0) {
            // Create the synthethic variables using the desired rules
            this.inputVariables[i] = ffM_variables.makeSyntheticVariables(this.inputFiles[i],
               this.inputFITSKeywords[i],
               this.currentConfiguration.variableList,
               this.inputErrors[i])
          }
       }
   }




   // -- Add a list of input files, parse their keywords and generate the value of their synthetic variables
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
            var errorList = new Array;
            var imageKeywords = ffM_FITS_Keywords.makeImageKeywordsfromFile(fileNames[i], errorList);
            this.inputFiles.push(fileNames[i]);
            this.inputFITSKeywords.push(imageKeywords);
            if (errorList.length === 0) {
               // Create the synthethic variables using the desired rules
               var variables = ffM_variables.makeSyntheticVariables(fileNames[i], imageKeywords,
                   this.currentConfiguration.variableList, errorList);
               this.inputVariables.push(variables);
            } else {
               this.inputVariables.push({});
            }

            this.inputErrors.push(errorList);
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
      this.inputErrors.splice(index,1);
   }


   // -----------------------------------------------------------------------------------------------------
   // ---  Lookup the index of each file in inputFiles (used to find the index of all files
   //      in the input TreeBox and save them, to detect change of ordering)
   this.lookupFileIndex = function(listOfFiles) {
   // -----------------------------------------------------------------------------------------------------
      var fileIndices = [];
      for (var i = 0; i < listOfFiles.length; ++i) {
         var aFile = listOfFiles[i];

         var aFileIndex = this.inputFiles.indexOf(aFile);
         if (aFileIndex < 0) {
               throw ("SCRIPT ERROR : lookupFileIndex: file not in inputFiles: " + aFile + " (" + i + ")");
         }
#ifdef DEBUG
         debug("lookupFileIndex: " + i + ": file[" + aFileIndex + "] = " + aFile);
#endif
         fileIndices.push(aFileIndex);
      }
      return fileIndices;
   }



   // -----------------------------------------------------------------------------------------------------
   // ---  Build the list of target files for the checked input files, result stored in object variables
   this.buildTargetFiles = function(listOfFiles) {
   // -----------------------------------------------------------------------------------------------------

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

      // Compile directory template and copy others templates
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

      // Get value of named variables, of 'rank', regexp results or FITS keywords (accessed by lexical scope)
      var targetDirectoryVariableResolver = function(v) {
               if (variables.hasOwnProperty(v)) {
                  return variables[v];
               } else if (v === "rank") {
                  return rankString;
               } else if (regexpVariables.hasOwnProperty(v)) {
                  return regexpVariables[v];
               } else {
                  return ffM_variables.filterFITSValue(fitsKeywords.getUnquotedValue(v));
               }
      };


      // Get value of named variables, of 'rank', 'targetDir', regexp results or FITS keywords (accessed by lexical scope)
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
                  return ffM_variables.filterFITSValue(fitsKeywords.getUnquotedValue(v));
               }
      };

      // Get value of named variables, of 'rank', 'count', regexp results or FITS keywords (accessed by lexical scope)
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
                  return ffM_variables.filterFITSValue(fitsKeywords.getUnquotedValue(v));
               }
      };


      // Calculate the transformation of each file name, in order (order is important for the group functionality)

      for (var inputOrderIndex = 0; inputOrderIndex < listOfFiles.length; ++inputOrderIndex) {

            // Get all information required on input file, its variables, FITS keys ...

            var inputFile = listOfFiles[inputOrderIndex];

            var inputFileIndex = this.inputFiles.indexOf(inputFile);
            if (inputFileIndex < 0) {
               throw ("SCRIPT ERROR : buildTargetFiles: file not in inputFiles: " + inputFile + " (" + inputOrderIndex + ")");
            }
#ifdef DEBUG
            debug("buildTargetFiles: " + inputOrderIndex + ": processing inputFile[" + inputFileIndex + "] = " + inputFile);
#endif

            var errorList = this.inputErrors[inputFileIndex].slice(0);
            // Check if there was any error during the load of the file (analysis of synthetic variables)
            if (errorList.length === 0) {
              

               // No error in input file loading , attempt to generate template

               var inputFileName =  File.extractName(inputFile);

               // The values of the stable synthetic variables are precalculated when the file is added 
               // to the input list and explictely recalculated if the configuration is changed, so we can directly use the 
               // precalculated variable bindings.
               variables = this.inputVariables[inputFileIndex];
               // FITS keywords are also stable in a file
               fitsKeywords = this.inputFITSKeywords[inputFileIndex];

               // Some variables cannot be precalculated when the file is loaded, because
               // they depend on properties that can be changed in the main dialog, like the regular expression to
               // parse file names or the order of the files or other data that may change after file is loaded.

               // Variable to represent parts of the input file name, as parsed by a user specified regexp.
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

               //   &rank;      The rank in the list of files of the file being moved/copied
               rankString = format(this.currentConfiguration.builtins.rank.format,inputOrderIndex);


               // Do the template operations (build  the new file name), keeping track of errors

               var targetDirectory = targetDirectoryCompiledTemplate.expandTemplate(errorList,targetDirectoryVariableResolver);
   #ifdef DEBUG
               debug("buildTargetFiles: expanded targetDirectory = " + targetDirectory + ", errors = " + errorList);
   #endif

               if (errorList.length ===0) {
               // Expand the groupByTemplate to form the id of the counter (targetDir may be used in the group template)
                  group = groupByCompiledTemplate.expandTemplate(errorList,groupByVariableResolver);
   #ifdef DEBUG
                  debug("buildTargetFiles: expanded group = " + group + ", errors: " + errorList.join(","));
   #endif
                  if (errorList.length === 0) {

                     // The count variable must be handled especially
                     //   &count;    The index of the file in its group, the group being defined by the group template above
                     count = 0;
                     if (countingGroups.hasOwnProperty(group)) {
                      count = countingGroups[group];
                     }
                     count ++;
                     countingGroups[group] = count;
                     countString = format(this.currentConfiguration.builtins.count.format,count);
   #ifdef DEBUG
                     debug("buildTargetFiles: for group = " + group + ", count = " + countString);
   #endif

                     var targetString = targetFileNameCompiledTemplate.expandTemplate(errorList,targetFileVariableResolver);
   #ifdef DEBUG
                     debug("buildTargetFiles: expanded targetString = " + targetString + ", errors: " + errorList.join(","));
   #endif
                     // This requires that the variable 'extension' be well defined
                     // Add a default extension
                     if (File.extractExtension(targetString).length === 0) {
                        targetString += variables['extension'];
                     }
                  }
               }
            }

            //Console.writeln("** buildTargetFiles " + inputOrderIndex + " " + inputFileIndex + " "  + " " + targetString);
            // Build target fles, errors and counters,
            // Check for conversion
            this.targetFilesIndices.push(inputFileIndex);
            if (errorList.length>0) {
               this.targetFiles.push(null);
               this.errorPerFile.push(errorList.join(", "));
               this.nmbFilesInError += 1;
            } else {
               this.targetFiles.push(targetString);
               this.errorPerFile.push(null);
               this.nmbFilesTransformed += 1;
               // debug("TG " + targetString + " ENDS "  + targetString.endsWith('.xisf') + " BEFORE " + this.someFilesAreConverted );
               if (targetString.endsWith('.xisf'))
               {
                  this.someFilesAreConverted = true;
               }
            }
         }

       
      

#ifdef DEBUG
         debug("buildTargetFiles: Total files: " + this.targetFiles.length + ",  someFilesAreConverted=" + this.someFilesAreConverted);
#endif

    }


    // --- Check that the operations can be executed for a list of files ------------------------------
    //     This assumes that the files are still in the same order and when they were created
    //     Return a list of errors, empty if there is no error and the operation is possible
    this.checkValidTargets = function(listOfFiles) {

      var errors = [];

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
   this.makeListsOfTransforms = function(listOfCheckedFiles, listOfCheckedFilesIndex) {
      var listsOfTransforms = {
         inputFileIndices: [],
         inputFiles: [],
         targetFiles: [],
         errorMessages: [],
      }

     this.buildTargetFiles(listOfCheckedFiles);

     // There must be a file in targetFiles for each file in listOfCheckedFiles,
     // Similarly for each index in listOfCheckedFilesIndex
     //for (var i = 0; i<this.targetFiles.length; i++) {
     //    Console.writeln("** makeListsOfTransforms " + listOfCheckedFiles[i] + ", " + listOfCheckedFilesIndex[i] +
     //       ", " + this.targetFiles[i]);
     //}

     for (var i = 0; i<this.targetFiles.length; i++) {
         //var index = listOfCheckedFilesIndex[i];
         var index = this.targetFilesIndices[i];
         var inputFile = this.inputFiles[index];
         var targetFile = this.targetFiles[i];
         var errorList = this.errorPerFile[i];
         //Console.writeln("** makeListsOfTransforms " + i + " " + index + " " + inputFile + " " + targetFile);
         // We want the index in the input TreeBox, not in the list of targets or inputList, to skip non checked files
         listsOfTransforms.inputFileIndices.push(listOfCheckedFilesIndex[i]);
         listsOfTransforms.inputFiles.push(inputFile);
         if (targetFile) {
            listsOfTransforms.targetFiles.push(targetFile);
            listsOfTransforms.errorMessages.push(null);
         } else {
            listsOfTransforms.targetFiles.push(null);
            listsOfTransforms.errorMessages.push(errorList);
         }
      }
      return listsOfTransforms;
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
         var targetDirectory = getDirectoryOfFileWithDriveLetter(targetFile);
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


// NOT TESTED, LIKELY INCORRECT
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

