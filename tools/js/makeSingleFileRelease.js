"use strict";

// makeSingleFileRelease

// Script to consolidate all scripts of one project and setup version,
// to make a single file easier for distribution.

// Adapt VERSION below and execute the script, check output on the console.

#include <pjsr/DataType.jsh>

#define VERSION "1.4"
#define MAIN_FILE_NAME "FITSFileManager.js"

// This should be identical for all my projects
// The (relative) path of the directories of the sources
#define SOURCE_RELATIVE_DIR "../../main/js"
// The (relative) path of the directory that will contain the consolidate script
#define TARGET_RELATIVE_DIR "../../../releases"

(function (version, mainFileName) {

   // Return a full normalized path to the directory containing the parameter file (or directory)
   var getDirectoryOfFileWithDriveLetter = function getDirectoryOfFileWithDriveLetter( a_file_path )
   {
      let unix_path = File.windowsPathToUnix( a_file_path );
      let pathNormalized = File.fullPath(unix_path);
      let directoryWithDrive = File.extractDrive( pathNormalized ) + File.extractDirectory(pathNormalized);
      //Console.writeln("*** getDirectoryOfFileWithDriveLetter\n    a_file_path '" + a_file_path + "\n    unix_path '" + unix_path + "'\n    pathNormalized '" + pathNormalized + "' \n    directoryWithDrive '" + directoryWithDrive +"'");
      return directoryWithDrive;
   }

   // Return a full normalized path of a directory or a file, received in unix or windows format
   var getDirectoryWithDriveLetter = function getDirectoryWithDriveLetter( a_directory_path )
   {
      let unix_path = File.windowsPathToUnix( a_directory_path );
      let pathNormalized = File.fullPath(unix_path);
      //Console.writeln("*** getDirectoryWithDriveLetter\n    a_directory_path '" + a_directory_path + "'\n    unix_path '" + unix_path  + "'\n    pathNormalized '" + pathNormalized +"'");
      return pathNormalized;
   }

   // Directories are relative to the directory of the current script
   var releaseScriptFilePath = #__FILE__ ;
   var releaseScriptDir = getDirectoryOfFileWithDriveLetter(releaseScriptFilePath);

   var sourceDir = getDirectoryWithDriveLetter(releaseScriptDir + "/" + SOURCE_RELATIVE_DIR);
   var sourceMainFilePath = getDirectoryWithDriveLetter(sourceDir + "/" + mainFileName);
   var targetDirectory = getDirectoryWithDriveLetter(releaseScriptDir + "/" + TARGET_RELATIVE_DIR);

   // Read a file, return it as a list of lines
   var readTextFile = function(filePath) {
      if (!File.exists(filePath)) {
         throw "File not found: " +  File.fullPath(filePath);
      }
      let file = new File;
      file.openForReading(filePath);
      let buffer = file.read( DataType_ByteArray, file.size );
      file.close();

      let lines = buffer.toString().split( '\n' );
      return lines;
   }

   Console.writeln("Make release for '" + mainFileName + "', version '" + version  + "'");
   if (!File.directoryExists(targetDirectory)) {
      throw "Target directory not found: " +  File.fullPath(targetDirectory);
   }
   var mainFileNameOnly = File.extractName(mainFileName); // Name without extension
   var rootFileExtension = File.extractExtension(mainFileName);

   var targetFileName = mainFileNameOnly + "-" + version + rootFileExtension;
   var targetFilePath = targetDirectory + "/" + targetFileName;
   Console.writeln("  Target directory: " + targetDirectory);
   Console.writeln("  Target file name: " + targetFileName);


   var rootFileText = readTextFile(sourceMainFilePath);
   Console.writeln(mainFileName + " has " + rootFileText.length + " lines") ;

   var targetFile = new File;
   targetFile.createForWriting(targetFilePath);

   var includeRegExp1 = new RegExp("#include[ \\t]\"+(" + mainFileNameOnly + "-.+)\"");
   var includeRegExp2 = new RegExp("#include[ \\t]\"+(PJSR-logging\.jsh)\"");
   // var includeRegExp = /#include[ \t]"+(VaryParams-.+)"/;
   Console.writeln("  looking up include as " + includeRegExp1 + " or " + includeRegExp2);

   // For some reason adding the N make the regexp not working....
   var defineVersionRegExp = /^#define[ \t]+VERSIO/;

   for (var i=0; i<rootFileText.length; i++) {
      // To show log on console
      Console.flush();
      processEvents();

       // Copy the text, adding the included files and updating the VERSION string
      let line = rootFileText[i];
      let match = line.match(includeRegExp1);
      if (!match) match = line.match(includeRegExp2);
      if (match) {
         // Found an #include
         Console.writeln("  handling '"+ line.substring(0,line.length-1) + "'");
         
         let inludedFilePath = sourceDir + "/" + match[1];
         let includedText = readTextFile(inludedFilePath);
         Console.writeln("     including " + includedText.length + " lines from '" + match[1] + "'");
         targetFile.outTextLn("//================================================================");
         targetFile.outTextLn("// " + line.substring(1));
         targetFile.outTextLn("//================================================================");
         for (let j=0; j<includedText.length; j++) {
            targetFile.outTextLn(includedText[j]);
         }
         targetFile.outTextLn("//==== end of include ============================================");

      } else if (defineVersionRegExp.test(line)) {
         Console.writeln("  handling '" + line.substring(0,line.length-1) + "'");
         Console.writeln("     writing " + "#define VERSION \"" + version + "\"");
         targetFile.outTextLn("// Version created by makeRelease on " + new Date());
         targetFile.outTextLn("#define VERSION \"" + version + "\"");
      } else {
         targetFile.outTextLn(line);
      }

   }

   targetFile.close();
   Console.writeln("File '" + targetFilePath + "' created.");


})(VERSION, MAIN_FILE_NAME);

