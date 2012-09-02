"use strict";

// makeRelease

// Script to consolidate all scripts of one project and setup version,
// to make a single file easier for distribution

#include <pjsr/DataType.jsh>

#define VERSION "1.0"
#define ROOT_FILE "C:/Users/jmlugrin/Documents/Astronomie/Programs/PixInsight/PI my Scripts/FitsFileManager/sources/main/js/FITSFileManager.js"
#define TARGET_FILE_DIR "C:/Users/jmlugrin/Documents/Astronomie/Programs/PixInsight/PI my Scripts/FitsFileManager/releases"

(function (rootFilePath, version, targetDirectory) {

   // Read a file, return it as a list of lines
   var readTextFile = function(filePath) {
      if (!File.exists(filePath)) {
         throw "File not found: " +  File.fullPath(filePath);
      }
      var file = new File;
      file.openForReading(filePath);
      var buffer = file.read( DataType_ByteArray, file.size );
      file.close();

      var lines = buffer.toString().split( '\n' );
      return lines;
   }

   Console.writeln("Make release for '" + rootFilePath + "', version '" + version  + "'");
   if (!File.directoryExists(targetDirectory)) {
      throw "Target directory not found: " +  File.fullPath(targetDirectory);
   }
   var rootFileDirectory = File.extractDrive(rootFilePath) + File.extractDirectory(rootFilePath);
   var rootFileName = File.extractName(rootFilePath); // Name without extension
   var rootFileExtension = File.extractExtension(rootFilePath);

   var targetFileName = rootFileName + "-" + version + rootFileExtension;
   var targetFilePath = targetDirectory + "/" + targetFileName;
   Console.writeln("  Target directory: " + targetDirectory);
   Console.writeln("  Target file name: " + targetFileName);


   var rootFileText = readTextFile(rootFilePath);
   Console.writeln(rootFileName + " has " + rootFileText.length + " lines") ;

   var targetFile = new File;
   targetFile.createForWriting(targetFilePath);

   var includeRegExp = new RegExp("#include[ \\t]\"+(" + rootFileName + "-.+)\"");
   // var includeRegExp = /#include[ \t]"+(VaryParams-.+)"/;
   Console.writeln("  looking up include as " + includeRegExp);

   // For some reason adding the N make the regexp not working....
   var defineVersionRegExp = /^#define[ \t]+VERSIO/;

   for (var i=0; i<rootFileText.length; i++) {
      var line = rootFileText[i];
      var match = line.match(includeRegExp);
      if (match) {
         Console.writeln("  handling " + line);
         var inludedFilePath = rootFileDirectory + "/" + match[1];
         var includedText = readTextFile(inludedFilePath);
         Console.writeln("     including " + includedText.length + " lines from '" + match[1] + "'");
         targetFile.outTextLn("//================================================================");
         targetFile.outTextLn("// " + line.substring(1));
         targetFile.outTextLn("//================================================================");
         for (var j=0; j<includedText.length; j++) {
            targetFile.outTextLn(includedText[j]);
         }
         targetFile.outTextLn("//==== end of include ============================================");

      } else if (defineVersionRegExp.test(line)) {
         Console.writeln("  handling " + line);
         Console.writeln("     writing " + "#define VERSION \"" + version + "\"");
         targetFile.outTextLn("// Version created by makeRelease on " + new Date());
         targetFile.outTextLn("#define VERSION \"" + version + "\"");
      } else {
         targetFile.outTextLn(line);
      }

   }

   targetFile.close();
   Console.writeln("File '" + targetFilePath + "' created.");




})(ROOT_FILE, VERSION, TARGET_FILE_DIR);

