package com.ve.acpp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ve.view.editor.Editor;
import com.ve.view.editor.out.Config;
import com.ve.view.editor.out.TextInterface;
import com.ve.view.ext.ClipboardPanel;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    Editor editor;

    ClipboardPanel clipboardPanel;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editor = findViewById(R.id.activity_main_editor);
        Config config=new Config();
        config.showNonPrinting=true;
        config.wordWrap=false;
        editor.applyConfig(config);
        clipboardPanel=new ClipboardPanel(editor);
        editor.getOperator().setSelectionChangedListener((active, selStart, selEnd) -> {
            if (active){
                clipboardPanel.show();
            }else {
                clipboardPanel.hide();
            }
        });

        test();
    }

    private void test() {
        StringBuffer stringBuffer = new StringBuffer();
        Random random=new Random();
        for (int i = 0; i < 20; i++) {
            if (random.nextBoolean()) {
                stringBuffer.append(" asfgweilfuWEF72QWDFYFregargaergnwderghwaefwsefweg数的rnwd文档发给nueffgeriugiesgier\n");
            }
            stringBuffer.append("  asfgweilfuWEF72G\nQWDFYF、nwdwefgGAGDGSDFGHAWERGHWERHERREGRAGEsddnwd、nuef\nefgeriugiesgier\n");

        }

        TextInterface textInterface = editor.getTextInterface();
        textInterface.paste(stringBuffer.toString());
        textInterface.replaceText(0,textInterface.getLength()-1,"Good!");
    }

    public void onRedo(View view) {
        editor.getTextInterface().redo();
    }

    public void onUndo(View view) {
        editor.getTextInterface().undo();
    }
}
