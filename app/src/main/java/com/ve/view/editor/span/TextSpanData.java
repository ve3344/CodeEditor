package com.ve.view.editor.span;

import android.graphics.Color;
import android.graphics.Paint;

import java.util.ArrayList;
import java.util.List;

public class TextSpanData {


    public static final SpanType NORMAL = new SpanType(Color.BLACK);

    private List<SpanNode> spanNodes;

    public TextSpanData() {
        spanNodes = new ArrayList<>();
        addSpan(0, NORMAL);
    }


    public void addSpan(int position, SpanType type) {
        if (type == null) {
            type = NORMAL;
        }
        spanNodes.add(new SpanNode(position, type));

    }


    public void clear() {
        spanNodes.clear();
        addSpan(0, NORMAL);
    }

    public class SpanSeeker {
        private Paint paint;
        private SpanNode currentSpan, nextSpan;
        private int index = 0;
        private int spanPosition = 0;

        public SpanSeeker(Paint paint) {
            this.paint = paint;
        }

        public void begin(int currentPosition) {

            index = 0;
            spanPosition=0;
            nextSpan = spanNodes.get(index++);
            do {
                currentSpan = nextSpan;
                spanPosition += currentSpan.length;
                if (index < spanNodes.size()) {
                    nextSpan = spanNodes.get(index++);
                } else {
                    nextSpan = null;
                }
            } while (nextSpan != null && spanPosition <= currentPosition);

            currentSpan.type.onSpan(paint);
        }

        public boolean reachedNextSpan(int currentPosition, SpanNode span) {
            return span != null&&currentPosition>=spanPosition;
        }

        public void listenSpan(int currentPosition) {
            if (reachedNextSpan(currentPosition, nextSpan)) {
                currentSpan = nextSpan;
                spanPosition += currentSpan.length;
                currentSpan.type.onSpan(paint);

                if (index < spanNodes.size()) {
                    nextSpan = spanNodes.get(index++);
                } else {
                    nextSpan = null;
                }

            }
        }

        public SpanNode getCurrentSpan() {
            return currentSpan;
        }
    }

    public SpanSeeker getNewSeeker(Paint paint) {
        return new SpanSeeker(paint);
    }


    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getSimpleName() + "(" + spanNodes.size() + ")" + "{\n");
        for (SpanNode spanNode : spanNodes) {
            stringBuilder.append(String.format("[%d,%s]\n", spanNode.length, spanNode.type.toString()));
        }

        return stringBuilder.append("\n}\n").toString();
    }
}
