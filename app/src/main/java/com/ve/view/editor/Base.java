package com.ve.view.editor;

import com.ve.view.editor.out.Config;
import com.ve.view.text.document.Document;

public class Base {
    protected Editor editor;
    protected TextController controller;
    protected EditorInputConnection inputConnection;
    protected Document document;
    protected Caret caret;
    protected Painter painter;
    protected ListenManager operator;
    protected SpanManager spanManager;
    protected Config config;

    public Base(Editor editor){
        this.editor=editor;
    }
    public void init(){
        document=editor.document;
        controller=editor.controller;
        inputConnection=editor.inputConnection;
        caret=editor.caret;
        painter =editor.painter;
        operator=editor.operator;
        spanManager=editor.spanManager;
        config=editor.config;
    }
}
