// FITSFileManager-config-gui.js

// This file is part of FITSFileManager, see copyrigh in FITSFileManager.js


// This file contains tww modules:
//    ffM_GUI_support - General utility controls (currently used in this file only)
//    ffM_GUI_config  - Dialog to manage configuration sets


// ========================================================================================================================
// ffM_GUI_support - GUI support methods and controls
//   This module contains controls independent of the FITSFileManager data model
//   (but specialized in term of layout for use in the FITSFileManager gui)
// ========================================================================================================================
var ffM_GUI_support = (function (){

   // ---------------------------------------------------------------------------------------------------------
   // IconButtonBar - an horizontal bar of buttons defined by icons
   // parameters:
   //     buttons:  an array of objects  {icon:aBitmapName, toolTip: aText, action: aFunction()}
   // ---------------------------------------------------------------------------------------------------------
   var IconButtonBar = function(parent, buttons) {
      this.__base__ = Control;
      this.__base__(parent);

      var i, button;

      this.sizer =  new HorizontalSizer;
      this.sizer.margin = 6;
      this.sizer.spacing = 4;

      for (i=0; i<buttons.length;i ++) {
         var button = buttons[i];
         //Log.debug(i,button.icon, button.toolTip);
         var toolButton = new ToolButton( parent );
         this.sizer.add(toolButton);
         toolButton.icon = new Bitmap( button.icon );
         toolButton.toolTip = button.toolTip;
         toolButton.onClick = button.action;
      }
      this.sizer.addStretch();
   }
   IconButtonBar.prototype = new Control;




   // ---------------------------------------------------------------------------------------------------------
   // ManagedList_Box: A list selection box with the possibility to add, remove and move elements,
   //                  the list elements amy have multiple properties represented in multiple columns.
   // parameters:
   //    parent: UI Control
   //    modeldescription: An array of element model descriptions (one description for each column),
   //         each description is an object with the property (more could be added later):
   //            propertyName: name of the propery to use in the element model for the corresponding column
   //   initialListModel: A (usually empty) initial list model (an array of object with the properties described by modelDescription)
   //                    Usually the model is specified by the modelListChange call (including for initial populate)
   //   elementFactory: A function() that return a new element model (called when new is created)
   //   toolTip: The tool tip text
   //   selectionCallBack: a function(elementModel) that return the element model currently selected. an be null if
   //                      no element is selected (for example last one is deleted).
   //   sorted: if true the elements are sorted on the first column
   // ---------------------------------------------------------------------------------------------------------
   function ManagedList_Box(parent, modelDescription, initialListModel, elementFactory, toolTip, selectionCallback, sorted) {
      this.__base__ = Control;
      this.__base__(parent);

      var i;
      (typeof sorted === 'undefined') && (sorted = false);

      // -- Model (the model will be updated in place)
      var listModel = [];


      // -- private methods
      // Create a node based on the model described in modelDescription
      var makeNode = function(treeBox, nodeModel, index) {
         //Log.debug('ManagedList_Box: makeNode -',Log.pp(nodeModel),Log.pp(modelDescription));
         var node = new TreeBoxNode( treeBox, index);
         for (var i=0; i<modelDescription.length;i++){
            node.setText(i, nodeModel[modelDescription[i].propertyName].toString());
         }
         return node;
      }



      // -- UI

      this.sizer = new VerticalSizer;

      var treeBox = new TreeBox(this);
      this.sizer.add(treeBox);

      treeBox.rootDecoration = false;
      treeBox.numberOfColumns = 2;
      treeBox.multipleSelection = false;
      treeBox.headerVisible = false;
      treeBox.headerSorting = false;

      treeBox.sort(0,sorted); // DO NOT SEEMS TO WORK

      //treeBox.setMinSize( 700, 200 );
      // DO not seem to have any effect
      //treeBox.lineWidth = 1;
      treeBox.style = Frame.FrameStyleSunken;

      treeBox.toolTip = toolTip;


      // -- Model update methods
      // A new model must be used for the listModel
      this.modelListChanged = function(newModelList) {

         // Clear current list display
         var i;
         var nmbNodes = treeBox.numberOfChildren;
         for (i=nmbNodes; i>0; i--) {
            treeBox.remove(i-1);
         }
         // Update variable tracking current model
         listModel = newModelList;

         // Add new nodes
         for (i=0; i<listModel.length; i++) {
            // Just making the nodes add them to the treeBox
            makeNode(treeBox, listModel[i], i);
         }
      }
      // Create initial model
      this.modelListChanged(initialListModel);

     // The current values of the current row model changed
     this.currentModelElementChanged = function() {
        if (treeBox.selectedNodes.length>0) {
           var selectedNode = treeBox.selectedNodes[0];
           var selectedIndex = treeBox.childIndex(selectedNode);
           if (selectedIndex>=0 && selectedIndex<listModel.length) {
              for (var i=0; i<modelDescription.length;i++){
                 selectedNode.setText(i, listModel[selectedIndex][modelDescription[i].propertyName].toString());
              }
            }
        }
      }


     // -- Internal actions
     var upAction = function() {
        var nodeToMove,  nodeIndex, element1, element2;
        if (treeBox.selectedNodes.length>0) {
           nodeToMove = treeBox.selectedNodes[0];
           nodeIndex = treeBox.childIndex(nodeToMove);
           if (nodeIndex>0) {
              // Update model
              element1 = listModel[nodeIndex-1];
              element2 = listModel[nodeIndex];
              listModel.splice(nodeIndex-1, 2, element2, element1);
              // update UI
              treeBox.remove(nodeIndex);
              treeBox.insert(nodeIndex-1,nodeToMove);
              treeBox.currentNode = nodeToMove;
           }
        }
     }
     var downAction = function() {
        var nodeToMove,  nodeIndex, element1, element2;
        if (treeBox.selectedNodes.length>0) {
           nodeToMove = treeBox.selectedNodes[0];
           nodeIndex = treeBox.childIndex(nodeToMove);
           if (nodeIndex<treeBox.numberOfChildren-1) {
              // Update model
              element1 = listModel[nodeIndex];
              element2 = listModel[nodeIndex+1];
              listModel.splice(nodeIndex, 2, element2, element1);
              // update UI
              treeBox.remove(nodeIndex);
              treeBox.insert(nodeIndex+1,nodeToMove);
              treeBox.currentNode = nodeToMove;
           }
        }
     }
     var addAction = function() {
        var nodeBeforeNew, newNode;
        var nodeIndex = treeBox.numberOfChildren;
        if (treeBox.selectedNodes.length>0) {
           nodeBeforeNew = treeBox.selectedNodes[0];
           nodeIndex = treeBox.childIndex(nodeBeforeNew) + 1;
        }
        // Create node
        var element = elementFactory();
        if (element !== null) {
           // insert node in model then ui
           listModel.splice(nodeIndex, 0, element);
           newNode = makeNode(treeBox, element, nodeIndex);
           treeBox.currentNode = newNode;
           treeBox.onNodeSelectionUpdated();
        }
     }

     var deleteAction = function() {
        var nodeToDelete,  nodeIndex;
        if (treeBox.selectedNodes.length>0) {
           nodeToDelete = treeBox.selectedNodes[0];
           nodeIndex = treeBox.childIndex(nodeToDelete);
           listModel.splice(nodeIndex,1);
           treeBox.remove(nodeIndex);
        }
     }

     treeBox.onNodeSelectionUpdated  = function() {
        // There could be an empty selection (last element removed)
        if (treeBox.selectedNodes.length>0) {
           var selectedNode = treeBox.selectedNodes[0];
           var selectedIndex = treeBox.childIndex(selectedNode);
           if (selectedIndex>=0 && selectedIndex<listModel.length) {
              selectionCallback(listModel[selectedIndex]);
            }
         }
     }


     var buttons = [
        {
           icon: ":/images/interface/prevButton.png",
           toolTip: "Move item up",
           action: upAction
           },
        {
           icon: ":/images/interface/nextButton.png",
           toolTip:  "Move item down",
           action: downAction
        },
        {
           icon: ":/images/icons/add.png",
           toolTip:  "Add new item",
           action: addAction
        },
        {
           icon: ":/images/icons/cancel.png",
           toolTip:  "Delete item",
           action: deleteAction
        },
     ];


     var iconButtonBar = new IconButtonBar(this, buttons);
     this.sizer.add(iconButtonBar);

     //this.adjustToContents();


  }
  ManagedList_Box.prototype = new Control;


  return {
     IconButtonBar: IconButtonBar,
     ManagedList_Box: ManagedList_Box,
  }

}) ();





// ========================================================================================================================
// ffM_GUI_config - Configuration dialog
//    The Dialog used to update a ConfigurationSet
// Usage:
//    A factory method is exposed to create the Dialog
//    Before being executed it must be configured with the ConfigurationSet and the name of the configuration
//       to select at start.
//    The dialog will make a local copy of the Configuration object and update it in place. It will leave
//    data for all resolvers selected for a variable, so in case a resolver is changed and then changed back to
//    the initial value, the original data is recovered.  It is exepected that this redundant data will be
//    removed before export.
//    The dialog expose properties to get the updated ConfigurationSet and currently edited set,
//    the caller should use them in case of successful return (and ignored them in case of cancel).
// ========================================================================================================================

var ffM_GUI_config = (function (){

   // -- Private methods
   function makeOKCancel(parentDialog) {
      var cancel_Button, ok_Button;

      // TODO Add container first
      var c = new Control(parentDialog);
      c.sizer = new HorizontalSizer;
      c.sizer.margin = 6;
      c.sizer.spacing = 4;

      cancel_Button = new PushButton( c );
      cancel_Button.text = "Cancel";
      cancel_Button.enabled = true;
      cancel_Button.onClick = function() {
         debug("cancel");
         parentDialog.cancel();
      }
      ok_Button = new PushButton( c );
      ok_Button.text = "OK";
      ok_Button.enabled = true;
      ok_Button.onClick = function() {
         debug("ok");
         parentDialog.ok();
      }

      c.sizer.addStretch();
      c.sizer.add(cancel_Button);
      c.sizer.add(ok_Button);

      return c;

  }

  // ---------------------------------------------------------------------------------------------------------

  // Top pane - Selection of configuration
  function ConfigurationSelection_ComboBox (parent, initialNames, configurationSelectedCallback) {
      this.__base__ = ComboBox;
      this.__base__(parent);

      this.configurationNames = initialNames;

      var i;
      this.toolTip = Text.H.GROUP_TEMPLATE_TOOLTIP;
      this.enabled = true;
      this.editEnabled = false;
      for (i=0; i<initialNames.length;i++) {
         this.addItem(initialNames[i]);
      }
      if (this.configurationNames.length>0) {
         this.currentItem = 0;
      }

      this.onItemSelected = function() {
         if (this.currentItem>=0 && this.currentItem<this.configurationNames.length) {
            configurationSelectedCallback(this.configurationNames[this.currentItem]);
         }
      }

      this.configure = function(names, selectedName) {
         this.configurationNames = names;
         this.clear();
         for (i=0; i<names.length;i++) {
            this.addItem(names[i]);
            if (selectedName === names[i]) {
               this.currentItem = i;
            }
         }
      }
  }
  ConfigurationSelection_ComboBox.prototype = new ComboBox;



  // Helper to validate and normalize input text,
  // There is no real conversion, as we keep all information in text format
  // the toString() is just to avoid crash and help debug in case a non string object is received
  var propertyTypes = {
     FREE_TEXT: {
        name: "FREE_TEXT",
        propertyToText: function(value) {return value.toString()},
        textToProperty: function(text) {return text},
     },
     REG_EXP: {
        name: "REG_EXP",
        propertyToText: function(value) {return value.toString()},
        textToProperty: function(text) {return regExpToString(regExpFromUserString(text))},
     }
  }



  // Utility pane - A Label - Labelled text field of a property of an object
  //  parent: UI parent control
  //  style: Style related properties (minLabelWidth, minDataWidth)
  //  name: Text of the label of this text field
  //  property: Name of property of the target that will be used as source of text and destination of text
  //  propertyType: An object with two functions:
  //     propertyToText: that format the property as text
  //     textToProperty: that parse the text and return the property value, throw an exception in case of error
  //  valueChangedCallback(): function that will be called if the text is successfuly updated - if null there is no callback
  // The target (the object containing the property to edit) is specified dynamically (including at initialization)
  function TextEntryRow(parent, style, name, property, propertyType, valueChangedCallback) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     this.sizer = new HorizontalSizer;
     this.sizer.margin = 2;
     this.sizer.spacing = 2;

     this.property = property;
     this.target = null;

     var the_Label = new Label( this );
     this.sizer.add(the_Label);
     the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
     the_Label.minWidth = style.minLabelWidth;
     the_Label.text = name + ": ";

     var name_Edit = new Edit(this);
     name_Edit.minWidth = style.minDataWidth;
     this.sizer.add(name_Edit)

     name_Edit.onTextUpdated = function() {
        var value;
        if (that.target !== null) {
           //Log.debug("TextEntryRow: onTextUpdated:",property,this.text);
           try {
              value =  propertyType.textToProperty(this.text);
              // Next lines will only execute if value was correctly parsed
              name_Edit.textColor = 0x000000;
              that.target[property] = value;
              if (valueChangedCallback != null) {
                 valueChangedCallback();
              }
           } catch (error) {
              name_Edit.textColor = 0xFF0000;
           }
        }
     }
     // Define the target object (that must have the property defined originally), null disables input
     this.updateTarget = function(target) {
        that.target = target;
        if (target === null) {
           name_Edit.text = '';
           name_Edit.enabled = false;
        } else {
           if (! target.hasOwnProperty(property)) {
              throw "Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
           }
           name_Edit.text = propertyType.propertyToText(target[property]);
           name_Edit.enabled = true;
        }
     }

     this.updateTarget(null);
  }
  TextEntryRow.prototype = new Control;



  // -- Middle right sub-panes (components to edit variables)
  var makeResolverSelection_ComboBox = function(parent, mappingNames, mappingSelectionCallback) {
     var i;
     var comboBox = new ComboBox( parent );
     comboBox.toolTip = "Mapping";
     comboBox.enabled = true;
     comboBox.editEnabled = false;
     for (i=0; i<mappingNames.length;i++) {
        comboBox.addItem(mappingNames[i]);
     }
     comboBox.currentItem = 0;
     comboBox.enabled = false;

     comboBox.onItemSelected = function() {
        if (this.currentItem>=0) {
           mappingSelectionCallback(mappingNames[this.currentItem]);
        }
     }

     comboBox.selectResolver = function(name) {
        if (name == null) {
              comboBox.enabled = false;
        } else {
           for (i=0; i<mappingNames.length;i++) {
              if (name === mappingNames[i]) {
                 comboBox.currentItem = i;
                 comboBox.enabled = true;
                 break;
              }
           }
        }
     }

     return comboBox;
  }

  function ResolverSelectionRow(parent, rowStyle, name, mappingNames, mappingSelectionCallback) {
     this.__base__ = Control;
     this.__base__(parent);
     var that=this;

     this.sizer = new HorizontalSizer;
     this.sizer.margin = 2;
     this.sizer.spacing = 2;

     var the_Label = new Label( this );
     this.sizer.add(the_Label);
     the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
     the_Label.minWidth = rowStyle.minLabelWidth;
     the_Label.text = name + ": ";

     var resolver_ComboBox = makeResolverSelection_ComboBox (parent, mappingNames, mappingSelectionCallback);
     resolver_ComboBox.minWidth = rowStyle.minDataWidth;
     this.sizer.add(resolver_ComboBox);

     this.selectResolver = function(name) {
        resolver_ComboBox.selectResolver(name);
     }
  }
  ResolverSelectionRow.prototype = new Control;


  // ..............................................................................................
  // -- Controls for resolver
  // ..............................................................................................

  function MapFirstRegExpControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     var keyRow = new TextEntryRow(this, rowStyle, "FITS key", "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(keyRow);


     var transformationDefinitionFactory = function() {
        return {regexp: /.*/, replacement: '?'}
     }

     var transformationSelectionCallback = function(transformationModel) {
        that.selectTransformationToEdit(transformationModel);
     }

     var regExpListSelection_GroupBox = new GroupBox(this);
     this.sizer.add(regExpListSelection_GroupBox);
     regExpListSelection_GroupBox.title ="List of RegExp->value";
     regExpListSelection_GroupBox.sizer = new VerticalSizer;

     var selectionLayoutControl = new Control;
     regExpListSelection_GroupBox.sizer.add(selectionLayoutControl);
     selectionLayoutControl.sizer = new HorizontalSizer;
     selectionLayoutControl.sizer.addStretch();

     var regExpListSelection_Box = new ffM_GUI_support.ManagedList_Box(
           selectionLayoutControl,
           [{propertyName: 'regexp'},{propertyName: 'replacement'}],
           [], // Its model will be initialized dynamically
           transformationDefinitionFactory,
           "Transformation",
           transformationSelectionCallback,
           false // Keep in order
     );
     selectionLayoutControl.sizer.add(regExpListSelection_Box);


     this.currentRegExpRow = new TextEntryRow(regExpListSelection_GroupBox, rowStyle, "Regexp", "regexp",
        propertyTypes.REG_EXP,
        function() {regExpListSelection_Box.currentModelElementChanged()});
     regExpListSelection_GroupBox.sizer.add(this.currentRegExpRow);
     this.currentReplacementRow = new TextEntryRow(regExpListSelection_GroupBox,rowStyle, "Replacement", "replacement",
        propertyTypes.FREE_TEXT,
        function() {regExpListSelection_Box.currentModelElementChanged()});
     regExpListSelection_GroupBox.sizer.add(this.currentReplacementRow);


     this.selectTransformationToEdit = function(transformationModel) {
        this.currentRegExpRow.updateTarget(transformationModel);
        this.currentReplacementRow.updateTarget(transformationModel);
     }

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // initialize should probably be somewhere else
        this.initialize(variableDefinition);
        keyRow.updateTarget(variableDefinition.parameters[resolverName]);
        regExpListSelection_Box.modelListChanged(variableDefinition.parameters[resolverName].reChecks);
        // Workaround:
        // The modelListChanged above seem to generate callbacks events that update the regexp
        // and replacement row, but we want them to be disabled until the user explicitely select one.
        this.currentRegExpRow.updateTarget(null);
        this.currentReplacementRow.updateTarget(null);
     }

     this.leave = function() {
        this.currentRegExpRow.updateTarget(null);
        this.currentReplacementRow.updateTarget(null);
     }
  }
  MapFirstRegExpControl.prototype = new Control;

  // ..............................................................................................

  function ConstantValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;
     var constantValueRow = new TextEntryRow(this, rowStyle, "value", "value", propertyTypes.FREE_TEXT, null);
     this.sizer.add(constantValueRow);

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        constantValueRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  ConstantValueResolverControl.prototype = new Control;

  // ..............................................................................................

  function IntegerValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     var keyRow = new TextEntryRow(this, rowStyle, "FITS key", "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(keyRow);

     var formatRow = new TextEntryRow(this, rowStyle, "Format", "format", propertyTypes.FREE_TEXT, null);
     this.sizer.add(formatRow);

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        keyRow.updateTarget(variableDefinition.parameters[resolverName]);
        formatRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  IntegerValueResolverControl.prototype = new Control;


  // ..............................................................................................

  function IntegerPairValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key 1
     var key1Row = new TextEntryRow(this, rowStyle, "FITS key 1", "key1", propertyTypes.FREE_TEXT, null);
     this.sizer.add(key1Row);

     var key2Row = new TextEntryRow(this, rowStyle, "FITS key 2", "key2", propertyTypes.FREE_TEXT, null);
     this.sizer.add(key2Row);

     var formatRow = new TextEntryRow(this, rowStyle, "Format", "format", propertyTypes.FREE_TEXT, null);
     this.sizer.add(formatRow);

     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        key1Row.updateTarget(variableDefinition.parameters[resolverName]);
        key2Row.updateTarget(variableDefinition.parameters[resolverName]);
        formatRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  IntegerPairValueResolverControl.prototype = new Control;

  // ..............................................................................................

  function NightResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     var key1Row = new TextEntryRow(this, rowStyle, "LONG_OBS key", "keyLongObs", propertyTypes.FREE_TEXT, null);
     this.sizer.add(key1Row);

     var key2Row = new TextEntryRow(this, rowStyle, "JD key", "keyJD", propertyTypes.FREE_TEXT, null);
     this.sizer.add(key2Row);


     this.initialize = function(variableDefinition) {
        if (!variableDefinition.parameters.hasOwnProperty(resolverName)) {
           variableDefinition.parameters[resolverName] = deepCopyData(ffM_Resolver.resolverByName(resolverName).initial);
        }
     }

     this.populate = function(variableDefinition) {
        // Should probably be somewhere else
        this.initialize(variableDefinition);
        key1Row.updateTarget(variableDefinition.parameters[resolverName]);
        key2Row.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  NightResolverControl.prototype = new Control;




  // ..............................................................................................


  // -- Middle pane - Variable definitions (two panes: add/remove variables and edit selected variables)
  function VariableUIControl(parent, variableDefinitionFactory ) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     // -- Model data - set by selectVariable()
     this.currentVariableDefinition = null;

     // -- UI data
     // this.resolverSelection_GroupBox = null; // defined later
     this.currentResolver = null;

     this.sizer = new HorizontalSizer;
     this.sizer.margin = 6;
     this.sizer.spacing = 4;

     var labelWidth = this.font.width( "MMMMMMMMMMMM: " );
     var dataWidth = this.font.width( "MMMMMMMMMMMMMMMMMMMMMMM" );
     var rowStyle = {
        minLabelWidth: labelWidth,
        minDataWidth: dataWidth,
     }


     // -- GUI action callbacks
     // Variable selected in current configuration, forward to the model handling later in this object
     var variableSelectionCallback = function(variableDefinition) {
        //Log.debug("VariableUIControl: variableSelectionCallback - Variable selected: " +variableDefinition.name);
        that.selectVariable(variableDefinition);
     }




     // -- Left side - select variable being edited, add/remove variables

     var variableListSelection_GroupBox = new GroupBox(this);
     this.sizer.add(variableListSelection_GroupBox);
     variableListSelection_GroupBox.title = "Select variable";
     variableListSelection_GroupBox.sizer = new VerticalSizer; // Any sizer

     var variableListSelection_Box = new ffM_GUI_support.ManagedList_Box(
           variableListSelection_GroupBox,
          [{propertyName: 'name'},{propertyName: 'description'}],
          [], // Its model will be initialized dynamically
           variableDefinitionFactory,
           "Variable definitions",
           variableSelectionCallback,
           true // Sort by variable name
     );
     variableListSelection_GroupBox.sizer.add(variableListSelection_Box);



     //--  Right side - Enter parameters corresponding to selected variables
     var resolverSelectionCallback = function(resolverName) {
        Log.debug("VariableUIControl: resolverSelectionCallback - selected:",resolverName);
        that.updateResolver(resolverName);
     }

     var variableDetails_GroupBox = new GroupBox(this);
     variableDetails_GroupBox.title = "Parameters of variable";
     this.sizer.add(variableDetails_GroupBox);

     variableDetails_GroupBox.sizer = new VerticalSizer;



     //this.resolverSelection_GroupBox =  makeResolverSelection_ComboBox(this, resolverNames, resolverSelectionCallback);
     this.resolverSelectionRow =  new ResolverSelectionRow(this, rowStyle, "Resolver", ffM_Resolver.resolverNames, resolverSelectionCallback);
     variableDetails_GroupBox.sizer.add(this.resolverSelectionRow);

     this.variableNameRow = new TextEntryRow(this, rowStyle, "Variable name","name",
        propertyTypes.FREE_TEXT,
        function() {variableListSelection_Box.currentModelElementChanged()});
     variableDetails_GroupBox.sizer.add(this.variableNameRow);
     this.descriptionRow = new TextEntryRow(this, rowStyle, "Description","description",
        propertyTypes.FREE_TEXT,
        function() {variableListSelection_Box.currentModelElementChanged()});
     variableDetails_GroupBox.sizer.add(this.descriptionRow);

     var addNewResolverControl = function(resolverControl){
        variableDetails_GroupBox.sizer.add(resolverControl);
        ffM_Resolver.resolverByName(resolverControl.resolverName).control = resolverControl;
        resolverControl.hide();
     }

     // Make all resolver controls, only the selected one will be shown
     addNewResolverControl(new ConstantValueResolverControl(variableDetails_GroupBox, 'Constant', rowStyle));

     addNewResolverControl(new IntegerValueResolverControl(variableDetails_GroupBox, 'Integer', rowStyle));

     addNewResolverControl(new IntegerPairValueResolverControl(variableDetails_GroupBox, 'IntegerPair', rowStyle));

     addNewResolverControl(new MapFirstRegExpControl(variableDetails_GroupBox,'RegExpList', rowStyle));

     addNewResolverControl(new NightResolverControl(variableDetails_GroupBox,'Night', rowStyle));

     // FileName and FileExtension have no parameter
     ffM_Resolver.resolverByName('FileName').control = null
     ffM_Resolver.resolverByName('FileExtension').control = null

     variableDetails_GroupBox.sizer.addStretch();

     // TRICK - void growing when more proeprties are added to the right pane, the dialg can grow,
     // so it is possible to avoid this minimum size, but the effect is likely uggly.
     variableDetails_GroupBox.setMinHeight(20 * this.font.lineSpacing + 2*this.sizer.margin);



     // -- Update the model
     // The resolver name was updated (by select box, by changing variable or initially)
     // null if no resolver (like an empty variable list)
     this.updateResolver = function(resolverName) {
        //Log.debug(" updateResolver",resolverName,that.currentResolver);
        // There could be no controller if there is no parameter
        if (that.currentResolver != null && that.currentResolver.control !== null) {
           that.currentResolver.control.leave();
           that.currentResolver.control.hide();
        }
        that.currentResolver =null;
        if (resolverName != null) {
           var resolver = ffM_Resolver.resolverByName(resolverName);
           if (resolver === null) {
              throw "Invalid resolver '" + resolverName + "' for variable '"+ variableDefinition.name+"'";
           }
           if (that.currentVariableDefinition !== null) {
              // record the new resolver
              that.currentVariableDefinition.resolver = resolverName;
              // Populate and show it
              that.currentResolver = resolver;
              if (resolver.control !== null) {
                 resolver.control.populate(that.currentVariableDefinition );
                 resolver.control.show();
              }
           }
        }
     }

     // The variable to edit was selected
     this.selectVariable = function(variableDefinition) {
        Log.debug("VariableUIControl: selectVariable - Variable selected ",variableDefinition.name );
        var resolverName;

        that.currentVariableDefinition = variableDefinition;

        // populate the common fields
        this.variableNameRow.updateTarget(variableDefinition);
        this.descriptionRow.updateTarget(variableDefinition);

        // Find new resolver, populate and show it
        resolverName = that.currentVariableDefinition.resolver;
        that.updateResolver(resolverName);

        // Update the UI to have the resolver type of the current variable
        this.resolverSelectionRow.selectResolver(resolverName);
      }

     // The list of variables was changed externally (initial or change)
     this.updateVariableList = function(newVariableList) {
        // Update UI
        variableListSelection_Box.modelListChanged(newVariableList);
     }

  }

  VariableUIControl.prototype = new Control;


  function ConfigurationLayoutControl(parent, configurationSelectedCallback ) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     this.sizer = new HorizontalSizer;
     var configurationSelection_Label = new Label(this);
     this.sizer.add(configurationSelection_Label);
     configurationSelection_Label.text = "Configuration: ";
     this.configurationSelection_ComboBox = new ConfigurationSelection_ComboBox(this, [], configurationSelectedCallback);
     this.sizer.add(this.configurationSelection_ComboBox);
     this.sizer.addSpacing(12);
     this.configurationComment_Edit = new Edit;
     this.sizer.add(this.configurationComment_Edit);
     // To track edited comment
     this.selectedConfiguration = null;

     this.configure = function(editedConfigurationSet, currentConfigurationName) {
         var configurationNames = ffM_Configuration.getAllConfigurationNames(editedConfigurationSet);
         this.configurationSelection_ComboBox.configure(configurationNames, currentConfigurationName);
         this.selectedConfiguration = ffM_Configuration.getConfigurationByName(editedConfigurationSet, currentConfigurationName);
         this.configurationComment_Edit.text = this.selectedConfiguration.description;
     }
     this.configurationComment_Edit.onTextUpdated = function() {
        if (this.selectedConfiguration !== null) {
           this.selectedConfiguration.description = this.text;
        }
     }
   }
   ConfigurationLayoutControl.prototype = new Control;

  // ---------------------------------------------------------------------------------------------------------
   function BuiltinVariableGroup(parent) {
      this.__base__ = GroupBox;
      this.__base__(parent);
      var that = this;

      this.title = "Built in variable parameters";
      this.sizer = new HorizontalSizer;

      var labelWidth = this.font.width( "MMMMMMMMMMMM: " );
      var dataWidth = this.font.width( "MMMMMMMMMMMMMMMMMMMMMMM" );
      var rowStyle = {
        minLabelWidth: labelWidth,
        minDataWidth: dataWidth,
      }

      //this.sizer.addStretch();

      // Left columns
      var colLayout1_Control = new Control;
      this.sizer.add(colLayout1_Control);
      colLayout1_Control.sizer = new VerticalSizer;

      var rankFormat = new TextEntryRow(colLayout1_Control, rowStyle, "&rank; format", "rankFormat", propertyTypes.FREE_TEXT, null);
      colLayout1_Control.sizer.add(rankFormat);

      // Right column
      var colLayout2_Control = new Control;
      this.sizer.add(colLayout2_Control);
      colLayout2_Control.sizer = new VerticalSizer;

      var countFormat = new TextEntryRow(colLayout2_Control, rowStyle, "&count format", "countFormat", propertyTypes.FREE_TEXT, null);
      colLayout2_Control.sizer.add(countFormat);


      this.configure = function(editedConfigurationSet, currentConfigurationName) {
         var configurationNames = ffM_Configuration.getAllConfigurationNames(editedConfigurationSet);
         this.selectedConfiguration = ffM_Configuration.getConfigurationByName(editedConfigurationSet, currentConfigurationName);

         rankFormat.updateTarget(this.selectedConfiguration.builtins);
         countFormat.updateTarget(this.selectedConfiguration.builtins);
      }

   }
   BuiltinVariableGroup.prototype = new GroupBox();


  // ---------------------------------------------------------------------------------------------------------
  // This Dialog controls the update of a configurationSet, starting at a current configuration.
  // The configurationSet may be modified or not, at the end the caller must get
  // the configurationSet and new current configuration name properties from the dialog to get the current
  // state.
  // There is currently no way to add, delete or rename configurations
  function ConfigurationDialog( parentDialog) {
     this.__base__ = Dialog;
     this.__base__();
     var that = this;

     // Model -
     // Keeps track of configurationSet and current configuration selected
     // in a copy, so in case of cancel nothing is changed
     // Done in 'configure'
     this.editedConfigurationSet = null;
     this.currentConfigurationName = null;

     this.newVariableCounter = 0;

     this.windowTitle = Text.T.REMAPPING_SECTION_PART_TEXT;


     this.sizer = new VerticalSizer;
     this.sizer.margin = 6;
     this.sizer.spacing = 4;


     // -- GUI actions callbacks
     // configuration changed (also used in initialization)
     var configurationSelectedCallback = function(configurationName) {
        Log.debug("ConfigurationDialog: configurationSelectedCallback - ConfigurationSet selected:",configurationName);
        var selectedConfiguration = ffM_Configuration.getConfigurationByName(that.editedConfigurationSet, configurationName);
        if (selectedConfiguration == null) {
           throw "PROGRAM ERROR - Invalid configuration set name '" + configurationName +"'";
        }
        // Update model
        that.currentConfigurationName = configurationName;
        // Update UI
        that.variableUI.updateVariableList(selectedConfiguration.variableList);
        that.builtinVariable_Group.configure(that.editedConfigurationSet, that.currentConfigurationName);
        // Update the description text
        that.configurationLayoutControl.configure(that.editedConfigurationSet, that.currentConfigurationName);
     }

     // -- Model call backs
     // New variable requested in current configuration, define one with default values
     var variableDefinitionFactory = function() {
          Log.debug("ConfigurationDialog: variableDefinitionFactory - create new variable");
          that.newVariableCounter++;
          return ffM_Configuration.defineVariable("new_" + that.newVariableCounter,'','Constant');
     }

     // -- Build the top level pane

     // Top pane - select configuration to operate upon
     this.configurationLayoutControl = new ConfigurationLayoutControl(this, configurationSelectedCallback );
     this.sizer.add(this.configurationLayoutControl);



     // Middle pane - define variables, their resolvers and the resolver's parameters
     this.variableUI = new VariableUIControl(this, variableDefinitionFactory);
     this.sizer.add(this.variableUI);

     this.builtinVariable_Group = new BuiltinVariableGroup(this);
     this.sizer.add(this.builtinVariable_Group);

     // Bottom pane - buttons
     var okCancelButtons = makeOKCancel(this);
     this.sizer.add(okCancelButtons);

     this.setVariableSize();
     //this.adjustToContents();

     // -- Configure before executing
     this.configure = function configure(originalConfigurationSet, configurationNameToEdit) {
        this.editedConfigurationSet = deepCopyData(originalConfigurationSet);
        this.currentConfigurationName = configurationNameToEdit;
        var configurationNames = ffM_Configuration.getAllConfigurationNames(this.editedConfigurationSet);
        // Initialize content
        this.configurationLayoutControl.configure(this.editedConfigurationSet, this.currentConfigurationName);
        this.builtinVariable_Group.configure(this.editedConfigurationSet, this.currentConfigurationName);
        configurationSelectedCallback(this.currentConfigurationName);
     }



  }
  ConfigurationDialog.prototype = new Dialog;



  return {
     makeDialog: function(parent, configurationSet, configurationNameToEdit) {
        return new ConfigurationDialog(parent, configurationSet, configurationNameToEdit);
     }

  }

}) ();
