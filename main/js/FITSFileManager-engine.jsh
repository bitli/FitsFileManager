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

   // TODO Make a global parameter
   this.defaultKey = ["SET-TEMP","EXPOSURE","IMAGETYP","FILTER  ", "XBINNING","YBINNING"];


   // Variables that can be reset (when doing clear all)
   this.reset = function() {
      // Cache of file information. 3parallel arrays, the order is usually NOT the same as in the GUI
      this.inputFiles = []; //Array of filename with full path
      this.inputKeys = [];  // Array of an array of FITSKeywords for the corresponding file
      this.inputVariables = [];  // Array of Map of stable variables for the corresponding file

      // Cache global FITS key information (two parallel arrays)
      this.keyTable = [];   //accumulated names of keywords from all files
      this.keyEnabled = []; //true === selected keywords

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

   // -- Add a list of files
   this.addFiles = function (fileNames) {

#ifdef DEBUG
      debug("addFiles: Adding "+fileNames.length + " files");
#endif

      var nmbFilesAdded = 0;
      var nmbFilesDuplicated = 0;
      for ( var i = 0; i<fileNames.length; i++ ) {
#ifdef DEBUG
         debug("addFiles: Check for duplicate and add fileNames[" + i + "] " + fileNames[i]);
#endif
         if (this.inputFiles.indexOf(fileNames[i]) < 0) //Add file only one times
         {
            var keys = loadFITSKeywords(fileNames[i]);
            this.inputFiles.push(fileNames[i]);
            this.inputKeys.push(keys);
            var variables = makeSynthethicVariables(fileNames[i], keys);

            this.inputVariables.push(variables);
            nmbFilesAdded++;
         } else {
            nmbFilesDuplicated ++;
         }
      }

      Console.writeln("" + nmbFilesAdded + " files added, " + nmbFilesDuplicated + " duplicated file ignored");

   }

   // -- Remove a file by name
   this.removeFiles = function (fileName) {
      var index = this.inputFiles.indexOf(fileName);
      if (index < 0) {
         throw ("SCRIPT ERROR : removeFiles: file " + fileName + " not in inputFiles");
      }
      this.inputFiles.splice(index,1);
      this.inputKeys.splice(index,1);
      this.inputVariables.splice(index,1);
   }



   // --  Build the list of target files for the checked input files, result stored in object variables
   this.buildTargetFiles = function(listOfFiles) {

#ifdef DEBUG
      debug("buildTargetFiles: list of " + listOfFiles.length + " files");
      debug("buildTargetFiles: targetFileNameTemplate = '" + guiParameters.targetFileNameCompiledTemplate.templateString  + "'");
      debug("buildTargetFiles: sourceFileNameRegExp = '" + guiParameters.sourceFileNameRegExp + "'");
      debug("buildTargetFiles: groupByTemplate = '" + guiParameters.groupByCompiledTemplate.templateString  + "'");
#endif

      this.resetTarget();

      // A map of group count values
      var countingGroups = {};


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
      var targetDirectoryCompiledTemplate = ffM_template.analyzeTemplate(targetDirectoryTemplate);
      var groupByCompiledTemplate = guiParameters.groupByCompiledTemplate;
      var targetFileNameCompiledTemplate = guiParameters.targetFileNameCompiledTemplate;

#ifdef DEBUG
      debug("buildTargetFiles: targetDirectoryCompiledTemplate = " + targetDirectoryCompiledTemplate);
      debug("buildTargetFiles: groupByCompiledTemplate = " + groupByCompiledTemplate);
      debug("buildTargetFiles: targetFileNameCompiledTemplate = " + targetFileNameCompiledTemplate);
#endif


      // Count and groups are initialized inside each loop, declared here for clarity
      var count = 0;
      var group = '';
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

            var variables = this.inputVariables[inputFileIndex];

            var variableResolver = function(v) {
               if (variables.hasOwnProperty(v)) {
                  return variables[v];
               } else {
                  return null;
               }
            }


            //   &rank;      The rank in the list of files of the file being moved/copied, padded to COUNT_PAD.
            variables['rank'] = inputOrderIndex.pad(FFM_COUNT_PAD);

            // The file name part is calculated at each scan as the regxep may have been changed
            // TODO Optimize this maybe, clear the numbered variables of a previous scan
            //   &1; &2;, ... The corresponding match from the sourceFileNameRegExp
            if (guiParameters.sourceFileNameRegExp !== null) {
               var inputFileNameMatch = guiParameters.sourceFileNameRegExp.exec(inputFileName);
#ifdef DEBUG
               debug ("buildTargetFiles: inputFileNameMatch= " + inputFileNameMatch);
#endif
               if (inputFileNameMatch !== null) {
                  for (var j = 0; j<inputFileNameMatch.length; j++) {
                     variables[j.toString()] = inputFileNameMatch[j]
                  }
               }
            }

            // Use only directory part, count should not be used, used to initialize 'targetdir'
            variables['count'] = 'COUNT';


            var expansionErrors = [];
            var targetDirectory = targetDirectoryCompiledTemplate.expandTemplate(expansionErrors,variableResolver);
#ifdef DEBUG
            debug("buildTargetFiles: expanded targetDirectory = " + targetDirectory + ", errors = " + expansionErrors);
#endif
            variables['targetDir'] = targetDirectory;

            // Expand the groupByTemplate to form the id of the counter (targetDir may be used)
            var expansionErrors = [];
            group = groupByCompiledTemplate.expandTemplate(expansionErrors,variableResolver);
#ifdef DEBUG
            debug("buildTargetFiles: expanded group = " + group + ", errors: " + expansionErrors.join(","));
#endif
            count = 0;
            if (countingGroups.hasOwnProperty(group)) {
               count = countingGroups[group];
            }
            count ++;
            countingGroups[group] = count;
            variables['count'] = count.pad(FFM_COUNT_PAD);
#ifdef DEBUG
            debug("buildTargetFiles: for group = " + group + ", count = " + variables['count']);
#endif

            // We should not use 'targetDir' in the expansion of the file name
            variables['targetDir'] = 'TARGETDIR';
            // The resulting name may include directories

            var expansionErrors = [];
            var targetString = targetFileNameCompiledTemplate.expandTemplate(expansionErrors,variableResolver);
#ifdef DEBUG
            debug("buildTargetFiles: expanded targetString = " + targetString + ", errors: " + expansionErrors.join(","));
#endif

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
         debug("buildTargetFiles: Total files: ", this.targetFiles.length);
#endif

    }

    // Check that the operations can be executed for a list of files ------------------------------
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
            listOfTransforms.push("File ".concat(inputFile, "\n  to .../",targetFile, "\n"));
         } else {
            listOfTransforms.push("File ".concat(inputFile, "\n     Error: ",errorList, "\n"));
         }
      }
      return listOfTransforms;
    }


   // -- Execute copy or move operation ----------------------------------------------------
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
               if (EXECUTE_COMMANDS) File.createDirectory(targetDirectory, true);
            }

            // TO BE ON SAFE SIDE
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

            if (engine_mode===0) {
               console.writeln("move " + inputFile +"\n  to "+ targetFile);
               if (EXECUTE_COMMANDS) File.move(inputFile,targetFile);
            } else {
               console.writeln("copy " + inputFile+"\n  to "+ targetFile);
               if (EXECUTE_COMMANDS)  copyFile(inputFile,targetFile);
            }
            count ++;

            // To allow abort ?
            processEvents();

         }
         //console.writeln("Total files: ", this.inputFiles.length,"; Processed: ",count);

   };

   // -- Export the keywords of a list of files
   this.exportFITSKeyWords = function() {
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

      //output header (tab separated selected fits keyword + 'Filename')
      for ( var i =0; i<this.keyTable.length; i++) {
         if (!this.keyEnabled[i]) continue;
         f.outTextLn(this.keyTable[i]+tab);
      }
      f.outTextLn("Filename"+String.fromCharCode(10,13));

      //output FITS data
      for ( var j =0; j< this.targetFilesIndices.length; j++) {
         var inputIndex = this.targetFilesIndices[i];

         var key = this.inputKeys[inputIndex];
         for ( var i = 0; i< this.keyTable.length; i++) {
            if (!this.keyEnabled[i]) continue;
            var name = this.keyTable[i];
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

   // -- Return true if move or copy is possible
   this.canDoOperation = function() {
         return !((!this.inputFiles.length) || (!this.outputDirectory));
   }



   this.reset();
}

