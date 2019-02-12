package com.ve.view.ext;

import android.content.Context;
import android.content.res.TypedArray;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import com.ve.view.editor.Editor;

public class ClipboardPanel {
    protected Editor editor;
    private Context context;
    private ActionMode actionMode;

    public ClipboardPanel(Editor editor) {
        this.editor = editor;
        context = editor.getContext();


    }


    public void show() {
        startClipboardAction();
    }

    public void hide() {
        stopClipboardAction();
    }

    public void startClipboardAction() {
        // TODO: Implement this method
        if (actionMode == null)
            editor.startActionMode(new ActionMode.Callback() {

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {


                    actionMode = mode;
                    mode.setTitle(android.R.string.selectTextMode);
                    TypedArray array = context.getTheme().obtainStyledAttributes(new int[]{
                            android.R.attr.actionModeSelectAllDrawable,
                            android.R.attr.actionModeCutDrawable,
                            android.R.attr.actionModeCopyDrawable,
                            android.R.attr.actionModePasteDrawable,
                    });
                    menu.add(0, 0, 0, context.getString(android.R.string.selectAll))
                            .setShowAsActionFlags(2)
                            .setAlphabeticShortcut('a')
                            .setIcon(array.getDrawable(0));

                    menu.add(0, 1, 0, context.getString(android.R.string.cut))
                            .setShowAsActionFlags(2)
                            .setAlphabeticShortcut('x')
                            .setIcon(array.getDrawable(1));

                    menu.add(0, 2, 0, context.getString(android.R.string.copy))
                            .setShowAsActionFlags(2)
                            .setAlphabeticShortcut('c')
                            .setIcon(array.getDrawable(2));

                    menu.add(0, 3, 0, context.getString(android.R.string.paste))
                            .setShowAsActionFlags(2)
                            .setAlphabeticShortcut('v')
                            .setIcon(array.getDrawable(3));
                    array.recycle();
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    // TODO: Implement this method
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case 0:
                            editor.getSelectInterface().selectAll();
                            break;
                        case 1:
                            editor.getTextInterface().cut();
                            mode.finish();
                            break;
                        case 2:
                            editor.getTextInterface().copy();
                            mode.finish();
                            break;
                        case 3:
                            editor.getTextInterface().paste();
                            mode.finish();
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode p1) {
                    editor.getSelectInterface().cancelSelect();
                    actionMode = null;
                }
            });

    }

    public void stopClipboardAction() {
        if (actionMode != null) {
            actionMode.finish();
            actionMode = null;
        }
    }

}
