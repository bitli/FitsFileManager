// FITSFileManager-config-gui.js

// This file is part of FITSFileManager, see copyright in FITSFileManager.js


// This file contains tww modules:
//    ffM_GUI_support - General utility controls (currently used in this file only)
//    ffM_GUI_config  - Dialog to manage configuration sets


#define GROUP_BOX_MIN_SIZE 22

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

      var i, button, toolButton;

      this.sizer =  new HorizontalSizer;
      this.sizer.margin = 6;
      this.sizer.spacing = 4;

      for (var i=0; i<buttons.length;i ++) {
         var button = buttons[i];
#ifdef DEBUG
         Log.debug(i,button.icon, button.toolTip);
#endif
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

      treeBox.style = Frame.FrameStyleSunken;

      treeBox.toolTip = toolTip;


      // -- Model update methods
      // The list model is changed (the model is an array that is updated in place)
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

      var c = new Control(parentDialog);
      c.sizer = new HorizontalSizer;
      c.sizer.margin = 6;
      c.sizer.spacing = 4;

      cancel_Button = new PushButton( c );
      cancel_Button.text = "Cancel";
      cancel_Button.enabled = true;
      cancel_Button.onClick = function() {
         parentDialog.cancel();
      }
      ok_Button = new PushButton( c );
      ok_Button.text = "OK";
      ok_Button.enabled = true;
      ok_Button.onClick = function() {
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

      // -- UI
      this.toolTip = Text.H.SELECT_CONFIGURATION_BUTTON_TOOLTIP;
      this.enabled = true;
      this.editEnabled = false;
      for (i=0; i<initialNames.length;i++) {
         this.addItem(initialNames[i]);
      }
      if (this.configurationNames.length>0) {
         this.currentItem = 0;
      }

      // -- callback
      this.onItemSelected = function() {
         if (this.currentItem>=0 && this.currentItem<this.configurationNames.length) {
            configurationSelectedCallback(this.configurationNames[this.currentItem]);
         }
      }

      // -- Model update method, provide now list of names and new current name
      this.configure = function(names, selectedName) {
         this.configurationNames = names;
         this.clear();
         for (i=0; i<names.length;i++) {
            this.addItem(names[i]);
            if (names[i] === selectedName) {
               this.currentItem = i;
            }
         }
      }
  }
  ConfigurationSelection_ComboBox.prototype = new ComboBox;



  // Helper to validate and normalize input text,
  // There is no real conversion, as we keep all information in text format
  // the toString() is just to avoid crash and help debug in case a non string object is received
  var testInvalidVariableNameRegExp = /[&\(\);<>=!%*]/;
  var removeVariableReferencesRE = /&[0-9]+;/g
  var propertyTypes = {
     FREE_TEXT: {
        name: "FREE_TEXT",
        propertyToText: function(value) {return value.toString()},
        textToProperty: function(text) {return text},
     },
     REG_EXP: {
        // Check that this is a valid regular expression
        name: "REG_EXP",
        propertyToText: function(value) {return value.toString()},
        textToProperty: function(text) {return regExpToString(regExpFromUserString(text))},
     },
     REG_EXP_REPLACMENT: {
        // Check that this is a valid replacement for a regular expression,
        // this ensures that the only &<number>; are used (no &variable; or dangling &)
        name: "REG_EXP_REPLACMENT",
        propertyToText: function(value) {return value.toString()},
        textToProperty: function(text) {
           var withoutRef = text.replace(removeVariableReferencesRE,'');
           if (withoutRef.indexOf("&")>=0) {
               throw "Invalid replacement string"}
            else {return text}
        },
     },
     // Check that the characters are valid for a variable name
     VAR_NAME: {
        name: "VAR_NAME",
        propertyToText: function(value) {return value.toString()},
        textToProperty: function(text) {
            var t = text.trim();
            if (testInvalidVariableNameRegExp.test(t)) {
               throw "Invalid character in variable name"}
            else {return t}
         },
     },

  }



  // Utility pane - A Label - Labelled text field of a property of an object
  //  parent: UI parent control
  //  style: Style related properties (minLabelWidth, minDataWidth)
  //  name: Text of the label of this text field
  //  toolTip: the tooltip to present to the user
  //  property: Name of property of the target that will be used as source of text and destination of text
  //  propertyType: An object with two functions:
  //     propertyToText: that format the property as text
  //     textToProperty: that parse the text and return the property value, throw an exception in case of error
  //  valueChangedCallback(): function that will be called if the text is successfuly updated - if null there is no callback
  // The target (the object containing the property to edit) is specified dynamically (including at initialization)
  function TextEntryRow(parent, style, name, toolTip, property, propertyType, valueChangedCallback) {
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
     the_Label.toolTip = toolTip;

     var text_Edit = new Edit(this);
     text_Edit.minWidth = style.minDataWidth;
     this.sizer.add(text_Edit)
     text_Edit.toolTip = toolTip;

     text_Edit.onTextUpdated = function() {
        var value;
        if (that.target !== null) {
           //Log.debug("TextEntryRow: onTextUpdated:",property,this.text);
           try {
              value =  propertyType.textToProperty(this.text);
              // Next lines will only execute if value was correctly parsed
              text_Edit.textColor = 0x000000;
              that.target[property] = value;
              if (valueChangedCallback != null) {
                 valueChangedCallback();
              }
           } catch (error) {
              text_Edit.textColor = 0xFF0000;
           }
        }
     }
     // Define the target object (that must have the property defined originally), null disables input
     this.updateTarget = function(target) {
        that.target = target;
        if (target === null) {
           text_Edit.text = '';
           text_Edit.enabled = false;
        } else {
           if (! target.hasOwnProperty(property)) {
              throw "Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
           }
           text_Edit.text = propertyType.propertyToText(target[property]);
           text_Edit.enabled = true;
        }
     }

     this.updateTarget(null);
  }
  TextEntryRow.prototype = new Control;

   // TODO Refactor with Text Entry Row
  function BooleanEntryRow(parent, style, name, toolTip, property, valueChangedCallback) {
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
     the_Label.toolTip = toolTip;

     var bool_CheckBox = new CheckBox(this);
     bool_CheckBox.minWidth = style.minDataWidth;
     this.sizer.add(bool_CheckBox)
     bool_CheckBox.toolTip = toolTip;

     bool_CheckBox.onCheck = function() {
        var value;
        if (that.target !== null) {
             value =  this.checked;
            bool_CheckBox.textColor = 0x000000;
            that.target[property] = value;
            if (valueChangedCallback != null) {
               valueChangedCallback();
            }
        }
     }
     // Define the target object (that must have the property defined originally), null disables input
     this.updateTarget = function(target) {
        that.target = target;
        if (target === null) {
           bool_CheckBox.checked = false;
           bool_CheckBox.enabled = false;
        } else {
           if (! target.hasOwnProperty(property)) {
              throw "Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
           }
           bool_CheckBox.checked = target[property];
           bool_CheckBox.enabled = true;
        }
     }

     this.updateTarget(null);
  }
  BooleanEntryRow.prototype = new Control;


   // TODO Refactor with Text Entry Row
  function CheckListEntryRow(parent, style, name, toolTip, property, checkNames, checkValues, valueChangedCallback) {
     this.__base__ = Control;
     this.__base__(parent);
     var that = this;

     var i;

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
     the_Label.toolTip = toolTip;

     var checkBoxContainer = new Control(this);
     this.sizer.add(checkBoxContainer);
     checkBoxContainer.sizer = new HorizontalSizer;
     var checkBoxes = [];

      for (var i=0; i<checkNames.length; i++) {

         var check_Label = new Label( this );
         checkBoxContainer.sizer.add(check_Label);
         check_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
         check_Label.text = checkNames[i];
         checkBoxContainer.sizer.addSpacing(4);

         var bool_CheckBox = new RadioButton(checkBoxContainer);
         checkBoxContainer.sizer.add(bool_CheckBox);
         checkBoxes.push(bool_CheckBox);
         bool_CheckBox.toolTip = toolTip;
         bool_CheckBox.code = checkValues[i];
         checkBoxContainer.sizer.addSpacing(4);

         bool_CheckBox.onCheck = function() {
            var value;
            if (that.target !== null) {
               value =  this.code;
               bool_CheckBox.textColor = 0x000000;
               that.target[property] = value;
               if (valueChangedCallback != null) {
                  valueChangedCallback();
               }
            }
         }
     }
     checkBoxContainer.sizer.addStretch();

     // Define the target object (that must have the property defined originally), null disables input
     this.updateTarget = function(target) {
        that.target = target;
        if (target === null) {
            for (var i=0; i<checkBoxes.length;i ++) {
               checkBoxes[i].checked = false;
               checkBoxes[i].enabled = false;
            }
        } else {
            if (! target.hasOwnProperty(property)) {
               throw "Entry '" + name + "' does not have property '" + property + "': " + Log.pp(target);
            }
            var code = target[property];
            for (var i=0; i<checkBoxes.length;i ++) {
               checkBoxes[i].checked = (code === checkValues[i]);
               checkBoxes[i].enabled = true;
            }
        }
     }

     this.updateTarget(null);
  }
  CheckListEntryRow.prototype = new Control;


  // -- Middle right sub-panes (components to edit variables)
  function ResolverSelection_ComboBox(parent, mappingNames, mappingSelectionCallback) {
     this.__base__ = ComboBox;
     this.__base__(parent);

     var i;

     // -- UI
     this.toolTip = Text.H.VARIABLE_RESOLVER_TOOLTIP;
     this.enabled = true;
     this.editEnabled = false;
     for (i=0; i<mappingNames.length;i++) {
        this.addItem(mappingNames[i]);
     }
     this.currentItem = 0;
     this.enabled = false;

     // -- Call backs
     this.onItemSelected = function() {
        if (this.currentItem>=0) {
           mappingSelectionCallback(mappingNames[this.currentItem]);
        }
     }

     // -- Update model
     this.selectResolver = function(name) {
        if (name == null) {
              this.enabled = false;
        } else {
           for (i=0; i<mappingNames.length;i++) {
              if (name === mappingNames[i]) {
                 this.currentItem = i;
                 this.enabled = true;
                 break;
              }
           }
        }
     }
  }
  ResolverSelection_ComboBox.prototype = new ComboBox

  function ResolverSelectionRow(parent, rowStyle, name, mappingNames, mappingSelectionCallback) {
      this.__base__ = Control;
      this.__base__(parent);

      // -- UI
      this.sizer = new HorizontalSizer;
      this.sizer.margin = 2;
      this.sizer.spacing = 2;

      var the_Label = new Label( this );
      this.sizer.add(the_Label);
      the_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
      the_Label.minWidth = rowStyle.minLabelWidth;
      the_Label.text = name + ": ";

      var resolver_ComboBox = new ResolverSelection_ComboBox (parent, mappingNames, mappingSelectionCallback);
      resolver_ComboBox.minWidth = rowStyle.minDataWidth;
      this.sizer.add(resolver_ComboBox);

      // -- Update model
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
     var keyRow = new TextEntryRow(this, rowStyle, "FITS key", "Enter the name of a FITS key that will provide the value",
               "key", propertyTypes.FREE_TEXT, null);
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
           "Regular expression to text mapping.\nSelect a row to edit the details.\n"+
           "The first matching expression will define the value of the variable.\n"+
           "If none match, the variable is undefined. Use '/.*/' as the last expression to have a default value\n"+
           "&0; etc may be used to refer to the result of the regular expression",
           transformationSelectionCallback,
           false // Keep in order
     );
     selectionLayoutControl.sizer.add(regExpListSelection_Box);


     this.currentRegExpRow = new TextEntryRow(regExpListSelection_GroupBox, rowStyle, "Regexp",
        "A regular expression that will be tested against the key value", "regexp",
        propertyTypes.REG_EXP,
        function() {regExpListSelection_Box.currentModelElementChanged()});
     regExpListSelection_GroupBox.sizer.add(this.currentRegExpRow);

     this.currentReplacementRow = new TextEntryRow(regExpListSelection_GroupBox,rowStyle, "Replacement",
        "The replacement text to use if the regular expression matched.\n" +
        "&0; may be used to refer to the original text, &1;, &2; refers to parenthesized groups in the regular expression.", "replacement",
        propertyTypes.REG_EXP_REPLACMENT,
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
     var constantValueRow = new TextEntryRow(this, rowStyle, "Value",
     "The fixed value for this variable",
     "value", propertyTypes.FREE_TEXT, null);
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

  function TextValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     var keyRow = new TextEntryRow(this, rowStyle, "FITS key",
      "The name of a FITS key that will provide the value of this variable\n" +
      "(the FITS key value will be cleaned of special characters).",
      "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(keyRow);

     var formatRow = new TextEntryRow(this, rowStyle, "Format",
     "A valid C format string to display the value, for example '-%ls' to preceed the string with a dash\n"+
     "IMPORTANT - You must use '%ls', not '%s' to indicate the location of the string in the format",
     "format", propertyTypes.FREE_TEXT, null);
     this.sizer.add(formatRow);

     var caseRuleRow = new CheckListEntryRow(this, rowStyle, "Case conversion",
     "Case conversion",
     "case",['up','down','none'], ['UP','DOWN','NONE'], null);
     this.sizer.add(caseRuleRow);


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
        caseRuleRow.updateTarget(variableDefinition.parameters[resolverName]);
     }
     this.leave = function() {
        // Nothing the clean
     }
  }
  TextValueResolverControl.prototype = new Control;

  // ..............................................................................................

  function IntegerValueResolverControl(parent, resolverName, rowStyle) {
     this.__base__ = Control;
     this.__base__(parent);

     this.resolverName = resolverName;

     this.sizer = new VerticalSizer;

     // FITS Key
     var keyRow = new TextEntryRow(this, rowStyle, "FITS key",
      "The name of a FITS key that will provide the value",
      "key", propertyTypes.FREE_TEXT, null);
     this.sizer.add(keyRow);

     var useAbsoluteRow = new BooleanEntryRow(this, rowStyle, "Use absolute value",
        "If true, the absolute value of the corresponding value is taken (the sign is discarded)",
        "abs", null);
     this.sizer.add(useAbsoluteRow);

     var formatRow = new TextEntryRow(this, rowStyle, "Format",
     "A valid C format string to display the value, like '%4.4d', possibly with additional text like 'TEMP-%3d'",
     "format", propertyTypes.FREE_TEXT, null);
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
        useAbsoluteRow.updateTarget(variableDefinition.parameters[resolverName]);
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
     var key1Row = new TextEntryRow(this, rowStyle, "FITS key 1",
      "The name of a FITS key that will provide the first value",
      "key1", propertyTypes.FREE_TEXT, null);
     this.sizer.add(key1Row);

     var key2Row = new TextEntryRow(this, rowStyle, "FITS key 2",
      "The name of a FITS key that will provide the second value",
      "key2", propertyTypes.FREE_TEXT, null);
     this.sizer.add(key2Row);

     var formatRow = new TextEntryRow(this, rowStyle, "Format",
     "A valid C format string to display the 2 values, for example '%dx%d'",
     "format", propertyTypes.FREE_TEXT, null);
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

     var key1Row = new TextEntryRow(this, rowStyle, "LONG_OBS key",
     "The name of a FITS key that provide the longitude of the observatory",
     "keyLongObs", propertyTypes.FREE_TEXT, null);
     this.sizer.add(key1Row);

     var key2Row = new TextEntryRow(this, rowStyle, "JD key",
     "The name of a FITS key that contains the julian date of the observation",
     "keyJD", propertyTypes.FREE_TEXT, null);
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
           Text.H.VARIABLE_SELECTION_TOOLTIP,
           variableSelectionCallback,
           true // Sort by variable name
     );
     variableListSelection_GroupBox.sizer.add(variableListSelection_Box);



     //--  Right side - Enter parameters corresponding to selected variables
     var resolverSelectionCallback = function(resolverName) {
#ifdef DEBUG
        debug("VariableUIControl: resolverSelectionCallback - selected:",resolverName);
#endif
        that.updateResolver(resolverName);
     }

     var variableDetails_GroupBox = new GroupBox(this);
     variableDetails_GroupBox.title = "Parameters of variable";
     this.sizer.add(variableDetails_GroupBox);

     variableDetails_GroupBox.sizer = new VerticalSizer;



     this.resolverSelectionRow =  new ResolverSelectionRow(this, rowStyle, "Resolver", ffM_Resolver.resolverNames, resolverSelectionCallback);
     variableDetails_GroupBox.sizer.add(this.resolverSelectionRow);

     this.variableNameRow = new TextEntryRow(this, rowStyle, "Variable name",
        "Enter the name of the variable, it will be used as '&amp;name;' in a template",
        "name",
        propertyTypes.VAR_NAME,
        function() {variableListSelection_Box.currentModelElementChanged()});
     variableDetails_GroupBox.sizer.add(this.variableNameRow);

     this.descriptionRow = new TextEntryRow(this, rowStyle, "Description",
        "Enter a short description of the variable",
        "description",
        propertyTypes.FREE_TEXT,
        function() {variableListSelection_Box.currentModelElementChanged()});
     variableDetails_GroupBox.sizer.add(this.descriptionRow);

     this.variableShownRow = new BooleanEntryRow(this, rowStyle, "Show column by default",
        "If true, the corresponding column is shown in the Input file list by default",
        "show",
        function() {variableListSelection_Box.currentModelElementChanged()});
     variableDetails_GroupBox.sizer.add(this.variableShownRow);

     var addNewResolverControl = function(resolverControl){
        variableDetails_GroupBox.sizer.add(resolverControl);
        ffM_Resolver.resolverByName(resolverControl.resolverName).control = resolverControl;
        resolverControl.hide();
     }

     // Make all resolver controls, only the selected one will be shown
     addNewResolverControl(new ConstantValueResolverControl(variableDetails_GroupBox, 'Constant', rowStyle));

     addNewResolverControl(new TextValueResolverControl(variableDetails_GroupBox, 'Text', rowStyle));

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
     variableDetails_GroupBox.setMinHeight(GROUP_BOX_MIN_SIZE * this.font.lineSpacing + 2*this.sizer.margin);



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
#ifdef DEBUG
        debug("VariableUIControl: selectVariable - Variable selected ",variableDefinition.name );
#endif
         var resolverName;

        that.currentVariableDefinition = variableDefinition;

        // populate the common fields
        this.variableNameRow.updateTarget(variableDefinition);
        this.descriptionRow.updateTarget(variableDefinition);
        this.variableShownRow.updateTarget(variableDefinition);

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


   function ConfigurationGroupBox(parent, configurationSelectedCallback, configurationDuplicateCallback, configurationDeleteCallback ) {
      this.__base__ = GroupBox;
      this.__base__(parent);
      var that = this;

      // Initialized by configure()
      this.editedConfigurationSet = null;

      var configurationNameMinWidth = this.font.width( "MMMMMMMMMMMMMMMMM: " );
      this.title = "Configuration selection";

      this.sizer = new VerticalSizer;
      this.sizer.margin = 5;
      this.sizer.spacing = 5;

      var nameRow = new Control(this);
      this.sizer.add(nameRow);

      nameRow.sizer = new HorizontalSizer;

      nameRow.sizer.margin = 1;
      nameRow.sizer.spacing = 2;
      this.configurationSelection_ComboBox = new ConfigurationSelection_ComboBox(nameRow, [], configurationSelectedCallback);
      nameRow.sizer.add(this.configurationSelection_ComboBox);
      this.configurationSelection_ComboBox.minWidth = configurationNameMinWidth;


      nameRow.sizer.addStretch();

      var name_Label = new Label( nameRow );
      nameRow.sizer.add(name_Label);
      name_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
      name_Label.text = " Name: ";

      this.configurationName_Edit = new Edit(nameRow);
      nameRow.sizer.add(this.configurationName_Edit);
      this.configurationName_Edit.minWidth = configurationNameMinWidth;
      this.configurationName_Edit.toolTip = "Name of configuration\nIllegal special characters makes the field red.";
      this.configurationName_Edit.onTextUpdated = function() {
         // We should always have a selected configuration at this point, but play it safe
         if (that.selectedConfiguration !== null) {
            var t = this.text.trim();
            var configurationNames = ffM_Configuration.getAllConfigurationNames(that.editedConfigurationSet);
            // Avoid duplicate or illegal names (the check is a hack, but this is java script after all)
            // Characters are limited to avoid problem in case the name is used in some expression later
            if (t.length >0 && !(/[()\[\]{}&$;!?'".,]/.test(t)) && configurationNames.indexOf(t)===-1) {
               that.updateName(t);
            } else if (that.currentConfigurationName !== t) {
               // Red only if the trimmed value is not the current one
               this.textColor=0x00FF0000;
            } else {
               // Back to the current one, assumne legal
               this.textColor=0x00000000;
            }
         }
      }

      var addConfigurationButton = new ToolButton( nameRow );
      nameRow.sizer.add(addConfigurationButton);
      addConfigurationButton.icon = new Bitmap( ":/images/icons/copy.png" );
      addConfigurationButton.toolTip = "Add a configuration (duplicate current one)";
      addConfigurationButton.onClick = function() {
         configurationDuplicateCallback(that.currentConfigurationName);
      }

      var removeConfigurationButton = new ToolButton( nameRow );
      nameRow.sizer.add(removeConfigurationButton);
      removeConfigurationButton.icon = new Bitmap( ":/images/icons/cancel.png" );
      removeConfigurationButton.toolTip = "Delete the current configuration";
      removeConfigurationButton.onClick = function() {
         var msg = new MessageBox( "Do you want to delete the configuration '" +that.currentConfigurationName + "' ?",
                          "Are you sure?", StdIcon_Question, StdButton_Yes, StdButton_No );
         if (msg.execute() == StdButton_Yes) {
          configurationDeleteCallback(that.currentConfigurationName);
         }
      }

      var descriptionRow = new Control(this);
      this.sizer.add(descriptionRow);
      descriptionRow.sizer = new HorizontalSizer;

      var description_Label = new Label( descriptionRow );
      descriptionRow.sizer.add(description_Label);
      description_Label.textAlignment = TextAlign_Right|TextAlign_VertCenter;
      description_Label.text = " Description: ";

      descriptionRow.sizer.margin = 1;
      descriptionRow.sizer.spacing = 2;
      this.configurationComment_Edit = new Edit(descriptionRow);
      descriptionRow.sizer.add(this.configurationComment_Edit);
      this.configurationComment_Edit.toolTip = "Short description of the configuration";

      descriptionRow.sizer.addSpacing(50); // Pretty approximate

      // To track edited comment
      this.selectedConfiguration = null;


      this.configure = function(editedConfigurationSet, currentConfigurationName) {
         this.editedConfigurationSet = editedConfigurationSet;
         this.currentConfigurationName = currentConfigurationName;
         var configurationNames = ffM_Configuration.getAllConfigurationNames(editedConfigurationSet);
         this.configurationSelection_ComboBox.configure(configurationNames, currentConfigurationName);
         this.selectedConfiguration = ffM_Configuration.getConfigurationByName(editedConfigurationSet, currentConfigurationName);
         if (this.selectedConfiguration === null) {
            throw "Internal Error - configuration name '" + currentConfigurationName + "' not in list of configurations";
         }
         this.configurationComment_Edit.text = this.selectedConfiguration.description;
         this.configurationName_Edit.text = currentConfigurationName;
         this.configurationName_Edit.textColor =0x00000000;
      }
      this.updateName = function(newConfigurationName) {
         // Assume the name is allowed and not duplicated (checked by caller),
         // set the new current name in our working copy (our selected configuration)
         this.selectedConfiguration.name = newConfigurationName;
         this.currentConfigurationName = newConfigurationName;
         // Update the whole configuration selection box with the new names
         var configurationNames = ffM_Configuration.getAllConfigurationNames(this.editedConfigurationSet);
         if (configurationNames.indexOf(newConfigurationName)===-1) {
            throw "Internal Error - new configuration name '" + newConfigurationName + "' not in list of configurations";
         }
         this.configurationSelection_ComboBox.configure(configurationNames, newConfigurationName);
         this.configurationName_Edit.textColor=0x00000000;
      }
      this.configurationComment_Edit.onTextUpdated = function() {
         if (that.selectedConfiguration !== null) {
            that.selectedConfiguration.description = this.text;
         }
      }
   }
   ConfigurationGroupBox.prototype = new GroupBox;

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

      var rankFormat = new TextEntryRow(colLayout1_Control, rowStyle, "&rank; format",
      "Enter a valid C format string for the &rank; value, like '%3.3d'\nYou can also add text around like 'N%3.3d'",
      "format", propertyTypes.FREE_TEXT, null);
      colLayout1_Control.sizer.add(rankFormat);

      // Right column
      var colLayout2_Control = new Control;
      this.sizer.add(colLayout2_Control);
      colLayout2_Control.sizer = new VerticalSizer;

      var countFormat = new TextEntryRow(colLayout2_Control, rowStyle, "&count format",
      "Enter a valid C format string for the &count; value, like '%3.3d'\nYou can also add text around like 'group-%d'",
      "format", propertyTypes.FREE_TEXT, null);
      colLayout2_Control.sizer.add(countFormat);


      this.configure = function(editedConfigurationSet, currentConfigurationName) {
         var configurationNames = ffM_Configuration.getAllConfigurationNames(editedConfigurationSet);
         this.selectedConfiguration = ffM_Configuration.getConfigurationByName(editedConfigurationSet, currentConfigurationName);

         rankFormat.updateTarget(this.selectedConfiguration.builtins.rank);
         countFormat.updateTarget(this.selectedConfiguration.builtins.count);
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
#ifdef DEBUG
        debug("ConfigurationDialog: configurationSelectedCallback - ConfigurationSet selected:",configurationName);
#endif
        that.selectedConfiguration = ffM_Configuration.getConfigurationByName(that.editedConfigurationSet, configurationName);
        if (that.selectedConfiguration == null) {
           throw "PROGRAM ERROR - Invalid configuration set name '" + configurationName +"'";
        }

        // Update model
        that.currentConfigurationName = configurationName;
        // Update UI
        that.variableUI.updateVariableList(that.selectedConfiguration.variableList);
        that.builtinVariable_Group.configure(that.editedConfigurationSet, that.currentConfigurationName);
        // Update the description text
        that.configurationLayoutControl.configure(that.editedConfigurationSet, that.currentConfigurationName);
     }

     var deleteConfigurationCallback = function(currentConfigurationName) {
#ifdef DEBUG
        debug("ConfigurationDialog: deleteConfigurationCallback");
#endif
        var newConfigurationName = ffM_Configuration.removeConfigurationByName(that.editedConfigurationSet, currentConfigurationName);
        if (newConfigurationName == null) {
           Console.writeln("Configuration '" + currentConfigurationName  +"' not deleted, it is the last one");
        } else {
           configurationSelectedCallback(newConfigurationName);
        }
     }

     // We duplicate the current configuration
     var duplicateConfigurationCallback = function(configurationName) {
#ifdef DEBUG
        debug("ConfigurationDialog: duplicateConfigurationCallback");
#endif
        var selectedConfiguration = ffM_Configuration.getConfigurationByName(that.editedConfigurationSet, configurationName);
        var newConfiguration = deepCopyData(selectedConfiguration);
        newConfiguration.name = createUniqueName(configurationName,ffM_Configuration.getAllConfigurationNames(that.editedConfigurationSet));
        that.editedConfigurationSet.push(newConfiguration);
        configurationSelectedCallback(newConfiguration.name);
        Console.writeln("Configuration '" + newConfiguration.name  +"' created");
     }

     // -- Model call backs
     // New variable requested in current configuration, define one with default values
     var variableDefinitionFactory = function() {
#ifdef DEBUG
          debug("ConfigurationDialog: variableDefinitionFactory - create new variable");
#endif
          that.newVariableCounter++;
          return ffM_Configuration.defineVariable("new_" + that.newVariableCounter,'','Text');
     }

     // -- Build the top level pane

     // Top pane - select configuration to operate upon
     this.configurationLayoutControl = new ConfigurationGroupBox(this, configurationSelectedCallback, duplicateConfigurationCallback, deleteConfigurationCallback);
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
        this.selectedConfiguration = ffM_Configuration.getConfigurationByName(this.editedConfigurationSet, configurationNameToEdit);
        if (this.selectedConfiguration === null) {
           throw "Internal Error - current configuration not found:'" + configurationNameToEdit + "'";
        }
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
     },
     ConfigurationSelection_ComboBox: ConfigurationSelection_ComboBox,

  }

}) ();
