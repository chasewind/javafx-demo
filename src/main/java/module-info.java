module com.example.javafxdemo {
    requires java.desktop;
    requires javafx.base;
    requires  javafx.controls;
    requires javafx.fxml;
    requires java.net.http;
    requires org.slf4j;
    requires com.google.gson;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.commons.collections4;
    requires com.fasterxml.jackson.databind;
    requires lombok;
    requires com.google.common;
    requires java.logging;
    requires org.fxmisc.richtext;
    requires reactfx;
    requires org.fxmisc.flowless;


    exports org.controlsfx.control;
    exports org.controlsfx.control.action;
    exports org.controlsfx.control.cell;
    exports org.controlsfx.control.decoration;
    exports org.controlsfx.control.spreadsheet;
    exports org.controlsfx.control.table;
    exports org.controlsfx.control.tableview2;
    exports org.controlsfx.control.tableview2.actions;
    exports org.controlsfx.control.tableview2.cell;
    exports org.controlsfx.control.tableview2.event;
    exports org.controlsfx.control.tableview2.filter.filtereditor;
    exports org.controlsfx.control.tableview2.filter.filtermenubutton;
    exports org.controlsfx.control.tableview2.filter.parser;
    exports org.controlsfx.control.tableview2.filter.popupfilter;
    exports org.controlsfx.control.textfield;
    exports org.controlsfx.dialog;
    exports org.controlsfx.glyphfont;
    exports org.controlsfx.property;
    exports org.controlsfx.property.editor;
    exports org.controlsfx.tools;
    exports org.controlsfx.validation;
    exports org.controlsfx.validation.decoration;


    //要支持序列化
    opens com.example.javafxdemo to javafx.fxml, com.google.gson;
    opens com.example.javafxdemo.event to com.google.common;
    exports com.example.javafxdemo;
}