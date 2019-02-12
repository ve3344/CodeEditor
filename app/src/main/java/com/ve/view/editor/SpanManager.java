package com.ve.view.editor;

import android.graphics.Color;

import com.ve.lexer.Lexer;
import com.ve.view.utils.Pair;
import com.ve.view.editor.span.SpanType;
import com.ve.view.editor.span.TextSpanData;

public class SpanManager extends Base{
    protected final Lexer lexer = new Lexer(results -> editor.post(() -> {
        TextSpanData textSpanData = new TextSpanData();
        for (Pair result : results) {
            //System.err.printf("%d  %d\n",result.getFirst(),result.getSecond());
            textSpanData.addSpan(result.getFirst(),new SpanType(result.getSecond()==0? Color.BLACK:Color.RED));
        }
        painter.setTextSpanData(textSpanData);
        editor.invalidate();
    }));

    public SpanManager(Editor editor) {
        super(editor);
    }

    public void startSpan() {
        lexer.tokenize(document);
    }

    public void stopSpan() {
        lexer.cancelTokenize();
    }
}
