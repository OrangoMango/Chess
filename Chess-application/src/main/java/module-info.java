// File managed by WebFX (DO NOT EDIT MANUALLY)

module Chess.application {

    // Direct dependencies modules
    requires java.base;
    requires javafx.base;
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.media;
    requires webfx.platform.console;
    requires webfx.platform.fetch;
    requires webfx.platform.resource;
    requires webfx.platform.scheduler;

    // Exported packages
    exports com.orangomango.chess;
    exports com.orangomango.chess.multiplayer;
    exports com.orangomango.chess.ui;

    // Resources packages
    opens audio;
    opens images;

    // Provided services
    provides javafx.application.Application with com.orangomango.chess.MainApplication;

}